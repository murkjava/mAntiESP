package dev.murk.antiesp.listener;

import dev.murk.antiesp.config.Config;
import dev.murk.antiesp.cache.ChunkCacheManager;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockFadeEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityExplodeEvent;

import java.util.UUID;

public final class BlockEventListener implements Listener {
    private final ChunkCacheManager cacheManager;
    private final Config config;

    public BlockEventListener(ChunkCacheManager cacheManager, Config config) {
        this.cacheManager = cacheManager;
        this.config = config;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        if (config.isWorldDisabled(block.getWorld().getName())) {
            return;
        }

        cacheManager.removeBlock(block.getWorld().getUID(), block.getX(), block.getY(), block.getZ());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        Block block = event.getBlock();
        if (config.isWorldDisabled(block.getWorld().getName())) {
            return;
        }

        cacheManager.setBlock(block.getWorld().getUID(), block.getX(), block.getY(), block.getZ(), block.getBlockData());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBurn(BlockBurnEvent event) {
        Block block = event.getBlock();
        if (config.isWorldDisabled(block.getWorld().getName())) {
            return;
        }

        cacheManager.removeBlock(block.getWorld().getUID(), block.getX(), block.getY(), block.getZ());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockFade(BlockFadeEvent event) {
        Block block = event.getBlock();
        if (config.isWorldDisabled(block.getWorld().getName())) {
            return;
        }

        cacheManager.setBlock(block.getWorld().getUID(), block.getX(), block.getY(), block.getZ(), event.getNewState().getBlockData());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockExplode(BlockExplodeEvent event) {
        if (config.isWorldDisabled(event.getBlock().getWorld().getName())) {
            return;
        }

        UUID uuid = event.getBlock().getWorld().getUID();
        for (Block block : event.blockList()) {
            cacheManager.removeBlock(uuid, block.getX(), block.getY(), block.getZ());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent event) {
        if (config.isWorldDisabled(event.getLocation().getWorld().getName())) {
            return;
        }

        UUID uuid = event.getLocation().getWorld().getUID();
        for (Block block : event.blockList()) {
            cacheManager.removeBlock(uuid, block.getX(), block.getY(), block.getZ());
        }
    }
}
