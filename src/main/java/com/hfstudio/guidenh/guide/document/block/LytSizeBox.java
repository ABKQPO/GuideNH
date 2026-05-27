package com.hfstudio.guidenh.guide.document.block;

import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.guide.document.LytRect;
import com.hfstudio.guidenh.guide.document.interaction.DocumentDragTarget;
import com.hfstudio.guidenh.guide.internal.editor.gui.SceneEditorVerticalScrollbar;
import com.hfstudio.guidenh.guide.layout.LayoutContext;
import com.hfstudio.guidenh.guide.render.RenderContext;

public class LytSizeBox extends LytVBox implements DocumentDragTarget {

    private static final int SCROLLBAR_WIDTH = 5;
    private static final int SCROLLBAR_GAP = 4;
    private static final int MIN_WHEEL_STEP = 16;

    private final BorderRenderer borderRenderer = new BorderRenderer();

    private int preferredWidth;
    private int preferredHeight;
    private int contentHeight;
    private int viewportX;
    private int viewportY;
    private int viewportWidth;
    private int viewportHeight;
    private int scrollOffsetY;
    private int appliedScrollOffsetY;
    private boolean draggingContent;
    private int dragLastDocumentY;
    private boolean draggingScrollbar;
    private int scrollbarGrabOffsetY;

    public int getPreferredWidth() {
        return preferredWidth;
    }

    public void setPreferredWidth(int preferredWidth) {
        this.preferredWidth = Math.max(0, preferredWidth);
    }

    public int getPreferredHeight() {
        return preferredHeight;
    }

    public void setPreferredHeight(int preferredHeight) {
        this.preferredHeight = Math.max(0, preferredHeight);
    }

    @Override
    protected LytRect computeBoxLayout(LayoutContext context, int x, int y, int availableWidth) {
        int constrainedWidth = preferredWidth > 0 ? Math.min(availableWidth, preferredWidth) : availableWidth;
        int measuredWidth = Math.max(1, constrainedWidth);
        viewportX = x;
        viewportY = y;
        viewportWidth = measuredWidth;
        appliedScrollOffsetY = 0;

        LytRect contentBounds = super.computeBoxLayout(context, x, y, measuredWidth);
        contentHeight = contentBounds.height();
        viewportHeight = preferredHeight > 0 ? preferredHeight : contentHeight;

        if (preferredHeight > 0 && contentHeight > viewportHeight) {
            viewportWidth = Math.max(1, measuredWidth - SCROLLBAR_WIDTH - SCROLLBAR_GAP);
            contentBounds = super.computeBoxLayout(context, x, y, viewportWidth);
            contentHeight = contentBounds.height();
        }

        viewportHeight = preferredHeight > 0 ? preferredHeight : contentHeight;
        setScrollOffset(scrollOffsetY);

        int totalWidth = preferredWidth > 0 ? measuredWidth
            : contentBounds.width() + (hasVerticalScroll() ? SCROLLBAR_WIDTH + SCROLLBAR_GAP : 0);
        return new LytRect(x, y, totalWidth, viewportHeight);
    }

    @Override
    public void render(RenderContext context) {
        if (!hasVerticalScroll()) {
            super.render(context);
            return;
        }

        if (getBackgroundColor() != null) {
            context.fillRect(bounds, getBackgroundColor());
        }

        LytRect viewportBounds = getViewportBounds();
        context.pushLocalScissor(viewportBounds);
        try {
            for (LytBlock child : children) {
                child.render(context);
            }
        } finally {
            context.popScissor();
        }

        renderScrollbar(context);
        renderBorder(context);
    }

    @Override
    public boolean beginDrag(int documentX, int documentY, int button) {
        if (!hasVerticalScroll() || button != 0) {
            return false;
        }

        LytRect trackBounds = getScrollbarTrackBounds();
        if (trackBounds.contains(documentX, documentY)) {
            LytRect thumbBounds = getScrollbarThumbBounds();
            if (!thumbBounds.isEmpty() && thumbBounds.contains(documentX, documentY)) {
                scrollbarGrabOffsetY = documentY - thumbBounds.y();
            } else {
                scrollbarGrabOffsetY = thumbBounds.isEmpty() ? 0 : thumbBounds.height() / 2;
                updateScrollFromMouseY(documentY);
            }
            draggingScrollbar = true;
            draggingContent = false;
            return true;
        }

        if (!getViewportBounds().contains(documentX, documentY)) {
            return false;
        }

        draggingContent = true;
        draggingScrollbar = false;
        dragLastDocumentY = documentY;
        return true;
    }

    @Override
    public void dragTo(int documentX, int documentY) {
        if (draggingScrollbar) {
            updateScrollFromMouseY(documentY);
            return;
        }
        if (!draggingContent) {
            return;
        }

        int deltaY = documentY - dragLastDocumentY;
        dragLastDocumentY = documentY;
        setScrollOffset(scrollOffsetY - deltaY);
    }

    @Override
    public void endDrag() {
        draggingContent = false;
        draggingScrollbar = false;
    }

    @Override
    public boolean scroll(int documentX, int documentY, int wheelDelta) {
        if (wheelDelta == 0 || !hasVerticalScroll() || !getViewportBounds().contains(documentX, documentY)) {
            return false;
        }
        setScrollOffset(scrollOffsetY - Integer.signum(wheelDelta) * MIN_WHEEL_STEP);
        return true;
    }

    @Override
    public @Nullable LytNode pickNode(int x, int y) {
        if (!bounds.contains(x, y)) {
            return null;
        }
        if (getScrollbarTrackBounds().contains(x, y)) {
            return this;
        }
        if (!getViewportBounds().contains(x, y)) {
            return this;
        }

        for (LytNode child : getChildren()) {
            LytNode node = child.pickNode(x, y);
            if (node != null) {
                return node;
            }
        }
        return this;
    }

    private void renderScrollbar(RenderContext context) {
        LytRect trackBounds = getScrollbarTrackBounds();
        if (trackBounds.isEmpty()) {
            return;
        }

        context.fillRect(trackBounds, 0x30242B33);
        LytRect thumbBounds = getScrollbarThumbBounds();
        if (!thumbBounds.isEmpty()) {
            context.fillRect(thumbBounds, draggingScrollbar ? 0xFFCDD6E1 : 0xA0AAB5C2);
        }
    }

    private void renderBorder(RenderContext context) {
        if (getBorderTop().width() > 0 || getBorderLeft().width() > 0
            || getBorderRight().width() > 0
            || getBorderBottom().width() > 0) {
            borderRenderer
                .render(context, bounds, getBorderTop(), getBorderLeft(), getBorderRight(), getBorderBottom());
        }
    }

    private void setScrollOffset(int scrollOffsetY) {
        this.scrollOffsetY = SceneEditorVerticalScrollbar.clamp(scrollOffsetY, 0, getMaxScrollOffset());
        int deltaY = appliedScrollOffsetY - this.scrollOffsetY;
        if (deltaY != 0) {
            for (LytBlock child : children) {
                child.moveLayoutPos(0, deltaY);
            }
            appliedScrollOffsetY = this.scrollOffsetY;
        }
    }

    private void updateScrollFromMouseY(int mouseY) {
        LytRect trackBounds = getScrollbarTrackBounds();
        if (trackBounds.isEmpty()) {
            setScrollOffset(0);
            return;
        }

        setScrollOffset(
            SceneEditorVerticalScrollbar.offsetFromDrag(
                mouseY,
                scrollbarGrabOffsetY,
                trackBounds.y(),
                trackBounds.height(),
                contentHeight,
                viewportHeight));
    }

    private int getMaxScrollOffset() {
        return Math.max(0, contentHeight - viewportHeight);
    }

    private boolean hasVerticalScroll() {
        return getMaxScrollOffset() > 0;
    }

    private LytRect getViewportBounds() {
        return new LytRect(viewportX, viewportY, viewportWidth, viewportHeight);
    }

    private LytRect getScrollbarTrackBounds() {
        if (!hasVerticalScroll()) {
            return LytRect.empty();
        }
        return new LytRect(viewportX + viewportWidth + SCROLLBAR_GAP, viewportY, SCROLLBAR_WIDTH, viewportHeight);
    }

    private LytRect getScrollbarThumbBounds() {
        LytRect trackBounds = getScrollbarTrackBounds();
        if (trackBounds.isEmpty()) {
            return LytRect.empty();
        }

        SceneEditorVerticalScrollbar.Thumb thumb = SceneEditorVerticalScrollbar
            .computeThumb(trackBounds.y(), trackBounds.height(), contentHeight, viewportHeight, scrollOffsetY);
        return new LytRect(trackBounds.x(), thumb.start(), trackBounds.width(), thumb.size());
    }
}
