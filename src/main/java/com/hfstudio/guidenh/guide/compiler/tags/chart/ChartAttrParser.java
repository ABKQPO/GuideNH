package com.hfstudio.guidenh.guide.compiler.tags.chart;

import java.util.ArrayList;
import java.util.List;

import com.hfstudio.guidenh.guide.compiler.PageCompiler;
import com.hfstudio.guidenh.guide.compiler.tags.MdxAttrs;
import com.hfstudio.guidenh.guide.document.LytErrorSink;
import com.hfstudio.guidenh.guide.document.block.chart.ChartAxisOptions;
import com.hfstudio.guidenh.guide.document.block.chart.ChartLabelPosition;
import com.hfstudio.guidenh.guide.document.block.chart.ChartLegendPosition;
import com.hfstudio.guidenh.libs.mdast.mdx.model.MdxJsxElementFields;

/**
 * Static utility collection for parsing chart tag attributes.
 */
public final class ChartAttrParser {

    /** Default 16-color cyclic palette (opaque). */
    public static final int[] DEFAULT_PALETTE = new int[] { 0xFF4E79A7, 0xFFF28E2B, 0xFFE15759, 0xFF76B7B2, 0xFF59A14F,
        0xFFEDC948, 0xFFB07AA1, 0xFFFF9DA7, 0xFF9C755F, 0xFFBAB0AC, 0xFF1F77B4, 0xFFFF7F0E, 0xFF2CA02C, 0xFFD62728,
        0xFF9467BD, 0xFF8C564B };

    private ChartAttrParser() {}

    public static int paletteColor(int index) {
        int n = DEFAULT_PALETTE.length;
        int i = ((index % n) + n) % n;
        return DEFAULT_PALETTE[i];
    }

    /**
     * Parse a comma-separated double array, e.g. '10,20,30.5'. An empty string returns length 0.
     */
    public static double[] parseDoubleArray(String s) {
        if (s == null) {
            return new double[0];
        }
        String trimmed = s.trim();
        if (trimmed.isEmpty()) {
            return new double[0];
        }
        String[] parts = trimmed.split(",");
        List<Double> values = new ArrayList<>(parts.length);
        for (String part : parts) {
            String p = part.trim();
            if (p.isEmpty()) {
                continue;
            }
            try {
                values.add(Double.parseDouble(p));
            } catch (NumberFormatException ex) {
                // Skip unparsable item
            }
        }
        double[] out = new double[values.size()];
        for (int i = 0; i < out.length; i++) {
            out[i] = values.get(i);
        }
        return out;
    }

    /**
     * Parse a comma-separated string array.
     */
    public static String[] parseStringArray(String s) {
        if (s == null) {
            return new String[0];
        }
        String trimmed = s.trim();
        if (trimmed.isEmpty()) {
            return new String[0];
        }
        String[] parts = trimmed.split(",");
        String[] out = new String[parts.length];
        for (int i = 0; i < parts.length; i++) {
            out[i] = parts[i].trim();
        }
        return out;
    }

    /**
     * Parse a point sequence in 'x:y,x:y,...' form.
     */
    public static double[][] parsePointArray(String s) {
        if (s == null || s.trim()
            .isEmpty()) {
            return new double[][] { new double[0], new double[0] };
        }
        String[] parts = s.trim()
            .split(",");
        List<Double> xs = new ArrayList<>(parts.length);
        List<Double> ys = new ArrayList<>(parts.length);
        for (String part : parts) {
            String p = part.trim();
            int colon = p.indexOf(':');
            if (colon <= 0 || colon == p.length() - 1) {
                continue;
            }
            try {
                double x = Double.parseDouble(
                    p.substring(0, colon)
                        .trim());
                double y = Double.parseDouble(
                    p.substring(colon + 1)
                        .trim());
                xs.add(x);
                ys.add(y);
            } catch (NumberFormatException ex) {
                // Skip
            }
        }
        double[] xa = new double[xs.size()];
        double[] ya = new double[ys.size()];
        for (int i = 0; i < xa.length; i++) {
            xa[i] = xs.get(i);
            ya[i] = ys.get(i);
        }
        return new double[][] { xa, ya };
    }

    /**
     * Parse a color string. Supports {@code #RGB} / {@code #RRGGBB} / {@code #AARRGGBB}.
     * Returns {@code def} on failure.
     */
    public static int parseColor(String s, int def) {
        if (s == null) {
            return def;
        }
        String t = s.trim();
        if (t.isEmpty()) {
            return def;
        }
        if (t.charAt(0) == '#') {
            t = t.substring(1);
        } else if (t.startsWith("0x") || t.startsWith("0X")) {
            t = t.substring(2);
        }
        try {
            switch (t.length()) {
                case 3: {
                    int r = Integer.parseInt(t.substring(0, 1), 16) * 17;
                    int g = Integer.parseInt(t.substring(1, 2), 16) * 17;
                    int b = Integer.parseInt(t.substring(2, 3), 16) * 17;
                    return 0xFF000000 | (r << 16) | (g << 8) | b;
                }
                case 6:
                    return 0xFF000000 | Integer.parseInt(t, 16);
                case 8:
                    return (int) Long.parseLong(t, 16);
                default:
                    return def;
            }
        } catch (NumberFormatException ex) {
            return def;
        }
    }

    public static ChartLegendPosition parseLegendPosition(String s, ChartLegendPosition def) {
        return ChartLegendPosition.fromString(s, def);
    }

    public static ChartLabelPosition parseLabelPosition(String s, ChartLabelPosition def) {
        return ChartLabelPosition.fromString(s, def);
    }

    /**
     * Parse a boxed {@link Double}; null means "auto".
     */
    public static Double parseBoxedDouble(MdxJsxElementFields el, String name) {
        String s = MdxAttrs.getString(el, name, null);
        if (s == null) {
            return null;
        }
        String t = s.trim();
        if (t.isEmpty() || "auto".equalsIgnoreCase(t)) {
            return null;
        }
        try {
            return Double.parseDouble(t);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    /**
     * Read the set of axis attributes prefixed with {@code prefix}. For example, prefix="xAxis" reads
     * {@code xAxisLabel} / {@code xAxisMin} / {@code xAxisMax} / {@code xAxisStep} /
     * {@code xAxisUnit} / {@code xAxisTickFormat} / {@code showXGrid} / {@code xGridColor}.
     */
    public static ChartAxisOptions parseAxisOptions(PageCompiler compiler, LytErrorSink errorSink,
        MdxJsxElementFields el, String prefix, String gridFlagAttr, String gridColorAttr) {
        ChartAxisOptions opts = new ChartAxisOptions();
        opts.setLabel(MdxAttrs.getString(compiler, errorSink, el, prefix + "Label", null));
        opts.setMin(parseBoxedDouble(el, prefix + "Min"));
        opts.setMax(parseBoxedDouble(el, prefix + "Max"));
        opts.setStep(parseBoxedDouble(el, prefix + "Step"));
        opts.setUnit(MdxAttrs.getString(compiler, errorSink, el, prefix + "Unit", null));
        opts.setTickFormat(MdxAttrs.getString(compiler, errorSink, el, prefix + "TickFormat", null));
        opts.setGridVisible(MdxAttrs.getBoolean(compiler, errorSink, el, gridFlagAttr, true));
        String gridColor = MdxAttrs.getString(compiler, errorSink, el, gridColorAttr, null);
        if (gridColor != null) {
            opts.setGridColor(parseColor(gridColor, opts.getGridColor()));
        }
        return opts;
    }
}
