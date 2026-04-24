package com.hfstudio.guidenh.guide.layout.flow;

import com.hfstudio.guidenh.guide.document.block.LytBlock;
import com.hfstudio.guidenh.guide.render.RenderContext;

/**
 * Standalone block in-line with other content.
 */
public class LineBlock extends LineElement {

    private final LytBlock block;

    public LineBlock(LytBlock block) {
        this.block = block;
    }

    public LytBlock getBlock() {
        return block;
    }

    @Override
    public void render(RenderContext context) {
        block.render(context);
    }
}
