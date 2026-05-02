package com.hfstudio.guidenh.guide.document.block;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.guide.color.SymbolicColor;
import com.hfstudio.guidenh.guide.document.LytPoint;
import com.hfstudio.guidenh.guide.document.LytRect;
import com.hfstudio.guidenh.guide.internal.markdown.FileTreeParser.SlotKind;
import com.hfstudio.guidenh.guide.layout.LayoutContext;
import com.hfstudio.guidenh.guide.render.RenderContext;

/**
 * A block that renders a file tree as a stack of rows where each row carries a configurable depth
 * of connector lines drawn directly with {@link RenderContext#fillRect}, an optional icon block
 * and a {@link LytParagraph} payload re-parsed from inline markdown.
 *
 * <p>
 * Connectors are derived strictly from the slot kinds parsed for each row, so the visual output is
 * deterministic with respect to the source.
 */
public class LytFileTree extends LytBlock {

    private static final int DEFAULT_INDENT_PX = 14;
    private static final int DEFAULT_ROW_GAP_PX = 0;
    private static final int DEFAULT_ICON_BOX_PX = 16;
    private static final int DEFAULT_ICON_GAP_PX = 4;
    private static final int CONNECTOR_THICKNESS = 1;

    private final List<Row> rows = new ArrayList<>();
    private int indentPx = DEFAULT_INDENT_PX;
    private int rowGapPx = DEFAULT_ROW_GAP_PX;
    private int iconBoxPx = DEFAULT_ICON_BOX_PX;
    private int iconGapPx = DEFAULT_ICON_GAP_PX;

    public void appendRow(List<SlotKind> slots, @Nullable LytBlock iconBlock, LytParagraph payload) {
        Row row = new Row(new ArrayList<>(slots), iconBlock, payload);
        rows.add(row);
    }

    public void setIndentPx(int indentPx) {
        if (indentPx > 0) {
            this.indentPx = indentPx;
        }
    }

    public void setRowGapPx(int rowGapPx) {
        this.rowGapPx = Math.max(0, rowGapPx);
    }

    public int getIndentPx() {
        return indentPx;
    }

    public int getRowGapPx() {
        return rowGapPx;
    }

    public boolean isEmpty() {
        return rows.isEmpty();
    }

    @Override
    protected LytRect computeLayout(LayoutContext context, int x, int y, int availableWidth) {
        int currentY = y;
        int totalHeight = 0;

        for (int i = 0; i < rows.size(); i++) {
            Row row = rows.get(i);
            int iconX = x + row.slots.size() * indentPx;
            int payloadX;
            if (row.iconBlock != null) {
                // Give the icon enough headroom so its natural width can be measured. Cap it to
                // half of the remaining row space so a runaway label cannot eat the whole row.
                int iconAvailable = Math.max(iconBoxPx, (x + availableWidth - iconX) / 2);
                row.iconBlock.layout(context, iconX, currentY, Math.max(1, iconAvailable));
                int actualIconWidth = row.iconBlock.getBounds()
                    .width();
                int reservedIconWidth = Math.max(iconBoxPx, actualIconWidth);
                payloadX = iconX + reservedIconWidth + iconGapPx;
            } else {
                payloadX = iconX;
            }
            int payloadAvailable = Math.max(1, x + availableWidth - payloadX);
            LytRect payloadBounds = row.payload.layout(context, payloadX, currentY, payloadAvailable);
            int rowHeight = Math.max(
                payloadBounds.height(),
                row.iconBlock != null ? row.iconBlock.getBounds()
                    .height() : 0);
            if (rowHeight <= 0) {
                rowHeight = 1;
            }
            row.rowY = currentY;
            row.rowHeight = rowHeight;
            currentY += rowHeight;
            totalHeight += rowHeight;
            if (i < rows.size() - 1) {
                currentY += rowGapPx;
                totalHeight += rowGapPx;
            }
        }

        return new LytRect(x, y, availableWidth, totalHeight);
    }

    @Override
    protected void onLayoutMoved(int deltaX, int deltaY) {
        for (Row row : rows) {
            row.rowY += deltaY;
            if (row.iconBlock != null) {
                row.iconBlock.setLayoutPos(
                    new LytPoint(
                        row.iconBlock.getBounds()
                            .x() + deltaX,
                        row.iconBlock.getBounds()
                            .y() + deltaY));
            }
            row.payload.setLayoutPos(
                new LytPoint(
                    row.payload.getBounds()
                        .x() + deltaX,
                    row.payload.getBounds()
                        .y() + deltaY));
        }
    }

    @Override
    public void render(RenderContext context) {
        renderConnectors(context);
        for (Row row : rows) {
            if (row.iconBlock != null) {
                row.iconBlock.render(context);
            }
            row.payload.render(context);
        }
    }

    private void renderConnectors(RenderContext context) {
        int baseX = bounds.x();
        // Resolve symbolic color once per frame instead of on every fillRect.
        int connectorColor = context.resolveColor(SymbolicColor.TABLE_BORDER);
        int halfIndent = indentPx / 2;
        for (Row row : rows) {
            int rowY = row.rowY;
            int rowHeight = row.rowHeight;
            int rowMidY = rowY + rowHeight / 2;
            int rowBottomY = rowY + rowHeight + rowGapPx;
            int slotCount = row.slots.size();
            int columnCenterX = baseX + halfIndent;
            for (int slotIndex = 0; slotIndex < slotCount; slotIndex++, columnCenterX += indentPx) {
                SlotKind slot = row.slots.get(slotIndex);
                switch (slot) {
                    case VERTICAL -> drawVerticalLine(context, columnCenterX, rowY, rowBottomY, connectorColor);
                    case BRANCH -> {
                        drawVerticalLine(context, columnCenterX, rowY, rowBottomY, connectorColor);
                        drawHorizontalLine(
                            context,
                            columnCenterX,
                            columnCenterX - halfIndent + indentPx,
                            rowMidY,
                            connectorColor);
                    }
                    case LAST_BRANCH -> {
                        drawVerticalLine(context, columnCenterX, rowY, rowMidY + CONNECTOR_THICKNESS, connectorColor);
                        drawHorizontalLine(
                            context,
                            columnCenterX,
                            columnCenterX - halfIndent + indentPx,
                            rowMidY,
                            connectorColor);
                    }
                    case EMPTY -> {
                        // Empty slot draws nothing.
                    }
                }
            }
        }
    }

    private static void drawVerticalLine(RenderContext context, int x, int yStart, int yEnd, int color) {
        int top = Math.min(yStart, yEnd);
        int height = Math.abs(yEnd - yStart);
        if (height <= 0) {
            return;
        }
        context.fillRect(new LytRect(x, top, CONNECTOR_THICKNESS, height), color);
    }

    private static void drawHorizontalLine(RenderContext context, int xStart, int xEnd, int y, int color) {
        int left = Math.min(xStart, xEnd);
        int width = Math.abs(xEnd - xStart);
        if (width <= 0) {
            return;
        }
        context.fillRect(new LytRect(left, y, width, CONNECTOR_THICKNESS), color);
    }

    private static final class Row {

        final List<SlotKind> slots;
        @Nullable
        final LytBlock iconBlock;
        final LytParagraph payload;
        int rowY;
        int rowHeight;

        Row(List<SlotKind> slots, @Nullable LytBlock iconBlock, LytParagraph payload) {
            this.slots = slots;
            this.iconBlock = iconBlock;
            this.payload = payload;
        }
    }
}
