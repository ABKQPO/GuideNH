package com.hfstudio.guidenh.guide.document.block.shapes;

import com.hfstudio.guidenh.guide.document.LytRect;
import com.hfstudio.guidenh.guide.render.RenderContext;

public class BangShape implements ShapeRenderer {

    @Override
    public void render(RenderContext context, LytRect rect, int backgroundColor, int borderColor) {
        int x = rect.x(), y = rect.y(), w = rect.width(), h = rect.height();
        int r = rect.right(), b = rect.bottom(), cx = x + w / 2, cy = y + h / 2;
        int n = Math.max(2, Math.min(w, h) / 6);

        // 12-point burst: outward at cardinal directions, inward notches at diagonals
        context.fillPolygon(
            new float[] { cx, r - n, r, r, r, r - n, cx, x + n, x, x, x, x + n },
            new float[] { y, y, y + n, cy, b - n, b, b, b, b - n, cy, y + n, y },
            backgroundColor);

        context.drawLine(cx, y, r - n, y, 1, borderColor);
        context.drawLine(r - n, y, r, y + n, 1, borderColor);
        context.drawLine(r, y + n, r, cy, 1, borderColor);
        context.drawLine(r, cy, r, b - n, 1, borderColor);
        context.drawLine(r, b - n, r - n, b, 1, borderColor);
        context.drawLine(r - n, b, cx, b, 1, borderColor);
        context.drawLine(cx, b, x + n, b, 1, borderColor);
        context.drawLine(x + n, b, x, b - n, 1, borderColor);
        context.drawLine(x, b - n, x, cy, 1, borderColor);
        context.drawLine(x, cy, x, y + n, 1, borderColor);
        context.drawLine(x, y + n, x + n, y, 1, borderColor);
        context.drawLine(x + n, y, cx, y, 1, borderColor);
    }
}
