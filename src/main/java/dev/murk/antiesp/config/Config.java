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
    private final F5Settings f5 = new F5Settings();
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
        for (String matEntry : materials) {
            if (matEntry == null) continue;

            String pattern = matEntry.trim();
            if (pattern.isEmpty()) continue;

            for (Material material : Material.values()) {
                if (!material.isBlock()) continue;

                if (matchesPattern(material.name(), pattern)) {
                    transparentBlocks.add(material);
                }
            }
        }

        hide.setIgnoreNametag(fileConfig.getBoolean("hide.ignore-nametag", fileConfig.getBoolean("hide.ignoreNametag", false)));
        hide.setIgnoreSpectator(fileConfig.getBoolean("hide.ignore-spectator", true));
        hide.setIgnoreGlowing(fileConfig.getBoolean("hide.ignore-glowing", false));
        hide.setBlindness(fileConfig.getBoolean("hide.blindness", true));
        hide.setBlindnessDistance(Math.max(0.0, fileConfig.getDouble("hide.blindness-distance", 5.0)));
        hide.setInLava(fileConfig.getBoolean("hide.in-lava", true));
        hide.setLavaDistance(Math.max(0.0, fileConfig.getDouble("hide.lava-distance", 5.0)));

        if (fileConfig.isConfigurationSection("f5")) {
            f5.setEnabled(fileConfig.getBoolean("f5.enabled", true));
            f5.setDistance(Math.max(0.0, fileConfig.getDouble("f5.distance", 4.0)));
            f5.setCollisionOffset(Math.max(0.0, fileConfig.getDouble("f5.collision-offset", fileConfig.getDouble("f5.collisionOffset", 0.1))));
            f5.setFrontView(fileConfig.getBoolean("f5.front-view", fileConfig.getBoolean("f5.frontView", true)));
        } else {
            f5.setEnabled(fileConfig.getBoolean("f5", true));
            f5.setDistance(Math.max(0.0, fileConfig.getDouble("f5-distance", 4.0)));
            f5.setCollisionOffset(Math.max(0.0, fileConfig.getDouble("f5-collision-offset", 0.1)));
            f5.setFrontView(fileConfig.getBoolean("f5-front-view", true));
        }

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

    public static boolean matchesPattern(String name, String pattern) {
        if (name == null || pattern == null) return false;

        String upperName = name.toUpperCase(Locale.ROOT);
        String upperPattern = pattern.trim().toUpperCase(Locale.ROOT);

        if (upperName.equals(upperPattern)) return true;

        if (upperPattern.contains("*")) {
            String regex = upperPattern.replace(".", "\\.").replace("*", ".*");
            return upperName.matches(regex);
        }

        if (upperName.contains(upperPattern)) {
            String[] parts = upperName.split("_");
            for (String part : parts) {
                if (part.equals(upperPattern) || part.endsWith(upperPattern))
                    return true;
            }

            if (upperPattern.contains("_"))
                return upperName.contains(upperPattern);
        }

        return false;
    }

    @Getter
    @Setter
    public static final class HideSettings {
        private boolean ignoreNametag = false;
        private boolean ignoreSpectator = true;
        private boolean ignoreGlowing = false;
        private boolean blindness = true;
        private double blindnessDistance = 5.0;
        private double blindnessDistanceSquared = 25.0;
        private boolean inLava = true;
        private double lavaDistance = 5.0;
        private double lavaDistanceSquared = 25.0;

        public void setBlindnessDistance(double dist) {
            this.blindnessDistance = dist;
            this.blindnessDistanceSquared = dist * dist;
        }

        public void setLavaDistance(double dist) {
            this.lavaDistance = dist;
            this.lavaDistanceSquared = dist * dist;
        }
    }

    @Getter
    @Setter
    public static final class F5Settings {
        private boolean enabled = true;
        private double distance = 4.0;
        private double collisionOffset = 0.1;
        private boolean frontView = true;
    }
}
