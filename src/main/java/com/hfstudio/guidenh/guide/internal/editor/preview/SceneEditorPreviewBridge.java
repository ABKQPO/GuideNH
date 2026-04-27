package com.hfstudio.guidenh.guide.internal.editor.preview;

import java.nio.file.Path;

import javax.annotation.Nullable;

import com.hfstudio.guidenh.config.ModConfig;
import com.hfstudio.guidenh.guide.internal.editor.SceneEditorSession;
import com.hfstudio.guidenh.guide.internal.editor.model.SceneEditorSceneModel;
import com.hfstudio.guidenh.guide.scene.CameraSettings;
import com.hfstudio.guidenh.guide.scene.LytGuidebookScene;
import com.hfstudio.guidenh.guide.scene.level.GuidebookLevel;
import com.hfstudio.guidenh.guide.scene.structurelib.StructureLibPreviewSelection;
import com.hfstudio.guidenh.guide.scene.structurelib.StructureLibSceneImportService;

public final class SceneEditorPreviewBridge {

    private final SceneEditorPreviewCameraController previewCameraController;
    private final SceneEditorSceneNodePreviewApplier sceneNodePreviewApplier;

    public SceneEditorPreviewBridge(Path workingRoot) {
        this(workingRoot, new StructureLibSceneImportService());
    }

    SceneEditorPreviewBridge(Path workingRoot, StructureLibSceneImportService structureLibImportService) {
        this.previewCameraController = new SceneEditorPreviewCameraController();
        this.sceneNodePreviewApplier = new SceneEditorSceneNodePreviewApplier(workingRoot, structureLibImportService);
    }

    public LytGuidebookScene buildScene(SceneEditorSession session) {
        return buildScene(session, null);
    }

    public LytGuidebookScene buildScene(SceneEditorSession session,
        @Nullable StructureLibPreviewSelection structureLibSelectionOverride) {
        SceneEditorSceneModel model = session.getSceneModel();
        LytGuidebookScene scene = new LytGuidebookScene();
        scene.setInteractive(true);
        scene.setSceneButtonsVisible(false);
        scene.setVisibleLayerSliderEnabled(model.isAllowLayerSlider() || ModConfig.ui.sceneLayerSliderEnabled);
        scene.setSceneSize(model.getPreviewWidth(), model.getPreviewHeight());
        applyExportCamera(scene.getCamera(), model);
        sceneNodePreviewApplier.apply(session, scene, structureLibSelectionOverride);
        scene.snapshotInitialCamera();
        return scene;
    }

    public void rebuildScene(SceneEditorSession session, LytGuidebookScene scene,
        @Nullable StructureLibPreviewSelection structureLibSelectionOverride) {
        if (scene == null) {
            return;
        }
        scene.getAnnotations()
            .clear();
        scene.setHoveredStructureLibHatch(null);
        scene.setHoveredBlock(null);
        scene.clearAnnotationHover();
        scene.setStructureLibSceneMetadata(null);
        scene.setLevel(new GuidebookLevel());
        scene.setVisibleLayerSliderEnabled(
            session.getSceneModel()
                .isAllowLayerSlider() || ModConfig.ui.sceneLayerSliderEnabled);
        sceneNodePreviewApplier.apply(session, scene, structureLibSelectionOverride);
    }

    private void applyExportCamera(CameraSettings camera, SceneEditorSceneModel model) {
        previewCameraController.applyModelCamera(camera, model);
    }
}
