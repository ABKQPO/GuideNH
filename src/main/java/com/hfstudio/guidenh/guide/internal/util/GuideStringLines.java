package com.hfstudio.guidenh.guide.internal.util;

import java.util.ArrayList;
import java.util.List;

public class GuideStringLines {

    protected GuideStringLines() {}

    public interface LineVisitor {

        boolean accept(String line, int lineIndex);
    }

    public static String normalizeLineEndings(String text) {
        int firstCarriageReturn = text.indexOf('\r');
        if (firstCarriageReturn == -1) {
            return text;
        }

        StringBuilder normalized = new StringBuilder(text.length());
        normalized.append(text, 0, firstCarriageReturn);
        for (int index = firstCarriageReturn; index < text.length(); index++) {
            char value = text.charAt(index);
            if (value == '\r') {
                normalized.append('\n');
                if (index + 1 < text.length() && text.charAt(index + 1) == '\n') {
                    index++;
                }
            } else {
                normalized.append(value);
            }
        }
        return normalized.toString();
    }

    public static List<String> splitLines(String text) {
        List<String> lines = new ArrayList<>();
        visitLines(text, (line, lineIndex) -> {
            lines.add(line);
            return true;
        });
        return lines;
    }

    public static void visitLines(String text, LineVisitor visitor) {
        int start = 0;
        int lineIndex = 0;
        int length = text.length();
        for (int index = 0; index < length; index++) {
            char value = text.charAt(index);
            if (value != '\n' && value != '\r') {
                continue;
            }
            if (!visitor.accept(text.substring(start, index), lineIndex)) {
                return;
            }
            lineIndex++;
            if (value == '\r' && index + 1 < length && text.charAt(index + 1) == '\n') {
                index++;
            }
            start = index + 1;
        }
        visitor.accept(text.substring(start), lineIndex);
    }
}
