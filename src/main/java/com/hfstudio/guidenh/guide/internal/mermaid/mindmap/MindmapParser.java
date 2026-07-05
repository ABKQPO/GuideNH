package com.hfstudio.guidenh.guide.internal.mermaid.mindmap;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.github.bsideup.jabel.Desugar;
import com.hfstudio.guidenh.guide.internal.mermaid.MermaidNodeShape;
import com.hfstudio.guidenh.guide.internal.mermaid.MermaidParser;
import com.hfstudio.guidenh.guide.internal.mermaid.MermaidParser.NodeShapePattern;
import com.hfstudio.guidenh.guide.internal.mermaid.MermaidParser.ShapeMatch;
import com.hfstudio.guidenh.guide.internal.mermaid.MermaidSourceExtractor;
import com.hfstudio.guidenh.guide.internal.util.GuideStringLines;

public class MindmapParser {

    private static final Pattern CLASS_SUFFIX = Pattern.compile(":::([A-Za-z0-9_\\- ]+)$");
    private static final Pattern ICON_SUFFIX = Pattern.compile("::icon\\(([^)]*)\\)");
    private static final Pattern POSITION_SUFFIX = Pattern.compile("::pos\\(([-+]?\\d+)\\s*,\\s*([-+]?\\d+)\\)$");

    protected MindmapParser() {}

    public static String normalize(String source) {
        if (source == null || source.isEmpty()) {
            return "";
        }
        return GuideStringLines.normalizeLineEndings(source);
    }

    public static MindmapDocument parse(String source) {
        String normalized = normalize(source);
        List<String> lines = GuideStringLines.splitLines(normalized);
        MindmapLayoutMode layoutMode = MindmapLayoutMode.MINDMAP;
        int index = 0;

        if (!lines.isEmpty() && MermaidSourceExtractor.isFrontmatterDelimiter(lines.getFirst())) {
            int end = MermaidSourceExtractor.findFrontmatterEnd(lines);
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

        MindmapNode root = null;
        Deque<StackEntry> stack = new ArrayDeque<>();
        for (; index < lines.size(); index++) {
            String rawLine = lines.get(index);
            String trimmed = rawLine.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("%%")) {
                continue;
            }

            MindmapNode node = parseNode(trimmed);
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

        return new MindmapDocument(layoutMode, root);
    }

    private static boolean shouldSkipPreamble(String line) {
        String trimmed = line != null ? line.trim() : "";
        return trimmed.isEmpty() || trimmed.startsWith("%%");
    }

    private static MindmapLayoutMode parseFrontmatter(List<String> lines) {
        for (String line : lines) {
            String trimmed = line.trim();
            int colon = trimmed.indexOf(':');
            if (colon <= 0) {
                continue;
            }
            String key = trimmed.substring(0, colon)
                .trim();
            if ("layout".equalsIgnoreCase(key)) {
                return MindmapLayoutMode.fromConfigValue(trimmed.substring(colon + 1));
            }
        }
        return MindmapLayoutMode.MINDMAP;
    }

    private static MindmapNode parseNode(String line) {
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

        ShapeMatch parsedShape = NodeShapePattern.tryParseShape(working);
        String id = parsedShape != null ? parsedShape.prefix() : "";
        String label = parsedShape != null ? parsedShape.label() : working;
        MermaidNodeShape shape = parsedShape != null ? parsedShape.shape() : MermaidNodeShape.DEFAULT;

        String labelSource = MermaidParser.normalizeLabel(label);
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

        return new MindmapNode(id, labelSource, plainText, shape, classes, icon, posX, posY);
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
            .replace("`", "");
        normalized = normalized.replaceAll("</?[a-zA-Z]+[^>]*>", "");
        return MermaidParser.stripWrappingQuotes(normalized.trim());
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
    public record StackEntry(int indent, MindmapNode node) {}
}
