package com.hfstudio.guidenh.guide.compiler.tags.functiongraph;

import com.hfstudio.guidenh.guide.compiler.tags.chart.ChartAttrParser;
import com.hfstudio.guidenh.guide.document.block.functiongraph.DomainPredicate;
import com.hfstudio.guidenh.guide.document.block.functiongraph.FunctionExpr;
import com.hfstudio.guidenh.guide.document.block.functiongraph.FunctionExprParser;
import com.hfstudio.guidenh.guide.document.block.functiongraph.FunctionGraphPalette;
import com.hfstudio.guidenh.guide.document.block.functiongraph.FunctionPlot;
import com.hfstudio.guidenh.guide.document.block.functiongraph.LytFunctionGraph;
import com.hfstudio.guidenh.guide.document.block.functiongraph.MarkedPoint;

/**
 * Parses the body of a {@code ```funcgraph} fenced code block. The first non-blank line is treated
 * as the panel header (key=value pairs separated by whitespace). Each subsequent non-blank line is
 * either a function expression with optional pipe-delimited attributes
 * ({@code sin(x) | color=#ff5566 domain=-pi..pi}), or a marked point line beginning with {@code :}
 * (explicit) or {@code @} (plot reference).
 *
 * <p>
 * Comments start with {@code #} and run to end of line.
 */
public final class FunctionGraphFenceParser {

    private FunctionGraphFenceParser() {}

    public static LytFunctionGraph parse(String source) {
        LytFunctionGraph graph = new LytFunctionGraph();
        if (source == null) {
            return graph;
        }
        String[] lines = source.split("\\r?\\n");
        boolean headerParsed = false;
        int plotIndex = 0;
        for (String rawLine : lines) {
            String line = stripComment(rawLine).trim();
            if (line.isEmpty()) {
                continue;
            }
            if (!headerParsed && looksLikeHeader(line)) {
                applyHeader(graph, line);
                headerParsed = true;
                continue;
            }
            headerParsed = true;
            if (line.charAt(0) == ':') {
                MarkedPoint point = parseExplicitPoint(line.substring(1));
                if (point != null) {
                    graph.addPoint(point);
                }
                continue;
            }
            if (line.charAt(0) == '@') {
                MarkedPoint point = parsePlotPoint(line.substring(1));
                if (point != null) {
                    graph.addPoint(point);
                }
                continue;
            }
            FunctionPlot plot = parsePlotLine(line, plotIndex);
            if (plot != null) {
                graph.addPlot(plot);
                plotIndex++;
            }
        }
        return graph;
    }

    private static boolean looksLikeHeader(String line) {
        // The first line is a header when it is a plain key=value sequence with no leading
        // expression text. We detect the common keys explicitly to avoid mistaking a single bare
        // function expression for a header.
        for (String key : HEADER_KEYS) {
            if (line.startsWith(key + "=") || line.startsWith(key + " =")) {
                return true;
            }
            if (line.indexOf(' ' + key + '=') >= 0) {
                return true;
            }
        }
        return false;
    }

    private static final String[] HEADER_KEYS = { "width", "height", "title", "background", "border", "axisColor",
        "gridColor", "showGrid", "showAxes", "xMin", "xMax", "yMin", "yMax", "xRange", "yRange", "xStep", "yStep",
        "quadrants" };

    private static void applyHeader(LytFunctionGraph graph, String line) {
        AttrMap attrs = parseKeyValues(line);
        Integer width = attrs.intValue("width");
        Integer height = attrs.intValue("height");
        if (width != null || height != null) {
            graph.setExplicitSize(width != null ? width : -1, height != null ? height : -1);
        }
        String title = attrs.stringValue("title");
        if (title != null) {
            graph.setTitle(title);
        }
        String bg = attrs.stringValue("background");
        if (bg != null) {
            graph.setBackgroundColor(ChartAttrParser.parseColor(bg, 0xFF1B1F23));
        }
        String border = attrs.stringValue("border");
        if (border != null) {
            graph.setBorderColor(ChartAttrParser.parseColor(border, 0xFF3A4047));
        }
        String axisColor = attrs.stringValue("axisColor");
        if (axisColor != null) {
            graph.setAxisColor(ChartAttrParser.parseColor(axisColor, 0xFFB8C2CF));
        }
        String gridColor = attrs.stringValue("gridColor");
        if (gridColor != null) {
            graph.setGridColor(ChartAttrParser.parseColor(gridColor, 0x33B8C2CF));
        }
        Boolean showGrid = attrs.boolValue("showGrid");
        if (showGrid != null) {
            graph.setShowGrid(showGrid);
        }
        Boolean showAxes = attrs.boolValue("showAxes");
        if (showAxes != null) {
            graph.setShowAxes(showAxes);
        }
        applyRange(graph, attrs);
        String quadrants = attrs.stringValue("quadrants");
        if (quadrants != null) {
            graph.setQuadrantMask(FunctionGraphAttrs.parseQuadrantMask(quadrants));
        }
    }

    private static void applyRange(LytFunctionGraph graph, AttrMap attrs) {
        String xRange = attrs.stringValue("xRange");
        if (xRange != null) {
            double[] r = FunctionGraphAttrs.parseRange(xRange);
            graph.setExplicitXRange(r[0], r[1]);
        } else {
            double xMin = FunctionGraphAttrs.parseDouble(attrs.stringValue("xMin"), Double.NaN);
            double xMax = FunctionGraphAttrs.parseDouble(attrs.stringValue("xMax"), Double.NaN);
            if (!Double.isNaN(xMin) || !Double.isNaN(xMax)) {
                graph.setExplicitXRange(xMin, xMax);
            }
        }
        String yRange = attrs.stringValue("yRange");
        if (yRange != null) {
            double[] r = FunctionGraphAttrs.parseRange(yRange);
            graph.setExplicitYRange(r[0], r[1]);
        } else {
            double yMin = FunctionGraphAttrs.parseDouble(attrs.stringValue("yMin"), Double.NaN);
            double yMax = FunctionGraphAttrs.parseDouble(attrs.stringValue("yMax"), Double.NaN);
            if (!Double.isNaN(yMin) || !Double.isNaN(yMax)) {
                graph.setExplicitYRange(yMin, yMax);
            }
        }
        double xStep = FunctionGraphAttrs.parseDouble(attrs.stringValue("xStep"), Double.NaN);
        if (!Double.isNaN(xStep)) {
            graph.setExplicitXStep(xStep);
        }
        double yStep = FunctionGraphAttrs.parseDouble(attrs.stringValue("yStep"), Double.NaN);
        if (!Double.isNaN(yStep)) {
            graph.setExplicitYStep(yStep);
        }
    }

    private static FunctionPlot parsePlotLine(String line, int paletteIndex) {
        // Split off pipe-prefixed attributes; the first segment is the expression.
        int pipe = line.indexOf('|');
        // Treat '|...|' as absolute value, not as an attribute separator. Detect that by checking
        // for a matching closing pipe before any '=' sign.
        if (pipe >= 0 && lineHasMatchingAbsBars(line)) {
            pipe = -1;
        }
        String exprText = (pipe >= 0 ? line.substring(0, pipe) : line).trim();
        if (exprText.isEmpty()) {
            return null;
        }
        AttrMap attrs = pipe >= 0 ? parseKeyValues(line.substring(pipe + 1)) : new AttrMap();
        Boolean inverse = attrs.boolValue("inverse");
        boolean inv = inverse != null && inverse;
        FunctionExpr ast = FunctionExprParser.parse(exprText, inv ? 1 : 0);
        DomainPredicate domain = DomainPredicate.parse(attrs.stringValue("domain"));
        String colorStr = attrs.stringValue("color");
        int color = colorStr != null ? ChartAttrParser.parseColor(colorStr, FunctionGraphPalette.color(paletteIndex))
            : FunctionGraphPalette.color(paletteIndex);
        String label = attrs.stringValue("label");
        return new FunctionPlot(exprText, ast, inv, domain, color, label);
    }

    private static boolean lineHasMatchingAbsBars(String line) {
        // A '|...|' literal is recognised only when there is no '=' separator before the closing
        // pipe; the simple heuristic below handles the common cases.
        int firstPipe = line.indexOf('|');
        if (firstPipe < 0) {
            return false;
        }
        int secondPipe = line.indexOf('|', firstPipe + 1);
        if (secondPipe < 0) {
            return false;
        }
        for (int i = firstPipe + 1; i < secondPipe; i++) {
            if (line.charAt(i) == '=') {
                return false;
            }
        }
        return true;
    }

    private static MarkedPoint parseExplicitPoint(String body) {
        // Form: x,y[ key=value...]
        String trimmed = body.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        int sp = firstWhitespace(trimmed);
        String coord = sp < 0 ? trimmed : trimmed.substring(0, sp);
        AttrMap attrs = sp < 0 ? new AttrMap() : parseKeyValues(trimmed.substring(sp + 1));
        int comma = coord.indexOf(',');
        if (comma < 0) {
            return null;
        }
        double x = FunctionGraphAttrs.parseDouble(
            coord.substring(0, comma)
                .trim(),
            Double.NaN);
        double y = FunctionGraphAttrs.parseDouble(
            coord.substring(comma + 1)
                .trim(),
            Double.NaN);
        if (Double.isNaN(x) || Double.isNaN(y)) {
            return null;
        }
        String colorStr = attrs.stringValue("color");
        int color = colorStr != null ? ChartAttrParser.parseColor(colorStr, 0xFFFFFFFF) : 0xFFFFFFFF;
        return new MarkedPoint(MarkedPoint.MODE_EXPLICIT, -1, x, y, color, false, attrs.stringValue("label"));
    }

    private static MarkedPoint parsePlotPoint(String body) {
        // Form: plot=N (atX=v|atY=v) [color=...] [label=...]
        AttrMap attrs = parseKeyValues(body);
        Integer plotIdx = attrs.intValue("plot");
        if (plotIdx == null || plotIdx < 0) {
            return null;
        }
        String colorStr = attrs.stringValue("color");
        boolean inherit = colorStr == null;
        int color = colorStr != null ? ChartAttrParser.parseColor(colorStr, 0xFFFFFFFF) : 0xFFFFFFFF;
        String label = attrs.stringValue("label");
        double atX = FunctionGraphAttrs.parseDouble(attrs.stringValue("atX"), Double.NaN);
        if (!Double.isNaN(atX)) {
            return new MarkedPoint(MarkedPoint.MODE_PLOT_AT_X, plotIdx, atX, 0d, color, inherit, label);
        }
        double atY = FunctionGraphAttrs.parseDouble(attrs.stringValue("atY"), Double.NaN);
        if (!Double.isNaN(atY)) {
            return new MarkedPoint(MarkedPoint.MODE_PLOT_AT_Y, plotIdx, atY, 0d, color, inherit, label);
        }
        return null;
    }

    private static int firstWhitespace(String s) {
        for (int i = 0; i < s.length(); i++) {
            if (Character.isWhitespace(s.charAt(i))) {
                return i;
            }
        }
        return -1;
    }

    private static String stripComment(String line) {
        int hash = line.indexOf('#');
        if (hash < 0) {
            return line;
        }
        // Allow '#' inside hex colour literals; skip when it sits inside a key=value pair next to '='.
        if (hash > 0 && line.charAt(hash - 1) == '=') {
            return line;
        }
        return line.substring(0, hash);
    }

    /**
     * Parse a whitespace-separated sequence of {@code key=value} tokens, supporting
     * {@code key="quoted value"} and bare keys (interpreted as {@code key=true}).
     */
    private static AttrMap parseKeyValues(String text) {
        AttrMap map = new AttrMap();
        if (text == null) {
            return map;
        }
        int i = 0;
        int n = text.length();
        while (i < n) {
            while (i < n && Character.isWhitespace(text.charAt(i))) {
                i++;
            }
            if (i >= n) {
                break;
            }
            int keyStart = i;
            while (i < n && !Character.isWhitespace(text.charAt(i)) && text.charAt(i) != '=') {
                i++;
            }
            String key = text.substring(keyStart, i);
            if (key.isEmpty()) {
                i++;
                continue;
            }
            String value = "true";
            if (i < n && text.charAt(i) == '=') {
                i++;
                if (i < n && (text.charAt(i) == '"' || text.charAt(i) == '\'')) {
                    char quote = text.charAt(i);
                    i++;
                    int valStart = i;
                    while (i < n && text.charAt(i) != quote) {
                        i++;
                    }
                    value = text.substring(valStart, i);
                    if (i < n) {
                        i++;
                    }
                } else {
                    int valStart = i;
                    while (i < n && !Character.isWhitespace(text.charAt(i))) {
                        i++;
                    }
                    value = text.substring(valStart, i);
                }
            }
            map.put(key, value);
        }
        return map;
    }

    /** Tiny case-sensitive map wrapper that exposes typed accessors. */
    private static final class AttrMap {

        private final java.util.Map<String, String> data = new java.util.LinkedHashMap<>();

        public void put(String key, String value) {
            data.put(key, value);
        }

        public String stringValue(String key) {
            return data.get(key);
        }

        public Integer intValue(String key) {
            String s = data.get(key);
            if (s == null) {
                return null;
            }
            try {
                return Integer.parseInt(s.trim());
            } catch (NumberFormatException ex) {
                return null;
            }
        }

        public Boolean boolValue(String key) {
            String s = data.get(key);
            if (s == null) {
                return null;
            }
            String t = s.trim()
                .toLowerCase();
            if (t.equals("true") || t.equals("yes") || t.equals("on") || t.equals("1")) {
                return Boolean.TRUE;
            }
            if (t.equals("false") || t.equals("no") || t.equals("off") || t.equals("0")) {
                return Boolean.FALSE;
            }
            return null;
        }
    }
}
