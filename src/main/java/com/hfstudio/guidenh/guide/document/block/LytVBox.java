package com.hfstudio.guidenh.guide.document.block;

import com.hfstudio.guidenh.guide.document.LytRect;
import com.hfstudio.guidenh.guide.layout.LayoutContext;
import com.hfstudio.guidenh.guide.layout.Layouts;

/**
 * Lays out its children vertically.
 */
public class LytVBox extends LytAxisBox {

    @Override
    protected LytRect computeBoxLayout(LayoutContext context, int x, int y, int availableWidth) {
        // Padding is applied through the parent
        return Layouts.verticalLayout(
            context,
            children,
            x,
            y,
            availableWidth,
            isFullWidth(),
            0,
            0,
            0,
            0,
            getGap(),
            getAlignItems());
    }
}
