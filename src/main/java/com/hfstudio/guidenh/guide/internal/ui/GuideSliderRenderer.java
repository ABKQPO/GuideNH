package com.hfstudio.guidenh.guide.internal.ui;

import com.hfstudio.guidenh.guide.document.LytRect;

public final class GuideSliderRenderer {

    public static final int TRACK_COLOR = 0x6622262C;
    public static final int FILL_COLOR = 0xAA1CB4E9;
    public static final int THUMB_COLOR = 0xFFEAF6FF;
    public static final int ACTIVE_THUMB_COLOR = 0xFFFFFFFF;
    public static final int TRACK_HEIGHT = 4;
    public static final int THUMB_WIDTH = 6;
    public static final int THUMB_OVERHANG = 2;
    public static final int HIT_PADDING_Y = 3;

    private GuideSliderRenderer() {}

    public static int resolveThumbColor(boolean highlighted) {
        return highlighted ? ACTIVE_THUMB_COLOR : THUMB_COLOR;
    }

    public static SliderGeometry layout(int x, int y, int width, float fraction) {
        int sliderWidth = Math.max(0, width);
        float clampedFraction = clampFraction(fraction);
        int thumbTravel = Math.max(0, sliderWidth - THUMB_WIDTH);
        int thumbX = x + Math.round(clampedFraction * thumbTravel);
        return new SliderGeometry(
            new LytRect(x, y, sliderWidth, TRACK_HEIGHT),
            new LytRect(x, y, Math.max(0, thumbX + THUMB_WIDTH / 2 - x), TRACK_HEIGHT),
            new LytRect(thumbX, y - THUMB_OVERHANG, THUMB_WIDTH, TRACK_HEIGHT + THUMB_OVERHANG * 2),
            new LytRect(x, y - HIT_PADDING_Y, sliderWidth, TRACK_HEIGHT + HIT_PADDING_Y * 2));
    }

    public static void render(RectDrawer drawer, SliderGeometry geometry, boolean highlighted) {
        if (drawer == null || geometry == null) {
            return;
        }
        drawRect(drawer, geometry.trackRect(), TRACK_COLOR);
        drawRect(drawer, geometry.fillRect(), FILL_COLOR);
        drawRect(drawer, geometry.thumbRect(), resolveThumbColor(highlighted));
    }

    public static float fractionFromMouse(int mouseX, int sliderX, int sliderWidth) {
        if (sliderWidth <= 0) {
            return 0f;
        }
        return clampFraction((mouseX - sliderX) / (float) sliderWidth);
    }

    private static float clampFraction(float fraction) {
        if (fraction < 0f) {
            return 0f;
        }
        return fraction > 1f ? 1f : fraction;
    }

    private static void drawRect(RectDrawer drawer, LytRect rect, int color) {
        if (rect == null || rect.width() <= 0 || rect.height() <= 0) {
            return;
        }
        drawer.drawRect(rect.x(), rect.y(), rect.right(), rect.bottom(), color);
    }

    public interface RectDrawer {

        void drawRect(int left, int top, int right, int bottom, int color);
    }

    public static final class SliderGeometry {

        private final LytRect trackRect;
        private final LytRect fillRect;
        private final LytRect thumbRect;
        private final LytRect hitRect;

        private SliderGeometry(LytRect trackRect, LytRect fillRect, LytRect thumbRect, LytRect hitRect) {
            this.trackRect = trackRect;
            this.fillRect = fillRect;
            this.thumbRect = thumbRect;
            this.hitRect = hitRect;
        }

        public LytRect trackRect() {
            return trackRect;
        }

        public LytRect fillRect() {
            return fillRect;
        }

        public LytRect thumbRect() {
            return thumbRect;
        }

        public LytRect hitRect() {
            return hitRect;
        }
    }
}
