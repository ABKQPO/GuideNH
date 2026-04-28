package com.hfstudio.guidenh.guide.internal.editor.gui;

import java.util.function.Predicate;
import java.util.function.Supplier;

public class SceneEditorDraftTextController {

    private final Supplier<String> appliedValueSupplier;
    private final boolean emptyMeansRestoreAppliedValue;
    private final Predicate<String> draftCommitHandler;

    private String draftText;
    private boolean validationError;

    public SceneEditorDraftTextController(Supplier<String> appliedValueSupplier, boolean emptyMeansRestoreAppliedValue,
        Predicate<String> draftCommitHandler) {
        this.appliedValueSupplier = appliedValueSupplier;
        this.emptyMeansRestoreAppliedValue = emptyMeansRestoreAppliedValue;
        this.draftCommitHandler = draftCommitHandler;
        this.draftText = safeText(appliedValueSupplier.get());
        this.validationError = false;
    }

    public String getDraftText() {
        return draftText;
    }

    public void setDraftText(String draftText) {
        this.draftText = safeText(draftText);
        this.validationError = false;
    }

    public boolean hasValidationError() {
        return validationError;
    }

    public boolean commitDraftText() {
        if (emptyMeansRestoreAppliedValue && draftText.trim()
            .isEmpty()) {
            draftText = safeText(appliedValueSupplier.get());
            validationError = false;
            return true;
        }

        if (!draftCommitHandler.test(draftText)) {
            validationError = true;
            return false;
        }

        draftText = safeText(appliedValueSupplier.get());
        validationError = false;
        return true;
    }

    public void syncFromAppliedValue() {
        draftText = safeText(appliedValueSupplier.get());
        validationError = false;
    }

    public void restoreDraftState(String draftText, boolean validationError) {
        this.draftText = safeText(draftText);
        this.validationError = validationError;
    }

    private String safeText(String text) {
        return text != null ? text : "";
    }
}
