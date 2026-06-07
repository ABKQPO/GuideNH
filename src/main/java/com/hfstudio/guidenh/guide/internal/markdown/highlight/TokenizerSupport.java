package com.hfstudio.guidenh.guide.internal.markdown.highlight;

import java.util.ArrayList;
import java.util.List;

import com.hfstudio.guidenh.guide.internal.util.GuideStringLines;

public class TokenizerSupport {

    protected TokenizerSupport() {}

    public static void appendToken(List<CodeHighlightToken> out, String text, CodeTokenType type) {
        if (text == null || text.isEmpty()) {
            return;
        }
        if (!out.isEmpty()) {
            CodeHighlightToken last = out.get(out.size() - 1);
            if (last.type() == type) {
                out.set(out.size() - 1, new CodeHighlightToken(last.text() + text, type));
                return;
            }
        }
        out.add(new CodeHighlightToken(text, type));
    }

    public static boolean isIdentifierStart(char value) {
        return Character.isLetter(value) || value == '_' || value == '$';
    }

    public static boolean isIdentifierPart(char value) {
        return Character.isLetterOrDigit(value) || value == '_' || value == '$';
    }

    public static boolean isWhitespace(char value) {
        return Character.isWhitespace(value);
    }

    public static boolean isNumericStart(char value) {
        return Character.isDigit(value) || value == '-';
    }

    public static int skipWhitespace(String line, int index) {
        int current = index;
        while (current < line.length() && isWhitespace(line.charAt(current))) {
            current++;
        }
        return current;
    }

    public static int findQuotedLiteralEnd(String line, int start, char quote) {
        int index = start;
        while (index < line.length()) {
            char current = line.charAt(index);
            if (current == '\\') {
                index += 2;
                continue;
            }
            index++;
            if (current == quote) {
                break;
            }
        }
        return Math.min(index, line.length());
    }

    public static int findCommentStartOutsideQuotes(String line, char commentChar) {
        boolean inSingle = false;
        boolean inDouble = false;
        for (int index = 0; index < line.length(); index++) {
            char current = line.charAt(index);
            if (current == '\\') {
                index++;
                continue;
            }
            if (current == '\'' && !inDouble) {
                inSingle = !inSingle;
                continue;
            }
            if (current == '"' && !inSingle) {
                inDouble = !inDouble;
                continue;
            }
            if (!inSingle && !inDouble && current == commentChar) {
                return index;
            }
        }
        return -1;
    }

    public static List<String> splitLines(String codeText) {
        return GuideStringLines.splitLines(codeText);
    }

    public static List<CodeHighlightLine> plainLines(String codeText) {
        List<String> lines = splitLines(codeText);
        List<CodeHighlightLine> result = new ArrayList<>(Math.max(1, lines.size()));
        if (lines.isEmpty()) {
            result.add(new CodeHighlightLine(List.of()));
            return result;
        }
        for (String line : lines) {
            List<CodeHighlightToken> tokens = new ArrayList<>(1);
            appendToken(tokens, line, CodeTokenType.PLAIN);
            result.add(new CodeHighlightLine(List.copyOf(tokens)));
        }
        return result;
    }
}
