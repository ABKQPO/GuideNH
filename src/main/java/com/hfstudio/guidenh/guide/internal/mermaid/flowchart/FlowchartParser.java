package com.hfstudio.guidenh.guide.internal.mermaid.flowchart;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jetbrains.annotations.Nullable;

import com.github.bsideup.jabel.Desugar;
import com.hfstudio.guidenh.guide.internal.mermaid.FrontmatterKey;
import com.hfstudio.guidenh.guide.internal.mermaid.MermaidArrowHead;
import com.hfstudio.guidenh.guide.internal.mermaid.MermaidEdgeStyle;
import com.hfstudio.guidenh.guide.internal.mermaid.MermaidNodeShape;
import com.hfstudio.guidenh.guide.internal.mermaid.MermaidSourceExtractor;
import com.hfstudio.guidenh.guide.internal.mermaid.NodeShapeDefinition;
import com.hfstudio.guidenh.guide.internal.util.GuideStringLines;

public class FlowchartParser {

    private final FlowchartGraphBuilder builder = new FlowchartGraphBuilder();
    private final List<String> lines;

    private static final Pattern STYLE_PATTERN = Pattern.compile("(\\S+)\\s+(.+)");
    private static final Pattern CLASS_DEF_PATTERN = Pattern.compile("(\\S+)\\s+(.+)");
    private static final Pattern CLASS_PATTERN = Pattern.compile("([\\w,-]+)\\s+(\\S+)");
    private static final Pattern LINK_STYLE_PATTERN = Pattern.compile("(\\S+)\\s+(.+)");
    private static final Pattern ID_PATTERN = Pattern.compile("^[\\w-]+");
    private static final Pattern PIPED_LABEL_PATTERN = Pattern.compile("\\s*\\|([^|]*)\\|");
    private static final Pattern GRAPH_KW_PATTERN = Pattern
        .compile("^(flowchart-elk|flowchart|graph|swimlane-beta)\\b");
    private static final Pattern STATEMENT_PATTERN = Pattern.compile("^(style|classDef|class|linkStyle)\\b");

    private FlowchartParser(List<String> lines) {
        this.lines = lines;
    }

    public static FlowchartDocument parse(String source) {
        if (source == null || source.trim()
            .isEmpty()) {
            return new FlowchartDocument(FlowchartDirection.TB, Map.of(), List.of(), List.of());
        }
        String normalized = GuideStringLines.normalizeLineEndings(source);
        normalized = stripNodeContentBlocks(normalized);
        List<String> lines = GuideStringLines.splitLines(normalized);
        if (lines.isEmpty()) {
            return new FlowchartDocument(FlowchartDirection.TB, Map.of(), List.of(), List.of());
        }
        return new FlowchartParser(lines).doParse();
    }

    private FlowchartDocument doParse() {
        int index = 0;

        if (MermaidSourceExtractor.isFrontmatterDelimiter(lines.get(index))) {
            index = parseFrontmatter(index);
        }

        index = skipCommentAndEmptyLines(index);

        if (index < lines.size()) {
            index = parseGraphDeclarationLine(index);
        }

        parseDocumentBody(index);

        return builder.build();
    }

    static String stripNodeContentBlocks(String source) {
        StringBuilder result = new StringBuilder(source.length());
        int depth = 0;
        for (int i = 0; i < source.length(); i++) {
            if (depth == 0 && source.startsWith("<", i)) {
                int tagEnd = findTagEnd(source, i);
                if (tagEnd > i) {
                    if (source.substring(i, tagEnd + 1)
                        .startsWith("<NodeContent")) {
                        depth = 1;
                        i = tagEnd;
                        continue;
                    }
                }
            }
            if (depth > 0) {
                if (source.startsWith("</NodeContent>", i)) {
                    depth--;
                    if (depth == 0) {
                        i += "</NodeContent>".length() - 1;
                        continue;
                    }
                } else if (source.startsWith("<NodeContent", i)) {
                    depth++;
                }
                continue;
            }
            result.append(source.charAt(i));
        }
        return result.toString();
    }

    private static int findTagEnd(String source, int from) {
        boolean inQuote = false;
        char quoteChar = 0;
        for (int i = from; i < source.length(); i++) {
            char c = source.charAt(i);
            if (inQuote) {
                if (c == quoteChar) inQuote = false;
            } else if (c == '"' || c == '\'') {
                inQuote = true;
                quoteChar = c;
            } else if (c == '>') {
                return i;
            }
        }
        return -1;
    }

    static String stripTrailingComment(String line) {
        if (line == null || line.isEmpty()) return line;
        int idx = line.indexOf("%%");
        if (idx < 0) return line;
        if (idx == 0) return "";
        return line.substring(0, idx)
            .trim();
    }

    @Nullable
    static String parseDirectionToken(String s) {
        if (s == null || s.isEmpty()) return null;
        String upper = s.toUpperCase(Locale.ROOT)
            .trim();
        if ("TD".equals(upper)) return "TB";
        return switch (upper) {
            case "TB", "BT", "LR", "RL" -> upper;
            default -> null;
        };
    }

    static String skipGraphDeclaration(String line) {
        String trimmed = line.trim();
        if (trimmed.isEmpty()) return "";
        Matcher m = GRAPH_KW_PATTERN.matcher(trimmed.toLowerCase(Locale.ROOT));
        if (!m.find()) return trimmed;
        return trimmed.substring(m.end())
            .trim();
    }

    static String normalizeLabel(@Nullable String text) {
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

    static String stripWrappingQuotes(@Nullable String text) {
        if (text == null || text.length() < 2) return text != null ? text : "";
        if ((text.startsWith("\"") && text.endsWith("\"")) || (text.startsWith("'") && text.endsWith("'"))) {
            return text.substring(1, text.length() - 1);
        }
        return text;
    }

    private static String toSlug(String label) {
        if (label == null || label.isEmpty()) return "subgraph";
        String slug = label.toLowerCase(Locale.ROOT)
            .replaceAll("[^a-z0-9]+", "-")
            .replaceAll("^-|-$", "");
        return slug.isEmpty() ? "subgraph" : slug;
    }

    private static List<String> splitStyles(String styles) {
        if (styles == null || styles.isEmpty()) return List.of();
        return Arrays.stream(styles.split(","))
            .map(String::trim)
            .filter(s -> !s.isEmpty())
            .toList();
    }

    private static List<String> splitClasses(@Nullable String s) {
        if (s == null || s.isEmpty()) return List.of();
        return Arrays.stream(
            s.trim()
                .split("\\s+"))
            .filter(cls -> !cls.isEmpty())
            .toList();
    }

    @Nullable
    private static String extractIconFromLabel(String label) {
        if (label == null || !label.startsWith("fa:")) return null;
        int space = label.indexOf(' ');
        return space > 0 ? label.substring(0, space) : label;
    }

    private static String stripIconFromLabel(String label) {
        if (label == null) return "";
        int space = label.indexOf(' ');
        return space > 0 ? label.substring(space)
            .trim() : "";
    }

    private static List<String> splitOnAmpersand(String seg) {
        List<String> parts = new ArrayList<>();
        int start = 0;
        int depth = 0;
        for (int i = 0; i < seg.length(); i++) {
            char c = seg.charAt(i);
            if (c == '[' || c == '(' || c == '{') {
                depth++;
            } else if (c == ']' || c == ')' || c == '}') {
                if (depth > 0) depth--;
            } else if (c == '&' && depth == 0) {
                if (i > start) {
                    parts.add(
                        seg.substring(start, i)
                            .trim());
                }
                start = i + 1;
            }
        }
        if (start < seg.length()) {
            parts.add(
                seg.substring(start)
                    .trim());
        }
        return parts;
    }

    private static String unquote(@Nullable String s) {
        if (s == null || s.length() < 2) return s != null ? s : "";
        char first = s.charAt(0);
        char last = s.charAt(s.length() - 1);
        if ((first == '"' && last == '"') || (first == '\'' && last == '\'')) {
            return s.substring(1, s.length() - 1)
                .trim();
        }
        return s;
    }

    @Nullable
    private static MermaidNodeShape parseShapeName(@Nullable String name) {
        if (name == null || name.isEmpty()) return null;
        return switch (name.toLowerCase(Locale.ROOT)) {
            case "square", "rect" -> MermaidNodeShape.SQUARE;
            case "rounded", "round" -> MermaidNodeShape.ROUNDED;
            case "stadium" -> MermaidNodeShape.STADIUM;
            case "subprocess", "subroutine" -> MermaidNodeShape.SUBPROCESS;
            case "diamond" -> MermaidNodeShape.DIAMOND;
            case "cylinder" -> MermaidNodeShape.CYLINDER;
            case "asymmetric", "odd" -> MermaidNodeShape.ASYMMETRIC;
            case "trapezoid", "inv_trapezoid", "inverse_trapezoid" -> MermaidNodeShape.TRAPEZOID;
            case "hexagon" -> MermaidNodeShape.HEXAGON;
            case "circle" -> MermaidNodeShape.CIRCLE;
            case "doublecircle", "double-circle" -> MermaidNodeShape.DOUBLE_CIRCLE;
            case "ellipse" -> MermaidNodeShape.ELLIPSE;
            case "cloud" -> MermaidNodeShape.CLOUD;
            case "bang" -> MermaidNodeShape.BANG;
            default -> null;
        };
    }

    private static List<String> splitExtendedPairs(String body) {
        List<String> pairs = new ArrayList<>();
        int start = 0;
        int depth = 0;
        for (int i = 0; i < body.length(); i++) {
            char c = body.charAt(i);
            if (c == ',' && depth == 0) {
                pairs.add(
                    body.substring(start, i)
                        .trim());
                start = i + 1;
            } else if (c == '{') depth++;
            else if (c == '}') depth--;
            else if ((c == '"' || c == '\'') && depth == 0) {
                int end = body.indexOf(c, i + 1);
                if (end > i) i = end;
            }
        }
        if (start < body.length()) {
            pairs.add(
                body.substring(start)
                    .trim());
        }
        return pairs;
    }

    private static int findMatchingBrace(String s, int start) {
        if (start >= s.length() || s.charAt(start) != '{') return -1;
        int depth = 1;
        for (int i = start + 1; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '{') depth++;
            else if (c == '}') {
                depth--;
                if (depth == 0) return i;
            } else if (c == '"' || c == '\'') {
                int end = s.indexOf(c, i + 1);
                if (end > i) i = end;
            }
        }
        return -1;
    }

    private int skipCommentAndEmptyLines(int start) {
        int i = start;
        while (i < lines.size()) {
            String stripped = stripTrailingComment(lines.get(i)).trim();
            if (stripped.isEmpty() || stripped.startsWith("%%")) {
                i++;
            } else {
                break;
            }
        }
        return i;
    }

    private int parseFrontmatter(int startIndex) {
        if (startIndex >= lines.size()) return startIndex;
        String first = lines.get(startIndex)
            .trim();
        if (!"---".equals(first)) return startIndex;
        int end = -1;
        for (int i = startIndex + 1; i < lines.size(); i++) {
            if ("---".equals(
                lines.get(i)
                    .trim())) {
                end = i;
                break;
            }
        }
        if (end < 0) return startIndex;
        for (int i = startIndex + 1; i < end; i++) {
            String line = lines.get(i)
                .trim();
            int colon = line.indexOf(':');
            if (colon <= 0) continue;
            String key = line.substring(0, colon)
                .trim();
            String value = line.substring(colon + 1)
                .trim();
            FrontmatterKey fk = FrontmatterKey.byKey(key);
            if (fk != null) {
                Object parsed = fk.parse(value);
                if (parsed instanceof FlowchartDirection dir) {
                    builder.setDirection(dir.name());
                } else if (parsed instanceof FlowchartLayoutMode mode) {
                    builder.setLayoutMode(mode);
                } else if (parsed instanceof Integer intVal) {
                    applyLayoutConfig(key, intVal);
                }
                continue;
            }
            Integer intVal = tryParseInt(value);
            if (intVal != null) applyLayoutConfig(key, intVal);
        }
        return end + 1;
    }

    private void applyLayoutConfig(String key, int value) {
        switch (key) {
            case "nodeSpacing" -> builder.nodeSpacing = Math.max(1, value);
            case "rankSpacing" -> builder.rankSpacing = Math.max(1, value);
            case "padding" -> builder.canvasPadding = Math.max(0, value);
        }
    }

    @Nullable
    private static Integer tryParseInt(String s) {
        if (s == null || s.isEmpty()) return null;
        try {
            return Integer.parseInt(s.trim());
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private int parseGraphDeclarationLine(int index) {
        if (index >= lines.size()) return index;
        String line = lines.get(index)
            .trim();
        String remaining = skipGraphDeclaration(line);
        if (remaining.equals(line)) return index;
        String dirToken = parseDirectionToken(remaining);
        if (dirToken != null) {
            builder.setDirection(dirToken);
        }
        if (line.toLowerCase(Locale.ROOT)
            .startsWith("flowchart-elk")) {
            builder.setLayoutMode(FlowchartLayoutMode.fromConfigValue("elk"));
        }
        return index + 1;
    }

    private void parseDocumentBody(int startIndex) {
        for (int i = startIndex; i < lines.size(); i++) {
            String line = stripTrailingComment(lines.get(i)).trim();
            if (line.isEmpty() || line.startsWith("%%")) continue;

            if (line.startsWith("subgraph ")) {
                i = parseSubgraph(i);
                continue;
            }

            if (isStatementLine(line)) {
                parseStatementByKeyword(line);
                continue;
            }

            parseVertexOrEdgeLine(line);
        }
    }

    @Nullable
    NodeSpec parseNodeSpec(String text) {
        if (text == null || text.isEmpty()) return null;
        Matcher idMatcher = ID_PATTERN.matcher(text);
        if (!idMatcher.find()) return null;
        String id = idMatcher.group();
        String rest = text.substring(idMatcher.end())
            .trim();

        if (rest.isEmpty()) {
            return new NodeSpec(id, id, MermaidNodeShape.DEFAULT, List.of(), null, false, null);
        }

        if (rest.startsWith("@{")) {
            return parseExtendedNode(id, rest);
        }

        List<String> classes = List.of();
        int classSep = rest.indexOf(":::");
        if (classSep >= 0) {
            String classPart = rest.substring(classSep + 3)
                .trim();
            classes = splitClasses(classPart);
            rest = rest.substring(0, classSep)
                .trim();
        }

        MermaidNodeShape shape = MermaidNodeShape.DEFAULT;
        String label = id;
        @Nullable
        String icon = null;

        NodeShapeDefinition.MatchResult shapeResult = NodeShapeDefinition.match(rest);
        if (shapeResult != null) {
            label = normalizeLabel(shapeResult.inner());
            if (label.isEmpty()) label = id;
            shape = shapeResult.definition()
                .shape();
            icon = extractIconFromLabel(label);
            if (icon != null) {
                label = stripIconFromLabel(label);
                if (label.isEmpty()) label = id;
            }
        }

        return new NodeSpec(id, label, shape, classes, icon, false, null);
    }

    private NodeSpec parseExtendedNode(String id, String rest) {
        int braceEnd = findMatchingBrace(rest, 1);
        if (braceEnd < 0) {
            return new NodeSpec(id, id, MermaidNodeShape.DEFAULT, List.of(), null, false, null);
        }
        String body = rest.substring(2, braceEnd)
            .trim();
        String after = rest.substring(braceEnd + 1)
            .trim();

        MermaidNodeShape shape = MermaidNodeShape.DEFAULT;
        String label = id;
        @Nullable
        String icon = null;
        @Nullable
        Map<String, String> extra = null;

        List<String> pairs = splitExtendedPairs(body);
        for (String pair : pairs) {
            int colon = pair.indexOf(':');
            if (colon <= 0) continue;
            String key = pair.substring(0, colon)
                .trim()
                .toLowerCase(Locale.ROOT);
            String value = pair.substring(colon + 1)
                .trim();
            value = unquote(value);
            switch (key) {
                case "shape" -> {
                    MermaidNodeShape s = parseShapeName(value);
                    if (s != null) shape = s;
                }
                case "label" -> label = value.isEmpty() ? id : value;
                case "icon" -> icon = value.isEmpty() ? null : value;
                default -> {
                    if (extra == null) extra = new LinkedHashMap<>();
                    extra.put(key, value);
                }
            }
        }

        List<String> classes = List.of();
        if (!after.isEmpty()) {
            int classSep = after.indexOf(":::");
            if (classSep >= 0) {
                classes = splitClasses(
                    after.substring(classSep + 3)
                        .trim());
            }
        }

        return new NodeSpec(id, label, shape, classes, icon, false, extra);
    }

    @Nullable
    PipedLabel parsePipedLabel(String line, int fromPos) {
        Matcher m = PIPED_LABEL_PATTERN.matcher(line)
            .region(fromPos, line.length());
        if (!m.lookingAt()) return null;
        String label = normalizeLabel(
            m.group(1)
                .trim());
        return new PipedLabel(label, m.end());
    }

    void registerEdge(String fromId, String toId, LinkDefinition.MatchResult match, @Nullable String label) {
        LinkDefinition def = match.definition();
        String normalizedLabel = label != null ? normalizeLabel(label) : null;
        builder.addLink(
            fromId,
            toId,
            def.style(),
            def.arrowFwd(),
            def.arrowRev(),
            def.forwardHead(),
            def.reverseHead(),
            normalizedLabel,
            null,
            Math.max(1, match.length()));
    }

    void ensureNode(@Nullable NodeSpec spec) {
        if (spec == null || spec.id()
            .isEmpty()) return;
        if (builder.nodes.containsKey(spec.id())) {
            if (!spec.classes()
                .isEmpty()) {
                FlowchartNode existing = builder.nodes.get(spec.id());
                List<String> merged = new ArrayList<>(existing.getClasses());
                for (String c : spec.classes()) {
                    if (!merged.contains(c)) merged.add(c);
                }
                builder.nodes.put(
                    spec.id(),
                    new FlowchartNode(
                        existing.getId(),
                        existing.getLabel(),
                        existing.getShape(),
                        merged,
                        existing.getStyleOverride(),
                        existing.getIcon(),
                        existing.isMarkdownLabel(),
                        existing.getExtendedProperties()));
            }
            return;
        }
        builder.addVertex(
            spec.id(),
            spec.label(),
            spec.shape(),
            spec.classes(),
            null,
            spec.icon(),
            spec.markdownLabel(),
            spec.extendedProperties());
    }

    List<Object> tokenizeLine(String line) {
        List<Object> tokens = new ArrayList<>();
        int lastEnd = 0;
        while (lastEnd < line.length()) {
            LinkDefinition.MatchResult match = LinkDefinition.matchAt(line, lastEnd);
            if (match == null) break;
            if (match.position() == lastEnd && Character.isLetter(line.charAt(lastEnd))) {
                tokens.add(line.substring(lastEnd, lastEnd + 1));
                lastEnd++;
                continue;
            }
            int linkEnd = match.position() + match.length();
            PipedLabel piped = parsePipedLabel(line, linkEnd);
            String label = piped != null ? piped.label() : null;
            if (piped != null) {
                linkEnd = piped.endPosition();
            }
            if (match.position() > lastEnd) {
                tokens.add(
                    line.substring(lastEnd, match.position())
                        .trim());
            }
            tokens.add(new TokenLink(match, label));
            lastEnd = linkEnd;
        }
        if (lastEnd < line.length()) {
            tokens.add(
                line.substring(lastEnd)
                    .trim());
        }
        return tokens;
    }

    void parseVertexOrEdgeLine(String line) {
        line = stripTrailingComment(line);
        if (line == null || line.trim().isEmpty()) return;
        line = line.trim();
        List<Object> tokens = tokenizeLine(line);
        if (tokens.isEmpty()) return;

        List<String> leftNodeIds = new ArrayList<>();
        for (int i = 0; i < tokens.size(); i++) {
            Object token = tokens.get(i);
            if (token instanceof TokenLink(LinkDefinition.MatchResult match, String label)) {
                String rightText = "";
                if (i + 1 < tokens.size() && tokens.get(i + 1) instanceof String s) {
                    rightText = s;
                    i++;
                }
                List<NodeSpec> rightNodes = parseFirstNode(rightText);
                if (!rightNodes.isEmpty()) {
                    NodeSpec right = rightNodes.getFirst();
                    ensureNode(right);
                    for (String fromId : leftNodeIds) {
                        registerEdge(fromId, right.id(), match, label);
                    }
                    leftNodeIds.clear();
                    leftNodeIds.add(right.id());
                }
            } else if (token instanceof String seg) {
                for (String part : splitOnAmpersand(seg)) {
                    NodeSpec spec = parseNodeSpec(part.trim());
                    if (spec != null) {
                        ensureNode(spec);
                        leftNodeIds.add(spec.id());
                    }
                }
            }
        }
    }

    private List<NodeSpec> parseFirstNode(@Nullable String text) {
        List<NodeSpec> result = new ArrayList<>();
        if (text == null || text.trim()
            .isEmpty()) return result;
        String trimmed = text.trim();
        int amp = trimmed.indexOf('&');
        String first = amp >= 0 ? trimmed.substring(0, amp)
            .trim() : trimmed;
        NodeSpec spec = parseNodeSpec(first);
        if (spec != null) result.add(spec);
        return result;
    }

    private int parseSubgraph(int startIndex) {
        String line = stripTrailingComment(lines.get(startIndex)).trim();
        String rest = line.substring("subgraph".length())
            .trim();

        String id;
        String label;
        int bracketStart = rest.indexOf('[');
        if (bracketStart >= 0 && rest.endsWith("]")) {
            String rawLabel = rest.substring(bracketStart + 1, rest.length() - 1)
                .trim();
            label = normalizeLabel(rawLabel);
            String idPart = rest.substring(0, bracketStart)
                .trim();
            id = idPart.isEmpty() ? toSlug(label) : idPart;
        } else if (!rest.isEmpty()) {
            label = rest;
            id = rest;
        } else {
            id = null;
            label = null;
        }

        builder.startSubgraph(id, label);

        int i = startIndex + 1;
        while (i < lines.size()) {
            String inner = stripTrailingComment(lines.get(i)).trim();
            if (inner.isEmpty() || inner.startsWith("%%")) {
                i++;
                continue;
            }
            if (inner.equals("end")) {
                builder.endSubgraph();
                return i;
            }
            if (inner.startsWith("subgraph ")) {
                i = parseSubgraph(i);
                i++;
                continue;
            }
            if (inner.startsWith("direction ")) {
                String dirToken = inner.substring("direction ".length())
                    .trim();
                String parsed = parseDirectionToken(dirToken);
                if (parsed != null) {
                    builder.setSubgraphDirection(FlowchartDirection.fromString(parsed));
                }
                i++;
                continue;
            }
            if (isStatementLine(inner)) {
                parseStatementByKeyword(inner);
                i++;
                continue;
            }
            parseVertexOrEdgeLine(inner);
            i++;
        }

        throw new IllegalArgumentException("Unclosed subgraph starting at line " + startIndex);
    }

    private boolean isStatementLine(String line) {
        return STATEMENT_PATTERN.matcher(line)
            .find();
    }

    private void parseStatementByKeyword(String line) {
        if (line.startsWith("style ")) {
            parseStyleStatement(
                line.substring("style ".length())
                    .trim());
        } else if (line.startsWith("classDef ")) {
            parseClassDefStatement(
                line.substring("classDef ".length())
                    .trim());
        } else if (line.startsWith("class ")) {
            parseClassStatement(
                line.substring("class ".length())
                    .trim());
        } else if (line.startsWith("linkStyle ")) {
            parseLinkStyleStatement(
                line.substring("linkStyle ".length())
                    .trim());
        }
    }

    private void parseStyleStatement(String rest) {
        Matcher m = STYLE_PATTERN.matcher(rest);
        if (!m.matches()) return;
        String nodeId = m.group(1);
        if (builder.subgraphIds.contains(nodeId)) return;
        List<String> parts = splitStyles(m.group(2));
        if (parts.isEmpty()) return;
        String joined = String.join(",", parts);

        if (!builder.nodes.containsKey(nodeId)) {
            NodeSpec spec = parseNodeSpec(nodeId);
            if (spec == null) return;
            builder.addVertex(
                spec.id(),
                spec.label(),
                spec.shape(),
                spec.classes(),
                null,
                spec.icon(),
                spec.markdownLabel(),
                spec.extendedProperties());
        }
        builder.nodes.computeIfPresent(
            nodeId,
            (k, existing) -> new FlowchartNode(
                existing.getId(),
                existing.getLabel(),
                existing.getShape(),
                existing.getClasses(),
                joined,
                existing.getIcon(),
                existing.isMarkdownLabel(),
                existing.getExtendedProperties()));
    }

    private void parseClassDefStatement(String rest) {
        Matcher m = CLASS_DEF_PATTERN.matcher(rest);
        if (!m.matches()) return;
        String className = m.group(1);
        String styles = m.group(2);
        builder.addClassDef(className, splitStyles(styles));
    }

    private void parseClassStatement(String rest) {
        Matcher m = CLASS_PATTERN.matcher(rest);
        if (!m.matches()) return;
        String idsPart = m.group(1);
        String className = m.group(2);
        for (String id : idsPart.split(",")) {
            String trimmed = id.trim();
            if (!trimmed.isEmpty()) {
                builder.setClass(trimmed, className);
            }
        }
    }

    private void parseLinkStyleStatement(String rest) {
        Matcher m = LINK_STYLE_PATTERN.matcher(rest);
        if (!m.matches()) return;
        String indicesPart = m.group(1);
        String remainder = m.group(2);
        List<String> indices = "default".equalsIgnoreCase(indicesPart) ? List.of("default")
            : Arrays.stream(indicesPart.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
        if (remainder.startsWith("interpolate ")) {
            String interp = remainder.substring("interpolate ".length())
                .trim();
            int styleSpace = interp.indexOf(' ');
            if (styleSpace > 0) {
                String interpolateType = interp.substring(0, styleSpace)
                    .trim();
                String styles = interp.substring(styleSpace + 1)
                    .trim();
                builder.addLinkInterpolate(indices, interpolateType);
                builder.addLinkStyle(indices, splitStyles(styles));
            } else {
                builder.addLinkInterpolate(indices, interp);
            }
        } else {
            builder.addLinkStyle(indices, splitStyles(remainder));
        }
    }

    @Desugar
    record NodeSpec(String id, String label, MermaidNodeShape shape, List<String> classes, @Nullable String icon,
        boolean markdownLabel, @Nullable Map<String, String> extendedProperties) {}

    @Desugar
    record PipedLabel(@Nullable String label, int endPosition) {}

    @Desugar
    record TokenLink(LinkDefinition.MatchResult match, @Nullable String label) {}

    static class FlowchartGraphBuilder {

        FlowchartDirection direction = FlowchartDirection.TB;
        FlowchartLayoutMode layoutMode = FlowchartLayoutMode.BUILTIN;
        final Map<String, FlowchartNode> nodes = new LinkedHashMap<>();
        final List<FlowchartEdge> edges = new ArrayList<>();
        final List<FlowchartSubgraph> subgraphs = new ArrayList<>();
        int nodeSpacing = 20;
        int rankSpacing = 20;
        int canvasPadding = 20;

        private final Map<String, List<String>> classDefs = new LinkedHashMap<>();
        private final List<LinkStyleEntry> linkStyleEntries = new ArrayList<>();
        private final Deque<SubgraphContext> subgraphStack = new ArrayDeque<>();
        final Set<String> subgraphIds = new HashSet<>();

        void setDirection(String dir) {
            if (dir != null) {
                this.direction = FlowchartDirection.fromString(dir);
            }
        }

        void setLayoutMode(FlowchartLayoutMode mode) {
            if (mode != null) {
                this.layoutMode = mode;
            }
        }

        void addVertex(String id, @Nullable String label, @Nullable MermaidNodeShape shape,
            @Nullable List<String> classes, @Nullable String styleOverride, @Nullable String icon,
            boolean markdownLabel, @Nullable Map<String, String> extendedProperties) {
            if (id == null || id.isEmpty()) return;
            if (nodes.containsKey(id)) return;
            MermaidNodeShape resolvedShape = shape != null ? shape : MermaidNodeShape.DEFAULT;
            String resolvedLabel = (label != null && !label.isEmpty()) ? label : id;
            List<String> resolvedClasses = classes != null ? classes : List.of();
            FlowchartNode node = new FlowchartNode(
                id,
                resolvedLabel,
                resolvedShape,
                resolvedClasses,
                styleOverride,
                icon,
                markdownLabel,
                extendedProperties);
            nodes.put(id, node);
            if (!subgraphStack.isEmpty()) {
                subgraphStack.peek().nodeIds.add(id);
            }
        }

        void addLink(String fromId, String toId, MermaidEdgeStyle style, boolean arrowFwd, boolean arrowRev,
            MermaidArrowHead forwardHead, MermaidArrowHead reverseHead, @Nullable String label, @Nullable String edgeId,
            int length) {
            FlowchartEdge edge = new FlowchartEdge(
                fromId,
                toId,
                label,
                style,
                arrowFwd,
                arrowRev,
                forwardHead,
                reverseHead,
                edgeId,
                length);
            edges.add(edge);
            if (!subgraphStack.isEmpty()) {
                subgraphStack.peek().edges.add(edge);
            }
        }

        SubgraphContext startSubgraph(@Nullable String id, @Nullable String label) {
            SubgraphContext ctx = new SubgraphContext();
            ctx.id = id != null ? id : autoSubgraphId();
            ctx.label = label != null ? label : ctx.id;
            subgraphIds.add(ctx.id);
            if (!subgraphStack.isEmpty()) {
                ctx.parent = subgraphStack.peek();
            }
            subgraphStack.push(ctx);
            return ctx;
        }

        void endSubgraph() {
            if (subgraphStack.isEmpty()) return;
            SubgraphContext ctx = subgraphStack.pop();
            FlowchartSubgraph sg = new FlowchartSubgraph(
                ctx.id,
                ctx.label,
                new ArrayList<>(ctx.nodeIds),
                new ArrayList<>(ctx.edges),
                new ArrayList<>(ctx.children),
                ctx.direction);
            if (!subgraphStack.isEmpty()) {
                subgraphStack.peek().children.add(sg);
            } else {
                subgraphs.add(sg);
            }
        }

        void setSubgraphDirection(FlowchartDirection dir) {
            if (!subgraphStack.isEmpty()) {
                subgraphStack.peek().direction = dir;
            }
        }

        void setClass(String nodeId, String className) {
            FlowchartNode existing = nodes.get(nodeId);
            if (existing == null) return;
            List<String> merged = new ArrayList<>(existing.getClasses());
            if (!merged.contains(className)) {
                merged.add(className);
            }
            nodes.put(
                nodeId,
                new FlowchartNode(
                    existing.getId(),
                    existing.getLabel(),
                    existing.getShape(),
                    merged,
                    existing.getStyleOverride(),
                    existing.getIcon(),
                    existing.isMarkdownLabel(),
                    existing.getExtendedProperties()));
        }

        void addClassDef(String className, List<String> styles) {
            classDefs.put(className, styles);
        }

        void addLinkStyle(List<String> indices, List<String> styles) {
            linkStyleEntries.add(new LinkStyleEntry(indices, styles, null));
        }

        void addLinkInterpolate(List<String> indices, String interpolate) {
            linkStyleEntries.add(new LinkStyleEntry(indices, null, interpolate));
        }

        FlowchartDocument build() {
            resolveClassStyles();
            applyLinkStyles();
            var cfg = new FlowchartDocument.FlowchartConfig(nodeSpacing, rankSpacing, canvasPadding);
            return new FlowchartDocument(direction, nodes, edges, subgraphs, layoutMode, cfg);
        }

        private void resolveClassStyles() {
            for (Map.Entry<String, FlowchartNode> entry : nodes.entrySet()) {
                String nodeId = entry.getKey();
                FlowchartNode node = entry.getValue();
                if (node.getClasses()
                    .isEmpty()) continue;
                List<String> resolved = new ArrayList<>();
                for (String className : node.getClasses()) {
                    List<String> classStyles = classDefs.get(className);
                    if (classStyles != null) resolved.addAll(classStyles);
                }
                if (node.getStyleOverride() != null) {
                    resolved.add(node.getStyleOverride());
                }
                if (!resolved.isEmpty()) {
                    nodes.put(
                        nodeId,
                        new FlowchartNode(
                            node.getId(),
                            node.getLabel(),
                            node.getShape(),
                            node.getClasses(),
                            String.join(",", resolved),
                            node.getIcon(),
                            node.isMarkdownLabel(),
                            node.getExtendedProperties()));
                }
            }
        }

        private void applyLinkStyles() {
            for (LinkStyleEntry entry : linkStyleEntries) {
                for (String idxStr : entry.indices()) {
                    if ("default".equalsIgnoreCase(idxStr)) {
                        for (int i = 0; i < edges.size(); i++) {
                            edges.set(i, applyEntryToEdge(edges.get(i), entry));
                        }
                    } else {
                        try {
                            int idx = Integer.parseInt(idxStr);
                            if (idx >= 0 && idx < edges.size()) {
                                edges.set(idx, applyEntryToEdge(edges.get(idx), entry));
                            }
                        } catch (NumberFormatException ignored) {}
                    }
                }
            }
        }

        private FlowchartEdge applyEntryToEdge(FlowchartEdge edge, LinkStyleEntry entry) {
            String existing = edge.getStyleOverride();
            List<String> parts = new ArrayList<>();
            if (existing != null) parts.add(existing);
            if (entry.styles() != null) parts.addAll(entry.styles());
            String merged = parts.isEmpty() ? null : String.join(",", parts);
            return new FlowchartEdge(
                edge.getFrom(),
                edge.getTo(),
                edge.getLabel(),
                edge.getStyle(),
                edge.isArrowFwd(),
                edge.isArrowRev(),
                edge.getForwardHead(),
                edge.getReverseHead(),
                edge.getEdgeId(),
                edge.getLength(),
                merged);
        }

        private int autoIdCounter = 0;

        private String autoSubgraphId() {
            return "subgraph_" + (autoIdCounter++);
        }

        static class SubgraphContext {

            String id;
            String label;
            @Nullable
            FlowchartDirection direction;
            SubgraphContext parent;
            final List<String> nodeIds = new ArrayList<>();
            final List<FlowchartEdge> edges = new ArrayList<>();
            final List<FlowchartSubgraph> children = new ArrayList<>();
        }

        @Desugar
        record LinkStyleEntry(List<String> indices, @Nullable List<String> styles, @Nullable String interpolate) {}
    }
}
