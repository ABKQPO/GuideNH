package com.hfstudio.guidenh.guide.layout;

import java.util.ArrayList;
import java.util.List;
import java.util.OptionalInt;

import com.hfstudio.guidenh.guide.document.LytRect;
import com.hfstudio.guidenh.guide.style.ResolvedTextStyle;

public class LayoutContext implements FontMetrics {

    private final FontMetrics fontMetrics;
    private float visualScale = 1.0f;

    private final List<LytRect> leftFloats = new ArrayList<>();
    private final List<LytRect> rightFloats = new ArrayList<>();

    /** Cached right edge of the furthest-right left float. {@link Integer#MIN_VALUE} = dirty. */
    private int cachedLeftFloatRightEdge = Integer.MIN_VALUE;
    /** Cached left edge of the furthest-left right float. {@link Integer#MAX_VALUE} = dirty. */
    private int cachedRightFloatLeftEdge = Integer.MAX_VALUE;

    public LayoutContext(FontMetrics fontMetrics) {
        this.fontMetrics = fontMetrics;
    }

    public LayoutContext withVisualScale(float visualScale) {
        this.visualScale = Math.max(0.1f, Math.min(1.0f, visualScale));
        return this;
    }

    public float getVisualScale() {
        return visualScale;
    }

    public void addLeftFloat(LytRect bounds) {
        leftFloats.add(bounds);
        cachedLeftFloatRightEdge = Integer.MIN_VALUE;
    }

    public void addRightFloat(LytRect bounds) {
        rightFloats.add(bounds);
        cachedRightFloatLeftEdge = Integer.MAX_VALUE;
    }

    public OptionalInt getLeftFloatRightEdge() {
        if (leftFloats.isEmpty()) {
            return OptionalInt.empty();
        }
        return OptionalInt.of(getLeftFloatRightEdgeOr(0));
    }

    /**
     * Returns the right edge of the furthest-right left float, or {@code fallback} if there are none.
     * Prefer this over {@link #getLeftFloatRightEdge()} in hot paths to avoid {@link OptionalInt} allocation.
     */
    public int getLeftFloatRightEdgeOr(int fallback) {
        if (leftFloats.isEmpty()) {
            return fallback;
        }
        if (cachedLeftFloatRightEdge == Integer.MIN_VALUE) {
            int maxRight = Integer.MIN_VALUE + 1;
            for (var f : leftFloats) {
                int r = f.right();
                if (r > maxRight) maxRight = r;
            }
            cachedLeftFloatRightEdge = maxRight;
        }
        return cachedLeftFloatRightEdge;
    }

    public OptionalInt getRightFloatLeftEdge() {
        if (rightFloats.isEmpty()) {
            return OptionalInt.empty();
        }
        return OptionalInt.of(getRightFloatLeftEdgeOr(0));
    }

    /**
     * Returns the left edge of the furthest-left right float, or {@code fallback} if there are none.
     * Prefer this over {@link #getRightFloatLeftEdge()} in hot paths to avoid {@link OptionalInt} allocation.
     */
    public int getRightFloatLeftEdgeOr(int fallback) {
        if (rightFloats.isEmpty()) {
            return fallback;
        }
        if (cachedRightFloatLeftEdge == Integer.MAX_VALUE) {
            int minLeft = Integer.MAX_VALUE - 1;
            for (var f : rightFloats) {
                int x = f.x();
                if (x < minLeft) minLeft = x;
            }
            cachedRightFloatLeftEdge = minLeft;
        }
        return cachedRightFloatLeftEdge;
    }

    /** Clears all pending floats and returns the lowest y level below the cleared floats. */
    public OptionalInt clearFloats(boolean left, boolean right) {
        if (left && right) {
            var result = getMaxBottom(leftFloats, rightFloats);
            leftFloats.clear();
            rightFloats.clear();
            cachedLeftFloatRightEdge = Integer.MIN_VALUE;
            cachedRightFloatLeftEdge = Integer.MAX_VALUE;
            return result;
        } else if (left) {
            var result = getMaxBottom(leftFloats);
            leftFloats.clear();
            cachedLeftFloatRightEdge = Integer.MIN_VALUE;
            return result;
        } else if (right) {
            var result = getMaxBottom(rightFloats);
            rightFloats.clear();
            cachedRightFloatLeftEdge = Integer.MAX_VALUE;
            return result;
        } else {
            return OptionalInt.empty();
        }
    }

    /** Removes all floats whose bottom edge is at or above the given y position. */
    public void clearFloatsAbove(int y) {
        boolean leftChanged = leftFloats.removeIf(f -> f.bottom() <= y);
        boolean rightChanged = rightFloats.removeIf(f -> f.bottom() <= y);
        if (leftChanged) cachedLeftFloatRightEdge = Integer.MIN_VALUE;
        if (rightChanged) cachedRightFloatLeftEdge = Integer.MAX_VALUE;
    }

    @Override
    public float getAdvance(int codePoint, ResolvedTextStyle style) {
        return fontMetrics.getAdvance(codePoint, style);
    }

    @Override
    public int getLineHeight(ResolvedTextStyle style) {
        return fontMetrics.getLineHeight(style);
    }

    /**
     * If there's a float whose bottom edge is below the given y coordinate, return that bottom edge.
     */
    public OptionalInt getNextFloatBottomEdge(int y) {
        int nextBottom = Integer.MAX_VALUE;
        boolean found = false;

        for (var bounds : leftFloats) {
            var bottom = bounds.bottom();
            if (bottom > y && bottom < nextBottom) {
                nextBottom = bottom;
                found = true;
            }
        }

        for (var bounds : rightFloats) {
            var bottom = bounds.bottom();
            if (bottom > y && bottom < nextBottom) {
                nextBottom = bottom;
                found = true;
            }
        }

        return found ? OptionalInt.of(nextBottom) : OptionalInt.empty();
    }

    public static OptionalInt getMaxBottom(List<LytRect> boundsList) {
        int maxBottom = Integer.MIN_VALUE;
        boolean found = false;

        for (var bounds : boundsList) {
            maxBottom = Math.max(maxBottom, bounds.bottom());
            found = true;
        }

        return found ? OptionalInt.of(maxBottom) : OptionalInt.empty();
    }

    public static OptionalInt getMaxBottom(List<LytRect> leftBounds, List<LytRect> rightBounds) {
        int maxBottom = Integer.MIN_VALUE;
        boolean found = false;

        for (var bounds : leftBounds) {
            maxBottom = Math.max(maxBottom, bounds.bottom());
            found = true;
        }

        for (var bounds : rightBounds) {
            maxBottom = Math.max(maxBottom, bounds.bottom());
            found = true;
        }

        return found ? OptionalInt.of(maxBottom) : OptionalInt.empty();
    }
}
