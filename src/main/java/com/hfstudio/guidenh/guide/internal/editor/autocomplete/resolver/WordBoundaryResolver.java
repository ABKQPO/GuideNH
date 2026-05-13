package com.hfstudio.guidenh.guide.internal.editor.autocomplete.resolver;

import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.guide.internal.editor.autocomplete.SyntaxContextResolver;
import com.hfstudio.guidenh.guide.internal.editor.autocomplete.SyntaxElementType;
import com.hfstudio.guidenh.guide.internal.editor.autocomplete.TextSyntaxContext;

public final class WordBoundaryResolver implements SyntaxContextResolver {

    @Override
    @Nullable
    public TextSyntaxContext resolve(String text, int cursorIndex) {
        if (text == null || text.isEmpty() || cursorIndex < 0 || cursorIndex > text.length()) {
            return null;
        }

        int start = cursorIndex;
        while (start > 0 && isWordChar(text.charAt(start - 1))) {
            start--;
        }

        int end = cursorIndex;
        while (end < text.length() && isWordChar(text.charAt(end))) {
            end++;
        }

        if (start == end && start == cursorIndex) {
            return new TextSyntaxContext(SyntaxElementType.OTHER, cursorIndex, cursorIndex, null);
        }

        return new TextSyntaxContext(SyntaxElementType.WORD, start, end, null);
    }

    private static boolean isWordChar(char c) {
        return Character.isLetterOrDigit(c) || c == '_' || c == '-' || c == '.' || c == ':';
    }
}
