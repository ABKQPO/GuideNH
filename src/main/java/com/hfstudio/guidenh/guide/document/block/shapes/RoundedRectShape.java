package com.hfstudio.guidenh.guide.document.block.shapes;

import com.hfstudio.guidenh.guide.document.LytRect;
import com.hfstudio.guidenh.guide.render.RenderContext;

public class RoundedRectShape implements ShapeRenderer {

    @Override
    public void render(RenderContext context, LytRect rect, int backgroundColor, int borderColor) {
        int x = rect.x(), y = rect.y(), w = rect.width(), h = rect.height();
        int r = Math.clamp(w / 6, 1, 8);

        // Outer shape (border): fill outer rect + corner circles with borderColor
        context.fillRect(rect, borderColor);
        context.fillCircle(x + r, y + r, r, borderColor);
        context.fillCircle(x + w - r, y + r, r, borderColor);
        context.fillCircle(x + r, y + h - r, r, borderColor);
        context.fillCircle(x + w - r, y + h - r, r, borderColor);

        // Inner shape (background): inset by 1px to reveal border
        int ir = Math.max(r - 1, 0);
        context.fillRect(x + 1, y + 1, w - 2, h - 2, backgroundColor);
        context.fillCircle(x + r, y + r, ir, backgroundColor);
        context.fillCircle(x + w - r, y + r, ir, backgroundColor);
        context.fillCircle(x + r, y + h - r, ir, backgroundColor);
        context.fillCircle(x + w - r, y + h - r, ir, backgroundColor);
    }
}
