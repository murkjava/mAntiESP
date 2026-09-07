package dev.murk.antiesp.cache;

import org.bukkit.ChunkSnapshot;
import org.bukkit.Material;
import org.bukkit.block.data.BlockData;

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
                        BlockData data;
                        try {
                            data = snapshot.getBlockData(x, blockY, z);
                        } catch (Throwable ignored) {
                            continue;
                        }

                        if (data == null) {
                            continue;
                        }

                        if (MaterialClassifier.isFullOccluding(data)) {
                            if (bitSet == null) {
                                bitSet = new BitSet(4096);
                            }
                            int index = (y << 8) | (z << 4) | x;
                            bitSet.set(index);
                        } else {
                            BlockBox[] custom = MaterialClassifier.getCustomBoxes(data);
                            if (custom != null) {
                                occlusion.setCustomShape(x, blockY, z, custom);
                            }
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

    public boolean isBlocked(UUID worldId, int x, int y, int z,
                             double x0, double y0, double z0,
                             double x1, double y1, double z1) {
        Map<Long, ChunkOcclusion> cache = worldCaches.get(worldId);
        if (cache == null) {
            return false;
        }

        ChunkOcclusion chunk = cache.get(getChunkKey(x >> 4, z >> 4));
        if (chunk == null) {
            return false;
        }

        return chunk.isBlocked(x, y, z, x0, y0, z0, x1, y1, z1);
    }

    public void setBlock(UUID worldId, int x, int y, int z, BlockData data) {
        Map<Long, ChunkOcclusion> cache = worldCaches.computeIfAbsent(worldId, k -> new ConcurrentHashMap<>());
        ChunkOcclusion chunk = cache.computeIfAbsent(getChunkKey(x >> 4, z >> 4), k -> new ChunkOcclusion());

        if (data == null || data.getMaterial().isAir()) {
            chunk.removeBlock(x, y, z);
            return;
        }

        if (MaterialClassifier.isFullOccluding(data)) {
            chunk.setFullOccluding(x, y, z, true);
        } else {
            BlockBox[] custom = MaterialClassifier.getCustomBoxes(data);
            if (custom != null) {
                chunk.setCustomShape(x, y, z, custom);
            } else {
                chunk.removeBlock(x, y, z);
            }
        }
    }

    public void removeBlock(UUID worldId, int x, int y, int z) {
        Map<Long, ChunkOcclusion> cache = worldCaches.get(worldId);
        if (cache != null) {
            ChunkOcclusion chunk = cache.get(getChunkKey(x >> 4, z >> 4));
            if (chunk != null) {
                chunk.removeBlock(x, y, z);
            }
        }
    }

    public void setOccluding(UUID worldId, int x, int y, int z, boolean occluding) {
        Map<Long, ChunkOcclusion> cache = worldCaches.computeIfAbsent(worldId, k -> new ConcurrentHashMap<>());
        ChunkOcclusion chunk = cache.computeIfAbsent(getChunkKey(x >> 4, z >> 4), k -> new ChunkOcclusion());
        chunk.setFullOccluding(x, y, z, occluding);
    }

    public void clear() {
        worldCaches.clear();
    }
}
