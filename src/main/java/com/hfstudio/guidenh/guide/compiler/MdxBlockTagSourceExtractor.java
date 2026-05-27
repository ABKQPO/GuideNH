package com.hfstudio.guidenh.guide.compiler;

import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.libs.mdast.mdx.model.MdxJsxElementFields;
import com.hfstudio.guidenh.libs.unist.UnistPosition;

public class MdxBlockTagSourceExtractor {

    private MdxBlockTagSourceExtractor() {}

    public static @Nullable String extractRawBody(MdxJsxElementFields element, @Nullable String sourceText) {
        if (element == null || sourceText == null || sourceText.isEmpty()) {
            return null;
        }

        String tagName = element.name();
        if (tagName == null || tagName.isEmpty()) {
            return null;
        }

        UnistPosition position = element.position();
        if (position == null || position.start() == null) {
            return null;
        }

        int sourceStart = position.start()
            .offset();
        if (sourceStart < 0 || sourceStart >= sourceText.length()) {
            return null;
        }

        int openingTagStart = findOpeningTagStart(sourceText, sourceStart, tagName);
        if (openingTagStart < 0) {
            return null;
        }

        int openingTagEnd = findTagEnd(sourceText, openingTagStart);
        if (openingTagEnd < 0) {
            return null;
        }
        if (isSelfClosingTag(sourceText, openingTagStart, openingTagEnd)) {
            return "";
        }

        int closingTagStart = findMatchingClosingTagStart(sourceText, openingTagEnd + 1, tagName);
        if (closingTagStart < 0) {
            return null;
        }
        return sourceText.substring(openingTagEnd + 1, closingTagStart);
    }

    private static int findOpeningTagStart(String sourceText, int sourceStart, String tagName) {
        if (matchesOpeningTagName(sourceText, sourceStart, tagName)) {
            return sourceStart;
        }

        int searchStart = sourceStart;
        while (searchStart >= 0) {
            int candidate = sourceText.lastIndexOf('<', searchStart);
            if (candidate < 0) {
                break;
            }
            if (matchesOpeningTagName(sourceText, candidate, tagName)) {
                return candidate;
            }
            searchStart = candidate - 1;
        }
        return -1;
    }

    private static int findMatchingClosingTagStart(String sourceText, int searchStart, String tagName) {
        int depth = 1;
        int searchIndex = searchStart;
        while (searchIndex < sourceText.length()) {
            int tagStart = sourceText.indexOf('<', searchIndex);
            if (tagStart < 0) {
                return -1;
            }

            if (matchesClosingTagName(sourceText, tagStart, tagName)) {
                int tagEnd = findTagEnd(sourceText, tagStart);
                if (tagEnd < 0) {
                    return -1;
                }
                depth--;
                if (depth == 0) {
                    return tagStart;
                }
                searchIndex = tagEnd + 1;
                continue;
            }

            if (matchesOpeningTagName(sourceText, tagStart, tagName)) {
                int tagEnd = findTagEnd(sourceText, tagStart);
                if (tagEnd < 0) {
                    return -1;
                }
                if (!isSelfClosingTag(sourceText, tagStart, tagEnd)) {
                    depth++;
                }
                searchIndex = tagEnd + 1;
                continue;
            }

            searchIndex = tagStart + 1;
        }
        return -1;
    }

    private static int findTagEnd(String sourceText, int tagStart) {
        char quote = 0;
        int expressionDepth = 0;
        for (int index = tagStart + 1; index < sourceText.length(); index++) {
            char value = sourceText.charAt(index);
            if (quote != 0) {
                if (value == quote && !isEscaped(sourceText, index)) {
                    quote = 0;
                }
                continue;
            }
            if (value == '"' || value == '\'') {
                quote = value;
                continue;
            }
            if (value == '{') {
                expressionDepth++;
                continue;
            }
            if (value == '}' && expressionDepth > 0) {
                expressionDepth--;
                continue;
            }
            if (value == '>' && expressionDepth == 0) {
                return index;
            }
        }
        return -1;
    }

    private static boolean matchesOpeningTagName(String sourceText, int tagStart, String tagName) {
        if (tagStart < 0 || tagStart >= sourceText.length() || sourceText.charAt(tagStart) != '<') {
            return false;
        }
        int nameStart = tagStart + 1;
        if (nameStart >= sourceText.length() || sourceText.charAt(nameStart) == '/') {
            return false;
        }
        return matchesTagName(sourceText, nameStart, tagName);
    }

    private static boolean matchesClosingTagName(String sourceText, int tagStart, String tagName) {
        if (tagStart + 2 >= sourceText.length() || sourceText.charAt(tagStart) != '<'
            || sourceText.charAt(tagStart + 1) != '/') {
            return false;
        }
        return matchesTagName(sourceText, tagStart + 2, tagName);
    }

    private static boolean matchesTagName(String sourceText, int nameStart, String tagName) {
        int nameEnd = nameStart + tagName.length();
        if (nameEnd > sourceText.length() || !sourceText.regionMatches(nameStart, tagName, 0, tagName.length())) {
            return false;
        }
        return nameEnd == sourceText.length() || isTagNameBoundary(sourceText.charAt(nameEnd));
    }

    private static boolean isTagNameBoundary(char value) {
        return Character.isWhitespace(value) || value == '/' || value == '>';
    }

    private static boolean isSelfClosingTag(String sourceText, int tagStart, int tagEnd) {
        for (int index = tagEnd - 1; index > tagStart; index--) {
            char value = sourceText.charAt(index);
            if (Character.isWhitespace(value)) {
                continue;
            }
            return value == '/';
        }
        return false;
    }

    private static boolean isEscaped(String sourceText, int index) {
        int backslashes = 0;
        for (int cursor = index - 1; cursor >= 0 && sourceText.charAt(cursor) == '\\'; cursor--) {
            backslashes++;
        }
        return backslashes % 2 != 0;
    }
}
