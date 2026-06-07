package com.hfstudio.guidenh.guide.internal.host;

import com.hfstudio.guidenh.guide.document.LytRect;

public class ViewportState {

    private int scrollY;
    private int viewportWidth;
    private int viewportHeight;
    private int contentWidth;
    private int contentHeight;
    private boolean layoutDirty;

    public void updateViewport(int width, int height) {
        this.viewportWidth = width;
        this.viewportHeight = height;
    }

    public void updateContent(int width, int height) {
        this.contentWidth = width;
        this.contentHeight = height;
    }

    public int scrollY() {
        return scrollY;
    }

    public void scrollTo(int y) {
        this.scrollY = clampScroll(y);
    }

    public void scrollBy(int delta) {
        scrollTo(scrollY + delta);
    }

    private int clampScroll(int y) {
        int max = getMaxScrollY();
        if (y < 0) return 0;
        if (y > max) return max;
        return y;
    }

    public void clampScroll() {
        scrollY = clampScroll(scrollY);
    }

    public int getMaxScrollY() {
        return Math.max(0, contentHeight - viewportHeight);
    }

    public LytRect getRect() {
        return new LytRect(0, scrollY, viewportWidth, viewportHeight);
    }

    public boolean isLayoutDirty() {
        return layoutDirty;
    }

    public void setLayoutDirty(boolean dirty) {
        this.layoutDirty = dirty;
    }

    public int viewportWidth() {
        return viewportWidth;
    }

    public int viewportHeight() {
        return viewportHeight;
    }

    public int contentWidth() {
        return contentWidth;
    }

    public int contentHeight() {
        return contentHeight;
    }
}
