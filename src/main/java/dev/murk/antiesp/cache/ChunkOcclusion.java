package dev.murk.antiesp.cache;

import java.util.BitSet;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class ChunkOcclusion {
    private static final int SECTION_COUNT = 32;
    private static final int SECTION_OFFSET = 4;
    private final BitSet[] sections = new BitSet[SECTION_COUNT];
    private final Map<Integer, BlockBox[]> customShapes = new ConcurrentHashMap<>();

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

        if (!customShapes.isEmpty()) {
            int key = (sectionY << 12) | index;
            BlockBox[] boxes = customShapes.get(key);
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

    public synchronized void setFullOccluding(int x, int y, int z, boolean occluding) {
        int sectionY = (y >> 4) + SECTION_OFFSET;
        if (sectionY < 0 || sectionY >= SECTION_COUNT) {
            return;
        }

        int index = ((y & 15) << 8) | ((z & 15) << 4) | (x & 15);
        int key = (sectionY << 12) | index;
        customShapes.remove(key);

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
            customShapes.remove(key);
        } else {
            customShapes.put(key, boxes);
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
        customShapes.remove(key);
    }

    public synchronized void setSection(int sectionYIndex, BitSet bitSet) {
        int index = sectionYIndex + SECTION_OFFSET;
        if (index >= 0 && index < SECTION_COUNT) {
            sections[index] = bitSet;
        }
    }
}
