package com.hfstudio.guidenh.guide.document.block.chart;

import java.util.ArrayList;
import java.util.List;

import org.lwjgl.opengl.GL11;

import com.hfstudio.guidenh.guide.document.LytRect;
import com.hfstudio.guidenh.guide.render.RenderContext;
import com.hfstudio.guidenh.guide.style.ResolvedTextStyle;

/**
 * Legend rendering: lay out and draw based on position (horizontal at top/bottom, vertical at left/right).
 */
public final class ChartLegendRenderer {

    private ChartLegendRenderer() {}

    /** A single legend entry. */
    public static final class LegendEntry {

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
    public static final class Layout {

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
                int height = Math.max(swatch, lineHeight);
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
                    int w = swatch + 4 + context.getStringWidth(e.name, textStyle);
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
                    totalWidth += swatch + 4 + context.getStringWidth(e.name, textStyle);
                    if (i < layout.entries.size() - 1) {
                        totalWidth += LytChartBase.LEGEND_ENTRY_GAP;
                    }
                }
                int x = rect.x() + Math.max(0, (rect.width() - totalWidth) / 2);
                int y = rect.y() + (rect.height() - Math.max(swatch, lineHeight)) / 2;
                int textY = y + (Math.max(swatch, lineHeight) - lineHeight) / 2;
                int swY = y + (Math.max(swatch, lineHeight) - swatch) / 2;
                for (LegendEntry e : layout.entries) {
                    drawSwatch(context, e, x, swY, swatch);
                    x += swatch + 4;
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
                    context.drawText(e.name, x + swatch + 4, y, textStyle);
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
