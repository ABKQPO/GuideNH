package com.hfstudio.guidenh.guide.internal.editor.guide;

import com.hfstudio.guidenh.guide.internal.util.GuideStringLines;

public class GuideScreenEditorSourceState {

    private GuideScreenEditorSourceState() {}

    public static String normalizeEditorText(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        return GuideStringLines.normalizeLineEndings(text);
    }

    public static boolean isDirty(String draftSource, String savedSource) {
        if (draftSource != null && draftSource.equals(savedSource)) {
            return false;
        }
        return !normalizeEditorText(draftSource).equals(normalizeEditorText(savedSource));
    }
}
