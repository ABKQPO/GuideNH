package com.hfstudio.guidenh.guide.scene.support;

import java.util.ArrayList;
import java.util.List;

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

        AxisAlignedBB mergedBounds = resolveCollisionBounds(level, block, x, y, z);
        if (mergedBounds != null) {
            return mergedBounds;
        }

        try {
            AxisAlignedBB selectedBounds = block.getSelectedBoundingBoxFromPool(level.getOrCreateFakeWorld(), x, y, z);
            if (selectedBounds != null && isNonEmpty(selectedBounds)) {
                return selectedBounds;
            }
        } catch (Throwable ignored) {}

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

    @Nullable
    public static AxisAlignedBB resolveSelectedBounds(GuidebookLevel level, int x, int y, int z) {
        Block block = level.getBlock(x, y, z);
        if (block == null || block == Blocks.air) {
            return null;
        }

        try {
            block.setBlockBoundsBasedOnState(level.getOrCreateFakeWorld(), x, y, z);
            AxisAlignedBB selectedBounds = block.getSelectedBoundingBoxFromPool(level.getOrCreateFakeWorld(), x, y, z);
            if (selectedBounds != null && isNonEmpty(selectedBounds)) {
                return copyOf(selectedBounds);
            }
        } catch (Throwable ignored) {}

        return resolveWorldBounds(level, x, y, z);
    }

    @Nullable
    private static AxisAlignedBB resolveCollisionBounds(GuidebookLevel level, Block block, int x, int y, int z) {
        try {
            List<AxisAlignedBB> collisionBoxes = new ArrayList<>();
            AxisAlignedBB fullBlockBounds = AxisAlignedBB.getBoundingBox(x, y, z, x + 1d, y + 1d, z + 1d);
            block.addCollisionBoxesToList(level.getOrCreateFakeWorld(), x, y, z, fullBlockBounds, collisionBoxes, null);
            AxisAlignedBB merged = null;
            for (AxisAlignedBB collisionBox : collisionBoxes) {
                if (collisionBox == null || !isNonEmpty(collisionBox)) {
                    continue;
                }
                merged = merged == null ? copyOf(collisionBox) : merged.func_111270_a(collisionBox);
            }
            return merged;
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static boolean isNonEmpty(AxisAlignedBB bounds) {
        return bounds != null && bounds.maxX > bounds.minX && bounds.maxY > bounds.minY && bounds.maxZ > bounds.minZ;
    }

    private static AxisAlignedBB copyOf(AxisAlignedBB bounds) {
        return AxisAlignedBB
            .getBoundingBox(bounds.minX, bounds.minY, bounds.minZ, bounds.maxX, bounds.maxY, bounds.maxZ);
    }
}
