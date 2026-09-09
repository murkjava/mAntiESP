package dev.murk.antiesp.visibility;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerDestroyEntities;
import dev.murk.antiesp.MAntiESP;
import dev.murk.antiesp.config.Config;
import dev.murk.antiesp.listener.VisibilityListener;
import dev.murk.antiesp.packet.PacketSender;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class VisibilityManager {
    private final Map<UUID, Set<Integer>> hiddenEntities = new ConcurrentHashMap<>();
    private final Map<UUID, Set<Integer>> strippedEntities = new ConcurrentHashMap<>();

    public boolean hideFor(Player observer, Entity... targets) {
        if (observer == null || targets == null || targets.length == 0) {
            return false;
        }

        try {
            Set<Integer> hidden = hiddenEntities.computeIfAbsent(observer.getUniqueId(), k -> ConcurrentHashMap.newKeySet());
            Set<Integer> stripped = strippedEntities.get(observer.getUniqueId());
            List<Integer> toDestroy = new ArrayList<>();

            for (Entity target : targets) {
                if (target == null) {
                    continue;
                }

                int entityId = target.getEntityId();
                if (stripped != null) {
                    stripped.remove(entityId);
                }

                if (hidden.add(entityId)) {
                    toDestroy.add(entityId);
                }
            }

            if (!toDestroy.isEmpty()) {
                int[] entityIds = new int[toDestroy.size()];
                for (int i = 0; i < toDestroy.size(); i++) {
                    entityIds[i] = toDestroy.get(i);
                }
                WrapperPlayServerDestroyEntities packet = new WrapperPlayServerDestroyEntities(entityIds);
                PacketEvents.getAPI().getPlayerManager().sendPacket(observer, packet);
            }

            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean stripFor(Player observer, Entity... targets) {
        if (observer == null || targets == null || targets.length == 0) {
            return false;
        }

        try {
            Set<Integer> hidden = hiddenEntities.get(observer.getUniqueId());
            Set<Integer> stripped = strippedEntities.computeIfAbsent(observer.getUniqueId(), k -> ConcurrentHashMap.newKeySet());

            for (Entity target : targets) {
                if (target == null) {
                    continue;
                }

                int entityId = target.getEntityId();
                boolean wasHidden = hidden != null && hidden.remove(entityId);

                if (wasHidden) {
                    PacketSender.sendSpawnPacket(observer, target);
                    PacketSender.sendEntityTeleport(observer, target);
                    PacketSender.sendMetadataPacket(observer, target);
                    PacketSender.sendEmptyEquipmentPacket(observer, entityId);
                    PacketSender.sendPotionEffects(observer, target);
                    stripped.add(entityId);
                } else if (stripped.add(entityId)) {
                    PacketSender.sendEmptyEquipmentPacket(observer, entityId);
                }
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean showFor(Player observer, Entity... targets) {
        if (observer == null || targets == null || targets.length == 0) {
            return false;
        }

        try {
            Set<Integer> hidden = hiddenEntities.get(observer.getUniqueId());
            Set<Integer> stripped = strippedEntities.get(observer.getUniqueId());
            if ((hidden == null || hidden.isEmpty()) && (stripped == null || stripped.isEmpty())) {
                return false;
            }

            for (Entity target : targets) {
                if (target == null) continue;

                int entityId = target.getEntityId();
                boolean wasHidden = hidden != null && hidden.remove(entityId);
                boolean wasStripped = stripped != null && stripped.remove(entityId);

                if (wasHidden) {
                    PacketSender.sendAllSpawnPackets(observer, target);
                } else if (wasStripped) {
                    PacketSender.sendMetadataPacket(observer, target);
                    PacketSender.sendEquipmentPacket(observer, target);
                    PacketSender.sendPotionEffects(observer, target);
                }
            }

            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean hasHiddenEntities(Player observer) {
        if (observer == null) return false;
        Set<Integer> hidden = hiddenEntities.get(observer.getUniqueId());
        return hidden != null && !hidden.isEmpty();
    }

    public boolean hasStrippedEntities(Player observer) {
        if (observer == null) return false;
        Set<Integer> stripped = strippedEntities.get(observer.getUniqueId());
        return stripped != null && !stripped.isEmpty();
    }

    public void addHidden(Player observer, int entityId) {
        if (observer == null) {
            return;
        }
        Set<Integer> hidden = hiddenEntities.computeIfAbsent(observer.getUniqueId(), k -> ConcurrentHashMap.newKeySet());
        hidden.add(entityId);
    }

    public void addStripped(Player observer, int entityId) {
        if (observer == null) {
            return;
        }
        Set<Integer> stripped = strippedEntities.computeIfAbsent(observer.getUniqueId(), k -> ConcurrentHashMap.newKeySet());
        stripped.add(entityId);
    }

    public boolean isHidden(Player observer, int target) {
        if (observer == null) {
            return false;
        }

        Set<Integer> hidden = hiddenEntities.get(observer.getUniqueId());
        return hidden != null && hidden.contains(target);
    }

    public boolean isHidden(Player observer, Entity target) {
        if (observer == null || target == null) {
            return false;
        }

        Set<Integer> hidden = hiddenEntities.get(observer.getUniqueId());
        return hidden != null && hidden.contains(target.getEntityId());
    }

    public boolean isStripped(Player observer, int target) {
        if (observer == null) {
            return false;
        }

        Set<Integer> stripped = strippedEntities.get(observer.getUniqueId());
        return stripped != null && stripped.contains(target);
    }

    public boolean isStripped(Player observer, Entity target) {
        if (observer == null || target == null) {
            return false;
        }

        Set<Integer> stripped = strippedEntities.get(observer.getUniqueId());
        return stripped != null && stripped.contains(target.getEntityId());
    }

    public void removePlayer(UUID uuid) {
        if (uuid != null) {
            hiddenEntities.remove(uuid);
            strippedEntities.remove(uuid);
        }
    }

    public void removePlayer(Player player) {
        if (player != null) {
            removePlayer(player.getUniqueId());
        }
    }

    public void removeEntity(int entityId) {
        for (Set<Integer> set : hiddenEntities.values()) {
            set.remove(entityId);
        }
        for (Set<Integer> set : strippedEntities.values()) {
            set.remove(entityId);
        }
    }

    public void removeEntity(Entity entity) {
        if (entity != null) {
            removeEntity(entity.getEntityId());
        }
    }

    public void clear() {
        hiddenEntities.clear();
        strippedEntities.clear();
    }

    public void restoreAll() {
        Config config = MAntiESP.getInstance().getConfiguration();

        for (UUID uuid : VisibilityListener.CACHED_PLAYERS) {
            Player observer = Bukkit.getPlayer(uuid);
            if (observer == null) continue;

            Set<Integer> hidden = hiddenEntities.remove(observer.getUniqueId());
            Set<Integer> stripped = strippedEntities.remove(observer.getUniqueId());

            if ((hidden == null || hidden.isEmpty()) && (stripped == null || stripped.isEmpty())) {
                continue;
            }

            double maxDistance = config.getMaxDistance() + (config.getF5().isEnabled() ? config.getF5().getDistance() : 0.0);
            double maxDistSq = maxDistance * maxDistance;

            if (config.isOnlyPlayer()) {
                for (UUID targetUuid : VisibilityListener.CACHED_PLAYERS) {
                    if (uuid.equals(targetUuid)) continue;

                    Player target = Bukkit.getPlayer(targetUuid);
                    if (target == null || !observer.getWorld().equals(target.getWorld())) continue;

                    if (observer.getLocation().distanceSquared(target.getLocation()) <= maxDistSq) {
                        int entityId = target.getEntityId();
                        if (hidden != null && hidden.contains(entityId)) {
                            PacketSender.sendAllSpawnPackets(observer, target);
                        } else if (stripped != null && stripped.contains(entityId)) {
                            PacketSender.sendMetadataPacket(observer, target);
                            PacketSender.sendEquipmentPacket(observer, target);
                            PacketSender.sendPotionEffects(observer, target);
                        }
                    }
                }
            } else {
                if (hidden != null && !hidden.isEmpty()) {
                    for (Entity entity : observer.getNearbyEntities(maxDistance, maxDistance, maxDistance)) {
                        if (hidden.contains(entity.getEntityId())) {
                            PacketSender.sendAllSpawnPackets(observer, entity);
                        }
                    }
                }

                if (stripped != null && !stripped.isEmpty()) {
                    for (Entity entity : observer.getNearbyEntities(maxDistance, maxDistance, maxDistance)) {
                        if (stripped.contains(entity.getEntityId())) {
                            PacketSender.sendMetadataPacket(observer, entity);
                            PacketSender.sendEquipmentPacket(observer, entity);
                            PacketSender.sendPotionEffects(observer, entity);
                        }
                    }
                }
            }
        }
        hiddenEntities.clear();
        strippedEntities.clear();
    }
}
