package com.hfstudio.guidenh.guide.internal.editor.gui;

import com.github.bsideup.jabel.Desugar;

public final class SceneEditorVerticalScrollbar {

    private static final int MIN_THUMB_SIZE = 18;

    private SceneEditorVerticalScrollbar() {}

    public static Thumb computeThumb(int trackStart, int trackLength, int contentPixels, int viewportPixels,
        int offsetPixels) {
        int safeTrackLength = Math.max(1, trackLength);
        int safeContentPixels = Math.max(0, contentPixels);
        int safeViewportPixels = Math.max(0, viewportPixels);
        int maxOffset = Math.max(0, safeContentPixels - safeViewportPixels);
        if (maxOffset <= 0) {
            return new Thumb(trackStart, safeTrackLength);
        }

        int thumbHeight = Math
            .max(MIN_THUMB_SIZE, safeTrackLength * safeTrackLength / Math.max(safeTrackLength, safeContentPixels));
        thumbHeight = Math.min(thumbHeight, safeTrackLength);
        int thumbTrack = Math.max(1, safeTrackLength - thumbHeight);
        int clampedOffset = clamp(offsetPixels, 0, maxOffset);
        int thumbTop = trackStart + (int) ((long) thumbTrack * clampedOffset / maxOffset);
        return new Thumb(thumbTop, thumbHeight);
    }

    public static int offsetFromDrag(int mousePosition, int grabOffset, int trackStart, int trackLength,
        int contentPixels, int viewportPixels) {
        int safeTrackLength = Math.max(1, trackLength);
        int safeContentPixels = Math.max(0, contentPixels);
        int safeViewportPixels = Math.max(0, viewportPixels);
        int maxOffset = Math.max(0, safeContentPixels - safeViewportPixels);
        if (maxOffset <= 0) {
            return 0;
        }

        Thumb thumb = computeThumb(trackStart, trackLength, contentPixels, viewportPixels, 0);
        int thumbTrack = Math.max(1, safeTrackLength - thumb.size());
        int rawThumbTop = mousePosition - grabOffset;
        int clampedThumbTop = clamp(rawThumbTop, trackStart, trackStart + thumbTrack);
        return (int) ((long) (clampedThumbTop - trackStart) * maxOffset / thumbTrack);
    }

    private static int clamp(int value, int minValue, int maxValue) {
        if (value < minValue) {
            return minValue;
        }
        return value > maxValue ? maxValue : value;
    }

    @Desugar
    public record Thumb(int start, int size) {

        public int end() {
            return start + size;
        }
    }
}
