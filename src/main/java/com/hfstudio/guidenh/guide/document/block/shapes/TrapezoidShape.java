package com.hfstudio.guidenh.guide.document.block.shapes;

import com.hfstudio.guidenh.guide.document.LytRect;
import com.hfstudio.guidenh.guide.render.RenderContext;

public class TrapezoidShape implements ShapeRenderer {

    @Override
    public void render(RenderContext context, LytRect rect, int backgroundColor, int borderColor) {
        int x = rect.x(), y = rect.y(), w = rect.width(), h = rect.height();
        int r = rect.right(), b = rect.bottom();
        int inset = Math.max(1, h / 4);

        context.fillPolygon(new float[] { x + inset, r - inset, r, x }, new float[] { y, y, b, b }, backgroundColor);
        context.drawLine(x + inset, y, r - inset, y, 1, borderColor);
        context.drawLine(r - inset, y, r, b, 1, borderColor);
        context.drawLine(r, b, x, b, 1, borderColor);
        context.drawLine(x, b, x + inset, y, 1, borderColor);
    }
}
