package com.hfstudio.guidenh.guide.document.block.shapes;

import com.hfstudio.guidenh.guide.document.LytRect;
import com.hfstudio.guidenh.guide.render.RenderContext;

public class DiamondShape implements ShapeRenderer {

    @Override
    public void render(RenderContext context, LytRect rect, int backgroundColor, int borderColor) {
        int cx = rect.x() + rect.width() / 2;
        int cy = rect.y() + rect.height() / 2;
        int r = rect.right();
        int b = rect.bottom();
        int x = rect.x();

        context.fillPolygon(new float[] { cx, r, cx, x }, new float[] { rect.y(), cy, b, cy }, backgroundColor);
        context.drawLine(cx, rect.y(), r, cy, 1, borderColor);
        context.drawLine(r, cy, cx, b, 1, borderColor);
        context.drawLine(cx, b, x, cy, 1, borderColor);
        context.drawLine(x, cy, cx, rect.y(), 1, borderColor);
    }
}
