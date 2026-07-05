package com.hfstudio.guidenh.guide.document.block.shapes;

import com.hfstudio.guidenh.guide.document.LytRect;
import com.hfstudio.guidenh.guide.render.RenderContext;

@FunctionalInterface
public interface ShapeRenderer {

    void render(RenderContext context, LytRect rect, int backgroundColor, int borderColor);
}
