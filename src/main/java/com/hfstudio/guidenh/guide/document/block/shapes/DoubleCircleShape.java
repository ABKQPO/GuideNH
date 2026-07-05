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
            context.fillCircle(cx, cy, outerR, backgroundColor);
            context.drawCircleOutline(cx, cy, outerR, 1, borderColor);
            context.fillCircle(cx, cy, innerR, backgroundColor);
            context.drawCircleOutline(cx, cy, innerR, 1, borderColor);
        }
    }
}
