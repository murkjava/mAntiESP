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
import org.bukkit.event.player.*;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class VisibilityListener implements Listener {
    public static final Set<UUID> CACHED_PLAYERS = new HashSet<>();
    public static final Map<Integer, UUID> ENTITY_TO_PLAYER = new ConcurrentHashMap<>();

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

        for (Player player : Bukkit.getOnlinePlayers()) {
            CACHED_PLAYERS.add(player.getUniqueId());
            ENTITY_TO_PLAYER.put(player.getEntityId(), player.getUniqueId());
        }

        task = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            plugin.getVisibilityService().updateLocations();
            double maxDistance = config.getMaxDistance() + (config.getF5().isEnabled() ? config.getF5().getDistance() : 0.0);

            for (UUID uuid : CACHED_PLAYERS) {
                Player observer = Bukkit.getPlayer(uuid);
                if (observer == null) continue;

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
        CACHED_PLAYERS.clear();
        ENTITY_TO_PLAYER.clear();
    }

    @EventHandler
    public void on(PlayerJoinEvent e) {
        Player player = e.getPlayer();
        CACHED_PLAYERS.add(player.getUniqueId());
        ENTITY_TO_PLAYER.put(player.getEntityId(), player.getUniqueId());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void on(PlayerQuitEvent e) {
        Player player = e.getPlayer();

        CACHED_PLAYERS.remove(player.getUniqueId());
        ENTITY_TO_PLAYER.remove(player.getEntityId());

        plugin.getVisibilityManager().removePlayer(player.getUniqueId());
        plugin.getVisibilityManager().removeEntity(player.getEntityId());
        plugin.getVisibilityService().removePlayer(player.getUniqueId());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void on(EntityDeathEvent e) {
        plugin.getVisibilityManager().removeEntity(e.getEntity().getEntityId());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void on(PlayerChangedWorldEvent e) {
        Player player = e.getPlayer();

        ENTITY_TO_PLAYER.put(player.getEntityId(), player.getUniqueId());
        plugin.getVisibilityManager().removePlayer(player.getUniqueId());
        plugin.getVisibilityManager().removeEntity(player.getEntityId());
        plugin.getVisibilityService().removePlayer(player.getUniqueId());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void on(PlayerRespawnEvent e) {
        Player player = e.getPlayer();

        ENTITY_TO_PLAYER.put(player.getEntityId(), player.getUniqueId());
        plugin.getVisibilityManager().removePlayer(player.getUniqueId());
        plugin.getVisibilityManager().removeEntity(player.getEntityId());
        plugin.getVisibilityService().removePlayer(player.getUniqueId());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void on(PlayerTeleportEvent e) {
        if (e.getFrom().getWorld() == null || e.getTo().getWorld() == null) {
            return;
        }

        Player player = e.getPlayer();
        plugin.getVisibilityService().removePlayer(player.getUniqueId());

        if (!e.getFrom().getWorld().equals(e.getTo().getWorld())) {
            plugin.getVisibilityManager().removePlayer(player.getUniqueId());
            plugin.getVisibilityManager().removeEntity(player.getEntityId());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void on(EntityTeleportEvent e) {
        if (e.getTo() == null || e.getFrom().getWorld() == null || e.getTo().getWorld() == null) {
            return;
        }

        if (!e.getFrom().getWorld().equals(e.getTo().getWorld())) {
            plugin.getVisibilityManager().removeEntity(e.getEntity().getEntityId());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void on(ChunkUnloadEvent e) {
        for (Entity entity : e.getChunk().getEntities()) {
            plugin.getVisibilityManager().removeEntity(entity.getEntityId());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void on(EntityPotionEffectEvent e) {
        PotionEffectType type = e.getModifiedType();
        if (type != PotionEffectType.GLOWING && type != PotionEffectType.BLINDNESS && type != PotionEffectType.INVISIBILITY) {
            return;
        }

        if (!(e.getEntity() instanceof LivingEntity living)) {
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
