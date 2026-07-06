package com.hfstudio.guidenh.guide.document.block.shapes;

import com.hfstudio.guidenh.guide.document.LytRect;
import com.hfstudio.guidenh.guide.render.RenderContext;

public class DoubleCircleShape implements ShapeRenderer {

    private static final int GAP = 5;

    @Override
    public void render(RenderContext context, LytRect rect, int backgroundColor, int borderColor) {
        int cx = rect.x() + rect.width() / 2;
        int cy = rect.y() + rect.height() / 2;
        int outerR = Math.min(rect.width(), rect.height()) / 2;
        int innerR = Math.max(outerR - GAP, 1);
        if (outerR > 0) {
            context.fillCircle(cx, cy, outerR, borderColor);
            context.fillCircle(cx, cy, Math.max(outerR - 1, 1), backgroundColor);
            context.fillCircle(cx, cy, innerR, borderColor);
            context.fillCircle(cx, cy, Math.max(innerR - 1, 0), backgroundColor);
        }
    }

    @Override
    public LytRect contentBounds(LytRect nodeRect, int cw, int ch, int padX, int padY) {
        int cx = nodeRect.x() + nodeRect.width() / 2;
        int cy = nodeRect.y() + nodeRect.height() / 2;
        int r = Math.min(nodeRect.width(), nodeRect.height()) / 2;
        int insSide = (int) (r * Math.sqrt(2));
        return new LytRect(cx - insSide / 2, cy - insSide / 2, insSide, insSide);
    }

    @Override
    public LytRect minNodeRect(int cw, int ch, int padX, int padY) {
        int maxContent = Math.max(cw + 2 * padX, ch + 2 * padY);
        int side = (int) Math.ceil(maxContent * Math.sqrt(2));
        return new LytRect(0, 0, side, side);
    }
}
