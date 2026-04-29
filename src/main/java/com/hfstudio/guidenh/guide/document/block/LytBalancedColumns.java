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

        for (LytBlock child : children) {
            int blockWidth = Math.max(1, columnWidth - child.getMarginLeft() - child.getMarginRight());

            int leftColumnX = x + child.getMarginLeft();
            int leftColumnY = Layouts
                .offsetIntoContentArea(LytAxis.VERTICAL, y + columnBottoms[0], previousBlocks[0], child);
            LytRect childBounds = child.layout(context, leftColumnX, leftColumnY, blockWidth);
            int occupiedWidth = childBounds.width() + child.getMarginLeft() + child.getMarginRight();
            if (occupiedWidth > columnWidth) {
                return verticalFallback(context, x, y, availableWidth);
            }

            int leftProjectedBottom = childBounds.bottom() - y + child.getMarginBottom() + gap;

            int rightColumnX = x + columnWidth + gap + child.getMarginLeft();
            int rightColumnY = Layouts
                .offsetIntoContentArea(LytAxis.VERTICAL, y + columnBottoms[1], previousBlocks[1], child);
            int rightProjectedBottom = rightColumnY - y + childBounds.height() + child.getMarginBottom() + gap;

            int columnIndex = selectColumn(
                columnBottoms[0],
                columnBottoms[1],
                leftProjectedBottom,
                rightProjectedBottom,
                columnCounts[0],
                columnCounts[1]);

            if (columnIndex == 1) {
                child.setLayoutPos(
                    childBounds.point()
                        .add(rightColumnX - leftColumnX, rightColumnY - leftColumnY));
                childBounds = child.getBounds();
            }

            columnBottoms[columnIndex] = columnIndex == 0 ? leftProjectedBottom : rightProjectedBottom;
            columnCounts[columnIndex]++;
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

    private LytRect verticalFallback(LayoutContext context, int x, int y, int availableWidth) {
        return Layouts.verticalLayout(context, children, x, y, availableWidth, 0, 0, 0, 0, gap, AlignItems.START);
    }

    private static int selectColumn(int leftBottom, int rightBottom, int leftProjectedBottom, int rightProjectedBottom,
        int leftCount, int rightCount) {
        int leftProjectedHeight = Math.max(leftProjectedBottom, rightBottom);
        int rightProjectedHeight = Math.max(leftBottom, rightProjectedBottom);
        if (leftProjectedHeight < rightProjectedHeight) {
            return 0;
        }
        if (rightProjectedHeight < leftProjectedHeight) {
            return 1;
        }

        int leftProjectedGap = Math.abs(leftProjectedBottom - rightBottom);
        int rightProjectedGap = Math.abs(leftBottom - rightProjectedBottom);
        if (leftProjectedGap < rightProjectedGap) {
            return 0;
        }
        if (rightProjectedGap < leftProjectedGap) {
            return 1;
        }

        if (leftCount < rightCount) {
            return 0;
        }
        if (rightCount < leftCount) {
            return 1;
        }

        return 0;
    }
}
