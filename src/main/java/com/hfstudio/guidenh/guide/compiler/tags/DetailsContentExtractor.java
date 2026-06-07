package com.hfstudio.guidenh.guide.compiler.tags;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.guide.internal.util.GuideStringLines;

public class DetailsContentExtractor {

    private static final Pattern SUMMARY_PATTERN = Pattern
        .compile("(?is)^\\s*<\\s*summary\\b[^>]*>(.*?)</\\s*summary\\s*>\\s*(?:\\R)?");

    private DetailsContentExtractor() {}

    public static DetailsContent extract(@Nullable String source) {
        String body = source != null ? source : "";
        Matcher matcher = SUMMARY_PATTERN.matcher(body);
        if (!matcher.find()) {
            return new DetailsContent(null, body);
        }
        return new DetailsContent(matcher.group(1), body.substring(matcher.end()));
    }

    public static String dedent(String body) {
        String normalized = GuideStringLines.normalizeLineEndings(body != null ? body : "");
        if (normalized.isEmpty()) {
            return normalized;
        }

        var lines = GuideStringLines.splitLines(normalized);
        int firstContentLine = 0;
        while (firstContentLine < lines.size() && lines.get(firstContentLine)
            .trim()
            .isEmpty()) {
            firstContentLine++;
        }

        int minIndent = Integer.MAX_VALUE;
        for (int i = firstContentLine; i < lines.size(); i++) {
            String line = lines.get(i);
            if (line.trim()
                .isEmpty()) {
                continue;
            }
            minIndent = Math.min(minIndent, leadingWhitespaceWidth(line));
        }
        if (minIndent == Integer.MAX_VALUE) {
            minIndent = 0;
        }

        StringBuilder result = new StringBuilder(normalized.length());
        for (int i = firstContentLine; i < lines.size(); i++) {
            if (i > firstContentLine) {
                result.append('\n');
            }
            result.append(removeLeadingWhitespace(lines.get(i), minIndent));
        }

        while (result.length() > 0 && result.charAt(result.length() - 1) == '\n') {
            result.setLength(result.length() - 1);
        }
        if (body != null && body.equals(normalized) && body.endsWith("\n")) {
            result.append('\n');
        }
        return result.toString();
    }

    private static int leadingWhitespaceWidth(String line) {
        int width = 0;
        for (int i = 0; i < line.length(); i++) {
            char ch = line.charAt(i);
            if (ch == ' ') {
                width++;
            } else if (ch == '\t') {
                width += 4;
            } else {
                break;
            }
        }
        return width;
    }

    private static String removeLeadingWhitespace(String line, int widthToRemove) {
        int index = 0;
        int removed = 0;
        while (index < line.length() && removed < widthToRemove) {
            char ch = line.charAt(index);
            if (ch == ' ') {
                removed++;
                index++;
            } else if (ch == '\t') {
                removed += 4;
                index++;
            } else {
                break;
            }
        }
        return line.substring(index);
    }

    public record DetailsContent(@Nullable String summaryMarkdown, String bodyMarkdown) {}
}
