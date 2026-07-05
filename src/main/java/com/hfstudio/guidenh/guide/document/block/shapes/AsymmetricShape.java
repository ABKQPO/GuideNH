package com.hfstudio.guidenh.guide.document.block.shapes;

import com.hfstudio.guidenh.guide.document.LytRect;
import com.hfstudio.guidenh.guide.render.RenderContext;

public class AsymmetricShape implements ShapeRenderer {

    @Override
    public void render(RenderContext context, LytRect rect, int backgroundColor, int borderColor) {
        int x = rect.x(), y = rect.y(), w = rect.width(), h = rect.height();
        int r = rect.right(), b = rect.bottom(), cy = y + h / 2;
        int inset = Math.max(2, h / 4);

        context.fillPolygon(
            new float[] { r, r, x + inset, x, x + inset },
            new float[] { y, b, b, cy, y },
            backgroundColor);
        context.drawLine(x + inset, y, r, y, 1, borderColor);
        context.drawLine(r, y, r, b, 1, borderColor);
        context.drawLine(r, b, x + inset, b, 1, borderColor);
        context.drawLine(x + inset, b, x, cy, 1, borderColor);
        context.drawLine(x, cy, x + inset, y, 1, borderColor);
    }
}
