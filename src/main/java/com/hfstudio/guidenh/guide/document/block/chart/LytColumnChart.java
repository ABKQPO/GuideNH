package com.hfstudio.guidenh.guide.document.block.chart;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.item.ItemStack;

import com.hfstudio.guidenh.guide.document.LytRect;
import com.hfstudio.guidenh.guide.render.RenderContext;
import com.hfstudio.guidenh.guide.style.ResolvedTextStyle;

/**
 * Clustered column chart. The X axis is categorical (see {@link #setCategories}); the Y axis is numeric.
 * Multiple {@code <Series>} children form adjacent bar clusters per category.
 *
 * <p>
 * Optional combo extensions: {@link #setLineOverlays} adds line series drawn over the bars sharing the
 * same Y axis; {@link #setPieInset} draws a small pie chart in a corner of the plot area for summary share.
 */
public class LytColumnChart extends LytChartBase {

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
    private AxisRange yRangeCache;

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
        if (categoryCount == 0 || (series.isEmpty() && lineOverlays.isEmpty())) {
            return;
        }
        // If the pie inset uses RIGHT_OUTSIDE, peel off a dedicated right-hand area for it so the
        // columns/lines do not have to share space with the pie.
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
        // Compute combined data range across columns and line overlays so both share the Y axis.
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
        AxisRange yRange = AxisRange
            .compute(yAxis.getMin(), yAxis.getMax(), yAxis.getStep(), Math.min(0d, dMin), Math.max(0d, dMax));
        yRangeCache = yRange;

        // Reserve space along the left/bottom for axis labels and tick text.
        int[] insets = CartesianChartRenderer.computeAxisInsets(
            context,
            xAxis,
            yAxis,
            null,
            yRange,
            categories.length > 0 ? categories : null,
            true,
            true);
        LytRect inner = plotRect.shrink(insets[0], insets[1], insets[2], insets[3]);
        plotCache = inner;
        if (inner.width() <= 4 || inner.height() <= 4) {
            return;
        }

        CartesianChartRenderer
            .drawAxes(context, inner, xAxis, yAxis, null, yRange, ensureCategories(categoryCount), false);

        float categoryWidth = (float) inner.width() / categoryCount;
        int seriesCount = series.size();
        ResolvedTextStyle valueStyle = textStyle(getLabelColor());
        if (seriesCount > 0) {
            float clusterWidth = categoryWidth * barWidthRatio;
            float barWidth = clusterWidth / seriesCount;
            float baselineY = CartesianChartRenderer.mapY(0d, yRange, inner);
            for (int ci = 0; ci < categoryCount; ci++) {
                float clusterCenter = inner.x() + categoryWidth * (ci + 0.5f);
                float clusterLeft = clusterCenter - clusterWidth / 2f;
                for (int si = 0; si < seriesCount; si++) {
                    ChartSeries s = series.get(si);
                    if (ci >= s.getYs().length) continue;
                    double v = s.getYs()[ci];
                    float topY = CartesianChartRenderer.mapY(v, yRange, inner);
                    float x0 = clusterLeft + barWidth * si;
                    float x1 = x0 + barWidth - 0.5f;
                    int key = encodeKey(si, ci);
                    boolean hovered = key == hoveredKey;
                    float yTop = Math.min(topY, baselineY);
                    float yBot = Math.max(topY, baselineY);
                    if (hovered) {
                        yTop -= 2f;
                    }
                    LytRect bar = new LytRect(
                        (int) x0,
                        (int) yTop,
                        Math.max(1, (int) (x1 - x0)),
                        Math.max(1, (int) (yBot - yTop)));
                    context.fillRect(bar, s.getColor());
                    if (hovered) {
                        context.drawBorder(bar, 0xFF000000, 1);
                    }
                    drawValueLabel(context, valueStyle, v, bar, baselineY, topY);
                }
            }
        }

        // Line overlays: draw on top of bars sharing the Y axis. Each point sits at the cluster center
        // of its category index so they line up with the column groups beneath.
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
                    px[i] = inner.x() + categoryWidth * (i + 0.5f);
                    py[i] = CartesianChartRenderer.mapY(s.getYs()[i], yRange, inner);
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

        // Pie inset (if configured) goes after columns + lines so it sits on top.
        if (pieArea != null) {
            PieInsetRenderer.drawAt(context, pieArea, pieInset);
        } else {
            PieInsetRenderer.draw(context, inner, pieInset);
        }
    }

    private void drawValueLabel(RenderContext context, ResolvedTextStyle style, double value, LytRect bar,
        float baselineY, float topY) {
        if (getLabelPosition() == ChartLabelPosition.NONE) return;
        String text = formatValue(value);
        int tw = context.getStringWidth(text, style);
        int lh = context.getLineHeight(style);
        int textX = bar.x() + (bar.width() - tw) / 2;
        int textY;
        switch (getLabelPosition()) {
            case ABOVE:
                textY = (int) Math.min(topY, baselineY) - lh - 1;
                break;
            case BELOW:
                textY = (int) Math.max(topY, baselineY) + 1;
                break;
            case CENTER:
            case INSIDE:
                textY = bar.y() + (bar.height() - lh) / 2;
                break;
            case OUTSIDE:
                textY = value >= 0 ? (int) topY - lh - 1 : (int) topY + 1;
                break;
            default:
                return;
        }
        context.drawText(text, textX, textY, style);
    }

    private String[] ensureCategories(int count) {
        if (categories.length >= count) return categories;
        String[] out = new String[count];
        for (int i = 0; i < count; i++) {
            out[i] = i < categories.length ? categories[i] : Integer.toString(i + 1);
        }
        return out;
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
        if (plotCache.isEmpty() || yRangeCache == null) return -1;
        if (!plotCache.contains((int) x, (int) y)) return -1;
        int categoryCount = Math.max(categories.length, maxSeriesLength());
        if (categoryCount == 0) return -1;
        float categoryWidth = (float) plotCache.width() / categoryCount;
        // Test line overlay points first (small targets) so they win over bars beneath.
        if (!lineOverlays.isEmpty()) {
            float threshold = (LINE_POINT_RADIUS + 3f) * (LINE_POINT_RADIUS + 3f);
            float bestDist = threshold;
            int bestKey = -1;
            for (int li = 0; li < lineOverlays.size(); li++) {
                ChartSeries s = lineOverlays.get(li);
                int n = s.getYs().length;
                for (int i = 0; i < n; i++) {
                    float px = plotCache.x() + categoryWidth * (i + 0.5f);
                    float py = CartesianChartRenderer.mapY(s.getYs()[i], yRangeCache, plotCache);
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
        float clusterWidth = categoryWidth * barWidthRatio;
        float barWidth = clusterWidth / seriesCount;
        float baselineY = CartesianChartRenderer.mapY(0d, yRangeCache, plotCache);
        for (int ci = 0; ci < categoryCount; ci++) {
            float clusterCenter = plotCache.x() + categoryWidth * (ci + 0.5f);
            float clusterLeft = clusterCenter - clusterWidth / 2f;
            for (int si = 0; si < seriesCount; si++) {
                ChartSeries s = series.get(si);
                if (ci >= s.getYs().length) continue;
                double v = s.getYs()[ci];
                float topY = CartesianChartRenderer.mapY(v, yRangeCache, plotCache);
                float x0 = clusterLeft + barWidth * si;
                float x1 = x0 + barWidth;
                float yTop = Math.min(topY, baselineY);
                float yBot = Math.max(topY, baselineY);
                if (x >= x0 && x <= x1 && y >= yTop && y <= yBot) {
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
        // Percentage of the total for the current category.
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
