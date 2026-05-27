package com.hfstudio.guidenh.guide.internal.editor.preview;

import java.nio.file.Path;

import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.config.ModConfig;
import com.hfstudio.guidenh.guide.internal.editor.SceneEditorSession;
import com.hfstudio.guidenh.guide.internal.editor.model.SceneEditorSceneModel;
import com.hfstudio.guidenh.guide.scene.LytGuidebookScene;
import com.hfstudio.guidenh.guide.scene.level.GuidebookLevel;
import com.hfstudio.guidenh.integration.structurelib.StructureLibPreviewSelection;
import com.hfstudio.guidenh.integration.structurelib.StructureLibSceneImportService;

public class SceneEditorPreviewBridge {

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
        scene.setShowBackground(model.isShowBackground());
        scene.setForceOriginAxesVisible(true);
        scene.setSceneButtonsVisible(false);
        scene.setReserveBottomControlArea(false);
        scene.setVisibleLayerSliderEnabled(model.isAllowLayerSlider() || ModConfig.ui.sceneLayerSliderEnabled);
        scene.setSceneSize(model.getPreviewWidth(), model.getPreviewHeight());
        sceneNodePreviewApplier.apply(session, scene, structureLibSelectionOverride);
        previewCameraController.applyResolvedPreviewCamera(scene, model);
        return scene;
    }

    public void rebuildScene(SceneEditorSession session, LytGuidebookScene scene,
        @Nullable StructureLibPreviewSelection structureLibSelectionOverride) {
        if (scene == null) {
            return;
        }
        int ponderTick = scene.getPonderCurrentTickForExport();
        boolean ponderPaused = scene.isPonderPausedForPreviewRestore();
        boolean ponderFinished = scene.isPonderFinishedForPreviewRestore();
        scene.getAnnotations()
            .clear();
        scene.clearSoundCues();
        scene.setHoveredStructureLibHatch(null);
        scene.setHoveredBlock(null);
        scene.setHoveredEntity(null);
        scene.clearAnnotationHover();
        scene.setStructureLibSceneMetadata(null);
        scene.clearPonderDataForPreviewRebuild();
        scene.setLevel(new GuidebookLevel());
        scene.setReserveBottomControlArea(false);
        scene.setShowBackground(
            session.getSceneModel()
                .isShowBackground());
        scene.setForceOriginAxesVisible(true);
        scene.setVisibleLayerSliderEnabled(
            session.getSceneModel()
                .isAllowLayerSlider() || ModConfig.ui.sceneLayerSliderEnabled);
        sceneNodePreviewApplier.apply(session, scene, structureLibSelectionOverride);
        previewCameraController.applyResolvedPreviewCamera(scene, session.getSceneModel());
        scene.initializePonderTimelineBaseline();
        scene.restorePonderPreviewState(ponderTick, ponderPaused, ponderFinished);
    }

    public void rebuildAnnotations(SceneEditorSession session, LytGuidebookScene scene) {
        if (scene == null) {
            return;
        }
        scene.getAnnotations()
            .clear();
        scene.clearAnnotationHover();
        sceneNodePreviewApplier.applyAnnotations(session, scene);
    }

    public void releaseScene(@Nullable LytGuidebookScene scene) {
        if (scene == null) {
            return;
        }
        scene.setStructureLibSelectionChangeListener(null);
        scene.getAnnotations()
            .clear();
        scene.clearSoundCues();
        scene.clearAnnotationHover();
        scene.setHoveredStructureLibHatch(null);
        scene.setHoveredBlock(null);
        scene.setHoveredEntity(null);
        scene.setStructureLibSceneMetadata(null);
        scene.clearPonderDataForPreviewRebuild();
        scene.setLevel(new GuidebookLevel());
    }
}
