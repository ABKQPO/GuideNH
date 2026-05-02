package com.hfstudio.guidenh.guide.document.block.chart;

import com.hfstudio.guidenh.guide.document.LytRect;
import com.hfstudio.guidenh.guide.render.RenderContext;
import com.hfstudio.guidenh.guide.style.ResolvedTextStyle;

/**
 * Shared axis / grid / data-value-label rendering for Cartesian charts (column / bar / line / scatter).
 */
public final class CartesianChartRenderer {

    private CartesianChartRenderer() {}

    /** Compute insets reserved for axis labels; returns [left, top, right, bottom] (pixels). */
    public static int[] computeAxisInsets(RenderContext context, ChartAxisOptions xAxis, ChartAxisOptions yAxis,
        AxisRange xRange, AxisRange yRange, String[] xCategories, boolean showXTicks, boolean showYTicks) {
        ResolvedTextStyle style = LytChartBase.textStyle(0xFFCCCCCC);
        int lineH = context.getLineHeight(style);
        int left = 4;
        int top = 4;
        int right = 4;
        int bottom = 4;
        if (showYTicks && yRange != null) {
            int maxLabel = 0;
            for (double t = yRange.min; t <= yRange.max + 1e-9; t += yRange.step) {
                String s = yAxis.formatTick(t);
                int w = context.getStringWidth(s, style);
                if (w > maxLabel) {
                    maxLabel = w;
                }
            }
            left = Math.max(left, maxLabel + 6);
            // Y-axis title is drawn horizontally above the plot's top-left corner; reserve a row.
            if (yAxis.getLabel() != null && !yAxis.getLabel()
                .isEmpty()) {
                top = Math.max(top, lineH + 4);
            }
        }
        if (showXTicks) {
            bottom = Math.max(bottom, lineH + 4);
            if (xAxis.getLabel() != null && !xAxis.getLabel()
                .isEmpty()) {
                bottom += lineH + 2;
            }
            // Reserve a small right margin so the last X tick label does not overflow the plot.
            if (xRange != null) {
                String last = xAxis.formatTick(xRange.max);
                right = Math.max(right, context.getStringWidth(last, style) / 2 + 2);
            } else if (xCategories != null && xCategories.length > 0) {
                String last = xCategories[xCategories.length - 1];
                if (last != null) {
                    right = Math.max(right, context.getStringWidth(last, style) / 2 + 2);
                }
                String first = xCategories[0];
                if (first != null) {
                    left = Math.max(left, context.getStringWidth(first, style) / 2 + 2);
                }
            }
        }
        return new int[] { left, top, right, bottom };
    }

    /** Data value -> screen X coordinate. */
    public static float mapX(double value, AxisRange range, LytRect plotRect) {
        double t = range.normalize(value);
        return (float) (plotRect.x() + t * plotRect.width());
    }

    /** Data value -> screen Y coordinate (Y axis points up, so it is inverted). */
    public static float mapY(double value, AxisRange range, LytRect plotRect) {
        double t = range.normalize(value);
        return (float) (plotRect.bottom() - t * plotRect.height());
    }

    /** Render axes + grid lines + tick text inside plotRect. */
    public static void drawAxes(RenderContext context, LytRect plotRect, ChartAxisOptions xAxis, ChartAxisOptions yAxis,
        AxisRange xRange, AxisRange yRange, String[] xCategories, boolean numericX) {
        ResolvedTextStyle xLabelStyle = LytChartBase.textStyle(xAxis.getLabelColor());
        ResolvedTextStyle yLabelStyle = LytChartBase.textStyle(yAxis.getLabelColor());

        // Y-axis grid + ticks.
        if (yRange != null) {
            for (double t = yRange.min; t <= yRange.max + 1e-9; t += yRange.step) {
                float y = mapY(t, yRange, plotRect);
                if (yAxis.isGridVisible()) {
                    context.drawLine(plotRect.x(), y, plotRect.right(), y, 1f, yAxis.getGridColor());
                }
                String s = yAxis.formatTick(t);
                int sw = context.getStringWidth(s, yLabelStyle);
                int lh = context.getLineHeight(yLabelStyle);
                context.drawText(s, plotRect.x() - sw - 4, (int) y - lh / 2, yLabelStyle);
            }
        }

        // X axis: numeric or categorical. Tick labels are clamped horizontally so they do not
        // overflow the chart bounds at the edges.
        if (numericX && xRange != null) {
            for (double t = xRange.min; t <= xRange.max + 1e-9; t += xRange.step) {
                float x = mapX(t, xRange, plotRect);
                if (xAxis.isGridVisible()) {
                    context.drawLine(x, plotRect.y(), x, plotRect.bottom(), 1f, xAxis.getGridColor());
                }
                String s = xAxis.formatTick(t);
                int sw = context.getStringWidth(s, xLabelStyle);
                int tx = (int) x - sw / 2;
                // Allow at most half the label to extend past the plot edge so neighbouring text
                // does not collide with the chart border.
                tx = Math.max(plotRect.x() - sw / 2, Math.min(plotRect.right() - sw / 2, tx));
                context.drawText(s, tx, plotRect.bottom() + 3, xLabelStyle);
            }
        } else if (xCategories != null && xCategories.length > 0) {
            float step = (float) plotRect.width() / xCategories.length;
            for (int i = 0; i < xCategories.length; i++) {
                String label = xCategories[i] != null ? xCategories[i] : "";
                int sw = context.getStringWidth(label, xLabelStyle);
                float cx = plotRect.x() + step * (i + 0.5f);
                int tx = (int) cx - sw / 2;
                context.drawText(label, tx, plotRect.bottom() + 3, xLabelStyle);
            }
        }

        // Axis border.
        context.drawLine(plotRect.x(), plotRect.y(), plotRect.x(), plotRect.bottom(), 1f, xAxis.getAxisColor());
        context
            .drawLine(plotRect.x(), plotRect.bottom(), plotRect.right(), plotRect.bottom(), 1f, xAxis.getAxisColor());

        // Axis title.
        if (xAxis.getLabel() != null && !xAxis.getLabel()
            .isEmpty()) {
            int sw = context.getStringWidth(xAxis.getLabel(), xLabelStyle);
            int lh = context.getLineHeight(xLabelStyle);
            // Center the X-axis label below the tick row, but clamp to the plot's horizontal range
            // so a long label does not bleed past the right edge.
            int tx = plotRect.x() + Math.max(0, (plotRect.width() - sw) / 2);
            context.drawText(xAxis.getLabel(), tx, plotRect.bottom() + 3 + lh + 2, xLabelStyle);
        }
        if (yAxis.getLabel() != null && !yAxis.getLabel()
            .isEmpty()) {
            int lh = context.getLineHeight(yLabelStyle);
            // Place the Y-axis label horizontally above the plot's top-left corner instead of to
            // its left, so long labels (e.g. "Strength (dB)") never overflow the chart frame.
            context.drawText(yAxis.getLabel(), plotRect.x(), plotRect.y() - lh - 2, yLabelStyle);
        }
    }
}
