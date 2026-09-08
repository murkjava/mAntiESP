package dev.murk.antiesp.listener;

import dev.murk.antiesp.MAntiESP;
import dev.murk.antiesp.config.Config;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityPotionEffectEvent;
import org.bukkit.event.entity.EntityTeleportEvent;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.scheduler.BukkitTask;

public class VisibilityListener implements Listener {
    private final MAntiESP plugin;
    private final Config config;
    private BukkitTask task;

    public VisibilityListener(MAntiESP plugin, Config config) {
        this.plugin = plugin;
        this.config = config;
        startTask();
    }

    public void startTask() {
        cancelTask();
        task = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            plugin.getVisibilityService().updateLocations();
            double maxDistance = config.getMaxDistance() + (config.getF5().isEnabled() ? config.getF5().getDistance() : 0.0);
            for (Player observer : Bukkit.getOnlinePlayers()) {
                for (Entity target : observer.getNearbyEntities(maxDistance, maxDistance, maxDistance)) {
                    if (!config.shouldCheckEntity(target)) continue;

                    updateVisibility(observer, target);
                }
            }
        }, 0, config.getTicksPeriod());
    }

    public void restartTask() {
        startTask();
    }

    public void cancelTask() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void on(PlayerQuitEvent event) {
        Player player = event.getPlayer();

        plugin.getVisibilityManager().removePlayer(player.getUniqueId());
        plugin.getVisibilityManager().removeEntity(player.getEntityId());
        plugin.getVisibilityService().removePlayer(player.getUniqueId());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void on(EntityDeathEvent event) {
        plugin.getVisibilityManager().removeEntity(event.getEntity().getEntityId());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void on(PlayerChangedWorldEvent event) {
        Player player = event.getPlayer();

        plugin.getVisibilityManager().removePlayer(player.getUniqueId());
        plugin.getVisibilityManager().removeEntity(player.getEntityId());
        plugin.getVisibilityService().removePlayer(player.getUniqueId());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void on(PlayerRespawnEvent event) {
        Player player = event.getPlayer();

        plugin.getVisibilityManager().removePlayer(player.getUniqueId());
        plugin.getVisibilityManager().removeEntity(player.getEntityId());
        plugin.getVisibilityService().removePlayer(player.getUniqueId());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void on(PlayerTeleportEvent event) {
        if (event.getFrom().getWorld() == null || event.getTo().getWorld() == null) {
            return;
        }

        Player player = event.getPlayer();
        plugin.getVisibilityService().removePlayer(player.getUniqueId());

        if (!event.getFrom().getWorld().equals(event.getTo().getWorld())) {
            plugin.getVisibilityManager().removePlayer(player.getUniqueId());
            plugin.getVisibilityManager().removeEntity(player.getEntityId());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void on(EntityTeleportEvent event) {
        if (event.getTo() == null || event.getFrom().getWorld() == null || event.getTo().getWorld() == null) {
            return;
        }

        if (!event.getFrom().getWorld().equals(event.getTo().getWorld())) {
            plugin.getVisibilityManager().removeEntity(event.getEntity().getEntityId());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void on(ChunkUnloadEvent event) {
        for (Entity entity : event.getChunk().getEntities()) {
            plugin.getVisibilityManager().removeEntity(entity.getEntityId());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void on(EntityPotionEffectEvent event) {
        PotionEffectType type = event.getModifiedType();
        if (type != PotionEffectType.GLOWING && type != PotionEffectType.BLINDNESS && type != PotionEffectType.INVISIBILITY) {
            return;
        }

        if (!(event.getEntity() instanceof LivingEntity living)) {
            return;
        }

        Bukkit.getScheduler().runTask(plugin, () -> {
            if (!living.isValid()) return;

            double maxDistance = config.getMaxDistance() + (config.getF5().isEnabled() ? config.getF5().getDistance() : 0.0);
            if (living instanceof Player observer) {
                for (Entity target : observer.getNearbyEntities(maxDistance, maxDistance, maxDistance)) {
                    if (config.shouldCheckEntity(target)) {
                        updateVisibility(observer, target);
                    }
                }
            }

            double maxDistSq = maxDistance * maxDistance;
            for (Player observer : living.getWorld().getPlayers()) {
                if (observer.equals(living)) continue;

                if (observer.getLocation().distanceSquared(living.getLocation()) <= maxDistSq) {
                    if (config.shouldCheckEntity(living)) {
                        updateVisibility(observer, living);
                    }
                }
            }
        });
    }

    public void updateVisibility(Player observer, Entity target) {
        boolean canSee = plugin.getVisibilityService().canSee(observer, target);

        if (canSee) {
            plugin.getVisibilityManager().showFor(observer, target);
        } else if (!config.getHide().isIgnoreNametag() && plugin.getVisibilityService().canSeeNametag(observer, target)) {
            plugin.getVisibilityManager().stripFor(observer, target);
        } else {
            plugin.getVisibilityManager().hideFor(observer, target);
        }
    }
}
