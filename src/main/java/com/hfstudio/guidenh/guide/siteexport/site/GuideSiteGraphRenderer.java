package com.hfstudio.guidenh.guide.siteexport.site;

import java.util.ArrayList;
import java.util.List;

import com.hfstudio.guidenh.guide.document.block.functiongraph.FunctionPlot;
import com.hfstudio.guidenh.guide.document.block.functiongraph.LytFunctionGraph;
import com.hfstudio.guidenh.guide.document.block.functiongraph.MarkedPoint;
import com.hfstudio.guidenh.guide.internal.csv.CsvTableParser;
import com.hfstudio.guidenh.guide.internal.markdown.FileTreeParser;
import com.hfstudio.guidenh.guide.internal.markdown.FileTreeParser.FileTreeEntry;
import com.hfstudio.guidenh.guide.internal.markdown.FileTreeParser.FileTreeIcon;
import com.hfstudio.guidenh.guide.internal.markdown.FileTreeParser.FileTreeIconKind;
import com.hfstudio.guidenh.guide.internal.markdown.FileTreeParser.FileTreeModel;
import com.hfstudio.guidenh.guide.internal.markdown.FileTreeParser.SlotKind;
import com.hfstudio.guidenh.guide.internal.mermaid.MermaidMindmapDocument;
import com.hfstudio.guidenh.guide.internal.mermaid.MermaidMindmapNode;
import com.hfstudio.guidenh.guide.internal.mermaid.MermaidMindmapNodeShape;
import org.jetbrains.annotations.Nullable;

/**
 * Generates static HTML and SVG markup for chart, function-graph, file-tree,
 * mermaid-mindmap, and CSV table elements used in the static site export.
 */
public final class GuideSiteGraphRenderer {

    // Chart default dimensions
    private static final int CHART_DEFAULT_W = 320;
    private static final int CHART_DEFAULT_H = 200;
    // Function graph defaults
    private static final int GRAPH_DEFAULT_W = 320;
    private static final int GRAPH_DEFAULT_H = 220;
    // Layout constants shared across chart types
    private static final int PADDING = 8;
    private static final int TITLE_H = 10;
    private static final int TITLE_GAP = 4;
    private static final int AXIS_PAD_LEFT = 28;
    private static final int AXIS_PAD_BOTTOM = 14;
    private static final int LEGEND_SWATCH = 8;
    private static final int LEGEND_GAP = 6;
    private static final int LEGEND_ROW_H = 11;
    // Function graph sample count
    private static final int N_SAMPLES = 256;

    private GuideSiteGraphRenderer() {}

    // ===== HTML escape helpers =====

    static String esc(String s) {
        if (s == null) {
            return "";
        }
        return s.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;");
    }

    // ===== Color conversion =====

    /** Convert ARGB int (0xAARRGGBB) to CSS hex or rgba(). */
    static String argbToRgba(int argb) {
        int a = (argb >> 24) & 0xFF;
        int r = (argb >> 16) & 0xFF;
        int g = (argb >> 8) & 0xFF;
        int b = argb & 0xFF;
        if (a == 0xFF) {
            return String.format("#%02X%02X%02X", r, g, b);
        }
        return String.format("rgba(%d,%d,%d,%.3f)", r, g, b, a / 255.0);
    }

    // ===== File Tree =====

    static String renderFileTree(String source) {
        FileTreeModel model = FileTreeParser.parse(source);
        StringBuilder html = new StringBuilder();
        html.append("<div class=\"guide-file-tree\">");
        for (FileTreeEntry entry : model.entries()) {
            html.append("<div class=\"guide-file-tree-row\">");
            StringBuilder prefix = new StringBuilder();
            for (SlotKind slot : entry.slots()) {
                switch (slot) {
                    case VERTICAL:
                        prefix.append("\u2502   ");
                        break;
                    case BRANCH:
                        prefix.append("\u251C\u2500\u2500 ");
                        break;
                    case LAST_BRANCH:
                        prefix.append("\u2514\u2500\u2500 ");
                        break;
                    default:
                        prefix.append("    ");
                        break;
                }
            }
            if (prefix.length() > 0) {
                html.append("<span class=\"guide-file-tree-prefix\">")
                    .append(esc(prefix.toString()))
                    .append("</span>");
            }
            FileTreeIcon icon = entry.icon();
            if (icon != null && icon.kind() == FileTreeIconKind.TEXT) {
                html.append("<span class=\"guide-file-tree-icon\">")
                    .append(esc(icon.value()))
                    .append("</span> ");
            }
            html.append("<span class=\"guide-file-tree-name\">")
                .append(esc(entry.payloadSource()))
                .append("</span>");
            html.append("</div>");
        }
        html.append("</div>");
        return html.toString();
    }

    // ===== Mermaid Mindmap (SVG) =====
    //
    // The HTML renderer here mirrors the in-game LytMermaidMindmapCanvas: rounded boxes
    // with a colored accent stripe on the left, 1px L-shaped connectors, top-down layout.
    // Text width is approximated (we don't have access to MC font metrics here).

    private static final int MM_NODE_PAD_X = 10;
    private static final int MM_NODE_PAD_Y = 6;
    private static final int MM_GAP_X = 32;
    private static final int MM_GAP_Y = 18;
    private static final int MM_CANVAS_PAD = 12;
    private static final int MM_LINE_HEIGHT = 14;
    private static final int MM_ROOT_LINE_HEIGHT = 16;
    private static final int MM_CHAR_WIDTH = 7; // Approx Pixeloid Sans @ 12px
    private static final int MM_ROOT_CHAR_WIDTH = 8; // Bold root text
    private static final int MM_ACCENT_STRIPE = 3;
    private static final int MM_MIN_NODE_WIDTH = 64;

    private static final int MM_BG_COLOR = 0xF00C1117;
    private static final int MM_BORDER_COLOR = 0x66434C57;
    private static final int MM_CONNECTOR_COLOR = 0xFF5D6C7C;
    private static final int MM_ROOT_BG = 0xFF1F2A38;
    private static final int MM_NODE_BG = 0xFF111922;
    private static final int MM_ROOT_TEXT = 0xFFF1F6FB;
    private static final int MM_NODE_TEXT = 0xFFD7DEE7;
    private static final int MM_BADGE_TEXT = 0xFFB8C2CF;
    private static final int MM_BADGE_BG = 0xFF262A33;
    private static final int MM_DEFAULT_ACCENT = 0xFF7AA2F7;

    private static final class MmLayoutNode {

        final MermaidMindmapNode source;
        final boolean isRoot;
        final String[] lines;
        final @org.jetbrains.annotations.Nullable String badge;
        int width;
        int height;
        int subtreeWidth;
        int subtreeHeight;
        int x;
        int y;
        final List<MmLayoutNode> children = new ArrayList<>();

        MmLayoutNode(MermaidMindmapNode source, boolean isRoot) {
            this.source = source;
            this.isRoot = isRoot;
            String text = source.getText() != null ? source.getText() : "";
            this.lines = text.isEmpty() ? new String[] { "" } : text.split("\n");
            this.badge = source.getIcon();
        }
    }

    static String renderMermaidTree(MermaidMindmapDocument doc) {
        if (doc == null || doc.getRoot() == null) {
            return "<div class=\"guide-mermaid-pan\" data-guide-pannable>"
                + "<svg class=\"guide-mermaid-canvas\" width=\"100\" height=\"40\"></svg></div>";
        }
        MmLayoutNode root = buildMmLayout(doc.getRoot(), true);
        measureMmTopDown(root);
        layoutMmTopDown(root, 0, 0);

        int contentW = root.subtreeWidth;
        int contentH = root.subtreeHeight;
        int totalW = contentW + MM_CANVAS_PAD * 2;
        int totalH = contentH + MM_CANVAS_PAD * 2;

        StringBuilder svg = new StringBuilder();
        // Outer pan/zoom wrapper consumed by app.js (installMermaidPanZoom).
        svg.append("<div class=\"guide-mermaid-pan\" data-guide-pannable>");
        svg.append("<svg class=\"guide-mermaid-canvas\" xmlns=\"http://www.w3.org/2000/svg\" width=\"")
            .append(totalW)
            .append("\" height=\"")
            .append(totalH)
            .append("\" viewBox=\"0 0 ")
            .append(totalW)
            .append(" ")
            .append(totalH)
            .append("\">");
        // Canvas background and border, mirroring the in-game LytMermaidMindmapCanvas frame.
        svg.append("<rect x=\"0.5\" y=\"0.5\" width=\"")
            .append(totalW - 1)
            .append("\" height=\"")
            .append(totalH - 1)
            .append("\" fill=\"")
            .append(argbToRgba(MM_BG_COLOR))
            .append("\" stroke=\"")
            .append(argbToRgba(MM_BORDER_COLOR))
            .append("\" stroke-width=\"1\"/>");
        // Translate the diagram into the padded interior.
        svg.append("<g transform=\"translate(")
            .append(MM_CANVAS_PAD)
            .append(",")
            .append(MM_CANVAS_PAD)
            .append(")\">");
        renderMmConnectors(svg, root);
        renderMmNodes(svg, root);
        svg.append("</g></svg></div>");
        return svg.toString();
    }

    private static MmLayoutNode buildMmLayout(MermaidMindmapNode source, boolean isRoot) {
        MmLayoutNode node = new MmLayoutNode(source, isRoot);
        // Node width: longest line * approx char width, plus padding and the accent stripe.
        int charW = isRoot ? MM_ROOT_CHAR_WIDTH : MM_CHAR_WIDTH;
        int textWidth = 0;
        for (String line : node.lines) {
            textWidth = Math.max(textWidth, line.length() * charW);
        }
        if (node.badge != null) {
            textWidth = Math.max(textWidth, node.badge.length() * MM_CHAR_WIDTH + 8);
        }
        int lineH = isRoot ? MM_ROOT_LINE_HEIGHT : MM_LINE_HEIGHT;
        int textHeight = node.lines.length * lineH;
        int badgeExtra = node.badge != null ? lineH + 4 : 0;
        node.width = Math.max(MM_MIN_NODE_WIDTH, textWidth + MM_NODE_PAD_X * 2 + MM_ACCENT_STRIPE);
        node.height = textHeight + badgeExtra + MM_NODE_PAD_Y * 2;
        for (MermaidMindmapNode child : source.getChildren()) {
            node.children.add(buildMmLayout(child, false));
        }
        return node;
    }

    private static void measureMmTopDown(MmLayoutNode node) {
        if (node.children.isEmpty()) {
            node.subtreeWidth = node.width;
            node.subtreeHeight = node.height;
            return;
        }
        int childrenW = 0;
        int childrenH = 0;
        for (MmLayoutNode child : node.children) {
            measureMmTopDown(child);
            childrenW += child.subtreeWidth;
            childrenH = Math.max(childrenH, child.subtreeHeight);
        }
        childrenW += MM_GAP_X * (node.children.size() - 1);
        node.subtreeWidth = Math.max(node.width, childrenW);
        node.subtreeHeight = node.height + MM_GAP_Y + childrenH;
    }

    private static void layoutMmTopDown(MmLayoutNode node, int x, int y) {
        node.x = x + (node.subtreeWidth - node.width) / 2;
        node.y = y;
        if (node.children.isEmpty()) {
            return;
        }
        int childrenW = 0;
        for (MmLayoutNode child : node.children) {
            childrenW += child.subtreeWidth;
        }
        childrenW += MM_GAP_X * (node.children.size() - 1);
        int cursorX = x + (node.subtreeWidth - childrenW) / 2;
        int childY = y + node.height + MM_GAP_Y;
        for (MmLayoutNode child : node.children) {
            layoutMmTopDown(child, cursorX, childY);
            cursorX += child.subtreeWidth + MM_GAP_X;
        }
    }

    private static void renderMmConnectors(StringBuilder svg, MmLayoutNode node) {
        String stroke = argbToRgba(MM_CONNECTOR_COLOR);
        int parentCx = node.x + node.width / 2;
        int parentBottom = node.y + node.height;
        for (MmLayoutNode child : node.children) {
            int childCx = child.x + child.width / 2;
            int childTop = child.y;
            int midY = (parentBottom + childTop) / 2;
            // L-shaped 1px connector: parent bottom -> midY -> over child column -> child top.
            svg.append("<path d=\"M")
                .append(parentCx)
                .append(" ")
                .append(parentBottom)
                .append(" V")
                .append(midY)
                .append(" H")
                .append(childCx)
                .append(" V")
                .append(childTop)
                .append("\" stroke=\"")
                .append(stroke)
                .append("\" stroke-width=\"1\" fill=\"none\" shape-rendering=\"crispEdges\"/>");
            renderMmConnectors(svg, child);
        }
    }

    private static void renderMmNodes(StringBuilder svg, MmLayoutNode node) {
        int accent = resolveMmAccent(node);
        int bg = node.isRoot ? MM_ROOT_BG : MM_NODE_BG;
        renderMmNodeShape(svg, node, accent, bg);
        // Accent stripe along the left edge (matches the in-game canvas).
        svg.append("<rect x=\"")
            .append(node.x)
            .append("\" y=\"")
            .append(node.y)
            .append("\" width=\"")
            .append(MM_ACCENT_STRIPE)
            .append("\" height=\"")
            .append(node.height)
            .append("\" fill=\"")
            .append(argbToRgba(accent))
            .append("\"/>");

        int textX = node.x + MM_ACCENT_STRIPE + MM_NODE_PAD_X;
        int contentTop = node.y + MM_NODE_PAD_Y;
        if (node.badge != null) {
            int badgeWidth = node.badge.length() * MM_CHAR_WIDTH + 8;
            int badgeHeight = MM_LINE_HEIGHT + 2;
            svg.append("<rect x=\"")
                .append(textX)
                .append("\" y=\"")
                .append(contentTop)
                .append("\" width=\"")
                .append(badgeWidth)
                .append("\" height=\"")
                .append(badgeHeight)
                .append("\" fill=\"")
                .append(argbToRgba(MM_BADGE_BG))
                .append("\" stroke=\"")
                .append(argbToRgba(MM_BORDER_COLOR))
                .append("\" stroke-width=\"1\"/>");
            svg.append("<text x=\"")
                .append(textX + 4)
                .append("\" y=\"")
                .append(contentTop + badgeHeight - 4)
                .append("\" font-size=\"10\" fill=\"")
                .append(argbToRgba(MM_BADGE_TEXT))
                .append("\" font-family=\"inherit\">")
                .append(esc(node.badge))
                .append("</text>");
            contentTop += badgeHeight + 4;
        }

        int lineHeight = node.isRoot ? MM_ROOT_LINE_HEIGHT : MM_LINE_HEIGHT;
        int fontSize = node.isRoot ? 13 : 12;
        String textColor = argbToRgba(node.isRoot ? MM_ROOT_TEXT : MM_NODE_TEXT);
        for (int i = 0; i < node.lines.length; i++) {
            int ly = contentTop + (i + 1) * lineHeight - 3;
            svg.append("<text x=\"")
                .append(textX)
                .append("\" y=\"")
                .append(ly)
                .append("\" font-size=\"")
                .append(fontSize)
                .append("\" fill=\"")
                .append(textColor)
                .append("\" font-family=\"inherit\"");
            if (node.isRoot) {
                svg.append(" font-weight=\"bold\"");
            }
            svg.append(">")
                .append(esc(node.lines[i]))
                .append("</text>");
        }

        for (MmLayoutNode child : node.children) {
            renderMmNodes(svg, child);
        }
    }

    private static void renderMmNodeShape(StringBuilder svg, MmLayoutNode node, int accent, int bg) {
        String fill = argbToRgba(bg);
        String stroke = argbToRgba(accent);
        MermaidMindmapNodeShape shape = node.source.getShape();
        if (shape == null) {
            shape = MermaidMindmapNodeShape.DEFAULT;
        }
        int strokeWidth = shape == MermaidMindmapNodeShape.BANG ? 2 : 1;
        int rx = switch (shape) {
            case ROUNDED, CIRCLE -> Math.min(node.height, node.width) / 2;
            case BANG -> 4;
            case CLOUD, HEXAGON -> 8;
            case SQUARE -> 0;
            default -> 3;
        };
        svg.append("<rect x=\"")
            .append(node.x)
            .append("\" y=\"")
            .append(node.y)
            .append("\" width=\"")
            .append(node.width)
            .append("\" height=\"")
            .append(node.height)
            .append("\" rx=\"")
            .append(rx)
            .append("\" ry=\"")
            .append(rx)
            .append("\" fill=\"")
            .append(fill)
            .append("\" stroke=\"")
            .append(stroke)
            .append("\" stroke-width=\"")
            .append(strokeWidth)
            .append("\"/>");
    }

    private static int resolveMmAccent(MmLayoutNode node) {
        int accent = MM_DEFAULT_ACCENT;
        for (String className : node.source.getClasses()) {
            String lower = className.toLowerCase(java.util.Locale.ROOT);
            if (lower.contains("danger") || lower.contains("error")
                || lower.contains("urgent")
                || lower.contains("red")) {
                accent = 0xFFF7768E;
                break;
            }
            if (lower.contains("success") || lower.contains("green") || lower.contains("done")) {
                accent = 0xFF9ECE6A;
                break;
            }
            if (lower.contains("warn") || lower.contains("yellow") || lower.contains("amber")) {
                accent = 0xFFE0AF68;
                break;
            }
            if (lower.contains("muted") || lower.contains("gray") || lower.contains("grey")) {
                accent = 0xFF8B949E;
            }
        }
        return switch (node.source.getShape()) {
            case CIRCLE -> 0xFF7DCFFF;
            case HEXAGON -> 0xFFE0AF68;
            case CLOUD -> 0xFF73DACA;
            case BANG -> 0xFFF7768E;
            default -> accent;
        };
    }

    // ===== CSV Table =====

    static String renderCsvTable(String csvSource, boolean hasHeader) {
        List<List<String>> rows = CsvTableParser.parse(csvSource);
        return renderCsvTable(rows, hasHeader);
    }

    static String renderCsvTable(List<List<String>> rows, boolean hasHeader) {
        if (rows.isEmpty()) {
            return "<table class=\"guide-csv-table\"></table>";
        }
        StringBuilder html = new StringBuilder();
        html.append("<table class=\"guide-csv-table\">");
        int start = 0;
        if (hasHeader) {
            html.append("<thead><tr>");
            for (String cell : rows.get(0)) {
                html.append("<th>")
                    .append(esc(cell))
                    .append("</th>");
            }
            html.append("</tr></thead>");
            start = 1;
        }
        if (start < rows.size()) {
            html.append("<tbody>");
            for (int i = start; i < rows.size(); i++) {
                html.append("<tr>");
                for (String cell : rows.get(i)) {
                    html.append("<td>")
                        .append(esc(cell))
                        .append("</td>");
                }
                html.append("</tr>");
            }
            html.append("</tbody>");
        }
        html.append("</table>");
        return html.toString();
    }

    // ===== Chart data holder classes =====

    /** Chart series data (name, ARGB color, parallel xs/ys arrays). */
    static final class SeriesData {

        final String name;
        final int color;
        final double[] xs;
        final double[] ys;

        SeriesData(String name, int color, double[] xs, double[] ys) {
            this.name = name != null ? name : "";
            this.color = color;
            this.xs = xs != null ? xs : new double[0];
            this.ys = ys != null ? ys : new double[0];
        }
    }

    /** Pie-chart slice (label, value, ARGB color). */
    static final class SliceData {

        final String label;
        final double value;
        final int color;

        SliceData(String label, double value, int color) {
            this.label = label != null ? label : "";
            this.value = value;
            this.color = color;
        }
    }

    // ===== Column Chart (vertical bars, categorical X) =====

    static String renderColumnChart(int w, int h, int bgColor, int borderColor, String title, String[] categories,
        List<SeriesData> series, boolean showLegend) {
        if (w <= 0) {
            w = CHART_DEFAULT_W;
        }
        if (h <= 0) {
            h = CHART_DEFAULT_H;
        }
        if (series == null) {
            series = new ArrayList<>();
        }
        if (categories == null) {
            categories = new String[0];
        }

        double yMin = 0;
        double yMax = 0;
        for (SeriesData s : series) {
            for (double v : s.ys) {
                if (v < yMin) yMin = v;
                if (v > yMax) yMax = v;
            }
        }
        if (yMin == yMax) {
            yMax = yMin + 1;
        }
        double yStep = niceStep((yMax - yMin) / 5.0);
        yMin = Math.floor(yMin / yStep) * yStep;
        yMax = Math.ceil(yMax / yStep) * yStep;
        if (yMin == yMax) {
            yMax = yMin + yStep;
        }

        int titleBottom = computeTitleBottom(title);
        int legendH = computeLegendH(series, showLegend, w);

        int left = PADDING + AXIS_PAD_LEFT;
        int right = w - PADDING;
        int top = titleBottom;
        int bottom = h - PADDING - AXIS_PAD_BOTTOM - legendH;
        int plotW = Math.max(1, right - left);
        int plotH = Math.max(1, bottom - top);

        int nCat = Math.max(1, categories.length);
        int nSer = Math.max(1, series.size());
        double clusterW = (double) plotW / nCat;
        double barW = clusterW * 0.7 / nSer;
        double gap = (clusterW - barW * nSer) / 2.0;

        StringBuilder svg = openSvg(w, h, "guide-chart", bgColor, borderColor);
        appendTitle(svg, title, w);
        appendYGridAndLabels(svg, left, right, top, bottom, plotH, yMin, yMax);
        appendCategoryXLabels(svg, categories, left, clusterW, bottom);

        for (int si = 0; si < series.size(); si++) {
            SeriesData s = series.get(si);
            String fill = argbToRgba(s.color);
            int len = Math.min(s.xs.length, s.ys.length);
            for (int di = 0; di < len; di++) {
                int ci = (int) s.xs[di];
                if (ci < 0 || ci >= nCat) {
                    continue;
                }
                double value = s.ys[di];
                double bx = left + ci * clusterW + gap + si * barW;
                double by;
                double bh;
                double zeroY = bottom - Math.max(0, -yMin) / (yMax - yMin) * plotH;
                if (value >= 0) {
                    bh = value / (yMax - yMin) * plotH;
                    by = zeroY - bh;
                } else {
                    by = zeroY;
                    bh = -value / (yMax - yMin) * plotH;
                }
                if (bh < 0.5) {
                    bh = 0.5;
                }
                svg.append("<rect class=\"guide-chart-shape\" x=\"")
                    .append(fmtD(bx))
                    .append("\" y=\"")
                    .append(fmtD(by))
                    .append("\" width=\"")
                    .append(fmtD(barW))
                    .append("\" height=\"")
                    .append(fmtD(bh))
                    .append("\" fill=\"")
                    .append(fill)
                    .append("\"><title>")
                    .append(esc(buildChartTip(categories[ci], s.name, value)))
                    .append("</title></rect>");
            }
        }

        appendYAxis(svg, left, top, bottom);
        appendXAxis(svg, left, right, bottom);
        if (showLegend) {
            renderLegend(svg, series, left, bottom + AXIS_PAD_BOTTOM + LEGEND_GAP, w - 2 * PADDING);
        }
        return svg.append("</svg>")
            .toString();
    }

    // ===== Bar Chart (horizontal bars, categorical Y) =====

    static String renderBarChart(int w, int h, int bgColor, int borderColor, String title, String[] categories,
        List<SeriesData> series, boolean showLegend) {
        if (w <= 0) {
            w = CHART_DEFAULT_W;
        }
        if (h <= 0) {
            h = CHART_DEFAULT_H;
        }
        if (series == null) {
            series = new ArrayList<>();
        }
        if (categories == null) {
            categories = new String[0];
        }

        double xMin = 0;
        double xMax = 0;
        for (SeriesData s : series) {
            for (double v : s.ys) {
                if (v < xMin) xMin = v;
                if (v > xMax) xMax = v;
            }
        }
        if (xMin == xMax) {
            xMax = xMin + 1;
        }
        double xStep = niceStep((xMax - xMin) / 5.0);
        xMin = Math.floor(xMin / xStep) * xStep;
        xMax = Math.ceil(xMax / xStep) * xStep;
        if (xMin == xMax) {
            xMax = xMin + xStep;
        }

        int titleBottom = computeTitleBottom(title);
        int legendH = computeLegendH(series, showLegend, w);

        int catLabelW = 36;
        int left = PADDING + catLabelW;
        int right = w - PADDING;
        int top = titleBottom;
        int bottom = h - PADDING - AXIS_PAD_BOTTOM - legendH;
        int plotW = Math.max(1, right - left);
        int plotH = Math.max(1, bottom - top);

        int nCat = Math.max(1, categories.length);
        int nSer = Math.max(1, series.size());
        double rowH = (double) plotH / nCat;
        double barH = rowH * 0.7 / nSer;
        double gap = (rowH - barH * nSer) / 2.0;

        StringBuilder svg = openSvg(w, h, "guide-chart", bgColor, borderColor);
        appendTitle(svg, title, w);

        // X grid + labels at bottom
        double xStepG = niceStep((xMax - xMin) / 5.0);
        int nGridX = (int) Math.round((xMax - xMin) / xStepG);
        nGridX = Math.max(1, Math.min(nGridX, 10));
        for (int gi = 0; gi <= nGridX; gi++) {
            double xv = xMin + gi * (xMax - xMin) / nGridX;
            int gx = left + (int) Math.round((xv - xMin) / (xMax - xMin) * plotW);
            svg.append("<line x1=\"")
                .append(gx)
                .append("\" y1=\"")
                .append(top)
                .append("\" x2=\"")
                .append(gx)
                .append("\" y2=\"")
                .append(bottom)
                .append("\" stroke=\"#3A4047\" stroke-width=\"1\"/>");
            svg.append("<text x=\"")
                .append(gx)
                .append("\" y=\"")
                .append(bottom + 10)
                .append("\" text-anchor=\"middle\" font-size=\"8\" fill=\"#B8C2CF\" font-family=\"inherit\">")
                .append(esc(formatNum(xv)))
                .append("</text>");
        }

        // Category labels on left (Y axis)
        for (int ci = 0; ci < categories.length; ci++) {
            int cy = top + (int) Math.round((ci + 0.5) * rowH);
            svg.append("<text x=\"")
                .append(left - 4)
                .append("\" y=\"")
                .append(cy + 4)
                .append("\" text-anchor=\"end\" font-size=\"8\" fill=\"#B8C2CF\" font-family=\"inherit\">")
                .append(esc(categories[ci]))
                .append("</text>");
        }

        double xBase = left + Math.max(0, -xMin) / (xMax - xMin) * plotW;
        for (int si = 0; si < series.size(); si++) {
            SeriesData s = series.get(si);
            String fill = argbToRgba(s.color);
            int len = Math.min(s.xs.length, s.ys.length);
            for (int di = 0; di < len; di++) {
                int ci = (int) s.xs[di];
                if (ci < 0 || ci >= nCat) {
                    continue;
                }
                double value = s.ys[di];
                double by = top + ci * rowH + gap + si * barH;
                double bw;
                double bx;
                if (value >= 0) {
                    bx = xBase;
                    bw = value / (xMax - xMin) * plotW;
                } else {
                    bw = -value / (xMax - xMin) * plotW;
                    bx = xBase - bw;
                }
                if (bw < 0.5) {
                    bw = 0.5;
                }
                svg.append("<rect class=\"guide-chart-shape\" x=\"")
                    .append(fmtD(bx))
                    .append("\" y=\"")
                    .append(fmtD(by))
                    .append("\" width=\"")
                    .append(fmtD(bw))
                    .append("\" height=\"")
                    .append(fmtD(barH))
                    .append("\" fill=\"")
                    .append(fill)
                    .append("\"><title>")
                    .append(esc(buildChartTip(ci < categories.length ? categories[ci] : "", s.name, value)))
                    .append("</title></rect>");
            }
        }

        // Axis lines
        svg.append("<line x1=\"")
            .append(fmtD(xBase))
            .append("\" y1=\"")
            .append(top)
            .append("\" x2=\"")
            .append(fmtD(xBase))
            .append("\" y2=\"")
            .append(bottom)
            .append("\" stroke=\"#B8C2CF\" stroke-width=\"1\"/>");
        appendXAxis(svg, left, right, bottom);

        if (showLegend) {
            renderLegend(svg, series, left, bottom + AXIS_PAD_BOTTOM + LEGEND_GAP, w - 2 * PADDING);
        }
        return svg.append("</svg>")
            .toString();
    }

    // ===== Line Chart =====

    static String renderLineChart(int w, int h, int bgColor, int borderColor, String title, String[] categories,
        List<SeriesData> series, boolean numericX, boolean showPoints, boolean showLegend) {
        if (w <= 0) {
            w = CHART_DEFAULT_W;
        }
        if (h <= 0) {
            h = CHART_DEFAULT_H;
        }
        if (series == null) {
            series = new ArrayList<>();
        }
        if (categories == null) {
            categories = new String[0];
        }

        double xMin = 0;
        double xMax = 0;
        double yMin = Double.MAX_VALUE;
        double yMax = -Double.MAX_VALUE;
        int maxIdx = categories.length;
        for (SeriesData s : series) {
            for (double v : s.ys) {
                if (v < yMin) yMin = v;
                if (v > yMax) yMax = v;
            }
            if (numericX) {
                for (double v : s.xs) {
                    if (v < xMin) xMin = v;
                    if (v > xMax) xMax = v;
                }
            } else {
                for (double v : s.xs) {
                    int idx = (int) v;
                    if (idx >= maxIdx) maxIdx = idx + 1;
                }
            }
        }
        if (!numericX) {
            xMin = 0;
            xMax = Math.max(1, maxIdx - 1);
        }
        if (xMin == xMax) {
            xMax = xMin + 1;
        }
        if (!Double.isFinite(yMin)) {
            yMin = 0;
            yMax = 1;
        }
        if (yMin == yMax) {
            yMin -= 0.5;
            yMax += 0.5;
        }
        double yRange = yMax - yMin;
        yMin -= yRange * 0.05;
        yMax += yRange * 0.05;

        int titleBottom = computeTitleBottom(title);
        int legendH = computeLegendH(series, showLegend, w);

        int left = PADDING + AXIS_PAD_LEFT;
        int right = w - PADDING;
        int top = titleBottom;
        int bottom = h - PADDING - AXIS_PAD_BOTTOM - legendH;
        int plotW = Math.max(1, right - left);
        int plotH = Math.max(1, bottom - top);

        StringBuilder svg = openSvg(w, h, "guide-chart", bgColor, borderColor);
        appendTitle(svg, title, w);

        // Y grid + labels
        double yStep = niceStep((yMax - yMin) / 5.0);
        for (double yv = Math.floor(yMin / yStep) * yStep; yv <= yMax + yStep * 0.01; yv += yStep) {
            int gy = bottom - (int) Math.round((yv - yMin) / (yMax - yMin) * plotH);
            if (gy < top - 2 || gy > bottom + 2) {
                continue;
            }
            svg.append("<line x1=\"")
                .append(left)
                .append("\" y1=\"")
                .append(gy)
                .append("\" x2=\"")
                .append(right)
                .append("\" y2=\"")
                .append(gy)
                .append("\" stroke=\"#3A4047\" stroke-width=\"1\"/>");
            svg.append("<text x=\"")
                .append(left - 3)
                .append("\" y=\"")
                .append(gy + 4)
                .append("\" text-anchor=\"end\" font-size=\"8\" fill=\"#B8C2CF\" font-family=\"inherit\">")
                .append(esc(formatNum(yv)))
                .append("</text>");
        }

        // X axis labels
        if (!numericX) {
            for (int ci = 0; ci < maxIdx && ci < categories.length; ci++) {
                int cx = left + (int) Math.round((double) ci / Math.max(1, maxIdx - 1) * plotW);
                svg.append("<text x=\"")
                    .append(cx)
                    .append("\" y=\"")
                    .append(bottom + 10)
                    .append("\" text-anchor=\"middle\" font-size=\"8\" fill=\"#B8C2CF\" font-family=\"inherit\">")
                    .append(esc(categories[ci]))
                    .append("</text>");
            }
        } else {
            double xStep = niceStep((xMax - xMin) / 5.0);
            for (double xv = Math.floor(xMin / xStep) * xStep; xv <= xMax + xStep * 0.01; xv += xStep) {
                int gx = left + (int) Math.round((xv - xMin) / (xMax - xMin) * plotW);
                if (gx < left - 2 || gx > right + 2) {
                    continue;
                }
                svg.append("<line x1=\"")
                    .append(gx)
                    .append("\" y1=\"")
                    .append(top)
                    .append("\" x2=\"")
                    .append(gx)
                    .append("\" y2=\"")
                    .append(bottom)
                    .append("\" stroke=\"#3A4047\" stroke-width=\"1\"/>");
                svg.append("<text x=\"")
                    .append(gx)
                    .append("\" y=\"")
                    .append(bottom + 10)
                    .append("\" text-anchor=\"middle\" font-size=\"8\" fill=\"#B8C2CF\" font-family=\"inherit\">")
                    .append(esc(formatNum(xv)))
                    .append("</text>");
            }
        }

        // Lines + optional points
        for (SeriesData s : series) {
            String stroke = argbToRgba(s.color);
            int len = Math.min(s.xs.length, s.ys.length);
            if (len == 0) {
                continue;
            }
            StringBuilder pts = new StringBuilder();
            for (int i = 0; i < len; i++) {
                int px = left + (int) Math.round((s.xs[i] - xMin) / (xMax - xMin) * plotW);
                int py = bottom - (int) Math.round((s.ys[i] - yMin) / (yMax - yMin) * plotH);
                if (i > 0) {
                    pts.append(" ");
                }
                pts.append(px)
                    .append(",")
                    .append(py);
            }
            svg.append("<polyline class=\"guide-chart-shape\" points=\"")
                .append(pts)
                .append("\" stroke=\"")
                .append(stroke)
                .append("\" stroke-width=\"1.5\" fill=\"none\"><title>")
                .append(esc(s.name))
                .append("</title></polyline>");
            if (showPoints) {
                for (int i = 0; i < len; i++) {
                    int px = left + (int) Math.round((s.xs[i] - xMin) / (xMax - xMin) * plotW);
                    int py = bottom - (int) Math.round((s.ys[i] - yMin) / (yMax - yMin) * plotH);
                    String pointTip = buildChartTip(
                        numericX ? formatNum(s.xs[i])
                            : ((int) s.xs[i] >= 0 && (int) s.xs[i] < categories.length ? categories[(int) s.xs[i]]
                                : ""),
                        s.name,
                        s.ys[i]);
                    svg.append("<circle class=\"guide-chart-shape\" cx=\"")
                        .append(px)
                        .append("\" cy=\"")
                        .append(py)
                        .append("\" r=\"2\" fill=\"")
                        .append(stroke)
                        .append("\"><title>")
                        .append(esc(pointTip))
                        .append("</title></circle>");
                }
            }
        }

        appendYAxis(svg, left, top, bottom);
        appendXAxis(svg, left, right, bottom);
        if (showLegend) {
            renderLegend(svg, series, left, bottom + AXIS_PAD_BOTTOM + LEGEND_GAP, w - 2 * PADDING);
        }
        return svg.append("</svg>")
            .toString();
    }

    // ===== Pie Chart =====

    static String renderPieChart(int w, int h, int bgColor, int borderColor, String title, List<SliceData> slices,
        boolean showLegend) {
        if (w <= 0) {
            w = CHART_DEFAULT_W;
        }
        if (h <= 0) {
            h = CHART_DEFAULT_H;
        }
        if (slices == null) {
            slices = new ArrayList<>();
        }

        double total = 0;
        for (SliceData s : slices) {
            total += Math.max(0, s.value);
        }
        if (total <= 0) {
            total = 1;
        }

        int titleBottom = computeTitleBottom(title);
        int legendH = 0;
        if (showLegend && !slices.isEmpty()) {
            int cols = Math.max(1, (w - 2 * PADDING) / 80);
            legendH = (int) Math.ceil((double) slices.size() / cols) * (LEGEND_ROW_H + 2) + LEGEND_GAP;
        }

        int left = PADDING;
        int right = w - PADDING;
        int top = titleBottom;
        int bottom = h - PADDING - legendH;
        int plotW = right - left;
        int plotH = bottom - top;

        int cx = left + plotW / 2;
        int cy = top + plotH / 2;
        int r = Math.min(plotW, plotH) / 2 - 4;
        if (r < 4) {
            r = 4;
        }

        StringBuilder svg = openSvg(w, h, "guide-chart", bgColor, borderColor);
        appendTitle(svg, title, w);

        // Draw slices (startAngle = -90 deg = top, clockwise)
        double startAngle = -Math.PI / 2;
        for (SliceData s : slices) {
            double sweep = (s.value / total) * 2 * Math.PI;
            double endAngle = startAngle + sweep;
            double x1 = cx + r * Math.cos(startAngle);
            double y1 = cy + r * Math.sin(startAngle);
            double x2 = cx + r * Math.cos(endAngle);
            double y2 = cy + r * Math.sin(endAngle);
            int largeArc = sweep > Math.PI ? 1 : 0;
            double pct = total > 0 ? (s.value / total) * 100.0 : 0;
            svg.append("<path class=\"guide-chart-shape\" d=\"M ")
                .append(cx)
                .append(" ")
                .append(cy)
                .append(" L ")
                .append(fmtD(x1))
                .append(" ")
                .append(fmtD(y1))
                .append(" A ")
                .append(r)
                .append(" ")
                .append(r)
                .append(" 0 ")
                .append(largeArc)
                .append(" 1 ")
                .append(fmtD(x2))
                .append(" ")
                .append(fmtD(y2))
                .append(" Z\" fill=\"")
                .append(argbToRgba(s.color))
                .append("\" stroke=\"")
                .append(argbToRgba(bgColor))
                .append("\" stroke-width=\"0.5\"><title>")
                .append(esc(s.label + ": " + formatNum(s.value) + " (" + String.format("%.1f", pct) + "%)"))
                .append("</title></path>");
            startAngle = endAngle;
        }

        if (showLegend && !slices.isEmpty()) {
            List<SeriesData> legendItems = new ArrayList<>();
            for (SliceData s : slices) {
                legendItems.add(new SeriesData(s.label, s.color, new double[0], new double[0]));
            }
            renderLegend(svg, legendItems, PADDING, bottom + LEGEND_GAP, w - 2 * PADDING);
        }
        return svg.append("</svg>")
            .toString();
    }

    // ===== Scatter Chart =====

    static String renderScatterChart(int w, int h, int bgColor, int borderColor, String title, List<SeriesData> series,
        boolean showLegend) {
        if (w <= 0) {
            w = CHART_DEFAULT_W;
        }
        if (h <= 0) {
            h = CHART_DEFAULT_H;
        }
        if (series == null) {
            series = new ArrayList<>();
        }

        double xMin = Double.MAX_VALUE;
        double xMax = -Double.MAX_VALUE;
        double yMin = Double.MAX_VALUE;
        double yMax = -Double.MAX_VALUE;
        for (SeriesData s : series) {
            for (double v : s.xs) {
                if (v < xMin) xMin = v;
                if (v > xMax) xMax = v;
            }
            for (double v : s.ys) {
                if (v < yMin) yMin = v;
                if (v > yMax) yMax = v;
            }
        }
        if (!Double.isFinite(xMin)) {
            xMin = 0;
            xMax = 1;
        }
        if (!Double.isFinite(yMin)) {
            yMin = 0;
            yMax = 1;
        }
        if (xMin == xMax) {
            xMin -= 0.5;
            xMax += 0.5;
        }
        if (yMin == yMax) {
            yMin -= 0.5;
            yMax += 0.5;
        }
        double xPad = (xMax - xMin) * 0.05;
        double yPad = (yMax - yMin) * 0.05;
        xMin -= xPad;
        xMax += xPad;
        yMin -= yPad;
        yMax += yPad;

        int titleBottom = computeTitleBottom(title);
        int legendH = computeLegendH(series, showLegend, w);

        int left = PADDING + AXIS_PAD_LEFT;
        int right = w - PADDING;
        int top = titleBottom;
        int bottom = h - PADDING - AXIS_PAD_BOTTOM - legendH;
        int plotW = Math.max(1, right - left);
        int plotH = Math.max(1, bottom - top);

        StringBuilder svg = openSvg(w, h, "guide-chart", bgColor, borderColor);
        appendTitle(svg, title, w);

        // X grid + labels
        double xStep = niceStep((xMax - xMin) / 5.0);
        for (double xv = Math.floor(xMin / xStep) * xStep; xv <= xMax + xStep * 0.01; xv += xStep) {
            int gx = left + (int) Math.round((xv - xMin) / (xMax - xMin) * plotW);
            if (gx < left - 2 || gx > right + 2) {
                continue;
            }
            svg.append("<line x1=\"")
                .append(gx)
                .append("\" y1=\"")
                .append(top)
                .append("\" x2=\"")
                .append(gx)
                .append("\" y2=\"")
                .append(bottom)
                .append("\" stroke=\"#3A4047\" stroke-width=\"1\"/>");
            svg.append("<text x=\"")
                .append(gx)
                .append("\" y=\"")
                .append(bottom + 10)
                .append("\" text-anchor=\"middle\" font-size=\"8\" fill=\"#B8C2CF\" font-family=\"inherit\">")
                .append(esc(formatNum(xv)))
                .append("</text>");
        }
        appendYGridAndLabels(svg, left, right, top, bottom, plotH, yMin, yMax);

        // Scatter points
        for (SeriesData s : series) {
            String fill = argbToRgba(s.color);
            int len = Math.min(s.xs.length, s.ys.length);
            for (int i = 0; i < len; i++) {
                int px = left + (int) Math.round((s.xs[i] - xMin) / (xMax - xMin) * plotW);
                int py = bottom - (int) Math.round((s.ys[i] - yMin) / (yMax - yMin) * plotH);
                svg.append("<circle class=\"guide-chart-shape\" cx=\"")
                    .append(px)
                    .append("\" cy=\"")
                    .append(py)
                    .append("\" r=\"3\" fill=\"")
                    .append(fill)
                    .append("\"><title>")
                    .append(
                        esc(
                            (s.name.isEmpty() ? "" : s.name + ": ") + "("
                                + formatNum(s.xs[i])
                                + ", "
                                + formatNum(s.ys[i])
                                + ")"))
                    .append("</title></circle>");
            }
        }

        appendYAxis(svg, left, top, bottom);
        appendXAxis(svg, left, right, bottom);
        if (showLegend) {
            renderLegend(svg, series, left, bottom + AXIS_PAD_BOTTOM + LEGEND_GAP, w - 2 * PADDING);
        }
        return svg.append("</svg>")
            .toString();
    }

    // ===== Function Graph =====

    static String renderFunctionGraph(LytFunctionGraph graph) {
        int w = graph.getExplicitWidth() > 0 ? graph.getExplicitWidth() : GRAPH_DEFAULT_W;
        int h = graph.getExplicitHeight() > 0 ? graph.getExplicitHeight() : GRAPH_DEFAULT_H;
        String title = graph.getTitle();
        int bgColor = graph.getBackgroundColor();
        int borderColor = graph.getBorderColor();
        int axisColor = graph.getAxisColor();
        int gridColor = graph.getGridColor();
        boolean showGrid = graph.isShowGrid();
        boolean showAxes = graph.isShowAxes();
        List<FunctionPlot> plots = graph.getPlots();

        double xMin = Double.isNaN(graph.getExplicitXMin()) ? -10 : graph.getExplicitXMin();
        double xMax = Double.isNaN(graph.getExplicitXMax()) ? 10 : graph.getExplicitXMax();
        double yMin = Double.isNaN(graph.getExplicitYMin()) ? Double.NaN : graph.getExplicitYMin();
        double yMax = Double.isNaN(graph.getExplicitYMax()) ? Double.NaN : graph.getExplicitYMax();

        if (Double.isNaN(yMin) || Double.isNaN(yMax)) {
            double autoYMin = Double.MAX_VALUE;
            double autoYMax = -Double.MAX_VALUE;
            for (FunctionPlot plot : plots) {
                for (int i = 0; i <= N_SAMPLES; i++) {
                    double x = xMin + (xMax - xMin) * i / N_SAMPLES;
                    double y = plot.evaluate(x);
                    if (Double.isFinite(y)) {
                        if (y < autoYMin) autoYMin = y;
                        if (y > autoYMax) autoYMax = y;
                    }
                }
            }
            if (!Double.isFinite(autoYMin)) {
                autoYMin = xMin;
                autoYMax = xMax;
            }
            if (autoYMin == autoYMax) {
                autoYMin -= 1;
                autoYMax += 1;
            }
            double margin = (autoYMax - autoYMin) * 0.1;
            if (Double.isNaN(yMin)) yMin = autoYMin - margin;
            if (Double.isNaN(yMax)) yMax = autoYMax + margin;
        }

        return renderFunctionGraphSvg(
            plots,
            graph.getPoints(),
            w,
            h,
            title,
            bgColor,
            borderColor,
            axisColor,
            gridColor,
            showGrid,
            showAxes,
            xMin,
            xMax,
            yMin,
            yMax);
    }

    static String renderFunctionGraphSvg(List<FunctionPlot> plots, List<MarkedPoint> points, int w, int h, String title,
        int bgColor, int borderColor, int axisColor, int gridColor, boolean showGrid, boolean showAxes, double xMin,
        double xMax, double yMin, double yMax) {

        int titleBottom = computeTitleBottom(title);
        int leftPad = showAxes ? AXIS_PAD_LEFT : PADDING;
        int bottomPad = showAxes ? AXIS_PAD_BOTTOM : PADDING;

        int left = PADDING + leftPad;
        int right = w - PADDING;
        int top = titleBottom;
        int bottom = h - PADDING - bottomPad;
        int plotW = Math.max(1, right - left);
        int plotH = Math.max(1, bottom - top);

        StringBuilder svg = openSvg(w, h, "guide-function-graph", bgColor, borderColor);
        // Expose plot domain + plot rect so client-side JS (installChartHoverTooltips)
        // can map cursor pixels → data x/y for live (x, y) tooltips.
        // We can't add attrs to the already-emitted <svg> tag easily without rewriting
        // openSvg, so we embed them as <metadata> entries the JS reads.
        svg.append("<metadata data-plot-domain=\"true\" data-x-min=\"")
            .append(xMin)
            .append("\" data-x-max=\"")
            .append(xMax)
            .append("\" data-y-min=\"")
            .append(yMin)
            .append("\" data-y-max=\"")
            .append(yMax)
            .append("\" data-plot-left=\"")
            .append(left)
            .append("\" data-plot-right=\"")
            .append(right)
            .append("\" data-plot-top=\"")
            .append(top)
            .append("\" data-plot-bottom=\"")
            .append(bottom)
            .append("\"></metadata>");

        // Clip path for curve rendering
        svg.append("<defs><clipPath id=\"gc\"><rect x=\"")
            .append(left)
            .append("\" y=\"")
            .append(top)
            .append("\" width=\"")
            .append(plotW)
            .append("\" height=\"")
            .append(plotH)
            .append("\"/></clipPath></defs>");

        appendTitle(svg, title, w);

        String gridCss = argbToRgba(gridColor);
        String axisCss = argbToRgba(axisColor);

        if (showGrid) {
            double xStep = niceStep((xMax - xMin) / 6.0);
            double yStep = niceStep((yMax - yMin) / 6.0);
            for (double xv = Math.floor(xMin / xStep) * xStep; xv <= xMax + xStep * 0.01; xv += xStep) {
                int gx = left + (int) Math.round((xv - xMin) / (xMax - xMin) * plotW);
                if (gx < left || gx > right) {
                    continue;
                }
                svg.append("<line x1=\"")
                    .append(gx)
                    .append("\" y1=\"")
                    .append(top)
                    .append("\" x2=\"")
                    .append(gx)
                    .append("\" y2=\"")
                    .append(bottom)
                    .append("\" stroke=\"")
                    .append(gridCss)
                    .append("\" stroke-width=\"1\"/>");
            }
            for (double yv = Math.floor(yMin / yStep) * yStep; yv <= yMax + yStep * 0.01; yv += yStep) {
                int gy = bottom - (int) Math.round((yv - yMin) / (yMax - yMin) * plotH);
                if (gy < top || gy > bottom) {
                    continue;
                }
                svg.append("<line x1=\"")
                    .append(left)
                    .append("\" y1=\"")
                    .append(gy)
                    .append("\" x2=\"")
                    .append(right)
                    .append("\" y2=\"")
                    .append(gy)
                    .append("\" stroke=\"")
                    .append(gridCss)
                    .append("\" stroke-width=\"1\"/>");
            }
        }

        if (showAxes) {
            double yStep = niceStep((yMax - yMin) / 5.0);
            for (double yv = Math.floor(yMin / yStep) * yStep; yv <= yMax + yStep * 0.01; yv += yStep) {
                int gy = bottom - (int) Math.round((yv - yMin) / (yMax - yMin) * plotH);
                if (gy < top || gy > bottom) {
                    continue;
                }
                svg.append("<text x=\"")
                    .append(left - 3)
                    .append("\" y=\"")
                    .append(gy + 4)
                    .append("\" text-anchor=\"end\" font-size=\"8\" fill=\"")
                    .append(axisCss)
                    .append("\" font-family=\"inherit\">")
                    .append(esc(formatNum(yv)))
                    .append("</text>");
            }
            double xStep = niceStep((xMax - xMin) / 5.0);
            for (double xv = Math.floor(xMin / xStep) * xStep; xv <= xMax + xStep * 0.01; xv += xStep) {
                int gx = left + (int) Math.round((xv - xMin) / (xMax - xMin) * plotW);
                if (gx < left || gx > right) {
                    continue;
                }
                svg.append("<text x=\"")
                    .append(gx)
                    .append("\" y=\"")
                    .append(bottom + 10)
                    .append("\" text-anchor=\"middle\" font-size=\"8\" fill=\"")
                    .append(axisCss)
                    .append("\" font-family=\"inherit\">")
                    .append(esc(formatNum(xv)))
                    .append("</text>");
            }
            // Zero-crossing axis lines
            if (yMin <= 0 && yMax >= 0) {
                int ay = bottom - (int) Math.round((0 - yMin) / (yMax - yMin) * plotH);
                svg.append("<line x1=\"")
                    .append(left)
                    .append("\" y1=\"")
                    .append(ay)
                    .append("\" x2=\"")
                    .append(right)
                    .append("\" y2=\"")
                    .append(ay)
                    .append("\" stroke=\"")
                    .append(axisCss)
                    .append("\" stroke-width=\"1\"/>");
            }
            if (xMin <= 0 && xMax >= 0) {
                int ax = left + (int) Math.round((0 - xMin) / (xMax - xMin) * plotW);
                svg.append("<line x1=\"")
                    .append(ax)
                    .append("\" y1=\"")
                    .append(top)
                    .append("\" x2=\"")
                    .append(ax)
                    .append("\" y2=\"")
                    .append(bottom)
                    .append("\" stroke=\"")
                    .append(axisCss)
                    .append("\" stroke-width=\"1\"/>");
            }
        }

        // Function curves (clipped)
        svg.append("<g clip-path=\"url(#gc)\">");
        for (FunctionPlot plot : plots) {
            String stroke = argbToRgba(plot.getColor());
            String tip = plot.getLabel() != null && !plot.getLabel()
                .isEmpty() ? plot.getLabel() : plot.getExpressionText();
            StringBuilder pts = new StringBuilder();
            boolean inSeg = false;
            for (int i = 0; i <= N_SAMPLES; i++) {
                double x = xMin + (xMax - xMin) * i / N_SAMPLES;
                double y = plot.evaluate(x);
                if (!Double.isFinite(y)) {
                    if (inSeg && pts.length() > 0) {
                        flushPolyline(svg, pts, stroke, tip);
                        pts.setLength(0);
                        inSeg = false;
                    }
                    continue;
                }
                int px = left + (int) Math.round((x - xMin) / (xMax - xMin) * plotW);
                int py = bottom - (int) Math.round((y - yMin) / (yMax - yMin) * plotH);
                if (inSeg) {
                    pts.append(" ");
                }
                pts.append(px)
                    .append(",")
                    .append(py);
                inSeg = true;
            }
            if (inSeg && pts.length() > 0) {
                flushPolyline(svg, pts, stroke, tip);
            }
        }

        // Explicit marked points
        if (points != null) {
            for (MarkedPoint pt : points) {
                if (pt.getMode() == MarkedPoint.MODE_EXPLICIT) {
                    double pxVal = pt.getValueA();
                    double pyVal = pt.getValueB();
                    if (Double.isFinite(pxVal) && Double.isFinite(pyVal)) {
                        int px = left + (int) Math.round((pxVal - xMin) / (xMax - xMin) * plotW);
                        int py = bottom - (int) Math.round((pyVal - yMin) / (yMax - yMin) * plotH);
                        int pColor = pt.getColor();
                        if (pt.isColorInherit() && !plots.isEmpty()) {
                            int idx = Math.max(0, Math.min(pt.getPlotIndex(), plots.size() - 1));
                            pColor = plots.get(idx)
                                .getColor();
                        }
                        svg.append("<circle class=\"guide-chart-shape\" cx=\"")
                            .append(px)
                            .append("\" cy=\"")
                            .append(py)
                            .append("\" r=\"3\" fill=\"")
                            .append(argbToRgba(pColor))
                            .append("\"><title>")
                            .append(
                                esc(
                                    (pt.getLabel() != null && !pt.getLabel()
                                        .isEmpty() ? pt.getLabel() + ": " : "") + "("
                                        + formatNum(pxVal)
                                        + ", "
                                        + formatNum(pyVal)
                                        + ")"))
                            .append("</title></circle>");
                    }
                }
            }
        }

        svg.append("</g>");
        return svg.append("</svg>")
            .toString();
    }

    private static void flushPolyline(StringBuilder svg, StringBuilder pts, String stroke) {
        flushPolyline(svg, pts, stroke, null);
    }

    private static void flushPolyline(StringBuilder svg, StringBuilder pts, String stroke,
        @Nullable String tip) {
        svg.append("<polyline class=\"guide-chart-shape\" points=\"")
            .append(pts)
            .append("\" stroke=\"")
            .append(stroke)
            .append("\" stroke-width=\"1.5\" fill=\"none\"");
        if (tip != null && !tip.isEmpty()) {
            svg.append("><title>")
                .append(esc(tip))
                .append("</title></polyline>");
        } else {
            svg.append("/>");
        }
    }

    /**
     * Build a short tooltip string for a chart shape. Empty fields are dropped so we get e.g.
     * "Foo: 12" or "Q1 · Foo: 12".
     */
    private static String buildChartTip(String category, String series, double value) {
        StringBuilder sb = new StringBuilder();
        if (category != null && !category.isEmpty()) {
            sb.append(category);
        }
        if (series != null && !series.isEmpty()) {
            if (sb.length() > 0) {
                sb.append(" · ");
            }
            sb.append(series);
        }
        if (sb.length() > 0) {
            sb.append(": ");
        }
        sb.append(formatNum(value));
        return sb.toString();
    }

    // ===== Shared SVG helpers =====

    private static StringBuilder openSvg(int w, int h, String cls, int bgColor, int borderColor) {
        StringBuilder svg = new StringBuilder();
        svg.append("<svg class=\"")
            .append(cls)
            .append("\" width=\"")
            .append(w)
            .append("\" height=\"")
            .append(h)
            .append("\" viewBox=\"0 0 ")
            .append(w)
            .append(" ")
            .append(h)
            .append("\">");
        svg.append("<rect width=\"")
            .append(w)
            .append("\" height=\"")
            .append(h)
            .append("\" fill=\"")
            .append(argbToRgba(bgColor))
            .append("\"");
        if ((borderColor >>> 24) != 0) {
            svg.append(" stroke=\"")
                .append(argbToRgba(borderColor))
                .append("\" stroke-width=\"1\"");
        }
        svg.append("/>");
        return svg;
    }

    private static void appendTitle(StringBuilder svg, String title, int w) {
        if (title == null || title.isEmpty()) {
            return;
        }
        svg.append("<text x=\"")
            .append(w / 2)
            .append("\" y=\"")
            .append(PADDING + TITLE_H)
            .append("\" text-anchor=\"middle\" font-size=\"10\" fill=\"#E6E6E6\" font-family=\"inherit\">")
            .append(esc(title))
            .append("</text>");
    }

    private static void appendYGridAndLabels(StringBuilder svg, int left, int right, int top, int bottom, int plotH,
        double yMin, double yMax) {
        double yStep = niceStep((yMax - yMin) / 5.0);
        for (double yv = Math.floor(yMin / yStep) * yStep; yv <= yMax + yStep * 0.01; yv += yStep) {
            int gy = bottom - (int) Math.round((yv - yMin) / (yMax - yMin) * plotH);
            if (gy < top - 2 || gy > bottom + 2) {
                continue;
            }
            svg.append("<line x1=\"")
                .append(left)
                .append("\" y1=\"")
                .append(gy)
                .append("\" x2=\"")
                .append(right)
                .append("\" y2=\"")
                .append(gy)
                .append("\" stroke=\"#3A4047\" stroke-width=\"1\"/>");
            svg.append("<text x=\"")
                .append(left - 3)
                .append("\" y=\"")
                .append(gy + 4)
                .append("\" text-anchor=\"end\" font-size=\"8\" fill=\"#B8C2CF\" font-family=\"inherit\">")
                .append(esc(formatNum(yv)))
                .append("</text>");
        }
    }

    private static void appendCategoryXLabels(StringBuilder svg, String[] categories, int left, double clusterW,
        int bottom) {
        for (int ci = 0; ci < categories.length; ci++) {
            int cx = left + (int) Math.round((ci + 0.5) * clusterW);
            svg.append("<text x=\"")
                .append(cx)
                .append("\" y=\"")
                .append(bottom + 10)
                .append("\" text-anchor=\"middle\" font-size=\"8\" fill=\"#B8C2CF\" font-family=\"inherit\">")
                .append(esc(categories[ci]))
                .append("</text>");
        }
    }

    private static void appendYAxis(StringBuilder svg, int left, int top, int bottom) {
        svg.append("<line x1=\"")
            .append(left)
            .append("\" y1=\"")
            .append(top)
            .append("\" x2=\"")
            .append(left)
            .append("\" y2=\"")
            .append(bottom)
            .append("\" stroke=\"#B8C2CF\" stroke-width=\"1\"/>");
    }

    private static void appendXAxis(StringBuilder svg, int left, int right, int bottom) {
        svg.append("<line x1=\"")
            .append(left)
            .append("\" y1=\"")
            .append(bottom)
            .append("\" x2=\"")
            .append(right)
            .append("\" y2=\"")
            .append(bottom)
            .append("\" stroke=\"#B8C2CF\" stroke-width=\"1\"/>");
    }

    private static void renderLegend(StringBuilder svg, List<SeriesData> series, int x, int y, int availW) {
        int itemW = Math.max(60, Math.min(100, availW / Math.max(1, series.size())));
        int maxCols = Math.max(1, availW / itemW);
        int col = 0;
        int curX = x;
        int curY = y;
        for (SeriesData s : series) {
            if (col >= maxCols) {
                col = 0;
                curX = x;
                curY += LEGEND_ROW_H + 2;
            }
            svg.append("<rect x=\"")
                .append(curX)
                .append("\" y=\"")
                .append(curY)
                .append("\" width=\"")
                .append(LEGEND_SWATCH)
                .append("\" height=\"")
                .append(LEGEND_SWATCH)
                .append("\" fill=\"")
                .append(argbToRgba(s.color))
                .append("\"/>");
            svg.append("<text x=\"")
                .append(curX + LEGEND_SWATCH + LEGEND_GAP)
                .append("\" y=\"")
                .append(curY + LEGEND_SWATCH - 1)
                .append("\" font-size=\"9\" fill=\"#D7DEE7\" font-family=\"inherit\">")
                .append(esc(s.name))
                .append("</text>");
            curX += itemW;
            col++;
        }
    }

    private static int computeTitleBottom(String title) {
        if (title == null || title.isEmpty()) {
            return PADDING;
        }
        return PADDING + TITLE_H + TITLE_GAP;
    }

    private static int computeLegendH(List<SeriesData> series, boolean showLegend, int w) {
        if (!showLegend || series == null || series.isEmpty()) {
            return 0;
        }
        int itemW = Math.max(60, Math.min(100, (w - 2 * PADDING) / series.size()));
        int cols = Math.max(1, (w - 2 * PADDING) / itemW);
        return (int) Math.ceil((double) series.size() / cols) * (LEGEND_ROW_H + 2) + LEGEND_GAP;
    }

    // ===== Number / coordinate formatters =====

    /** Format a number for SVG axis labels: integer if whole, else limited decimals. */
    static String formatNum(double v) {
        if (v == 0) {
            return "0";
        }
        double absV = Math.abs(v);
        if (absV >= 1e6 || (absV < 1e-3)) {
            return String.format("%.2e", v);
        }
        if (absV == Math.floor(absV) && absV < 1e5) {
            return String.valueOf((long) v);
        }
        String s = String.format("%.4f", v);
        int dot = s.indexOf('.');
        if (dot >= 0) {
            int last = s.length() - 1;
            while (last > dot && s.charAt(last) == '0') {
                last--;
            }
            if (last == dot) {
                last--;
            }
            s = s.substring(0, last + 1);
        }
        return s;
    }

    /** Format a double to one decimal place for SVG geometry. */
    private static String fmtD(double v) {
        long r = Math.round(v * 10);
        if (r % 10 == 0) {
            return String.valueOf(r / 10);
        }
        return (r / 10) + "." + Math.abs(r % 10);
    }

    /** Choose a nice grid step for the given rough interval. */
    private static double niceStep(double rough) {
        if (rough <= 0) {
            return 1;
        }
        double exp = Math.pow(10, Math.floor(Math.log10(rough)));
        double frac = rough / exp;
        if (frac < 1.5) {
            return exp;
        }
        if (frac < 3.5) {
            return 2 * exp;
        }
        if (frac < 7.5) {
            return 5 * exp;
        }
        return 10 * exp;
    }
}
