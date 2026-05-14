package com.hfstudio.guidenh.guide.internal.item;

import org.jetbrains.annotations.Nullable;

import com.github.bsideup.jabel.Desugar;

public final class RegionWandSelection {

    @Nullable
    private static int[] pos1;
    @Nullable
    private static int[] pos2;

    private RegionWandSelection() {}

    public static void setPos(int which, int x, int y, int z) {
        int[] pos = new int[] { x, y, z };
        if (which == 1) {
            pos1 = pos;
        } else if (which == 2) {
            pos2 = pos;
        }
    }

    @Nullable
    public static int[] getPos(int which) {
        int[] pos = which == 1 ? pos1 : which == 2 ? pos2 : null;
        if (pos == null) {
            return null;
        }
        return new int[] { pos[0], pos[1], pos[2] };
    }

    public static boolean hasCompleteSelection() {
        return pos1 != null && pos2 != null;
    }

    public static void clear() {
        pos1 = null;
        pos2 = null;
    }

    @Nullable
    public static Bounds getBounds() {
        if (pos1 == null || pos2 == null) {
            return null;
        }
        int minX = Math.min(pos1[0], pos2[0]);
        int minY = Math.min(pos1[1], pos2[1]);
        int minZ = Math.min(pos1[2], pos2[2]);
        int maxX = Math.max(pos1[0], pos2[0]);
        int maxY = Math.max(pos1[1], pos2[1]);
        int maxZ = Math.max(pos1[2], pos2[2]);
        return new Bounds(minX, minY, minZ, maxX, maxY, maxZ);
    }

    @Desugar
    public record Bounds(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {

        public int sizeX() {
            return maxX - minX + 1;
        }

        public int sizeY() {
            return maxY - minY + 1;
        }

        public int sizeZ() {
            return maxZ - minZ + 1;
        }
    }
}
