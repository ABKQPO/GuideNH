package com.hfstudio.guidenh.guide.document.block.shapes;

import com.hfstudio.guidenh.guide.document.LytRect;
import com.hfstudio.guidenh.guide.render.RenderContext;

public class SubprocessShape implements ShapeRenderer {

    private static final int FRAME_WIDTH = 8;

    @Override
    public void render(RenderContext context, LytRect rect, int backgroundColor, int borderColor) {
        int x = rect.x(), y = rect.y(), w = rect.width(), h = rect.height();

        // Outer framed rect extends FRAME_WIDTH beyond inner on left and right
        context.fillRect(x - FRAME_WIDTH, y, w + FRAME_WIDTH * 2, h, borderColor);
        context.fillRect(x, y, w, h, backgroundColor);

        // Inner vertical divider lines at the frame edges
        context.drawLine(x, y, x, y + h, 1, borderColor);
        context.drawLine(x + w, y, x + w, y + h, 1, borderColor);
    }
}
