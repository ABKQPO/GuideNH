package com.hfstudio.guidenh.guide.document.block.chart;

import java.util.List;

import com.hfstudio.guidenh.guide.document.LytRect;
import com.hfstudio.guidenh.guide.render.RenderContext;
import com.hfstudio.guidenh.guide.style.ResolvedTextStyle;

public class CornerLegendRenderer {

    public static final int DEFAULT_WIDTH = 120;
    public static final int DEFAULT_HEIGHT = 64;
    public static final int DEFAULT_BACKGROUND = 0xAA111922;

    private static final int PADDING_X = 5;
    private static final int PADDING_Y = 4;
    private static final int GAP = 4;
    private static final int ENTRY_GAP = 2;
    private static final int MARKER_WIDTH = 10;
    private static final int MARKER_HEIGHT = 6;
    private static final int MIN_WIDTH = 24;
    private static final int MIN_HEIGHT = 12;
    private static final ResolvedTextStyle TEXT_STYLE = LytChartBase.textStyle(0xFFFFFFFF);

    protected CornerLegendRenderer() {}

    public static void render(RenderContext context, LytRect plotRect, List<CornerLegendEntry> entries,
        CornerLegendPosition position, int maxWidth, int maxHeight, int backgroundColor) {
        if (position == null || position == CornerLegendPosition.NONE
            || entries == null
            || entries.isEmpty()
            || plotRect.isEmpty()) {
            return;
        }
        int lineHeight = context.getLineHeight(TEXT_STYLE);
        if (plotRect.width() < MIN_WIDTH || plotRect.height() < MIN_HEIGHT) {
            return;
        }
        int width = clamp(maxWidth > 0 ? maxWidth : DEFAULT_WIDTH, MIN_WIDTH, plotRect.width());
        int height = clamp(maxHeight > 0 ? maxHeight : DEFAULT_HEIGHT, MIN_HEIGHT, plotRect.height());
        int entryCapacity = Math.max(0, (height - PADDING_Y * 2 + ENTRY_GAP) / Math.max(1, lineHeight + ENTRY_GAP));
        if (entryCapacity <= 0) {
            return;
        }
        int visibleCount = 0;
        for (CornerLegendEntry entry : entries) {
            if (entry != null && entry.isVisible()) {
                visibleCount++;
                if (visibleCount >= entryCapacity) {
                    break;
                }
            }
        }
        if (visibleCount == 0) {
            return;
        }
        int contentHeight = visibleCount * lineHeight + Math.max(0, visibleCount - 1) * ENTRY_GAP;
        height = Math.min(height, contentHeight + PADDING_Y * 2);
        int x = switch (position) {
            case TOP_LEFT, BOTTOM_LEFT -> plotRect.x() + GAP;
            case TOP_RIGHT, BOTTOM_RIGHT -> plotRect.right() - width - GAP;
            default -> throw new IllegalStateException("Unexpected value: " + position);
        };
        int y = switch (position) {
            case TOP_LEFT, TOP_RIGHT -> plotRect.y() + GAP;
            case BOTTOM_LEFT, BOTTOM_RIGHT -> plotRect.bottom() - height - GAP;
            default -> throw new IllegalStateException("Unexpected value: " + position);
        };
        x = clamp(x, plotRect.x(), plotRect.right() - width);
        y = clamp(y, plotRect.y(), plotRect.bottom() - height);
        LytRect box = new LytRect(x, y, width, height);
        context.fillRect(box, backgroundColor);
        context.drawBorder(box, 0x66FFFFFF, 1);

        int textX = x + PADDING_X + MARKER_WIDTH + GAP;
        int maxTextWidth = Math.max(0, x + width - PADDING_X - textX);
        int rowY = y + PADDING_Y;
        int drawn = 0;
        for (CornerLegendEntry entry : entries) {
            if (entry == null || !entry.isVisible()) {
                continue;
            }
            if (drawn >= entryCapacity) {
                break;
            }
            int markerX = x + PADDING_X;
            int markerY = rowY + Math.max(0, (lineHeight - MARKER_HEIGHT) / 2);
            if (entry.lineMarker()) {
                float cy = markerY + MARKER_HEIGHT / 2f;
                context.drawLine(markerX, cy, markerX + MARKER_WIDTH, cy, 1.5f, entry.color());
            } else {
                context.fillRect(markerX + 2, markerY, MARKER_HEIGHT, MARKER_HEIGHT, entry.color());
            }
            context.drawText(ellipsize(context, entry.name(), maxTextWidth), textX, rowY, TEXT_STYLE);
            rowY += lineHeight + ENTRY_GAP;
            drawn++;
        }
    }

    public static String ellipsize(RenderContext context, String text, int maxWidth) {
        if (text == null || text.isEmpty() || maxWidth <= 0) {
            return "";
        }
        if (context.getStringWidth(text, TEXT_STYLE) <= maxWidth) {
            return text;
        }
        String suffix = "...";
        int suffixWidth = context.getStringWidth(suffix, TEXT_STYLE);
        if (suffixWidth > maxWidth) {
            return "";
        }
        int end = text.length();
        while (end > 0) {
            String candidate = text.substring(0, end) + suffix;
            if (context.getStringWidth(candidate, TEXT_STYLE) <= maxWidth) {
                return candidate;
            }
            end--;
        }
        return suffix;
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
