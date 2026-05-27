package com.hfstudio.guidenh.guide.document.block.chart;

import java.util.ArrayList;
import java.util.List;

import org.lwjgl.opengl.GL11;

import com.hfstudio.guidenh.guide.document.LytRect;
import com.hfstudio.guidenh.guide.layout.LayoutContext;
import com.hfstudio.guidenh.guide.render.RenderContext;
import com.hfstudio.guidenh.guide.style.ResolvedTextStyle;

/**
 * Legend rendering: lay out and draw based on position (horizontal at top/bottom, vertical at left/right).
 */
public class ChartLegendRenderer {

    private static final int SWATCH_TEXT_GAP = 4;
    private static final int HORIZONTAL_ROW_GAP = 2;

    protected ChartLegendRenderer() {}

    /** A single legend entry. */
    public static class LegendEntry {

        public final String name;
        public final int color;
        public final ChartIcon icon;

        public LegendEntry(String name, int color) {
            this(name, color, null);
        }

        public LegendEntry(String name, int color, ChartIcon icon) {
            this.name = name != null ? name : "";
            this.color = color;
            this.icon = icon;
        }

        public boolean hasIcon() {
            return icon != null && (icon.hasItemStack() || icon.hasImage());
        }
    }

    /** Layout result for the legend and remaining plot area. */
    public static class Layout {

        public final List<LegendEntry> entries;
        public final ChartLegendPosition position;
        public final LytRect legendRect;
        public final int plotLeft;
        public final int plotTop;
        public final int plotRight;
        public final int plotBottom;

        public Layout(List<LegendEntry> entries, ChartLegendPosition position, LytRect legendRect, int plotLeft,
            int plotTop, int plotRight, int plotBottom) {
            this.entries = entries;
            this.position = position;
            this.legendRect = legendRect;
            this.plotLeft = plotLeft;
            this.plotTop = plotTop;
            this.plotRight = plotRight;
            this.plotBottom = plotBottom;
        }
    }

    public static Layout computeLayout(RenderContext context, List<LegendEntry> entries, ChartLegendPosition position,
        int contentLeft, int contentTop, int contentRight, int contentBottom) {
        if (entries == null || entries.isEmpty() || position == null || position == ChartLegendPosition.NONE) {
            return new Layout(
                entries != null ? entries : new ArrayList<>(),
                ChartLegendPosition.NONE,
                LytRect.empty(),
                contentLeft,
                contentTop,
                contentRight,
                contentBottom);
        }

        ResolvedTextStyle textStyle = LytChartBase.textStyle(0xFFCCCCCC);
        int lineHeight = context.getLineHeight(textStyle);
        int swatch = LytChartBase.LEGEND_SWATCH_SIZE;
        int gap = LytChartBase.LEGEND_GAP;

        switch (position) {
            case TOP:
            case BOTTOM: {
                int height = measureHorizontalLegendHeight(
                    entries,
                    position,
                    Math.max(1, contentRight - contentLeft),
                    context::getStringWidth,
                    lineHeight,
                    swatch,
                    textStyle);
                LytRect rect;
                int plotTop = contentTop;
                int plotBottom = contentBottom;
                if (position == ChartLegendPosition.TOP) {
                    rect = new LytRect(contentLeft, contentTop, contentRight - contentLeft, height);
                    plotTop = contentTop + height + gap;
                } else {
                    rect = new LytRect(contentLeft, contentBottom - height, contentRight - contentLeft, height);
                    plotBottom = contentBottom - height - gap;
                }
                return new Layout(entries, position, rect, contentLeft, plotTop, contentRight, plotBottom);
            }
            case LEFT:
            case RIGHT: {
                int width = 0;
                for (LegendEntry e : entries) {
                    int w = swatch + SWATCH_TEXT_GAP + context.getStringWidth(e.name, textStyle);
                    if (w > width) {
                        width = w;
                    }
                }
                width = Math.max(48, width);
                LytRect rect;
                int plotLeft = contentLeft;
                int plotRight = contentRight;
                if (position == ChartLegendPosition.LEFT) {
                    rect = new LytRect(contentLeft, contentTop, width, contentBottom - contentTop);
                    plotLeft = contentLeft + width + gap;
                } else {
                    rect = new LytRect(contentRight - width, contentTop, width, contentBottom - contentTop);
                    plotRight = contentRight - width - gap;
                }
                return new Layout(entries, position, rect, plotLeft, contentTop, plotRight, contentBottom);
            }
            default:
                return new Layout(
                    entries,
                    ChartLegendPosition.NONE,
                    LytRect.empty(),
                    contentLeft,
                    contentTop,
                    contentRight,
                    contentBottom);
        }
    }

    public static void render(RenderContext context, Layout layout, ResolvedTextStyle styleTemplate) {
        if (layout.position == ChartLegendPosition.NONE || layout.entries.isEmpty()) {
            return;
        }
        ResolvedTextStyle textStyle = LytChartBase.textStyle(0xFFCCCCCC);
        int lineHeight = context.getLineHeight(textStyle);
        int swatch = LytChartBase.LEGEND_SWATCH_SIZE;
        LytRect rect = layout.legendRect;

        switch (layout.position) {
            case TOP:
            case BOTTOM: {
                int totalWidth = 0;
                for (int i = 0; i < layout.entries.size(); i++) {
                    LegendEntry e = layout.entries.get(i);
                    totalWidth += swatch + SWATCH_TEXT_GAP + context.getStringWidth(e.name, textStyle);
                    if (i < layout.entries.size() - 1) {
                        totalWidth += LytChartBase.LEGEND_ENTRY_GAP;
                    }
                }
                int rowHeight = Math.max(swatch, lineHeight);
                int x = rect.x() + Math.max(0, (rect.width() - totalWidth) / 2);
                int y = rect.y();
                int textY = y + (rowHeight - lineHeight) / 2;
                int swY = y + (rowHeight - swatch) / 2;
                for (LegendEntry e : layout.entries) {
                    int itemWidth = swatch + SWATCH_TEXT_GAP + context.getStringWidth(e.name, textStyle);
                    if (x > rect.x() && x + itemWidth > rect.right()) {
                        x = rect.x();
                        y += rowHeight + HORIZONTAL_ROW_GAP;
                        textY = y + (rowHeight - lineHeight) / 2;
                        swY = y + (rowHeight - swatch) / 2;
                    }
                    drawSwatch(context, e, x, swY, swatch);
                    x += swatch + SWATCH_TEXT_GAP;
                    context.drawText(e.name, x, textY, textStyle);
                    x += context.getStringWidth(e.name, textStyle) + LytChartBase.LEGEND_ENTRY_GAP;
                }
                break;
            }
            case LEFT:
            case RIGHT: {
                int x = rect.x();
                int y = rect.y();
                for (LegendEntry e : layout.entries) {
                    int swY = y + (lineHeight - swatch) / 2;
                    drawSwatch(context, e, x, swY, swatch);
                    context.drawText(e.name, x + swatch + SWATCH_TEXT_GAP, y, textStyle);
                    y += lineHeight + 2;
                    if (y + lineHeight > rect.bottom()) {
                        break;
                    }
                }
                break;
            }
            default:
                break;
        }
    }

    public static int measureHeight(LayoutContext context, List<LegendEntry> entries, ChartLegendPosition position,
        int availableWidth) {
        if (entries == null || entries.isEmpty() || position == null || position == ChartLegendPosition.NONE) {
            return 0;
        }
        if (position != ChartLegendPosition.TOP && position != ChartLegendPosition.BOTTOM) {
            return 0;
        }
        ResolvedTextStyle textStyle = LytChartBase.textStyle(0xFFCCCCCC);
        return measureHorizontalLegendHeight(
            entries,
            position,
            Math.max(1, availableWidth),
            (text, style) -> measureTextWidth(context, text, style),
            context.getLineHeight(textStyle),
            LytChartBase.LEGEND_SWATCH_SIZE,
            textStyle);
    }

    private static int measureHorizontalLegendHeight(List<LegendEntry> entries, ChartLegendPosition position,
        int availableWidth, TextWidthMeasure textWidthMeasure, int lineHeight, int swatch,
        ResolvedTextStyle textStyle) {
        if (position != ChartLegendPosition.TOP && position != ChartLegendPosition.BOTTOM) {
            return Math.max(swatch, lineHeight);
        }
        int rowHeight = Math.max(swatch, lineHeight);
        int rows = 1;
        int rowWidth = 0;
        for (LegendEntry entry : entries) {
            int itemWidth = swatch + SWATCH_TEXT_GAP + textWidthMeasure.measure(entry.name, textStyle);
            int nextWidth = rowWidth == 0 ? itemWidth : rowWidth + LytChartBase.LEGEND_ENTRY_GAP + itemWidth;
            if (rowWidth > 0 && nextWidth > availableWidth) {
                rows++;
                rowWidth = itemWidth;
            } else {
                rowWidth = nextWidth;
            }
        }
        return rows * rowHeight + Math.max(0, rows - 1) * HORIZONTAL_ROW_GAP;
    }

    private static int measureTextWidth(LayoutContext context, String text, ResolvedTextStyle style) {
        if (text == null || text.isEmpty()) {
            return 0;
        }
        float width = 0f;
        for (int offset = 0; offset < text.length();) {
            int codePoint = text.codePointAt(offset);
            width += context.getAdvance(codePoint, style);
            offset += Character.charCount(codePoint);
        }
        return Math.round(width);
    }

    private interface TextWidthMeasure {

        int measure(String text, ResolvedTextStyle style);
    }

    private static void drawSwatch(RenderContext context, LegendEntry entry, int x, int y, int size) {
        if (entry.icon != null && entry.icon.hasItemStack()) {
            float scale = (float) size / 16f;
            GL11.glPushMatrix();
            GL11.glTranslatef(x, y, 0f);
            GL11.glScalef(scale, scale, 1f);
            context.renderItem(entry.icon.getStack(), 0, 0);
            GL11.glPopMatrix();
            return;
        }
        if (entry.icon != null && entry.icon.hasImage()) {
            context.fillTexturedRect(new LytRect(x, y, size, size), entry.icon.getTexture());
            return;
        }
        context.fillRect(new LytRect(x, y, size, size), entry.color);
    }
}
