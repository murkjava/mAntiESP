package dev.murk.antiesp.cache;

import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Bisected;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.Bed;
import org.bukkit.block.data.type.Gate;
import org.bukkit.block.data.type.Slab;
import org.bukkit.block.data.type.Snow;
import org.bukkit.block.data.type.Stairs;
import org.bukkit.block.data.type.TrapDoor;

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

            if (name.contains("LEAVES") || name.contains("GLASS") || name.contains("BARRIER") || name.contains("SLAB") || name.contains("STAIRS")
                    || name.contains("CARPET") || name.contains("FENCE") || name.contains("WALL") || name.contains("GATE") || name.contains("DOOR")
                    || name.contains("TRAPDOOR") || name.contains("BED") || name.contains("CHEST") || name.contains("SHULKER") || name.contains("ANVIL")
                    || name.contains("HOPPER") || name.contains("CAULDRON") || name.contains("LANTERN") || name.contains("CHAIN") || name.contains("BARS")
                    || name.contains("END_ROD") || name.contains("DAYLIGHT") || name.contains("LECTERN") || name.contains("BELL") || name.contains("CAMPFIRE")
                    || name.contains("GRINDSTONE") || name.contains("STONECUTTER") || name.contains("ENCHANTING") || name.contains("BREWING") || name.contains("POT")
                    || name.contains("SCAFFOLDING") || name.contains("POINTED_DRIPSTONE") || name.contains("AMETHYST") || name.contains("SKULL")
                    || name.contains("HEAD") || name.contains("BANNER") || name.contains("SIGN") || name.contains("ROD") || name.contains("CANDLE")) {
                continue;
            }

            if (material.isOccluding()) {
                OCCLUDING[material.ordinal()] = true;
            }
        }
    }

    public static void applyTransparentBlocks(Set<Material> transparentBlocks) {
        initDefault();

        if (transparentBlocks == null) return;

        for (Material material : transparentBlocks) {
            if (material == null) continue;

            int ordinal = material.ordinal();
            if (ordinal < OCCLUDING.length) {
                OCCLUDING[ordinal] = false;
            }
        }
    }

    public static boolean isOccluding(Material material) {
        if (material == null) return false;

        int ordinal = material.ordinal();
        if (ordinal >= OCCLUDING.length) return false;

        return OCCLUDING[ordinal];
    }

    public static boolean isFullOccluding(BlockData data) {
        if (data == null) return false;

        if (data instanceof Slab slab) return slab.getType() == Slab.Type.DOUBLE && isOccluding(slab.getMaterial());

        return isOccluding(data.getMaterial());
    }

    public static BlockBox[] getCustomBoxes(BlockData data) {
        if (data == null) {
            return null;
        }

        if (data instanceof Slab slab) {
            if (slab.getType() == Slab.Type.BOTTOM) {
                return new BlockBox[]{BlockBox.SLAB_BOTTOM};
            } else if (slab.getType() == Slab.Type.TOP) {
                return new BlockBox[]{BlockBox.SLAB_TOP};
            }

            return null;
        }

        if (data instanceof Stairs stairs) {
            Bisected.Half half = stairs.getHalf();
            BlockFace facing = stairs.getFacing();
            BlockBox base = (half == Bisected.Half.BOTTOM) ? BlockBox.SLAB_BOTTOM : BlockBox.SLAB_TOP;
            BlockBox step;

            if (half == Bisected.Half.BOTTOM) {
                step = switch (facing) {
                    case NORTH -> new BlockBox(0, 0.5f, 0, 1, 1, 0.5f);
                    case SOUTH -> new BlockBox(0, 0.5f, 0.5f, 1, 1, 1);
                    case WEST -> new BlockBox(0, 0.5f, 0, 0.5f, 1, 1);
                    case EAST -> new BlockBox(0.5f, 0.5f, 0, 1, 1, 1);
                    default -> new BlockBox(0, 0.5f, 0, 1, 1, 0.5f);
                };
            } else {
                step = switch (facing) {
                    case NORTH -> new BlockBox(0, 0, 0, 1, 0.5f, 0.5f);
                    case SOUTH -> new BlockBox(0, 0, 0.5f, 1, 0.5f, 1);
                    case WEST -> new BlockBox(0, 0, 0, 0.5f, 0.5f, 1);
                    case EAST -> new BlockBox(0.5f, 0, 0, 1, 0.5f, 1);
                    default -> new BlockBox(0, 0, 0, 1, 0.5f, 0.5f);
                };
            }

            return new BlockBox[]{base, step};
        }

        if (data instanceof Snow snow) {
            float height = Math.min(1.0f, snow.getLayers() / 8.0f);
            return new BlockBox[]{new BlockBox(0, 0, 0, 1, height, 1)};
        }

        if (data.getMaterial().name().contains("CARPET")) {
            return new BlockBox[]{BlockBox.CARPET};
        }

        if (data instanceof TrapDoor trapdoor) {
            if (!trapdoor.isOpen()) {
                if (trapdoor.getHalf() == Bisected.Half.BOTTOM) {
                    return new BlockBox[]{new BlockBox(0, 0, 0, 1, 0.1875f, 1)};
                } else {
                    return new BlockBox[]{new BlockBox(0, 0.8125f, 0, 1, 1, 1)};
                }
            }
            return null;
        }

        if (data instanceof Bed) {
            return new BlockBox[]{BlockBox.BED};
        }

        if (data instanceof Gate gate) {
            if (!gate.isOpen()) {
                return new BlockBox[]{new BlockBox(0, 0, 0, 1, 1.5f, 1)};
            }
            return null;
        }

        return null;
    }
}
