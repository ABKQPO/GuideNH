package com.hfstudio.guidenh.guide.document.block.shapes;

import com.hfstudio.guidenh.guide.document.LytRect;
import com.hfstudio.guidenh.guide.render.RenderContext;

public class CloudShape implements ShapeRenderer {

    @Override
    public void render(RenderContext context, LytRect rect, int backgroundColor, int borderColor) {
        int x = rect.x(), y = rect.y(), w = rect.width(), h = rect.height();
        int cx = x + w / 2, cy = y + h / 2;
        int r = Math.min(w, h) / 3;

        // Cloud silhouette: center body with overlapping bumps
        int r2 = (int) (r * 0.7f);
        int r3 = (int) (r * 0.55f);

        // Center body
        context.fillCircle(cx, cy, r, backgroundColor);
        // Upper bumps
        context.fillCircle(cx - (int) (r * 0.5f), cy - (int) (r * 0.3f), r2, backgroundColor);
        context.fillCircle(cx + (int) (r * 0.45f), cy - (int) (r * 0.3f), r2, backgroundColor);
        // Lower bumps
        context.fillCircle(cx - (int) (r * 0.55f), cy + (int) (r * 0.3f), r3, backgroundColor);
        context.fillCircle(cx + (int) (r * 0.5f), cy + (int) (r * 0.3f), r3, backgroundColor);

        // Outlines
        context.drawCircleOutline(cx, cy, r, 1, borderColor);
        context.drawCircleOutline(cx - (int) (r * 0.5f), cy - (int) (r * 0.3f), r2, 1, borderColor);
        context.drawCircleOutline(cx + (int) (r * 0.45f), cy - (int) (r * 0.3f), r2, 1, borderColor);
        context.drawCircleOutline(cx - (int) (r * 0.55f), cy + (int) (r * 0.3f), r3, 1, borderColor);
        context.drawCircleOutline(cx + (int) (r * 0.5f), cy + (int) (r * 0.3f), r3, 1, borderColor);
    }
}
