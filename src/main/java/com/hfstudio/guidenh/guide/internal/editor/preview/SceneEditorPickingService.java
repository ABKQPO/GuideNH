package com.hfstudio.guidenh.guide.internal.editor.preview;

import java.util.List;
import java.util.UUID;

import javax.annotation.Nullable;

import com.hfstudio.guidenh.guide.internal.editor.SceneEditorSession;
import com.hfstudio.guidenh.guide.internal.editor.model.SceneEditorElementModel;
import com.hfstudio.guidenh.guide.scene.LytGuidebookScene;
import com.hfstudio.guidenh.guide.scene.annotation.SceneAnnotation;

public final class SceneEditorPickingService {

    public boolean applyPreviewClickSelection(SceneEditorSession session, LytGuidebookScene previewScene, int mouseX,
        int mouseY, boolean autoPickEnabled) {
        if (!autoPickEnabled) {
            return false;
        }

        UUID pickedElementId = findPickedElementId(session, previewScene, mouseX, mouseY);
        UUID selectedElementId = session.getSelectionState()
            .getSelectedElementId();
        if (pickedElementId == null) {
            if (selectedElementId == null && session.getSelectionState()
                .getSelectedHandleId() == null) {
                return false;
            }
            session.getSelectionState()
                .setSelectedElementId(null);
            session.getSelectionState()
                .setSelectedHandleId(null);
            return true;
        }
        if (pickedElementId.equals(selectedElementId)) {
            return false;
        }

        session.getSelectionState()
            .setSelectedElementId(pickedElementId);
        return true;
    }

    @Nullable
    public UUID findPickedElementId(SceneEditorSession session, LytGuidebookScene previewScene, int mouseX,
        int mouseY) {
        SceneAnnotation hitAnnotation = previewScene.updateAnnotationHover(mouseX, mouseY);
        if (hitAnnotation == null) {
            return null;
        }

        int annotationIndex = previewScene.getAnnotations()
            .indexOf(hitAnnotation);
        if (annotationIndex < 0) {
            return null;
        }

        List<SceneEditorElementModel> visibleElements = getVisibleElements(session);
        if (annotationIndex >= visibleElements.size()) {
            return null;
        }
        return visibleElements.get(annotationIndex)
            .getId();
    }

    private List<SceneEditorElementModel> getVisibleElements(SceneEditorSession session) {
        List<SceneEditorElementModel> visibleElements = new java.util.ArrayList<>();
        for (SceneEditorElementModel element : session.getSceneModel()
            .getElements()) {
            if (element.isVisible()) {
                visibleElements.add(element);
            }
        }
        return visibleElements;
    }
}
