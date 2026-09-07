package dev.murk.antiesp.packet;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.protocol.entity.data.EntityData;
import com.github.retrooper.packetevents.protocol.item.ItemStack;
import com.github.retrooper.packetevents.protocol.player.Equipment;
import com.github.retrooper.packetevents.protocol.player.EquipmentSlot;
import com.github.retrooper.packetevents.util.Vector3d;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityEquipment;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityMetadata;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityTeleport;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSpawnEntity;
import io.github.retrooper.packetevents.util.SpigotConversionUtil;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EntityEquipment;

import java.util.ArrayList;
import java.util.List;

public final class PacketSender {

    private PacketSender() {
    }

    public static void sendAllSpawnPackets(Player observer, Entity entity) {
        sendSpawnPacket(observer, entity);
        sendEntityTeleport(observer, entity);
        sendMetadataPacket(observer, entity);
        sendEquipmentPacket(observer, entity);
    }

    public static void sendSpawnPacket(Player observer, Entity entity) {
        WrapperPlayServerSpawnEntity packet = new WrapperPlayServerSpawnEntity(
                entity.getEntityId(),
                entity.getUniqueId(),
                SpigotConversionUtil.fromBukkitEntityType(entity.getType()),
                SpigotConversionUtil.fromBukkitLocation(entity.getLocation()),
                entity.getLocation().getYaw(),
                0,
                Vector3d.zero()
        );

        PacketEvents.getAPI().getPlayerManager().sendPacket(observer, packet);
    }

    public static void sendEntityTeleport(Player observer, Entity entity) {
        Location location = entity.getLocation();

        WrapperPlayServerEntityTeleport teleportPacket = new WrapperPlayServerEntityTeleport(
                entity.getEntityId(),
                SpigotConversionUtil.fromBukkitLocation(entity.getLocation()).getPosition(),
                location.getYaw(),
                location.getPitch(),
                entity.isOnGround()
        );

        PacketEvents.getAPI().getPlayerManager().sendPacket(observer, teleportPacket);
    }

    public static void sendMetadataPacket(Player observer, Entity entity) {
        if (!(entity instanceof LivingEntity)) {
            return;
        }

        List<EntityData<?>> metadata = SpigotConversionUtil.getEntityMetadata(entity);
        WrapperPlayServerEntityMetadata packet = new WrapperPlayServerEntityMetadata(entity.getEntityId(), metadata);
        PacketEvents.getAPI().getPlayerManager().sendPacket(observer, packet);
    }

    public static void sendEmptyEquipmentPacket(Player observer, int entityId) {
        List<Equipment> equipmentList = List.of(
                new Equipment(EquipmentSlot.MAIN_HAND, ItemStack.EMPTY),
                new Equipment(EquipmentSlot.OFF_HAND, ItemStack.EMPTY),
                new Equipment(EquipmentSlot.BOOTS, ItemStack.EMPTY),
                new Equipment(EquipmentSlot.LEGGINGS, ItemStack.EMPTY),
                new Equipment(EquipmentSlot.CHEST_PLATE, ItemStack.EMPTY),
                new Equipment(EquipmentSlot.HELMET, ItemStack.EMPTY)
        );
        WrapperPlayServerEntityEquipment packet = new WrapperPlayServerEntityEquipment(entityId, equipmentList);
        PacketEvents.getAPI().getPlayerManager().sendPacket(observer, packet);
    }

    public static void sendEquipmentPacket(Player observer, Entity entity) {
        if (!(entity instanceof LivingEntity living)) {
            return;
        }

        EntityEquipment eq = living.getEquipment();
        if (eq == null) {
            return;
        }

        List<Equipment> equipmentList = new ArrayList<>();
        addEquipmentIfPresent(equipmentList, EquipmentSlot.MAIN_HAND, eq.getItemInMainHand());
        addEquipmentIfPresent(equipmentList, EquipmentSlot.OFF_HAND, eq.getItemInOffHand());
        addEquipmentIfPresent(equipmentList, EquipmentSlot.HELMET, eq.getHelmet());
        addEquipmentIfPresent(equipmentList, EquipmentSlot.CHEST_PLATE, eq.getChestplate());
        addEquipmentIfPresent(equipmentList, EquipmentSlot.LEGGINGS, eq.getLeggings());
        addEquipmentIfPresent(equipmentList, EquipmentSlot.BOOTS, eq.getBoots());

        WrapperPlayServerEntityEquipment packet = new WrapperPlayServerEntityEquipment(entity.getEntityId(), equipmentList);
        PacketEvents.getAPI().getPlayerManager().sendPacket(observer, packet);
    }

    private static void addEquipmentIfPresent(List<Equipment> list, EquipmentSlot slot, org.bukkit.inventory.ItemStack bukkitItem) {
        if (bukkitItem != null && !bukkitItem.getType().isAir()) {
            list.add(new Equipment(slot, SpigotConversionUtil.fromBukkitItemStack(bukkitItem)));
        } else {
            list.add(new Equipment(slot, ItemStack.EMPTY));
        }
    }
}
