package com.hfstudio.guidenh.guide.internal.editor.autocomplete.resolver;

import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.guide.internal.editor.autocomplete.SyntaxContextResolver;
import com.hfstudio.guidenh.guide.internal.editor.autocomplete.SyntaxElementType;
import com.hfstudio.guidenh.guide.internal.editor.autocomplete.SyntaxUtils;
import com.hfstudio.guidenh.guide.internal.editor.autocomplete.TextSyntaxContext;

/** Detects cursor position inside YAML frontmatter (between --- delimiters at document start). */
public final class FrontmatterResolver implements SyntaxContextResolver {

    private static final String DELIM = "---";

    @Override
    @Nullable
    public TextSyntaxContext resolve(String text, int cursorIndex) {
        if (text == null || text.isEmpty()) return null;
        if (!text.startsWith(DELIM)) return null;

        int firstEnd = text.indexOf('\n', DELIM.length());
        if (firstEnd < 0) return null;
        int secondStart = text.indexOf(DELIM, firstEnd + 1);
        if (secondStart < 0) return null; // unclosed frontmatter

        if (cursorIndex < DELIM.length() || cursorIndex > secondStart) return null;

        // Find which line cursor is on
        String line = getLineAt(text, cursorIndex);
        if (line == null) return null;

        int colonIdx = line.indexOf(':');
        if (colonIdx < 0) return SyntaxUtils.resolveWord(text, cursorIndex);

        String key = line.substring(0, colonIdx).trim();
        // Skip YAML comments and list markers
        if (key.isEmpty() || key.startsWith("#") || key.startsWith("- ")) {
            return SyntaxUtils.resolveWord(text, cursorIndex);
        }

        int valueStart = colonIdx + 1;
        while (valueStart < line.length() && line.charAt(valueStart) == ' ') valueStart++;

        int lineStart = text.lastIndexOf('\n', cursorIndex - 1) + 1;
        int valueAbsStart = lineStart + valueStart;
        int valueAbsEnd = lineStart + line.length();

        // Trim trailing comment from value end
        int hashIdx = line.indexOf(" #", valueStart);
        if (hashIdx >= 0) valueAbsEnd = lineStart + hashIdx;
        valueAbsEnd = Math.min(valueAbsEnd, secondStart);

        if (cursorIndex >= valueAbsStart && cursorIndex <= valueAbsEnd) {
            String partialText = text.substring(valueAbsStart, cursorIndex);
            return new TextSyntaxContext(SyntaxElementType.WORD, valueAbsStart, valueAbsEnd,
                new FrontmatterContext(key, true, valueAbsStart, valueAbsEnd, partialText));
        }

        // Cursor is on the key
        int keyStart = lineStart + line.indexOf(key);
        int keyEnd = keyStart + key.length();
        if (cursorIndex >= keyStart && cursorIndex <= keyEnd) {
            return new TextSyntaxContext(SyntaxElementType.WORD, keyStart, keyEnd,
                new FrontmatterContext(key, false, keyStart, keyEnd,
                    text.substring(keyStart, cursorIndex)));
        }

        return SyntaxUtils.resolveWord(text, cursorIndex);
    }

    @Nullable
    private String getLineAt(String text, int cursorIndex) {
        int lineStart = text.lastIndexOf('\n', cursorIndex - 1) + 1;
        int lineEnd = text.indexOf('\n', cursorIndex);
        if (lineEnd < 0) lineEnd = text.length();
        if (lineStart >= lineEnd) return null;
        return text.substring(lineStart, lineEnd);
    }
}
