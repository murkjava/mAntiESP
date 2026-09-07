package dev.murk.antiesp.listener;

import dev.murk.antiesp.config.Config;
import dev.murk.antiesp.cache.ChunkCacheManager;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.ChunkSnapshot;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.event.world.WorldUnloadEvent;
import org.bukkit.plugin.Plugin;

import java.util.UUID;

public final class ChunkEventListener implements Listener {
    private final Plugin plugin;
    private final ChunkCacheManager cacheManager;
    private final Config config;

    public ChunkEventListener(Plugin plugin, ChunkCacheManager cacheManager, Config config) {
        this.plugin = plugin;
        this.cacheManager = cacheManager;
        this.config = config;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onChunkLoad(ChunkLoadEvent event) {
        if (config.isWorldDisabled(event.getWorld().getName())) {
            return;
        }

        Chunk chunk = event.getChunk();
        UUID worldId = event.getWorld().getUID();
        ChunkSnapshot snapshot = chunk.getChunkSnapshot(false, false, false);

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            cacheManager.processChunkSnapshot(worldId, snapshot);
        });
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onChunkUnload(ChunkUnloadEvent event) {
        if (config.isWorldDisabled(event.getWorld().getName())) {
            return;
        }

        Chunk chunk = event.getChunk();
        cacheManager.unloadChunk(event.getWorld().getUID(), chunk.getX(), chunk.getZ());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onWorldUnload(WorldUnloadEvent event) {
        cacheManager.unloadWorld(event.getWorld().getUID());
    }
}
