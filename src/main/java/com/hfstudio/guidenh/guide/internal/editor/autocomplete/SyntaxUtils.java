package com.hfstudio.guidenh.guide.internal.editor.autocomplete;

/** Shared text-scanning utilities for syntax resolvers. */
public final class SyntaxUtils {

    private SyntaxUtils() {}

    /** Returns true if {@code c} is a word character (letter, digit, or common separators). */
    public static boolean isWordChar(char c) {
        return Character.isLetterOrDigit(c) || c == '_' || c == '-' || c == '.' || c == ':';
    }

    /** Resolves cursor to word boundaries in plain text. */
    public static TextSyntaxContext resolveWord(String text, int cursorIndex) {
        int start = cursorIndex;
        while (start > 0 && isWordChar(text.charAt(start - 1))) start--;
        int end = cursorIndex;
        while (end < text.length() && isWordChar(text.charAt(end))) end++;
        return new TextSyntaxContext(SyntaxElementType.WORD, start, end, null);
    }
}
