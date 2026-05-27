package com.hfstudio.guidenh.guide.scene.support;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;

import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.guide.scene.level.GuidebookLevel;

public class GuideBlockBoundsResolver {

    private GuideBlockBoundsResolver() {}

    @Nullable
    public static AxisAlignedBB resolveWorldBounds(GuidebookLevel level, int x, int y, int z) {
        Block block = level.getBlock(x, y, z);
        if (block == null || block == Blocks.air) {
            return null;
        }
        World fakeWorld = level.getOrCreateFakeWorld();

        AxisAlignedBB mergedBounds = resolveCollisionBounds(level, block, x, y, z, fakeWorld);
        if (mergedBounds != null) {
            return mergedBounds;
        }

        try {
            AxisAlignedBB selectedBounds = block.getSelectedBoundingBoxFromPool(fakeWorld, x, y, z);
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
            block.setBlockBoundsBasedOnState(fakeWorld != null ? fakeWorld : level, x, y, z);
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
            World fakeWorld = level.getOrCreateFakeWorld();
            block.setBlockBoundsBasedOnState(fakeWorld, x, y, z);
            AxisAlignedBB selectedBounds = block.getSelectedBoundingBoxFromPool(fakeWorld, x, y, z);
            if (selectedBounds != null && isNonEmpty(selectedBounds)) {
                return copyOf(selectedBounds);
            }
        } catch (Throwable ignored) {}

        return resolveWorldBounds(level, x, y, z);
    }

    @Nullable
    public static AxisAlignedBB resolveRayHitBounds(GuidebookLevel level, int x, int y, int z, Vec3 rayStart,
        Vec3 rayEnd) {
        Block block = level.getBlock(x, y, z);
        if (block == null || block == Blocks.air || rayStart == null || rayEnd == null) {
            return null;
        }

        List<AxisAlignedBB> collisionBounds = collectCollisionBounds(level, block, x, y, z);
        AxisAlignedBB bestBounds = resolveNearestRayHitBounds(collisionBounds, rayStart, rayEnd);

        if (bestBounds != null) {
            return copyOf(bestBounds);
        }
        return resolveSelectedBounds(level, x, y, z);
    }

    @Nullable
    static AxisAlignedBB resolveNearestRayHitBounds(List<AxisAlignedBB> collisionBoxes, Vec3 rayStart, Vec3 rayEnd) {
        if (collisionBoxes == null || rayStart == null || rayEnd == null) {
            return null;
        }
        AxisAlignedBB bestBounds = null;
        double bestDistanceSq = Double.POSITIVE_INFINITY;
        for (AxisAlignedBB collisionBox : collisionBoxes) {
            if (collisionBox == null || !isNonEmpty(collisionBox)) {
                continue;
            }
            var intercept = collisionBox.calculateIntercept(rayStart, rayEnd);
            if (intercept == null || intercept.hitVec == null) {
                continue;
            }
            double distanceSq = intercept.hitVec.squareDistanceTo(rayStart);
            if (distanceSq < bestDistanceSq) {
                bestDistanceSq = distanceSq;
                bestBounds = collisionBox;
            }
        }
        return bestBounds != null ? copyOf(bestBounds) : null;
    }

    @Nullable
    public static AxisAlignedBB resolveCollisionBounds(GuidebookLevel level, Block block, int x, int y, int z) {
        return resolveCollisionBounds(level, block, x, y, z, null);
    }

    @Nullable
    public static AxisAlignedBB resolveCollisionBounds(GuidebookLevel level, Block block, int x, int y, int z,
        @Nullable World fakeWorld) {
        try {
            return mergeCollisionBounds(collectCollisionBounds(level, block, x, y, z, fakeWorld));
        } catch (Throwable ignored) {
            return null;
        }
    }

    public static List<AxisAlignedBB> collectCollisionBounds(GuidebookLevel level, Block block, int x, int y, int z) {
        return collectCollisionBounds(level, block, x, y, z, null);
    }

    public static List<AxisAlignedBB> collectCollisionBounds(GuidebookLevel level, Block block, int x, int y, int z,
        @Nullable World fakeWorld) {
        List<AxisAlignedBB> collisionBoxes = new ArrayList<>();
        AxisAlignedBB fullBlockBounds = AxisAlignedBB.getBoundingBox(x, y, z, x + 1d, y + 1d, z + 1d);
        block.addCollisionBoxesToList(
            fakeWorld != null ? fakeWorld : level.getOrCreateFakeWorld(),
            x,
            y,
            z,
            fullBlockBounds,
            collisionBoxes,
            null);
        return collisionBoxes;
    }

    @Nullable
    public static AxisAlignedBB mergeCollisionBounds(List<AxisAlignedBB> collisionBoxes) {
        AxisAlignedBB merged = null;
        for (AxisAlignedBB collisionBox : collisionBoxes) {
            if (collisionBox == null || !isNonEmpty(collisionBox)) {
                continue;
            }
            merged = merged == null ? copyOf(collisionBox) : merged.func_111270_a(collisionBox);
        }
        return merged;
    }

    public static boolean isNonEmpty(AxisAlignedBB bounds) {
        return bounds != null && bounds.maxX > bounds.minX && bounds.maxY > bounds.minY && bounds.maxZ > bounds.minZ;
    }

    public static AxisAlignedBB copyOf(AxisAlignedBB bounds) {
        return AxisAlignedBB
            .getBoundingBox(bounds.minX, bounds.minY, bounds.minZ, bounds.maxX, bounds.maxY, bounds.maxZ);
    }
}
