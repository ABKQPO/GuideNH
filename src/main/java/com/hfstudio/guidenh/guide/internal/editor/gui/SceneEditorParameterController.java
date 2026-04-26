package com.hfstudio.guidenh.guide.internal.editor.gui;

import com.hfstudio.guidenh.guide.internal.editor.SceneEditorSession;
import com.hfstudio.guidenh.guide.internal.editor.md.SceneEditorMarkdownCodec;
import com.hfstudio.guidenh.guide.internal.editor.model.SceneEditorSceneModel;

public final class SceneEditorParameterController {

    private final SceneEditorSession session;
    private final SceneEditorMarkdownCodec codec;

    public SceneEditorParameterController(SceneEditorSession session, SceneEditorMarkdownCodec codec) {
        this.session = session;
        this.codec = codec;
    }

    public void setPreviewWidth(int value) {
        session.getSceneModel()
            .setPreviewWidth(value);
        syncText();
    }

    public void setPreviewHeight(int value) {
        session.getSceneModel()
            .setPreviewHeight(value);
        syncText();
    }

    public void setRotationX(float value) {
        session.getSceneModel()
            .setRotationX(value);
        syncText();
    }

    public void setRotationY(float value) {
        session.getSceneModel()
            .setRotationY(value);
        syncText();
    }

    public void setRotationZ(float value) {
        session.getSceneModel()
            .setRotationZ(value);
        syncText();
    }

    public void setZoom(float value) {
        session.getSceneModel()
            .setZoom(value);
        syncText();
    }

    public void setCenterX(float value) {
        session.getSceneModel()
            .setCenterX(value);
        syncText();
    }

    public void setCenterY(float value) {
        session.getSceneModel()
            .setCenterY(value);
        syncText();
    }

    public void setCenterZ(float value) {
        session.getSceneModel()
            .setCenterZ(value);
        syncText();
    }

    public void setInteractive(boolean value) {
        session.getSceneModel()
            .setInteractive(value);
        syncText();
    }

    public String syncText() {
        SceneEditorSceneModel sceneModel = session.getSceneModel();
        String serialized = codec.serialize(sceneModel);
        session.setRawText(serialized);
        return serialized;
    }
}
