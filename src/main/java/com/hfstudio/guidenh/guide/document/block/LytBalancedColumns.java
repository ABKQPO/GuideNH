package com.hfstudio.guidenh.guide.document.block;

import com.hfstudio.guidenh.guide.document.LytRect;
import com.hfstudio.guidenh.guide.layout.LayoutContext;
import com.hfstudio.guidenh.guide.layout.Layouts;

/**
 * Places children into up to two columns, always appending the next child to the shorter column.
 * Falls back to a normal vertical stack when two columns do not fit.
 */
public class LytBalancedColumns extends LytBox {

    private static final int DEFAULT_COLUMN_COUNT = 2;

    private int gap;

    @Override
    protected LytRect computeBoxLayout(LayoutContext context, int x, int y, int availableWidth) {
        if (children.isEmpty()) {
            return new LytRect(x, y, 0, 0);
        }

        int columnCount = resolveColumnCount(context, x, y, availableWidth);
        if (columnCount <= 1) {
            return Layouts.verticalLayout(context, children, x, y, availableWidth, 0, 0, 0, 0, gap, AlignItems.START);
        }

        int columnWidth = Math.max(1, (availableWidth - gap * (columnCount - 1)) / columnCount);
        int[] columnBottoms = new int[columnCount];
        LytBlock[] previousBlocks = new LytBlock[columnCount];
        int contentWidth = 0;
        int contentHeight = 0;

        for (LytBlock child : children) {
            int columnIndex = selectShortestColumn(columnBottoms);
            int columnX = x + columnIndex * (columnWidth + gap);
            int columnY = Layouts.offsetIntoContentArea(
                LytAxis.VERTICAL,
                y + columnBottoms[columnIndex],
                previousBlocks[columnIndex],
                child);
            int blockWidth = Math.max(1, columnWidth - child.getMarginLeft() - child.getMarginRight());
            LytRect childBounds = child.layout(context, columnX + child.getMarginLeft(), columnY, blockWidth);
            columnBottoms[columnIndex] = childBounds.bottom() - y + child.getMarginBottom() + gap;
            previousBlocks[columnIndex] = child;
            contentWidth = Math.max(contentWidth, childBounds.right() - x);
            contentHeight = Math.max(contentHeight, childBounds.bottom() - y);
        }

        return new LytRect(x, y, contentWidth, contentHeight);
    }

    public int getGap() {
        return gap;
    }

    public void setGap(int gap) {
        this.gap = gap;
    }

    private int resolveColumnCount(LayoutContext context, int x, int y, int availableWidth) {
        if (children.size() < DEFAULT_COLUMN_COUNT) return 1;

        int maxMeasuredWidth = 0;
        for (LytBlock child : children) {
            int blockWidth = Math.max(1, availableWidth - child.getMarginLeft() - child.getMarginRight());
            LytRect childBounds = child
                .layout(context, x + child.getMarginLeft(), y + child.getMarginTop(), blockWidth);
            maxMeasuredWidth = Math
                .max(maxMeasuredWidth, childBounds.width() + child.getMarginLeft() + child.getMarginRight());
        }

        int twoColumnWidth = Math.max(1, (availableWidth - gap) / DEFAULT_COLUMN_COUNT);
        return maxMeasuredWidth <= twoColumnWidth ? DEFAULT_COLUMN_COUNT : 1;
    }

    private static int selectShortestColumn(int[] columnBottoms) {
        int bestIndex = 0;
        int bestBottom = columnBottoms[0];
        for (int i = 1; i < columnBottoms.length; i++) {
            if (columnBottoms[i] < bestBottom) {
                bestBottom = columnBottoms[i];
                bestIndex = i;
            }
        }
        return bestIndex;
    }
}
