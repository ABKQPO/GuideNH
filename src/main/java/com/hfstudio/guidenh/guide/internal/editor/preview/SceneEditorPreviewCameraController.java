package com.hfstudio.guidenh.guide.internal.editor.preview;

import com.hfstudio.guidenh.guide.internal.editor.model.SceneEditorSceneModel;
import com.hfstudio.guidenh.guide.scene.CameraSettings;
import com.hfstudio.guidenh.guide.scene.LytGuidebookScene;
import com.hfstudio.guidenh.guide.scene.PerspectivePreset;
import com.hfstudio.guidenh.guide.scene.level.GuidebookLevel;

public class SceneEditorPreviewCameraController {

    private static final float EDITOR_PREVIEW_YAW_OFFSET = 180.0F;

    public void applyResolvedPreviewCamera(LytGuidebookScene scene, SceneEditorSceneModel model) {
        applyResolvedPreviewCamera(scene.getCamera(), scene, model);
        scene.snapshotInitialCamera();
    }

    public void applyModelCamera(CameraSettings camera, SceneEditorSceneModel model) {
        if (model.getPerspectivePreset() != null && !model.getPerspectivePreset()
            .isEmpty()) {
            camera.setPerspectivePreset(PerspectivePreset.fromSerializedName(model.getPerspectivePreset()));
        } else {
            camera.setPerspectivePreset(PerspectivePreset.ISOMETRIC_NORTH_EAST);
        }
        if (!Float.isNaN(model.getRotationX())) {
            camera.setRotationX(model.getRotationX());
        }
        if (!Float.isNaN(model.getRotationY())) {
            camera.setRotationY(model.getRotationY() + EDITOR_PREVIEW_YAW_OFFSET);
        }
        if (!Float.isNaN(model.getRotationZ())) {
            camera.setRotationZ(model.getRotationZ());
        }
        if (!Float.isNaN(model.getOffsetX())) {
            camera.setOffsetX(model.getOffsetX());
        }
        if (!Float.isNaN(model.getOffsetY())) {
            camera.setOffsetY(model.getOffsetY());
        }
        if (!Float.isNaN(model.getZoom())) {
            camera.setZoom(model.getZoom());
        }
    }

    public void applyResolvedPreviewCamera(CameraSettings camera, LytGuidebookScene scene,
        SceneEditorSceneModel model) {
        applyModelCamera(camera, model);

        GuidebookLevel level = scene.getLevel();
        if (level == null || level.isEmpty()) {
            return;
        }

        int width = model.getPreviewWidth();
        int height = model.getPreviewHeight();
        camera.setViewportSize(width, height);

        float[] autoCenter = level.getCenter();
        float centerX;
        float centerY;
        float centerZ;
        if (model.hasExplicitCenter()) {
            centerX = Float.isNaN(model.getCenterX()) ? autoCenter[0] : model.getCenterX();
            centerY = Float.isNaN(model.getCenterY()) ? autoCenter[1] : model.getCenterY();
            centerZ = Float.isNaN(model.getCenterZ()) ? autoCenter[2] : model.getCenterZ();
        } else {
            centerX = autoCenter[0];
            centerY = autoCenter[1];
            centerZ = autoCenter[2];
        }
        camera.setRotationCenter(centerX, centerY, centerZ);

        float offsetX = model.hasExplicitOffsetX() ? model.getOffsetX() : 0f;
        float offsetY = model.hasExplicitOffsetY() ? model.getOffsetY() : 0f;
        camera.setOffsetX(offsetX);
        camera.setOffsetY(offsetY);

        if (!model.hasExplicitZoom()) {
            camera.setZoom(1f);
            camera.setOffsetX(0f);
            camera.setOffsetY(0f);
            camera.setZoom(resolveAutoZoom(camera, level, width, height));
            camera.setOffsetX(offsetX);
            camera.setOffsetY(offsetY);
        }
    }

    public void resetPreviewView(LytGuidebookScene scene, SceneEditorSceneModel model) {
        boolean annotationsVisible = scene.isAnnotationsVisible();
        applyResolvedPreviewCamera(scene.getCamera(), scene, model);
        scene.snapshotInitialCamera();
        scene.setAnnotationsVisible(annotationsVisible);
    }

    private float resolveAutoZoom(CameraSettings camera, GuidebookLevel level, int width, int height) {
        int[] bounds = level.getBounds();
        float lx = bounds[0];
        float ly = bounds[1];
        float lz = bounds[2];
        float hx = bounds[3] + 1f;
        float hy = bounds[4] + 1f;
        float hz = bounds[5] + 1f;

        float minSX = Float.MAX_VALUE;
        float maxSX = -Float.MAX_VALUE;
        float minSY = Float.MAX_VALUE;
        float maxSY = -Float.MAX_VALUE;
        for (int i = 0; i < 8; i++) {
            float wx = (i & 1) == 0 ? lx : hx;
            float wy = (i & 2) == 0 ? ly : hy;
            float wz = (i & 4) == 0 ? lz : hz;
            var projected = camera.worldToScreen(wx, wy, wz);
            if (projected.x < minSX) {
                minSX = projected.x;
            }
            if (projected.x > maxSX) {
                maxSX = projected.x;
            }
            if (projected.y < minSY) {
                minSY = projected.y;
            }
            if (projected.y > maxSY) {
                maxSY = projected.y;
            }
        }

        float spanX = maxSX - minSX;
        float spanY = maxSY - minSY;
        if (spanX > 0.5f || spanY > 0.5f) {
            float zoomX = spanX > 0.5f ? (float) width / spanX : Float.MAX_VALUE;
            float zoomY = spanY > 0.5f ? (float) height / spanY : Float.MAX_VALUE;
            float zoom = Math.min(zoomX, zoomY) * 0.85f;
            return Math.max(LytGuidebookScene.MIN_ZOOM, Math.min(LytGuidebookScene.MAX_ZOOM, zoom));
        }
        return 1f;
    }
}
