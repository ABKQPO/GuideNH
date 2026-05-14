package com.hfstudio.guidenh.guide.internal.editor.autocomplete.resolver;

import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.guide.internal.editor.autocomplete.SyntaxContextResolver;
import com.hfstudio.guidenh.guide.internal.editor.autocomplete.SyntaxElementType;
import com.hfstudio.guidenh.guide.internal.editor.autocomplete.TextSyntaxContext;

public class FencedBlockLanguageResolver implements SyntaxContextResolver {

    @Override
    @Nullable
    public TextSyntaxContext resolve(String text, int cursorIndex) {
        if (text == null || cursorIndex < 0 || cursorIndex > text.length()) {
            return null;
        }
        int lineStart = findLineStart(text, cursorIndex);
        int fenceStart = skipLeadingSpaces(text, lineStart, cursorIndex);
        if (fenceStart + 3 > cursorIndex || !startsWithFence(text, fenceStart)) {
            return null;
        }
        if (!isOpeningFence(text, lineStart)) {
            return null;
        }
        int languageStart = fenceStart + 3;
        int replaceStart = skipSpaces(text, languageStart, cursorIndex);
        if (cursorIndex < replaceStart || containsWhitespace(text, replaceStart, cursorIndex)) {
            return null;
        }
        String partial = text.substring(replaceStart, cursorIndex);
        return new TextSyntaxContext(
            SyntaxElementType.FENCE_LANGUAGE,
            replaceStart,
            cursorIndex,
            new FenceLanguageContext(replaceStart, cursorIndex, partial));
    }

    private static int findLineStart(String text, int cursorIndex) {
        int pos = Math.min(cursorIndex, text.length());
        while (pos > 0) {
            char previous = text.charAt(pos - 1);
            if (previous == '\n' || previous == '\r') {
                break;
            }
            pos--;
        }
        return pos;
    }

    private static int skipLeadingSpaces(String text, int lineStart, int cursorIndex) {
        int pos = lineStart;
        int max = Math.min(cursorIndex, text.length());
        while (pos < max && text.charAt(pos) == ' ') {
            pos++;
        }
        return pos;
    }

    private static int skipSpaces(String text, int start, int cursorIndex) {
        int pos = start;
        int max = Math.min(cursorIndex, text.length());
        while (pos < max && text.charAt(pos) == ' ') {
            pos++;
        }
        return pos;
    }

    private static boolean startsWithFence(String text, int fenceStart) {
        return isFenceRun(text, fenceStart, '`') || isFenceRun(text, fenceStart, '~');
    }

    private static boolean isFenceRun(String text, int fenceStart, char marker) {
        return fenceStart + 2 < text.length() && text.charAt(fenceStart) == marker
            && text.charAt(fenceStart + 1) == marker
            && text.charAt(fenceStart + 2) == marker;
    }

    private static boolean isOpeningFence(String text, int currentLineStart) {
        int fenceCount = 0;
        int lineStart = 0;
        while (lineStart < currentLineStart) {
            int lineEnd = findLineEnd(text, lineStart);
            int fenceStart = skipLeadingSpaces(text, lineStart, lineEnd);
            if (startsWithFence(text, fenceStart)) {
                fenceCount++;
            }
            lineStart = nextLineStart(text, lineEnd);
        }
        return fenceCount % 2 == 0;
    }

    private static int findLineEnd(String text, int lineStart) {
        int pos = lineStart;
        while (pos < text.length()) {
            char c = text.charAt(pos);
            if (c == '\n' || c == '\r') {
                break;
            }
            pos++;
        }
        return pos;
    }

    private static int nextLineStart(String text, int lineEnd) {
        int pos = lineEnd;
        if (pos < text.length() && text.charAt(pos) == '\r') {
            pos++;
        }
        if (pos < text.length() && text.charAt(pos) == '\n') {
            pos++;
        }
        return pos;
    }

    private static boolean containsWhitespace(String text, int start, int end) {
        for (int i = start; i < end; i++) {
            if (Character.isWhitespace(text.charAt(i))) {
                return true;
            }
        }
        return false;
    }
}
