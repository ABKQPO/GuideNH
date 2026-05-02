package com.hfstudio.guidenh.guide.compiler.tags.functiongraph;

import com.hfstudio.guidenh.guide.compiler.PageCompiler;
import com.hfstudio.guidenh.guide.compiler.tags.MdxAttrs;
import com.hfstudio.guidenh.guide.compiler.tags.chart.ChartAttrParser;
import com.hfstudio.guidenh.guide.document.LytErrorSink;
import com.hfstudio.guidenh.guide.document.block.functiongraph.DomainPredicate;
import com.hfstudio.guidenh.guide.document.block.functiongraph.FunctionExpr;
import com.hfstudio.guidenh.guide.document.block.functiongraph.FunctionExprParser;
import com.hfstudio.guidenh.guide.document.block.functiongraph.FunctionGraphPalette;
import com.hfstudio.guidenh.guide.document.block.functiongraph.FunctionPlot;
import com.hfstudio.guidenh.guide.document.block.functiongraph.LytFunctionGraph;
import com.hfstudio.guidenh.guide.document.block.functiongraph.MarkedPoint;
import com.hfstudio.guidenh.libs.mdast.mdx.model.MdxJsxElementFields;

/**
 * Shared attribute parsing helpers for the {@code <FunctionGraph>} / {@code <Function>} /
 * {@code <Plot>} / {@code <Point>} tag families. Mirrors the conventions used by the chart attr
 * parsers so authors who already know one can reuse the same shapes.
 */
public final class FunctionGraphAttrs {

    public static final int QUADRANT_MASK_AUTO = 0;

    private FunctionGraphAttrs() {}

    /** Apply the panel-level attributes onto {@code graph}. */
    public static void applyContainerAttrs(LytFunctionGraph graph, PageCompiler compiler, LytErrorSink sink,
        MdxJsxElementFields el) {
        graph.setTitle(MdxAttrs.getString(compiler, sink, el, "title", null));
        graph.setExplicitSize(
            MdxAttrs.getInt(compiler, sink, el, "width", -1),
            MdxAttrs.getInt(compiler, sink, el, "height", -1));

        String bg = MdxAttrs.getString(compiler, sink, el, "background", null);
        if (bg != null) {
            graph.setBackgroundColor(ChartAttrParser.parseColor(bg, 0xFF1B1F23));
        }
        String border = MdxAttrs.getString(compiler, sink, el, "border", null);
        if (border != null) {
            graph.setBorderColor(ChartAttrParser.parseColor(border, 0xFF3A4047));
        }
        String axis = MdxAttrs.getString(compiler, sink, el, "axisColor", null);
        if (axis != null) {
            graph.setAxisColor(ChartAttrParser.parseColor(axis, 0xFFB8C2CF));
        }
        String grid = MdxAttrs.getString(compiler, sink, el, "gridColor", null);
        if (grid != null) {
            graph.setGridColor(ChartAttrParser.parseColor(grid, 0x33B8C2CF));
        }
        graph.setShowGrid(MdxAttrs.getBoolean(compiler, sink, el, "showGrid", true));
        graph.setShowAxes(MdxAttrs.getBoolean(compiler, sink, el, "showAxes", true));

        applyRange(graph, compiler, sink, el);
        graph.setQuadrantMask(parseQuadrantMask(MdxAttrs.getString(compiler, sink, el, "quadrants", null)));
    }

    /** Apply axis range and step attributes. */
    public static void applyRange(LytFunctionGraph graph, PageCompiler compiler, LytErrorSink sink,
        MdxJsxElementFields el) {
        String xRange = MdxAttrs.getString(compiler, sink, el, "xRange", null);
        if (xRange != null) {
            double[] r = parseRange(xRange);
            graph.setExplicitXRange(r[0], r[1]);
        } else {
            double xMin = parseDouble(MdxAttrs.getString(compiler, sink, el, "xMin", null), Double.NaN);
            double xMax = parseDouble(MdxAttrs.getString(compiler, sink, el, "xMax", null), Double.NaN);
            if (!Double.isNaN(xMin) || !Double.isNaN(xMax)) {
                graph.setExplicitXRange(xMin, xMax);
            }
        }
        String yRange = MdxAttrs.getString(compiler, sink, el, "yRange", null);
        if (yRange != null) {
            double[] r = parseRange(yRange);
            graph.setExplicitYRange(r[0], r[1]);
        } else {
            double yMin = parseDouble(MdxAttrs.getString(compiler, sink, el, "yMin", null), Double.NaN);
            double yMax = parseDouble(MdxAttrs.getString(compiler, sink, el, "yMax", null), Double.NaN);
            if (!Double.isNaN(yMin) || !Double.isNaN(yMax)) {
                graph.setExplicitYRange(yMin, yMax);
            }
        }
        double xStep = parseDouble(MdxAttrs.getString(compiler, sink, el, "xStep", null), Double.NaN);
        if (!Double.isNaN(xStep)) {
            graph.setExplicitXStep(xStep);
        }
        double yStep = parseDouble(MdxAttrs.getString(compiler, sink, el, "yStep", null), Double.NaN);
        if (!Double.isNaN(yStep)) {
            graph.setExplicitYStep(yStep);
        }
    }

    /**
     * Parse a single {@code <Plot>} child element into a {@link FunctionPlot}. Returns {@code null}
     * when no expression is provided.
     */
    public static FunctionPlot parsePlot(PageCompiler compiler, LytErrorSink sink, MdxJsxElementFields el,
        int paletteIndex) {
        String expr = MdxAttrs.getString(compiler, sink, el, "expr", null);
        if (expr == null) {
            return null;
        }
        boolean inverse = MdxAttrs.getBoolean(compiler, sink, el, "inverse", false);
        FunctionExpr ast = FunctionExprParser.parse(expr, inverse ? 1 : 0);
        DomainPredicate domain = DomainPredicate.parse(MdxAttrs.getString(compiler, sink, el, "domain", null));
        String colorStr = MdxAttrs.getString(compiler, sink, el, "color", null);
        int color = colorStr != null ? ChartAttrParser.parseColor(colorStr, FunctionGraphPalette.color(paletteIndex))
            : FunctionGraphPalette.color(paletteIndex);
        String label = MdxAttrs.getString(compiler, sink, el, "label", null);
        return new FunctionPlot(expr, ast, inverse, domain, color, label);
    }

    /** Parse a single {@code <Point>} child element into a {@link MarkedPoint}. */
    public static MarkedPoint parsePoint(PageCompiler compiler, LytErrorSink sink, MdxJsxElementFields el) {
        String colorStr = MdxAttrs.getString(compiler, sink, el, "color", null);
        boolean colorInherit = colorStr == null;
        int color = colorStr != null ? ChartAttrParser.parseColor(colorStr, 0xFFFFFFFF) : 0xFFFFFFFF;
        String label = MdxAttrs.getString(compiler, sink, el, "label", null);

        double xValue = parseDouble(MdxAttrs.getString(compiler, sink, el, "x", null), Double.NaN);
        double yValue = parseDouble(MdxAttrs.getString(compiler, sink, el, "y", null), Double.NaN);
        if (!Double.isNaN(xValue) && !Double.isNaN(yValue)) {
            return new MarkedPoint(MarkedPoint.MODE_EXPLICIT, -1, xValue, yValue, color, false, label);
        }

        int plotIndex = MdxAttrs.getInt(compiler, sink, el, "plot", -1);
        if (plotIndex >= 0) {
            double atX = parseDouble(MdxAttrs.getString(compiler, sink, el, "atX", null), Double.NaN);
            double atY = parseDouble(MdxAttrs.getString(compiler, sink, el, "atY", null), Double.NaN);
            if (!Double.isNaN(atX)) {
                return new MarkedPoint(MarkedPoint.MODE_PLOT_AT_X, plotIndex, atX, 0d, color, colorInherit, label);
            }
            if (!Double.isNaN(atY)) {
                return new MarkedPoint(MarkedPoint.MODE_PLOT_AT_Y, plotIndex, atY, 0d, color, colorInherit, label);
            }
        }
        return null;
    }

    /** Parse {@code "1,2"} style quadrant lists, with {@code "all"} selecting every quadrant. */
    public static int parseQuadrantMask(String spec) {
        if (spec == null) {
            return QUADRANT_MASK_AUTO;
        }
        String trimmed = spec.trim();
        if (trimmed.isEmpty()) {
            return QUADRANT_MASK_AUTO;
        }
        if (trimmed.equalsIgnoreCase("all")) {
            return 0xF;
        }
        int mask = 0;
        for (String raw : trimmed.split(",")) {
            String part = raw.trim();
            try {
                int q = Integer.parseInt(part);
                if (q >= 1 && q <= 4) {
                    mask |= 1 << (q - 1);
                }
            } catch (NumberFormatException ex) {
                // ignored
            }
        }
        return mask;
    }

    /** Parse {@code "min..max"}; missing endpoint becomes {@link Double#NaN}. */
    public static double[] parseRange(String spec) {
        double[] out = { Double.NaN, Double.NaN };
        if (spec == null) {
            return out;
        }
        int sep = spec.indexOf("..");
        if (sep < 0) {
            return out;
        }
        out[0] = parseDouble(
            spec.substring(0, sep)
                .trim(),
            Double.NaN);
        out[1] = parseDouble(
            spec.substring(sep + 2)
                .trim(),
            Double.NaN);
        return out;
    }

    /** Parse a numeric literal; supports the same constants as the expression parser. */
    public static double parseDouble(String text, double fallback) {
        if (text == null) {
            return fallback;
        }
        String trimmed = text.trim();
        if (trimmed.isEmpty()) {
            return fallback;
        }
        return DomainPredicate.parseNumberOrConstant(trimmed, fallback);
    }
}
