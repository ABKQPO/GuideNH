package com.hfstudio.guidenh.guide.document.block.shapes;

import com.hfstudio.guidenh.guide.document.LytRect;
import com.hfstudio.guidenh.guide.render.RenderContext;

public class DiamondShape implements ShapeRenderer {

    @Override
    public void render(RenderContext context, LytRect rect, int backgroundColor, int borderColor) {
        int cx = rect.x() + rect.width() / 2;
        int cy = rect.y() + rect.height() / 2;
        int r = rect.right();
        int b = rect.bottom();
        int x = rect.x();

        float[] xs = { cx, r, cx, x };
        float[] ys = { rect.y(), cy, b, cy };
        float[] shrunkXs = new float[4];
        float[] shrunkYs = new float[4];
        shrinkPoly(xs, ys, shrunkXs, shrunkYs, cx, cy);
        context.fillPolygon(xs, ys, borderColor);
        context.fillPolygon(shrunkXs, shrunkYs, backgroundColor);
    }

    @Override
    public LytRect contentBounds(LytRect nodeRect, int cw, int ch, int padX, int padY) {
        int cx = nodeRect.x() + nodeRect.width() / 2;
        int cy = nodeRect.y() + nodeRect.height() / 2;
        int insW = nodeRect.width() / 2;
        int insH = nodeRect.height() / 2;
        return new LytRect(cx - insW / 2, cy - insH / 2, insW, insH);
    }

    @Override
    public LytRect minNodeRect(int cw, int ch, int padX, int padY) {
        int w = (cw + 2 * padX) * 2;
        int h = (ch + 2 * padY) * 2;
        return new LytRect(0, 0, w, h);
    }

    private static void shrinkPoly(float[] xs, float[] ys, float[] outXs, float[] outYs, float cx, float cy) {
        for (int i = 0; i < xs.length; i++) {
            float dx = xs[i] - cx;
            float dy = ys[i] - cy;
            float d = (float) Math.sqrt(dx * dx + dy * dy);
            if (d > 1) {
                float s = (d - 1) / d;
                outXs[i] = cx + dx * s;
                outYs[i] = cy + dy * s;
            } else {
                outXs[i] = xs[i];
                outYs[i] = ys[i];
            }
        }
    }
}
