package com.hfstudio.guidenh.guide.internal.editor.guide;

public enum GuideScreenEditorLayoutMode {

    EDITOR_ONLY,
    SPLIT,
    PREVIEW_ONLY;

    public static GuideScreenEditorLayoutMode fromConfig(int value) {
        if (value <= 0) {
            return SPLIT;
        }
        if (value == 1) {
            return EDITOR_ONLY;
        }
        return PREVIEW_ONLY;
    }

    public int toConfigValue() {
        if (this == EDITOR_ONLY) {
            return 1;
        }
        if (this == PREVIEW_ONLY) {
            return 2;
        }
        return 0;
    }
}
