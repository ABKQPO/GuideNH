package com.hfstudio.guidenh.guide.scene.support;

import javax.annotation.Nullable;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.util.AxisAlignedBB;

import com.hfstudio.guidenh.guide.scene.level.GuidebookLevel;

public final class GuideBlockBoundsResolver {

    private GuideBlockBoundsResolver() {}

    @Nullable
    public static AxisAlignedBB resolveWorldBounds(GuidebookLevel level, int x, int y, int z) {
        Block block = level.getBlock(x, y, z);
        if (block == null || block == Blocks.air) {
            return null;
        }

        double minX = 0d;
        double minY = 0d;
        double minZ = 0d;
        double maxX = 1d;
        double maxY = 1d;
        double maxZ = 1d;

        try {
            block.setBlockBoundsBasedOnState(level, x, y, z);
            minX = block.getBlockBoundsMinX();
            minY = block.getBlockBoundsMinY();
            minZ = block.getBlockBoundsMinZ();
            maxX = block.getBlockBoundsMaxX();
            maxY = block.getBlockBoundsMaxY();
            maxZ = block.getBlockBoundsMaxZ();
            if (maxX <= minX || maxY <= minY || maxZ <= minZ) {
                minX = minY = minZ = 0d;
                maxX = maxY = maxZ = 1d;
            }
        } catch (Throwable ignored) {
            minX = minY = minZ = 0d;
            maxX = maxY = maxZ = 1d;
        }

        return AxisAlignedBB.getBoundingBox(x + minX, y + minY, z + minZ, x + maxX, y + maxY, z + maxZ);
    }
}
