package com.hfstudio.guidenh.guide.internal.mermaid;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.github.bsideup.jabel.Desugar;

public final class MermaidMindmapParser {

    private static final Pattern CLASS_SUFFIX = Pattern.compile(":::([A-Za-z0-9_\\- ]+)$");
    private static final Pattern ICON_SUFFIX = Pattern.compile("::icon\\(([^)]*)\\)");
    private static final Pattern POSITION_SUFFIX = Pattern.compile("::pos\\(([-+]?\\d+)\\s*,\\s*([-+]?\\d+)\\)$");

    private MermaidMindmapParser() {}

    public static String normalize(String source) {
        if (source == null || source.isEmpty()) {
            return "";
        }
        return source.replace("\r\n", "\n")
            .replace('\r', '\n');
    }

    public static MermaidMindmapDocument parse(String source) {
        String normalized = normalize(source);
        List<String> lines = Arrays.asList(normalized.split("\n", -1));
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

        String normalizedLabel = normalizeLabel(label);
        if (normalizedLabel.isEmpty() && icon != null && !icon.isEmpty()) {
            normalizedLabel = formatIconLabel(icon);
        }
        if (normalizedLabel.isEmpty()) {
            throw new IllegalArgumentException("Mermaid mindmap contains an empty node declaration.");
        }
        if (id.isEmpty()) {
            id = normalizedLabel.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-|-$)", "");
        }

        return new MermaidMindmapNode(id, normalizedLabel, shape, classes, icon, posX, posY);
    }

    private static List<String> splitClasses(String classes) {
        List<String> result = new ArrayList<>();
        for (String value : classes.split("\\s+")) {
            String trimmed = value.trim();
            if (!trimmed.isEmpty()) {
                result.add(trimmed);
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

    private static String normalizeLabel(String text) {
        String normalized = text != null ? text : "";
        normalized = normalized.replace("<br/>", "\n")
            .replace("<br />", "\n")
            .replace("<br>", "\n")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&amp;", "&")
            .replace("**", "")
            .replace("__", "")
            .replace("~~", "")
            .replace("`", "");
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

        String[] parts = icon.trim()
            .split("\\s+");
        String leaf = parts[parts.length - 1];
        if (leaf.startsWith("fa-")) {
            leaf = leaf.substring(3);
        }
        return leaf.replace('-', ' ')
            .trim();
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
