package com.hfstudio.guidenh.guide.document.block.chart;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.item.ItemStack;

import com.hfstudio.guidenh.guide.document.LytRect;
import com.hfstudio.guidenh.guide.render.RenderContext;
import com.hfstudio.guidenh.guide.style.ResolvedTextStyle;

/**
 * Pie chart. On hover the hovered slice is offset outward along its angle bisector.
 */
public class LytPieChart extends LytChartBase {

    private static final int CIRCLE_SEGMENTS = 32;
    private static final float HOVER_OFFSET = 4f;

    private List<PieSlice> slices = new ArrayList<>();
    private float startAngleDeg = -90f;
    private boolean clockwise = true;

    private float cxCache;
    private float cyCache;
    private float radiusCache;
    private double totalCache;

    public void setSlices(List<PieSlice> slices) {
        this.slices = slices != null ? slices : new ArrayList<>();
    }

    public List<PieSlice> getSlices() {
        return slices;
    }

    public void setStartAngleDeg(float startAngleDeg) {
        this.startAngleDeg = startAngleDeg;
    }

    public void setClockwise(boolean clockwise) {
        this.clockwise = clockwise;
    }

    @Override
    protected List<ChartLegendRenderer.LegendEntry> collectLegendEntries() {
        List<ChartLegendRenderer.LegendEntry> entries = new ArrayList<>();
        for (PieSlice s : slices) {
            entries.add(new ChartLegendRenderer.LegendEntry(s.getLabel(), s.getColor(), s.getIcon()));
        }
        return entries;
    }

    @Override
    protected void renderChart(RenderContext context, LytRect plotRect) {
        if (slices.isEmpty()) return;
        double total = 0d;
        for (PieSlice s : slices) {
            total += Math.max(0d, s.getValue());
        }
        if (total <= 0d) return;
        totalCache = total;

        float cx = plotRect.x() + plotRect.width() / 2f;
        float cy = plotRect.y() + plotRect.height() / 2f;
        float radius = Math.max(8f, Math.min(plotRect.width(), plotRect.height()) / 2f - HOVER_OFFSET - 2f);
        cxCache = cx;
        cyCache = cy;
        radiusCache = radius;

        ResolvedTextStyle labelStyle = textStyle(getLabelColor());
        int lh = context.getLineHeight(labelStyle);
        double angle = Math.toRadians(startAngleDeg);
        double dir = clockwise ? 1d : -1d;
        for (int i = 0; i < slices.size(); i++) {
            PieSlice slice = slices.get(i);
            double sweep = (slice.getValue() / total) * Math.PI * 2d * dir;
            double mid = angle + sweep / 2d;
            boolean hovered = i == hoveredKey;
            // Hovered slice keeps its apex at (cx, cy); only the outer arc bulges outward by
            // HOVER_OFFSET so the wedge is emphasized without dislocating its centre.
            float drawRadius = hovered ? radius + HOVER_OFFSET : radius;
            drawSlice(context, cx, cy, drawRadius, angle, sweep, slice.getColor());
            // Label.
            ChartLabelPosition pos = getLabelPosition();
            if (pos != ChartLabelPosition.NONE) {
                String text;
                switch (pos) {
                    case OUTSIDE:
                    case ABOVE:
                    case BELOW:
                        text = slice.getLabel() + " " + formatPercent(slice.getValue() / total);
                        break;
                    case INSIDE:
                    case CENTER:
                    default:
                        text = formatPercent(slice.getValue() / total);
                        break;
                }
                int tw = context.getStringWidth(text, labelStyle);
                float labelR = pos == ChartLabelPosition.OUTSIDE || pos == ChartLabelPosition.ABOVE
                    || pos == ChartLabelPosition.BELOW ? drawRadius + 4f : drawRadius * 0.6f;
                float tx = cx + (float) Math.cos(mid) * labelR - tw / 2f;
                float ty = cy + (float) Math.sin(mid) * labelR - lh / 2f;
                // Clamp label inside the plot rectangle so OUTSIDE labels do not overflow the chart frame.
                int clampedTx = Math.max(plotRect.x(), Math.min(plotRect.right() - tw, (int) tx));
                int clampedTy = Math.max(plotRect.y(), Math.min(plotRect.bottom() - lh, (int) ty));
                context.drawText(text, clampedTx, clampedTy, labelStyle);
            }
            angle += sweep;
        }
    }

    private static void drawSlice(RenderContext context, float cx, float cy, float radius, double startAngle,
        double sweepAngle, int color) {
        if (Math.abs(sweepAngle) < 1e-6) return;
        int segments = Math.max(2, (int) Math.ceil(CIRCLE_SEGMENTS * Math.abs(sweepAngle) / (Math.PI * 2d)));
        float[] xs = new float[segments + 2];
        float[] ys = new float[segments + 2];
        xs[0] = cx;
        ys[0] = cy;
        for (int i = 0; i <= segments; i++) {
            double a = startAngle + sweepAngle * (i / (double) segments);
            xs[i + 1] = cx + (float) Math.cos(a) * radius;
            ys[i + 1] = cy + (float) Math.sin(a) * radius;
        }
        context.fillPolygon(xs, ys, color);
    }

    @Override
    protected int hitTest(float x, float y) {
        if (radiusCache <= 0f || totalCache <= 0d) return -1;
        float dx = x - cxCache;
        float dy = y - cyCache;
        float dist = (float) Math.sqrt(dx * dx + dy * dy);
        // Allow slightly more than the radius to cover the hover offset.
        if (dist > radiusCache + HOVER_OFFSET) return -1;
        double pointAngle = Math.atan2(dy, dx);
        double startAngle = Math.toRadians(startAngleDeg);
        double dir = clockwise ? 1d : -1d;
        double angle = startAngle;
        for (int i = 0; i < slices.size(); i++) {
            PieSlice slice = slices.get(i);
            double sweep = (slice.getValue() / totalCache) * Math.PI * 2d * dir;
            if (angleIsBetween(pointAngle, angle, angle + sweep)) {
                return i;
            }
            angle += sweep;
        }
        return -1;
    }

    /** Tests whether {@code angle} lies on the arc (from, to) (in the direction from -> to). */
    public static boolean angleIsBetween(double angle, double from, double to) {
        // Normalize all angles to the same cycle as `from`: bring `angle` into [from, from + 2π).
        double twoPi = Math.PI * 2d;
        if (Math.abs(to - from) < 1e-9) return false;
        double sweep = to - from;
        // Move `angle` to the right side of `from` (along the sweep direction).
        double rel = angle - from;
        if (sweep > 0) {
            while (rel < 0) rel += twoPi;
            while (rel >= twoPi) rel -= twoPi;
            return rel >= 0 && rel < sweep;
        } else {
            // Reverse: along the decreasing direction.
            while (rel > 0) rel -= twoPi;
            while (rel <= -twoPi) rel += twoPi;
            return rel <= 0 && rel > sweep;
        }
    }

    @Override
    protected String describeHit(int key) {
        if (key < 0 || key >= slices.size()) return null;
        PieSlice slice = slices.get(key);
        StringBuilder sb = new StringBuilder();
        if (!slice.getLabel()
            .isEmpty()) {
            sb.append(slice.getLabel())
                .append('\n');
        }
        sb.append(formatValue(slice.getValue()));
        if (totalCache > 0d) {
            sb.append('\n')
                .append(formatPercent(slice.getValue() / totalCache));
        }
        return sb.toString();
    }

    @Override
    protected ItemStack getHitItemStack(int key) {
        if (key < 0 || key >= slices.size()) return null;
        ChartIcon icon = slices.get(key)
            .getIcon();
        return icon != null && icon.hasItemStack() ? icon.getStack() : null;
    }

    @Override
    protected String getHitExtraTooltip(int key) {
        if (key < 0 || key >= slices.size()) return null;
        return slices.get(key)
            .getTooltipExtra();
    }
}
