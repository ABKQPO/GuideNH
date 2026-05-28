package com.hfstudio.guidenh.guide.siteexport.site;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.guide.document.block.chart.CornerLegendPosition;
import com.hfstudio.guidenh.guide.document.block.chart.CornerLegendRenderer;
import com.hfstudio.guidenh.guide.document.block.functiongraph.AutoPointLabelMode;
import com.hfstudio.guidenh.guide.document.block.functiongraph.AutoPointSpec;
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

/**
 * Generates static HTML and SVG markup for chart, function-graph, file-tree,
 * mermaid-mindmap, and CSV table elements used in the static site export.
 */
public class GuideSiteGraphRenderer {

    // Monotonically increasing counter used to generate unique clip-path IDs so that
    // multiple function-graph SVGs embedded in the same HTML page do not share the
    // same id="gc" definition (inline SVGs share the document's ID namespace).
    private static int nextClipId = 0;

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
    private static final int PIE_OUTSIDE_GAP = 6;
    // Function graph sample count
    private static final int N_SAMPLES = 1024;
    private static final int AUTO_POINT_MAX_PER_PLOT = 96;
    private static final int AUTO_POINT_MAX_TARGETS_PER_PLOT = 256;
    private static final int AUTO_POINT_SCAN_STEPS = 128;
    private static final int AUTO_POINT_SOLVE_STEPS = 24;
    private static final int AUTO_POINT_LABEL_GAP = 4;
    private static final int CORNER_LEGEND_PADDING_X = 5;
    private static final int CORNER_LEGEND_PADDING_Y = 4;
    private static final int CORNER_LEGEND_GAP = 4;
    private static final int CORNER_LEGEND_ROW_H = 11;
    private static final int CORNER_LEGEND_MARKER_W = 10;
    private static final int CORNER_LEGEND_MARKER_H = 6;

    private GuideSiteGraphRenderer() {}

    // HTML escaping helpers.

    public static String esc(String s) {
        if (s == null) {
            return "";
        }
        return s.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;");
    }

    // Color conversion.

    /** Convert ARGB int (0xAARRGGBB) to CSS hex or rgba(). */
    public static String argbToRgba(int argb) {
        int a = (argb >> 24) & 0xFF;
        int r = (argb >> 16) & 0xFF;
        int g = (argb >> 8) & 0xFF;
        int b = argb & 0xFF;
        if (a == 0xFF) {
            return String.format("#%02X%02X%02X", r, g, b);
        }
        return String.format("rgba(%d,%d,%d,%.3f)", r, g, b, a / 255.0);
    }

    // File tree.

    public static String renderFileTree(String source) {
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

    // Mermaid mindmap (SVG).
    // The HTML renderer mirrors the in-game LytMermaidMindmapCanvas: rounded boxes
    // with a colored accent stripe on the left, 1px L-shaped connectors, top-down layout.
    // Text width is approximated because we do not have access to MC font metrics here.

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

    private static class MmLayoutNode {

        private final MermaidMindmapNode source;
        @Nullable
        private final String parentId;
        private final boolean isRoot;
        @Nullable
        private final String htmlContent;
        private final String[] lines;
        private final @Nullable String badge;
        final List<MmLayoutNode> children = new ArrayList<>();

        private MmLayoutNode(MermaidMindmapNode source, @Nullable String parentId, boolean isRoot,
            @Nullable String htmlContent) {
            this.source = source;
            this.parentId = parentId;
            this.isRoot = isRoot;
            this.htmlContent = htmlContent;
            String text = source.getText() != null ? source.getText() : "";
            this.lines = text.isEmpty() ? new String[] { "" } : text.split("\n");
            this.badge = simplifyMermaidIcon(source.getIcon());
        }
    }

    public static String renderMermaidTree(MermaidMindmapDocument doc) {
        return renderMermaidTree(doc, new LinkedHashMap<String, String>());
    }

    public static String renderMermaidTree(MermaidMindmapDocument doc, Map<String, String> nodeHtmlById) {
        if (doc == null || doc.getRoot() == null) {
            return "<div class=\"guide-mermaid-pan\" data-guide-pannable>"
                + "<div class=\"guide-mermaid-stage\" data-guide-mermaid-stage>"
                + "<svg class=\"guide-mermaid-canvas\" width=\"100\" height=\"40\"></svg>"
                + "<div class=\"guide-mermaid-node-layer\"></div></div></div>";
        }
        MmLayoutNode root = buildMmLayout(
            doc.getRoot(),
            null,
            true,
            nodeHtmlById != null ? nodeHtmlById : new LinkedHashMap<String, String>());
        StringBuilder html = new StringBuilder();
        html.append(
            "<div class=\"guide-mermaid-pan\" data-guide-pannable><div class=\"guide-mermaid-stage\" data-guide-mermaid-stage>");
        html.append(
            "<svg class=\"guide-mermaid-canvas\" xmlns=\"http://www.w3.org/2000/svg\" aria-hidden=\"true\"></svg>");
        html.append("<div class=\"guide-mermaid-node-layer\">");
        renderMmNodes(html, root);
        html.append("</div></div></div>");
        return html.toString();
    }

    private static MmLayoutNode buildMmLayout(MermaidMindmapNode source, @Nullable String parentId, boolean isRoot,
        Map<String, String> nodeHtmlById) {
        MmLayoutNode node = new MmLayoutNode(source, parentId, isRoot, nodeHtmlById.get(source.getId()));
        for (MermaidMindmapNode child : source.getChildren()) {
            node.children.add(buildMmLayout(child, source.getId(), false, nodeHtmlById));
        }
        return node;
    }

    private static void renderMmNodes(StringBuilder html, MmLayoutNode node) {
        html.append("<article class=\"guide-mermaid-node guide-mermaid-shape-")
            .append(escapeShapeClass(node.source.getShape()))
            .append(node.isRoot ? " guide-mermaid-node-root" : "")
            .append("\" data-node-id=\"")
            .append(esc(node.source.getId()))
            .append("\"");
        if (node.parentId != null && !node.parentId.isEmpty()) {
            html.append(" data-parent-id=\"")
                .append(esc(node.parentId))
                .append("\"");
        }
        html.append(" data-accent=\"")
            .append(argbToRgba(resolveMmAccent(node)))
            .append("\"");
        html.append(">");
        html.append("<span class=\"guide-mermaid-node-accent\"></span>");
        if (node.badge != null && !node.badge.isEmpty()) {
            html.append("<div class=\"guide-mermaid-node-badge\">")
                .append(esc(node.badge))
                .append("</div>");
        }
        html.append("<div class=\"guide-mermaid-node-body\">");
        if (node.htmlContent != null && !node.htmlContent.trim()
            .isEmpty()) {
            html.append(node.htmlContent);
        } else {
            appendMmFallbackLines(html, node);
        }
        html.append("</div></article>");
        for (MmLayoutNode child : node.children) {
            renderMmNodes(html, child);
        }
    }

    private static void appendMmFallbackLines(StringBuilder html, MmLayoutNode node) {
        for (String line : node.lines) {
            html.append("<div class=\"guide-mermaid-node-line\">")
                .append(esc(line))
                .append("</div>");
        }
    }

    private static String escapeShapeClass(@Nullable MermaidMindmapNodeShape shape) {
        return switch (shape != null ? shape : MermaidMindmapNodeShape.DEFAULT) {
            case ROUNDED -> "rounded";
            case CIRCLE -> "circle";
            case HEXAGON -> "hexagon";
            case CLOUD -> "cloud";
            case BANG -> "bang";
            case SQUARE -> "square";
            default -> "default";
        };
    }

    private static @Nullable String simplifyMermaidIcon(@Nullable String icon) {
        if (icon == null || icon.trim()
            .isEmpty()) {
            return null;
        }
        String trimmed = icon.trim();
        String leaf = trimmed.substring(lastWhitespaceSeparatedTokenStart(trimmed));
        if (leaf.startsWith("fa-")) {
            leaf = leaf.substring(3);
        }
        leaf = leaf.replace('-', ' ')
            .trim();
        return leaf.isEmpty() ? trimmed : leaf;
    }

    private static int lastWhitespaceSeparatedTokenStart(String text) {
        int index = text.length() - 1;
        while (index >= 0 && !Character.isWhitespace(text.charAt(index))) {
            index--;
        }
        return index + 1;
    }

    private static int resolveMmAccent(MmLayoutNode node) {
        int accent = MM_DEFAULT_ACCENT;
        for (String className : node.source.getClasses()) {
            String lower = className.toLowerCase(Locale.ROOT);
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

    // CSV table.

    public static String renderCsvTable(String csvSource, boolean hasHeader) {
        List<List<String>> rows = CsvTableParser.parse(csvSource);
        return renderCsvTable(rows, hasHeader);
    }

    public static String renderCsvTable(List<List<String>> rows, boolean hasHeader) {
        if (rows.isEmpty()) {
            return "<div class=\"guide-csv-table-wrap\"><table class=\"guide-csv-table\"></table></div>";
        }
        StringBuilder html = new StringBuilder();
        html.append("<div class=\"guide-csv-table-wrap\"><table class=\"guide-csv-table\">");
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
        html.append("</table></div>");
        return html.toString();
    }

    // Chart data holder classes.

    /** Chart series data (name, ARGB color, parallel xs/ys arrays). */
    public static final class SeriesData {

        /** Series rendered as vertical bars (default). */
        public static final String TYPE_COLUMN = "column";
        /** Series rendered as a line overlay on a column chart. */
        public static final String TYPE_LINE = "line";

        public final String name;
        public final int color;
        public final double[] xs;
        public final double[] ys;
        /** Rendering type: {@link #TYPE_COLUMN} or {@link #TYPE_LINE}. */
        public final String type;

        public SeriesData(String name, int color, double[] xs, double[] ys) {
            this(name, color, xs, ys, TYPE_COLUMN);
        }

        public SeriesData(String name, int color, double[] xs, double[] ys, String type) {
            this.name = name != null ? name : "";
            this.color = color;
            this.xs = xs != null ? xs : new double[0];
            this.ys = ys != null ? ys : new double[0];
            this.type = type != null ? type : TYPE_COLUMN;
        }
    }

    /** Inset pie chart that overlays a corner of a column chart. */
    public static final class PieInsetData {

        public final List<SliceData> slices;
        /** Diameter of the inset pie in SVG user-space units. */
        public final int size;
        /** Corner or side placement: "top-right", "left", "bottom-right", "bottom-left", "right". */
        public final String position;
        public final @Nullable String title;

        public PieInsetData(List<SliceData> slices, int size, String position, @Nullable String title) {
            this.slices = slices != null ? slices : new ArrayList<>();
            this.size = Math.max(20, size);
            this.position = position != null ? position : "top-right";
            this.title = title;
        }
    }

    /** Pie-chart slice (label, value, ARGB color). */
    public static final class SliceData {

        public final String label;
        public final double value;
        public final int color;

        public SliceData(String label, double value, int color) {
            this.label = label != null ? label : "";
            this.value = value;
            this.color = color;
        }
    }

    // Column chart (vertical bars, categorical X).

    /** Backward-compatible overload. Delegates to the composite version with no inset. */
    public static String renderColumnChart(int w, int h, int bgColor, int borderColor, String title,
        String[] categories, List<SeriesData> series, boolean showLegend) {
        return renderColumnChart(w, h, bgColor, borderColor, title, categories, series, showLegend, null, null, false);
    }

    /**
     * Composite column chart: renders bar series, optional line-overlay series, and an optional
     * pie inset in one of the chart's corners.
     *
     * @param pieInset   optional pie inset; {@code null} for a plain column chart
     * @param yAxisUnit  optional unit label shown beside the Y-axis (e.g. "t"); {@code null} to omit
     * @param labelAbove when {@code true}, draw the numeric value above each bar
     */
    public static String renderColumnChart(int w, int h, int bgColor, int borderColor, String title,
        String[] categories, List<SeriesData> series, boolean showLegend, @Nullable PieInsetData pieInset,
        @Nullable String yAxisUnit, boolean labelAbove) {
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
        boolean pieRightOutside = pieInset != null && isPieInsetRightOutside(pieInset.position);
        int right = w - PADDING - (pieRightOutside ? pieInset.size + PIE_OUTSIDE_GAP : 0);
        int top = titleBottom;
        int bottom = h - PADDING - AXIS_PAD_BOTTOM - legendH;
        int plotW = Math.max(1, right - left);
        int plotH = Math.max(1, bottom - top);

        int nCat = Math.max(1, categories.length);
        double clusterW = (double) plotW / nCat;

        StringBuilder svg = openSvg(w, h, "guide-chart", bgColor, borderColor);
        appendTitle(svg, title, w);
        appendYGridAndLabels(svg, left, right, top, bottom, plotH, yMin, yMax);
        appendCategoryXLabels(svg, categories, left, clusterW, bottom);

        // Y-axis unit label (e.g. "(t)" above Y axis)
        if (yAxisUnit != null && !yAxisUnit.isEmpty()) {
            svg.append("<text x=\"")
                .append(left - 3)
                .append("\" y=\"")
                .append(top - 2)
                .append("\" text-anchor=\"end\" font-size=\"7\" fill=\"#B8C2CF\" font-family=\"inherit\">(")
                .append(esc(yAxisUnit))
                .append(")</text>");
        }

        // Determine how many COLUMN series there are for bar-width calculation
        int nColSer = (int) series.stream()
            .filter(s -> SeriesData.TYPE_COLUMN.equals(s.type))
            .count();
        if (nColSer == 0) nColSer = 1;
        double barW = clusterW * 0.7 / nColSer;
        double gap = (clusterW - barW * nColSer) / 2.0;

        int colIdx = 0;
        for (SeriesData s : series) {
            if (!SeriesData.TYPE_COLUMN.equals(s.type)) continue;
            String fill = argbToRgba(s.color);
            int len = Math.min(s.xs.length, s.ys.length);
            for (int di = 0; di < len; di++) {
                int ci = (int) s.xs[di];
                if (ci < 0 || ci >= nCat) {
                    continue;
                }
                double value = s.ys[di];
                double bx = left + ci * clusterW + gap + colIdx * barW;
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
                if (labelAbove) {
                    svg.append("<text x=\"")
                        .append(fmtD(bx + barW / 2))
                        .append("\" y=\"")
                        .append(fmtD(by - 2))
                        .append("\" text-anchor=\"middle\" font-size=\"7\" fill=\"")
                        .append(fill)
                        .append("\" font-family=\"inherit\">")
                        .append(esc(formatNum(value)))
                        .append("</text>");
                }
            }
            colIdx++;
        }

        // Line-overlay series (same Y scale as column bars)
        for (SeriesData s : series) {
            if (!SeriesData.TYPE_LINE.equals(s.type)) continue;
            String stroke = argbToRgba(s.color);
            int len = Math.min(s.xs.length, s.ys.length);
            StringBuilder linePts = new StringBuilder();
            for (int di = 0; di < len; di++) {
                int ci = (int) s.xs[di];
                if (ci < 0 || ci >= nCat) continue;
                double cx = left + (ci + 0.5) * clusterW;
                double cy = bottom - (s.ys[di] - yMin) / (yMax - yMin) * plotH;
                if (linePts.length() > 0) linePts.append(" ");
                linePts.append(fmtD(cx))
                    .append(",")
                    .append(fmtD(cy));
            }
            if (linePts.length() > 0) {
                svg.append("<polyline class=\"guide-chart-shape\" points=\"")
                    .append(linePts)
                    .append("\" stroke=\"")
                    .append(stroke)
                    .append("\" stroke-width=\"2\" fill=\"none\"><title>")
                    .append(esc(s.name))
                    .append("</title></polyline>");
                // Dots at each data point
                for (int di = 0; di < len; di++) {
                    int ci = (int) s.xs[di];
                    if (ci < 0 || ci >= nCat) continue;
                    double cx = left + (ci + 0.5) * clusterW;
                    double cy = bottom - (s.ys[di] - yMin) / (yMax - yMin) * plotH;
                    svg.append("<circle class=\"guide-chart-shape\" cx=\"")
                        .append(fmtD(cx))
                        .append("\" cy=\"")
                        .append(fmtD(cy))
                        .append("\" r=\"3\" fill=\"")
                        .append(stroke)
                        .append("\"><title>")
                        .append(esc(buildChartTip(ci < categories.length ? categories[ci] : "", s.name, s.ys[di])))
                        .append("</title></circle>");
                }
            }
        }

        // Pie inset overlay
        if (pieInset != null && !pieInset.slices.isEmpty()) {
            double total = 0;
            for (SliceData sl : pieInset.slices) total += sl.value;
            if (total <= 0) total = 1;
            int pr = pieInset.size / 2;
            int pcx, pcy;
            String position = normalizePieInsetPosition(pieInset.position);
            switch (position) {
                case "left":
                    pcx = left + pr + 4;
                    pcy = top + pr + 4;
                    break;
                case "right":
                    pcx = w - PADDING - pr;
                    pcy = top + Math.max(pr, plotH / 2);
                    break;
                case "bottom-right":
                    pcx = right - pr - 4;
                    pcy = bottom - pr - 4;
                    break;
                case "bottom-left":
                    pcx = left + pr + 4;
                    pcy = bottom - pr - 4;
                    break;
                default:
                    pcx = right - pr - 4;
                    pcy = top + pr + 4;
                    break;
            }
            // Semi-transparent backing circle
            svg.append("<circle cx=\"")
                .append(pcx)
                .append("\" cy=\"")
                .append(pcy)
                .append("\" r=\"")
                .append(pr + 2)
                .append("\" fill=\"")
                .append(argbToRgba(bgColor))
                .append("\" opacity=\"0.85\"/>");
            // Pie slices
            double ang = -Math.PI / 2;
            for (SliceData sl : pieInset.slices) {
                double sweep = (sl.value / total) * 2 * Math.PI;
                double ea = ang + sweep;
                double x1 = pcx + pr * Math.cos(ang);
                double y1 = pcy + pr * Math.sin(ang);
                double x2 = pcx + pr * Math.cos(ea);
                double y2 = pcy + pr * Math.sin(ea);
                int la = sweep > Math.PI ? 1 : 0;
                double pct = (sl.value / total) * 100.0;
                svg.append("<path class=\"guide-chart-shape\" d=\"M ")
                    .append(pcx)
                    .append(" ")
                    .append(pcy)
                    .append(" L ")
                    .append(fmtD(x1))
                    .append(" ")
                    .append(fmtD(y1))
                    .append(" A ")
                    .append(pr)
                    .append(" ")
                    .append(pr)
                    .append(" 0 ")
                    .append(la)
                    .append(" 1 ")
                    .append(fmtD(x2))
                    .append(" ")
                    .append(fmtD(y2))
                    .append(" Z\" fill=\"")
                    .append(argbToRgba(sl.color))
                    .append("\" stroke=\"")
                    .append(argbToRgba(bgColor))
                    .append("\" stroke-width=\"0.5\"><title>")
                    .append(esc(sl.label + ": " + formatNum(sl.value) + " (" + String.format("%.1f", pct) + "%)"))
                    .append("</title></path>");
                ang = ea;
            }
            // Inset title
            if (pieInset.title != null && !pieInset.title.isEmpty()) {
                svg.append("<text x=\"")
                    .append(pcx)
                    .append("\" y=\"")
                    .append(pcy + pr + 9)
                    .append("\" text-anchor=\"middle\" font-size=\"7\" fill=\"#B8C2CF\" font-family=\"inherit\">")
                    .append(esc(pieInset.title))
                    .append("</text>");
            }
        }

        appendYAxis(svg, left, top, bottom);
        appendXAxis(svg, left, right, bottom);
        if (showLegend) {
            renderLegend(svg, series, left, bottom + AXIS_PAD_BOTTOM + LEGEND_GAP, w - 2 * PADDING);
        }
        return svg.append("</svg></div>")
            .toString();
    }

    private static boolean isPieInsetRightOutside(@Nullable String position) {
        return "right".equals(normalizePieInsetPosition(position));
    }

    private static String normalizePieInsetPosition(@Nullable String position) {
        if (position == null) {
            return "top-right";
        }
        return switch (position.trim()
            .toLowerCase(Locale.ROOT)) {
            case "left", "topleft", "top-left", "tl" -> "left";
            case "bottomright", "bottom-right", "br" -> "bottom-right";
            case "bottomleft", "bottom-left", "bl" -> "bottom-left";
            case "right", "rightoutside", "right-outside", "outside", "side" -> "right";
            default -> "top-right";
        };
    }

    // Bar chart (horizontal bars, categorical Y).

    public static String renderBarChart(int w, int h, int bgColor, int borderColor, String title, String[] categories,
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
        return svg.append("</svg></div>")
            .toString();
    }

    // Line chart.

    public static String renderLineChart(int w, int h, int bgColor, int borderColor, String title, String[] categories,
        List<SeriesData> series, boolean numericX, boolean showPoints, boolean showLegend) {
        return renderLineChart(
            w,
            h,
            bgColor,
            borderColor,
            title,
            categories,
            series,
            numericX,
            showPoints,
            showLegend,
            CornerLegendPosition.NONE,
            CornerLegendRenderer.DEFAULT_WIDTH,
            CornerLegendRenderer.DEFAULT_HEIGHT,
            CornerLegendRenderer.DEFAULT_BACKGROUND);
    }

    public static String renderLineChart(int w, int h, int bgColor, int borderColor, String title, String[] categories,
        List<SeriesData> series, boolean numericX, boolean showPoints, boolean showLegend,
        CornerLegendPosition cornerLegendPosition, int cornerLegendWidth, int cornerLegendHeight,
        int cornerLegendBackgroundColor) {
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
        renderChartCornerLegend(
            svg,
            series,
            left,
            right,
            top,
            bottom,
            cornerLegendPosition,
            cornerLegendWidth,
            cornerLegendHeight,
            cornerLegendBackgroundColor,
            true);
        if (showLegend) {
            renderLegend(svg, series, left, bottom + AXIS_PAD_BOTTOM + LEGEND_GAP, w - 2 * PADDING);
        }
        return svg.append("</svg></div>")
            .toString();
    }

    // Pie chart.

    public static String renderPieChart(int w, int h, int bgColor, int borderColor, String title,
        List<SliceData> slices, boolean showLegend) {
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
        return svg.append("</svg></div>")
            .toString();
    }

    // Scatter chart.

    public static String renderScatterChart(int w, int h, int bgColor, int borderColor, String title,
        List<SeriesData> series, boolean showLegend) {
        return renderScatterChart(
            w,
            h,
            bgColor,
            borderColor,
            title,
            series,
            showLegend,
            CornerLegendPosition.NONE,
            CornerLegendRenderer.DEFAULT_WIDTH,
            CornerLegendRenderer.DEFAULT_HEIGHT,
            CornerLegendRenderer.DEFAULT_BACKGROUND);
    }

    public static String renderScatterChart(int w, int h, int bgColor, int borderColor, String title,
        List<SeriesData> series, boolean showLegend, CornerLegendPosition cornerLegendPosition, int cornerLegendWidth,
        int cornerLegendHeight, int cornerLegendBackgroundColor) {
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
        renderChartCornerLegend(
            svg,
            series,
            left,
            right,
            top,
            bottom,
            cornerLegendPosition,
            cornerLegendWidth,
            cornerLegendHeight,
            cornerLegendBackgroundColor,
            false);
        if (showLegend) {
            renderLegend(svg, series, left, bottom + AXIS_PAD_BOTTOM + LEGEND_GAP, w - 2 * PADDING);
        }
        return svg.append("</svg></div>")
            .toString();
    }

    // Function graph.

    public static String renderFunctionGraph(LytFunctionGraph graph) {
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
            yMax,
            graph.getCornerLegendPosition(),
            graph.getCornerLegendWidth(),
            graph.getCornerLegendHeight(),
            graph.getCornerLegendBackgroundColor());
    }

    public static String renderFunctionGraphSvg(List<FunctionPlot> plots, List<MarkedPoint> points, int w, int h,
        String title, int bgColor, int borderColor, int axisColor, int gridColor, boolean showGrid, boolean showAxes,
        double xMin, double xMax, double yMin, double yMax) {
        return renderFunctionGraphSvg(
            plots,
            points,
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
            yMax,
            CornerLegendPosition.NONE,
            CornerLegendRenderer.DEFAULT_WIDTH,
            CornerLegendRenderer.DEFAULT_HEIGHT,
            CornerLegendRenderer.DEFAULT_BACKGROUND);
    }

    public static String renderFunctionGraphSvg(List<FunctionPlot> plots, List<MarkedPoint> points, int w, int h,
        String title, int bgColor, int borderColor, int axisColor, int gridColor, boolean showGrid, boolean showAxes,
        double xMin, double xMax, double yMin, double yMax, CornerLegendPosition cornerLegendPosition,
        int cornerLegendWidth, int cornerLegendHeight, int cornerLegendBackgroundColor) {

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
        // Expose the plot domain and plot rect so client-side JS (installChartHoverTooltips)
        // can map cursor pixels to data x/y for live (x, y) tooltips.
        // We cannot add attributes to the already-emitted <svg> tag easily without rewriting
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

        // Clip path for curve rendering. Use a unique ID so that multiple function-graph
        // SVGs embedded in the same HTML page do not collide on the shared document ID namespace.
        String clipId = "fg" + nextClipId++;
        svg.append("<defs><clipPath id=\"")
            .append(clipId)
            .append("\"><rect x=\"")
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
            // Zero-crossing axis lines.
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
        svg.append("<g clip-path=\"url(#")
            .append(clipId)
            .append(")\">");
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

        renderFunctionGraphAutoPoints(svg, plots, left, right, top, bottom, plotW, plotH, xMin, xMax, yMin, yMax);
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
        renderFunctionGraphCornerLegend(
            svg,
            plots,
            left,
            right,
            top,
            bottom,
            cornerLegendPosition,
            cornerLegendWidth,
            cornerLegendHeight,
            cornerLegendBackgroundColor);
        return svg.append("</svg></div>")
            .toString();
    }

    private static void renderFunctionGraphAutoPoints(StringBuilder svg, List<FunctionPlot> plots, int left, int right,
        int top, int bottom, int plotW, int plotH, double xMin, double xMax, double yMin, double yMax) {
        for (FunctionPlot plot : plots) {
            AutoPointSpec spec = plot.getAutoPointSpec();
            if (spec == null || !spec.isEnabled()) {
                continue;
            }
            int color = spec.colorInherit() ? plot.getColor() : spec.color();
            int drawn = 0;
            if (!Double.isNaN(spec.everyX())) {
                drawn = renderFunctionGraphAutoPointsEveryX(
                    svg,
                    plot,
                    spec,
                    color,
                    drawn,
                    left,
                    right,
                    top,
                    bottom,
                    plotW,
                    plotH,
                    xMin,
                    xMax,
                    yMin,
                    yMax);
            }
            if (!Double.isNaN(spec.everyY()) && drawn < AUTO_POINT_MAX_PER_PLOT) {
                renderFunctionGraphAutoPointsEveryY(
                    svg,
                    plot,
                    spec,
                    color,
                    drawn,
                    left,
                    right,
                    top,
                    bottom,
                    plotW,
                    plotH,
                    xMin,
                    xMax,
                    yMin,
                    yMax);
            }
        }
    }

    private static int renderFunctionGraphAutoPointsEveryX(StringBuilder svg, FunctionPlot plot, AutoPointSpec spec,
        int color, int drawn, int left, int right, int top, int bottom, int plotW, int plotH, double xMin, double xMax,
        double yMin, double yMax) {
        if (plot.isInverse()) {
            return renderFunctionGraphAutoPointIntersections(
                svg,
                plot,
                spec,
                color,
                spec.everyX(),
                true,
                drawn,
                left,
                right,
                top,
                bottom,
                plotW,
                plotH,
                xMin,
                xMax,
                yMin,
                yMax);
        }
        double step = spec.everyX();
        double value = Math.ceil(xMin / step) * step;
        int targets = 0;
        while (value <= xMax + 1e-9 && drawn < AUTO_POINT_MAX_PER_PLOT && targets < AUTO_POINT_MAX_TARGETS_PER_PLOT) {
            if (appendFunctionGraphAutoPoint(
                svg,
                value,
                plot.evaluate(value),
                color,
                spec.labelMode(),
                left,
                right,
                top,
                bottom,
                plotW,
                plotH,
                xMin,
                xMax,
                yMin,
                yMax)) {
                drawn++;
            }
            value += step;
            targets++;
        }
        return drawn;
    }

    private static int renderFunctionGraphAutoPointsEveryY(StringBuilder svg, FunctionPlot plot, AutoPointSpec spec,
        int color, int drawn, int left, int right, int top, int bottom, int plotW, int plotH, double xMin, double xMax,
        double yMin, double yMax) {
        if (plot.isInverse()) {
            double step = spec.everyY();
            double value = Math.ceil(yMin / step) * step;
            int targets = 0;
            while (value <= yMax + 1e-9 && drawn < AUTO_POINT_MAX_PER_PLOT
                && targets < AUTO_POINT_MAX_TARGETS_PER_PLOT) {
                if (appendFunctionGraphAutoPoint(
                    svg,
                    plot.evaluate(value),
                    value,
                    color,
                    spec.labelMode(),
                    left,
                    right,
                    top,
                    bottom,
                    plotW,
                    plotH,
                    xMin,
                    xMax,
                    yMin,
                    yMax)) {
                    drawn++;
                }
                value += step;
                targets++;
            }
            return drawn;
        }
        double step = spec.everyY();
        double value = Math.ceil(yMin / step) * step;
        int targets = 0;
        while (value <= yMax + 1e-9 && drawn < AUTO_POINT_MAX_PER_PLOT && targets < AUTO_POINT_MAX_TARGETS_PER_PLOT) {
            drawn = renderFunctionGraphAutoPointIntersections(
                svg,
                plot,
                spec,
                color,
                value,
                false,
                drawn,
                left,
                right,
                top,
                bottom,
                plotW,
                plotH,
                xMin,
                xMax,
                yMin,
                yMax);
            value += step;
            targets++;
        }
        return drawn;
    }

    private static int renderFunctionGraphAutoPointIntersections(StringBuilder svg, FunctionPlot plot,
        AutoPointSpec spec, int color, double target, boolean targetX, int drawn, int left, int right, int top,
        int bottom, int plotW, int plotH, double xMin, double xMax, double yMin, double yMax) {
        double independentMin = plot.isInverse() ? yMin : xMin;
        double independentMax = plot.isInverse() ? yMax : xMax;
        double prevIndependent = independentMin;
        double prevValue = functionGraphAutoPointDifference(plot, prevIndependent, target, targetX);
        for (int i = 1; i <= AUTO_POINT_SCAN_STEPS && drawn < AUTO_POINT_MAX_PER_PLOT; i++) {
            double independent = independentMin + (independentMax - independentMin) * i / AUTO_POINT_SCAN_STEPS;
            double value = functionGraphAutoPointDifference(plot, independent, target, targetX);
            if (Double.isFinite(prevValue) && Double.isFinite(value) && prevValue * value <= 0d) {
                double solved = solveFunctionGraphAutoPoint(
                    plot,
                    target,
                    targetX,
                    prevIndependent,
                    independent,
                    prevValue);
                double dataX = plot.isInverse() ? plot.evaluate(solved) : solved;
                double dataY = plot.isInverse() ? solved : plot.evaluate(solved);
                if (appendFunctionGraphAutoPoint(
                    svg,
                    dataX,
                    dataY,
                    color,
                    spec.labelMode(),
                    left,
                    right,
                    top,
                    bottom,
                    plotW,
                    plotH,
                    xMin,
                    xMax,
                    yMin,
                    yMax)) {
                    drawn++;
                }
            }
            prevIndependent = independent;
            prevValue = value;
        }
        return drawn;
    }

    private static double functionGraphAutoPointDifference(FunctionPlot plot, double independent, double target,
        boolean targetX) {
        double dataX = plot.isInverse() ? plot.evaluate(independent) : independent;
        double dataY = plot.isInverse() ? independent : plot.evaluate(independent);
        return (targetX ? dataX : dataY) - target;
    }

    private static double solveFunctionGraphAutoPoint(FunctionPlot plot, double target, boolean targetX, double lo,
        double hi, double fLo) {
        for (int i = 0; i < AUTO_POINT_SOLVE_STEPS; i++) {
            double mid = (lo + hi) * 0.5d;
            double fMid = functionGraphAutoPointDifference(plot, mid, target, targetX);
            if (!Double.isFinite(fMid) || Math.abs(fMid) < 1e-9) {
                return mid;
            }
            if (fLo * fMid <= 0d) {
                hi = mid;
            } else {
                lo = mid;
                fLo = fMid;
            }
        }
        return (lo + hi) * 0.5d;
    }

    private static boolean appendFunctionGraphAutoPoint(StringBuilder svg, double dataX, double dataY, int color,
        AutoPointLabelMode labelMode, int left, int right, int top, int bottom, int plotW, int plotH, double xMin,
        double xMax, double yMin, double yMax) {
        if (!Double.isFinite(dataX) || !Double.isFinite(dataY)) {
            return false;
        }
        int px = left + (int) Math.round((dataX - xMin) / (xMax - xMin) * plotW);
        int py = bottom - (int) Math.round((dataY - yMin) / (yMax - yMin) * plotH);
        if (px < left - 3 || px > right + 3 || py < top - 3 || py > bottom + 3) {
            return false;
        }
        String pointLabel = "(" + formatNum(dataX) + ", " + formatNum(dataY) + ")";
        svg.append("<circle class=\"guide-chart-shape\" cx=\"")
            .append(px)
            .append("\" cy=\"")
            .append(py)
            .append("\" r=\"4\" fill=\"#FFFFFF\"/>");
        svg.append("<circle class=\"guide-chart-shape\" cx=\"")
            .append(px)
            .append("\" cy=\"")
            .append(py)
            .append("\" r=\"3\" fill=\"")
            .append(argbToRgba(color))
            .append("\"><title>")
            .append(esc(pointLabel))
            .append("</title></circle>");
        if (labelMode != null && labelMode != AutoPointLabelMode.NONE) {
            String label = switch (labelMode) {
                case X -> formatNum(dataX);
                case Y -> formatNum(dataY);
                case XY -> pointLabel;
                case NONE -> "";
            };
            int textW = Math.max(1, label.length() * 6);
            int labelX = px + AUTO_POINT_LABEL_GAP;
            if (labelX + textW > right) {
                labelX = px - textW - AUTO_POINT_LABEL_GAP;
            }
            int labelY = py - AUTO_POINT_LABEL_GAP;
            if (labelY - 8 < top) {
                labelY = py + 10;
            }
            svg.append("<text x=\"")
                .append(labelX)
                .append("\" y=\"")
                .append(labelY)
                .append("\" font-size=\"8\" fill=\"#D7DEE7\" font-family=\"inherit\">")
                .append(esc(label))
                .append("</text>");
        }
        return true;
    }

    private static void renderFunctionGraphCornerLegend(StringBuilder svg, List<FunctionPlot> plots, int left,
        int right, int top, int bottom, CornerLegendPosition position, int maxWidth, int maxHeight,
        int backgroundColor) {
        if (position == null || position == CornerLegendPosition.NONE || plots == null || plots.isEmpty()) {
            return;
        }
        int plotW = right - left;
        int plotH = bottom - top;
        if (plotW < 24 || plotH < 12) {
            return;
        }
        int width = clamp(maxWidth > 0 ? maxWidth : CornerLegendRenderer.DEFAULT_WIDTH, 24, plotW);
        int height = clamp(maxHeight > 0 ? maxHeight : CornerLegendRenderer.DEFAULT_HEIGHT, 12, plotH);
        int capacity = Math.max(0, (height - CORNER_LEGEND_PADDING_Y * 2) / CORNER_LEGEND_ROW_H);
        if (capacity <= 0) {
            return;
        }
        List<FunctionPlot> visible = new ArrayList<>();
        for (FunctionPlot plot : plots) {
            if (plot.getLabel() != null && !plot.getLabel()
                .isEmpty()) {
                visible.add(plot);
                if (visible.size() >= capacity) {
                    break;
                }
            }
        }
        if (visible.isEmpty()) {
            return;
        }
        height = Math.min(height, visible.size() * CORNER_LEGEND_ROW_H + CORNER_LEGEND_PADDING_Y * 2);
        int x = switch (position) {
            case TOP_LEFT, BOTTOM_LEFT -> left + CORNER_LEGEND_GAP;
            case TOP_RIGHT, BOTTOM_RIGHT -> right - width - CORNER_LEGEND_GAP;
            case NONE -> right - width - CORNER_LEGEND_GAP;
        };
        int y = switch (position) {
            case TOP_LEFT, TOP_RIGHT -> top + CORNER_LEGEND_GAP;
            case BOTTOM_LEFT, BOTTOM_RIGHT -> bottom - height - CORNER_LEGEND_GAP;
            case NONE -> top + CORNER_LEGEND_GAP;
        };
        x = clamp(x, left, right - width);
        y = clamp(y, top, bottom - height);
        svg.append("<rect x=\"")
            .append(x)
            .append("\" y=\"")
            .append(y)
            .append("\" width=\"")
            .append(width)
            .append("\" height=\"")
            .append(height)
            .append("\" fill=\"")
            .append(argbToRgba(backgroundColor))
            .append("\" stroke=\"rgba(255,255,255,0.4)\" stroke-width=\"1\"/>");
        int rowY = y + CORNER_LEGEND_PADDING_Y + 8;
        int markerX = x + CORNER_LEGEND_PADDING_X;
        int textX = markerX + CORNER_LEGEND_MARKER_W + CORNER_LEGEND_GAP;
        int maxChars = Math.max(0, (x + width - CORNER_LEGEND_PADDING_X - textX) / 6);
        for (FunctionPlot plot : visible) {
            int markerY = rowY - CORNER_LEGEND_MARKER_H / 2 - 2;
            svg.append("<line x1=\"")
                .append(markerX)
                .append("\" y1=\"")
                .append(markerY + CORNER_LEGEND_MARKER_H / 2)
                .append("\" x2=\"")
                .append(markerX + CORNER_LEGEND_MARKER_W)
                .append("\" y2=\"")
                .append(markerY + CORNER_LEGEND_MARKER_H / 2)
                .append("\" stroke=\"")
                .append(argbToRgba(plot.getColor()))
                .append("\" stroke-width=\"1.5\"/>");
            svg.append("<text x=\"")
                .append(textX)
                .append("\" y=\"")
                .append(rowY)
                .append("\" font-size=\"9\" fill=\"#FFFFFF\" font-family=\"inherit\">")
                .append(esc(ellipsize(plot.getLabel(), maxChars)))
                .append("</text>");
            rowY += CORNER_LEGEND_ROW_H;
        }
    }

    private static void renderChartCornerLegend(StringBuilder svg, List<SeriesData> series, int left, int right,
        int top, int bottom, CornerLegendPosition position, int maxWidth, int maxHeight, int backgroundColor,
        boolean lineMarker) {
        if (position == null || position == CornerLegendPosition.NONE || series == null || series.isEmpty()) {
            return;
        }
        int plotW = right - left;
        int plotH = bottom - top;
        if (plotW < 24 || plotH < 12) {
            return;
        }
        int width = clamp(maxWidth > 0 ? maxWidth : CornerLegendRenderer.DEFAULT_WIDTH, 24, plotW);
        int height = clamp(maxHeight > 0 ? maxHeight : CornerLegendRenderer.DEFAULT_HEIGHT, 12, plotH);
        int capacity = Math.max(0, (height - CORNER_LEGEND_PADDING_Y * 2) / CORNER_LEGEND_ROW_H);
        if (capacity <= 0) {
            return;
        }
        List<SeriesData> visible = new ArrayList<>();
        for (SeriesData item : series) {
            if (item != null && item.name != null && !item.name.isEmpty()) {
                visible.add(item);
                if (visible.size() >= capacity) {
                    break;
                }
            }
        }
        if (visible.isEmpty()) {
            return;
        }
        height = Math.min(height, visible.size() * CORNER_LEGEND_ROW_H + CORNER_LEGEND_PADDING_Y * 2);
        int x = switch (position) {
            case TOP_LEFT, BOTTOM_LEFT -> left + CORNER_LEGEND_GAP;
            case TOP_RIGHT, BOTTOM_RIGHT -> right - width - CORNER_LEGEND_GAP;
            case NONE -> right - width - CORNER_LEGEND_GAP;
        };
        int y = switch (position) {
            case TOP_LEFT, TOP_RIGHT -> top + CORNER_LEGEND_GAP;
            case BOTTOM_LEFT, BOTTOM_RIGHT -> bottom - height - CORNER_LEGEND_GAP;
            case NONE -> top + CORNER_LEGEND_GAP;
        };
        x = clamp(x, left, right - width);
        y = clamp(y, top, bottom - height);
        svg.append("<rect x=\"")
            .append(x)
            .append("\" y=\"")
            .append(y)
            .append("\" width=\"")
            .append(width)
            .append("\" height=\"")
            .append(height)
            .append("\" fill=\"")
            .append(argbToRgba(backgroundColor))
            .append("\" stroke=\"rgba(255,255,255,0.4)\" stroke-width=\"1\"/>");
        int rowY = y + CORNER_LEGEND_PADDING_Y + 8;
        int markerX = x + CORNER_LEGEND_PADDING_X;
        int textX = markerX + CORNER_LEGEND_MARKER_W + CORNER_LEGEND_GAP;
        int maxChars = Math.max(0, (x + width - CORNER_LEGEND_PADDING_X - textX) / 6);
        for (SeriesData item : visible) {
            int markerY = rowY - CORNER_LEGEND_MARKER_H / 2 - 2;
            if (lineMarker) {
                svg.append("<line x1=\"")
                    .append(markerX)
                    .append("\" y1=\"")
                    .append(markerY + CORNER_LEGEND_MARKER_H / 2)
                    .append("\" x2=\"")
                    .append(markerX + CORNER_LEGEND_MARKER_W)
                    .append("\" y2=\"")
                    .append(markerY + CORNER_LEGEND_MARKER_H / 2)
                    .append("\" stroke=\"")
                    .append(argbToRgba(item.color))
                    .append("\" stroke-width=\"1.5\"/>");
            } else {
                svg.append("<rect x=\"")
                    .append(markerX + 2)
                    .append("\" y=\"")
                    .append(markerY)
                    .append("\" width=\"")
                    .append(CORNER_LEGEND_MARKER_H)
                    .append("\" height=\"")
                    .append(CORNER_LEGEND_MARKER_H)
                    .append("\" fill=\"")
                    .append(argbToRgba(item.color))
                    .append("\"/>");
            }
            svg.append("<text x=\"")
                .append(textX)
                .append("\" y=\"")
                .append(rowY)
                .append("\" font-size=\"9\" fill=\"#FFFFFF\" font-family=\"inherit\">")
                .append(esc(ellipsize(item.name, maxChars)))
                .append("</text>");
            rowY += CORNER_LEGEND_ROW_H;
        }
    }

    private static void flushPolyline(StringBuilder svg, StringBuilder pts, String stroke) {
        flushPolyline(svg, pts, stroke, null);
    }

    private static void flushPolyline(StringBuilder svg, StringBuilder pts, String stroke, @Nullable String tip) {
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
     * "Foo: 12" or "Q1 - Foo: 12".
     */
    private static String buildChartTip(String category, String series, double value) {
        StringBuilder sb = new StringBuilder();
        if (category != null && !category.isEmpty()) {
            sb.append(category);
        }
        if (series != null && !series.isEmpty()) {
            if (sb.length() > 0) {
                sb.append(" - ");
            }
            sb.append(series);
        }
        if (sb.length() > 0) {
            sb.append(": ");
        }
        sb.append(formatNum(value));
        return sb.toString();
    }

    // Shared SVG helpers.

    private static StringBuilder openSvg(int w, int h, String cls, int bgColor, int borderColor) {
        StringBuilder svg = new StringBuilder();
        svg.append("<div class=\"guide-svg-wrap\"><svg class=\"")
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

    private static String ellipsize(String text, int maxChars) {
        if (text == null || text.isEmpty() || maxChars <= 0) {
            return "";
        }
        if (text.length() <= maxChars) {
            return text;
        }
        if (maxChars <= 3) {
            return text.substring(0, maxChars);
        }
        return text.substring(0, maxChars - 3) + "...";
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    // Number and coordinate formatters.

    /** Format a number for SVG axis labels: integer if whole, else limited decimals. */
    public static String formatNum(double v) {
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
