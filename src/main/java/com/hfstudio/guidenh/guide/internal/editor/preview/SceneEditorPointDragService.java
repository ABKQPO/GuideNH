package com.hfstudio.guidenh.guide.internal.editor.preview;

import java.util.UUID;

import javax.annotation.Nullable;

import org.joml.Vector3f;

import com.hfstudio.guidenh.guide.document.LytRect;
import com.hfstudio.guidenh.guide.internal.editor.gui.SceneEditorElementPropertyController;
import com.hfstudio.guidenh.guide.internal.editor.model.SceneEditorElementModel;
import com.hfstudio.guidenh.guide.internal.editor.model.SceneEditorElementType;
import com.hfstudio.guidenh.guide.scene.CameraSettings;
import com.hfstudio.guidenh.guide.scene.level.GuidebookLevel;

public final class SceneEditorPointDragService {

    private final SceneEditorSnapService snapService;
    private final float[] rayScratch = new float[6];
    private final Vector3f dragPointScratch = new Vector3f();
    private final Vector3f constrainedPointScratch = new Vector3f();
    private final Vector3f projectedOriginScratch = new Vector3f();
    private final Vector3f projectedAxisScratch = new Vector3f();
    private final Vector3f projectedWorldScratch = new Vector3f();

    public SceneEditorPointDragService(SceneEditorSnapService snapService) {
        this.snapService = snapService;
    }

    @Nullable
    public DragState beginCenterDrag(SceneEditorElementModel element, CameraSettings camera, LytRect viewport,
        int mouseX, int mouseY) {
        return beginHandleDrag(element, SceneEditorHandleOverlay.CENTER_HANDLE_ID, camera, viewport, mouseX, mouseY);
    }

    public boolean updateCenterDrag(SceneEditorElementPropertyController controller, GuidebookLevel level,
        CameraSettings camera, LytRect viewport, DragState state, int mouseX, int mouseY, boolean snapEnabled) {
        return updateCenterDrag(
            controller,
            level,
            camera,
            viewport,
            state,
            mouseX,
            mouseY,
            snapEnabled,
            SceneEditorSnapModes.defaultModes());
    }

    public boolean updateCenterDrag(SceneEditorElementPropertyController controller, GuidebookLevel level,
        CameraSettings camera, LytRect viewport, DragState state, int mouseX, int mouseY, boolean snapEnabled,
        SceneEditorSnapModes snapModes) {
        return updateHandleDrag(controller, level, camera, viewport, state, mouseX, mouseY, snapEnabled, snapModes);
    }

    public boolean supportsPointDrag(@Nullable SceneEditorElementModel element) {
        if (element == null) {
            return false;
        }
        SceneEditorElementType type = element.getType();
        return type == SceneEditorElementType.BLOCK || type == SceneEditorElementType.DIAMOND
            || type == SceneEditorElementType.LINE
            || type == SceneEditorElementType.BOX;
    }

    @Nullable
    public DragState beginHandleDrag(SceneEditorElementModel element, String handleId, CameraSettings camera,
        LytRect viewport, int mouseX, int mouseY) {
        if (!supportsPointDrag(element)) {
            return null;
        }
        SceneEditorElementType type = element.getType();
        if (type == SceneEditorElementType.BLOCK || type == SceneEditorElementType.DIAMOND) {
            return beginPointHandleDrag(element, handleId, camera);
        }
        if (type == SceneEditorElementType.LINE) {
            return beginLineHandleDrag(element, handleId, camera);
        }
        if (type == SceneEditorElementType.BOX) {
            return beginBoxCornerDrag(element, handleId, camera);
        }
        return null;
    }

    public boolean updateHandleDrag(SceneEditorElementPropertyController controller, GuidebookLevel level,
        CameraSettings camera, LytRect viewport, DragState state, int mouseX, int mouseY, boolean snapEnabled) {
        return updateHandleDrag(
            controller,
            level,
            camera,
            viewport,
            state,
            mouseX,
            mouseY,
            snapEnabled,
            SceneEditorSnapModes.defaultModes());
    }

    public boolean updateHandleDrag(SceneEditorElementPropertyController controller, GuidebookLevel level,
        CameraSettings camera, LytRect viewport, DragState state, int mouseX, int mouseY, boolean snapEnabled,
        SceneEditorSnapModes snapModes) {
        if (state.behavior == DragBehavior.POINT_HANDLE) {
            return updatePointHandleDrag(
                controller,
                level,
                camera,
                viewport,
                state,
                mouseX,
                mouseY,
                snapEnabled,
                snapModes);
        }
        Vector3f draggedPoint = projectMouseToPlane(
            camera,
            viewport,
            mouseX,
            mouseY,
            state.originPoint,
            state.planeNormal,
            dragPointScratch);
        if (draggedPoint == null) {
            return false;
        }
        if (state.behavior == DragBehavior.LINE_FROM) {
            Vector3f resolvedPoint = snapService
                .snapFreePointToRay(level, camera, viewport, mouseX, mouseY, draggedPoint, snapEnabled, snapModes);
            return controller.setPrimaryVector(state.elementId, resolvedPoint.x, resolvedPoint.y, resolvedPoint.z);
        }
        if (state.behavior == DragBehavior.LINE_TO) {
            Vector3f resolvedPoint = snapService
                .snapFreePointToRay(level, camera, viewport, mouseX, mouseY, draggedPoint, snapEnabled, snapModes);
            return controller.setSecondaryVector(state.elementId, resolvedPoint.x, resolvedPoint.y, resolvedPoint.z);
        }
        if (state.behavior == DragBehavior.BOX_CORNER && state.fixedPoint != null) {
            Vector3f resolvedPoint = snapService
                .snapFreePointToRay(level, camera, viewport, mouseX, mouseY, draggedPoint, snapEnabled, snapModes);
            float minX = Math.min(resolvedPoint.x, state.fixedPoint.x);
            float minY = Math.min(resolvedPoint.y, state.fixedPoint.y);
            float minZ = Math.min(resolvedPoint.z, state.fixedPoint.z);
            float maxX = Math.max(resolvedPoint.x, state.fixedPoint.x);
            float maxY = Math.max(resolvedPoint.y, state.fixedPoint.y);
            float maxZ = Math.max(resolvedPoint.z, state.fixedPoint.z);
            boolean changedPrimary = controller.setPrimaryVector(state.elementId, minX, minY, minZ);
            boolean changedSecondary = controller.setSecondaryVector(state.elementId, maxX, maxY, maxZ);
            return changedPrimary || changedSecondary;
        }
        return false;
    }

    @Nullable
    private DragState beginPointHandleDrag(SceneEditorElementModel element, String handleId, CameraSettings camera) {
        DragMode mode = DragMode.fromHandleId(handleId);
        if (mode == null) {
            return null;
        }
        Vector3f origin = new Vector3f(element.getPrimaryX(), element.getPrimaryY(), element.getPrimaryZ());
        Vector3f cameraForward = getCameraForward(camera);
        if (mode.isAxis()) {
            return DragState.pointHandle(element.getId(), element.getType(), origin, mode, mode.axisDirection(), null);
        }
        Vector3f planeNormal = mode == DragMode.CENTER ? cameraForward : mode.planeNormal();
        return DragState.pointHandle(element.getId(), element.getType(), origin, mode, null, planeNormal);
    }

    @Nullable
    private DragState beginLineHandleDrag(SceneEditorElementModel element, String handleId, CameraSettings camera) {
        Vector3f planeNormal = getCameraForward(camera);
        if (SceneEditorHandleOverlay.LINE_FROM_HANDLE_ID.equals(handleId)) {
            return DragState.lineFrom(
                element.getId(),
                new Vector3f(element.getPrimaryX(), element.getPrimaryY(), element.getPrimaryZ()),
                planeNormal);
        }
        if (SceneEditorHandleOverlay.LINE_TO_HANDLE_ID.equals(handleId)) {
            return DragState.lineTo(
                element.getId(),
                new Vector3f(element.getSecondaryX(), element.getSecondaryY(), element.getSecondaryZ()),
                planeNormal);
        }
        return null;
    }

    @Nullable
    private DragState beginBoxCornerDrag(SceneEditorElementModel element, String handleId, CameraSettings camera) {
        Vector3f movingCorner = getBoxCornerPoint(element, handleId);
        if (movingCorner == null) {
            return null;
        }
        String oppositeHandleId = getOppositeBoxHandleId(handleId);
        Vector3f oppositeCorner = getBoxCornerPoint(element, oppositeHandleId);
        if (oppositeCorner == null) {
            return null;
        }
        return DragState.boxCorner(element.getId(), movingCorner, oppositeCorner, getCameraForward(camera));
    }

    private boolean updatePointHandleDrag(SceneEditorElementPropertyController controller, GuidebookLevel level,
        CameraSettings camera, LytRect viewport, DragState state, int mouseX, int mouseY, boolean snapEnabled,
        SceneEditorSnapModes snapModes) {
        Vector3f draggedPoint = state.mode.isAxis()
            ? projectMouseToAxis(
                camera,
                viewport,
                mouseX,
                mouseY,
                state.originPoint,
                state.axisDirection,
                dragPointScratch)
            : projectMouseToPlane(camera, viewport, mouseX, mouseY, state.originPoint, state.planeNormal, dragPointScratch);
        if (draggedPoint == null) {
            return false;
        }

        Vector3f constrainedPoint = applyConstraint(draggedPoint, state.mode, state.originPoint, constrainedPointScratch);
        Vector3f resolvedPoint = state.elementType == SceneEditorElementType.BLOCK
            ? resolveBlockPoint(constrainedPoint, state.mode, state.originPoint)
            : state.mode == DragMode.CENTER ? snapService
                .snapFreePointToRay(level, camera, viewport, mouseX, mouseY, constrainedPoint, snapEnabled, snapModes)
                : resolveDiamondPoint(level, constrainedPoint, state.mode, state.originPoint, snapEnabled, snapModes);
        return controller.setPrimaryVector(state.elementId, resolvedPoint.x, resolvedPoint.y, resolvedPoint.z);
    }

    @Nullable
    private Vector3f projectMouseToPlane(CameraSettings camera, LytRect viewport, int mouseX, int mouseY,
        Vector3f planePoint, Vector3f planeNormal) {
        return projectMouseToPlane(camera, viewport, mouseX, mouseY, planePoint, planeNormal, new Vector3f());
    }

    @Nullable
    private Vector3f projectMouseToPlane(CameraSettings camera, LytRect viewport, int mouseX, int mouseY,
        Vector3f planePoint, Vector3f planeNormal, Vector3f dest) {
        float relX = mouseX - (viewport.x() + viewport.width() * 0.5f);
        float relY = mouseY - (viewport.y() + viewport.height() * 0.5f);
        float[] ray = camera.screenToWorldRay(relX, relY, rayScratch);
        float denominator = ray[3] * planeNormal.x + ray[4] * planeNormal.y + ray[5] * planeNormal.z;
        if (Math.abs(denominator) < 1e-6f) {
            return null;
        }

        float offsetX = planePoint.x - ray[0];
        float offsetY = planePoint.y - ray[1];
        float offsetZ = planePoint.z - ray[2];
        float distance = (offsetX * planeNormal.x + offsetY * planeNormal.y + offsetZ * planeNormal.z) / denominator;
        return dest.set(ray[0] + ray[3] * distance, ray[1] + ray[4] * distance, ray[2] + ray[5] * distance);
    }

    private Vector3f getCameraForward(CameraSettings camera) {
        float[] ray = camera.screenToWorldRay(0f, 0f, rayScratch);
        return new Vector3f(ray[3], ray[4], ray[5]);
    }

    @Nullable
    private Vector3f projectMouseToAxis(CameraSettings camera, LytRect viewport, int mouseX, int mouseY,
        Vector3f origin, Vector3f axisDirection) {
        return projectMouseToAxis(camera, viewport, mouseX, mouseY, origin, axisDirection, new Vector3f());
    }

    @Nullable
    private Vector3f projectMouseToAxis(CameraSettings camera, LytRect viewport, int mouseX, int mouseY,
        Vector3f origin, Vector3f axisDirection, Vector3f dest) {
        Vector3f originScreen = projectWorldPoint(
            camera,
            viewport,
            origin.x,
            origin.y,
            origin.z,
            projectedOriginScratch);
        Vector3f axisScreen = projectWorldPoint(
            camera,
            viewport,
            origin.x + axisDirection.x,
            origin.y + axisDirection.y,
            origin.z + axisDirection.z,
            projectedAxisScratch);
        float screenDx = axisScreen.x - originScreen.x;
        float screenDy = axisScreen.y - originScreen.y;
        float screenLengthSq = screenDx * screenDx + screenDy * screenDy;
        if (screenLengthSq >= 1e-4f) {
            float projectedDistance = ((mouseX - originScreen.x) * screenDx + (mouseY - originScreen.y) * screenDy)
                / screenLengthSq;
            return dest.set(
                origin.x + axisDirection.x * projectedDistance,
                origin.y + axisDirection.y * projectedDistance,
                origin.z + axisDirection.z * projectedDistance);
        }

        return projectMouseToAxisByRay(camera, viewport, mouseX, mouseY, origin, axisDirection, dest);
    }

    @Nullable
    private Vector3f projectMouseToAxisByRay(CameraSettings camera, LytRect viewport, int mouseX, int mouseY,
        Vector3f origin, Vector3f axisDirection) {
        return projectMouseToAxisByRay(camera, viewport, mouseX, mouseY, origin, axisDirection, new Vector3f());
    }

    @Nullable
    private Vector3f projectMouseToAxisByRay(CameraSettings camera, LytRect viewport, int mouseX, int mouseY,
        Vector3f origin, Vector3f axisDirection, Vector3f dest) {
        float relX = mouseX - (viewport.x() + viewport.width() * 0.5f);
        float relY = mouseY - (viewport.y() + viewport.height() * 0.5f);
        float[] ray = camera.screenToWorldRay(relX, relY, rayScratch);

        float a = ray[3] * ray[3] + ray[4] * ray[4] + ray[5] * ray[5];
        float b = ray[3] * axisDirection.x + ray[4] * axisDirection.y + ray[5] * axisDirection.z;
        float c = axisDirection.x * axisDirection.x + axisDirection.y * axisDirection.y
            + axisDirection.z * axisDirection.z;
        float wx = ray[0] - origin.x;
        float wy = ray[1] - origin.y;
        float wz = ray[2] - origin.z;
        float d = ray[3] * wx + ray[4] * wy + ray[5] * wz;
        float e = axisDirection.x * wx + axisDirection.y * wy + axisDirection.z * wz;
        float denominator = a * c - b * b;
        if (Math.abs(denominator) < 1e-6f) {
            return null;
        }

        float lineDistance = (a * e - b * d) / denominator;
        return dest.set(
            origin.x + axisDirection.x * lineDistance,
            origin.y + axisDirection.y * lineDistance,
            origin.z + axisDirection.z * lineDistance);
    }

    private Vector3f projectWorldPoint(CameraSettings camera, LytRect viewport, float x, float y, float z) {
        return projectWorldPoint(camera, viewport, x, y, z, new Vector3f());
    }

    private Vector3f projectWorldPoint(CameraSettings camera, LytRect viewport, float x, float y, float z,
        Vector3f dest) {
        Vector3f projected = camera.worldToScreen(x, y, z, projectedWorldScratch);
        return dest.set(
            viewport.x() + viewport.width() / 2f + projected.x,
            viewport.y() + viewport.height() / 2f + projected.y,
            projected.z);
    }

    private Vector3f applyConstraint(Vector3f point, DragMode mode, Vector3f origin) {
        return applyConstraint(point, mode, origin, new Vector3f());
    }

    private Vector3f applyConstraint(Vector3f point, DragMode mode, Vector3f origin, Vector3f constrained) {
        constrained.set(point);
        if (mode.lockX()) {
            constrained.x = origin.x;
        }
        if (mode.lockY()) {
            constrained.y = origin.y;
        }
        if (mode.lockZ()) {
            constrained.z = origin.z;
        }
        return constrained;
    }

    private Vector3f resolveBlockPoint(Vector3f point, DragMode mode, Vector3f origin) {
        Vector3f rounded = snapService.snapBlockPosition(point.x, point.y, point.z);
        Vector3f roundedOrigin = snapService.snapBlockPosition(origin.x, origin.y, origin.z);
        return applyConstraint(rounded, mode, roundedOrigin);
    }

    private Vector3f resolveDiamondPoint(GuidebookLevel level, Vector3f point, DragMode mode, Vector3f origin,
        boolean snapEnabled, SceneEditorSnapModes snapModes) {
        if (mode == DragMode.CENTER) {
            return snapService.snapFreePoint(level, point.x, point.y, point.z, snapEnabled, snapModes);
        }
        return snapService.snapConstrainedPoint(
            level,
            point.x,
            point.y,
            point.z,
            snapEnabled,
            snapModes,
            mode.lockX(),
            mode.lockY(),
            mode.lockZ(),
            origin.x,
            origin.y,
            origin.z);
    }

    @Nullable
    private Vector3f getBoxCornerPoint(SceneEditorElementModel element, String handleId) {
        float minX = Math.min(element.getPrimaryX(), element.getSecondaryX());
        float minY = Math.min(element.getPrimaryY(), element.getSecondaryY());
        float minZ = Math.min(element.getPrimaryZ(), element.getSecondaryZ());
        float maxX = Math.max(element.getPrimaryX(), element.getSecondaryX());
        float maxY = Math.max(element.getPrimaryY(), element.getSecondaryY());
        float maxZ = Math.max(element.getPrimaryZ(), element.getSecondaryZ());

        if (SceneEditorHandleOverlay.BOX_CORNER_MIN_MIN_MIN_HANDLE_ID.equals(handleId)) {
            return new Vector3f(minX, minY, minZ);
        }
        if (SceneEditorHandleOverlay.BOX_CORNER_MIN_MIN_MAX_HANDLE_ID.equals(handleId)) {
            return new Vector3f(minX, minY, maxZ);
        }
        if (SceneEditorHandleOverlay.BOX_CORNER_MIN_MAX_MIN_HANDLE_ID.equals(handleId)) {
            return new Vector3f(minX, maxY, minZ);
        }
        if (SceneEditorHandleOverlay.BOX_CORNER_MIN_MAX_MAX_HANDLE_ID.equals(handleId)) {
            return new Vector3f(minX, maxY, maxZ);
        }
        if (SceneEditorHandleOverlay.BOX_CORNER_MAX_MIN_MIN_HANDLE_ID.equals(handleId)) {
            return new Vector3f(maxX, minY, minZ);
        }
        if (SceneEditorHandleOverlay.BOX_CORNER_MAX_MIN_MAX_HANDLE_ID.equals(handleId)) {
            return new Vector3f(maxX, minY, maxZ);
        }
        if (SceneEditorHandleOverlay.BOX_CORNER_MAX_MAX_MIN_HANDLE_ID.equals(handleId)) {
            return new Vector3f(maxX, maxY, minZ);
        }
        if (SceneEditorHandleOverlay.BOX_CORNER_MAX_MAX_MAX_HANDLE_ID.equals(handleId)) {
            return new Vector3f(maxX, maxY, maxZ);
        }
        return null;
    }

    private String getOppositeBoxHandleId(String handleId) {
        if (SceneEditorHandleOverlay.BOX_CORNER_MIN_MIN_MIN_HANDLE_ID.equals(handleId)) {
            return SceneEditorHandleOverlay.BOX_CORNER_MAX_MAX_MAX_HANDLE_ID;
        }
        if (SceneEditorHandleOverlay.BOX_CORNER_MIN_MIN_MAX_HANDLE_ID.equals(handleId)) {
            return SceneEditorHandleOverlay.BOX_CORNER_MAX_MAX_MIN_HANDLE_ID;
        }
        if (SceneEditorHandleOverlay.BOX_CORNER_MIN_MAX_MIN_HANDLE_ID.equals(handleId)) {
            return SceneEditorHandleOverlay.BOX_CORNER_MAX_MIN_MAX_HANDLE_ID;
        }
        if (SceneEditorHandleOverlay.BOX_CORNER_MIN_MAX_MAX_HANDLE_ID.equals(handleId)) {
            return SceneEditorHandleOverlay.BOX_CORNER_MAX_MIN_MIN_HANDLE_ID;
        }
        if (SceneEditorHandleOverlay.BOX_CORNER_MAX_MIN_MIN_HANDLE_ID.equals(handleId)) {
            return SceneEditorHandleOverlay.BOX_CORNER_MIN_MAX_MAX_HANDLE_ID;
        }
        if (SceneEditorHandleOverlay.BOX_CORNER_MAX_MIN_MAX_HANDLE_ID.equals(handleId)) {
            return SceneEditorHandleOverlay.BOX_CORNER_MIN_MAX_MIN_HANDLE_ID;
        }
        if (SceneEditorHandleOverlay.BOX_CORNER_MAX_MAX_MIN_HANDLE_ID.equals(handleId)) {
            return SceneEditorHandleOverlay.BOX_CORNER_MIN_MIN_MAX_HANDLE_ID;
        }
        return SceneEditorHandleOverlay.BOX_CORNER_MIN_MIN_MIN_HANDLE_ID;
    }

    private enum DragBehavior {
        POINT_HANDLE,
        LINE_FROM,
        LINE_TO,
        BOX_CORNER
    }

    private enum DragMode {

        CENTER(SceneEditorHandleOverlay.CENTER_HANDLE_ID, false, false, false),
        AXIS_X(SceneEditorHandleOverlay.X_AXIS_HANDLE_ID, false, true, true),
        AXIS_Y(SceneEditorHandleOverlay.Y_AXIS_HANDLE_ID, true, false, true),
        AXIS_Z(SceneEditorHandleOverlay.Z_AXIS_HANDLE_ID, true, true, false),
        PLANE_XY(SceneEditorHandleOverlay.XY_PLANE_HANDLE_ID, false, false, true),
        PLANE_YZ(SceneEditorHandleOverlay.YZ_PLANE_HANDLE_ID, true, false, false),
        PLANE_ZX(SceneEditorHandleOverlay.ZX_PLANE_HANDLE_ID, false, true, false);

        private final String handleId;
        private final boolean lockX;
        private final boolean lockY;
        private final boolean lockZ;

        DragMode(String handleId, boolean lockX, boolean lockY, boolean lockZ) {
            this.handleId = handleId;
            this.lockX = lockX;
            this.lockY = lockY;
            this.lockZ = lockZ;
        }

        @Nullable
        private static DragMode fromHandleId(String handleId) {
            for (DragMode mode : values()) {
                if (mode.handleId.equals(handleId)) {
                    return mode;
                }
            }
            return null;
        }

        private boolean isAxis() {
            return this == AXIS_X || this == AXIS_Y || this == AXIS_Z;
        }

        private Vector3f axisDirection() {
            if (this == AXIS_X) {
                return new Vector3f(1f, 0f, 0f);
            }
            if (this == AXIS_Y) {
                return new Vector3f(0f, 1f, 0f);
            }
            return new Vector3f(0f, 0f, 1f);
        }

        private Vector3f planeNormal() {
            if (this == PLANE_XY) {
                return new Vector3f(0f, 0f, 1f);
            }
            if (this == PLANE_YZ) {
                return new Vector3f(1f, 0f, 0f);
            }
            return new Vector3f(0f, 1f, 0f);
        }

        private boolean lockX() {
            return lockX;
        }

        private boolean lockY() {
            return lockY;
        }

        private boolean lockZ() {
            return lockZ;
        }
    }

    public static final class DragState {

        private final UUID elementId;
        private final SceneEditorElementType elementType;
        private final Vector3f originPoint;
        private final DragBehavior behavior;
        @Nullable
        private final DragMode mode;
        @Nullable
        private final Vector3f axisDirection;
        @Nullable
        private final Vector3f planeNormal;
        @Nullable
        private final Vector3f fixedPoint;

        private DragState(UUID elementId, SceneEditorElementType elementType, Vector3f originPoint,
            DragBehavior behavior, @Nullable DragMode mode, @Nullable Vector3f axisDirection,
            @Nullable Vector3f planeNormal, @Nullable Vector3f fixedPoint) {
            this.elementId = elementId;
            this.elementType = elementType;
            this.originPoint = originPoint;
            this.behavior = behavior;
            this.mode = mode;
            this.axisDirection = axisDirection;
            this.planeNormal = planeNormal;
            this.fixedPoint = fixedPoint;
        }

        private static DragState pointHandle(UUID elementId, SceneEditorElementType elementType, Vector3f originPoint,
            DragMode mode, @Nullable Vector3f axisDirection, @Nullable Vector3f planeNormal) {
            return new DragState(
                elementId,
                elementType,
                originPoint,
                DragBehavior.POINT_HANDLE,
                mode,
                axisDirection,
                planeNormal,
                null);
        }

        private static DragState lineFrom(UUID elementId, Vector3f originPoint, Vector3f planeNormal) {
            return new DragState(
                elementId,
                SceneEditorElementType.LINE,
                originPoint,
                DragBehavior.LINE_FROM,
                null,
                null,
                planeNormal,
                null);
        }

        private static DragState lineTo(UUID elementId, Vector3f originPoint, Vector3f planeNormal) {
            return new DragState(
                elementId,
                SceneEditorElementType.LINE,
                originPoint,
                DragBehavior.LINE_TO,
                null,
                null,
                planeNormal,
                null);
        }

        private static DragState boxCorner(UUID elementId, Vector3f originPoint, Vector3f fixedPoint,
            Vector3f planeNormal) {
            return new DragState(
                elementId,
                SceneEditorElementType.BOX,
                originPoint,
                DragBehavior.BOX_CORNER,
                null,
                null,
                planeNormal,
                fixedPoint);
        }
    }
}
