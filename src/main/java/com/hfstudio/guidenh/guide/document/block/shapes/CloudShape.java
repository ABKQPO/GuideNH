package com.hfstudio.guidenh.guide.document.block.shapes;

import com.hfstudio.guidenh.guide.document.LytRect;
import com.hfstudio.guidenh.guide.render.RenderContext;

public class CloudShape implements ShapeRenderer {

    @Override
    public void render(RenderContext context, LytRect rect, int backgroundColor, int borderColor) {
        int x = rect.x(), y = rect.y(), w = rect.width(), h = rect.height();
        int cx = x + w / 2, cy = y + h / 2;
        int r = Math.min(w, h) / 3;

        int r2 = (int) (r * 0.7f);
        int r3 = (int) (r * 0.55f);

        float ux1 = cx - (int) (r * 0.5f);
        float uy1 = cy - (int) (r * 0.3f);
        float ux2 = cx + (int) (r * 0.45f);
        float uy2 = cy - (int) (r * 0.3f);
        float lx1 = cx - (int) (r * 0.55f);
        float ly1 = cy + (int) (r * 0.3f);
        float lx2 = cx + (int) (r * 0.5f);
        float ly2 = cy + (int) (r * 0.3f);
        int ir = Math.max(r - 1, 1);

        context.fillCircle(cx, cy, r, borderColor);
        context.fillCircle(ux1, uy1, r2, borderColor);
        context.fillCircle(ux2, uy2, r2, borderColor);
        context.fillCircle(lx1, ly1, r3, borderColor);
        context.fillCircle(lx2, ly2, r3, borderColor);
        context.fillCircle(cx, cy, ir, backgroundColor);
        context.fillCircle(ux1, uy1, Math.max(r2 - 1, 1), backgroundColor);
        context.fillCircle(ux2, uy2, Math.max(r2 - 1, 1), backgroundColor);
        context.fillCircle(lx1, ly1, Math.max(r3 - 1, 1), backgroundColor);
        context.fillCircle(lx2, ly2, Math.max(r3 - 1, 1), backgroundColor);
    }

    @Override
    public LytRect contentBounds(LytRect nodeRect, int cw, int ch, int padX, int padY) {
        int cx = nodeRect.x() + nodeRect.width() / 2;
        int cy = nodeRect.y() + nodeRect.height() / 2;
        int r = Math.min(nodeRect.width(), nodeRect.height()) / 3;
        // Inscribed square inside the central cloud circle, padded
        int insSide = (int) (r * Math.sqrt(2));
        int availW = Math.max(insSide - 2 * padX, 1);
        int availH = Math.max(insSide - 2 * padY, 1);
        int contentW = Math.min(availW, cw);
        int contentH = Math.min(availH, ch);
        return new LytRect(cx - contentW / 2, cy - contentH / 2, contentW, contentH);
    }

    @Override
    public LytRect minNodeRect(int cw, int ch, int padX, int padY) {
        // The content fits within the inscribed square of the central cloud circle.
        // The central circle radius r = min(w,h)/3, inscribed square side = r*sqrt(2).
        // Need r*sqrt(2) >= max(cw+2*padX, ch+2*padY), so r >= max_padded/sqrt(2).
        // Since r = min(w,h)/3, min(w,h) >= 3*max_padded/sqrt(2).
        int maxPadded = Math.max(cw + 2 * padX, ch + 2 * padY);
        int dim = (int) Math.ceil(3 * maxPadded / Math.sqrt(2));
        return new LytRect(0, 0, dim, dim);
    }
}
