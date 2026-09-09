package dev.murk.antiesp.cache;

import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Bisected;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Directional;
import org.bukkit.block.data.FaceAttachable;
import org.bukkit.block.data.type.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

public final class MaterialClassifier {
    private static final boolean[] OCCLUDING = new boolean[Material.values().length];
    private static final boolean[] TRANSPARENT = new boolean[Material.values().length];

    static {
        initDefault();
    }

    private MaterialClassifier() {
    }

    private static void initDefault() {
        Arrays.fill(OCCLUDING, false);
        Arrays.fill(TRANSPARENT, false);

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
                    || name.contains("SCAFFOLDING") || name.contains("POINTED_DRIPSTONE") || (name.contains("AMETHYST") && !name.contains("BLOCK")) || name.contains("SKULL")
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
                TRANSPARENT[ordinal] = true;
            }
        }
    }

    public static boolean isTransparent(Material material) {
        if (material == null || material.isAir()) return true;
        int ordinal = material.ordinal();
        if (ordinal >= TRANSPARENT.length) return false;
        return TRANSPARENT[ordinal];
    }

    public static boolean isOccluding(Material material) {
        if (material == null) return false;

        int ordinal = material.ordinal();
        if (ordinal >= OCCLUDING.length) return false;

        return OCCLUDING[ordinal];
    }

    public static boolean isFullOccluding(BlockData data) {
        if (data == null) return false;

        if (data instanceof Slab slab) {
            return slab.getType() == Slab.Type.DOUBLE && !isTransparent(slab.getMaterial());
        }

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
            return getStairBoxes(stairs);
        }

        if (data instanceof Snow snow) {
            float height = Math.min(1.0f, (snow.getLayers() - 1) * 0.125f);
            return height > 0.0f ? new BlockBox[]{new BlockBox(0, 0, 0, 1, height, 1)} : null;
        }

        if (data instanceof TrapDoor trapdoor) {
            return getTrapdoorBoxes(trapdoor);
        }

        if (data instanceof Door door) {
            return getDoorBoxes(door);
        }

        if (data instanceof Gate gate) {
            if (gate.isOpen()) return null;
            BlockFace facing = gate.getFacing();
            if (facing == BlockFace.NORTH || facing == BlockFace.SOUTH) {
                return new BlockBox[]{new BlockBox(0.0f, 0.0f, 0.375f, 1.0f, 1.5f, 0.625f)};
            } else {
                return new BlockBox[]{new BlockBox(0.375f, 0.0f, 0.0f, 0.625f, 1.5f, 1.0f)};
            }
        }

        if (data instanceof Fence fence) {
            return getFenceBoxes(fence);
        }

        if (data instanceof Wall wall) {
            return getWallBoxes(wall);
        }

        if (data instanceof GlassPane pane) {
            return getPaneBoxes(pane);
        }

        if (data instanceof Chest chest) {
            return switch (chest.getFacing()) {
                case NORTH -> new BlockBox[]{new BlockBox(0.0625f, 0.0f, 0.0f, 0.9375f, 0.875f, 0.9375f)};
                case SOUTH -> new BlockBox[]{new BlockBox(0.0625f, 0.0f, 0.0625f, 0.9375f, 0.875f, 1.0f)};
                case WEST -> new BlockBox[]{new BlockBox(0.0f, 0.0f, 0.0625f, 0.9375f, 0.875f, 0.9375f)};
                default -> new BlockBox[]{new BlockBox(0.0625f, 0.0f, 0.0625f, 1.0f, 0.875f, 0.9375f)};
            };
        }

        if (data instanceof EnderChest) {
            return new BlockBox[]{new BlockBox(0.0625f, 0.0f, 0.0625f, 0.9375f, 0.875f, 0.9375f)};
        }

        if (data instanceof Bed bed) {
            return getBedBoxes(bed);
        }

        if (data instanceof Hopper hopper) {
            return getHopperBoxes(hopper);
        }

        if (data instanceof Cake cake) {
            float eaten = (1 + cake.getBites() * 2) / 16.0f;
            return new BlockBox[]{new BlockBox(eaten, 0.0f, 0.0625f, 0.9375f, 0.5f, 0.9375f)};
        }

        if (data instanceof Cocoa cocoa) {
            return getCocoaBoxes(cocoa.getAge(), cocoa.getFacing());
        }

        if (data instanceof Bell bell) {
            return getBellBoxes(bell);
        }

        if (data instanceof Scaffolding) {
            return new BlockBox[]{new BlockBox(0.0f, 0.875f, 0.0f, 1.0f, 1.0f, 1.0f)};
        }

        if (data instanceof Ladder ladder) {
            return switch (ladder.getFacing()) {
                case NORTH -> new BlockBox[]{new BlockBox(0.0f, 0.0f, 0.8125f, 1.0f, 1.0f, 1.0f)};
                case SOUTH -> new BlockBox[]{new BlockBox(0.0f, 0.0f, 0.0f, 1.0f, 1.0f, 0.1875f)};
                case WEST -> new BlockBox[]{new BlockBox(0.8125f, 0.0f, 0.0f, 1.0f, 1.0f, 1.0f)};
                default -> new BlockBox[]{new BlockBox(0.0f, 0.0f, 0.0f, 0.1875f, 1.0f, 1.0f)};
            };
        }

        if (data instanceof Campfire) {
            return new BlockBox[]{new BlockBox(0.0f, 0.0f, 0.0f, 1.0f, 0.4375f, 1.0f)};
        }

        if (data instanceof Lantern lantern) {
            if (lantern.isHanging()) {
                return new BlockBox[]{
                        new BlockBox(0.3125f, 0.0625f, 0.3125f, 0.6875f, 0.5f, 0.6875f),
                        new BlockBox(0.375f, 0.5f, 0.375f, 0.625f, 0.625f, 0.625f)
                };
            }
            return new BlockBox[]{
                    new BlockBox(0.3125f, 0.0f, 0.3125f, 0.6875f, 0.4375f, 0.6875f),
                    new BlockBox(0.375f, 0.4375f, 0.375f, 0.625f, 0.5625f, 0.625f)
            };
        }

        if (data instanceof Lectern) {
            return new BlockBox[]{
                    new BlockBox(0.0f, 0.0f, 0.0f, 1.0f, 0.125f, 1.0f),
                    new BlockBox(0.25f, 0.125f, 0.25f, 0.75f, 0.875f, 0.75f)
            };
        }

        if (data instanceof Grindstone grindstone) {
            return getGrindstoneBoxes(grindstone);
        }

        if (data instanceof Chain chain) {
            org.bukkit.Axis axis = chain.getAxis();
            if (axis == org.bukkit.Axis.X) {
                return new BlockBox[]{new BlockBox(0.0f, 0.40625f, 0.40625f, 1.0f, 0.59375f, 0.59375f)};
            } else if (axis == org.bukkit.Axis.Y) {
                return new BlockBox[]{new BlockBox(0.40625f, 0.0f, 0.40625f, 0.59375f, 1.0f, 0.59375f)};
            } else {
                return new BlockBox[]{new BlockBox(0.40625f, 0.40625f, 0.0f, 0.59375f, 0.59375f, 1.0f)};
            }
        }

        if (data instanceof EndPortalFrame frame) {
            if (frame.hasEye()) {
                return new BlockBox[]{
                        new BlockBox(0.0f, 0.0f, 0.0f, 1.0f, 0.8125f, 1.0f),
                        new BlockBox(0.25f, 0.8125f, 0.25f, 0.75f, 1.0f, 0.75f)
                };
            }
            return new BlockBox[]{new BlockBox(0.0f, 0.0f, 0.0f, 1.0f, 0.8125f, 1.0f)};
        }

        if (data instanceof DaylightDetector) {
            return new BlockBox[]{new BlockBox(0.0f, 0.0f, 0.0f, 1.0f, 0.375f, 1.0f)};
        }

        if (data instanceof Repeater || data instanceof Comparator) {
            return new BlockBox[]{new BlockBox(0.0f, 0.0f, 0.0f, 1.0f, 0.125f, 1.0f)};
        }

        if (data instanceof SeaPickle pickle) {
            return getPickleBoxes(pickle.getPickles());
        }

        if (data instanceof TurtleEgg egg) {
            if (egg.getEggs() == 1) {
                return new BlockBox[]{new BlockBox(0.1875f, 0.0f, 0.1875f, 0.75f, 0.4375f, 0.75f)};
            }
            return new BlockBox[]{new BlockBox(0.0625f, 0.0f, 0.0625f, 0.9375f, 0.4375f, 0.9375f)};
        }

        if (data instanceof WallSign wallSign) {
            return switch (wallSign.getFacing()) {
                case NORTH -> new BlockBox[]{new BlockBox(0.0f, 0.28125f, 0.875f, 1.0f, 0.78125f, 1.0f)};
                case SOUTH -> new BlockBox[]{new BlockBox(0.0f, 0.28125f, 0.0f, 1.0f, 0.78125f, 0.125f)};
                case WEST -> new BlockBox[]{new BlockBox(0.875f, 0.28125f, 0.0f, 1.0f, 0.78125f, 1.0f)};
                case EAST -> new BlockBox[]{new BlockBox(0.0f, 0.28125f, 0.0f, 0.125f, 0.78125f, 1.0f)};
                default -> null;
            };
        }

        if (data instanceof Sign) {
            return new BlockBox[]{new BlockBox(0.25f, 0.0f, 0.25f, 0.75f, 1.0f, 0.75f)};
        }

        if (data instanceof Piston piston) {
            if (!piston.isExtended()) {
                return new BlockBox[]{BlockBox.FULL_CUBE};
            }
            return switch (piston.getFacing()) {
                case DOWN -> new BlockBox[]{new BlockBox(0.0f, 0.25f, 0.0f, 1.0f, 1.0f, 1.0f)};
                case UP -> new BlockBox[]{new BlockBox(0.0f, 0.0f, 0.0f, 1.0f, 0.75f, 1.0f)};
                case NORTH -> new BlockBox[]{new BlockBox(0.0f, 0.0f, 0.25f, 1.0f, 1.0f, 1.0f)};
                case SOUTH -> new BlockBox[]{new BlockBox(0.0f, 0.0f, 0.0f, 1.0f, 1.0f, 0.75f)};
                case WEST -> new BlockBox[]{new BlockBox(0.25f, 0.0f, 0.0f, 1.0f, 1.0f, 1.0f)};
                case EAST -> new BlockBox[]{new BlockBox(0.0f, 0.0f, 0.0f, 0.75f, 1.0f, 1.0f)};
                default -> new BlockBox[]{BlockBox.FULL_CUBE};
            };
        }

        if (data instanceof PistonHead head) {
            BlockFace facing = head.getFacing();
            return switch (facing) {
                case DOWN -> new BlockBox[]{
                        new BlockBox(0.0f, 0.0f, 0.0f, 1.0f, 0.25f, 1.0f),
                        new BlockBox(0.375f, 0.25f, 0.375f, 0.625f, 1.0f, 0.625f)
                };
                case UP -> new BlockBox[]{
                        new BlockBox(0.0f, 0.75f, 0.0f, 1.0f, 1.0f, 1.0f),
                        new BlockBox(0.375f, 0.0f, 0.375f, 0.625f, 0.75f, 0.625f)
                };
                case NORTH -> new BlockBox[]{
                        new BlockBox(0.0f, 0.0f, 0.0f, 1.0f, 1.0f, 0.25f),
                        new BlockBox(0.375f, 0.375f, 0.25f, 0.625f, 0.625f, 1.0f)
                };
                case SOUTH -> new BlockBox[]{
                        new BlockBox(0.0f, 0.0f, 0.75f, 1.0f, 1.0f, 1.0f),
                        new BlockBox(0.375f, 0.375f, 0.0f, 0.625f, 0.625f, 0.75f)
                };
                case WEST -> new BlockBox[]{
                        new BlockBox(0.0f, 0.0f, 0.0f, 0.25f, 1.0f, 1.0f),
                        new BlockBox(0.25f, 0.375f, 0.375f, 1.0f, 0.625f, 0.625f)
                };
                case EAST -> new BlockBox[]{
                        new BlockBox(0.75f, 0.0f, 0.0f, 1.0f, 1.0f, 1.0f),
                        new BlockBox(0.0f, 0.375f, 0.375f, 0.75f, 0.625f, 0.625f)
                };
                default -> new BlockBox[]{BlockBox.FULL_CUBE};
            };
        }

        String name = data.getMaterial().name();

        if (name.contains("CARPET")) {
            return new BlockBox[]{BlockBox.CARPET};
        }

        if (name.contains("ANVIL")) {
            if (data instanceof Directional dir) {
                BlockFace face = dir.getFacing();
                if (face == BlockFace.NORTH || face == BlockFace.SOUTH) {
                    return new BlockBox[]{
                            new BlockBox(0.125f, 0.0f, 0.125f, 0.875f, 0.25f, 0.875f),
                            new BlockBox(0.25f, 0.25f, 0.1875f, 0.75f, 0.3125f, 0.8125f),
                            new BlockBox(0.375f, 0.3125f, 0.25f, 0.625f, 0.625f, 0.75f),
                            new BlockBox(0.1875f, 0.625f, 0.0f, 0.8125f, 1.0f, 1.0f)
                    };
                } else {
                    return new BlockBox[]{
                            new BlockBox(0.125f, 0.0f, 0.125f, 0.875f, 0.25f, 0.875f),
                            new BlockBox(0.1875f, 0.25f, 0.25f, 0.8125f, 0.3125f, 0.75f),
                            new BlockBox(0.25f, 0.3125f, 0.375f, 0.75f, 0.625f, 0.625f),
                            new BlockBox(0.0f, 0.625f, 0.1875f, 1.0f, 1.0f, 0.8125f)
                    };
                }
            }
            return new BlockBox[]{new BlockBox(0.125f, 0.0f, 0.125f, 0.875f, 1.0f, 0.875f)};
        }

        if (name.contains("CAULDRON")) {
            return new BlockBox[]{
                    new BlockBox(0.0f, 0.0f, 0.0f, 1.0f, 0.3125f, 1.0f),
                    new BlockBox(0.0f, 0.3125f, 0.0f, 0.125f, 1.0f, 1.0f),
                    new BlockBox(0.875f, 0.3125f, 0.0f, 1.0f, 1.0f, 1.0f),
                    new BlockBox(0.125f, 0.3125f, 0.0f, 0.875f, 1.0f, 0.125f),
                    new BlockBox(0.125f, 0.3125f, 0.875f, 0.875f, 1.0f, 1.0f)
            };
        }

        switch (name) {
            case "COMPOSTER" -> {
                return new BlockBox[]{
                        new BlockBox(0.0f, 0.0f, 0.0f, 1.0f, 0.125f, 1.0f),
                        new BlockBox(0.0f, 0.125f, 0.0f, 0.125f, 1.0f, 1.0f),
                        new BlockBox(0.875f, 0.125f, 0.0f, 1.0f, 1.0f, 1.0f),
                        new BlockBox(0.125f, 0.125f, 0.0f, 0.875f, 1.0f, 0.125f),
                        new BlockBox(0.125f, 0.125f, 0.875f, 0.875f, 1.0f, 1.0f)
                };
            }
            case "STONECUTTER" -> {
                return new BlockBox[]{new BlockBox(0.0f, 0.0f, 0.0f, 1.0f, 0.5625f, 1.0f)};
            }
            case "ENCHANTING_TABLE" -> {
                return new BlockBox[]{new BlockBox(0.0f, 0.0f, 0.0f, 1.0f, 0.75f, 1.0f)};
            }
            case "BREWING_STAND" -> {
                return new BlockBox[]{
                        new BlockBox(0.0625f, 0.0f, 0.0625f, 0.9375f, 0.125f, 0.9375f),
                        new BlockBox(0.4375f, 0.0f, 0.4375f, 0.5625f, 0.875f, 0.5625f)
                };
            }
            case "HONEY_BLOCK", "CACTUS" -> {
                return new BlockBox[]{new BlockBox(0.0625f, 0.0f, 0.0625f, 0.9375f, 0.9375f, 0.9375f)};
            }
            case "DRAGON_EGG" -> {
                return new BlockBox[]{new BlockBox(0.0625f, 0.0f, 0.0625f, 0.9375f, 1.0f, 0.9375f)};
            }
            case "SOUL_SAND", "MUD" -> {
                return new BlockBox[]{new BlockBox(0.0f, 0.0f, 0.0f, 1.0f, 0.875f, 1.0f)};
            }
        }

        if (name.contains("FLOWER_POT") || name.startsWith("POTTED_")) {
            return new BlockBox[]{new BlockBox(0.3125f, 0.0f, 0.3125f, 0.6875f, 0.375f, 0.6875f)};
        }

        if (name.equals("CONDUIT")) {
            return new BlockBox[]{new BlockBox(0.3125f, 0.3125f, 0.3125f, 0.6875f, 0.6875f, 0.6875f)};
        }

        if (name.contains("END_ROD") || name.contains("LIGHTNING_ROD")) {
            if (data instanceof Directional dir) {
                BlockFace facing = dir.getFacing();
                if (facing == BlockFace.NORTH || facing == BlockFace.SOUTH) {
                    return new BlockBox[]{new BlockBox(0.375f, 0.375f, 0.0f, 0.625f, 0.625f, 1.0f)};
                } else if (facing == BlockFace.EAST || facing == BlockFace.WEST) {
                    return new BlockBox[]{new BlockBox(0.0f, 0.375f, 0.375f, 1.0f, 0.625f, 0.625f)};
                } else {
                    return new BlockBox[]{new BlockBox(0.375f, 0.0f, 0.375f, 0.625f, 1.0f, 0.625f)};
                }
            }
            return new BlockBox[]{new BlockBox(0.375f, 0.0f, 0.375f, 0.625f, 1.0f, 0.625f)};
        }

        if (name.contains("HEAD") || name.contains("SKULL")) {
            if (data instanceof Directional dir) {
                BlockFace facing = dir.getFacing();
                return switch (facing) {
                    case NORTH -> new BlockBox[]{new BlockBox(0.25f, 0.25f, 0.5f, 0.75f, 0.75f, 1.0f)};
                    case SOUTH -> new BlockBox[]{new BlockBox(0.25f, 0.25f, 0.0f, 0.75f, 0.75f, 0.5f)};
                    case WEST -> new BlockBox[]{new BlockBox(0.5f, 0.25f, 0.25f, 1.0f, 0.75f, 0.75f)};
                    case EAST -> new BlockBox[]{new BlockBox(0.0f, 0.25f, 0.25f, 0.5f, 0.75f, 0.75f)};
                    default -> new BlockBox[]{new BlockBox(0.25f, 0.0f, 0.25f, 0.75f, 0.5f, 0.75f)};
                };
            }
            return new BlockBox[]{new BlockBox(0.25f, 0.0f, 0.25f, 0.75f, 0.5f, 0.75f)};
        }

        if (name.contains("TORCH")) {
            if (data instanceof Directional dir) {
                return switch (dir.getFacing()) {
                    case NORTH -> new BlockBox[]{new BlockBox(0.34375f, 0.1875f, 0.6875f, 0.65625f, 0.8125f, 1.0f)};
                    case SOUTH -> new BlockBox[]{new BlockBox(0.34375f, 0.1875f, 0.0f, 0.65625f, 0.8125f, 0.3125f)};
                    case WEST -> new BlockBox[]{new BlockBox(0.6875f, 0.1875f, 0.34375f, 1.0f, 0.8125f, 0.65625f)};
                    case EAST -> new BlockBox[]{new BlockBox(0.0f, 0.1875f, 0.34375f, 0.3125f, 0.8125f, 0.65625f)};
                    default -> new BlockBox[]{new BlockBox(0.375f, 0.0f, 0.375f, 0.625f, 0.625f, 0.625f)};
                };
            }
            return new BlockBox[]{new BlockBox(0.375f, 0.0f, 0.375f, 0.625f, 0.625f, 0.625f)};
        }

        if (name.contains("PRESSURE_PLATE")) {
            return new BlockBox[]{new BlockBox(0.0625f, 0.0f, 0.0625f, 0.9375f, 0.0625f, 0.9375f)};
        }

        if (name.equals("SCULK_SENSOR") || name.equals("CALIBRATED_SCULK_SENSOR") || name.equals("SCULK_SHRIEKER")) {
            return new BlockBox[]{new BlockBox(0.0f, 0.0f, 0.0f, 1.0f, 0.5f, 1.0f)};
        }

        if (name.equals("DECORATED_POT")) {
            return new BlockBox[]{new BlockBox(0.0625f, 0.0f, 0.0625f, 0.9375f, 1.0f, 0.9375f)};
        }

        if (name.contains("AMETHYST_BUD")) {
            if (data instanceof Directional dir) {
                int p0 = name.contains("SMALL") ? 3 : (name.contains("MEDIUM") ? 4 : 5);
                int p1 = name.contains("SMALL") ? 4 : 3;
                return getAmethystBoxes(dir.getFacing(), p0, p1);
            }
        }

        if (name.equals("AZALEA") || name.equals("FLOWERING_AZALEA")) {
            return new BlockBox[]{
                    new BlockBox(0.0f, 0.5f, 0.0f, 1.0f, 1.0f, 1.0f),
                    new BlockBox(0.375f, 0.0f, 0.375f, 0.625f, 0.5f, 0.625f)
            };
        }

        if (name.equals("SNIFFER_EGG")) {
            return new BlockBox[]{new BlockBox(0.0625f, 0.0f, 0.125f, 0.9375f, 1.0f, 0.875f)};
        }

        return null;
    }

    private static BlockBox[] getStairBoxes(Stairs stairs) {
        Bisected.Half half = stairs.getHalf();
        if (half == null) {
            half = Bisected.Half.BOTTOM;
        }
        BlockFace facing = stairs.getFacing();
        if (facing == null) {
            facing = BlockFace.NORTH;
        }
        Stairs.Shape shape = stairs.getShape();
        if (shape == null) {
            shape = Stairs.Shape.STRAIGHT;
        }

        float y0 = (half == Bisected.Half.BOTTOM) ? 0.5f : 0.0f;
        float y1 = (half == Bisected.Half.BOTTOM) ? 1.0f : 0.5f;
        BlockBox base = (half == Bisected.Half.BOTTOM) ? BlockBox.SLAB_BOTTOM : BlockBox.SLAB_TOP;

        return switch (facing) {
            case NORTH -> switch (shape) {
                case STRAIGHT -> new BlockBox[]{base, new BlockBox(0, y0, 0, 1, y1, 0.5f)};
                case INNER_LEFT -> new BlockBox[]{base, new BlockBox(0, y0, 0, 1, y1, 0.5f), new BlockBox(0, y0, 0.5f, 0.5f, y1, 1)};
                case INNER_RIGHT -> new BlockBox[]{base, new BlockBox(0, y0, 0, 1, y1, 0.5f), new BlockBox(0.5f, y0, 0.5f, 1, y1, 1)};
                case OUTER_LEFT -> new BlockBox[]{base, new BlockBox(0, y0, 0, 0.5f, y1, 0.5f)};
                case OUTER_RIGHT -> new BlockBox[]{base, new BlockBox(0.5f, y0, 0, 1, y1, 0.5f)};
            };
            case SOUTH -> switch (shape) {
                case STRAIGHT -> new BlockBox[]{base, new BlockBox(0, y0, 0.5f, 1, y1, 1)};
                case INNER_LEFT -> new BlockBox[]{base, new BlockBox(0, y0, 0.5f, 1, y1, 1), new BlockBox(0.5f, y0, 0, 1, y1, 0.5f)};
                case INNER_RIGHT -> new BlockBox[]{base, new BlockBox(0, y0, 0.5f, 1, y1, 1), new BlockBox(0, y0, 0, 0.5f, y1, 0.5f)};
                case OUTER_LEFT -> new BlockBox[]{base, new BlockBox(0.5f, y0, 0.5f, 1, y1, 1)};
                case OUTER_RIGHT -> new BlockBox[]{base, new BlockBox(0, y0, 0.5f, 0.5f, y1, 1)};
            };
            case WEST -> switch (shape) {
                case STRAIGHT -> new BlockBox[]{base, new BlockBox(0, y0, 0, 0.5f, y1, 1)};
                case INNER_LEFT -> new BlockBox[]{base, new BlockBox(0, y0, 0, 0.5f, y1, 1), new BlockBox(0.5f, y0, 0.5f, 1, y1, 1)};
                case INNER_RIGHT -> new BlockBox[]{base, new BlockBox(0, y0, 0, 0.5f, y1, 1), new BlockBox(0.5f, y0, 0, 0.5f, y1, 0.5f)};
                case OUTER_LEFT -> new BlockBox[]{base, new BlockBox(0, y0, 0, 0.5f, y1, 0.5f)};
                case OUTER_RIGHT -> new BlockBox[]{base, new BlockBox(0, y0, 0.5f, 0.5f, y1, 1)};
            };
            case EAST -> switch (shape) {
                case STRAIGHT -> new BlockBox[]{base, new BlockBox(0.5f, y0, 0, 1, y1, 1)};
                case INNER_LEFT -> new BlockBox[]{base, new BlockBox(0.5f, y0, 0, 1, y1, 1), new BlockBox(0, y0, 0, 0.5f, y1, 0.5f)};
                case INNER_RIGHT -> new BlockBox[]{base, new BlockBox(0.5f, y0, 0, 1, y1, 1), new BlockBox(0, y0, 0.5f, 0.5f, y1, 1)};
                case OUTER_LEFT -> new BlockBox[]{base, new BlockBox(0.5f, y0, 0, 1, y1, 0.5f)};
                case OUTER_RIGHT -> new BlockBox[]{base, new BlockBox(0.5f, y0, 0.5f, 1, y1, 1)};
            };
            default -> new BlockBox[]{base, new BlockBox(0, y0, 0, 1, y1, 0.5f)};
        };
    }

    private static BlockBox[] getTrapdoorBoxes(TrapDoor trapdoor) {
        if (!trapdoor.isOpen()) {
            if (trapdoor.getHalf() == Bisected.Half.BOTTOM) {
                return new BlockBox[]{new BlockBox(0, 0, 0, 1, 0.1875f, 1)};
            } else {
                return new BlockBox[]{new BlockBox(0, 0.8125f, 0, 1, 1, 1)};
            }
        }
        return switch (trapdoor.getFacing()) {
            case NORTH -> new BlockBox[]{new BlockBox(0.0f, 0.0f, 0.8125f, 1.0f, 1.0f, 1.0f)};
            case SOUTH -> new BlockBox[]{new BlockBox(0.0f, 0.0f, 0.0f, 1.0f, 1.0f, 0.1875f)};
            case WEST -> new BlockBox[]{new BlockBox(0.8125f, 0.0f, 0.0f, 1.0f, 1.0f, 1.0f)};
            default -> new BlockBox[]{new BlockBox(0.0f, 0.0f, 0.0f, 0.1875f, 1.0f, 1.0f)};
        };
    }

    private static BlockBox[] getDoorBoxes(Door door) {
        if (!door.isOpen()) {
            return switch (door.getFacing()) {
                case NORTH -> new BlockBox[]{new BlockBox(0.0f, 0.0f, 0.8125f, 1.0f, 1.0f, 1.0f)};
                case SOUTH -> new BlockBox[]{new BlockBox(0.0f, 0.0f, 0.0f, 1.0f, 1.0f, 0.1875f)};
                case WEST -> new BlockBox[]{new BlockBox(0.8125f, 0.0f, 0.0f, 1.0f, 1.0f, 1.0f)};
                default -> new BlockBox[]{new BlockBox(0.0f, 0.0f, 0.0f, 0.1875f, 1.0f, 1.0f)};
            };
        }
        BlockFace facing = door.getFacing();
        Door.Hinge hinge = door.getHinge();
        if (hinge == Door.Hinge.LEFT) {
            return switch (facing) {
                case NORTH -> new BlockBox[]{new BlockBox(0.8125f, 0.0f, 0.0f, 1.0f, 1.0f, 1.0f)};
                case SOUTH -> new BlockBox[]{new BlockBox(0.0f, 0.0f, 0.0f, 0.1875f, 1.0f, 1.0f)};
                case WEST -> new BlockBox[]{new BlockBox(0.0f, 0.0f, 0.8125f, 1.0f, 1.0f, 1.0f)};
                default -> new BlockBox[]{new BlockBox(0.0f, 0.0f, 0.0f, 1.0f, 1.0f, 0.1875f)};
            };
        } else {
            return switch (facing) {
                case NORTH -> new BlockBox[]{new BlockBox(0.0f, 0.0f, 0.0f, 0.1875f, 1.0f, 1.0f)};
                case SOUTH -> new BlockBox[]{new BlockBox(0.8125f, 0.0f, 0.0f, 1.0f, 1.0f, 1.0f)};
                case WEST -> new BlockBox[]{new BlockBox(0.0f, 0.0f, 0.0f, 1.0f, 1.0f, 0.1875f)};
                default -> new BlockBox[]{new BlockBox(0.0f, 0.0f, 0.8125f, 1.0f, 1.0f, 1.0f)};
            };
        }
    }

    private static BlockBox[] getFenceBoxes(Fence fence) {
        List<BlockBox> list = new ArrayList<>(5);
        list.add(new BlockBox(0.375f, 0.0f, 0.375f, 0.625f, 1.5f, 0.625f));
        if (fence.hasFace(BlockFace.NORTH)) list.add(new BlockBox(0.375f, 0.0f, 0.0f, 0.625f, 1.5f, 0.375f));
        if (fence.hasFace(BlockFace.SOUTH)) list.add(new BlockBox(0.375f, 0.0f, 0.625f, 0.625f, 1.5f, 1.0f));
        if (fence.hasFace(BlockFace.WEST)) list.add(new BlockBox(0.0f, 0.0f, 0.375f, 0.375f, 1.5f, 0.625f));
        if (fence.hasFace(BlockFace.EAST)) list.add(new BlockBox(0.625f, 0.0f, 0.375f, 1.0f, 1.5f, 0.625f));
        return list.toArray(new BlockBox[0]);
    }

    private static BlockBox[] getWallBoxes(Wall wall) {
        List<BlockBox> list = new ArrayList<>(5);
        if (wall.isUp()) list.add(new BlockBox(0.25f, 0.0f, 0.25f, 0.75f, 1.5f, 0.75f));
        if (wall.getHeight(BlockFace.NORTH) != Wall.Height.NONE) list.add(new BlockBox(0.25f, 0.0f, 0.0f, 0.75f, 1.5f, 0.5f));
        if (wall.getHeight(BlockFace.SOUTH) != Wall.Height.NONE) list.add(new BlockBox(0.25f, 0.0f, 0.5f, 0.75f, 1.5f, 1.0f));
        if (wall.getHeight(BlockFace.WEST) != Wall.Height.NONE) list.add(new BlockBox(0.0f, 0.0f, 0.25f, 0.5f, 1.5f, 0.75f));
        if (wall.getHeight(BlockFace.EAST) != Wall.Height.NONE) list.add(new BlockBox(0.5f, 0.0f, 0.25f, 1.0f, 1.5f, 0.75f));
        return list.toArray(new BlockBox[0]);
    }

    private static BlockBox[] getPaneBoxes(GlassPane pane) {
        List<BlockBox> list = new ArrayList<>(5);
        list.add(new BlockBox(0.4375f, 0.0f, 0.4375f, 0.5625f, 1.0f, 0.5625f));
        if (pane.hasFace(BlockFace.NORTH)) list.add(new BlockBox(0.4375f, 0.0f, 0.0f, 0.5625f, 1.0f, 0.4375f));
        if (pane.hasFace(BlockFace.SOUTH)) list.add(new BlockBox(0.4375f, 0.0f, 0.5625f, 0.5625f, 1.0f, 1.0f));
        if (pane.hasFace(BlockFace.WEST)) list.add(new BlockBox(0.0f, 0.0f, 0.4375f, 0.4375f, 1.0f, 0.5625f));
        if (pane.hasFace(BlockFace.EAST)) list.add(new BlockBox(0.5625f, 0.0f, 0.4375f, 1.0f, 1.0f, 0.5625f));
        return list.toArray(new BlockBox[0]);
    }

    private static BlockBox[] getBedBoxes(Bed bed) {
        BlockFace facing = bed.getPart() == Bed.Part.HEAD ? bed.getFacing() : bed.getFacing().getOppositeFace();
        BlockBox main = new BlockBox(0.0f, 0.1875f, 0.0f, 1.0f, 0.5625f, 1.0f);
        return switch (facing) {
            case NORTH -> new BlockBox[]{
                    main,
                    new BlockBox(0.0f, 0.0f, 0.0f, 0.1875f, 0.1875f, 0.1875f),
                    new BlockBox(0.8125f, 0.0f, 0.0f, 1.0f, 0.1875f, 0.1875f)
            };
            case SOUTH -> new BlockBox[]{
                    main,
                    new BlockBox(0.0f, 0.0f, 0.8125f, 0.1875f, 0.1875f, 1.0f),
                    new BlockBox(0.8125f, 0.0f, 0.8125f, 1.0f, 0.1875f, 1.0f)
            };
            case WEST -> new BlockBox[]{
                    main,
                    new BlockBox(0.0f, 0.0f, 0.0f, 0.1875f, 0.1875f, 0.1875f),
                    new BlockBox(0.0f, 0.0f, 0.8125f, 0.1875f, 0.1875f, 1.0f)
            };
            default -> new BlockBox[]{
                    main,
                    new BlockBox(0.8125f, 0.0f, 0.0f, 1.0f, 0.1875f, 0.1875f),
                    new BlockBox(0.8125f, 0.0f, 0.8125f, 1.0f, 0.1875f, 1.0f)
            };
        };
    }

    private static BlockBox[] getHopperBoxes(Hopper hopper) {
        BlockFace facing = hopper.getFacing();
        BlockBox spout = switch (facing) {
            case DOWN -> new BlockBox(0.375f, 0.0f, 0.375f, 0.625f, 0.25f, 0.625f);
            case NORTH -> new BlockBox(0.375f, 0.25f, 0.0f, 0.625f, 0.5f, 0.25f);
            case SOUTH -> new BlockBox(0.375f, 0.25f, 0.75f, 0.625f, 0.5f, 1.0f);
            case WEST -> new BlockBox(0.0f, 0.25f, 0.375f, 0.25f, 0.5f, 0.625f);
            default -> new BlockBox(0.75f, 0.25f, 0.375f, 1.0f, 0.5f, 0.625f);
        };
        return new BlockBox[]{
                spout,
                new BlockBox(0.0f, 0.625f, 0.0f, 1.0f, 0.6875f, 1.0f),
                new BlockBox(0.0f, 0.6875f, 0.0f, 0.125f, 1.0f, 1.0f),
                new BlockBox(0.125f, 0.6875f, 0.0f, 1.0f, 1.0f, 0.125f),
                new BlockBox(0.125f, 0.6875f, 0.875f, 1.0f, 1.0f, 1.0f),
                new BlockBox(0.25f, 0.25f, 0.25f, 0.75f, 0.625f, 0.75f),
                new BlockBox(0.875f, 0.6875f, 0.125f, 1.0f, 1.0f, 0.875f)
        };
    }

    private static BlockBox[] getCandleBoxes(int count) {
        return switch (count) {
            case 1 -> new BlockBox[]{new BlockBox(0.4375f, 0.0f, 0.4375f, 0.5625f, 0.375f, 0.5625f)};
            case 2 -> new BlockBox[]{new BlockBox(0.3125f, 0.0f, 0.375f, 0.6875f, 0.375f, 0.5625f)};
            case 3 -> new BlockBox[]{new BlockBox(0.3125f, 0.0f, 0.375f, 0.625f, 0.375f, 0.6875f)};
            default -> new BlockBox[]{new BlockBox(0.3125f, 0.0f, 0.3125f, 0.6875f, 0.375f, 0.625f)};
        };
    }

    private static BlockBox[] getCocoaBoxes(int age, BlockFace facing) {
        return switch (facing) {
            case EAST -> switch (age) {
                case 0 -> new BlockBox[]{new BlockBox(0.6875f, 0.4375f, 0.375f, 0.9375f, 0.75f, 0.625f)};
                case 1 -> new BlockBox[]{new BlockBox(0.5625f, 0.3125f, 0.3125f, 0.9375f, 0.75f, 0.6875f)};
                default -> new BlockBox[]{new BlockBox(0.4375f, 0.1875f, 0.25f, 0.9375f, 0.75f, 0.75f)};
            };
            case WEST -> switch (age) {
                case 0 -> new BlockBox[]{new BlockBox(0.0625f, 0.4375f, 0.375f, 0.3125f, 0.75f, 0.625f)};
                case 1 -> new BlockBox[]{new BlockBox(0.0625f, 0.3125f, 0.3125f, 0.4375f, 0.75f, 0.6875f)};
                default -> new BlockBox[]{new BlockBox(0.0625f, 0.1875f, 0.25f, 0.5625f, 0.75f, 0.75f)};
            };
            case NORTH -> switch (age) {
                case 0 -> new BlockBox[]{new BlockBox(0.375f, 0.4375f, 0.0625f, 0.625f, 0.75f, 0.3125f)};
                case 1 -> new BlockBox[]{new BlockBox(0.3125f, 0.3125f, 0.0625f, 0.6875f, 0.75f, 0.4375f)};
                default -> new BlockBox[]{new BlockBox(0.25f, 0.1875f, 0.0625f, 0.75f, 0.75f, 0.5625f)};
            };
            default -> switch (age) {
                case 0 -> new BlockBox[]{new BlockBox(0.375f, 0.4375f, 0.6875f, 0.625f, 0.75f, 0.9375f)};
                case 1 -> new BlockBox[]{new BlockBox(0.3125f, 0.3125f, 0.5625f, 0.6875f, 0.75f, 0.9375f)};
                default -> new BlockBox[]{new BlockBox(0.25f, 0.1875f, 0.4375f, 0.75f, 0.75f, 0.9375f)};
            };
        };
    }

    private static BlockBox[] getBellBoxes(Bell bell) {
        Bell.Attachment attachment = bell.getAttachment();
        BlockFace direction = bell.getFacing();

        if (attachment == Bell.Attachment.FLOOR) {
            return direction != BlockFace.NORTH && direction != BlockFace.SOUTH
                    ? new BlockBox[]{new BlockBox(0.25f, 0.0f, 0.0f, 0.75f, 1.0f, 1.0f)}
                    : new BlockBox[]{new BlockBox(0.0f, 0.0f, 0.25f, 1.0f, 1.0f, 0.75f)};
        }

        List<BlockBox> boxes = new ArrayList<>(3);
        boxes.add(new BlockBox(0.3125f, 0.375f, 0.3125f, 0.6875f, 0.8125f, 0.6875f));
        boxes.add(new BlockBox(0.25f, 0.25f, 0.25f, 0.75f, 0.375f, 0.75f));

        if (attachment == Bell.Attachment.CEILING) {
            boxes.add(new BlockBox(0.4375f, 0.8125f, 0.4375f, 0.5625f, 1.0f, 0.5625f));
        } else if (attachment == Bell.Attachment.DOUBLE_WALL) {
            if (direction != BlockFace.NORTH && direction != BlockFace.SOUTH) {
                boxes.add(new BlockBox(0.0f, 0.8125f, 0.4375f, 1.0f, 0.9375f, 0.5625f));
            } else {
                boxes.add(new BlockBox(0.4375f, 0.8125f, 0.0f, 0.5625f, 0.9375f, 1.0f));
            }
        } else if (direction == BlockFace.NORTH) {
            boxes.add(new BlockBox(0.4375f, 0.8125f, 0.0f, 0.5625f, 0.9375f, 0.8125f));
        } else if (direction == BlockFace.SOUTH) {
            boxes.add(new BlockBox(0.4375f, 0.8125f, 0.1875f, 0.5625f, 0.9375f, 1.0f));
        } else if (direction == BlockFace.EAST) {
            boxes.add(new BlockBox(0.1875f, 0.8125f, 0.4375f, 1.0f, 0.9375f, 0.5625f));
        } else {
            boxes.add(new BlockBox(0.0f, 0.8125f, 0.4375f, 0.8125f, 0.9375f, 0.5625f));
        }

        return boxes.toArray(new BlockBox[0]);
    }

    private static BlockBox[] getGrindstoneBoxes(Grindstone grindstone) {
        FaceAttachable.AttachedFace face = grindstone.getAttachedFace();
        BlockFace facing = grindstone.getFacing();

        if (face == FaceAttachable.AttachedFace.FLOOR) {
            if (facing == BlockFace.NORTH || facing == BlockFace.SOUTH) {
                return new BlockBox[]{
                        new BlockBox(0.125f, 0.0f, 0.375f, 0.25f, 0.4375f, 0.625f),
                        new BlockBox(0.75f, 0.0f, 0.375f, 0.875f, 0.4375f, 0.625f),
                        new BlockBox(0.125f, 0.4375f, 0.3125f, 0.25f, 0.8125f, 0.6875f),
                        new BlockBox(0.75f, 0.4375f, 0.3125f, 0.875f, 0.8125f, 0.6875f),
                        new BlockBox(0.25f, 0.25f, 0.125f, 0.75f, 1.0f, 0.875f)
                };
            } else {
                return new BlockBox[]{
                        new BlockBox(0.375f, 0.0f, 0.125f, 0.625f, 0.4375f, 0.25f),
                        new BlockBox(0.375f, 0.0f, 0.75f, 0.625f, 0.4375f, 0.875f),
                        new BlockBox(0.3125f, 0.4375f, 0.125f, 0.6875f, 0.8125f, 0.25f),
                        new BlockBox(0.3125f, 0.4375f, 0.75f, 0.6875f, 0.8125f, 0.875f),
                        new BlockBox(0.125f, 0.25f, 0.25f, 0.875f, 1.0f, 0.75f)
                };
            }
        } else if (face == FaceAttachable.AttachedFace.WALL) {
            return switch (facing) {
                case NORTH -> new BlockBox[]{
                        new BlockBox(0.125f, 0.375f, 0.4375f, 0.25f, 0.625f, 1.0f),
                        new BlockBox(0.75f, 0.375f, 0.4375f, 0.875f, 0.625f, 1.0f),
                        new BlockBox(0.125f, 0.3125f, 0.1875f, 0.25f, 0.6875f, 0.5625f),
                        new BlockBox(0.75f, 0.3125f, 0.1875f, 0.875f, 0.6875f, 0.5625f),
                        new BlockBox(0.25f, 0.125f, 0.0f, 0.75f, 0.875f, 0.75f)
                };
                case WEST -> new BlockBox[]{
                        new BlockBox(0.4375f, 0.375f, 0.125f, 1.0f, 0.625f, 0.25f),
                        new BlockBox(0.4375f, 0.375f, 0.75f, 1.0f, 0.625f, 0.875f),
                        new BlockBox(0.1875f, 0.3125f, 0.125f, 0.5625f, 0.6875f, 0.25f),
                        new BlockBox(0.1875f, 0.3125f, 0.75f, 0.5625f, 0.6875f, 0.875f),
                        new BlockBox(0.0f, 0.125f, 0.25f, 0.75f, 0.875f, 0.75f)
                };
                case SOUTH -> new BlockBox[]{
                        new BlockBox(0.125f, 0.375f, 0.0f, 0.25f, 0.625f, 0.4375f),
                        new BlockBox(0.75f, 0.375f, 0.0f, 0.875f, 0.625f, 0.4375f),
                        new BlockBox(0.125f, 0.3125f, 0.4375f, 0.25f, 0.6875f, 0.8125f),
                        new BlockBox(0.75f, 0.3125f, 0.4375f, 0.875f, 0.6875f, 0.8125f),
                        new BlockBox(0.25f, 0.125f, 0.25f, 0.75f, 0.875f, 1.0f)
                };
                default -> new BlockBox[]{
                        new BlockBox(0.0f, 0.375f, 0.125f, 0.4375f, 0.625f, 0.25f),
                        new BlockBox(0.0f, 0.375f, 0.75f, 0.4375f, 0.625f, 0.875f),
                        new BlockBox(0.4375f, 0.3125f, 0.125f, 0.8125f, 0.6875f, 0.25f),
                        new BlockBox(0.4375f, 0.3125f, 0.75f, 0.8125f, 0.6875f, 0.875f),
                        new BlockBox(0.25f, 0.125f, 0.25f, 1.0f, 0.875f, 0.75f)
                };
            };
        } else {
            if (facing == BlockFace.NORTH || facing == BlockFace.SOUTH) {
                return new BlockBox[]{
                        new BlockBox(0.125f, 0.5625f, 0.375f, 0.25f, 1.0f, 0.625f),
                        new BlockBox(0.75f, 0.5625f, 0.375f, 0.875f, 1.0f, 0.625f),
                        new BlockBox(0.125f, 0.1875f, 0.3125f, 0.25f, 0.5625f, 0.6875f),
                        new BlockBox(0.75f, 0.1875f, 0.3125f, 0.875f, 0.5625f, 0.6875f),
                        new BlockBox(0.25f, 0.0f, 0.125f, 0.75f, 0.75f, 0.875f)
                };
            } else {
                return new BlockBox[]{
                        new BlockBox(0.375f, 0.5625f, 0.125f, 0.625f, 1.0f, 0.25f),
                        new BlockBox(0.375f, 0.5625f, 0.75f, 0.625f, 1.0f, 0.875f),
                        new BlockBox(0.3125f, 0.1875f, 0.125f, 0.6875f, 0.5625f, 0.25f),
                        new BlockBox(0.3125f, 0.1875f, 0.75f, 0.6875f, 0.5625f, 0.875f),
                        new BlockBox(0.125f, 0.0f, 0.25f, 0.875f, 0.75f, 0.75f)
                };
            }
        }
    }

    private static BlockBox[] getPickleBoxes(int count) {
        return switch (count) {
            case 1 -> new BlockBox[]{new BlockBox(0.375f, 0.0f, 0.375f, 0.625f, 0.375f, 0.625f)};
            case 2 -> new BlockBox[]{new BlockBox(0.1875f, 0.0f, 0.1875f, 0.8125f, 0.375f, 0.8125f)};
            case 3 -> new BlockBox[]{new BlockBox(0.125f, 0.0f, 0.125f, 0.875f, 0.375f, 0.875f)};
            default -> new BlockBox[]{new BlockBox(0.125f, 0.0f, 0.125f, 0.875f, 0.4375f, 0.875f)};
        };
    }

    private static BlockBox[] getAmethystBoxes(BlockFace facing, int p0, int p1) {
        float f0 = p0 / 16.0f;
        float f1 = p1 / 16.0f;
        float inv0 = (16 - p0) / 16.0f;
        float inv1 = (16 - p1) / 16.0f;

        return switch (facing) {
            case DOWN -> new BlockBox[]{new BlockBox(f1, inv0, f1, inv1, 1.0f, inv1)};
            case NORTH -> new BlockBox[]{new BlockBox(f1, f1, inv0, inv1, inv1, 1.0f)};
            case SOUTH -> new BlockBox[]{new BlockBox(f1, f1, 0.0f, inv1, inv1, f0)};
            case EAST -> new BlockBox[]{new BlockBox(0.0f, f1, f1, f0, inv1, inv1)};
            case WEST -> new BlockBox[]{new BlockBox(inv0, f1, f1, 1.0f, inv1, inv1)};
            default -> new BlockBox[]{new BlockBox(f1, 0.0f, f1, inv1, f0, inv1)};
        };
    }
}

