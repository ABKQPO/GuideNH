package com.hfstudio.guidenh.guide.internal.editor.autocomplete;

public interface SelectionStrategy {

    int getSelectionStart(TextSyntaxContext ctx, String text, int cursorIndex);

    int getSelectionEnd(TextSyntaxContext ctx, String text, int cursorIndex);
}
