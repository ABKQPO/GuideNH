package com.hfstudio.guidenh.guide.internal.editor.gui;

import java.util.List;

public class SceneEditorElementReorderController {

    private boolean dragging;
    private int draggedIndex;
    private int insertionIndex;

    public SceneEditorElementReorderController() {
        this.dragging = false;
        this.draggedIndex = -1;
        this.insertionIndex = -1;
    }

    public boolean beginDrag(int draggedIndex) {
        if (draggedIndex < 0) {
            return false;
        }
        this.dragging = true;
        this.draggedIndex = draggedIndex;
        this.insertionIndex = draggedIndex;
        return true;
    }

    public boolean isDragging() {
        return dragging;
    }

    public int getDraggedIndex() {
        return draggedIndex;
    }

    public int getInsertionIndex() {
        return insertionIndex;
    }

    public void updateDrag(int mouseY, List<RowMetrics> rows) {
        if (!dragging) {
            return;
        }
        insertionIndex = computeInsertionIndex(mouseY, rows);
    }

    public MoveOperation finishDrag(int mouseY, List<RowMetrics> rows) {
        if (!dragging) {
            return MoveOperation.none();
        }
        updateDrag(mouseY, rows);
        MoveOperation operation = toMoveOperation(draggedIndex, insertionIndex, rows.size());
        cancelDrag();
        return operation;
    }

    public void cancelDrag() {
        dragging = false;
        draggedIndex = -1;
        insertionIndex = -1;
    }

    private int computeInsertionIndex(int mouseY, List<RowMetrics> rows) {
        for (int i = 0; i < rows.size(); i++) {
            if (mouseY < rows.get(i)
                .getCenterY()) {
                return i;
            }
        }
        return rows.size();
    }

    private MoveOperation toMoveOperation(int fromIndex, int insertionIndex, int rowCount) {
        if (fromIndex < 0 || rowCount <= 0) {
            return MoveOperation.none();
        }

        int boundedInsertion = insertionIndex;
        if (boundedInsertion < 0) {
            boundedInsertion = 0;
        } else if (boundedInsertion > rowCount) {
            boundedInsertion = rowCount;
        }

        if (boundedInsertion == fromIndex || boundedInsertion == fromIndex + 1) {
            return MoveOperation.none();
        }

        int toIndex = boundedInsertion < fromIndex ? boundedInsertion : boundedInsertion - 1;
        return new MoveOperation(fromIndex, toIndex);
    }

    public static class RowMetrics {

        private final int top;
        private final int height;

        public RowMetrics(int top, int height) {
            this.top = top;
            this.height = height;
        }

        public int getTop() {
            return top;
        }

        public int getHeight() {
            return height;
        }

        public int getCenterY() {
            return top + height / 2;
        }
    }

    public static class MoveOperation {

        private final int fromIndex;
        private final int toIndex;

        public MoveOperation(int fromIndex, int toIndex) {
            this.fromIndex = fromIndex;
            this.toIndex = toIndex;
        }

        public int getFromIndex() {
            return fromIndex;
        }

        public int getToIndex() {
            return toIndex;
        }

        public boolean hasMove() {
            return fromIndex >= 0 && toIndex >= 0 && fromIndex != toIndex;
        }

        public static MoveOperation none() {
            return new MoveOperation(-1, -1);
        }
    }
}
