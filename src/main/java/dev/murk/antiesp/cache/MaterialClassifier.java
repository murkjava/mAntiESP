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
                    || name.contains("BARRIER")) {
                continue;
            }

            if (material.isOccluding() || name.contains("SLAB") || name.contains("STAIRS")) {
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
