package com.hfstudio.guidenh.guide.internal.editor.gui;

public class SceneEditorScrollState {

    private int offsetPixels;
    private int viewportPixels;
    private int contentPixels;

    public int getOffsetPixels() {
        return offsetPixels;
    }

    public void setOffsetPixels(int offsetPixels) {
        this.offsetPixels = offsetPixels;
        clamp();
    }

    public int getViewportPixels() {
        return viewportPixels;
    }

    public void setViewportPixels(int viewportPixels) {
        this.viewportPixels = Math.max(0, viewportPixels);
        clamp();
    }

    public int getContentPixels() {
        return contentPixels;
    }

    public void setContentPixels(int contentPixels) {
        this.contentPixels = Math.max(0, contentPixels);
        clamp();
    }

    public int getMaxOffsetPixels() {
        return Math.max(0, contentPixels - viewportPixels);
    }

    public void scrollPixels(int deltaPixels) {
        offsetPixels += deltaPixels;
        clamp();
    }

    public void clamp() {
        if (offsetPixels < 0) {
            offsetPixels = 0;
            return;
        }
        int maxOffset = getMaxOffsetPixels();
        if (offsetPixels > maxOffset) {
            offsetPixels = maxOffset;
        }
    }
}
