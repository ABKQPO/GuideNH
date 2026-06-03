package com.hfstudio.guidenh.guide.document.block;

public class ResponsiveVisualSizing {

    private ResponsiveVisualSizing() {}

    public static int scaleHeightForWidth(int baseWidth, int baseHeight, int actualWidth, int minHeight) {
        int safeBaseWidth = Math.max(1, baseWidth);
        int safeBaseHeight = Math.max(1, baseHeight);
        int safeActualWidth = Math.max(1, actualWidth);
        if (safeActualWidth >= safeBaseWidth) {
            return safeBaseHeight;
        }
        float scale = safeActualWidth / (float) safeBaseWidth;
        return Math.max(Math.max(1, minHeight), Math.round(safeBaseHeight * scale));
    }

    public static int scaleWidth(int baseWidth, float visualScale, int minWidth) {
        int safeBaseWidth = Math.max(1, baseWidth);
        float clampedScale = Math.clamp(visualScale, 0.1f, 1.0f);
        if (clampedScale >= 0.999f) {
            return safeBaseWidth;
        }
        return Math.max(Math.max(1, minWidth), Math.round(safeBaseWidth * clampedScale));
    }

    public static int scaleBodyHeightForWidth(int baseWidth, int totalHeight, int actualWidth, int fixedHeight,
        int minBodyHeight) {
        int safeTotalHeight = Math.max(1, totalHeight);
        int safeFixedHeight = Math.clamp(fixedHeight, 0, safeTotalHeight - 1);
        int bodyHeight = Math.max(1, safeTotalHeight - safeFixedHeight);
        int scaledBodyHeight = scaleHeightForWidth(baseWidth, bodyHeight, actualWidth, minBodyHeight);
        return safeFixedHeight + scaledBodyHeight;
    }
}
