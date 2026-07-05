package com.hfstudio.guidenh.guide.document.block.shapes;

import com.hfstudio.guidenh.guide.document.LytRect;
import com.hfstudio.guidenh.guide.render.RenderContext;

public class StadiumShape implements ShapeRenderer {

    @Override
    public void render(RenderContext context, LytRect rect, int backgroundColor, int borderColor) {
        int x = rect.x(), y = rect.y(), w = rect.width(), h = rect.height();
        int r = Math.min(w, h) / 2;
        float cy = y + h / 2f;

        context.fillRect(rect, backgroundColor);
        context.fillCircle(x + r, cy, r, backgroundColor);
        context.fillCircle(x + w - r, cy, r, backgroundColor);

        context.drawLine(x + r, y, x + w - r, y, 1, borderColor);
        context.drawLine(x + r, y + h, x + w - r, y + h, 1, borderColor);
        context.drawCircleOutline(x + r, cy, r, 1, borderColor);
        context.drawCircleOutline(x + w - r, cy, r, 1, borderColor);
    }
}
