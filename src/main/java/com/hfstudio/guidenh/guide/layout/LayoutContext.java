package com.hfstudio.guidenh.guide.layout;

import java.util.ArrayList;
import java.util.List;
import java.util.OptionalInt;

import com.hfstudio.guidenh.guide.document.LytRect;
import com.hfstudio.guidenh.guide.style.ResolvedTextStyle;

public class LayoutContext implements FontMetrics {

    private final FontMetrics fontMetrics;

    private final List<LytRect> leftFloats = new ArrayList<>();
    private final List<LytRect> rightFloats = new ArrayList<>();

    public LayoutContext(FontMetrics fontMetrics) {
        this.fontMetrics = fontMetrics;
    }

    public void addLeftFloat(LytRect bounds) {
        leftFloats.add(bounds);
    }

    public void addRightFloat(LytRect bounds) {
        rightFloats.add(bounds);
    }

    public OptionalInt getLeftFloatRightEdge() {
        if (leftFloats.isEmpty()) {
            return OptionalInt.empty();
        }

        int maxRight = Integer.MIN_VALUE;
        for (var bounds : leftFloats) {
            maxRight = Math.max(maxRight, bounds.right());
        }
        return OptionalInt.of(maxRight);
    }

    public OptionalInt getRightFloatLeftEdge() {
        if (rightFloats.isEmpty()) {
            return OptionalInt.empty();
        }

        int minLeft = Integer.MAX_VALUE;
        for (var bounds : rightFloats) {
            minLeft = Math.min(minLeft, bounds.x());
        }
        return OptionalInt.of(minLeft);
    }

    // Clears all pending floats and returns the lowest y level below the cleared floats
    public OptionalInt clearFloats(boolean left, boolean right) {
        if (left && right) {
            var result = getMaxBottom(leftFloats, rightFloats);
            leftFloats.clear();
            rightFloats.clear();
            return result;
        } else if (left) {
            var result = getMaxBottom(leftFloats);
            leftFloats.clear();
            return result;
        } else if (right) {
            var result = getMaxBottom(rightFloats);
            rightFloats.clear();
            return result;
        } else {
            return OptionalInt.empty();
        }
    }

    // Close out all floats above the given y position
    public void clearFloatsAbove(int y) {
        leftFloats.removeIf(f -> f.bottom() <= y);
        rightFloats.removeIf(f -> f.bottom() <= y);
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
