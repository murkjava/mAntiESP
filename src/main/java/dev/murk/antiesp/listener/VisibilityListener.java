package dev.murk.antiesp.listener;

import dev.murk.antiesp.MAntiESP;
import dev.murk.antiesp.config.Config;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
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

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class VisibilityListener implements Listener {
    public static final Set<UUID> CACHED_PLAYERS = ConcurrentHashMap.newKeySet();
    public static final Map<Integer, UUID> ENTITY_TO_PLAYER = new ConcurrentHashMap<>();

    private final MAntiESP plugin;
    private final Config config;
    private BukkitTask task;

    private static final class PlayerSnapshot {
        final Player player;
        final UUID worldId;
        final double x;
        final double y;
        final double z;
        final int cellX;
        final int cellZ;

        PlayerSnapshot(Player player) {
            this.player = player;
            this.worldId = player.getWorld().getUID();
            Location loc = player.getLocation();
            this.x = loc.getX();
            this.y = loc.getY();
            this.z = loc.getZ();
            this.cellX = ((int) Math.floor(x)) >> 6;
            this.cellZ = ((int) Math.floor(z)) >> 6;
        }
    }

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
            Collection<? extends Player> online = Bukkit.getOnlinePlayers();
            if (online.isEmpty()) {
                return;
            }

            plugin.getVisibilityService().updateLocations(online);
            double maxDistance = config.getMaxDistance() + (config.getF5().isEnabled() ? config.getF5().getDistance() : 0.0);
            double maxDistSq = maxDistance * maxDistance;

            List<PlayerSnapshot> snapshots = new ArrayList<>(online.size());
            for (Player player : online) {
                if (player != null && player.isOnline()) {
                    snapshots.add(new PlayerSnapshot(player));
                }
            }

            if (config.isOnlyPlayer()) {
                Map<UUID, Map<Long, List<PlayerSnapshot>>> grid = new HashMap<>();
                for (int i = 0; i < snapshots.size(); i++) {
                    PlayerSnapshot s = snapshots.get(i);
                    long cellKey = (((long) s.cellX) << 32) | (s.cellZ & 0xFFFFFFFFL);
                    grid.computeIfAbsent(s.worldId, k -> new HashMap<>())
                            .computeIfAbsent(cellKey, k -> new ArrayList<>(4))
                            .add(s);
                }

                int cellRadius = (int) Math.ceil(maxDistance / 64.0);

                for (int i = 0; i < snapshots.size(); i++) {
                    PlayerSnapshot observer = snapshots.get(i);
                    boolean hasBypass = observer.player.hasPermission("mantiesp.bypass")
                            || (config.getHide().isIgnoreSpectator() && observer.player.getGameMode() == GameMode.SPECTATOR);

                    if (hasBypass) {
                        if (plugin.getVisibilityManager().hasHiddenEntities(observer.player)
                                || plugin.getVisibilityManager().hasStrippedEntities(observer.player)) {
                            plugin.getVisibilityManager().restoreFor(observer.player);
                        }
                        continue;
                    }

                    Map<Long, List<PlayerSnapshot>> worldGrid = grid.get(observer.worldId);
                    if (worldGrid == null) continue;

                    int minCx = observer.cellX - cellRadius;
                    int maxCx = observer.cellX + cellRadius;
                    int minCz = observer.cellZ - cellRadius;
                    int maxCz = observer.cellZ + cellRadius;

                    for (int cx = minCx; cx <= maxCx; cx++) {
                        for (int cz = minCz; cz <= maxCz; cz++) {
                            long key = (((long) cx) << 32) | (cz & 0xFFFFFFFFL);
                            List<PlayerSnapshot> targets = worldGrid.get(key);
                            if (targets == null) continue;

                            for (int t = 0; t < targets.size(); t++) {
                                PlayerSnapshot target = targets.get(t);
                                if (target.player == observer.player) continue;

                                double dx = target.x - observer.x;
                                double dy = target.y - observer.y;
                                double dz = target.z - observer.z;
                                if (dx * dx + dy * dy + dz * dz <= maxDistSq) {
                                    updateVisibility(observer.player, target.player);
                                }
                            }
                        }
                    }
                }
            } else {
                for (int i = 0; i < snapshots.size(); i++) {
                    PlayerSnapshot observer = snapshots.get(i);
                    boolean hasBypass = observer.player.hasPermission("mantiesp.bypass")
                            || (config.getHide().isIgnoreSpectator() && observer.player.getGameMode() == GameMode.SPECTATOR);

                    if (hasBypass) {
                        if (plugin.getVisibilityManager().hasHiddenEntities(observer.player)
                                || plugin.getVisibilityManager().hasStrippedEntities(observer.player)) {
                            plugin.getVisibilityManager().restoreFor(observer.player);
                        }
                        continue;
                    }

                    for (Entity target : observer.player.getNearbyEntities(maxDistance, maxDistance, maxDistance)) {
                        if (!config.shouldCheckEntity(target)) continue;

                        updateVisibility(observer.player, target);
                    }
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

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void on(PlayerGameModeChangeEvent e) {
        if (e.getNewGameMode() == GameMode.SPECTATOR) {
            plugin.getVisibilityManager().restoreFor(e.getPlayer());
        }
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
        if (e.getFrom().getWorld() == null || e.getTo().getWorld() == null) return;

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
            double maxDistSq = maxDistance * maxDistance;

            if (living instanceof Player observer) {
                if (config.isOnlyPlayer()) {
                    Location obsLoc = observer.getLocation();
                    double ox = obsLoc.getX();
                    double oy = obsLoc.getY();
                    double oz = obsLoc.getZ();

                    for (Player target : observer.getWorld().getPlayers()) {
                        if (observer.equals(target)) continue;

                        Location targetLoc = target.getLocation();
                        double dx = targetLoc.getX() - ox;
                        double dy = targetLoc.getY() - oy;
                        double dz = targetLoc.getZ() - oz;
                        if (dx * dx + dy * dy + dz * dz <= maxDistSq) {
                            updateVisibility(observer, target);
                        }
                    }
                } else {
                    for (Entity target : observer.getNearbyEntities(maxDistance, maxDistance, maxDistance)) {
                        if (config.shouldCheckEntity(target)) {
                            updateVisibility(observer, target);
                        }
                    }
                }
            }

            if (config.shouldCheckEntity(living)) {
                Location livingLoc = living.getLocation();
                double lx = livingLoc.getX();
                double ly = livingLoc.getY();
                double lz = livingLoc.getZ();

                for (Player observer : living.getWorld().getPlayers()) {
                    if (observer.equals(living)) continue;

                    Location obsLoc = observer.getLocation();
                    double dx = lx - obsLoc.getX();
                    double dy = ly - obsLoc.getY();
                    double dz = lz - obsLoc.getZ();
                    if (dx * dx + dy * dy + dz * dz <= maxDistSq) {
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
