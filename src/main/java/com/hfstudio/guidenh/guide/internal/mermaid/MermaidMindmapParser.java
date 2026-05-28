package com.hfstudio.guidenh.guide.internal.mermaid;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.github.bsideup.jabel.Desugar;
import com.hfstudio.guidenh.guide.internal.util.GuideStringLines;

public class MermaidMindmapParser {

    private static final Pattern CLASS_SUFFIX = Pattern.compile(":::([A-Za-z0-9_\\- ]+)$");
    private static final Pattern ICON_SUFFIX = Pattern.compile("::icon\\(([^)]*)\\)");
    private static final Pattern POSITION_SUFFIX = Pattern.compile("::pos\\(([-+]?\\d+)\\s*,\\s*([-+]?\\d+)\\)$");

    protected MermaidMindmapParser() {}

    public static String normalize(String source) {
        if (source == null || source.isEmpty()) {
            return "";
        }
        return GuideStringLines.normalizeLineEndings(source);
    }

    public static MermaidMindmapDocument parse(String source) {
        String normalized = normalize(source);
        List<String> lines = GuideStringLines.splitLines(normalized);
        MermaidMindmapLayoutMode layoutMode = MermaidMindmapLayoutMode.MINDMAP;
        int index = 0;

        if (!lines.isEmpty() && "---".equals(
            lines.get(0)
                .trim())) {
            int end = findFrontmatterEnd(lines);
            if (end > 0) {
                layoutMode = parseFrontmatter(lines.subList(1, end));
                index = end + 1;
            }
        }

        while (index < lines.size() && shouldSkipPreamble(lines.get(index))) {
            index++;
        }

        if (index >= lines.size() || !"mindmap".equals(
            lines.get(index)
                .trim())) {
            throw new IllegalArgumentException(
                "Mermaid runtime support currently requires a 'mindmap' root declaration.");
        }
        index++;

        MermaidMindmapNode root = null;
        Deque<StackEntry> stack = new ArrayDeque<>();
        for (; index < lines.size(); index++) {
            String rawLine = lines.get(index);
            String trimmed = rawLine.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("%%")) {
                continue;
            }

            MermaidMindmapNode node = parseNode(trimmed);
            int indent = countIndent(rawLine);

            if (root == null) {
                root = node;
                stack.push(new StackEntry(indent, node));
                continue;
            }

            while (!stack.isEmpty() && stack.peek()
                .indent() >= indent) {
                stack.pop();
            }
            if (stack.isEmpty()) {
                throw new IllegalArgumentException("Mermaid mindmap must have exactly one root node.");
            }

            stack.peek()
                .node()
                .addChild(node);
            stack.push(new StackEntry(indent, node));
        }

        if (root == null) {
            throw new IllegalArgumentException("Mermaid mindmap is missing its root node.");
        }

        return new MermaidMindmapDocument(layoutMode, root);
    }

    private static boolean shouldSkipPreamble(String line) {
        String trimmed = line != null ? line.trim() : "";
        return trimmed.isEmpty() || trimmed.startsWith("%%");
    }

    private static int findFrontmatterEnd(List<String> lines) {
        for (int i = 1; i < lines.size(); i++) {
            if ("---".equals(
                lines.get(i)
                    .trim())) {
                return i;
            }
        }
        return -1;
    }

    private static MermaidMindmapLayoutMode parseFrontmatter(List<String> lines) {
        for (String line : lines) {
            String trimmed = line.trim();
            int colon = trimmed.indexOf(':');
            if (colon <= 0) {
                continue;
            }
            String key = trimmed.substring(0, colon)
                .trim();
            if ("layout".equalsIgnoreCase(key)) {
                return MermaidMindmapLayoutMode.fromConfigValue(trimmed.substring(colon + 1));
            }
        }
        return MermaidMindmapLayoutMode.MINDMAP;
    }

    private static MermaidMindmapNode parseNode(String line) {
        String working = line != null ? line.trim() : "";
        List<String> classes = new ArrayList<>();
        while (true) {
            Matcher matcher = CLASS_SUFFIX.matcher(working);
            if (!matcher.find()) {
                break;
            }
            classes.addAll(splitClasses(matcher.group(1)));
            working = working.substring(0, matcher.start())
                .trim();
        }

        String icon = null;
        Integer posX = null;
        Integer posY = null;
        Matcher iconMatcher = ICON_SUFFIX.matcher(working);
        while (iconMatcher.find()) {
            String found = iconMatcher.group(1)
                .trim();
            if (!found.isEmpty()) {
                icon = found;
            }
            working = working.substring(0, iconMatcher.start())
                .trim()
                + working.substring(iconMatcher.end())
                    .trim();
            iconMatcher = ICON_SUFFIX.matcher(working);
        }

        Matcher posMatcher = POSITION_SUFFIX.matcher(working);
        while (posMatcher.find()) {
            posX = Integer.parseInt(posMatcher.group(1));
            posY = Integer.parseInt(posMatcher.group(2));
            working = working.substring(0, posMatcher.start())
                .trim()
                + working.substring(posMatcher.end())
                    .trim();
            posMatcher = POSITION_SUFFIX.matcher(working);
        }

        if (working.startsWith("::icon(") && working.endsWith(")")) {
            icon = working.substring("::icon(".length(), working.length() - 1)
                .trim();
            working = "";
        }

        ShapeParseResult parsedShape = tryParseShape(working);
        String id = parsedShape != null ? parsedShape.id() : "";
        String label = parsedShape != null ? parsedShape.label() : working;
        MermaidMindmapNodeShape shape = parsedShape != null ? parsedShape.shape() : MermaidMindmapNodeShape.DEFAULT;

        String labelSource = normalizeLabelSource(label);
        String plainText = toPlainText(labelSource);
        if (plainText.isEmpty() && icon != null && !icon.isEmpty()) {
            labelSource = formatIconLabel(icon);
            plainText = labelSource;
        }
        if (plainText.isEmpty()) {
            throw new IllegalArgumentException("Mermaid mindmap contains an empty node declaration.");
        }
        if (id.isEmpty()) {
            id = toSlugId(plainText);
        }

        return new MermaidMindmapNode(id, labelSource, plainText, shape, classes, icon, posX, posY);
    }

    private static List<String> splitClasses(String classes) {
        List<String> result = new ArrayList<>();
        int start = -1;
        for (int index = 0, length = classes.length(); index <= length; index++) {
            char value = index < length ? classes.charAt(index) : ' ';
            if (Character.isWhitespace(value)) {
                if (start >= 0) {
                    result.add(classes.substring(start, index));
                    start = -1;
                }
            } else if (start < 0) {
                start = index;
            }
        }
        return result;
    }

    private static ShapeParseResult tryParseShape(String text) {
        ShapeParseResult parsed = parseShape(text, "{{", "}}", MermaidMindmapNodeShape.HEXAGON);
        if (parsed != null) {
            return parsed;
        }

        parsed = parseShape(text, "))", "((", MermaidMindmapNodeShape.BANG);
        if (parsed != null) {
            return parsed;
        }

        parsed = parseShape(text, "((", "))", MermaidMindmapNodeShape.CIRCLE);
        if (parsed != null) {
            return parsed;
        }

        parsed = parseShape(text, ")", "(", MermaidMindmapNodeShape.CLOUD);
        if (parsed != null) {
            return parsed;
        }

        parsed = parseShape(text, "[", "]", MermaidMindmapNodeShape.SQUARE);
        if (parsed != null) {
            return parsed;
        }

        return parseShape(text, "(", ")", MermaidMindmapNodeShape.ROUNDED);
    }

    private static ShapeParseResult parseShape(String text, String open, String close, MermaidMindmapNodeShape shape) {
        if (text == null || text.isEmpty()) {
            return null;
        }
        int openIndex = text.indexOf(open);
        if (openIndex < 0 || !text.endsWith(close) || openIndex + open.length() > text.length() - close.length()) {
            return null;
        }

        String prefix = text.substring(0, openIndex)
            .trim();
        String label = text.substring(openIndex + open.length(), text.length() - close.length());
        return new ShapeParseResult(prefix, stripWrappingQuotes(label.trim()), shape);
    }

    private static String normalizeLabelSource(String text) {
        String normalized = text != null ? text : "";
        normalized = normalized.replace("<br/>", "\n")
            .replace("<br />", "\n")
            .replace("<br>", "\n")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&amp;", "&");
        return stripWrappingQuotes(normalized.trim());
    }

    private static String toPlainText(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        String normalized = text;
        normalized = normalized.replace("![", "[");
        normalized = normalized.replaceAll("\\[([^\\]]+)]\\(([^)]+)\\)", "$1");
        normalized = normalized.replaceAll("\\[([^\\]]+)]\\[([^\\]]+)]", "$1");
        normalized = normalized.replace("**", "")
            .replace("__", "")
            .replace("~~", "")
            .replace("++", "")
            .replace("^^", "")
            .replace("::", "")
            .replace("`", "")
            .replace("<", "")
            .replace(">", "");
        return stripWrappingQuotes(normalized.trim());
    }

    private static String stripWrappingQuotes(String text) {
        if (text == null || text.length() < 2) {
            return text != null ? text : "";
        }
        if ((text.startsWith("\"") && text.endsWith("\"")) || (text.startsWith("'") && text.endsWith("'"))) {
            return text.substring(1, text.length() - 1);
        }
        return text;
    }

    private static String formatIconLabel(String icon) {
        if (icon == null || icon.trim()
            .isEmpty()) {
            return "";
        }

        String trimmed = icon.trim();
        String leaf = trimmed.substring(lastWhitespaceSeparatedTokenStart(trimmed));
        if (leaf.startsWith("fa-")) {
            leaf = leaf.substring(3);
        }
        return leaf.replace('-', ' ')
            .trim();
    }

    private static String toSlugId(String label) {
        String lower = label.toLowerCase(Locale.ROOT);
        StringBuilder builder = new StringBuilder(lower.length());
        boolean previousDash = true;
        for (int index = 0; index < lower.length(); index++) {
            char value = lower.charAt(index);
            if ((value >= 'a' && value <= 'z') || (value >= '0' && value <= '9')) {
                builder.append(value);
                previousDash = false;
            } else if (!previousDash) {
                builder.append('-');
                previousDash = true;
            }
        }
        if (!builder.isEmpty() && builder.charAt(builder.length() - 1) == '-') {
            builder.setLength(builder.length() - 1);
        }
        return builder.toString();
    }

    private static int lastWhitespaceSeparatedTokenStart(String text) {
        int index = text.length() - 1;
        while (index >= 0 && !Character.isWhitespace(text.charAt(index))) {
            index--;
        }
        return index + 1;
    }

    private static int countIndent(String rawLine) {
        int indent = 0;
        for (int i = 0; i < rawLine.length(); i++) {
            char current = rawLine.charAt(i);
            if (current == ' ') {
                indent++;
            } else if (current == '\t') {
                indent += 4;
            } else {
                break;
            }
        }
        return indent;
    }

    @Desugar
    public record ShapeParseResult(String id, String label, MermaidMindmapNodeShape shape) {}

    @Desugar
    public record StackEntry(int indent, MermaidMindmapNode node) {}
}
