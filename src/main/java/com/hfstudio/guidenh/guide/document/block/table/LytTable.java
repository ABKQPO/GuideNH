package com.hfstudio.guidenh.guide.document.block.table;

import java.util.ArrayList;
import java.util.List;

import com.hfstudio.guidenh.guide.color.SymbolicColor;
import com.hfstudio.guidenh.guide.document.LytRect;
import com.hfstudio.guidenh.guide.document.block.LytBlock;
import com.hfstudio.guidenh.guide.layout.LayoutContext;
import com.hfstudio.guidenh.guide.render.RenderContext;

public class LytTable extends LytBlock {

    /**
     * Width of border around cells.
     */
    public static final int CELL_BORDER = 1;
    private final List<LytTableRow> rows = new ArrayList<>();

    private final List<LytTableColumn> columns = new ArrayList<>();

    @Override
    protected LytRect computeLayout(LayoutContext context, int x, int y, int availableWidth) {
        if (columns.isEmpty()) {
            return LytRect.empty();
        }

        layoutColumns(x, availableWidth);

        // Layout each row
        var currentY = y + CELL_BORDER;
        for (var row : rows) {
            var rowTop = currentY;
            var rowBottom = currentY;
            for (var cell : row.getChildren()) {
                var column = cell.column;
                var cellBounds = cell.layout(context, column.x, currentY, column.width);
                rowBottom = Math.max(rowBottom, cellBounds.bottom());
            }
            row.bounds = new LytRect(x, rowTop, availableWidth, rowBottom - rowTop);
            currentY = rowBottom + CELL_BORDER;
        }

        return new LytRect(x, y, availableWidth, currentY - y);
    }

    @Override
    protected void onLayoutMoved(int deltaX, int deltaY) {
        for (var col : columns) {
            col.x += deltaX;
        }
        for (var row : rows) {
            row.bounds = row.bounds.move(deltaX, deltaY);
            for (var cell : row.getChildren()) {
                cell.setLayoutPos(
                    cell.getBounds()
                        .point()
                        .add(deltaX, deltaY));
            }
        }
    }

    @Override
    public void render(RenderContext context) {
        // Render the table cell borders
        var bounds = getBounds();
        for (int i = 0; i < columns.size() - 1; i++) {
            var column = columns.get(i);
            var colRight = column.x + column.width;
            context.fillRect(colRight, bounds.y(), 1, bounds.height(), SymbolicColor.TABLE_BORDER);
        }

        for (int i = 0; i < rows.size() - 1; i++) {
            var row = rows.get(i);
            context.fillRect(bounds.x(), row.bounds.bottom(), bounds.width(), 1, SymbolicColor.TABLE_BORDER);
        }

        for (var row : rows) {
            for (var cell : row.getChildren()) {
                cell.render(context);
            }
        }
    }

    public LytTableRow appendRow() {
        var row = new LytTableRow(this);
        rows.add(row);
        return row;
    }

    public List<LytTableColumn> getColumns() {
        return columns;
    }

    public LytTableColumn getOrCreateColumn(int index) {
        while (index >= columns.size()) {
            columns.add(new LytTableColumn());
        }
        return columns.get(index);
    }

    private void layoutColumns(int x, int availableWidth) {
        int innerWidth = Math.max(0, availableWidth - (columns.size() + 1) * CELL_BORDER);
        int totalPreferredWidth = 0;
        int flexibleColumns = 0;
        for (var column : columns) {
            if (column.preferredWidth > 0) {
                totalPreferredWidth += column.preferredWidth;
            } else {
                flexibleColumns++;
            }
        }

        int colX = x + CELL_BORDER;
        if (totalPreferredWidth > 0 && totalPreferredWidth <= innerWidth) {
            int remainingWidth = innerWidth - totalPreferredWidth;
            int flexibleWidth = flexibleColumns > 0 ? remainingWidth / flexibleColumns : 0;
            int assignedWidth = 0;
            for (var column : columns) {
                column.x = colX;
                column.width = column.preferredWidth > 0 ? column.preferredWidth : flexibleWidth;
                assignedWidth += column.width;
                colX += column.width + CELL_BORDER;
            }

            if (assignedWidth < innerWidth) {
                var lastCol = columns.get(columns.size() - 1);
                lastCol.width += innerWidth - assignedWidth;
            }
            return;
        }

        int cellWidth = columns.isEmpty() ? 0 : innerWidth / columns.size();
        for (var column : columns) {
            column.x = colX;
            column.width = cellWidth;
            colX += column.width + CELL_BORDER;
        }

        var lastCol = columns.get(columns.size() - 1);
        lastCol.width = (x + availableWidth) - lastCol.x - CELL_BORDER;
    }

    @Override
    public List<LytTableRow> getChildren() {
        return rows;
    }
}
