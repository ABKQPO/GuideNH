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

        context.fillRect(new LytRect(x, bodyTop, w, bodyBottom - bodyTop), borderColor);
        context.fillCircle(cx, bodyTop, ellipseR, borderColor);
        context.fillCircle(cx, bodyBottom, ellipseR, borderColor);

        int ir = Math.max(ellipseR - 1, 0);
        context.fillRect(new LytRect(x + 1, bodyTop + 1, w - 2, bodyBottom - bodyTop - 2), backgroundColor);
        context.fillCircle(cx, bodyTop, ir, backgroundColor);
        context.fillCircle(cx, bodyBottom, ir, backgroundColor);
    }

    @Override
    public LytRect contentBounds(LytRect nodeRect, int cw, int ch, int padX, int padY) {
        int ellipseR = Math.min(nodeRect.width(), nodeRect.height()) / 4;
        int top = nodeRect.y() + ellipseR;
        int bodyH = nodeRect.height() - 2 * ellipseR;
        return new LytRect(nodeRect.x() + padX, top + padY, nodeRect.width() - 2 * padX, bodyH - 2 * padY);
    }

    @Override
    public LytRect minNodeRect(int cw, int ch, int padX, int padY) {
        int pw = cw + 2 * padX;
        int ph = ch + 2 * padY;
        // body = h - 2*min(w,h)/4. For any aspect ratio h >= 2*ph guarantees body >= ph.
        return new LytRect(0, 0, pw, ph * 2);
    }
}
