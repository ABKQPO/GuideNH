package com.hfstudio.guidenh.guide.scene;

import java.util.Locale;

public enum BlockStatsMode {

    AUTO,
    MANUAL;

    public static BlockStatsMode fromString(String value, BlockStatsMode fallback) {
        if (value == null) {
            return fallback;
        }
        String normalized = value.trim()
            .toLowerCase(Locale.ROOT);
        if (normalized.isEmpty()) {
            return fallback;
        }
        return switch (normalized) {
            case "manual" -> MANUAL;
            case "auto", "automatic" -> AUTO;
            default -> fallback;
        };
    }
}
