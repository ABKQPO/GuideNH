package com.hfstudio.guidenh.guide.internal.editor;

import javax.annotation.Nullable;

import com.hfstudio.guidenh.config.ModConfig;
import com.hfstudio.guidenh.guide.internal.editor.gui.SceneEditorTextSyncController.ValidationKind;
import com.hfstudio.guidenh.guide.internal.editor.gui.SceneEditorUndoHistory;
import com.hfstudio.guidenh.guide.internal.editor.gui.SceneEditorUndoSnapshot;
import com.hfstudio.guidenh.guide.internal.editor.gui.SceneEditorUndoUiState;
import com.hfstudio.guidenh.guide.internal.editor.model.SceneEditorSceneModel;
import com.hfstudio.guidenh.guide.internal.editor.model.SceneEditorSelectionState;

public final class SceneEditorSession {

    private SceneEditorSceneModel sceneModel;
    private final boolean blankSession;
    private final SceneEditorSelectionState selectionState;
    private final SceneEditorUndoHistory undoHistory;
    private String lastSynchronizedText;
    private String lastAppliedText;
    private String rawText;
    private boolean dirty;
    @Nullable
    private String importedStructureSnbt;

    private SceneEditorSession(SceneEditorSceneModel sceneModel, boolean blankSession) {
        this.sceneModel = sceneModel;
        this.blankSession = blankSession;
        this.selectionState = new SceneEditorSelectionState();
        this.undoHistory = new SceneEditorUndoHistory(ModConfig.ui.sceneEditorUndoHistoryLimit);
        this.lastSynchronizedText = "";
        this.lastAppliedText = "";
        this.rawText = "";
        this.dirty = false;
        this.importedStructureSnbt = null;
    }

    public static SceneEditorSession createBlank() {
        return new SceneEditorSession(SceneEditorSceneModel.blank(), true);
    }

    public static SceneEditorSession createImported(String structureSource) {
        return new SceneEditorSession(SceneEditorSceneModel.withStructureSource(structureSource), false);
    }

    public SceneEditorSceneModel getSceneModel() {
        return sceneModel;
    }

    public void setSceneModel(SceneEditorSceneModel sceneModel) {
        this.sceneModel = sceneModel;
        this.dirty = true;
    }

    public boolean isBlankSession() {
        return blankSession;
    }

    public SceneEditorSelectionState getSelectionState() {
        return selectionState;
    }

    public SceneEditorUndoHistory getUndoHistory() {
        return undoHistory;
    }

    public String getLastSynchronizedText() {
        return lastSynchronizedText;
    }

    public String getLastAppliedText() {
        return lastAppliedText;
    }

    public void setLastAppliedText(String lastAppliedText) {
        this.lastAppliedText = lastAppliedText;
    }

    public String getRawText() {
        return rawText;
    }

    public void setRawText(String rawText) {
        this.rawText = rawText;
        this.dirty = !rawText.equals(this.lastSynchronizedText);
    }

    public boolean isDirty() {
        return dirty;
    }

    public void markDirty() {
        this.dirty = true;
    }

    public void markSaved(String synchronizedText) {
        this.lastSynchronizedText = synchronizedText;
        this.lastAppliedText = synchronizedText;
        this.rawText = synchronizedText;
        this.dirty = false;
    }

    public void markAppliedSaved(String synchronizedText) {
        this.lastSynchronizedText = synchronizedText;
        this.lastAppliedText = synchronizedText;
        this.dirty = !rawText.equals(this.lastSynchronizedText);
    }

    public SceneEditorUndoSnapshot captureContentSnapshot(ValidationKind validationKind,
        @Nullable String validationMessage) {
        return captureContentSnapshot(validationKind, validationMessage, SceneEditorUndoUiState.empty());
    }

    public SceneEditorUndoSnapshot captureContentSnapshot(ValidationKind validationKind,
        @Nullable String validationMessage, @Nullable SceneEditorUndoUiState uiState) {
        return new SceneEditorUndoSnapshot(
            rawText,
            lastAppliedText,
            sceneModel.copy(),
            validationKind,
            validationMessage,
            uiState);
    }

    public void restoreContentSnapshot(SceneEditorUndoSnapshot snapshot) {
        this.rawText = snapshot.getRawText();
        this.lastAppliedText = snapshot.getLastAppliedText();
        this.sceneModel = snapshot.getSceneModel()
            .copy();
        this.dirty = !rawText.equals(this.lastSynchronizedText);
    }

    @Nullable
    public String getImportedStructureSnbt() {
        return importedStructureSnbt;
    }

    public void setImportedStructureSnbt(@Nullable String importedStructureSnbt) {
        this.importedStructureSnbt = importedStructureSnbt;
    }
}
