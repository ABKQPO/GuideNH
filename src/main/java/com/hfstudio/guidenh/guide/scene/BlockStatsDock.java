package com.hfstudio.guidenh.guide.scene;

import java.util.Locale;

public enum BlockStatsDock {

    INSIDE,
    LEFT,
    TOP,
    RIGHT,
    BOTTOM;

    public static BlockStatsDock fromString(String value, BlockStatsDock fallback) {
        if (value == null || value.trim()
            .isEmpty()) {
            return fallback;
        }
        String normalized = value.trim()
            .replace("-", "")
            .replace("_", "")
            .toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "left" -> LEFT;
            case "top" -> TOP;
            case "right" -> RIGHT;
            case "bottom" -> BOTTOM;
            case "inside", "overlay" -> INSIDE;
            default -> fallback;
        };
    }

    public boolean isOutside() {
        return this != INSIDE;
    }
}
