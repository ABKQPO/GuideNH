package com.hfstudio.guidenh.guide.document.block.shapes;

import com.hfstudio.guidenh.guide.document.LytRect;
import com.hfstudio.guidenh.guide.render.RenderContext;

public class SubprocessShape implements ShapeRenderer {

    private static final int FRAME_WIDTH = 8;

    @Override
    public void render(RenderContext context, LytRect rect, int backgroundColor, int borderColor) {
        int x = rect.x(), y = rect.y(), w = rect.width(), h = rect.height();
        int innerX = x + FRAME_WIDTH;
        int innerW = w - FRAME_WIDTH * 2;

        context.fillRect(x, y, w, h, borderColor);
        context.fillRect(innerX, y, innerW, h, backgroundColor);

        context.drawLine(innerX, y, innerX, y + h, 1, borderColor);
        context.drawLine(innerX + innerW, y, innerX + innerW, y + h, 1, borderColor);
    }

    @Override
    public LytRect contentBounds(LytRect nodeRect, int cw, int ch, int padX, int padY) {
        int innerX = nodeRect.x() + FRAME_WIDTH;
        int innerW = nodeRect.width() - FRAME_WIDTH * 2;
        return new LytRect(innerX + padX, nodeRect.y() + padY, innerW - 2 * padX, nodeRect.height() - 2 * padY);
    }

    @Override
    public LytRect minNodeRect(int cw, int ch, int padX, int padY) {
        int w = cw + 2 * padX + FRAME_WIDTH * 2;
        int h = ch + 2 * padY;
        return new LytRect(0, 0, w, h);
    }
}
