package com.hfstudio.guidenh.guide.document.block.shapes;

import com.hfstudio.guidenh.guide.document.LytRect;
import com.hfstudio.guidenh.guide.render.RenderContext;

public class BangShape implements ShapeRenderer {

    @Override
    public void render(RenderContext context, LytRect rect, int backgroundColor, int borderColor) {
        int x = rect.x(), y = rect.y(), w = rect.width(), h = rect.height();
        int r = rect.right(), b = rect.bottom(), cx = x + w / 2, cy = y + h / 2;
        int n = Math.max(2, Math.min(w, h) / 6);

        float[] xs = { cx, r - n, r, r, r, r - n, cx, x + n, x, x, x, x + n };
        float[] ys = { y, y, y + n, cy, b - n, b, b, b, b - n, cy, y + n, y };
        float centerX = x + w / 2f;
        float centerY = y + h / 2f;
        float[] shrunkXs = new float[12];
        float[] shrunkYs = new float[12];
        for (int i = 0; i < 12; i++) {
            float dx = xs[i] - centerX;
            float dy = ys[i] - centerY;
            float d = (float) Math.sqrt(dx * dx + dy * dy);
            if (d > 1) {
                float s = (d - 1) / d;
                shrunkXs[i] = centerX + dx * s;
                shrunkYs[i] = centerY + dy * s;
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
        int n = Math.max(2, Math.min(w, h) / 6);
        // The bang shape has inward notches of depth n on each side.
        // Inscribed rectangle: reduced by n from each side (the notches).
        int insL = n;
        int insR = n;
        int insT = n;
        int insB = n;
        int availW = Math.max(w - insL - insR - 2 * padX, 1);
        int availH = Math.max(h - insT - insB - 2 * padY, 1);
        int contentW = Math.min(availW, cw);
        int contentH = Math.min(availH, ch);
        return new LytRect(cx - contentW / 2, cy - contentH / 2, contentW, contentH);
    }

    @Override
    public LytRect minNodeRect(int cw, int ch, int padX, int padY) {
        // The inscribed rectangle has width = w - 2*n, height = h - 2*n, where n = min(w,h)/6.
        // For typical case w ≈ h: n = w/6, so inscribed = w - w/3 = 2w/3.
        // Need 2w/3 >= max(cw+2*padX, ch+2*padY), so w >= 1.5*max_padded.
        // For simplicity, use 1.5x the larger padded dimension.
        int maxPadded = Math.max(cw + 2 * padX, ch + 2 * padY);
        int dim = (int) Math.ceil(maxPadded * 1.5);
        return new LytRect(0, 0, dim, dim);
    }
}
