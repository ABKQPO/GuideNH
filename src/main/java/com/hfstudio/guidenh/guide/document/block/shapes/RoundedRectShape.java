package com.hfstudio.guidenh.guide.document.block.shapes;

import com.hfstudio.guidenh.guide.document.LytRect;
import com.hfstudio.guidenh.guide.render.RenderContext;

public class RoundedRectShape implements ShapeRenderer {

    @Override
    public void render(RenderContext context, LytRect rect, int backgroundColor, int borderColor) {
        int x = rect.x(), y = rect.y(), w = rect.width(), h = rect.height();
        int r = Math.clamp(Math.min(w, h) / 5, 2, 12);

        context.fillRect(rect, borderColor);
        context.fillCircle(x + r, y + r, r, borderColor);
        context.fillCircle(x + w - r, y + r, r, borderColor);
        context.fillCircle(x + r, y + h - r, r, borderColor);
        context.fillCircle(x + w - r, y + h - r, r, borderColor);

        int inset = 1;
        int ir = Math.max(r - inset, 1);
        int ix = x + inset, iy = y + inset, iw = w - inset * 2, ih = h - inset * 2;
        context.fillRect(ix, iy, iw, ih, backgroundColor);
        context.fillCircle(ix + ir, iy + ir, ir, backgroundColor);
        context.fillCircle(ix + iw - ir, iy + ir, ir, backgroundColor);
        context.fillCircle(ix + ir, iy + ih - ir, ir, backgroundColor);
        context.fillCircle(ix + iw - ir, iy + ih - ir, ir, backgroundColor);
    }

    @Override
    public LytRect contentBounds(LytRect nodeRect, int cw, int ch, int padX, int padY) {
        return nodeRect.shrink(padX, padY, padX, padY);
    }

    @Override
    public LytRect minNodeRect(int cw, int ch, int padX, int padY) {
        return new LytRect(0, 0, cw + 2 * padX, ch + 2 * padY);
    }
}
