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

public class PacketCancelListener extends PacketListenerAbstract {
    private final VisibilityManager visibilityManager;
    private final VisibilityService visibilityService;
    private final Config config;

    public PacketCancelListener(MAntiESP plugin) {
        super(PacketListenerPriority.LOW);
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
            double height = 1.5;
            boolean hasNametag = false;
            if (entityType == EntityTypes.PLAYER) {
                height = 1.8;
                if (!config.getHide().isIgnoreNametag()) {
                    var uuidOpt = packet.getUUID();
                    if (uuidOpt.isPresent()) {
                        Player targetPlayer = Bukkit.getPlayer(uuidOpt.get());
                        if (targetPlayer != null) {
                            hasNametag = visibilityService.canSeeNametag(observer, targetPlayer);
                        }
                    }
                }
            } else if (config.isOnlyPlayer()) {
                return true;
            }

            var pos = packet.getPosition();
            boolean canSee = visibilityService.canSee(observer, pos.getX(), pos.getY(), pos.getZ(), height);
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

            boolean hasNametag = false;
            if (!config.getHide().isIgnoreNametag()) {
                var uuid = packet.getUUID();
                Player targetPlayer = Bukkit.getPlayer(uuid);
                if (targetPlayer != null) {
                    hasNametag = visibilityService.canSeeNametag(observer, targetPlayer);
                }
            }

            var pos = packet.getPosition();
            boolean canSee = visibilityService.canSee(observer, pos.getX(), pos.getY(), pos.getZ(), 1.8);
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

            var pos = packet.getPosition();
            boolean canSee = visibilityService.canSee(observer, pos.getX(), pos.getY(), pos.getZ(), 1.5);
            if (!canSee) {
                event.setCancelled(true);
                visibilityManager.addHidden(observer, entityId);
            }
            return true;
        }

        return false;
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