package com.hfstudio.guidenh.guide.layout.flow;

import com.hfstudio.guidenh.guide.document.block.LytBlock;
import com.hfstudio.guidenh.guide.layout.LayoutContext;
import com.hfstudio.guidenh.guide.render.RenderContext;

/**
 * Standalone block in-line with other content.
 */
public class LineBlock extends LineElement {

    private final LytBlock block;
    private int layoutOffsetX;
    private int layoutOffsetY;

    public LineBlock(LytBlock block) {
        this.block = block;
    }

    public LytBlock getBlock() {
        return block;
    }

    public void setLayoutOffset(int layoutOffsetX, int layoutOffsetY) {
        this.layoutOffsetX = layoutOffsetX;
        this.layoutOffsetY = layoutOffsetY;
    }

    public void layoutBlock(LayoutContext context, int availableWidth) {
        block.layout(context, bounds.x() - layoutOffsetX, bounds.y() - layoutOffsetY, availableWidth);
    }

    @Override
    public void render(RenderContext context) {
        block.render(context);
    }
}
