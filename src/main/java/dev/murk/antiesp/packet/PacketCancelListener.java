package dev.murk.antiesp.packet;

import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.server.*;
import dev.murk.antiesp.MAntiESP;
import dev.murk.antiesp.config.Config;
import dev.murk.antiesp.visibility.VisibilityManager;
import dev.murk.antiesp.visibility.VisibilityService;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.UUID;

public class PacketCancelListener extends PacketListenerAbstract {
    private final MAntiESP plugin;
    private final VisibilityManager visibilityManager;
    private final VisibilityService visibilityService;
    private final Config config;

    public PacketCancelListener(MAntiESP plugin) {
        super(PacketListenerPriority.LOW);
        this.plugin = plugin;
        this.visibilityManager = plugin.getVisibilityManager();
        this.visibilityService = plugin.getVisibilityService();
        this.config = plugin.getConfiguration();
    }

    @Override
    public void onPacketSend(PacketSendEvent event) {
        Player observer = event.getPlayer();
        if (observer == null) {
            return;
        }

        if (checkBeforeSpawnPacket(event, observer)) {
            return;
        }

        if (!visibilityManager.hasHiddenEntities(observer) && !visibilityManager.hasStrippedEntities(observer)) {
            return;
        }

        if (event.getPacketType() == PacketType.Play.Server.SET_PASSENGERS) {
            WrapperPlayServerSetPassengers packet = new WrapperPlayServerSetPassengers(event);
            if (visibilityManager.isHidden(observer, packet.getEntityId())) {
                event.setCancelled(true);
                return;
            }

            for (int passengerId : packet.getPassengers()) {
                if (visibilityManager.isHidden(observer, passengerId)) {
                    event.setCancelled(true);
                    return;
                }
            }

            return;
        }

        if (event.getPacketType() == PacketType.Play.Server.ATTACH_ENTITY) {
            WrapperPlayServerAttachEntity packet = new WrapperPlayServerAttachEntity(event);
            if (visibilityManager.isHidden(observer, packet.getAttachedId()) || visibilityManager.isHidden(observer, packet.getHoldingId())) {
                event.setCancelled(true);
                return;
            }

            return;
        }

        if (event.getPacketType() == PacketType.Play.Server.COLLECT_ITEM) {
            WrapperPlayServerCollectItem packet = new WrapperPlayServerCollectItem(event);
            if (visibilityManager.isHidden(observer, packet.getCollectorEntityId()) || visibilityManager.isHidden(observer, packet.getCollectedEntityId())) {
                event.setCancelled(true);
                return;
            }

            return;
        }

        int entityId = extractEntityId(event);
        if (entityId != -1) {
            if (visibilityManager.isHidden(observer, entityId)) {
                if (event.getPacketType() == PacketType.Play.Server.ENTITY_METADATA || event.getPacketType() == PacketType.Play.Server.ENTITY_EFFECT) {
                    Player target = findOnlinePlayer(null, entityId);
                    if (target != null && visibilityService.canSee(observer, target)) {
                        Player finalTarget = target;
                        Bukkit.getScheduler().runTask(plugin, () -> plugin.getVisibilityListener().updateVisibility(observer, finalTarget));
                    }
                }
                event.setCancelled(true);
                return;
            }

            if (visibilityManager.isStripped(observer, entityId)) {
                if (event.getPacketType() == PacketType.Play.Server.ENTITY_EQUIPMENT) {
                    WrapperPlayServerEntityEquipment eqPacket = new WrapperPlayServerEntityEquipment(event);
                    boolean allEmpty = true;
                    for (var eq : eqPacket.getEquipment()) {
                        if (eq.getItem() != null && !eq.getItem().isEmpty()) {
                            allEmpty = false;
                            break;
                        }
                    }
                    if (!allEmpty) {
                        event.setCancelled(true);
                    }
                }
            }
        }
    }

    private boolean checkBeforeSpawnPacket(PacketSendEvent event, Player observer) {
        var type = event.getPacketType();

        if (type == PacketType.Play.Server.SPAWN_ENTITY) {
            WrapperPlayServerSpawnEntity packet = new WrapperPlayServerSpawnEntity(event);
            int entityId = packet.getEntityId();
            if (visibilityManager.isHidden(observer, entityId)) {
                event.setCancelled(true);
                return true;
            }

            var entityType = packet.getEntityType();
            UUID uuid = packet.getUUID().orElse(null);
            Player targetPlayer = findOnlinePlayer(uuid, entityId);

            boolean isPlayer = entityType == EntityTypes.PLAYER || targetPlayer != null;
            double height = isPlayer ? 1.8 : 1.5;

            if (!isPlayer && config.isOnlyPlayer()) {
                return true;
            }

            boolean hasNametag = false;
            if (!config.getHide().isIgnoreNametag() && targetPlayer != null) {
                hasNametag = visibilityService.canSeeNametag(observer, targetPlayer);
            }

            boolean canSee;
            if (targetPlayer != null) {
                canSee = visibilityService.canSee(observer, targetPlayer);
            } else {
                var pos = packet.getPosition();
                canSee = visibilityService.canSee(observer, pos.getX(), pos.getY(), pos.getZ(), height);
            }

            if (!canSee) {
                if (hasNametag) {
                    visibilityManager.addStripped(observer, entityId);
                } else {
                    event.setCancelled(true);
                    visibilityManager.addHidden(observer, entityId);
                }
            }
            return true;
        }

        if (type == PacketType.Play.Server.SPAWN_PLAYER) {
            WrapperPlayServerSpawnPlayer packet = new WrapperPlayServerSpawnPlayer(event);
            int entityId = packet.getEntityId();
            if (visibilityManager.isHidden(observer, entityId)) {
                event.setCancelled(true);
                return true;
            }

            Player targetPlayer = findOnlinePlayer(packet.getUUID(), entityId);

            boolean hasNametag = false;
            if (!config.getHide().isIgnoreNametag() && targetPlayer != null) {
                hasNametag = visibilityService.canSeeNametag(observer, targetPlayer);
            }

            boolean canSee;
            if (targetPlayer != null) {
                canSee = visibilityService.canSee(observer, targetPlayer);
            } else {
                var pos = packet.getPosition();
                canSee = visibilityService.canSee(observer, pos.getX(), pos.getY(), pos.getZ(), 1.8);
            }

            if (!canSee) {
                if (hasNametag) {
                    visibilityManager.addStripped(observer, entityId);
                } else {
                    event.setCancelled(true);
                    visibilityManager.addHidden(observer, entityId);
                }
            }
            return true;
        }

        if (type == PacketType.Play.Server.SPAWN_LIVING_ENTITY) {
            WrapperPlayServerSpawnLivingEntity packet = new WrapperPlayServerSpawnLivingEntity(event);
            int entityId = packet.getEntityId();
            if (visibilityManager.isHidden(observer, entityId)) {
                event.setCancelled(true);
                return true;
            }

            if (config.isOnlyPlayer()) {
                return true;
            }

            Player targetPlayer = findOnlinePlayer(null, entityId);

            boolean hasNametag = false;
            if (!config.getHide().isIgnoreNametag() && targetPlayer != null) {
                hasNametag = visibilityService.canSeeNametag(observer, targetPlayer);
            }

            boolean canSee;
            if (targetPlayer != null) {
                canSee = visibilityService.canSee(observer, targetPlayer);
            } else {
                var pos = packet.getPosition();
                canSee = visibilityService.canSee(observer, pos.getX(), pos.getY(), pos.getZ(), 1.5);
            }

            if (!canSee) {
                if (hasNametag) {
                    visibilityManager.addStripped(observer, entityId);
                } else {
                    event.setCancelled(true);
                    visibilityManager.addHidden(observer, entityId);
                }
            }
            return true;
        }

        return false;
    }

    private Player findOnlinePlayer(UUID uuid, int entityId) {
        if (uuid != null) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                return player;
            }
        }
        if (entityId != -1) {
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (player.getEntityId() == entityId) {
                    return player;
                }
            }
        }
        return null;
    }

    private int extractEntityId(PacketSendEvent event) {
        var type = event.getPacketType();

        if (type == PacketType.Play.Server.ENTITY_RELATIVE_MOVE) {
            return new WrapperPlayServerEntityRelativeMove(event).getEntityId();
        } else if (type == PacketType.Play.Server.ENTITY_RELATIVE_MOVE_AND_ROTATION) {
            return new WrapperPlayServerEntityRelativeMoveAndRotation(event).getEntityId();
        } else if (type == PacketType.Play.Server.ENTITY_ROTATION) {
            return new WrapperPlayServerEntityRotation(event).getEntityId();
        } else if (type == PacketType.Play.Server.ENTITY_TELEPORT) {
            return new WrapperPlayServerEntityTeleport(event).getEntityId();
        } else if (type == PacketType.Play.Server.ENTITY_VELOCITY) {
            return new WrapperPlayServerEntityVelocity(event).getEntityId();
        } else if (type == PacketType.Play.Server.ENTITY_HEAD_LOOK) {
            return new WrapperPlayServerEntityHeadLook(event).getEntityId();
        } else if (type == PacketType.Play.Server.ENTITY_ANIMATION) {
            return new WrapperPlayServerEntityAnimation(event).getEntityId();
        } else if (type == PacketType.Play.Server.ENTITY_METADATA) {
            return new WrapperPlayServerEntityMetadata(event).getEntityId();
        } else if (type == PacketType.Play.Server.ENTITY_EQUIPMENT) {
            return new WrapperPlayServerEntityEquipment(event).getEntityId();
        } else if (type == PacketType.Play.Server.ENTITY_STATUS) {
            return new WrapperPlayServerEntityStatus(event).getEntityId();
        } else if (type == PacketType.Play.Server.ENTITY_EFFECT) {
            return new WrapperPlayServerEntityEffect(event).getEntityId();
        } else if (type == PacketType.Play.Server.REMOVE_ENTITY_EFFECT) {
            return new WrapperPlayServerRemoveEntityEffect(event).getEntityId();
        } else if (type == PacketType.Play.Server.UPDATE_ATTRIBUTES) {
            return new WrapperPlayServerUpdateAttributes(event).getEntityId();
        } else if (type == PacketType.Play.Server.HURT_ANIMATION) {
            return new WrapperPlayServerHurtAnimation(event).getEntityId();
        } else if (type == PacketType.Play.Server.DAMAGE_EVENT) {
            return new WrapperPlayServerDamageEvent(event).getEntityId();
        } else if (type == PacketType.Play.Server.ENTITY_SOUND_EFFECT) {
            return new WrapperPlayServerEntitySoundEffect(event).getEntityId();
        } else if (type == PacketType.Play.Server.ENTITY_POSITION_SYNC) {
            return new WrapperPlayServerEntityPositionSync(event).getId();
        }

        return -1;
    }
}