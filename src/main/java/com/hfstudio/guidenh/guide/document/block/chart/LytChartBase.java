package com.hfstudio.guidenh.guide.document.block.chart;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import net.minecraft.item.ItemStack;

import com.hfstudio.guidenh.guide.color.ConstantColor;
import com.hfstudio.guidenh.guide.document.LytRect;
import com.hfstudio.guidenh.guide.document.block.LytBlock;
import com.hfstudio.guidenh.guide.document.interaction.GuideTooltip;
import com.hfstudio.guidenh.guide.document.interaction.InteractiveElement;
import com.hfstudio.guidenh.guide.document.interaction.TextTooltip;
import com.hfstudio.guidenh.guide.internal.tooltip.AppendedItemTooltip;
import com.hfstudio.guidenh.guide.layout.LayoutContext;
import com.hfstudio.guidenh.guide.render.RenderContext;
import com.hfstudio.guidenh.guide.style.ResolvedTextStyle;
import com.hfstudio.guidenh.guide.style.TextAlignment;
import com.hfstudio.guidenh.guide.style.WhiteSpaceMode;

/**
 * Common base class for all 5 chart types: handles common attributes (title, size, background, legend
 * position, etc.), layout, and hover state management.
 */
public abstract class LytChartBase extends LytBlock implements InteractiveElement {

    protected static final int DEFAULT_WIDTH = 320;
    protected static final int DEFAULT_HEIGHT = 200;
    protected static final int PADDING = 8;
    protected static final int TITLE_GAP = 4;
    protected static final int LEGEND_GAP = 6;
    protected static final int LEGEND_SWATCH_SIZE = 8;
    protected static final int LEGEND_ENTRY_GAP = 12;

    private String title;
    private int explicitWidth = -1;
    private int explicitHeight = -1;
    private int backgroundColor = 0xFF1B1F23;
    private int borderColor = 0xFF3A4047;
    private int titleColor = 0xFFE6E6E6;
    private ChartLegendPosition legendPosition = ChartLegendPosition.TOP;
    private ChartLabelPosition labelPosition = ChartLabelPosition.NONE;
    private int labelColor = 0xFFEEEEEE;

    /** Currently hovered hit key; {@code -1} means none. The exact semantics is decided by each subclass. */
    protected int hoveredKey = -1;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setExplicitSize(int width, int height) {
        this.explicitWidth = width > 0 ? width : -1;
        this.explicitHeight = height > 0 ? height : -1;
    }

    public int getBackgroundColor() {
        return backgroundColor;
    }

    public void setBackgroundColor(int backgroundColor) {
        this.backgroundColor = backgroundColor;
    }

    public int getBorderColor() {
        return borderColor;
    }

    public void setBorderColor(int borderColor) {
        this.borderColor = borderColor;
    }

    public int getTitleColor() {
        return titleColor;
    }

    public void setTitleColor(int titleColor) {
        this.titleColor = titleColor;
    }

    public ChartLegendPosition getLegendPosition() {
        return legendPosition;
    }

    public void setLegendPosition(ChartLegendPosition legendPosition) {
        this.legendPosition = legendPosition != null ? legendPosition : ChartLegendPosition.NONE;
    }

    public ChartLabelPosition getLabelPosition() {
        return labelPosition;
    }

    public void setLabelPosition(ChartLabelPosition labelPosition) {
        this.labelPosition = labelPosition != null ? labelPosition : ChartLabelPosition.NONE;
    }

    public int getLabelColor() {
        return labelColor;
    }

    public void setLabelColor(int labelColor) {
        this.labelColor = labelColor;
    }

    @Override
    protected LytRect computeLayout(LayoutContext context, int x, int y, int availableWidth) {
        int width = explicitWidth > 0 ? explicitWidth : DEFAULT_WIDTH;
        width += getExtraPlotWidth();
        int height = explicitHeight > 0 ? explicitHeight : DEFAULT_HEIGHT;
        if (width > availableWidth) {
            width = availableWidth;
        }
        return new LytRect(x, y, width, height);
    }

    /**
     * Subclasses override to request additional horizontal space (for example, a side-mounted pie inset).
     * Default 0.
     */
    protected int getExtraPlotWidth() {
        return 0;
    }

    @Override
    protected void onLayoutMoved(int deltaX, int deltaY) {}

    @Override
    public final void render(RenderContext context) {
        context.fillRect(bounds, backgroundColor);
        context.drawBorder(bounds, borderColor, 1);

        ResolvedTextStyle textStyle = textStyle(0xFFFFFFFF);
        int contentTop = bounds.y() + PADDING;
        int contentBottom = bounds.bottom() - PADDING;
        int contentLeft = bounds.x() + PADDING;
        int contentRight = bounds.right() - PADDING;

        if (title != null && !title.isEmpty()) {
            ResolvedTextStyle titleStyle = textStyle(titleColor);
            int titleWidth = context.getStringWidth(title, titleStyle);
            int titleX = bounds.x() + (bounds.width() - titleWidth) / 2;
            context.drawText(title, titleX, contentTop, titleStyle);
            contentTop += context.getLineHeight(titleStyle) + TITLE_GAP;
        }

        // Compute legend area.
        List<ChartLegendRenderer.LegendEntry> legend = collectLegendEntries();
        ChartLegendRenderer.Layout legendLayout = ChartLegendRenderer
            .computeLayout(context, legend, legendPosition, contentLeft, contentTop, contentRight, contentBottom);

        int plotLeft = legendLayout.plotLeft;
        int plotTop = legendLayout.plotTop;
        int plotRight = legendLayout.plotRight;
        int plotBottom = legendLayout.plotBottom;
        if (plotRight - plotLeft <= 8 || plotBottom - plotTop <= 8) {
            return;
        }
        LytRect plotRect = new LytRect(plotLeft, plotTop, plotRight - plotLeft, plotBottom - plotTop);

        renderChart(context, plotRect);
        ChartLegendRenderer.render(context, legendLayout, textStyle);
    }

    /**
     * Subclasses implement the chart-specific drawing; {@code plotRect} has already excluded the space
     * occupied by the title and legend.
     */
    protected abstract void renderChart(RenderContext context, LytRect plotRect);

    /**
     * Collect legend entries; empty by default. Subclasses override as needed.
     */
    protected abstract List<ChartLegendRenderer.LegendEntry> collectLegendEntries();

    @Override
    public final Optional<GuideTooltip> getTooltip(float x, float y) {
        int hit = hitTest(x, y);
        hoveredKey = hit;
        if (hit < 0) {
            return Optional.empty();
        }
        String text = describeHit(hit);
        String extra = getHitExtraTooltip(hit);
        ItemStack stack = getHitItemStack(hit);
        if (stack != null) {
            List<String> extraLines = new ArrayList<>();
            appendNonEmptyLines(extraLines, text);
            appendNonEmptyLines(extraLines, extra);
            return Optional.of(new AppendedItemTooltip(stack, extraLines));
        }
        StringBuilder sb = new StringBuilder();
        if (text != null && !text.isEmpty()) {
            sb.append(text);
        }
        if (extra != null && !extra.isEmpty()) {
            if (sb.length() > 0) {
                sb.append('\n');
            }
            sb.append(extra);
        }
        if (sb.length() == 0) {
            return Optional.empty();
        }
        return Optional.of(new TextTooltip(sb.toString()));
    }

    private static void appendNonEmptyLines(List<String> sink, String text) {
        if (text == null || text.isEmpty()) {
            return;
        }
        for (String line : text.split("\n")) {
            if (!line.isEmpty()) {
                sink.add(line);
            }
        }
    }

    /**
     * The {@link ItemStack} corresponding to the hit item; when non-{@code null} is returned, the vanilla
     * item tooltip is used with extra text appended.
     */
    protected ItemStack getHitItemStack(int key) {
        return null;
    }

    /**
     * Optional extra tooltip text for the hit item; appended after {@link #describeHit(int)}.
     */
    protected String getHitExtraTooltip(int key) {
        return null;
    }

    /**
     * Hit testing: returns a non-negative key indicating that an item is hovered (semantics decided by
     * subclass), or {@code -1} for none.
     */
    protected abstract int hitTest(float x, float y);

    /**
     * Convert a hit key to tooltip text (use \n to separate multiple lines).
     */
    protected abstract String describeHit(int key);

    public static ResolvedTextStyle textStyle(int argb) {
        return new ResolvedTextStyle(
            1f,
            false,
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

    public static String formatPercent(double ratio) {
        if (Double.isNaN(ratio) || Double.isInfinite(ratio)) {
            return "0%";
        }
        return String.format("%.1f%%", ratio * 100d);
    }

    public static String formatValue(double value) {
        if (Math.abs(value - Math.rint(value)) < 1e-6) {
            return Long.toString((long) Math.rint(value));
        }
        return String.format("%.2f", value);
    }
}
