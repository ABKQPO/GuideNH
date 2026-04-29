package com.hfstudio.guidenh.guide.document.block;

import com.hfstudio.guidenh.guide.document.LytRect;
import com.hfstudio.guidenh.guide.layout.LayoutContext;
import com.hfstudio.guidenh.guide.layout.Layouts;

/**
 * Places children into up to two columns, always preferring the left-most column when it has the
 * same or more free vertical space than the right column. Falls back to a normal vertical stack
 * when any child cannot fit inside a half-width column.
 */
public class LytBalancedColumns extends LytBox {

    private static final int DEFAULT_COLUMN_COUNT = 2;
    private static final int MIN_LEFT_PRIORITY_SLACK = 8;
    private static final int LEFT_PRIORITY_HEIGHT_DIVISOR = 4;

    private int gap;

    @Override
    protected LytRect computeBoxLayout(LayoutContext context, int x, int y, int availableWidth) {
        if (children.isEmpty()) {
            return new LytRect(x, y, 0, 0);
        }

        if (children.size() < DEFAULT_COLUMN_COUNT) {
            return verticalFallback(context, x, y, availableWidth);
        }

        int columnWidth = Math.max(1, (availableWidth - gap * (DEFAULT_COLUMN_COUNT - 1)) / DEFAULT_COLUMN_COUNT);
        int[] columnBottoms = new int[DEFAULT_COLUMN_COUNT];
        int[] columnCounts = new int[DEFAULT_COLUMN_COUNT];
        LytBlock[] previousBlocks = new LytBlock[DEFAULT_COLUMN_COUNT];
        int contentWidth = 0;
        int contentHeight = 0;
        int placedCount = 0;
        int totalPlacedHeight = 0;

        for (LytBlock child : children) {
            int columnIndex = selectColumn(
                columnBottoms,
                columnCounts,
                resolveLeftPrioritySlack(totalPlacedHeight, placedCount, gap));
            int columnX = x + columnIndex * (columnWidth + gap);
            int columnY = Layouts.offsetIntoContentArea(
                LytAxis.VERTICAL,
                y + columnBottoms[columnIndex],
                previousBlocks[columnIndex],
                child);
            int blockWidth = Math.max(1, columnWidth - child.getMarginLeft() - child.getMarginRight());
            LytRect childBounds = child.layout(context, columnX + child.getMarginLeft(), columnY, blockWidth);
            int occupiedWidth = childBounds.width() + child.getMarginLeft() + child.getMarginRight();
            if (occupiedWidth > columnWidth) {
                return verticalFallback(context, x, y, availableWidth);
            }
            columnBottoms[columnIndex] = childBounds.bottom() - y + child.getMarginBottom() + gap;
            columnCounts[columnIndex]++;
            previousBlocks[columnIndex] = child;
            contentWidth = Math.max(contentWidth, childBounds.right() - x);
            contentHeight = Math.max(contentHeight, childBounds.bottom() - y);
            placedCount++;
            totalPlacedHeight += childBounds.height();
        }

        return new LytRect(x, y, contentWidth, contentHeight);
    }

    public int getGap() {
        return gap;
    }

    public void setGap(int gap) {
        this.gap = gap;
    }

    private LytRect verticalFallback(LayoutContext context, int x, int y, int availableWidth) {
        return Layouts.verticalLayout(context, children, x, y, availableWidth, 0, 0, 0, 0, gap, AlignItems.START);
    }

    private static int selectColumn(int[] columnBottoms, int[] columnCounts, int leftPrioritySlack) {
        int leftBottom = columnBottoms[0];
        int rightBottom = columnBottoms[1];
        if (leftBottom <= rightBottom) {
            return 0;
        }
        boolean startsFreshRowOnLeft = columnCounts[0] == columnCounts[1];
        if (startsFreshRowOnLeft && leftBottom - rightBottom <= leftPrioritySlack) {
            return 0;
        }
        return 1;
    }

    private static int resolveLeftPrioritySlack(int totalPlacedHeight, int placedCount, int gap) {
        if (placedCount <= 0) {
            return Math.max(MIN_LEFT_PRIORITY_SLACK, gap * 2);
        }
        int averageHeight = Math.max(1, totalPlacedHeight / placedCount);
        return Math.max(MIN_LEFT_PRIORITY_SLACK, Math.max(gap * 2, averageHeight / LEFT_PRIORITY_HEIGHT_DIVISOR));
    }
}
