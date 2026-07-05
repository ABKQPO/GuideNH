package com.hfstudio.guidenh.guide.document.block.shapes;

import com.hfstudio.guidenh.guide.document.LytRect;
import com.hfstudio.guidenh.guide.render.RenderContext;

public class CylinderShape implements ShapeRenderer {

    @Override
    public void render(RenderContext context, LytRect rect, int backgroundColor, int borderColor) {
        int x = rect.x(), y = rect.y(), w = rect.width(), h = rect.height();
        int cx = x + w / 2;
        int ellipseR = Math.min(w, h) / 4;
        int bodyTop = y + ellipseR;
        int bodyBottom = y + h - ellipseR;

        context.fillRect(new LytRect(x, bodyTop, w, bodyBottom - bodyTop), backgroundColor);
        context.fillCircle(cx, bodyTop, ellipseR, backgroundColor);
        context.fillCircle(cx, bodyBottom, ellipseR, backgroundColor);

        context.drawCircleOutline(cx, bodyTop, ellipseR, 1, borderColor);
        context.drawLine(x, bodyTop, x, bodyBottom, 1, borderColor);
        context.drawLine(x + w, bodyTop, x + w, bodyBottom, 1, borderColor);
        context.drawCircleOutline(cx, bodyBottom, ellipseR, 1, borderColor);
    }
}
