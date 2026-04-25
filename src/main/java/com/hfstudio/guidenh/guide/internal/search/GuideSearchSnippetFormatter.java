package com.hfstudio.guidenh.guide.internal.search;

import java.util.ArrayList;
import java.util.List;

import com.github.bsideup.jabel.Desugar;
import com.hfstudio.guidenh.guide.document.DefaultStyles;
import com.hfstudio.guidenh.guide.document.block.LytVisitor;
import com.hfstudio.guidenh.guide.document.flow.LytFlowContent;
import com.hfstudio.guidenh.guide.document.flow.LytFlowSpan;
import com.hfstudio.guidenh.guide.document.flow.LytFlowText;

public final class GuideSearchSnippetFormatter {

    private static final String START = "<B>";
    private static final String END = "</B>";
    private static final String ELLIPSIS = "...";

    private GuideSearchSnippetFormatter() {}

    public static LytFlowContent format(String fragmentMarkup) {
        var expanded = expandHighlightMarkup(fragmentMarkup == null ? "" : fragmentMarkup);
        var plain = new StringBuilder();
        var ranges = parseRanges(expanded, plain);
        return buildFlowContent(plain.toString(), ranges);
    }

    public static LytFlowContent clipToVisibleChars(LytFlowContent content, int maxVisibleChars) {
        if (countVisibleChars(content) <= maxVisibleChars) {
            return content;
        }
        return clipNode(content, maxVisibleChars);
    }

    public static LytFlowContent clipToVisibleCharsWithEllipsis(LytFlowContent content, int maxVisibleChars) {
        int visibleChars = countVisibleChars(content);
        if (visibleChars <= maxVisibleChars) {
            return content;
        }
        if (maxVisibleChars <= 0) {
            return new LytFlowSpan();
        }
        if (maxVisibleChars <= ELLIPSIS.length()) {
            return LytFlowText.of(ELLIPSIS.substring(0, maxVisibleChars));
        }

        var plainText = toPlainText(content);
        int clippedChars = Math.max(0, maxVisibleChars - ELLIPSIS.length());
        while (clippedChars > 0 && Character.isWhitespace(plainText.charAt(clippedChars - 1))) {
            clippedChars--;
        }

        var clipped = clipNode(content, clippedChars);
        if (clipped instanceof LytFlowSpan span) {
            span.appendText(ELLIPSIS);
            return span;
        }

        var root = new LytFlowSpan();
        root.append(clipped);
        root.appendText(ELLIPSIS);
        return root;
    }

    static String expandHighlightMarkup(String fragmentMarkup) {
        var plain = new StringBuilder();
        var ranges = parseRanges(fragmentMarkup, plain);
        if (ranges.isEmpty()) {
            return plain.toString();
        }

        var expanded = new ArrayList<IntRange>(ranges.size());
        for (var range : ranges) {
            int start = range.startInclusive();
            int end = range.endExclusive();
            while (start > 0 && isTokenChar(plain.charAt(start - 1)) && isTokenChar(plain.charAt(start))) {
                start--;
            }
            while (end < plain.length() && isTokenChar(plain.charAt(end - 1)) && isTokenChar(plain.charAt(end))) {
                end++;
            }
            if (!expanded.isEmpty() && start <= expanded.get(expanded.size() - 1)
                .endExclusive()) {
                var previous = expanded.remove(expanded.size() - 1);
                expanded.add(new IntRange(previous.startInclusive(), Math.max(previous.endExclusive(), end)));
            } else {
                expanded.add(new IntRange(start, end));
            }
        }

        var rebuilt = new StringBuilder();
        int cursor = 0;
        for (var range : expanded) {
            if (range.startInclusive() > cursor) {
                rebuilt.append(plain, cursor, range.startInclusive());
            }
            rebuilt.append(START)
                .append(plain, range.startInclusive(), range.endExclusive())
                .append(END);
            cursor = range.endExclusive();
        }
        if (cursor < plain.length()) {
            rebuilt.append(plain, cursor, plain.length());
        }
        return rebuilt.toString();
    }

    private static List<IntRange> parseRanges(String markup, StringBuilder plain) {
        var ranges = new ArrayList<IntRange>();
        int rangeStart = -1;
        for (int i = 0; i < markup.length(); i++) {
            if (markup.startsWith(START, i)) {
                rangeStart = plain.length();
                i += START.length() - 1;
                continue;
            }
            if (markup.startsWith(END, i)) {
                ranges.add(new IntRange(rangeStart, plain.length()));
                rangeStart = -1;
                i += END.length() - 1;
                continue;
            }
            plain.append(markup.charAt(i));
        }
        return ranges;
    }

    private static boolean isTokenChar(char c) {
        return Character.isLetterOrDigit(c) || c == '_';
    }

    private static LytFlowContent buildFlowContent(String plainText, List<IntRange> ranges) {
        var root = new LytFlowSpan();
        int cursor = 0;
        for (var range : ranges) {
            if (range.startInclusive() > cursor) {
                root.appendText(plainText.substring(cursor, range.startInclusive()));
            }
            var highlighted = new LytFlowSpan();
            highlighted.setStyle(DefaultStyles.SEARCH_RESULT_HIGHLIGHT);
            highlighted.appendText(plainText.substring(range.startInclusive(), range.endExclusive()));
            root.append(highlighted);
            cursor = range.endExclusive();
        }
        if (cursor < plainText.length()) {
            root.appendText(plainText.substring(cursor));
        }
        return root;
    }

    private static int countVisibleChars(LytFlowContent content) {
        var count = new int[1];
        content.visit(new LytVisitor() {

            @Override
            public void text(String value) {
                count[0] += value.length();
            }
        });
        return count[0];
    }

    private static String toPlainText(LytFlowContent content) {
        var text = new StringBuilder();
        content.visit(new LytVisitor() {

            @Override
            public void text(String value) {
                text.append(value);
            }
        });
        return text.toString();
    }

    private static LytFlowContent clipNode(LytFlowContent content, int remainingChars) {
        if (remainingChars <= 0) {
            return new LytFlowSpan();
        }

        if (content instanceof LytFlowText textNode) {
            var clipped = new LytFlowText();
            var text = textNode.getText();
            clipped.setText(text.substring(0, Math.min(text.length(), remainingChars)));
            clipped.setStyle(textNode.getStyle());
            clipped.setHoverStyle(textNode.getHoverStyle());
            return clipped;
        }

        if (content instanceof LytFlowSpan span) {
            int spanChars = countVisibleChars(span);
            if (Boolean.TRUE.equals(
                span.getStyle()
                    .underlined())
                && spanChars > remainingChars) {
                return new LytFlowSpan();
            }

            var copy = new LytFlowSpan();
            copy.setStyle(span.getStyle());
            copy.setHoverStyle(span.getHoverStyle());

            int remaining = remainingChars;
            for (var child : span.getChildren()) {
                if (remaining <= 0) {
                    break;
                }
                var clippedChild = clipNode(child, remaining);
                int clippedChars = countVisibleChars(clippedChild);
                if (clippedChars == 0) {
                    continue;
                }
                copy.append(clippedChild);
                remaining -= clippedChars;
            }
            return copy;
        }

        return content;
    }

    @Desugar
    record IntRange(int startInclusive, int endExclusive) {

        IntRange {
            if (startInclusive < 0 || endExclusive < startInclusive) {
                throw new IndexOutOfBoundsException("Invalid range: [" + startInclusive + ", " + endExclusive + ")");
            }
        }
    }
}
