package com.hfstudio.guidenh.guide.document.block.chart;

import java.util.Locale;

public enum CornerLegendPosition {

    NONE,
    TOP_RIGHT,
    TOP_LEFT,
    BOTTOM_RIGHT,
    BOTTOM_LEFT;

    public static CornerLegendPosition fromString(String value, CornerLegendPosition fallback) {
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
            case "none", "false", "off" -> NONE;
            case "topright", "righttop", "tr" -> TOP_RIGHT;
            case "topleft", "lefttop", "tl" -> TOP_LEFT;
            case "bottomright", "rightbottom", "br" -> BOTTOM_RIGHT;
            case "bottomleft", "leftbottom", "bl" -> BOTTOM_LEFT;
            default -> fallback;
        };
    }
}
