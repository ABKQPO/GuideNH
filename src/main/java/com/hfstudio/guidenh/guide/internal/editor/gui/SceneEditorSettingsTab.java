package com.hfstudio.guidenh.guide.internal.editor.gui;

import java.util.List;

import com.hfstudio.guidenh.guide.internal.GuidebookText;

public enum SceneEditorSettingsTab {

    CAMERA(GuidebookText.SceneEditorTabCamera, 0, 6),
    ROTATION(GuidebookText.SceneEditorTabRotation, 6, 9),
    PREVIEW(GuidebookText.SceneEditorTabPreview, 9, 12);

    private final GuidebookText textKey;
    private final int startIndex;
    private final int endExclusive;

    SceneEditorSettingsTab(GuidebookText textKey, int startIndex, int endExclusive) {
        this.textKey = textKey;
        this.startIndex = startIndex;
        this.endExclusive = endExclusive;
    }

    public GuidebookText getTextKey() {
        return textKey;
    }

    public int rowCount() {
        return Math.max(0, endExclusive - startIndex);
    }

    public <T> List<T> visibleRows(List<T> rows) {
        int safeEnd = Math.min(endExclusive, rows.size());
        int safeStart = Math.min(startIndex, safeEnd);
        return rows.subList(safeStart, safeEnd);
    }
}
