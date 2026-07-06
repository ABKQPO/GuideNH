package com.hfstudio.guidenh.guide.document.block.shapes;

import com.hfstudio.guidenh.guide.document.LytRect;
import com.hfstudio.guidenh.guide.render.RenderContext;

public class StadiumShape implements ShapeRenderer {

    @Override
    public void render(RenderContext context, LytRect rect, int backgroundColor, int borderColor) {
        int x = rect.x(), y = rect.y(), w = rect.width(), h = rect.height();
        int r = Math.min(w, h) / 2;
        float cy = y + h / 2f;

        context.fillRect(x + r, y, w - r * 2, h, borderColor);
        context.fillCircle(x + r, cy, r, borderColor);
        context.fillCircle(x + w - r, cy, r, borderColor);

        int ir = Math.max(r - 1, 0);
        context.fillRect(x + r, y + 1, w - r * 2, h - 2, backgroundColor);
        context.fillCircle(x + r, cy, ir, backgroundColor);
        context.fillCircle(x + w - r, cy, ir, backgroundColor);
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
