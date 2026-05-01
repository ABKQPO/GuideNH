package com.hfstudio.guidenh.guide.document.block.chart;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.item.ItemStack;

import com.hfstudio.guidenh.guide.document.LytRect;
import com.hfstudio.guidenh.guide.render.RenderContext;
import com.hfstudio.guidenh.guide.style.ResolvedTextStyle;

/**
 * Horizontal bar chart (X numeric, Y categorical).
 *
 * <p>
 * Optional combo extensions: {@link #setLineOverlays} adds line series drawn over the bars sharing
 * the same X axis (each line point sits at the cluster center of its category); {@link #setPieInset}
 * draws a small pie chart in a corner of the plot area.
 */
public class LytBarChart extends LytChartBase {

    private static final int LINE_THICKNESS = 1;
    private static final int LINE_POINT_RADIUS = 2;
    /** High bit marker so line-overlay keys do not collide with bar keys ((si<<16)|ci). */
    private static final int LINE_KEY_FLAG = 0x40000000;
    /** Extra horizontal space reserved on the chart's right side when pie inset is RIGHT_OUTSIDE. */
    private static final int PIE_OUTSIDE_GAP = 6;

    private List<ChartSeries> series = new ArrayList<>();
    private List<ChartSeries> lineOverlays = new ArrayList<>();
    private PieInsetSpec pieInset;
    private String[] categories = new String[0];
    private ChartAxisOptions xAxis = new ChartAxisOptions();
    private ChartAxisOptions yAxis = new ChartAxisOptions();
    private float barWidthRatio = 0.7f;

    private LytRect plotCache = LytRect.empty();
    private AxisRange xRangeCache;

    public void setSeries(List<ChartSeries> series) {
        this.series = series != null ? series : new ArrayList<>();
    }

    public List<ChartSeries> getSeries() {
        return series;
    }

    public void setLineOverlays(List<ChartSeries> overlays) {
        this.lineOverlays = overlays != null ? overlays : new ArrayList<>();
    }

    public List<ChartSeries> getLineOverlays() {
        return lineOverlays;
    }

    public void setPieInset(PieInsetSpec pieInset) {
        this.pieInset = pieInset;
    }

    public PieInsetSpec getPieInset() {
        return pieInset;
    }

    public void setCategories(String[] categories) {
        this.categories = categories != null ? categories : new String[0];
    }

    public void setXAxis(ChartAxisOptions xAxis) {
        if (xAxis != null) this.xAxis = xAxis;
    }

    public void setYAxis(ChartAxisOptions yAxis) {
        if (yAxis != null) this.yAxis = yAxis;
    }

    public void setBarWidthRatio(float r) {
        if (r > 0f && r <= 1f) this.barWidthRatio = r;
    }

    @Override
    protected int getExtraPlotWidth() {
        if (pieInset != null && pieInset.getPosition() == PieInsetSpec.Position.RIGHT_OUTSIDE) {
            return pieInset.getSize() + PIE_OUTSIDE_GAP;
        }
        return 0;
    }

    @Override
    protected List<ChartLegendRenderer.LegendEntry> collectLegendEntries() {
        List<ChartLegendRenderer.LegendEntry> entries = new ArrayList<>();
        for (ChartSeries s : series) {
            entries.add(new ChartLegendRenderer.LegendEntry(s.getName(), s.getColor(), s.getIcon()));
        }
        for (ChartSeries s : lineOverlays) {
            entries.add(new ChartLegendRenderer.LegendEntry(s.getName(), s.getColor(), s.getIcon()));
        }
        return entries;
    }

    @Override
    protected void renderChart(RenderContext context, LytRect plotRect) {
        int categoryCount = Math.max(categories.length, maxSeriesLength());
        if (categoryCount == 0 || (series.isEmpty() && lineOverlays.isEmpty())) return;
        // Peel off a dedicated right-hand area for the pie inset when configured.
        LytRect pieArea = null;
        if (pieInset != null && pieInset.getPosition() == PieInsetSpec.Position.RIGHT_OUTSIDE) {
            int extra = pieInset.getSize() + PIE_OUTSIDE_GAP;
            if (plotRect.width() > extra + 32) {
                pieArea = new LytRect(
                    plotRect.right() - pieInset.getSize(),
                    plotRect.y(),
                    pieInset.getSize(),
                    plotRect.height());
                plotRect = new LytRect(plotRect.x(), plotRect.y(), plotRect.width() - extra, plotRect.height());
            }
        }
        double dMin = 0d;
        double dMax = 0d;
        for (ChartSeries s : series) {
            for (double v : s.getYs()) {
                if (v < dMin) dMin = v;
                if (v > dMax) dMax = v;
            }
        }
        for (ChartSeries s : lineOverlays) {
            for (double v : s.getYs()) {
                if (v < dMin) dMin = v;
                if (v > dMax) dMax = v;
            }
        }
        AxisRange xRange = AxisRange
            .compute(xAxis.getMin(), xAxis.getMax(), xAxis.getStep(), Math.min(0d, dMin), Math.max(0d, dMax));
        xRangeCache = xRange;

        // Estimate left-side (category) and bottom (value tick) insets.
        ResolvedTextStyle style = textStyle(0xFFCCCCCC);
        int lh = context.getLineHeight(style);
        int leftInset = 4;
        for (int i = 0; i < categoryCount; i++) {
            String c = i < categories.length ? categories[i] : Integer.toString(i + 1);
            int w = context.getStringWidth(c, style);
            if (w > leftInset) leftInset = w;
        }
        leftInset += 6;
        int bottomInset = lh + 4;
        if (xAxis.getLabel() != null && !xAxis.getLabel()
            .isEmpty()) {
            bottomInset += lh + 2;
        }
        LytRect inner = plotRect.shrink(leftInset, 4, 4, bottomInset);
        plotCache = inner;
        if (inner.width() <= 4 || inner.height() <= 4) return;

        // Grid (vertical lines correspond to X values).
        for (double t = xRange.min; t <= xRange.max + 1e-9; t += xRange.step) {
            float gx = CartesianChartRenderer.mapX(t, xRange, inner);
            if (xAxis.isGridVisible()) {
                context.drawLine(gx, inner.y(), gx, inner.bottom(), 1f, xAxis.getGridColor());
            }
            String s = xAxis.formatTick(t);
            int sw = context.getStringWidth(s, style);
            context.drawText(s, (int) gx - sw / 2, inner.bottom() + 3, style);
        }
        if (xAxis.getLabel() != null && !xAxis.getLabel()
            .isEmpty()) {
            int sw = context.getStringWidth(xAxis.getLabel(), style);
            context
                .drawText(xAxis.getLabel(), inner.x() + (inner.width() - sw) / 2, inner.bottom() + 3 + lh + 2, style);
        }

        // Category ticks.
        float categoryHeight = (float) inner.height() / categoryCount;
        for (int i = 0; i < categoryCount; i++) {
            String c = i < categories.length ? categories[i] : Integer.toString(i + 1);
            int sw = context.getStringWidth(c, style);
            float cy = inner.y() + categoryHeight * (i + 0.5f);
            context.drawText(c, inner.x() - sw - 4, (int) cy - lh / 2, style);
        }

        // Border.
        context.drawLine(inner.x(), inner.y(), inner.x(), inner.bottom(), 1f, xAxis.getAxisColor());
        context.drawLine(inner.x(), inner.bottom(), inner.right(), inner.bottom(), 1f, xAxis.getAxisColor());

        int seriesCount = series.size();
        float categoryHeightForCluster = categoryHeight;
        float baselineX = CartesianChartRenderer.mapX(0d, xRange, inner);
        ResolvedTextStyle valueStyle = textStyle(getLabelColor());
        if (seriesCount > 0) {
            float clusterHeight = categoryHeightForCluster * barWidthRatio;
            float barHeight = clusterHeight / seriesCount;
            for (int ci = 0; ci < categoryCount; ci++) {
                float clusterCenter = inner.y() + categoryHeightForCluster * (ci + 0.5f);
                float clusterTop = clusterCenter - clusterHeight / 2f;
                for (int si = 0; si < seriesCount; si++) {
                    ChartSeries s = series.get(si);
                    if (ci >= s.getYs().length) continue;
                    double v = s.getYs()[ci];
                    float endX = CartesianChartRenderer.mapX(v, xRange, inner);
                    float y0 = clusterTop + barHeight * si;
                    float y1 = y0 + barHeight - 0.5f;
                    int key = encodeKey(si, ci);
                    boolean hovered = key == hoveredKey;
                    float xLeft = Math.min(endX, baselineX);
                    float xRight = Math.max(endX, baselineX);
                    if (hovered) {
                        xRight += 2f;
                    }
                    LytRect bar = new LytRect(
                        (int) xLeft,
                        (int) y0,
                        Math.max(1, (int) (xRight - xLeft)),
                        Math.max(1, (int) (y1 - y0)));
                    context.fillRect(bar, s.getColor());
                    if (hovered) {
                        context.drawBorder(bar, 0xFF000000, 1);
                    }
                    drawValueLabel(context, valueStyle, v, bar, endX);
                }
            }
        }

        // Line overlays: each point sits at (mapX(value), categoryCenterY) so the line traces
        // category by category along the vertical category axis.
        if (!lineOverlays.isEmpty()) {
            int hoveredLineSeries = isLineKey(hoveredKey) ? decodeSeries(hoveredKey & ~LINE_KEY_FLAG) : -1;
            int hoveredLinePoint = isLineKey(hoveredKey) ? decodeCategory(hoveredKey & ~LINE_KEY_FLAG) : -1;
            for (int li = 0; li < lineOverlays.size(); li++) {
                ChartSeries s = lineOverlays.get(li);
                int n = s.getYs().length;
                if (n == 0) continue;
                float[] px = new float[n];
                float[] py = new float[n];
                for (int i = 0; i < n; i++) {
                    px[i] = CartesianChartRenderer.mapX(s.getYs()[i], xRange, inner);
                    py[i] = inner.y() + categoryHeightForCluster * (i + 0.5f);
                }
                for (int i = 0; i + 1 < n; i++) {
                    float thick = LINE_THICKNESS + 1f;
                    if (hoveredLineSeries == li && (hoveredLinePoint == i || hoveredLinePoint == i + 1)) {
                        thick += 1f;
                    }
                    context.drawLine(px[i], py[i], px[i + 1], py[i + 1], thick, s.getColor());
                }
                for (int i = 0; i < n; i++) {
                    boolean ph = hoveredLineSeries == li && hoveredLinePoint == i;
                    float r = ph ? LINE_POINT_RADIUS + 2f : LINE_POINT_RADIUS;
                    context.fillCircle(px[i], py[i], r, s.getColor());
                    if (ph) {
                        context.drawCircleOutline(px[i], py[i], r, 1f, 0xFF000000);
                    }
                }
            }
        }

        if (pieArea != null) {
            PieInsetRenderer.drawAt(context, pieArea, pieInset);
        } else {
            PieInsetRenderer.draw(context, inner, pieInset);
        }
    }

    private void drawValueLabel(RenderContext context, ResolvedTextStyle style, double value, LytRect bar, float endX) {
        if (getLabelPosition() == ChartLabelPosition.NONE) return;
        String text = formatValue(value);
        int tw = context.getStringWidth(text, style);
        int lh = context.getLineHeight(style);
        int textX;
        int textY = bar.y() + (bar.height() - lh) / 2;
        switch (getLabelPosition()) {
            case ABOVE:
                textX = bar.x() + (bar.width() - tw) / 2;
                textY = bar.y() - lh - 1;
                break;
            case BELOW:
                textX = bar.x() + (bar.width() - tw) / 2;
                textY = bar.bottom() + 1;
                break;
            case CENTER:
            case INSIDE:
                textX = bar.x() + (bar.width() - tw) / 2;
                break;
            case OUTSIDE:
                textX = value >= 0 ? (int) endX + 3 : (int) endX - tw - 3;
                break;
            default:
                return;
        }
        context.drawText(text, textX, textY, style);
    }

    private int maxSeriesLength() {
        int n = 0;
        for (ChartSeries s : series) {
            if (s.getYs().length > n) n = s.getYs().length;
        }
        return n;
    }

    private static int encodeKey(int seriesIdx, int categoryIdx) {
        return (seriesIdx & 0xFFFF) << 16 | (categoryIdx & 0xFFFF);
    }

    private static int encodeLineKey(int lineIdx, int pointIdx) {
        return LINE_KEY_FLAG | ((lineIdx & 0xFFFF) << 16) | (pointIdx & 0xFFFF);
    }

    private static boolean isLineKey(int key) {
        return key >= 0 && (key & LINE_KEY_FLAG) != 0;
    }

    private static int decodeSeries(int key) {
        return (key >>> 16) & 0xFFFF;
    }

    private static int decodeCategory(int key) {
        return key & 0xFFFF;
    }

    @Override
    protected int hitTest(float x, float y) {
        if (plotCache.isEmpty() || xRangeCache == null) return -1;
        if (!plotCache.contains((int) x, (int) y)) return -1;
        int categoryCount = Math.max(categories.length, maxSeriesLength());
        if (categoryCount == 0) return -1;
        float categoryHeight = (float) plotCache.height() / categoryCount;
        // Test line overlay points first so they take priority over bars beneath.
        if (!lineOverlays.isEmpty()) {
            float threshold = (LINE_POINT_RADIUS + 3f) * (LINE_POINT_RADIUS + 3f);
            float bestDist = threshold;
            int bestKey = -1;
            for (int li = 0; li < lineOverlays.size(); li++) {
                ChartSeries s = lineOverlays.get(li);
                int n = s.getYs().length;
                for (int i = 0; i < n; i++) {
                    float px = CartesianChartRenderer.mapX(s.getYs()[i], xRangeCache, plotCache);
                    float py = plotCache.y() + categoryHeight * (i + 0.5f);
                    float dx = x - px;
                    float dy = y - py;
                    float d = dx * dx + dy * dy;
                    if (d < bestDist) {
                        bestDist = d;
                        bestKey = encodeLineKey(li, i);
                    }
                }
            }
            if (bestKey >= 0) return bestKey;
        }
        if (series.isEmpty()) return -1;
        int seriesCount = series.size();
        float clusterHeight = categoryHeight * barWidthRatio;
        float barHeight = clusterHeight / seriesCount;
        float baselineX = CartesianChartRenderer.mapX(0d, xRangeCache, plotCache);
        for (int ci = 0; ci < categoryCount; ci++) {
            float clusterCenter = plotCache.y() + categoryHeight * (ci + 0.5f);
            float clusterTop = clusterCenter - clusterHeight / 2f;
            for (int si = 0; si < seriesCount; si++) {
                ChartSeries s = series.get(si);
                if (ci >= s.getYs().length) continue;
                double v = s.getYs()[ci];
                float endX = CartesianChartRenderer.mapX(v, xRangeCache, plotCache);
                float y0 = clusterTop + barHeight * si;
                float y1 = y0 + barHeight;
                float xLeft = Math.min(endX, baselineX);
                float xRight = Math.max(endX, baselineX);
                if (x >= xLeft && x <= xRight && y >= y0 && y <= y1) {
                    return encodeKey(si, ci);
                }
            }
        }
        return -1;
    }

    @Override
    protected String describeHit(int key) {
        if (isLineKey(key)) {
            int li = decodeSeries(key & ~LINE_KEY_FLAG);
            int pi = decodeCategory(key & ~LINE_KEY_FLAG);
            if (li >= lineOverlays.size()) return null;
            ChartSeries s = lineOverlays.get(li);
            if (pi >= s.getYs().length) return null;
            String cat = pi < categories.length ? categories[pi] : Integer.toString(pi + 1);
            StringBuilder sb = new StringBuilder();
            if (!s.getName()
                .isEmpty()) {
                sb.append(s.getName())
                    .append('\n');
            }
            sb.append(cat)
                .append(": ")
                .append(formatValue(s.getYs()[pi]));
            return sb.toString();
        }
        int si = decodeSeries(key);
        int ci = decodeCategory(key);
        if (si >= series.size()) return null;
        ChartSeries s = series.get(si);
        if (ci >= s.getYs().length) return null;
        double v = s.getYs()[ci];
        double sum = 0d;
        for (ChartSeries x : series) {
            if (ci < x.getYs().length) sum += Math.abs(x.getYs()[ci]);
        }
        String cat = ci < categories.length ? categories[ci] : Integer.toString(ci + 1);
        StringBuilder sb = new StringBuilder();
        if (!s.getName()
            .isEmpty()) {
            sb.append(s.getName())
                .append('\n');
        }
        sb.append(cat)
            .append(": ")
            .append(formatValue(v));
        if (sum > 0d) {
            sb.append('\n')
                .append(formatPercent(Math.abs(v) / sum));
        }
        return sb.toString();
    }

    @Override
    protected ItemStack getHitItemStack(int key) {
        if (isLineKey(key)) {
            int li = decodeSeries(key & ~LINE_KEY_FLAG);
            if (li < 0 || li >= lineOverlays.size()) return null;
            ChartIcon icon = lineOverlays.get(li)
                .getIcon();
            return icon != null && icon.hasItemStack() ? icon.getStack() : null;
        }
        int si = decodeSeries(key);
        if (si < 0 || si >= series.size()) return null;
        ChartIcon icon = series.get(si)
            .getIcon();
        return icon != null && icon.hasItemStack() ? icon.getStack() : null;
    }

    @Override
    protected String getHitExtraTooltip(int key) {
        if (isLineKey(key)) {
            int li = decodeSeries(key & ~LINE_KEY_FLAG);
            if (li < 0 || li >= lineOverlays.size()) return null;
            return lineOverlays.get(li)
                .getTooltipExtra();
        }
        int si = decodeSeries(key);
        if (si < 0 || si >= series.size()) return null;
        return series.get(si)
            .getTooltipExtra();
    }
}
