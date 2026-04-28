package com.hfstudio.guidenh.guide.internal.editor.preview;

import com.hfstudio.guidenh.guide.internal.editor.model.SceneEditorSceneModel;
import com.hfstudio.guidenh.guide.scene.CameraSettings;
import com.hfstudio.guidenh.guide.scene.LytGuidebookScene;
import com.hfstudio.guidenh.guide.scene.PerspectivePreset;

public class SceneEditorPreviewCameraController {

    public void applyModelCamera(LytGuidebookScene scene, SceneEditorSceneModel model) {
        applyModelCamera(scene.getCamera(), model);
        scene.snapshotInitialCamera();
    }

    public void applyModelCamera(CameraSettings camera, SceneEditorSceneModel model) {
        if (model.getPerspectivePreset() != null && !model.getPerspectivePreset()
            .isEmpty()) {
            camera.setPerspectivePreset(PerspectivePreset.fromSerializedName(model.getPerspectivePreset()));
        }
        camera.setRotationX(model.getRotationX());
        camera.setRotationY(model.getRotationY());
        camera.setRotationZ(model.getRotationZ());
        camera.setOffsetX(model.getOffsetX());
        camera.setOffsetY(model.getOffsetY());
        camera.setZoom(model.getZoom());
        camera.setRotationCenter(model.getCenterX(), model.getCenterY(), model.getCenterZ());
    }

    public void resetPreviewView(LytGuidebookScene scene, SceneEditorSceneModel model) {
        boolean annotationsVisible = scene.isAnnotationsVisible();
        applyModelCamera(scene, model);
        scene.setAnnotationsVisible(annotationsVisible);
    }
}
