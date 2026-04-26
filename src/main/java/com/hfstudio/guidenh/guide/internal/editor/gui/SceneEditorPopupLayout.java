package com.hfstudio.guidenh.guide.internal.editor.gui;

import com.hfstudio.guidenh.guide.document.LytRect;

public final class SceneEditorPopupLayout {

    private SceneEditorPopupLayout() {}

    public static LytRect placeBelowAnchor(LytRect anchor, int popupWidth, int popupHeight, int viewportWidth,
        int viewportHeight, int padding) {
        int preferredX = anchor.right() - popupWidth;
        int preferredY = anchor.bottom();
        return clampToViewport(preferredX, preferredY, popupWidth, popupHeight, viewportWidth, viewportHeight, padding);
    }

    public static LytRect clampToViewport(int preferredX, int preferredY, int popupWidth, int popupHeight,
        int viewportWidth, int viewportHeight, int padding) {
        int safeWidth = Math.max(0, viewportWidth);
        int safeHeight = Math.max(0, viewportHeight);
        int safePadding = Math.max(0, padding);
        int maxX = Math.max(safePadding, safeWidth - popupWidth - safePadding);
        int maxY = Math.max(safePadding, safeHeight - popupHeight - safePadding);
        int x = clamp(preferredX, safePadding, maxX);
        int y = clamp(preferredY, safePadding, maxY);
        return new LytRect(x, y, Math.max(0, popupWidth), Math.max(0, popupHeight));
    }

    private static int clamp(int value, int minValue, int maxValue) {
        if (value < minValue) {
            return minValue;
        }
        return value > maxValue ? maxValue : value;
    }
}
