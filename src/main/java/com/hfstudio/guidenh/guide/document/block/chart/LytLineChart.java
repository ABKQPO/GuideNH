package com.hfstudio.guidenh.guide.document.block.chart;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.item.ItemStack;

import com.hfstudio.guidenh.guide.document.LytRect;
import com.hfstudio.guidenh.guide.render.RenderContext;
import com.hfstudio.guidenh.guide.style.ResolvedTextStyle;

/**
 * Line chart. Each series is defined by X / Y value arrays. The X axis can use categories (see
 * {@link #setCategories}).
 * On hover within a data point's circular hit area: the point is highlighted (radius +2 with a black
 * outline); the two segments adjacent to it are thickened by +1, and the point is pushed outward by 2px
 * along the tangent's normal direction.
 */
public class LytLineChart extends LytChartBase {

    private static final int POINT_RADIUS = 2;
    private static final int LINE_THICKNESS = 1;

    private List<ChartSeries> series = new ArrayList<>();
    private String[] categories = new String[0];
    private ChartAxisOptions xAxis = new ChartAxisOptions();
    private ChartAxisOptions yAxis = new ChartAxisOptions();
    private boolean numericX = false;
    private boolean showPoints = true;

    private LytRect plotCache = LytRect.empty();
    private AxisRange xRangeCache;
    private AxisRange yRangeCache;

    public void setSeries(List<ChartSeries> series) {
        this.series = series != null ? series : new ArrayList<>();
    }

    public List<ChartSeries> getSeries() {
        return series;
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

    public void setNumericX(boolean numericX) {
        this.numericX = numericX;
    }

    public void setShowPoints(boolean showPoints) {
        this.showPoints = showPoints;
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

        double yMin = Double.POSITIVE_INFINITY;
        double yMax = Double.NEGATIVE_INFINITY;
        double xMin = Double.POSITIVE_INFINITY;
        double xMax = Double.NEGATIVE_INFINITY;
        for (ChartSeries s : series) {
            for (double v : s.getYs()) {
                if (v < yMin) yMin = v;
                if (v > yMax) yMax = v;
            }
            for (double v : s.getXs()) {
                if (v < xMin) xMin = v;
                if (v > xMax) xMax = v;
            }
        }
        if (!Double.isFinite(yMin)) {
            yMin = 0d;
            yMax = 1d;
        }
        AxisRange yRange = AxisRange.compute(yAxis.getMin(), yAxis.getMax(), yAxis.getStep(), yMin, yMax);
        AxisRange xRange = null;
        if (numericX) {
            xRange = AxisRange.compute(xAxis.getMin(), xAxis.getMax(), xAxis.getStep(), xMin, xMax);
        }
        xRangeCache = xRange;
        yRangeCache = yRange;

        int categoryCount = Math.max(categories.length, maxSeriesLength());
        int[] insets = CartesianChartRenderer.computeAxisInsets(
            context,
            xAxis,
            yAxis,
            xRange,
            yRange,
            numericX ? null : ensureCategories(categoryCount),
            true,
            true);
        LytRect inner = plotRect.shrink(insets[0], insets[1], insets[2], insets[3]);
        plotCache = inner;
        if (inner.width() <= 4 || inner.height() <= 4) return;

        CartesianChartRenderer.drawAxes(
            context,
            inner,
            xAxis,
            yAxis,
            xRange,
            yRange,
            numericX ? null : ensureCategories(categoryCount),
            numericX);

        for (int si = 0; si < series.size(); si++) {
            ChartSeries s = series.get(si);
            int n = s.getYs().length;
            if (n == 0) continue;
            float[] px = new float[n];
            float[] py = new float[n];
            for (int i = 0; i < n; i++) {
                px[i] = pointX(i, s, xRange, inner, categoryCount);
                py[i] = CartesianChartRenderer.mapY(s.getYs()[i], yRange, inner);
            }

            int hoveredSeries = decodeSeries(hoveredKey);
            int hoveredPoint = decodePoint(hoveredKey);
            // Line segments (segments adjacent to the hovered point are thickened).
            for (int i = 0; i + 1 < n; i++) {
                float thick = LINE_THICKNESS;
                if (hoveredKey >= 0 && hoveredSeries == si && (hoveredPoint == i || hoveredPoint == i + 1)) {
                    thick = LINE_THICKNESS + 1f;
                }
                context.drawLine(px[i], py[i], px[i + 1], py[i + 1], thick, s.getColor());
            }

            // Data points.
            if (showPoints) {
                for (int i = 0; i < n; i++) {
                    boolean hovered = hoveredKey >= 0 && hoveredSeries == si && hoveredPoint == i;
                    float x = px[i];
                    float y = py[i];
                    if (hovered) {
                        // Push outward by 2px along the "orientation" (normal of the average tangent of the
                        // adjacent segments).
                        float[] off = computeOutwardNormal(px, py, i, n);
                        x += off[0] * 2f;
                        y += off[1] * 2f;
                    }
                    float r = hovered ? POINT_RADIUS + 2f : POINT_RADIUS;
                    context.fillCircle(x, y, r, s.getColor());
                    if (hovered) {
                        context.drawCircleOutline(x, y, r, 1f, 0xFF000000);
                    }
                }
            }

            // Data value labels.
            if (getLabelPosition() != ChartLabelPosition.NONE) {
                ResolvedTextStyle style = textStyle(getLabelColor());
                int lh = context.getLineHeight(style);
                for (int i = 0; i < n; i++) {
                    String text = formatValue(s.getYs()[i]);
                    int tw = context.getStringWidth(text, style);
                    int tx = (int) px[i] - tw / 2;
                    int ty;
                    switch (getLabelPosition()) {
                        case ABOVE:
                        case OUTSIDE:
                            ty = (int) py[i] - lh - 2;
                            break;
                        case BELOW:
                            ty = (int) py[i] + 3;
                            break;
                        case CENTER:
                        case INSIDE:
                            ty = (int) py[i] - lh / 2;
                            break;
                        default:
                            continue;
                    }
                    context.drawText(text, tx, ty, style);
                }
            }
        }
    }

    private static float[] computeOutwardNormal(float[] xs, float[] ys, int i, int n) {
        float dx = 0f;
        float dy = 0f;
        if (i > 0) {
            dx += xs[i] - xs[i - 1];
            dy += ys[i] - ys[i - 1];
        }
        if (i + 1 < n) {
            dx += xs[i + 1] - xs[i];
            dy += ys[i + 1] - ys[i];
        }
        // Normal (-dy, dx). "Outward" takes the upper screen direction (smaller y): ensure ny < 0.
        float nx = -dy;
        float ny = dx;
        if (ny > 0f) {
            nx = -nx;
            ny = -ny;
        }
        float len = (float) Math.sqrt(nx * nx + ny * ny);
        if (len < 1e-4f) return new float[] { 0f, -1f };
        return new float[] { nx / len, ny / len };
    }

    private float pointX(int i, ChartSeries s, AxisRange xRange, LytRect inner, int categoryCount) {
        if (numericX && xRange != null && i < s.getXs().length) {
            return CartesianChartRenderer.mapX(s.getXs()[i], xRange, inner);
        }
        int count = Math.max(1, categoryCount);
        float step = (float) inner.width() / count;
        return inner.x() + step * (i + 0.5f);
    }

    private String[] ensureCategories(int count) {
        if (count == 0) return new String[0];
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
        if (plotCache.isEmpty() || yRangeCache == null || series.isEmpty()) return -1;
        int categoryCount = Math.max(categories.length, maxSeriesLength());
        float threshold = POINT_RADIUS + 3f;
        float bestDist = threshold * threshold;
        int bestKey = -1;
        for (int si = 0; si < series.size(); si++) {
            ChartSeries s = series.get(si);
            int n = s.getYs().length;
            for (int i = 0; i < n; i++) {
                float px = pointX(i, s, xRangeCache, plotCache, categoryCount);
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
        if (pi >= s.getYs().length) return null;
        StringBuilder sb = new StringBuilder();
        if (!s.getName()
            .isEmpty()) {
            sb.append(s.getName())
                .append('\n');
        }
        if (numericX && pi < s.getXs().length) {
            sb.append("x: ")
                .append(formatValue(s.getXs()[pi]))
                .append('\n');
        } else {
            String cat = pi < categories.length ? categories[pi] : Integer.toString(pi + 1);
            sb.append(cat)
                .append('\n');
        }
        sb.append("y: ")
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
