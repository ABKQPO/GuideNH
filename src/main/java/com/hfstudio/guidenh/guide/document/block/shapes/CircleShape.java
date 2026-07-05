package com.hfstudio.guidenh.guide.document.block.shapes;

import com.hfstudio.guidenh.guide.document.LytRect;
import com.hfstudio.guidenh.guide.render.RenderContext;

public class CircleShape implements ShapeRenderer {

    @Override
    public void render(RenderContext context, LytRect rect, int backgroundColor, int borderColor) {
        int cx = rect.x() + rect.width() / 2;
        int cy = rect.y() + rect.height() / 2;
        int r = Math.min(rect.width(), rect.height()) / 2;
        if (r > 0) {
            context.fillCircle(cx, cy, r, backgroundColor);
            context.drawCircleOutline(cx, cy, r, 1, borderColor);
        }
    }
}
