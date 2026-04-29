package com.hfstudio.guidenh.guide.internal.editor.model;

import java.util.UUID;

import javax.annotation.Nullable;

public class SceneEditorSelectionState {

    @Nullable
    private UUID selectedElementId;
    @Nullable
    private String selectedHandleId;
    private boolean dragging;

    @Nullable
    public UUID getSelectedElementId() {
        return selectedElementId;
    }

    public void setSelectedElementId(@Nullable UUID selectedElementId) {
        this.selectedElementId = selectedElementId;
    }

    @Nullable
    public String getSelectedHandleId() {
        return selectedHandleId;
    }

    public void setSelectedHandleId(@Nullable String selectedHandleId) {
        this.selectedHandleId = selectedHandleId;
    }

    public boolean isDragging() {
        return dragging;
    }

    public void setDragging(boolean dragging) {
        this.dragging = dragging;
    }
}
