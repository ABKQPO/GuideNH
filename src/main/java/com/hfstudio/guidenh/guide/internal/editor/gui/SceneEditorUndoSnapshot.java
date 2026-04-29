package com.hfstudio.guidenh.guide.internal.editor.gui;

import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.guide.internal.editor.model.SceneEditorSceneModel;

public class SceneEditorUndoSnapshot {

    private final String rawText;
    private final String lastAppliedText;
    private final SceneEditorSceneModel sceneModel;
    private final SceneEditorTextSyncController.ValidationKind validationKind;
    @Nullable
    private final String validationMessage;
    private final SceneEditorUndoUiState uiState;

    public SceneEditorUndoSnapshot(String rawText, String lastAppliedText, SceneEditorSceneModel sceneModel,
        SceneEditorTextSyncController.ValidationKind validationKind, @Nullable String validationMessage) {
        this(rawText, lastAppliedText, sceneModel, validationKind, validationMessage, SceneEditorUndoUiState.empty());
    }

    public SceneEditorUndoSnapshot(String rawText, String lastAppliedText, SceneEditorSceneModel sceneModel,
        SceneEditorTextSyncController.ValidationKind validationKind, @Nullable String validationMessage,
        @Nullable SceneEditorUndoUiState uiState) {
        this.rawText = rawText != null ? rawText : "";
        this.lastAppliedText = lastAppliedText != null ? lastAppliedText : "";
        this.sceneModel = sceneModel;
        this.validationKind = validationKind;
        this.validationMessage = validationMessage;
        this.uiState = uiState != null ? uiState : SceneEditorUndoUiState.empty();
    }

    public String getRawText() {
        return rawText;
    }

    public String getLastAppliedText() {
        return lastAppliedText;
    }

    public SceneEditorSceneModel getSceneModel() {
        return sceneModel;
    }

    public SceneEditorTextSyncController.ValidationKind getValidationKind() {
        return validationKind;
    }

    @Nullable
    public String getValidationMessage() {
        return validationMessage;
    }

    public SceneEditorUndoUiState getUiState() {
        return uiState;
    }
}
