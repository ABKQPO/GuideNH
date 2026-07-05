package com.hfstudio.guidenh.guide.document.block.shapes;

import com.hfstudio.guidenh.guide.document.LytRect;
import com.hfstudio.guidenh.guide.render.RenderContext;

public class HexagonShape implements ShapeRenderer {

    @Override
    public void render(RenderContext context, LytRect rect, int backgroundColor, int borderColor) {
        int x = rect.x(), y = rect.y(), w = rect.width(), h = rect.height();
        int r = rect.right(), b = rect.bottom(), cy = y + h / 2;
        int inset = Math.max(1, h / 4);

        context.fillPolygon(
            new float[] { x + inset, r - inset, r, r - inset, x + inset, x },
            new float[] { y, y, cy, b, b, cy },
            backgroundColor);
        context.drawLine(x + inset, y, r - inset, y, 1, borderColor);
        context.drawLine(r - inset, y, r, cy, 1, borderColor);
        context.drawLine(r, cy, r - inset, b, 1, borderColor);
        context.drawLine(r - inset, b, x + inset, b, 1, borderColor);
        context.drawLine(x + inset, b, x, cy, 1, borderColor);
        context.drawLine(x, cy, x + inset, y, 1, borderColor);
    }
}
