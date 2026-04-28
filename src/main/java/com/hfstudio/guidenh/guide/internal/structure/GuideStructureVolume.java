package com.hfstudio.guidenh.guide.internal.structure;

public class GuideStructureVolume {

    private GuideStructureVolume() {}

    public static long blockCount(int sizeX, int sizeY, int sizeZ) {
        return (long) sizeX * (long) sizeY * (long) sizeZ;
    }

    public static boolean exceedsLimit(int sizeX, int sizeY, int sizeZ, long limit) {
        return blockCount(sizeX, sizeY, sizeZ) > limit;
    }
}
