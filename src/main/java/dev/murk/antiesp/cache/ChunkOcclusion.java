package dev.murk.antiesp.cache;

import java.util.BitSet;

public final class ChunkOcclusion {
    private static final int SECTION_COUNT = 32;
    private static final int SECTION_OFFSET = 4;
    private final BitSet[] sections = new BitSet[SECTION_COUNT];

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

    public synchronized void setOccluding(int x, int y, int z, boolean occluding) {
        int sectionY = (y >> 4) + SECTION_OFFSET;
        if (sectionY < 0 || sectionY >= SECTION_COUNT) {
            return;
        }

        BitSet section = sections[sectionY];
        if (section == null) {
            if (!occluding) {
                return;
            }
            section = new BitSet(4096);
            sections[sectionY] = section;
        }

        int index = ((y & 15) << 8) | ((z & 15) << 4) | (x & 15);
        section.set(index, occluding);
    }

    public synchronized void setSection(int sectionYIndex, BitSet bitSet) {
        int index = sectionYIndex + SECTION_OFFSET;
        if (index >= 0 && index < SECTION_COUNT) {
            sections[index] = bitSet;
        }
    }
}
