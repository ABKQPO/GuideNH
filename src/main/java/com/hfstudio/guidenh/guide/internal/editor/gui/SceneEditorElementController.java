package com.hfstudio.guidenh.guide.internal.editor.gui;

import java.util.UUID;

import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.guide.internal.editor.SceneEditorSession;
import com.hfstudio.guidenh.guide.internal.editor.md.SceneEditorMarkdownCodec;
import com.hfstudio.guidenh.guide.internal.editor.model.SceneEditorElementModel;
import com.hfstudio.guidenh.guide.internal.editor.model.SceneEditorElementType;
import com.hfstudio.guidenh.guide.internal.editor.model.SceneEditorSceneModel;

public class SceneEditorElementController {

    private final SceneEditorSession session;
    private final SceneEditorMarkdownCodec codec;

    public SceneEditorElementController(SceneEditorSession session, SceneEditorMarkdownCodec codec) {
        this.session = session;
        this.codec = codec;
    }

    public SceneEditorElementModel addElement(SceneEditorElementType type) {
        SceneEditorElementModel element = new SceneEditorElementModel(type);
        session.getSceneModel()
            .addElement(element);
        session.getSelectionState()
            .setSelectedElementId(element.getId());
        syncText();
        return element;
    }

    public boolean removeElement(UUID elementId) {
        boolean removed = session.getSceneModel()
            .removeElement(elementId);
        if (!removed) {
            return false;
        }
        if (elementId.equals(
            session.getSelectionState()
                .getSelectedElementId())) {
            session.getSelectionState()
                .setSelectedElementId(null);
        }
        syncText();
        return true;
    }

    public boolean setVisible(UUID elementId, boolean visible) {
        SceneEditorElementModel element = requireElement(elementId);
        if (element == null) {
            return false;
        }
        element.setVisible(visible);
        syncText();
        return true;
    }

    public boolean moveElement(int fromIndex, int toIndex) {
        boolean moved = session.getSceneModel()
            .moveElement(fromIndex, toIndex);
        if (!moved) {
            return false;
        }
        syncText();
        return true;
    }

    public String syncText() {
        SceneEditorSceneModel sceneModel = session.getSceneModel();
        String serialized = codec.serialize(sceneModel);
        session.setRawText(serialized);
        return serialized;
    }

    @Nullable
    private SceneEditorElementModel requireElement(UUID elementId) {
        return session.getSceneModel()
            .getElement(elementId)
            .orElse(null);
    }
}
