package com.hfstudio.guidenh.guide.internal.editor.gui;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public class SceneEditorUndoUiState {

    public static final SceneEditorUndoUiState EMPTY = new SceneEditorUndoUiState(Collections.emptyMap());

    private final Map<String, SceneEditorUndoFieldState> fieldStates;

    private SceneEditorUndoUiState(Map<String, SceneEditorUndoFieldState> fieldStates) {
        this.fieldStates = fieldStates;
    }

    public static SceneEditorUndoUiState empty() {
        return EMPTY;
    }

    public static Builder builder() {
        return new Builder();
    }

    public boolean isEmpty() {
        return fieldStates.isEmpty();
    }

    public Optional<SceneEditorUndoFieldState> getField(String fieldKey) {
        if (fieldKey == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(fieldStates.get(fieldKey));
    }

    public static class Builder {

        private final Map<String, SceneEditorUndoFieldState> fieldStates = new LinkedHashMap<>();

        public Builder put(String fieldKey, SceneEditorUndoFieldState fieldState) {
            if (fieldKey == null || fieldState == null) {
                return this;
            }
            fieldStates.put(fieldKey, fieldState);
            return this;
        }

        public SceneEditorUndoUiState build() {
            if (fieldStates.isEmpty()) {
                return empty();
            }
            return new SceneEditorUndoUiState(Collections.unmodifiableMap(new LinkedHashMap<>(fieldStates)));
        }
    }
}
