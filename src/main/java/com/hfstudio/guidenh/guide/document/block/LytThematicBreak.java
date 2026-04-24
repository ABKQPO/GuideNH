package com.hfstudio.guidenh.guide.document.block;

import com.hfstudio.guidenh.guide.color.SymbolicColor;
import com.hfstudio.guidenh.guide.document.LytRect;
import com.hfstudio.guidenh.guide.layout.LayoutContext;
import com.hfstudio.guidenh.guide.render.RenderContext;

public class LytThematicBreak extends LytBlock {

    @Override
    public LytRect computeLayout(LayoutContext context, int x, int y, int availableWidth) {
        return new LytRect(x, y, availableWidth, 6);
    }

    @Override
    protected void onLayoutMoved(int deltaX, int deltaY) {}

    @Override
    public void render(RenderContext context) {
        var line = bounds.withHeight(2)
            .centerVerticallyIn(bounds);

        context.fillRect(line, SymbolicColor.THEMATIC_BREAK);
    }
}
