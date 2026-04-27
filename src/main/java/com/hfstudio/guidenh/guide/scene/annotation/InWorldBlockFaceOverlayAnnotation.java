package com.hfstudio.guidenh.guide.scene.annotation;

import java.util.Collections;
import java.util.Set;

import com.hfstudio.guidenh.guide.color.ColorValue;

public final class InWorldBlockFaceOverlayAnnotation extends InWorldAnnotation {

    private final int blockX;
    private final int blockY;
    private final int blockZ;
    private final ColorValue color;
    private final Set<Long> groupedPositions;

    public InWorldBlockFaceOverlayAnnotation(int blockX, int blockY, int blockZ, ColorValue color,
        Set<Long> groupedPositions) {
        this.blockX = blockX;
        this.blockY = blockY;
        this.blockZ = blockZ;
        this.color = color;
        this.groupedPositions = groupedPositions != null ? groupedPositions : Collections.<Long>emptySet();
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

    public ColorValue color() {
        return color;
    }

    public boolean hasGroupedNeighbor(int x, int y, int z) {
        return groupedPositions.contains(packBlockPos(x, y, z));
    }

    private static long packBlockPos(int x, int y, int z) {
        return (((long) x & 0x3FFFFFFL) << 38) | (((long) z & 0x3FFFFFFL) << 12) | ((long) y & 0xFFFL);
    }
}
