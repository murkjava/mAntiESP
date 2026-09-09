package dev.murk.antiesp.cache;

import java.util.BitSet;

public final class ChunkOcclusion {
    private static final int SECTION_COUNT = 40;
    private static final int SECTION_OFFSET = 8;
    private final BitSet[] sections = new BitSet[SECTION_COUNT];
    private volatile IntBoxMap customShapes = null;

    private record IntBoxMap(int[] keys, BlockBox[][] values) {
        static final IntBoxMap EMPTY = new IntBoxMap(new int[0], new BlockBox[0][]);

        public BlockBox[] get(int key) {
            int low = 0;
            int high = keys.length - 1;
            while (low <= high) {
                int mid = (low + high) >>> 1;
                int midKey = keys[mid];
                if (midKey < key) {
                    low = mid + 1;
                } else if (midKey > key) {
                    high = mid - 1;
                } else {
                    return values[mid];
                }
            }
            return null;
        }

        public IntBoxMap put(int key, BlockBox[] val) {
            int low = 0;
            int high = keys.length - 1;
            while (low <= high) {
                int mid = (low + high) >>> 1;
                int midKey = keys[mid];
                if (midKey < key) {
                    low = mid + 1;
                } else if (midKey > key) {
                    high = mid - 1;
                } else {
                    BlockBox[][] newValues = values.clone();
                    newValues[mid] = val;
                    return new IntBoxMap(keys, newValues);
                }
            }
            int insertIndex = low;
            int newLen = keys.length + 1;
            int[] newKeys = new int[newLen];
            BlockBox[][] newValues = new BlockBox[newLen][];

            System.arraycopy(keys, 0, newKeys, 0, insertIndex);
            System.arraycopy(values, 0, newValues, 0, insertIndex);

            newKeys[insertIndex] = key;
            newValues[insertIndex] = val;

            System.arraycopy(keys, insertIndex, newKeys, insertIndex + 1, keys.length - insertIndex);
            System.arraycopy(values, insertIndex, newValues, insertIndex + 1, values.length - insertIndex);

            return new IntBoxMap(newKeys, newValues);
        }

        public IntBoxMap remove(int key) {
            int low = 0;
            int high = keys.length - 1;
            int found = -1;
            while (low <= high) {
                int mid = (low + high) >>> 1;
                int midKey = keys[mid];
                if (midKey < key) {
                    low = mid + 1;
                } else if (midKey > key) {
                    high = mid - 1;
                } else {
                    found = mid;
                    break;
                }
            }
            if (found == -1) {
                return this;
            }
            if (keys.length == 1) {
                return EMPTY;
            }
            int newLen = keys.length - 1;
            int[] newKeys = new int[newLen];
            BlockBox[][] newValues = new BlockBox[newLen][];

            System.arraycopy(keys, 0, newKeys, 0, found);
            System.arraycopy(values, 0, newValues, 0, found);

            System.arraycopy(keys, found + 1, newKeys, found, newLen - found);
            System.arraycopy(values, found + 1, newValues, found, newLen - found);

            return new IntBoxMap(newKeys, newValues);
        }

        public boolean isEmpty() {
            return keys.length == 0;
        }
    }

    public boolean isOccluding(int x, int y, int z) {
        int sectionY = (y >> 4) + SECTION_OFFSET;
        if (sectionY < 0 || sectionY >= SECTION_COUNT) {
            return false;
        }

        BitSet section = sections[sectionY];
        if (section == null) {
            return false;
        }

        int index = ((y & 15) << 8) | ((z & 15) << 4) | (x & 15);
        return section.get(index);
    }

    public boolean isBlocked(int x, int y, int z,
                             double x0, double y0, double z0,
                             double x1, double y1, double z1) {
        int sectionY = (y >> 4) + SECTION_OFFSET;
        if (sectionY < 0 || sectionY >= SECTION_COUNT) {
            return false;
        }

        BitSet section = sections[sectionY];
        int index = ((y & 15) << 8) | ((z & 15) << 4) | (x & 15);
        if (section != null && section.get(index)) {
            return true;
        }

        IntBoxMap shapes = customShapes;
        if (shapes != null && !shapes.isEmpty()) {
            int key = (sectionY << 12) | index;
            BlockBox[] boxes = shapes.get(key);
            if (boxes != null) {
                for (BlockBox box : boxes) {
                    if (box.intersects(x0, y0, z0, x1, y1, z1, x, y, z)) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    public double clip(int x, int y, int z,
                       double x0, double y0, double z0,
                       double x1, double y1, double z1) {
        int sectionY = (y >> 4) + SECTION_OFFSET;
        if (sectionY < 0 || sectionY >= SECTION_COUNT) {
            return -1.0;
        }

        BitSet section = sections[sectionY];
        int index = ((y & 15) << 8) | ((z & 15) << 4) | (x & 15);
        if (section != null && section.get(index)) {
            return BlockBox.FULL_CUBE.clip(x0, y0, z0, x1, y1, z1, x, y, z);
        }

        IntBoxMap shapes = customShapes;
        if (shapes != null && !shapes.isEmpty()) {
            int key = (sectionY << 12) | index;
            BlockBox[] boxes = shapes.get(key);
            if (boxes != null) {
                double minT = -1.0;
                for (BlockBox box : boxes) {
                    double t = box.clip(x0, y0, z0, x1, y1, z1, x, y, z);
                    if (t >= 0.0) {
                        if (minT < 0.0 || t < minT) {
                            minT = t;
                        }
                    }
                }
                return minT;
            }
        }

        return -1.0;
    }

    public synchronized void setFullOccluding(int x, int y, int z, boolean occluding) {
        int sectionY = (y >> 4) + SECTION_OFFSET;
        if (sectionY < 0 || sectionY >= SECTION_COUNT) {
            return;
        }

        int index = ((y & 15) << 8) | ((z & 15) << 4) | (x & 15);
        int key = (sectionY << 12) | index;
        if (customShapes != null) {
            customShapes = customShapes.remove(key);
            if (customShapes.isEmpty()) {
                customShapes = null;
            }
        }

        BitSet section = sections[sectionY];
        if (section == null) {
            if (!occluding) {
                return;
            }
            section = new BitSet(4096);
            sections[sectionY] = section;
        }

        section.set(index, occluding);
    }

    public synchronized void setCustomShape(int x, int y, int z, BlockBox[] boxes) {
        int sectionY = (y >> 4) + SECTION_OFFSET;
        if (sectionY < 0 || sectionY >= SECTION_COUNT) {
            return;
        }

        int index = ((y & 15) << 8) | ((z & 15) << 4) | (x & 15);
        int key = (sectionY << 12) | index;

        BitSet section = sections[sectionY];
        if (section != null) {
            section.set(index, false);
        }

        if (boxes == null || boxes.length == 0) {
            if (customShapes != null) {
                customShapes = customShapes.remove(key);
                if (customShapes.isEmpty()) {
                    customShapes = null;
                }
            }
        } else {
            customShapes = (customShapes == null) ? IntBoxMap.EMPTY.put(key, boxes) : customShapes.put(key, boxes);
        }
    }

    public synchronized void removeBlock(int x, int y, int z) {
        int sectionY = (y >> 4) + SECTION_OFFSET;
        if (sectionY < 0 || sectionY >= SECTION_COUNT) {
            return;
        }

        int index = ((y & 15) << 8) | ((z & 15) << 4) | (x & 15);
        int key = (sectionY << 12) | index;

        BitSet section = sections[sectionY];
        if (section != null) {
            section.set(index, false);
        }
        if (customShapes != null) {
            customShapes = customShapes.remove(key);
            if (customShapes.isEmpty()) {
                customShapes = null;
            }
        }
    }

    public synchronized void setSection(int sectionYIndex, BitSet bitSet) {
        int index = sectionYIndex + SECTION_OFFSET;
        if (index >= 0 && index < SECTION_COUNT) {
            sections[index] = bitSet;
        }
    }
}
