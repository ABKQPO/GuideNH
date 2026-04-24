package com.hfstudio.guidenh.guide.scene.level;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;

public final class GuidebookChunk {

    public final int chunkX;

    public final int chunkZ;

    private final Block[] blocks = new Block[16 * 16 * 256];

    private final byte[] metas = new byte[16 * 16 * 256];

    private int filledCount = 0;

    public GuidebookChunk(int chunkX, int chunkZ) {
        this.chunkX = chunkX;
        this.chunkZ = chunkZ;
    }

    private static int index(int x, int y, int z) {
        return ((x & 15) << 12) | ((z & 15) << 8) | (y & 255);
    }

    public Block getBlock(int x, int y, int z) {
        if (y < 0 || y >= 256) return null;
        return blocks[index(x, y, z)];
    }

    public int getMeta(int x, int y, int z) {
        if (y < 0 || y >= 256) return 0;
        return metas[index(x, y, z)] & 0xFF;
    }

    public boolean setBlock(int x, int y, int z, Block block, int meta) {
        if (y < 0 || y >= 256) return false;
        int idx = index(x, y, z);
        Block prev = blocks[idx];
        boolean prevFilled = prev != null && prev != Blocks.air;
        boolean nextFilled = block != null && block != Blocks.air;
        blocks[idx] = nextFilled ? block : null;
        metas[idx] = (byte) (meta & 0xFF);
        if (prevFilled && !nextFilled) {
            filledCount--;
            return true;
        } else if (!prevFilled && nextFilled) {
            filledCount++;
            return true;
        }
        return false;
    }

    public boolean isEmpty() {
        return filledCount == 0;
    }

    public int getFilledCount() {
        return filledCount;
    }

    public void forEachBlock(BlockIterator it) {
        for (int lx = 0; lx < 16; lx++) {
            for (int lz = 0; lz < 16; lz++) {
                for (int y = 0; y < 256; y++) {
                    int idx = ((lx) << 12) | ((lz) << 8) | y;
                    Block b = blocks[idx];
                    if (b != null && b != Blocks.air) {
                        it.accept(lx, y, lz, b, metas[idx] & 0xFF);
                    }
                }
            }
        }
    }

    @FunctionalInterface
    public interface BlockIterator {

        void accept(int localX, int y, int localZ, Block block, int meta);
    }
}
