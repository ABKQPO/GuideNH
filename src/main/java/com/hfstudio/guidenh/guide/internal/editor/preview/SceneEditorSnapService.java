package com.hfstudio.guidenh.guide.internal.editor.preview;

import javax.annotation.Nullable;

import net.minecraft.entity.Entity;
import net.minecraft.util.AxisAlignedBB;

import org.joml.Vector3f;

import com.hfstudio.guidenh.guide.document.LytRect;
import com.hfstudio.guidenh.guide.scene.CameraSettings;
import com.hfstudio.guidenh.guide.scene.level.GuidebookLevel;
import com.hfstudio.guidenh.guide.scene.support.GuideBlockBoundsResolver;

public final class SceneEditorSnapService {

    private static final float DEFAULT_SNAP_DISTANCE = 0.2f;
    private final BlockBounds boundsScratch = new BlockBounds();

    public Vector3f snapBlockPosition(float x, float y, float z) {
        return new Vector3f(Math.round(x), Math.round(y), Math.round(z));
    }

    public Vector3f snapFreePoint(@Nullable GuidebookLevel level, float x, float y, float z, boolean snapEnabled) {
        return snapFreePoint(level, x, y, z, snapEnabled, SceneEditorSnapModes.defaultModes(), DEFAULT_SNAP_DISTANCE);
    }

    public Vector3f snapFreePoint(@Nullable GuidebookLevel level, float x, float y, float z, boolean snapEnabled,
        SceneEditorSnapModes snapModes) {
        return snapFreePoint(level, x, y, z, snapEnabled, snapModes, DEFAULT_SNAP_DISTANCE);
    }

    public Vector3f snapFreePoint(@Nullable GuidebookLevel level, float x, float y, float z, boolean snapEnabled,
        float snapDistance) {
        return snapFreePoint(level, x, y, z, snapEnabled, SceneEditorSnapModes.defaultModes(), snapDistance);
    }

    public Vector3f snapFreePoint(@Nullable GuidebookLevel level, float x, float y, float z, boolean snapEnabled,
        SceneEditorSnapModes snapModes, float snapDistance) {
        Vector3f desired = new Vector3f(x, y, z);
        if (!snapEnabled || level == null || level.isEmpty() || snapDistance <= 0f || !snapModes.hasEnabledMode()) {
            return desired;
        }

        FreeSnapAccumulator accumulator = new FreeSnapAccumulator(x, y, z, snapDistance);
        visitLevelSnapCandidates(level, x, y, z, snapModes, accumulator);
        return accumulator.toVectorOr(desired);
    }

    public Vector3f snapConstrainedPoint(@Nullable GuidebookLevel level, float x, float y, float z, boolean snapEnabled,
        boolean lockX, boolean lockY, boolean lockZ, float fixedX, float fixedY, float fixedZ) {
        return snapConstrainedPoint(
            level,
            x,
            y,
            z,
            snapEnabled,
            SceneEditorSnapModes.defaultModes(),
            DEFAULT_SNAP_DISTANCE,
            lockX,
            lockY,
            lockZ,
            fixedX,
            fixedY,
            fixedZ);
    }

    public Vector3f snapConstrainedPoint(@Nullable GuidebookLevel level, float x, float y, float z, boolean snapEnabled,
        SceneEditorSnapModes snapModes, boolean lockX, boolean lockY, boolean lockZ, float fixedX, float fixedY,
        float fixedZ) {
        return snapConstrainedPoint(
            level,
            x,
            y,
            z,
            snapEnabled,
            snapModes,
            DEFAULT_SNAP_DISTANCE,
            lockX,
            lockY,
            lockZ,
            fixedX,
            fixedY,
            fixedZ);
    }

    public Vector3f snapConstrainedPoint(@Nullable GuidebookLevel level, float x, float y, float z, boolean snapEnabled,
        float snapDistance, boolean lockX, boolean lockY, boolean lockZ, float fixedX, float fixedY, float fixedZ) {
        return snapConstrainedPoint(
            level,
            x,
            y,
            z,
            snapEnabled,
            SceneEditorSnapModes.defaultModes(),
            snapDistance,
            lockX,
            lockY,
            lockZ,
            fixedX,
            fixedY,
            fixedZ);
    }

    public Vector3f snapConstrainedPoint(@Nullable GuidebookLevel level, float x, float y, float z, boolean snapEnabled,
        SceneEditorSnapModes snapModes, float snapDistance, boolean lockX, boolean lockY, boolean lockZ, float fixedX,
        float fixedY, float fixedZ) {
        Vector3f desired = new Vector3f(lockX ? fixedX : x, lockY ? fixedY : y, lockZ ? fixedZ : z);
        if (!snapEnabled || level == null || level.isEmpty() || snapDistance <= 0f || !snapModes.hasEnabledMode()) {
            return desired;
        }
        if (!lockX && !lockY && !lockZ) {
            return snapFreePoint(level, desired.x, desired.y, desired.z, true, snapModes, snapDistance);
        }

        ConstrainedSnapAccumulator accumulator = new ConstrainedSnapAccumulator(
            desired.x,
            desired.y,
            desired.z,
            snapDistance,
            lockX,
            lockY,
            lockZ,
            fixedX,
            fixedY,
            fixedZ);
        visitLevelSnapCandidates(level, desired.x, desired.y, desired.z, snapModes, accumulator);
        return accumulator.toVectorOr(desired);
    }

    public Vector3f snapFreePointToRay(@Nullable GuidebookLevel level, CameraSettings camera, LytRect viewport,
        int mouseX, int mouseY, Vector3f fallbackPoint, boolean snapEnabled, SceneEditorSnapModes snapModes) {
        return snapFreePointToRay(
            level,
            camera,
            viewport,
            mouseX,
            mouseY,
            fallbackPoint,
            snapEnabled,
            snapModes,
            DEFAULT_SNAP_DISTANCE);
    }

    public Vector3f snapFreePointToRay(@Nullable GuidebookLevel level, CameraSettings camera, LytRect viewport,
        int mouseX, int mouseY, Vector3f fallbackPoint, boolean snapEnabled, SceneEditorSnapModes snapModes,
        float snapDistance) {
        Vector3f desired = new Vector3f(fallbackPoint);
        if (!snapEnabled || level == null || level.isEmpty() || snapDistance <= 0f || !snapModes.hasEnabledMode()) {
            return desired;
        }

        float relX = mouseX - (viewport.x() + viewport.width() * 0.5f);
        float relY = mouseY - (viewport.y() + viewport.height() * 0.5f);
        float[] ray = camera.screenToWorldRay(relX, relY);
        RaySnapAccumulator accumulator = new RaySnapAccumulator(ray, desired.x, desired.y, desired.z, snapDistance);
        visitLevelSnapCandidates(level, desired.x, desired.y, desired.z, snapModes, accumulator);
        return accumulator.hasBestPoint() ? accumulator.toVector()
            : snapFreePoint(level, desired.x, desired.y, desired.z, snapEnabled, snapModes, snapDistance);
    }

    private boolean resolveBlockBounds(GuidebookLevel level, int blockX, int blockY, int blockZ, BlockBounds dest) {
        AxisAlignedBB blockBounds = GuideBlockBoundsResolver.resolveWorldBounds(level, blockX, blockY, blockZ);
        if (blockBounds == null) {
            return false;
        }
        dest.set(
            (float) blockBounds.minX,
            (float) blockBounds.minY,
            (float) blockBounds.minZ,
            (float) blockBounds.maxX,
            (float) blockBounds.maxY,
            (float) blockBounds.maxZ);
        return true;
    }

    private void visitLevelSnapCandidates(GuidebookLevel level, float desiredX, float desiredY, float desiredZ,
        SceneEditorSnapModes snapModes, SnapCandidateVisitor visitor) {
        BlockBounds bounds = boundsScratch;
        for (int[] pos : level.getFilledBlocks()) {
            if (!resolveBlockBounds(level, pos[0], pos[1], pos[2], bounds)) {
                continue;
            }
            visitSnapCandidates(bounds, desiredX, desiredY, desiredZ, snapModes, visitor);
        }
        for (Entity entity : level.getEntities()) {
            if (entity == null || entity.isDead || entity.boundingBox == null) {
                continue;
            }
            AxisAlignedBB entityBounds = entity.boundingBox;
            bounds.set(
                (float) entityBounds.minX,
                (float) entityBounds.minY,
                (float) entityBounds.minZ,
                (float) entityBounds.maxX,
                (float) entityBounds.maxY,
                (float) entityBounds.maxZ);
            visitSnapCandidates(bounds, desiredX, desiredY, desiredZ, snapModes, visitor);
        }
    }

    private void visitSnapCandidates(BlockBounds bounds, float desiredX, float desiredY, float desiredZ,
        SceneEditorSnapModes snapModes, SnapCandidateVisitor visitor) {
        if (snapModes.isPointEnabled()) {
            visitor.visit(bounds.minX, bounds.minY, bounds.minZ, 3f);
            visitor.visit(bounds.minX, bounds.minY, bounds.maxZ, 3f);
            visitor.visit(bounds.minX, bounds.maxY, bounds.minZ, 3f);
            visitor.visit(bounds.minX, bounds.maxY, bounds.maxZ, 3f);
            visitor.visit(bounds.maxX, bounds.minY, bounds.minZ, 3f);
            visitor.visit(bounds.maxX, bounds.minY, bounds.maxZ, 3f);
            visitor.visit(bounds.maxX, bounds.maxY, bounds.minZ, 3f);
            visitor.visit(bounds.maxX, bounds.maxY, bounds.maxZ, 3f);
        }
        if (snapModes.isLineEnabled()) {
            visitProjectedSegment(
                visitor,
                desiredX,
                desiredY,
                desiredZ,
                bounds.minX,
                bounds.minY,
                bounds.minZ,
                bounds.maxX,
                bounds.minY,
                bounds.minZ,
                2f);
            visitProjectedSegment(
                visitor,
                desiredX,
                desiredY,
                desiredZ,
                bounds.minX,
                bounds.minY,
                bounds.maxZ,
                bounds.maxX,
                bounds.minY,
                bounds.maxZ,
                2f);
            visitProjectedSegment(
                visitor,
                desiredX,
                desiredY,
                desiredZ,
                bounds.minX,
                bounds.maxY,
                bounds.minZ,
                bounds.maxX,
                bounds.maxY,
                bounds.minZ,
                2f);
            visitProjectedSegment(
                visitor,
                desiredX,
                desiredY,
                desiredZ,
                bounds.minX,
                bounds.maxY,
                bounds.maxZ,
                bounds.maxX,
                bounds.maxY,
                bounds.maxZ,
                2f);
            visitProjectedSegment(
                visitor,
                desiredX,
                desiredY,
                desiredZ,
                bounds.minX,
                bounds.minY,
                bounds.minZ,
                bounds.minX,
                bounds.maxY,
                bounds.minZ,
                2f);
            visitProjectedSegment(
                visitor,
                desiredX,
                desiredY,
                desiredZ,
                bounds.minX,
                bounds.minY,
                bounds.maxZ,
                bounds.minX,
                bounds.maxY,
                bounds.maxZ,
                2f);
            visitProjectedSegment(
                visitor,
                desiredX,
                desiredY,
                desiredZ,
                bounds.maxX,
                bounds.minY,
                bounds.minZ,
                bounds.maxX,
                bounds.maxY,
                bounds.minZ,
                2f);
            visitProjectedSegment(
                visitor,
                desiredX,
                desiredY,
                desiredZ,
                bounds.maxX,
                bounds.minY,
                bounds.maxZ,
                bounds.maxX,
                bounds.maxY,
                bounds.maxZ,
                2f);
            visitProjectedSegment(
                visitor,
                desiredX,
                desiredY,
                desiredZ,
                bounds.minX,
                bounds.minY,
                bounds.minZ,
                bounds.minX,
                bounds.minY,
                bounds.maxZ,
                2f);
            visitProjectedSegment(
                visitor,
                desiredX,
                desiredY,
                desiredZ,
                bounds.minX,
                bounds.maxY,
                bounds.minZ,
                bounds.minX,
                bounds.maxY,
                bounds.maxZ,
                2f);
            visitProjectedSegment(
                visitor,
                desiredX,
                desiredY,
                desiredZ,
                bounds.maxX,
                bounds.minY,
                bounds.minZ,
                bounds.maxX,
                bounds.minY,
                bounds.maxZ,
                2f);
            visitProjectedSegment(
                visitor,
                desiredX,
                desiredY,
                desiredZ,
                bounds.maxX,
                bounds.maxY,
                bounds.minZ,
                bounds.maxX,
                bounds.maxY,
                bounds.maxZ,
                2f);
        }
        if (snapModes.isFaceEnabled()) {
            visitor.visit(
                bounds.minX,
                clamp(desiredY, bounds.minY, bounds.maxY),
                clamp(desiredZ, bounds.minZ, bounds.maxZ),
                1f);
            visitor.visit(
                bounds.maxX,
                clamp(desiredY, bounds.minY, bounds.maxY),
                clamp(desiredZ, bounds.minZ, bounds.maxZ),
                1f);
            visitor.visit(
                clamp(desiredX, bounds.minX, bounds.maxX),
                bounds.minY,
                clamp(desiredZ, bounds.minZ, bounds.maxZ),
                1f);
            visitor.visit(
                clamp(desiredX, bounds.minX, bounds.maxX),
                bounds.maxY,
                clamp(desiredZ, bounds.minZ, bounds.maxZ),
                1f);
            visitor.visit(
                clamp(desiredX, bounds.minX, bounds.maxX),
                clamp(desiredY, bounds.minY, bounds.maxY),
                bounds.minZ,
                1f);
            visitor.visit(
                clamp(desiredX, bounds.minX, bounds.maxX),
                clamp(desiredY, bounds.minY, bounds.maxY),
                bounds.maxZ,
                1f);
        }
        if (snapModes.isCenterEnabled()) {
            visitor.visit(
                (bounds.minX + bounds.maxX) * 0.5f,
                (bounds.minY + bounds.maxY) * 0.5f,
                (bounds.minZ + bounds.maxZ) * 0.5f,
                2f);
        }
    }

    private static void visitProjectedSegment(SnapCandidateVisitor visitor, float desiredX, float desiredY,
        float desiredZ, float fromX, float fromY, float fromZ, float toX, float toY, float toZ,
        float distanceMultiplier) {
        float dx = toX - fromX;
        float dy = toY - fromY;
        float dz = toZ - fromZ;
        float lengthSq = dx * dx + dy * dy + dz * dz;
        if (lengthSq <= 1e-6f) {
            visitor.visit(fromX, fromY, fromZ, distanceMultiplier);
            return;
        }
        float t = ((desiredX - fromX) * dx + (desiredY - fromY) * dy + (desiredZ - fromZ) * dz) / lengthSq;
        if (t < 0f) {
            t = 0f;
        } else if (t > 1f) {
            t = 1f;
        }
        visitor.visit(fromX + dx * t, fromY + dy * t, fromZ + dz * t, distanceMultiplier);
    }

    private static float clamp(float value, float min, float max) {
        if (value < min) {
            return min;
        }
        if (value > max) {
            return max;
        }
        return value;
    }

    private static float squaredDistance(float ax, float ay, float az, float bx, float by, float bz) {
        float dx = ax - bx;
        float dy = ay - by;
        float dz = az - bz;
        return dx * dx + dy * dy + dz * dz;
    }

    private interface SnapCandidateVisitor {

        void visit(float x, float y, float z, float distanceMultiplier);
    }

    private static final class FreeSnapAccumulator implements SnapCandidateVisitor {

        private final float desiredX;
        private final float desiredY;
        private final float desiredZ;
        private final float snapDistanceSq;
        private float bestDistanceSq = Float.POSITIVE_INFINITY;
        private boolean found;
        private float bestX;
        private float bestY;
        private float bestZ;

        private FreeSnapAccumulator(float desiredX, float desiredY, float desiredZ, float snapDistance) {
            this.desiredX = desiredX;
            this.desiredY = desiredY;
            this.desiredZ = desiredZ;
            this.snapDistanceSq = snapDistance * snapDistance;
        }

        @Override
        public void visit(float x, float y, float z, float distanceMultiplier) {
            float maxDistanceSq = snapDistanceSq * distanceMultiplier * distanceMultiplier;
            float distanceSq = squaredDistance(x, y, z, desiredX, desiredY, desiredZ);
            if (distanceSq > maxDistanceSq || distanceSq > bestDistanceSq) {
                return;
            }
            bestDistanceSq = distanceSq;
            bestX = x;
            bestY = y;
            bestZ = z;
            found = true;
        }

        private Vector3f toVectorOr(Vector3f fallback) {
            return found ? new Vector3f(bestX, bestY, bestZ) : fallback;
        }
    }

    private static final class ConstrainedSnapAccumulator implements SnapCandidateVisitor {

        private final float desiredX;
        private final float desiredY;
        private final float desiredZ;
        private final float snapDistanceSq;
        private final boolean lockX;
        private final boolean lockY;
        private final boolean lockZ;
        private final float fixedX;
        private final float fixedY;
        private final float fixedZ;
        private float bestDistanceSq = Float.POSITIVE_INFINITY;
        private boolean found;
        private float bestX;
        private float bestY;
        private float bestZ;

        private ConstrainedSnapAccumulator(float desiredX, float desiredY, float desiredZ, float snapDistance,
            boolean lockX, boolean lockY, boolean lockZ, float fixedX, float fixedY, float fixedZ) {
            this.desiredX = desiredX;
            this.desiredY = desiredY;
            this.desiredZ = desiredZ;
            this.snapDistanceSq = snapDistance * snapDistance;
            this.lockX = lockX;
            this.lockY = lockY;
            this.lockZ = lockZ;
            this.fixedX = fixedX;
            this.fixedY = fixedY;
            this.fixedZ = fixedZ;
        }

        @Override
        public void visit(float x, float y, float z, float distanceMultiplier) {
            float candidateX = lockX ? fixedX : x;
            float candidateY = lockY ? fixedY : y;
            float candidateZ = lockZ ? fixedZ : z;
            float maxDistanceSq = snapDistanceSq * distanceMultiplier * distanceMultiplier;
            float distanceSq = squaredDistance(candidateX, candidateY, candidateZ, desiredX, desiredY, desiredZ);
            if (distanceSq > maxDistanceSq || distanceSq > bestDistanceSq) {
                return;
            }
            bestDistanceSq = distanceSq;
            bestX = candidateX;
            bestY = candidateY;
            bestZ = candidateZ;
            found = true;
        }

        private Vector3f toVectorOr(Vector3f fallback) {
            return found ? new Vector3f(bestX, bestY, bestZ) : fallback;
        }
    }

    private static final class RaySnapAccumulator implements SnapCandidateVisitor {

        private final float[] ray;
        private final float desiredX;
        private final float desiredY;
        private final float desiredZ;
        private final float snapDistanceSq;
        private float bestDesiredDistanceSq = Float.POSITIVE_INFINITY;
        private float bestRayDistanceSq = Float.POSITIVE_INFINITY;
        private float bestRayT = Float.POSITIVE_INFINITY;
        private boolean found;
        private float bestX;
        private float bestY;
        private float bestZ;

        private RaySnapAccumulator(float[] ray, float desiredX, float desiredY, float desiredZ, float snapDistance) {
            this.ray = ray;
            this.desiredX = desiredX;
            this.desiredY = desiredY;
            this.desiredZ = desiredZ;
            this.snapDistanceSq = snapDistance * snapDistance;
        }

        @Override
        public void visit(float x, float y, float z, float distanceMultiplier) {
            float maxDistanceSq = snapDistanceSq * distanceMultiplier * distanceMultiplier;
            float vx = x - ray[0];
            float vy = y - ray[1];
            float vz = z - ray[2];
            float rayT = vx * ray[3] + vy * ray[4] + vz * ray[5];
            if (rayT < -0.1f) {
                return;
            }
            float closestX = ray[0] + ray[3] * rayT;
            float closestY = ray[1] + ray[4] * rayT;
            float closestZ = ray[2] + ray[5] * rayT;
            float rayDistanceSq = squaredDistance(x, y, z, closestX, closestY, closestZ);
            if (rayDistanceSq > maxDistanceSq) {
                return;
            }
            float desiredDistanceSq = squaredDistance(x, y, z, desiredX, desiredY, desiredZ);
            if (desiredDistanceSq < bestDesiredDistanceSq - 1e-6f
                || Math.abs(desiredDistanceSq - bestDesiredDistanceSq) <= 1e-6f
                    && (rayDistanceSq < bestRayDistanceSq - 1e-6f
                        || Math.abs(rayDistanceSq - bestRayDistanceSq) <= 1e-6f && rayT < bestRayT)) {
                bestDesiredDistanceSq = desiredDistanceSq;
                bestRayDistanceSq = rayDistanceSq;
                bestRayT = rayT;
                bestX = x;
                bestY = y;
                bestZ = z;
                found = true;
            }
        }

        private boolean hasBestPoint() {
            return found;
        }

        private Vector3f toVector() {
            return new Vector3f(bestX, bestY, bestZ);
        }
    }

    private static final class BlockBounds {

        private float minX;
        private float minY;
        private float minZ;
        private float maxX;
        private float maxY;
        private float maxZ;

        private void set(float minX, float minY, float minZ, float maxX, float maxY, float maxZ) {
            this.minX = minX;
            this.minY = minY;
            this.minZ = minZ;
            this.maxX = maxX;
            this.maxY = maxY;
            this.maxZ = maxZ;
        }
    }
}
