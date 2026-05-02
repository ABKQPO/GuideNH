package com.hfstudio.guidenh.guide.document.block.chart;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.item.ItemStack;

import com.hfstudio.guidenh.guide.document.LytRect;
import com.hfstudio.guidenh.guide.render.RenderContext;
import com.hfstudio.guidenh.guide.style.ResolvedTextStyle;

/**
 * XY scatter chart: draws only data points without connecting lines.
 */
public class LytScatterChart extends LytChartBase {

    private static final int POINT_RADIUS = 3;

    private List<ChartSeries> series = new ArrayList<>();
    private ChartAxisOptions xAxis = new ChartAxisOptions();
    private ChartAxisOptions yAxis = new ChartAxisOptions();

    private LytRect plotCache = LytRect.empty();
    private AxisRange xRangeCache;
    private AxisRange yRangeCache;

    public void setSeries(List<ChartSeries> series) {
        this.series = series != null ? series : new ArrayList<>();
    }

    public List<ChartSeries> getSeries() {
        return series;
    }

    public void setXAxis(ChartAxisOptions xAxis) {
        if (xAxis != null) this.xAxis = xAxis;
    }

    public void setYAxis(ChartAxisOptions yAxis) {
        if (yAxis != null) this.yAxis = yAxis;
    }

    @Override
    protected List<ChartLegendRenderer.LegendEntry> collectLegendEntries() {
        List<ChartLegendRenderer.LegendEntry> entries = new ArrayList<>();
        for (ChartSeries s : series) {
            entries.add(new ChartLegendRenderer.LegendEntry(s.getName(), s.getColor(), s.getIcon()));
        }
        return entries;
    }

    @Override
    protected void renderChart(RenderContext context, LytRect plotRect) {
        if (series.isEmpty()) return;
        double xMin = Double.POSITIVE_INFINITY;
        double xMax = Double.NEGATIVE_INFINITY;
        double yMin = Double.POSITIVE_INFINITY;
        double yMax = Double.NEGATIVE_INFINITY;
        for (ChartSeries s : series) {
            for (double v : s.getXs()) {
                if (v < xMin) xMin = v;
                if (v > xMax) xMax = v;
            }
            for (double v : s.getYs()) {
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
        AxisRange xRange = AxisRange.compute(xAxis.getMin(), xAxis.getMax(), xAxis.getStep(), xMin, xMax);
        AxisRange yRange = AxisRange.compute(yAxis.getMin(), yAxis.getMax(), yAxis.getStep(), yMin, yMax);
        xRangeCache = xRange;
        yRangeCache = yRange;

        int[] insets = CartesianChartRenderer
            .computeAxisInsets(context, xAxis, yAxis, xRange, yRange, null, true, true);
        LytRect inner = plotRect.shrink(insets[0], insets[1], insets[2], insets[3]);
        plotCache = inner;
        if (inner.width() <= 4 || inner.height() <= 4) return;

        CartesianChartRenderer.drawAxes(context, inner, xAxis, yAxis, xRange, yRange, null, true);

        ResolvedTextStyle valueStyle = textStyle(getLabelColor());
        int lh = context.getLineHeight(valueStyle);
        int hoveredSeries = decodeSeries(hoveredKey);
        int hoveredPoint = decodePoint(hoveredKey);
        for (int si = 0; si < series.size(); si++) {
            ChartSeries s = series.get(si);
            int n = Math.min(s.getXs().length, s.getYs().length);
            for (int i = 0; i < n; i++) {
                float x = CartesianChartRenderer.mapX(s.getXs()[i], xRange, inner);
                float y = CartesianChartRenderer.mapY(s.getYs()[i], yRange, inner);
                boolean hovered = hoveredKey >= 0 && hoveredSeries == si && hoveredPoint == i;
                float r = hovered ? POINT_RADIUS + 2f : POINT_RADIUS;
                int color = hovered ? brighten(s.getColor()) : s.getColor();
                context.fillCircle(x, y, r, color);
                if (hovered) {
                    context.drawCircleOutline(x, y, r, 1f, 0xFF000000);
                }
                if (getLabelPosition() != ChartLabelPosition.NONE) {
                    String text = "(" + formatValue(s.getXs()[i]) + "," + formatValue(s.getYs()[i]) + ")";
                    int tw = context.getStringWidth(text, valueStyle);
                    int tx = (int) x - tw / 2;
                    int ty;
                    switch (getLabelPosition()) {
                        case ABOVE:
                        case OUTSIDE:
                            ty = (int) y - lh - 2;
                            break;
                        case BELOW:
                            ty = (int) y + 3;
                            break;
                        default:
                            continue;
                    }
                    context.drawText(text, tx, ty, valueStyle);
                }
            }
        }
    }

    private static int brighten(int argb) {
        int a = (argb >>> 24) & 0xFF;
        int r = (argb >>> 16) & 0xFF;
        int g = (argb >>> 8) & 0xFF;
        int b = argb & 0xFF;
        r = Math.min(255, r + 32);
        g = Math.min(255, g + 32);
        b = Math.min(255, b + 32);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private static int encodeKey(int seriesIdx, int pointIdx) {
        return (seriesIdx & 0xFFFF) << 16 | (pointIdx & 0xFFFF);
    }

    private static int decodeSeries(int key) {
        return (key >>> 16) & 0xFFFF;
    }

    private static int decodePoint(int key) {
        return key & 0xFFFF;
    }

    @Override
    protected int hitTest(float x, float y) {
        if (plotCache.isEmpty() || xRangeCache == null || yRangeCache == null || series.isEmpty()) return -1;
        float threshold = POINT_RADIUS + 3f;
        float bestDist = threshold * threshold;
        int bestKey = -1;
        for (int si = 0; si < series.size(); si++) {
            ChartSeries s = series.get(si);
            int n = Math.min(s.getXs().length, s.getYs().length);
            for (int i = 0; i < n; i++) {
                float px = CartesianChartRenderer.mapX(s.getXs()[i], xRangeCache, plotCache);
                float py = CartesianChartRenderer.mapY(s.getYs()[i], yRangeCache, plotCache);
                float dx = x - px;
                float dy = y - py;
                float d = dx * dx + dy * dy;
                if (d < bestDist) {
                    bestDist = d;
                    bestKey = encodeKey(si, i);
                }
            }
        }
        return bestKey;
    }

    @Override
    protected String describeHit(int key) {
        int si = decodeSeries(key);
        int pi = decodePoint(key);
        if (si >= series.size()) return null;
        ChartSeries s = series.get(si);
        if (pi >= s.getXs().length || pi >= s.getYs().length) return null;
        StringBuilder sb = new StringBuilder();
        if (!s.getName()
            .isEmpty()) {
            sb.append(s.getName())
                .append('\n');
        }
        sb.append("x: ")
            .append(formatValue(s.getXs()[pi]))
            .append('\n')
            .append("y: ")
            .append(formatValue(s.getYs()[pi]));
        return sb.toString();
    }

    @Override
    protected ItemStack getHitItemStack(int key) {
        int si = decodeSeries(key);
        if (si < 0 || si >= series.size()) return null;
        ChartIcon icon = series.get(si)
            .getIcon();
        return icon != null && icon.hasItemStack() ? icon.getStack() : null;
    }

    @Override
    protected String getHitExtraTooltip(int key) {
        int si = decodeSeries(key);
        if (si < 0 || si >= series.size()) return null;
        return series.get(si)
            .getTooltipExtra();
    }
}
