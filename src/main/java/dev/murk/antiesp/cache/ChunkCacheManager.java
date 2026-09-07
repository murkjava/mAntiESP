package dev.murk.antiesp.cache;

import org.bukkit.ChunkSnapshot;
import org.bukkit.Material;

import java.util.BitSet;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ChunkCacheManager {
    private final Map<UUID, Map<Long, ChunkOcclusion>> worldCaches = new ConcurrentHashMap<>();

    public static long getChunkKey(int chunkX, int chunkZ) {
        return ((long) chunkX << 32) | (chunkZ & 0xFFFFFFFFL);
    }

    public void processChunkSnapshot(UUID worldId, ChunkSnapshot snapshot) {
        int chunkX = snapshot.getX();
        int chunkZ = snapshot.getZ();
        long chunkKey = getChunkKey(chunkX, chunkZ);

        ChunkOcclusion occlusion = new ChunkOcclusion();

        int minSection = 0;
        int maxSection = 16;
        try {
            snapshot.isSectionEmpty(-4);
            minSection = -4;
            maxSection = 20;
        } catch (Throwable ignored) {
        }

        for (int sectionY = minSection; sectionY < maxSection; sectionY++) {
            try {
                if (snapshot.isSectionEmpty(sectionY)) {
                    continue;
                }
            } catch (Throwable ignored) {
                continue;
            }

            BitSet bitSet = null;
            int baseY = sectionY << 4;

            for (int y = 0; y < 16; y++) {
                int blockY = baseY + y;
                for (int z = 0; z < 16; z++) {
                    for (int x = 0; x < 16; x++) {
                        Material material;
                        try {
                            material = snapshot.getBlockType(x, blockY, z);
                        } catch (Throwable ignored) {
                            continue;
                        }

                        if (MaterialClassifier.isOccluding(material)) {
                            if (bitSet == null) {
                                bitSet = new BitSet(4096);
                            }
                            int index = (y << 8) | (z << 4) | x;
                            bitSet.set(index);
                        }
                    }
                }
            }

            if (bitSet != null) {
                occlusion.setSection(sectionY, bitSet);
            }
        }

        Map<Long, ChunkOcclusion> cache = worldCaches.computeIfAbsent(worldId, k -> new ConcurrentHashMap<>());
        cache.put(chunkKey, occlusion);
    }

    public void unloadChunk(UUID worldId, int chunkX, int chunkZ) {
        Map<Long, ChunkOcclusion> cache = worldCaches.get(worldId);
        if (cache != null) {
            cache.remove(getChunkKey(chunkX, chunkZ));
        }
    }

    public void unloadWorld(UUID worldId) {
        worldCaches.remove(worldId);
    }

    public boolean isOccluding(UUID worldId, int x, int y, int z) {
        Map<Long, ChunkOcclusion> cache = worldCaches.get(worldId);
        if (cache == null) {
            return false;
        }

        ChunkOcclusion chunk = cache.get(getChunkKey(x >> 4, z >> 4));
        if (chunk == null) {
            return false;
        }

        return chunk.isOccluding(x, y, z);
    }

    public void setOccluding(UUID worldId, int x, int y, int z, boolean occluding) {
        Map<Long, ChunkOcclusion> cache = worldCaches.computeIfAbsent(worldId, k -> new ConcurrentHashMap<>());
        ChunkOcclusion chunk = cache.computeIfAbsent(getChunkKey(x >> 4, z >> 4), k -> new ChunkOcclusion());
        chunk.setOccluding(x, y, z, occluding);
    }

    public void clear() {
        worldCaches.clear();
    }
}
