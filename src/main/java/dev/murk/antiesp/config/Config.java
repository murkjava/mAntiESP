package dev.murk.antiesp.config;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Getter
public final class Config {
    private int ticksPeriod = 3;
    private boolean onlyPlayer = true;
    private boolean hideAllEntities = false;
    private final Set<EntityType> targetEntities = new HashSet<>();
    private double minDistance = 3.0;
    private double minDistanceSquared = 9.0;
    private double maxDistance = 64.0;
    private double maxDistanceSquared = 4096.0;
    private final Set<String> disabledWorlds = new HashSet<>();
    private final Set<Material> transparentBlocks = new HashSet<>();
    private final HideSettings hide = new HideSettings();
    private double hitboxExpansionX = 0.25;
    private double hitboxExpansionY = 0.2;
    private double hitboxExpansionZ = 0.25;
    private boolean predictMovement = true;
    private double predictionMultiplier = 1.5;

    public void load(FileConfiguration fileConfig) {
        ticksPeriod = Math.max(1, fileConfig.getInt("ticks-period", 3));
        onlyPlayer = fileConfig.getBoolean("only-player", true);

        targetEntities.clear();
        hideAllEntities = false;
        List<String> entities = fileConfig.getStringList("entities-list");
        for (String entry : entities) {
            if (entry == null) continue;

            String trimmed = entry.trim();
            if ("all".equalsIgnoreCase(trimmed)) {
                hideAllEntities = true;
                continue;
            }

            try {
                EntityType type = EntityType.valueOf(trimmed.toUpperCase(Locale.ROOT));
                targetEntities.add(type);
            } catch (IllegalArgumentException ignored) {}
        }

        minDistance = fileConfig.getDouble("min-distance", 3.0);
        minDistanceSquared = minDistance * minDistance;

        maxDistance = fileConfig.getDouble("max-distance", 64.0);
        maxDistanceSquared = maxDistance * maxDistance;

        disabledWorlds.clear();
        List<String> worlds = fileConfig.getStringList("disabled-worlds");
        for (String world : worlds) {
            if (world != null) {
                disabledWorlds.add(world.toLowerCase(Locale.ROOT));
            }
        }

        transparentBlocks.clear();
        List<String> materials = fileConfig.getStringList("transparent-blocks");
        for (String matName : materials) {
            if (matName == null) continue;

            Material material = Material.matchMaterial(matName.trim());
            if (material != null) {
                transparentBlocks.add(material);
            }
        }

        hide.setIgnoreNametag(fileConfig.getBoolean("hide.ignore-nametag", fileConfig.getBoolean("hide.ignoreNametag", false)));
        hide.setIgnoreSpectator(fileConfig.getBoolean("hide.ignore-spectator", true));
        hide.setIgnoreGlowing(fileConfig.getBoolean("hide.ignore-glowing", false));
        hide.setBlindness(fileConfig.getBoolean("hide.blindness", true));
        hide.setInLava(fileConfig.getBoolean("hide.in-lava", true));

        if (fileConfig.isConfigurationSection("hitbox-expansion")) {
            hitboxExpansionX = Math.max(0.0, fileConfig.getDouble("hitbox-expansion.x", 0.25));
            hitboxExpansionY = Math.max(0.0, fileConfig.getDouble("hitbox-expansion.y", 0.2));
            hitboxExpansionZ = Math.max(0.0, fileConfig.getDouble("hitbox-expansion.z", 0.25));
        } else {
            double uniform = Math.max(0.0, fileConfig.getDouble("hitbox-expansion", 0.25));
            hitboxExpansionX = uniform;
            hitboxExpansionY = uniform;
            hitboxExpansionZ = uniform;
        }

        if (fileConfig.isConfigurationSection("movement-prediction")) {
            predictMovement = fileConfig.getBoolean("movement-prediction.enabled", true);
            predictionMultiplier = Math.max(0.0, fileConfig.getDouble("movement-prediction.multiplier", 1.5));
        } else {
            predictMovement = fileConfig.getBoolean("movement-prediction", fileConfig.getBoolean("predict-movement", true));
            predictionMultiplier = Math.max(0.0, fileConfig.getDouble("movement-prediction-multiplier", fileConfig.getDouble("prediction-multiplier", 1.5)));
        }
    }

    public boolean shouldCheckEntity(EntityType entityType) {
        if (entityType == null) {
            return false;
        }
        if (onlyPlayer) {
            return entityType == EntityType.PLAYER;
        }
        if (hideAllEntities) {
            return true;
        }
        return targetEntities.contains(entityType);
    }

    public boolean shouldCheckEntity(Entity entity) {
        if (entity == null) {
            return false;
        }
        return shouldCheckEntity(entity.getType());
    }

    public boolean isWorldDisabled(String worldName) {
        if (worldName == null) {
            return false;
        }
        return disabledWorlds.contains(worldName.toLowerCase(Locale.ROOT));
    }

    @Getter
    @Setter
    public static final class HideSettings {
        private boolean ignoreNametag = false;
        private boolean ignoreSpectator = true;
        private boolean ignoreGlowing = false;
        private boolean blindness = true;
        private boolean inLava = true;
    }
}
