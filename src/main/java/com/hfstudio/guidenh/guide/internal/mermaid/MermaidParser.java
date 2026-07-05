package com.hfstudio.guidenh.guide.internal.mermaid;

import org.jetbrains.annotations.Nullable;

public class MermaidParser {

    protected MermaidParser() {}

    public static String normalizeLabel(@Nullable String text) {
        if (text == null) return "";
        return stripWrappingQuotes(
            text.replace("<br/>", "\n")
                .replace("<br />", "\n")
                .replace("<br>", "\n")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&amp;", "&")
                .trim());
    }

    public static String stripWrappingQuotes(@Nullable String text) {
        if (text == null || text.length() < 2) return text != null ? text : "";
        if ((text.startsWith("\"") && text.endsWith("\"")) || (text.startsWith("'") && text.endsWith("'"))) {
            return text.substring(1, text.length() - 1);
        }
        return text;
    }

    public static int findIdEnd(String line) {
        if (line == null || line.isEmpty()) return -1;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '[' || c == '('
                || c == '{'
                || c == '>'
                || c == '/'
                || c == '\\'
                || c == ':'
                || Character.isWhitespace(c)) {
                if (i > 0) return i;
                return -1;
            }
            if (!Character.isLetterOrDigit(c) && c != '_' && c != '-') {
                if (i == 0) return -1;
                return i;
            }
        }
        return line.length();
    }

    public static boolean isShapeStart(char c) {
        return c == '[' || c == '(' || c == '{' || c == '>' || c == '/' || c == '\\';
    }

    @Nullable
    public static String extractLeadingId(String text) {
        if (text == null || text.isEmpty()) return null;
        int end = findIdEnd(text);
        if (end <= 0) return null;
        return text.substring(0, end);
    }

    public enum NodeShapePattern {

        SUBPROCESS("[[", "]]", MermaidNodeShape.SUBPROCESS),
        CYLINDER("[(", ")]", MermaidNodeShape.CYLINDER),
        STADIUM("([", "])", MermaidNodeShape.STADIUM),
        ASYMMETRIC(">", "]", MermaidNodeShape.ASYMMETRIC),
        HEXAGON("{{", "}}", MermaidNodeShape.HEXAGON),
        DIAMOND("{", "}", MermaidNodeShape.DIAMOND),
        TRAPEZOID_A("[/", "/]", MermaidNodeShape.TRAPEZOID),
        TRAPEZOID_B("[\\", "\\]", MermaidNodeShape.TRAPEZOID),
        TRAPEZOID_C("/[", "]/", MermaidNodeShape.TRAPEZOID),
        TRAPEZOID_D("\\", "\\", MermaidNodeShape.TRAPEZOID),
        SQUARE("[", "]", MermaidNodeShape.SQUARE),
        DOUBLE_CIRCLE("((", "))", MermaidNodeShape.DOUBLE_CIRCLE),
        ROUNDED("(", ")", MermaidNodeShape.ROUNDED),
        BANG("))", "((", MermaidNodeShape.BANG),
        CLOUD(")", "(", MermaidNodeShape.CLOUD);

        final String open;
        final String close;
        final MermaidNodeShape shape;

        NodeShapePattern(String open, String close, MermaidNodeShape shape) {
            this.open = open;
            this.close = close;
            this.shape = shape;
        }

        @Nullable
        public static NodeShapeResult match(String rest) {
            if (rest == null || rest.isEmpty()) return null;
            for (NodeShapePattern pattern : values()) {
                // BANG/CLOUD are declared after all valid flowchart shapes, so they
                // can never match first in this startsWith loop — no flowchart node
                // shape syntax starts with ) or )).
                if (rest.startsWith(pattern.open) && rest.endsWith(pattern.close)) {
                    String inner = rest.substring(pattern.open.length(), rest.length() - pattern.close.length())
                        .trim();
                    if (pattern == DOUBLE_CIRCLE) {
                        if (inner.startsWith("(") && inner.endsWith(")")) {
                            return new NodeShapeResult(
                                null,
                                inner.substring(1, inner.length() - 1)
                                    .trim(),
                                MermaidNodeShape.DOUBLE_CIRCLE);
                        }
                        return new NodeShapeResult(null, inner, MermaidNodeShape.CIRCLE);
                    }
                    return new NodeShapeResult(null, inner, pattern.shape);
                }
            }
            return null;
        }

        @Nullable
        public static ShapeMatch tryParseShape(String text) {
            if (text == null || text.isEmpty()) return null;
            for (NodeShapePattern pattern : values()) {
                int openIndex = text.indexOf(pattern.open);
                if (openIndex < 0 || !text.endsWith(pattern.close)) continue;
                int length = text.length() - pattern.close.length();
                if (openIndex + pattern.open.length() > length) continue;
                String prefix = text.substring(0, openIndex)
                    .trim();
                String label = text.substring(openIndex + pattern.open.length(), length)
                    .trim();
                if (pattern == DOUBLE_CIRCLE) {
                    if (label.startsWith("(") && label.endsWith(")")) {
                        return new ShapeMatch(
                            prefix,
                            label.substring(1, label.length() - 1)
                                .trim(),
                            MermaidNodeShape.DOUBLE_CIRCLE);
                    }
                    return new ShapeMatch(prefix, label, MermaidNodeShape.CIRCLE);
                }
                return new ShapeMatch(prefix, label, pattern.shape);
            }
            return null;
        }
    }

    public record NodeShapeResult(@Nullable String prefix, String label, MermaidNodeShape shape) {}

    public record ShapeMatch(String prefix, String label, MermaidNodeShape shape) {}
}
