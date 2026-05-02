package com.hfstudio.guidenh.guide.document.block.chart;

import com.hfstudio.guidenh.guide.document.LytRect;
import com.hfstudio.guidenh.guide.render.RenderContext;
import com.hfstudio.guidenh.guide.style.ResolvedTextStyle;

/**
 * Stateless helper that draws a {@link PieInsetSpec} inside a host chart's plot rectangle.
 * The inset occupies a small square in the requested corner of the plot area.
 */
public final class PieInsetRenderer {

    private static final int CIRCLE_SEGMENTS = 24;
    private static final int MARGIN = 4;

    private PieInsetRenderer() {}

    public static void draw(RenderContext context, LytRect plotRect, PieInsetSpec spec) {
        if (spec == null || spec.getSlices()
            .isEmpty()) {
            return;
        }
        int size = Math.min(spec.getSize(), Math.min(plotRect.width(), plotRect.height()) - MARGIN * 2);
        if (size < 16) return;
        int areaX;
        int areaY;
        switch (spec.getPosition()) {
            case TOP_LEFT:
                areaX = plotRect.x() + MARGIN;
                areaY = plotRect.y() + MARGIN;
                break;
            case BOTTOM_RIGHT:
                areaX = plotRect.right() - size - MARGIN;
                areaY = plotRect.bottom() - size - MARGIN;
                break;
            case BOTTOM_LEFT:
                areaX = plotRect.x() + MARGIN;
                areaY = plotRect.bottom() - size - MARGIN;
                break;
            case RIGHT_OUTSIDE:
                // Should be drawn via drawAt(...) into a dedicated area; ignore here.
                return;
            case TOP_RIGHT:
            default:
                areaX = plotRect.right() - size - MARGIN;
                areaY = plotRect.y() + MARGIN;
                break;
        }
        drawInternal(context, spec, areaX, areaY, size);
    }

    /**
     * Draw the pie inset filling the provided rectangle entirely; used when the host chart reserves a
     * dedicated outside area (e.g. {@link PieInsetSpec.Position#RIGHT_OUTSIDE}).
     */
    public static void drawAt(RenderContext context, LytRect area, PieInsetSpec spec) {
        if (spec == null || area == null || area.width() <= 16 || area.height() <= 16) {
            return;
        }
        if (spec.getSlices()
            .isEmpty()) {
            return;
        }
        int size = Math.min(area.width(), area.height());
        int areaX = area.x() + (area.width() - size) / 2;
        int areaY = area.y() + (area.height() - size) / 2;
        drawInternal(context, spec, areaX, areaY, size);
    }

    private static void drawInternal(RenderContext context, PieInsetSpec spec, int areaX, int areaY, int size) {
        double total = 0d;
        for (PieSlice slice : spec.getSlices()) {
            total += Math.max(0d, slice.getValue());
        }
        if (total <= 0d) return;

        // Optional title above the pie.
        ResolvedTextStyle titleStyle = textStyle(spec.getTitleColor());
        int titleHeight = 0;
        if (!spec.getTitle()
            .isEmpty()) {
            titleHeight = context.getLineHeight(titleStyle);
            int tw = context.getStringWidth(spec.getTitle(), titleStyle);
            // Center within the inset; if the title is wider than the inset, anchor to the inset's
            // left edge so it does not bleed into the host plot.
            int tx = tw <= size ? areaX + (size - tw) / 2 : areaX;
            int ty = areaY;
            context.drawText(spec.getTitle(), tx, ty, titleStyle);
        }

        int pieSize = size - titleHeight;
        if (pieSize < 12) return;
        float cx = areaX + size / 2f;
        float cy = areaY + titleHeight + pieSize / 2f;
        float radius = pieSize / 2f - 1f;

        double angle = Math.toRadians(spec.getStartAngleDeg());
        double dir = spec.isClockwise() ? 1d : -1d;
        for (PieSlice slice : spec.getSlices()) {
            double sweep = (slice.getValue() / total) * Math.PI * 2d * dir;
            drawSlice(context, cx, cy, radius, angle, sweep, slice.getColor());
            angle += sweep;
        }
        // Thin outline around the pie to separate it from the host plot.
        context.drawCircleOutline(cx, cy, radius, 1f, 0xFF202020);
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

    private static ResolvedTextStyle textStyle(int color) {
        return LytChartBase.textStyle(color);
    }
}
