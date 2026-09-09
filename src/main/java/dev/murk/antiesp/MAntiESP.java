package dev.murk.antiesp;

import com.github.retrooper.packetevents.PacketEvents;
import dev.murk.antiesp.cache.ChunkCacheManager;
import dev.murk.antiesp.cache.MaterialClassifier;
import dev.murk.antiesp.command.AntiESPCommand;
import dev.murk.antiesp.config.Config;
import dev.murk.antiesp.listener.BlockEventListener;
import dev.murk.antiesp.listener.ChunkEventListener;
import dev.murk.antiesp.listener.VisibilityListener;
import dev.murk.antiesp.packet.PacketCancelListener;
import dev.murk.antiesp.visibility.VisibilityManager;
import dev.murk.antiesp.visibility.VisibilityService;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.ChunkSnapshot;
import org.bukkit.World;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

@Getter
public final class MAntiESP extends JavaPlugin {
    @Getter
    private static MAntiESP instance;
    private Config configuration;
    private ChunkCacheManager chunkCacheManager;
    private VisibilityManager visibilityManager;
    private VisibilityService visibilityService;
    private VisibilityListener visibilityListener;

    private PacketCancelListener packetCancel;

    @Override
    public void onEnable() {
        instance = this;

        saveDefaultConfig();
        reloadConfig();
        configuration = new Config();
        configuration.load(getConfig());
        MaterialClassifier.applyTransparentBlocks(configuration.getTransparentBlocks());

        chunkCacheManager = new ChunkCacheManager();
        visibilityManager = new VisibilityManager();
        visibilityService = new VisibilityService(chunkCacheManager, configuration);

        PluginManager pm = Bukkit.getPluginManager();
        pm.registerEvents(new ChunkEventListener(this, chunkCacheManager, configuration), this);
        pm.registerEvents(new BlockEventListener(chunkCacheManager, configuration), this);

        visibilityListener = new VisibilityListener(this, configuration);
        pm.registerEvents(visibilityListener, this);

        packetCancel = new PacketCancelListener(this);
        PacketEvents.getAPI().getEventManager().registerListener(packetCancel);

        PluginCommand cmd = getCommand("mantiesp");
        if (cmd != null) {
            AntiESPCommand executor = new AntiESPCommand(this);
            cmd.setExecutor(executor);
            cmd.setTabCompleter(executor);
        }

        Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
            for (World world : Bukkit.getWorlds()) {
                if (configuration.isWorldDisabled(world.getName())) {
                    continue;
                }
                for (Chunk chunk : world.getLoadedChunks()) {
                    ChunkSnapshot snapshot = chunk.getChunkSnapshot(false, false, false);
                    chunkCacheManager.processChunkSnapshot(world.getUID(), snapshot);
                }
            }
        });
    }

    public void reload() {
        onDisable();
        onEnable();
    }

    @Override
    public void onDisable() {
        if (packetCancel != null) {
            PacketEvents.getAPI().getEventManager().unregisterListener(packetCancel);
            packetCancel = null;
        }

        if (visibilityManager != null) {
            visibilityManager.restoreAll();
            visibilityManager = null;
        }

        if (visibilityListener != null) {
            visibilityListener.cancelTask();
            visibilityListener = null;
        }

        if (chunkCacheManager != null) {
            chunkCacheManager.clear();
            chunkCacheManager = null;
        }

        if (visibilityService != null) {
            visibilityService.lastLocations().clear();
            visibilityService.velocities().clear();
            visibilityService = null;
        }

        instance = null;
    }
}
