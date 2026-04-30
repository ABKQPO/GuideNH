package com.hfstudio.guidenh.guide.document.block;

import com.hfstudio.guidenh.guide.document.LytRect;
import com.hfstudio.guidenh.guide.layout.LayoutContext;

public class LytWidthBox extends LytVBox {

    private int preferredWidth;

    public int getPreferredWidth() {
        return preferredWidth;
    }

    public void setPreferredWidth(int preferredWidth) {
        this.preferredWidth = Math.max(0, preferredWidth);
    }

    @Override
    protected LytRect computeBoxLayout(LayoutContext context, int x, int y, int availableWidth) {
        int constrainedWidth = preferredWidth > 0 ? Math.min(availableWidth, preferredWidth) : availableWidth;
        return super.computeBoxLayout(context, x, y, Math.max(1, constrainedWidth));
    }
}
