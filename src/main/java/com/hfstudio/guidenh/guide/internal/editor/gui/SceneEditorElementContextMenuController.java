package com.hfstudio.guidenh.guide.internal.editor.gui;

import java.util.Locale;
import java.util.UUID;
import java.util.function.IntSupplier;

import javax.annotation.Nullable;

import com.hfstudio.guidenh.guide.internal.editor.SceneEditorSession;
import com.hfstudio.guidenh.guide.internal.editor.md.SceneEditorMarkdownCodec;
import com.hfstudio.guidenh.guide.internal.editor.model.SceneEditorElementModel;
import com.hfstudio.guidenh.guide.internal.editor.model.SceneEditorSceneModel;

public final class SceneEditorElementContextMenuController {

    private final SceneEditorSession session;
    private final SceneEditorMarkdownCodec codec;
    private final IntSupplier randomColorSupplier;

    @Nullable
    private SceneEditorElementModel clipboardElement;

    public SceneEditorElementContextMenuController(SceneEditorSession session, SceneEditorMarkdownCodec codec,
        IntSupplier randomColorSupplier) {
        this.session = session;
        this.codec = codec;
        this.randomColorSupplier = randomColorSupplier;
        this.clipboardElement = null;
    }

    public boolean hasClipboard() {
        return clipboardElement != null;
    }

    public boolean copyElement(UUID elementId) {
        SceneEditorElementModel element = requireElement(elementId);
        if (element == null) {
            return false;
        }
        clipboardElement = element.duplicate();
        return true;
    }

    public boolean cutElement(UUID elementId) {
        if (!copyElement(elementId)) {
            return false;
        }
        return deleteElement(elementId);
    }

    public boolean pasteAfter(UUID targetElementId) {
        if (clipboardElement == null) {
            return false;
        }
        SceneEditorSceneModel sceneModel = session.getSceneModel();
        int targetIndex = indexOfElement(targetElementId);
        if (targetIndex < 0) {
            return false;
        }

        SceneEditorElementModel pasted = clipboardElement.duplicate();
        sceneModel.getElements()
            .add(targetIndex + 1, pasted);
        session.getSelectionState()
            .setSelectedElementId(pasted.getId());
        syncText();
        return true;
    }

    public boolean pasteAtTop() {
        if (clipboardElement == null) {
            return false;
        }
        SceneEditorElementModel pasted = clipboardElement.duplicate();
        session.getSceneModel()
            .getElements()
            .add(0, pasted);
        session.getSelectionState()
            .setSelectedElementId(pasted.getId());
        syncText();
        return true;
    }

    public boolean toggleVisibility(UUID elementId) {
        SceneEditorElementModel element = requireElement(elementId);
        if (element == null) {
            return false;
        }
        element.setVisible(!element.isVisible());
        syncText();
        return true;
    }

    public boolean randomizeColor(UUID elementId) {
        SceneEditorElementModel element = requireElement(elementId);
        if (element == null) {
            return false;
        }
        int color = 0xFF000000 | (randomColorSupplier.getAsInt() & 0x00FFFFFF);
        element.setColorLiteral(String.format(Locale.ROOT, "#%08X", color));
        syncText();
        return true;
    }

    public boolean deleteElement(UUID elementId) {
        SceneEditorSceneModel sceneModel = session.getSceneModel();
        boolean removed = sceneModel.removeElement(elementId);
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

    public String syncText() {
        String serialized = codec.serialize(session.getSceneModel());
        session.setRawText(serialized);
        return serialized;
    }

    private int indexOfElement(UUID elementId) {
        for (int i = 0; i < session.getSceneModel()
            .getElements()
            .size(); i++) {
            if (session.getSceneModel()
                .getElements()
                .get(i)
                .getId()
                .equals(elementId)) {
                return i;
            }
        }
        return -1;
    }

    @Nullable
    private SceneEditorElementModel requireElement(UUID elementId) {
        return session.getSceneModel()
            .getElement(elementId)
            .orElse(null);
    }
}
