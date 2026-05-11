package com.hfstudio.guidenh.guide.scene;

import java.util.Locale;

public enum BlockStatsCorner {

    TOP_RIGHT,
    TOP_LEFT,
    BOTTOM_RIGHT,
    BOTTOM_LEFT;

    public static BlockStatsCorner fromString(String value, BlockStatsCorner fallback) {
        if (value == null) {
            return fallback;
        }
        String normalized = value.trim()
            .replace("-", "")
            .replace("_", "")
            .toLowerCase(Locale.ROOT);
        if (normalized.isEmpty()) {
            return fallback;
        }
        return switch (normalized) {
            case "topright", "righttop", "tr" -> TOP_RIGHT;
            case "topleft", "lefttop", "tl" -> TOP_LEFT;
            case "bottomright", "rightbottom", "br" -> BOTTOM_RIGHT;
            case "bottomleft", "leftbottom", "bl" -> BOTTOM_LEFT;
            default -> fallback;
        };
    }
}
