package com.hfstudio.guidenh.guide.internal.editor.gui;

public final class SceneEditorUndoFieldState {

    private final String draftText;
    private final boolean validationError;

    public SceneEditorUndoFieldState(String draftText, boolean validationError) {
        this.draftText = draftText != null ? draftText : "";
        this.validationError = validationError;
    }

    public String getDraftText() {
        return draftText;
    }

    public boolean hasValidationError() {
        return validationError;
    }
}
