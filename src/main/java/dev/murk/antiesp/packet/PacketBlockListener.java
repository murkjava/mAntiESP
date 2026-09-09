package dev.murk.antiesp.packet;

import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.util.Vector3i;
import com.github.retrooper.packetevents.wrapper.play.server.*;
import dev.murk.antiesp.cache.ChunkCacheManager;
import dev.murk.antiesp.config.Config;
import org.bukkit.entity.Player;

import java.util.UUID;

public final class PacketBlockListener extends PacketListenerAbstract {
    private final ChunkCacheManager cacheManager;
    private final Config config;

    public PacketBlockListener(ChunkCacheManager cacheManager, Config config) {
        super(PacketListenerPriority.MONITOR);
        this.cacheManager = cacheManager;
        this.config = config;
    }

    @Override
    public void onPacketSend(PacketSendEvent event) {
        Player observer = event.getPlayer();
        if (observer == null) {
            return;
        }

        if (config.isWorldDisabled(observer.getWorld().getName())) {
            return;
        }

        UUID worldId = observer.getWorld().getUID();

        if (event.getPacketType() == PacketType.Play.Server.BLOCK_CHANGE) {
            WrapperPlayServerBlockChange packet = new WrapperPlayServerBlockChange(event);
            Vector3i pos = packet.getBlockPosition();
            cacheManager.setBlock(worldId, pos.getX(), pos.getY(), pos.getZ(), packet.getBlockId());
            return;
        }

        if (event.getPacketType() == PacketType.Play.Server.MULTI_BLOCK_CHANGE) {
            WrapperPlayServerMultiBlockChange packet = new WrapperPlayServerMultiBlockChange(event);
            Vector3i sectionPos = packet.getChunkPosition();
            int sectionX = sectionPos.getX() << 4;
            int sectionY = sectionPos.getY() << 4;
            int sectionZ = sectionPos.getZ() << 4;
            for (WrapperPlayServerMultiBlockChange.EncodedBlock eb : packet.getBlocks()) {
                cacheManager.setBlock(worldId, sectionX + eb.getX(), sectionY + eb.getY(), sectionZ + eb.getZ(), eb.getBlockId());
            }
            return;
        }

        if (event.getPacketType() == PacketType.Play.Server.CHUNK_DATA) {
            WrapperPlayServerChunkData packet = new WrapperPlayServerChunkData(event);
            cacheManager.processColumn(worldId, packet.getColumn());
            return;
        }

        if (event.getPacketType() == PacketType.Play.Server.UNLOAD_CHUNK) {
            WrapperPlayServerUnloadChunk packet = new WrapperPlayServerUnloadChunk(event);
            cacheManager.unloadChunk(worldId, packet.getChunkX(), packet.getChunkZ());
            return;
        }

        if (event.getPacketType() == PacketType.Play.Server.EXPLOSION) {
            WrapperPlayServerExplosion packet = new WrapperPlayServerExplosion(event);
            int originX = (int) Math.floor(packet.getPosition().getX());
            int originY = (int) Math.floor(packet.getPosition().getY());
            int originZ = (int) Math.floor(packet.getPosition().getZ());
            if (packet.getRecords() != null) {
                for (Vector3i record : packet.getRecords()) {
                    cacheManager.removeBlock(worldId, originX + record.getX(), originY + record.getY(), originZ + record.getZ());
                }
            }
            return;
        }

        if (event.getPacketType() == PacketType.Play.Server.ACKNOWLEDGE_PLAYER_DIGGING) {
            WrapperPlayServerAcknowledgePlayerDigging packet = new WrapperPlayServerAcknowledgePlayerDigging(event);
            if (packet.isSuccessful()) {
                Vector3i pos = packet.getBlockPosition();
                cacheManager.setBlock(worldId, pos.getX(), pos.getY(), pos.getZ(), packet.getBlockId());
            }
        }
    }
}
