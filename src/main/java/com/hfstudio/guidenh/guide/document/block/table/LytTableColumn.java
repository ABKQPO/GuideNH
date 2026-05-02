package com.hfstudio.guidenh.guide.document.block.table;

public class LytTableColumn {

    int x;
    int width;
    int preferredWidth;

    public int getPreferredWidth() {
        return preferredWidth;
    }

    public void setPreferredWidth(int preferredWidth) {
        this.preferredWidth = Math.max(0, preferredWidth);
    }
}
