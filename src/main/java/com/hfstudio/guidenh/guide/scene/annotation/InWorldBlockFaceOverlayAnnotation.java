package com.hfstudio.guidenh.guide.scene.annotation;

import java.util.Set;

import com.hfstudio.guidenh.guide.color.ColorValue;

public class InWorldBlockFaceOverlayAnnotation extends InWorldBoxFaceOverlayAnnotation {

    private final int blockX;
    private final int blockY;
    private final int blockZ;
    private final Set<Long> groupedPositions;

    public InWorldBlockFaceOverlayAnnotation(int blockX, int blockY, int blockZ, ColorValue color,
        Set<Long> groupedPositions) {
        super(blockX, blockY, blockZ, blockX + 1f, blockY + 1f, blockZ + 1f, color);
        this.blockX = blockX;
        this.blockY = blockY;
        this.blockZ = blockZ;
        this.groupedPositions = groupedPositions;
    }

    public int getBlockX() {
        return blockX;
    }

    public int getBlockY() {
        return blockY;
    }

    public int getBlockZ() {
        return blockZ;
    }

    @Override
    public boolean shouldDrawNegativeXFace() {
        return !hasGroupedNeighbor(blockX - 1, blockY, blockZ);
    }

    @Override
    public boolean shouldDrawPositiveXFace() {
        return !hasGroupedNeighbor(blockX + 1, blockY, blockZ);
    }

    @Override
    public boolean shouldDrawNegativeYFace() {
        return !hasGroupedNeighbor(blockX, blockY - 1, blockZ);
    }

    @Override
    public boolean shouldDrawPositiveYFace() {
        return !hasGroupedNeighbor(blockX, blockY + 1, blockZ);
    }

    @Override
    public boolean shouldDrawNegativeZFace() {
        return !hasGroupedNeighbor(blockX, blockY, blockZ - 1);
    }

    @Override
    public boolean shouldDrawPositiveZFace() {
        return !hasGroupedNeighbor(blockX, blockY, blockZ + 1);
    }

    public boolean hasGroupedNeighbor(int x, int y, int z) {
        return groupedPositions != null && groupedPositions.contains(packBlockPos(x, y, z));
    }

    public static long packBlockPos(int x, int y, int z) {
        return (((long) x & 0x3FFFFFFL) << 38) | (((long) z & 0x3FFFFFFL) << 12) | ((long) y & 0xFFFL);
    }
}
