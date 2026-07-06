package com.hfstudio.guidenh.guide.document.block.shapes;

import com.hfstudio.guidenh.guide.document.LytRect;
import com.hfstudio.guidenh.guide.render.RenderContext;

public class AsymmetricShape implements ShapeRenderer {

    @Override
    public void render(RenderContext context, LytRect rect, int backgroundColor, int borderColor) {
        int x = rect.x(), y = rect.y(), w = rect.width(), h = rect.height();
        int r = rect.right(), b = rect.bottom(), cy = y + h / 2;
        int inset = Math.max(2, h / 4);

        float cx = x + w / 2f;
        float cy2 = y + h / 2f;
        float[] xs = { r, r, x + inset, x, x + inset };
        float[] ys = { y, b, b, cy, y };
        float[] shrunkXs = new float[5];
        float[] shrunkYs = new float[5];
        for (int i = 0; i < 5; i++) {
            float dx = xs[i] - cx;
            float dy = ys[i] - cy2;
            float d = (float) Math.sqrt(dx * dx + dy * dy);
            if (d > 1) {
                float s = (d - 1) / d;
                shrunkXs[i] = cx + dx * s;
                shrunkYs[i] = cy2 + dy * s;
            } else {
                shrunkXs[i] = xs[i];
                shrunkYs[i] = ys[i];
            }
        }
        context.fillPolygon(xs, ys, borderColor);
        context.fillPolygon(shrunkXs, shrunkYs, backgroundColor);
    }

    @Override
    public LytRect contentBounds(LytRect nodeRect, int cw, int ch, int padX, int padY) {
        int w = nodeRect.width();
        int h = nodeRect.height();
        int cx = nodeRect.x() + w / 2;
        int cy = nodeRect.y() + h / 2;
        int inset = Math.max(2, h / 4);
        // Asymmetric is right-heavy: full width at right, inset from left edge at top/bottom.
        // The inscribed rectangle: left = x + inset, right = w, top = y, bottom = h.
        int availW = Math.max(w - inset - 2 * padX, 1);
        int availH = Math.max(h - 2 * padY, 1);
        int contentW = Math.min(availW, cw);
        int contentH = Math.min(availH, ch);
        return new LytRect(cx - contentW / 2, cy - contentH / 2, contentW, contentH);
    }

    @Override
    public LytRect minNodeRect(int cw, int ch, int padX, int padY) {
        int pw = cw + 2 * padX;
        int ph = ch + 2 * padY;
        // The inscribed area is roughly (w - inset) × h, where inset = h/4.
        // For content: w - h/4 >= pw and h >= ph.
        // w >= pw + h/4 >= pw + ph/4
        int w = pw + Math.max(ph / 4, 1);
        return new LytRect(0, 0, w, ph);
    }
}
