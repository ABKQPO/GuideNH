package com.hfstudio.guidenh.guide.scene.support;

import javax.annotation.Nullable;

import net.minecraft.entity.Entity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;

public final class GuideEntityRayPicker {

    private GuideEntityRayPicker() {}

    @Nullable
    public static Hit pick(Iterable<Entity> entities, Vec3 rayStart, Vec3 rayEnd, @Nullable Integer visibleLayerY) {
        if (entities == null || rayStart == null || rayEnd == null) {
            return null;
        }

        Hit bestHit = null;
        double bestDistanceSq = Double.POSITIVE_INFINITY;
        for (Entity entity : entities) {
            if (entity == null || entity.isDead || entity.boundingBox == null) {
                continue;
            }

            AxisAlignedBB bounds = entity.boundingBox;
            if (visibleLayerY != null && !intersectsVisibleLayer(bounds, visibleLayerY.intValue())) {
                continue;
            }

            MovingObjectPosition intercept = bounds.calculateIntercept(rayStart, rayEnd);
            if (intercept == null || intercept.hitVec == null) {
                continue;
            }

            double distanceSq = intercept.hitVec.squareDistanceTo(rayStart);
            if (distanceSq < bestDistanceSq) {
                bestDistanceSq = distanceSq;
                MovingObjectPosition hitResult = new MovingObjectPosition(entity, intercept.hitVec);
                hitResult.sideHit = intercept.sideHit;
                hitResult.hitInfo = intercept.hitInfo;
                bestHit = new Hit(entity, copyOf(bounds), hitResult, distanceSq);
            }
        }

        return bestHit;
    }

    private static boolean intersectsVisibleLayer(AxisAlignedBB bounds, int visibleLayerY) {
        return bounds.maxY > visibleLayerY && bounds.minY < visibleLayerY + 1.0D;
    }

    private static AxisAlignedBB copyOf(AxisAlignedBB bounds) {
        return AxisAlignedBB
            .getBoundingBox(bounds.minX, bounds.minY, bounds.minZ, bounds.maxX, bounds.maxY, bounds.maxZ);
    }

    public static final class Hit {

        private final Entity entity;
        private final AxisAlignedBB bounds;
        private final MovingObjectPosition hitResult;
        private final double distanceSq;

        private Hit(Entity entity, AxisAlignedBB bounds, MovingObjectPosition hitResult, double distanceSq) {
            this.entity = entity;
            this.bounds = bounds;
            this.hitResult = hitResult;
            this.distanceSq = distanceSq;
        }

        public Entity getEntity() {
            return entity;
        }

        public AxisAlignedBB getBounds() {
            return bounds;
        }

        public MovingObjectPosition getHitResult() {
            return hitResult;
        }

        public double getDistanceSq() {
            return distanceSq;
        }
    }
}
