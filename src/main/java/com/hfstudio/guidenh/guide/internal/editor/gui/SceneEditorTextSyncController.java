package com.hfstudio.guidenh.guide.internal.editor.gui;

import java.util.Objects;

import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.guide.internal.editor.SceneEditorSession;
import com.hfstudio.guidenh.guide.internal.editor.md.SceneEditorMarkdownCodec;
import com.hfstudio.guidenh.guide.internal.editor.md.SceneEditorMarkdownElementRangeIndex;
import com.hfstudio.guidenh.guide.internal.editor.md.SceneEditorMarkdownParseResult;

public class SceneEditorTextSyncController {

    public enum ValidationKind {
        NONE,
        SYNTAX,
        UNSUPPORTED
    }

    public enum LiveApplyResult {
        APPLIED,
        INVALID,
        NO_CHANGE
    }

    private final SceneEditorSession session;
    private final SceneEditorMarkdownCodec codec;
    private SceneEditorMarkdownElementRangeIndex appliedRangeIndex;
    private SceneEditorMarkdownElementRangeIndex draftRangeIndex;
    private ValidationKind validationKind;
    @Nullable
    private String validationMessage;
    @Nullable
    private String lastProcessedDraftText;
    private ValidationKind lastProcessedValidationKind;
    @Nullable
    private String lastProcessedValidationMessage;

    public SceneEditorTextSyncController(SceneEditorSession session, SceneEditorMarkdownCodec codec) {
        this.session = session;
        this.codec = codec;
        this.appliedRangeIndex = SceneEditorMarkdownElementRangeIndex.empty();
        this.draftRangeIndex = SceneEditorMarkdownElementRangeIndex.empty();
        this.validationKind = ValidationKind.NONE;
        this.validationMessage = null;
        this.lastProcessedDraftText = null;
        this.lastProcessedValidationKind = ValidationKind.NONE;
        this.lastProcessedValidationMessage = null;
    }

    public void setDraftText(String text) {
        session.setRawText(text);
        draftRangeIndex = SceneEditorMarkdownElementRangeIndex.fromBestEffortText(
            text,
            session.getSceneModel()
                .getElements());
        validationKind = ValidationKind.NONE;
        validationMessage = null;
    }

    public boolean commitDraftText() {
        return commitDraftText(null, SceneEditorUndoUiState.empty());
    }

    public boolean commitDraftText(@Nullable String mergeKey, @Nullable SceneEditorUndoUiState uiState) {
        SceneEditorMarkdownParseResult result = codec.parse(session.getRawText());
        if (result instanceof SceneEditorMarkdownParseResult.Success success) {
            ensureHistoryInitialized();
            String synchronizedText = codec.serialize(success.model());
            applyParsedModel(success.model(), synchronizedText, true);
            recordAppliedSnapshot(mergeKey, uiState, false);
            rememberProcessedDraftState();
            return true;
        }
        if (result instanceof SceneEditorMarkdownParseResult.SyntaxError syntaxError) {
            validationKind = ValidationKind.SYNTAX;
            validationMessage = syntaxError.message();
            rebuildRangeIndexes();
            recordCurrentSnapshot(mergeKey, uiState, false);
            rememberProcessedDraftState();
            return false;
        }
        if (result instanceof SceneEditorMarkdownParseResult.Unsupported unsupported) {
            validationKind = ValidationKind.UNSUPPORTED;
            validationMessage = unsupported.message();
            rebuildRangeIndexes();
            recordCurrentSnapshot(mergeKey, uiState, false);
            rememberProcessedDraftState();
            return false;
        }
        validationKind = ValidationKind.SYNTAX;
        validationMessage = "Unknown scene editor parse error";
        rebuildRangeIndexes();
        recordCurrentSnapshot(mergeKey, uiState, false);
        rememberProcessedDraftState();
        return false;
    }

    public LiveApplyResult applyLiveDraftText() {
        String draftText = session.getRawText();
        if (lastProcessedDraftText != null && lastProcessedDraftText.equals(draftText)) {
            validationKind = lastProcessedValidationKind;
            validationMessage = lastProcessedValidationMessage;
            rebuildRangeIndexes();
            return LiveApplyResult.NO_CHANGE;
        }

        SceneEditorMarkdownParseResult result = codec.parse(draftText);
        if (result instanceof SceneEditorMarkdownParseResult.Success success) {
            String synchronizedText = codec.serialize(success.model());
            if (synchronizedText.equals(session.getLastAppliedText())) {
                validationKind = ValidationKind.NONE;
                validationMessage = null;
                rebuildRangeIndexes();
                rememberProcessedDraftState();
                return LiveApplyResult.NO_CHANGE;
            }
            applyParsedModel(success.model(), synchronizedText, false);
            rememberProcessedDraftState();
            return LiveApplyResult.APPLIED;
        }
        if (result instanceof SceneEditorMarkdownParseResult.SyntaxError syntaxError) {
            validationKind = ValidationKind.SYNTAX;
            validationMessage = syntaxError.message();
            rebuildRangeIndexes();
            rememberProcessedDraftState();
            return LiveApplyResult.INVALID;
        }
        if (result instanceof SceneEditorMarkdownParseResult.Unsupported unsupported) {
            validationKind = ValidationKind.UNSUPPORTED;
            validationMessage = unsupported.message();
            rebuildRangeIndexes();
            rememberProcessedDraftState();
            return LiveApplyResult.INVALID;
        }
        validationKind = ValidationKind.SYNTAX;
        validationMessage = "Unknown scene editor parse error";
        rebuildRangeIndexes();
        rememberProcessedDraftState();
        return LiveApplyResult.INVALID;
    }

    public void replaceDraftFromAppliedModel() {
        String synchronizedText = codec.serialize(session.getSceneModel());
        session.setLastAppliedText(synchronizedText);
        session.setRawText(synchronizedText);
        validationKind = ValidationKind.NONE;
        validationMessage = null;
        rebuildRangeIndexes();
        rememberProcessedDraftState();
    }

    public boolean hasValidationError() {
        return validationKind != ValidationKind.NONE;
    }

    public void recordDraftSnapshot(String mergeKey) {
        recordDraftSnapshot(mergeKey, SceneEditorUndoUiState.empty());
    }

    public void recordDraftSnapshot(String mergeKey, @Nullable SceneEditorUndoUiState uiState) {
        recordDraftSnapshot(mergeKey, uiState, true);
    }

    public void recordDraftSnapshot(String mergeKey, @Nullable SceneEditorUndoUiState uiState, boolean keepOpen) {
        ensureHistoryInitialized();
        session.getUndoHistory()
            .pushOrMerge(
                mergeKey,
                session.captureContentSnapshot(validationKind, validationMessage, uiState),
                keepOpen);
    }

    public void recordAppliedSnapshot(String mergeKey) {
        recordAppliedSnapshot(mergeKey, SceneEditorUndoUiState.empty());
    }

    public void recordAppliedSnapshot(String mergeKey, @Nullable SceneEditorUndoUiState uiState) {
        recordAppliedSnapshot(mergeKey, uiState, mergeKey != null);
    }

    public void recordAppliedSnapshot(String mergeKey, @Nullable SceneEditorUndoUiState uiState, boolean keepOpen) {
        ensureHistoryInitialized();
        session.getUndoHistory()
            .pushOrMerge(mergeKey, session.captureContentSnapshot(ValidationKind.NONE, null, uiState), keepOpen);
    }

    public void recordCurrentSnapshot(String mergeKey, @Nullable SceneEditorUndoUiState uiState) {
        recordCurrentSnapshot(mergeKey, uiState, true);
    }

    public void recordCurrentSnapshot(String mergeKey, @Nullable SceneEditorUndoUiState uiState, boolean keepOpen) {
        ensureHistoryInitialized();
        session.getUndoHistory()
            .pushOrMerge(
                mergeKey,
                session.captureContentSnapshot(validationKind, validationMessage, uiState),
                keepOpen);
    }

    public ValidationKind getValidationKind() {
        return validationKind;
    }

    public void restoreSnapshot(SceneEditorUndoSnapshot snapshot) {
        session.restoreContentSnapshot(snapshot);
        validationKind = snapshot.getValidationKind();
        validationMessage = snapshot.getValidationMessage();
        rebuildRangeIndexes();
        rememberProcessedDraftState();
    }

    public SceneEditorMarkdownElementRangeIndex getDisplayRangeIndex() {
        if (shouldUseDraftRangeIndex() && !draftRangeIndex.isEmpty()) {
            return draftRangeIndex;
        }
        return appliedRangeIndex;
    }

    @Nullable
    public String getValidationMessage() {
        return validationMessage;
    }

    private void ensureHistoryInitialized() {
        if (session.getUndoHistory()
            .hasSnapshots()) {
            return;
        }
        String baselineText = session.getLastAppliedText();
        session.getUndoHistory()
            .pushStandalone(
                new SceneEditorUndoSnapshot(
                    baselineText,
                    baselineText,
                    session.getSceneModel()
                        .copy(),
                    ValidationKind.NONE,
                    null));
    }

    private void applyParsedModel(com.hfstudio.guidenh.guide.internal.editor.model.SceneEditorSceneModel model,
        String synchronizedText, boolean normalizeRawText) {
        String previousStructureSource = session.getSceneModel()
            .getStructureSource();
        session.setSceneModel(model);
        session.setLastAppliedText(synchronizedText);
        if (normalizeRawText) {
            session.setRawText(synchronizedText);
        }
        if (!Objects.equals(previousStructureSource, model.getStructureSource())) {
            session.setImportedStructureSnbt(null);
        }
        validationKind = ValidationKind.NONE;
        validationMessage = null;
        rebuildRangeIndexes();
    }

    private boolean shouldUseDraftRangeIndex() {
        return validationKind != ValidationKind.NONE || !session.getRawText()
            .equals(session.getLastAppliedText());
    }

    private void rememberProcessedDraftState() {
        lastProcessedDraftText = session.getRawText();
        lastProcessedValidationKind = validationKind;
        lastProcessedValidationMessage = validationMessage;
    }

    private void rebuildRangeIndexes() {
        appliedRangeIndex = SceneEditorMarkdownElementRangeIndex.fromBestEffortText(
            session.getLastAppliedText(),
            session.getSceneModel()
                .getElements());
        draftRangeIndex = shouldUseDraftRangeIndex() ? SceneEditorMarkdownElementRangeIndex.fromBestEffortText(
            session.getRawText(),
            session.getSceneModel()
                .getElements())
            : appliedRangeIndex;
    }
}
