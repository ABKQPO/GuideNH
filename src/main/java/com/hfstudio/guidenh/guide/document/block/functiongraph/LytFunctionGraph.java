package com.hfstudio.guidenh.guide.document.block.functiongraph;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.hfstudio.guidenh.guide.color.ConstantColor;
import com.hfstudio.guidenh.guide.document.LytRect;
import com.hfstudio.guidenh.guide.document.block.LytBlock;
import com.hfstudio.guidenh.guide.document.interaction.DocumentDragTarget;
import com.hfstudio.guidenh.guide.document.interaction.GuideTooltip;
import com.hfstudio.guidenh.guide.document.interaction.InteractiveElement;
import com.hfstudio.guidenh.guide.layout.LayoutContext;
import com.hfstudio.guidenh.guide.render.RenderContext;
import com.hfstudio.guidenh.guide.style.ResolvedTextStyle;
import com.hfstudio.guidenh.guide.style.TextAlignment;
import com.hfstudio.guidenh.guide.style.WhiteSpaceMode;

/**
 * Function graph block. Plots one or more {@link FunctionPlot} curves on a Cartesian panel with
 * interactive Desmos-style hovering: while the cursor is over a curve the segment is thickened, an
 * accent point is drawn at the cursor's x value, and a custom tooltip anchored to the point is
 * rendered. Pressing the mouse button latches the highlight onto that curve so the user can drag
 * along it freely until the button is released, even when the cursor strays vertically.
 *
 * <p>
 * Layout, sampling and the tooltip overlay are all handled inside this single block so the rest
 * of the document does not need to coordinate with it.
 */
public class LytFunctionGraph extends LytBlock implements InteractiveElement, DocumentDragTarget {

    private static final int DEFAULT_WIDTH = 320;
    private static final int DEFAULT_HEIGHT = 220;
    private static final int PADDING = 8;
    private static final int TITLE_GAP = 4;
    private static final int AXIS_LABEL_GAP = 4;
    private static final int AXIS_PAD_LEFT = 28;
    private static final int AXIS_PAD_BOTTOM = 14;
    private static final int MIN_SAMPLES = 64;
    private static final int MAX_SAMPLES = 1024;
    private static final float HIT_THRESHOLD_PX = 4f;
    private static final float HIGHLIGHT_LINE_BONUS = 1.0f;
    private static final int POINT_RADIUS = 3;
    private static final float POINT_OUTER_RING = 1f;
    private static final int TOOLTIP_PADDING_X = 5;
    private static final int TOOLTIP_PADDING_Y = 4;
    private static final int TOOLTIP_GAP = 8;
    private static final int LEGEND_GAP_ABOVE = 4;
    private static final int LEGEND_ROW_GAP = 2;
    private static final int LEGEND_ITEM_GAP = 10;
    private static final int LEGEND_SWATCH_SIZE = 7;
    private static final int LEGEND_SWATCH_TEXT_GAP = 4;

    private static final ResolvedTextStyle TITLE_STYLE = makeStyle(0xFFE6E6E6, true);
    private static final ResolvedTextStyle AXIS_LABEL_STYLE = makeStyle(0xFFB8C2CF, false);
    private static final ResolvedTextStyle TOOLTIP_TITLE_STYLE = makeStyle(0xFFFFFFFF, false);
    private static final ResolvedTextStyle TOOLTIP_BODY_STYLE = makeStyle(0xFFD7DEE7, false);
    private static final ResolvedTextStyle LEGEND_LABEL_STYLE = makeStyle(0xFFD7DEE7, false);

    private final List<FunctionPlot> plots = new ArrayList<>();
    private final List<MarkedPoint> points = new ArrayList<>();

    private String title;
    private int explicitWidth = -1;
    private int explicitHeight = -1;
    private int backgroundColor = 0xFF1B1F23;
    private int borderColor = 0xFF3A4047;
    private int axisColor = 0xFFB8C2CF;
    private int gridColor = 0x33B8C2CF;
    private boolean showGrid = true;
    private boolean showAxes = true;

    private double explicitXMin = Double.NaN;
    private double explicitXMax = Double.NaN;
    private double explicitYMin = Double.NaN;
    private double explicitYMax = Double.NaN;
    private double explicitXStep = Double.NaN;
    private double explicitYStep = Double.NaN;

    /** Bitmask of visible quadrants (bit i = quadrant i+1). {@code 0} means auto. */
    private int explicitQuadrantMask = 0;

    private double effectiveXMin;
    private double effectiveXMax;
    private double effectiveYMin;
    private double effectiveYMax;
    private double effectiveXStep;
    private double effectiveYStep;

    private LytRect plotRectCache = LytRect.empty();
    private float[][] sampleXs;
    private float[][] sampleYs;
    private long sampleCacheKey = -1L;

    private int activePlotIndex = -1;
    private double activeDataX;
    private boolean isDragging;
    private int dragButton;

    public void addPlot(FunctionPlot plot) {
        if (plot != null) {
            plots.add(plot);
        }
    }

    public void addPoint(MarkedPoint point) {
        if (point != null) {
            points.add(point);
        }
    }

    public List<FunctionPlot> getPlots() {
        return plots;
    }

    public List<MarkedPoint> getPoints() {
        return points;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setExplicitSize(int width, int height) {
        this.explicitWidth = width > 0 ? width : -1;
        this.explicitHeight = height > 0 ? height : -1;
    }

    public void setBackgroundColor(int backgroundColor) {
        this.backgroundColor = backgroundColor;
    }

    public void setBorderColor(int borderColor) {
        this.borderColor = borderColor;
    }

    public void setAxisColor(int axisColor) {
        this.axisColor = axisColor;
    }

    public void setGridColor(int gridColor) {
        this.gridColor = gridColor;
    }

    public void setShowGrid(boolean showGrid) {
        this.showGrid = showGrid;
    }

    public void setShowAxes(boolean showAxes) {
        this.showAxes = showAxes;
    }

    public void setExplicitXRange(double min, double max) {
        this.explicitXMin = min;
        this.explicitXMax = max;
    }

    public void setExplicitYRange(double min, double max) {
        this.explicitYMin = min;
        this.explicitYMax = max;
    }

    public void setExplicitXStep(double step) {
        this.explicitXStep = step;
    }

    public void setExplicitYStep(double step) {
        this.explicitYStep = step;
    }

    public void setQuadrantMask(int mask) {
        this.explicitQuadrantMask = mask & 0xF;
    }

    @Override
    protected LytRect computeLayout(LayoutContext context, int x, int y, int availableWidth) {
        int width = explicitWidth > 0 ? explicitWidth : DEFAULT_WIDTH;
        if (width > availableWidth) {
            width = availableWidth;
        }
        int height = explicitHeight > 0 ? explicitHeight : DEFAULT_HEIGHT;
        invalidateSamples();
        return new LytRect(x, y, width, height);
    }

    @Override
    protected void onLayoutMoved(int deltaX, int deltaY) {
        invalidateSamples();
    }

    @Override
    public void render(RenderContext context) {
        context.fillRect(bounds, backgroundColor);
        context.drawBorder(bounds, borderColor, 1);

        int contentTop = bounds.y() + PADDING;
        int contentBottom = bounds.bottom() - PADDING;
        int contentLeft = bounds.x() + PADDING;
        int contentRight = bounds.right() - PADDING;

        if (title != null && !title.isEmpty()) {
            int tw = context.getStringWidth(title, TITLE_STYLE);
            int tx = bounds.x() + (bounds.width() - tw) / 2;
            context.drawText(title, tx, contentTop, TITLE_STYLE);
            contentTop += context.getLineHeight(TITLE_STYLE) + TITLE_GAP;
        }

        int plotLeft = contentLeft + AXIS_PAD_LEFT;
        int plotRight = contentRight;
        int plotTop = contentTop;
        int legendWidth = Math.max(0, plotRight - plotLeft);
        int legendHeight = measureLegendHeight(context, legendWidth);
        int plotBottom = contentBottom - AXIS_PAD_BOTTOM - (legendHeight > 0 ? legendHeight + LEGEND_GAP_ABOVE : 0);
        if (plotRight - plotLeft <= 16 || plotBottom - plotTop <= 16) {
            return;
        }
        LytRect plotRect = new LytRect(plotLeft, plotTop, plotRight - plotLeft, plotBottom - plotTop);

        resolveRanges();
        plotRectCache = plotRect;
        ensureSamples(plotRect);

        // Re-resolve once with sampled extents now that all plots have been visited; this lets the
        // y-axis auto-expand when samples produced negative values while the user did not pin yMin.
        if (autoExpandFromSamples(plotRect)) {
            ensureSamples(plotRect);
        }

        if (showGrid) {
            drawGrid(context, plotRect);
        }
        if (showAxes) {
            drawAxes(context, plotRect);
        }

        for (int i = 0; i < plots.size(); i++) {
            renderPlot(context, plotRect, i);
        }

        renderMarkedPoints(context, plotRect);

        if (activePlotIndex >= 0 && activePlotIndex < plots.size()) {
            renderActiveOverlay(context, plotRect);
        }

        if (legendHeight > 0) {
            int legendTop = plotRect.bottom() + AXIS_PAD_BOTTOM + LEGEND_GAP_ABOVE;
            renderLegend(context, plotRect.x(), legendTop, legendWidth);
        }
    }

    @Override
    public Optional<GuideTooltip> getTooltip(float x, float y) {
        if (!isDragging) {
            updateHover(x, y);
        }
        // Tooltip is rendered manually anchored to the point; no built-in tooltip is returned.
        return Optional.empty();
    }

    @Override
    public void onMouseLeave() {
        if (!isDragging) {
            activePlotIndex = -1;
        }
    }

    @Override
    public boolean beginDrag(int documentX, int documentY, int button) {
        if (plotRectCache.isEmpty()) {
            return false;
        }
        if (!plotRectCache.contains(documentX, documentY)) {
            return false;
        }
        int hit = hitTest(documentX, documentY);
        if (hit < 0) {
            return false;
        }
        activePlotIndex = hit;
        activeDataX = unmapXToData(
            documentX,
            plots.get(hit)
                .isInverse());
        isDragging = true;
        dragButton = button;
        return true;
    }

    @Override
    public void dragTo(int documentX, int documentY) {
        if (!isDragging || activePlotIndex < 0 || activePlotIndex >= plots.size()) {
            return;
        }
        FunctionPlot plot = plots.get(activePlotIndex);
        // Clamp the cursor x onto the plot rect so dragging out of the panel still tracks the
        // closest valid sample; vertical movement is intentionally ignored.
        int clampedX = Math.max(plotRectCache.x(), Math.min(plotRectCache.right(), documentX));
        activeDataX = unmapXToData(clampedX, plot.isInverse());
    }

    @Override
    public void endDrag() {
        isDragging = false;
        // Keep the active highlight visible until the next hover update clears it.
    }

    @Override
    public boolean scroll(int documentX, int documentY, int wheelDelta) {
        return false;
    }

    private void resolveRanges() {
        double xMin = !Double.isNaN(explicitXMin) ? explicitXMin : 0d;
        double xMax = !Double.isNaN(explicitXMax) ? explicitXMax : 10d;
        if (xMax <= xMin) {
            xMax = xMin + 1d;
        }
        double yMin = !Double.isNaN(explicitYMin) ? explicitYMin : 0d;
        double yMax = !Double.isNaN(explicitYMax) ? explicitYMax : 10d;
        if (yMax <= yMin) {
            yMax = yMin + 1d;
        }

        // Quadrant attribute can force the visible window to span negative axes. Each quadrant bit
        // expands the corresponding half-axis when not pinned by the user.
        if (explicitQuadrantMask != 0) {
            boolean q1 = (explicitQuadrantMask & 1) != 0;
            boolean q2 = (explicitQuadrantMask & 2) != 0;
            boolean q3 = (explicitQuadrantMask & 4) != 0;
            boolean q4 = (explicitQuadrantMask & 8) != 0;
            if ((q2 || q3) && Double.isNaN(explicitXMin)) {
                xMin = -Math.max(Math.abs(xMax), 10d);
            }
            if ((q1 || q4) && Double.isNaN(explicitXMax)) {
                xMax = Math.max(Math.abs(xMin), 10d);
            }
            if ((q1 || q2) && Double.isNaN(explicitYMax)) {
                yMax = Math.max(Math.abs(yMin), 10d);
            }
            if ((q3 || q4) && Double.isNaN(explicitYMin)) {
                yMin = -Math.max(Math.abs(yMax), 10d);
            }
        }

        effectiveXMin = xMin;
        effectiveXMax = xMax;
        effectiveYMin = yMin;
        effectiveYMax = yMax;
        effectiveXStep = !Double.isNaN(explicitXStep) && explicitXStep > 0 ? explicitXStep : niceStep(xMax - xMin);
        effectiveYStep = !Double.isNaN(explicitYStep) && explicitYStep > 0 ? explicitYStep : niceStep(yMax - yMin);
    }

    /**
     * After the first sampling pass, optionally expand the y axis (and x for inverse plots) to fit
     * actual sample values; only applied when the user did not pin the corresponding bound.
     */
    private boolean autoExpandFromSamples(LytRect plotRect) {
        if (sampleXs == null || plots.isEmpty()) {
            return false;
        }
        boolean changed = false;
        double minY = Double.POSITIVE_INFINITY;
        double maxY = Double.NEGATIVE_INFINITY;
        for (int i = 0; i < plots.size(); i++) {
            FunctionPlot plot = plots.get(i);
            if (plot.isInverse()) {
                continue;
            }
            int n = sampleXs[i] != null ? sampleXs[i].length : 0;
            for (int j = 0; j < n; j++) {
                double dy = unmapY(sampleYs[i][j]);
                if (Double.isFinite(dy)) {
                    if (dy < minY) {
                        minY = dy;
                    }
                    if (dy > maxY) {
                        maxY = dy;
                    }
                }
            }
        }
        if (Double.isFinite(minY) && minY < effectiveYMin && Double.isNaN(explicitYMin)) {
            effectiveYMin = Math.floor(minY);
            changed = true;
        }
        if (Double.isFinite(maxY) && maxY > effectiveYMax && Double.isNaN(explicitYMax)) {
            effectiveYMax = Math.ceil(maxY);
            changed = true;
        }
        if (changed) {
            effectiveYStep = !Double.isNaN(explicitYStep) && explicitYStep > 0 ? explicitYStep
                : niceStep(effectiveYMax - effectiveYMin);
            invalidateSamples();
        }
        return changed;
    }

    private void invalidateSamples() {
        sampleCacheKey = -1L;
        sampleXs = null;
        sampleYs = null;
    }

    private void ensureSamples(LytRect plotRect) {
        long key = ((long) plotRect.x()) * 31L + plotRect.y();
        key = key * 31L + plotRect.width();
        key = key * 31L + plotRect.height();
        key = key * 31L + Double.hashCode(effectiveXMin);
        key = key * 31L + Double.hashCode(effectiveXMax);
        key = key * 31L + Double.hashCode(effectiveYMin);
        key = key * 31L + Double.hashCode(effectiveYMax);
        key = key * 31L + plots.size();
        if (key == sampleCacheKey && sampleXs != null) {
            return;
        }
        int sampleCount = Math.max(MIN_SAMPLES, Math.min(MAX_SAMPLES, plotRect.width() * 2));
        sampleXs = new float[plots.size()][];
        sampleYs = new float[plots.size()][];
        for (int i = 0; i < plots.size(); i++) {
            FunctionPlot plot = plots.get(i);
            float[] xs = new float[sampleCount];
            float[] ys = new float[sampleCount];
            for (int s = 0; s < sampleCount; s++) {
                double t = (double) s / (double) (sampleCount - 1);
                if (plot.isInverse()) {
                    double yVal = effectiveYMin + (effectiveYMax - effectiveYMin) * t;
                    double xVal = plot.evaluate(yVal);
                    xs[s] = (float) mapX(xVal);
                    ys[s] = (float) mapY(yVal);
                    if (!Double.isFinite(xVal)) {
                        xs[s] = Float.NaN;
                    }
                } else {
                    double xVal = effectiveXMin + (effectiveXMax - effectiveXMin) * t;
                    double yVal = plot.evaluate(xVal);
                    xs[s] = (float) mapX(xVal);
                    ys[s] = (float) mapY(yVal);
                    if (!Double.isFinite(yVal)) {
                        ys[s] = Float.NaN;
                    }
                }
            }
            sampleXs[i] = xs;
            sampleYs[i] = ys;
        }
        sampleCacheKey = key;
    }

    private double mapX(double value) {
        double t = (value - effectiveXMin) / (effectiveXMax - effectiveXMin);
        return plotRectCache.x() + t * plotRectCache.width();
    }

    private double mapY(double value) {
        double t = (value - effectiveYMin) / (effectiveYMax - effectiveYMin);
        return plotRectCache.bottom() - t * plotRectCache.height();
    }

    private double unmapX(double screenX) {
        double t = (screenX - plotRectCache.x()) / plotRectCache.width();
        return effectiveXMin + t * (effectiveXMax - effectiveXMin);
    }

    private double unmapY(double screenY) {
        double t = (plotRectCache.bottom() - screenY) / plotRectCache.height();
        return effectiveYMin + t * (effectiveYMax - effectiveYMin);
    }

    /** Convert a screen x to data coordinates. For inverse plots returns the data y instead. */
    private double unmapXToData(double screenX, boolean inverse) {
        return inverse ? unmapY(screenX) : unmapX(screenX);
    }

    private void drawGrid(RenderContext context, LytRect plotRect) {
        if (effectiveXStep > 0) {
            double start = Math.ceil(effectiveXMin / effectiveXStep) * effectiveXStep;
            for (double v = start; v <= effectiveXMax + 1e-9; v += effectiveXStep) {
                float x = (float) mapX(v);
                context.drawLine(x, plotRect.y(), x, plotRect.bottom(), 1f, gridColor);
            }
        }
        if (effectiveYStep > 0) {
            double start = Math.ceil(effectiveYMin / effectiveYStep) * effectiveYStep;
            for (double v = start; v <= effectiveYMax + 1e-9; v += effectiveYStep) {
                float y = (float) mapY(v);
                context.drawLine(plotRect.x(), y, plotRect.right(), y, 1f, gridColor);
            }
        }
    }

    private void drawAxes(RenderContext context, LytRect plotRect) {
        // Vertical (y) axis pinned to x = 0 when visible, otherwise to plotRect.x.
        float axisX = (float) mapX(0d);
        if (axisX < plotRect.x() || axisX > plotRect.right()) {
            axisX = plotRect.x();
        }
        context.drawLine(axisX, plotRect.y(), axisX, plotRect.bottom(), 1f, axisColor);

        float axisY = (float) mapY(0d);
        if (axisY < plotRect.y() || axisY > plotRect.bottom()) {
            axisY = plotRect.bottom();
        }
        context.drawLine(plotRect.x(), axisY, plotRect.right(), axisY, 1f, axisColor);

        // Y tick labels along left edge of plot rect.
        if (effectiveYStep > 0) {
            double start = Math.ceil(effectiveYMin / effectiveYStep) * effectiveYStep;
            int lh = context.getLineHeight(AXIS_LABEL_STYLE);
            for (double v = start; v <= effectiveYMax + 1e-9; v += effectiveYStep) {
                String label = formatTick(v);
                int sw = context.getStringWidth(label, AXIS_LABEL_STYLE);
                int ly = (int) mapY(v) - lh / 2;
                context.drawText(label, plotRect.x() - sw - AXIS_LABEL_GAP, ly, AXIS_LABEL_STYLE);
            }
        }
        if (effectiveXStep > 0) {
            double start = Math.ceil(effectiveXMin / effectiveXStep) * effectiveXStep;
            for (double v = start; v <= effectiveXMax + 1e-9; v += effectiveXStep) {
                String label = formatTick(v);
                int sw = context.getStringWidth(label, AXIS_LABEL_STYLE);
                int lx = (int) mapX(v) - sw / 2;
                lx = Math.max(plotRect.x() - sw / 2, Math.min(plotRect.right() - sw / 2, lx));
                context.drawText(label, lx, plotRect.bottom() + AXIS_LABEL_GAP, AXIS_LABEL_STYLE);
            }
        }
    }

    private void renderPlot(RenderContext context, LytRect plotRect, int index) {
        FunctionPlot plot = plots.get(index);
        float[] xs = sampleXs[index];
        float[] ys = sampleYs[index];
        if (xs == null || ys == null) {
            return;
        }
        boolean highlighted = activePlotIndex == index;
        float thickness = 1f + (highlighted ? HIGHLIGHT_LINE_BONUS : 0f);
        int color = plot.getColor();
        float plotHalfHeight = plotRect.height() * 0.5f;

        for (int i = 0; i + 1 < xs.length; i++) {
            float x1 = xs[i];
            float y1 = ys[i];
            float x2 = xs[i + 1];
            float y2 = ys[i + 1];
            if (Float.isNaN(x1) || Float.isNaN(y1) || Float.isNaN(x2) || Float.isNaN(y2)) {
                continue;
            }
            // Skip catastrophic vertical jumps which usually indicate an asymptote.
            if (Math.abs(y2 - y1) > plotHalfHeight) {
                continue;
            }
            // Quick scissor: if both endpoints are clearly outside the rect on the same side, skip.
            if ((y1 < plotRect.y() && y2 < plotRect.y()) || (y1 > plotRect.bottom() && y2 > plotRect.bottom())) {
                continue;
            }
            if ((x1 < plotRect.x() && x2 < plotRect.x()) || (x1 > plotRect.right() && x2 > plotRect.right())) {
                continue;
            }
            context.drawLine(x1, y1, x2, y2, thickness, color);
        }
    }

    private void renderMarkedPoints(RenderContext context, LytRect plotRect) {
        for (MarkedPoint point : points) {
            double dataX;
            double dataY;
            int color;
            switch (point.getMode()) {
                case MarkedPoint.MODE_EXPLICIT:
                    dataX = point.getValueA();
                    dataY = point.getValueB();
                    color = point.getColor();
                    break;
                case MarkedPoint.MODE_PLOT_AT_X: {
                    int pi = point.getPlotIndex();
                    if (pi < 0 || pi >= plots.size()) {
                        continue;
                    }
                    FunctionPlot plot = plots.get(pi);
                    dataX = point.getValueA();
                    dataY = plot.evaluate(dataX);
                    color = point.isColorInherit() ? plot.getColor() : point.getColor();
                    break;
                }
                case MarkedPoint.MODE_PLOT_AT_Y: {
                    int pi = point.getPlotIndex();
                    if (pi < 0 || pi >= plots.size()) {
                        continue;
                    }
                    FunctionPlot plot = plots.get(pi);
                    dataY = point.getValueA();
                    dataX = solveForX(plot, dataY);
                    color = point.isColorInherit() ? plot.getColor() : point.getColor();
                    if (Double.isNaN(dataX)) {
                        continue;
                    }
                    break;
                }
                default:
                    continue;
            }
            if (!Double.isFinite(dataX) || !Double.isFinite(dataY)) {
                continue;
            }
            float sx = (float) mapX(dataX);
            float sy = (float) mapY(dataY);
            if (sx < plotRect.x() - POINT_RADIUS || sx > plotRect.right() + POINT_RADIUS) {
                continue;
            }
            if (sy < plotRect.y() - POINT_RADIUS || sy > plotRect.bottom() + POINT_RADIUS) {
                continue;
            }
            context.fillCircle(sx, sy, POINT_RADIUS + POINT_OUTER_RING, 0xFFFFFFFF);
            context.fillCircle(sx, sy, POINT_RADIUS, color);
        }
    }

    private void renderActiveOverlay(RenderContext context, LytRect plotRect) {
        FunctionPlot plot = plots.get(activePlotIndex);
        double dataX = activeDataX;
        double dataY;
        if (plot.isInverse()) {
            dataY = dataX;
            dataX = plot.evaluate(dataY);
        } else {
            dataY = plot.evaluate(dataX);
        }
        if (!Double.isFinite(dataX) || !Double.isFinite(dataY)) {
            return;
        }
        float sx = (float) mapX(dataX);
        float sy = (float) mapY(dataY);
        if (sx < plotRect.x() || sx > plotRect.right() || sy < plotRect.y() || sy > plotRect.bottom()) {
            return;
        }
        context.fillCircle(sx, sy, POINT_RADIUS + POINT_OUTER_RING, 0xFFFFFFFF);
        context.fillCircle(sx, sy, POINT_RADIUS, plot.getColor());

        // Tooltip panel.
        String line1 = !isEmpty(plot.getLabel()) ? plot.getLabel() : plot.getExpressionText();
        String line2 = "(" + formatValue(dataX) + ", " + formatValue(dataY) + ")";
        int lineH = context.getLineHeight(TOOLTIP_BODY_STYLE);
        int textWidth = Math
            .max(context.getStringWidth(line1, TOOLTIP_TITLE_STYLE), context.getStringWidth(line2, TOOLTIP_BODY_STYLE));
        int boxWidth = textWidth + TOOLTIP_PADDING_X * 2;
        int boxHeight = lineH * 2 + TOOLTIP_PADDING_Y * 2;
        int boxX = (int) sx - boxWidth / 2;
        int boxY = (int) sy - boxHeight - TOOLTIP_GAP;
        // Flip below the point if the natural placement runs out of room above.
        if (boxY < bounds.y() + 2) {
            boxY = (int) sy + TOOLTIP_GAP;
        }
        // Clamp horizontally to the block bounds so it never escapes the panel sideways.
        boxX = Math.max(bounds.x() + 2, Math.min(bounds.right() - boxWidth - 2, boxX));
        boxY = Math.max(bounds.y() + 2, Math.min(bounds.bottom() - boxHeight - 2, boxY));

        LytRect box = new LytRect(boxX, boxY, boxWidth, boxHeight);
        context.fillRect(box, 0xEE202428);
        context.drawBorder(box, 0xFF555555, 1);
        context.drawText(line1, boxX + TOOLTIP_PADDING_X, boxY + TOOLTIP_PADDING_Y, TOOLTIP_TITLE_STYLE);
        context.drawText(line2, boxX + TOOLTIP_PADDING_X, boxY + TOOLTIP_PADDING_Y + lineH, TOOLTIP_BODY_STYLE);
    }

    /**
     * Measure the total height needed to lay out the legend below the plot, given the available
     * width. Returns {@code 0} when no plot has a label, suppressing the legend area entirely.
     */
    private int measureLegendHeight(RenderContext context, int availableWidth) {
        if (availableWidth <= 0) {
            return 0;
        }
        boolean any = false;
        for (FunctionPlot plot : plots) {
            if (plot.getLabel() != null && !plot.getLabel()
                .isEmpty()) {
                any = true;
                break;
            }
        }
        if (!any) {
            return 0;
        }
        int rowHeight = Math.max(LEGEND_SWATCH_SIZE, context.getLineHeight(LEGEND_LABEL_STYLE));
        int rows = 1;
        int rowWidth = 0;
        for (FunctionPlot plot : plots) {
            String label = plot.getLabel();
            if (label == null || label.isEmpty()) {
                continue;
            }
            int itemWidth = LEGEND_SWATCH_SIZE + LEGEND_SWATCH_TEXT_GAP
                + context.getStringWidth(label, LEGEND_LABEL_STYLE);
            int needed = rowWidth == 0 ? itemWidth : rowWidth + LEGEND_ITEM_GAP + itemWidth;
            if (rowWidth > 0 && needed > availableWidth) {
                rows++;
                rowWidth = itemWidth;
            } else {
                rowWidth = needed;
            }
        }
        return rows * rowHeight + (rows - 1) * LEGEND_ROW_GAP;
    }

    /**
     * Render the legend at {@code (left, top)}. Items flow left-to-right and wrap onto a new row
     * once the next item would exceed {@code availableWidth}.
     */
    private void renderLegend(RenderContext context, int left, int top, int availableWidth) {
        if (availableWidth <= 0) {
            return;
        }
        int rowHeight = Math.max(LEGEND_SWATCH_SIZE, context.getLineHeight(LEGEND_LABEL_STYLE));
        int x = left;
        int y = top;
        boolean firstInRow = true;
        for (FunctionPlot plot : plots) {
            String label = plot.getLabel();
            if (label == null || label.isEmpty()) {
                continue;
            }
            int itemWidth = LEGEND_SWATCH_SIZE + LEGEND_SWATCH_TEXT_GAP
                + context.getStringWidth(label, LEGEND_LABEL_STYLE);
            int needed = firstInRow ? itemWidth : (x - left) + LEGEND_ITEM_GAP + itemWidth;
            if (!firstInRow && needed > availableWidth) {
                y += rowHeight + LEGEND_ROW_GAP;
                x = left;
                firstInRow = true;
            }
            if (!firstInRow) {
                x += LEGEND_ITEM_GAP;
            }
            int swatchY = y + (rowHeight - LEGEND_SWATCH_SIZE) / 2;
            LytRect swatch = new LytRect(x, swatchY, LEGEND_SWATCH_SIZE, LEGEND_SWATCH_SIZE);
            context.fillRect(swatch, plot.getColor());
            context.drawBorder(swatch, 0xFF000000, 1);
            int textY = y + (rowHeight - context.getLineHeight(LEGEND_LABEL_STYLE)) / 2;
            context.drawText(label, x + LEGEND_SWATCH_SIZE + LEGEND_SWATCH_TEXT_GAP, textY, LEGEND_LABEL_STYLE);
            x += itemWidth;
            firstInRow = false;
        }
    }

    private void updateHover(float x, float y) {
        if (plotRectCache.isEmpty()) {
            activePlotIndex = -1;
            return;
        }
        if (!plotRectCache.contains((int) x, (int) y)) {
            activePlotIndex = -1;
            return;
        }
        int hit = hitTest(x, y);
        if (hit < 0) {
            activePlotIndex = -1;
            return;
        }
        activePlotIndex = hit;
        FunctionPlot plot = plots.get(hit);
        activeDataX = unmapXToData(x, plot.isInverse());
    }

    private int hitTest(float x, float y) {
        if (sampleXs == null) {
            return -1;
        }
        float bestDistSq = HIT_THRESHOLD_PX * HIT_THRESHOLD_PX;
        int bestIndex = -1;
        for (int i = 0; i < plots.size(); i++) {
            float[] xs = sampleXs[i];
            float[] ys = sampleYs[i];
            if (xs == null) {
                continue;
            }
            for (int s = 0; s + 1 < xs.length; s++) {
                float x1 = xs[s];
                float y1 = ys[s];
                float x2 = xs[s + 1];
                float y2 = ys[s + 1];
                if (Float.isNaN(x1) || Float.isNaN(y1) || Float.isNaN(x2) || Float.isNaN(y2)) {
                    continue;
                }
                float distSq = pointSegmentDistSq(x, y, x1, y1, x2, y2);
                if (distSq < bestDistSq) {
                    bestDistSq = distSq;
                    bestIndex = i;
                }
            }
        }
        return bestIndex;
    }

    private static float pointSegmentDistSq(float px, float py, float x1, float y1, float x2, float y2) {
        float dx = x2 - x1;
        float dy = y2 - y1;
        float lenSq = dx * dx + dy * dy;
        if (lenSq < 1e-6f) {
            float ex = px - x1;
            float ey = py - y1;
            return ex * ex + ey * ey;
        }
        float t = ((px - x1) * dx + (py - y1) * dy) / lenSq;
        if (t < 0f) t = 0f;
        if (t > 1f) t = 1f;
        float qx = x1 + t * dx;
        float qy = y1 + t * dy;
        float ex = px - qx;
        float ey = py - qy;
        return ex * ex + ey * ey;
    }

    /** Bisection solver used when a marked point only knows the y value. */
    private double solveForX(FunctionPlot plot, double targetY) {
        double xMin = effectiveXMin;
        double xMax = effectiveXMax;
        int steps = 64;
        double prevX = xMin;
        double prevF = plot.evaluate(prevX) - targetY;
        for (int i = 1; i < steps; i++) {
            double t = (double) i / (double) (steps - 1);
            double xVal = xMin + (xMax - xMin) * t;
            double f = plot.evaluate(xVal) - targetY;
            if (Double.isFinite(prevF) && Double.isFinite(f) && prevF * f <= 0d) {
                // Bisect within [prevX, xVal].
                double lo = prevX;
                double hi = xVal;
                double fLo = prevF;
                for (int k = 0; k < 32; k++) {
                    double mid = (lo + hi) * 0.5d;
                    double fm = plot.evaluate(mid) - targetY;
                    if (Math.abs(fm) < 1e-9) {
                        return mid;
                    }
                    if (fLo * fm <= 0d) {
                        hi = mid;
                    } else {
                        lo = mid;
                        fLo = fm;
                    }
                }
                return (lo + hi) * 0.5d;
            }
            prevX = xVal;
            prevF = f;
        }
        return Double.NaN;
    }

    private static double niceStep(double range) {
        if (!(range > 0d) || !Double.isFinite(range)) {
            return 1d;
        }
        double exponent = Math.floor(Math.log10(range));
        double pow10 = Math.pow(10d, exponent);
        double mantissa = range / pow10;
        double base;
        if (mantissa < 1.5d) {
            base = 0.2d;
        } else if (mantissa < 3d) {
            base = 0.5d;
        } else if (mantissa < 7d) {
            base = 1d;
        } else {
            base = 2d;
        }
        return base * pow10;
    }

    private static String formatTick(double value) {
        if (Math.abs(value - Math.rint(value)) < 1e-6) {
            return Long.toString((long) Math.rint(value));
        }
        return String.format("%.2f", value);
    }

    private static String formatValue(double value) {
        if (!Double.isFinite(value)) {
            return Double.toString(value);
        }
        if (Math.abs(value - Math.rint(value)) < 1e-6) {
            return Long.toString((long) Math.rint(value));
        }
        return String.format("%.3f", value);
    }

    private static boolean isEmpty(String s) {
        return s == null || s.isEmpty();
    }

    private static ResolvedTextStyle makeStyle(int argb, boolean bold) {
        return new ResolvedTextStyle(
            1f,
            bold,
            false,
            false,
            false,
            false,
            false,
            false,
            null,
            new ConstantColor(argb),
            WhiteSpaceMode.NORMAL,
            TextAlignment.LEFT,
            false);
    }

    @SuppressWarnings("unused")
    private int unusedDragButtonAccessor() {
        // The drag button is captured for future use (e.g. distinguishing left/right behaviour) but
        // is not consulted today; this accessor keeps it from being trimmed by static analysis.
        return dragButton;
    }
}
