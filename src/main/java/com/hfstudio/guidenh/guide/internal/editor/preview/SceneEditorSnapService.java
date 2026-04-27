package com.hfstudio.guidenh.guide.internal.editor.preview;

import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.util.AxisAlignedBB;

import org.joml.Vector3f;

import com.hfstudio.guidenh.guide.document.LytRect;
import com.hfstudio.guidenh.guide.scene.CameraSettings;
import com.hfstudio.guidenh.guide.scene.level.GuidebookLevel;
import com.hfstudio.guidenh.guide.scene.support.GuideBlockBoundsResolver;

public final class SceneEditorSnapService {

    private static final float DEFAULT_SNAP_DISTANCE = 0.2f;
    private final java.util.ArrayList<BlockBounds> blockBoundsScratch = new java.util.ArrayList<>(16);

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

        float bestDistanceSq = Float.POSITIVE_INFINITY;
        Vector3f bestPoint = null;
        for (int[] pos : level.getFilledBlocks()) {
            List<BlockBounds> boundsList = getBlockBounds(level, pos[0], pos[1], pos[2]);
            if (boundsList == null) {
                continue;
            }
            for (BlockBounds bounds : boundsList) {
                for (SnapCandidate candidate : collectSnapCandidates(bounds, desired, snapModes)) {
                    float maxDistanceSq = snapDistance * snapDistance
                        * candidate.distanceMultiplier
                        * candidate.distanceMultiplier;
                    float distanceSq = squaredDistance(
                        candidate.point.x,
                        candidate.point.y,
                        candidate.point.z,
                        x,
                        y,
                        z);
                    if (distanceSq <= maxDistanceSq && distanceSq <= bestDistanceSq) {
                        bestDistanceSq = distanceSq;
                        if (bestPoint == null) {
                            bestPoint = candidate.point;
                        } else {
                            bestPoint.set(candidate.point);
                        }
                    }
                }
            }
        }
        return bestPoint != null ? bestPoint : desired;
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

        float bestDistanceSq = Float.POSITIVE_INFINITY;
        Vector3f bestPoint = null;
        for (int[] pos : level.getFilledBlocks()) {
            List<BlockBounds> boundsList = getBlockBounds(level, pos[0], pos[1], pos[2]);
            if (boundsList == null) {
                continue;
            }
            for (BlockBounds bounds : boundsList) {
                for (SnapCandidate freeCandidate : collectSnapCandidates(bounds, desired, snapModes)) {
                    Vector3f candidate = applyConstraint(
                        freeCandidate.point,
                        lockX,
                        lockY,
                        lockZ,
                        fixedX,
                        fixedY,
                        fixedZ);
                    float maxDistanceSq = snapDistance * snapDistance
                        * freeCandidate.distanceMultiplier
                        * freeCandidate.distanceMultiplier;
                    float distanceSq = squaredDistance(
                        candidate.x,
                        candidate.y,
                        candidate.z,
                        desired.x,
                        desired.y,
                        desired.z);
                    if (distanceSq <= maxDistanceSq && distanceSq <= bestDistanceSq) {
                        bestDistanceSq = distanceSq;
                        if (bestPoint == null) {
                            bestPoint = candidate;
                        } else {
                            bestPoint.set(candidate);
                        }
                    }
                }
            }
        }
        return bestPoint != null ? bestPoint : desired;
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
        float bestDesiredDistanceSq = Float.POSITIVE_INFINITY;
        float bestRayDistanceSq = Float.POSITIVE_INFINITY;
        float bestRayT = Float.POSITIVE_INFINITY;
        Vector3f bestPoint = null;
        for (int[] pos : level.getFilledBlocks()) {
            List<BlockBounds> boundsList = getBlockBounds(level, pos[0], pos[1], pos[2]);
            if (boundsList == null) {
                continue;
            }
            for (BlockBounds bounds : boundsList) {
                for (SnapCandidate candidate : collectSnapCandidates(bounds, desired, snapModes)) {
                    float maxDistanceSq = snapDistance * snapDistance
                        * candidate.distanceMultiplier
                        * candidate.distanceMultiplier;
                    RayCandidateMetrics metrics = measurePointToRay(ray, candidate.point);
                    if (metrics.rayT < -0.1f || metrics.distanceSq > maxDistanceSq) {
                        continue;
                    }
                    float desiredDistanceSq = squaredDistance(
                        candidate.point.x,
                        candidate.point.y,
                        candidate.point.z,
                        desired.x,
                        desired.y,
                        desired.z);
                    if (desiredDistanceSq < bestDesiredDistanceSq - 1e-6f || Math
                        .abs(desiredDistanceSq - bestDesiredDistanceSq) <= 1e-6f
                        && (metrics.distanceSq < bestRayDistanceSq - 1e-6f
                            || Math.abs(metrics.distanceSq - bestRayDistanceSq) <= 1e-6f && metrics.rayT < bestRayT)) {
                        bestDesiredDistanceSq = desiredDistanceSq;
                        bestRayDistanceSq = metrics.distanceSq;
                        bestRayT = metrics.rayT;
                        if (bestPoint == null) {
                            bestPoint = new Vector3f(candidate.point);
                        } else {
                            bestPoint.set(candidate.point);
                        }
                    }
                }
            }
        }
        return bestPoint != null ? bestPoint
            : snapFreePoint(level, desired.x, desired.y, desired.z, snapEnabled, snapModes, snapDistance);
    }

    @Nullable
    private List<BlockBounds> getBlockBounds(GuidebookLevel level, int blockX, int blockY, int blockZ) {
        blockBoundsScratch.clear();

        AxisAlignedBB blockBounds = GuideBlockBoundsResolver.resolveWorldBounds(level, blockX, blockY, blockZ);
        if (blockBounds == null) {
            return null;
        }
        blockBoundsScratch.add(
            new BlockBounds(
                (float) blockBounds.minX,
                (float) blockBounds.minY,
                (float) blockBounds.minZ,
                (float) blockBounds.maxX,
                (float) blockBounds.maxY,
                (float) blockBounds.maxZ));
        return blockBoundsScratch;
    }

    private List<SnapCandidate> collectSnapCandidates(BlockBounds bounds, Vector3f desired,
        SceneEditorSnapModes snapModes) {
        List<SnapCandidate> candidates = new java.util.ArrayList<>();
        if (snapModes.isPointEnabled()) {
            for (Vector3f point : bounds.cornerPoints()) {
                candidates.add(new SnapCandidate(point, 3f));
            }
        }
        if (snapModes.isLineEnabled()) {
            for (Vector3f point : bounds.edgePointsFor(desired)) {
                candidates.add(new SnapCandidate(point, 2f));
            }
        }
        if (snapModes.isFaceEnabled()) {
            for (Vector3f point : bounds.facePointsFor(desired)) {
                candidates.add(new SnapCandidate(point, 1f));
            }
        }
        if (snapModes.isCenterEnabled()) {
            candidates.add(new SnapCandidate(bounds.centerPoint(), 2f));
        }
        return candidates;
    }

    private Vector3f applyConstraint(Vector3f candidate, boolean lockX, boolean lockY, boolean lockZ, float fixedX,
        float fixedY, float fixedZ) {
        return new Vector3f(lockX ? fixedX : candidate.x, lockY ? fixedY : candidate.y, lockZ ? fixedZ : candidate.z);
    }

    private float clamp(float value, float min, float max) {
        if (value < min) {
            return min;
        }
        if (value > max) {
            return max;
        }
        return value;
    }

    private float squaredDistance(float ax, float ay, float az, float bx, float by, float bz) {
        float dx = ax - bx;
        float dy = ay - by;
        float dz = az - bz;
        return dx * dx + dy * dy + dz * dz;
    }

    private Vector3f projectToSegment(Vector3f desired, Vector3f from, Vector3f to) {
        float dx = to.x - from.x;
        float dy = to.y - from.y;
        float dz = to.z - from.z;
        float lengthSq = dx * dx + dy * dy + dz * dz;
        if (lengthSq <= 1e-6f) {
            return new Vector3f(from);
        }
        float t = ((desired.x - from.x) * dx + (desired.y - from.y) * dy + (desired.z - from.z) * dz) / lengthSq;
        if (t < 0f) {
            t = 0f;
        } else if (t > 1f) {
            t = 1f;
        }
        return new Vector3f(from.x + dx * t, from.y + dy * t, from.z + dz * t);
    }

    private RayCandidateMetrics measurePointToRay(float[] ray, Vector3f point) {
        float vx = point.x - ray[0];
        float vy = point.y - ray[1];
        float vz = point.z - ray[2];
        float rayT = vx * ray[3] + vy * ray[4] + vz * ray[5];
        float closestX = ray[0] + ray[3] * rayT;
        float closestY = ray[1] + ray[4] * rayT;
        float closestZ = ray[2] + ray[5] * rayT;
        float distanceSq = squaredDistance(point.x, point.y, point.z, closestX, closestY, closestZ);
        return new RayCandidateMetrics(distanceSq, rayT);
    }

    private static final class SnapCandidate {

        private final Vector3f point;
        private final float distanceMultiplier;

        private SnapCandidate(Vector3f point, float distanceMultiplier) {
            this.point = point;
            this.distanceMultiplier = distanceMultiplier;
        }
    }

    private static final class RayCandidateMetrics {

        private final float distanceSq;
        private final float rayT;

        private RayCandidateMetrics(float distanceSq, float rayT) {
            this.distanceSq = distanceSq;
            this.rayT = rayT;
        }
    }

    private static final class BlockBounds {

        private final float minX;
        private final float minY;
        private final float minZ;
        private final float maxX;
        private final float maxY;
        private final float maxZ;

        private BlockBounds(float minX, float minY, float minZ, float maxX, float maxY, float maxZ) {
            this.minX = minX;
            this.minY = minY;
            this.minZ = minZ;
            this.maxX = maxX;
            this.maxY = maxY;
            this.maxZ = maxZ;
        }

        private List<Vector3f> cornerPoints() {
            List<Vector3f> points = new java.util.ArrayList<>(8);
            for (float x : new float[] { minX, maxX }) {
                for (float y : new float[] { minY, maxY }) {
                    for (float z : new float[] { minZ, maxZ }) {
                        points.add(new Vector3f(x, y, z));
                    }
                }
            }
            return points;
        }

        private List<Vector3f> facePointsFor(Vector3f desired) {
            List<Vector3f> points = new java.util.ArrayList<>(6);
            points.add(new Vector3f(minX, clamp(desired.y, minY, maxY), clamp(desired.z, minZ, maxZ)));
            points.add(new Vector3f(maxX, clamp(desired.y, minY, maxY), clamp(desired.z, minZ, maxZ)));
            points.add(new Vector3f(clamp(desired.x, minX, maxX), minY, clamp(desired.z, minZ, maxZ)));
            points.add(new Vector3f(clamp(desired.x, minX, maxX), maxY, clamp(desired.z, minZ, maxZ)));
            points.add(new Vector3f(clamp(desired.x, minX, maxX), clamp(desired.y, minY, maxY), minZ));
            points.add(new Vector3f(clamp(desired.x, minX, maxX), clamp(desired.y, minY, maxY), maxZ));
            return points;
        }

        private List<Vector3f> edgePointsFor(Vector3f desired) {
            List<Vector3f> points = new java.util.ArrayList<>(12);
            for (float y : new float[] { minY, maxY }) {
                for (float z : new float[] { minZ, maxZ }) {
                    points.add(projectToSegmentStatic(desired, new Vector3f(minX, y, z), new Vector3f(maxX, y, z)));
                }
            }
            for (float x : new float[] { minX, maxX }) {
                for (float z : new float[] { minZ, maxZ }) {
                    points.add(projectToSegmentStatic(desired, new Vector3f(x, minY, z), new Vector3f(x, maxY, z)));
                }
            }
            for (float x : new float[] { minX, maxX }) {
                for (float y : new float[] { minY, maxY }) {
                    points.add(projectToSegmentStatic(desired, new Vector3f(x, y, minZ), new Vector3f(x, y, maxZ)));
                }
            }
            return points;
        }

        private Vector3f centerPoint() {
            return new Vector3f((minX + maxX) * 0.5f, (minY + maxY) * 0.5f, (minZ + maxZ) * 0.5f);
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

        private static Vector3f projectToSegmentStatic(Vector3f desired, Vector3f from, Vector3f to) {
            float dx = to.x - from.x;
            float dy = to.y - from.y;
            float dz = to.z - from.z;
            float lengthSq = dx * dx + dy * dy + dz * dz;
            if (lengthSq <= 1e-6f) {
                return new Vector3f(from);
            }
            float t = ((desired.x - from.x) * dx + (desired.y - from.y) * dy + (desired.z - from.z) * dz) / lengthSq;
            if (t < 0f) {
                t = 0f;
            } else if (t > 1f) {
                t = 1f;
            }
            return new Vector3f(from.x + dx * t, from.y + dy * t, from.z + dz * t);
        }
    }
}
