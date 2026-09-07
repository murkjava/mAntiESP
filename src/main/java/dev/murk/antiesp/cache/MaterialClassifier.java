package dev.murk.antiesp.cache;

import org.bukkit.Material;

import java.util.Set;

public final class MaterialClassifier {
    private static final boolean[] OCCLUDING = new boolean[Material.values().length];

    static {
        initDefault();
    }

    private MaterialClassifier() {
    }

    private static void initDefault() {
        for (Material material : Material.values()) {
            if (!material.isBlock()) {
                continue;
            }

            String name = material.name();

            if (name.contains("LEAVES")
                    || name.contains("GLASS")
                    || name.contains("BARRIER")
                    || name.contains("SLAB")
                    || name.contains("STAIRS")
                    || name.contains("CARPET")
                    || name.contains("FENCE")
                    || name.contains("WALL")
                    || name.contains("GATE")
                    || name.contains("DOOR")
                    || name.contains("TRAPDOOR")
                    || name.contains("BED")
                    || name.contains("CHEST")
                    || name.contains("SHULKER")
                    || name.contains("ANVIL")
                    || name.contains("HOPPER")
                    || name.contains("CAULDRON")
                    || name.contains("LANTERN")
                    || name.contains("CHAIN")
                    || name.contains("BARS")
                    || name.contains("END_ROD")
                    || name.contains("DAYLIGHT")
                    || name.contains("LECTERN")
                    || name.contains("BELL")
                    || name.contains("CAMPFIRE")
                    || name.contains("GRINDSTONE")
                    || name.contains("STONECUTTER")
                    || name.contains("ENCHANTING")
                    || name.contains("BREWING")
                    || name.contains("POT")
                    || name.contains("SCAFFOLDING")
                    || name.contains("POINTED_DRIPSTONE")
                    || name.contains("AMETHYST")
                    || name.contains("SKULL")
                    || name.contains("HEAD")
                    || name.contains("BANNER")
                    || name.contains("SIGN")
                    || name.contains("ROD")
                    || name.contains("CANDLE")) {
                continue;
            }

            if (material.isOccluding()) {
                OCCLUDING[material.ordinal()] = true;
            }
        }
    }

    public static void applyTransparentBlocks(Set<Material> transparentBlocks) {
        initDefault();
        if (transparentBlocks == null) {
            return;
        }
        for (Material material : transparentBlocks) {
            if (material == null) {
                continue;
            }
            int ordinal = material.ordinal();
            if (ordinal >= 0 && ordinal < OCCLUDING.length) {
                OCCLUDING[ordinal] = false;
            }
        }
    }

    public static boolean isOccluding(Material material) {
        if (material == null) {
            return false;
        }
        int ordinal = material.ordinal();
        if (ordinal < 0 || ordinal >= OCCLUDING.length) {
            return false;
        }
        return OCCLUDING[ordinal];
    }
}
