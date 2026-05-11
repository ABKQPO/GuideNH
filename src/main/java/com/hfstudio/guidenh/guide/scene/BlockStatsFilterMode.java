package com.hfstudio.guidenh.guide.scene;

import java.util.Locale;

public enum BlockStatsFilterMode {

    BLACKLIST,
    WHITELIST;

    public static BlockStatsFilterMode fromString(String value, BlockStatsFilterMode fallback) {
        if (value == null) {
            return fallback;
        }
        String normalized = value.trim()
            .toLowerCase(Locale.ROOT);
        if (normalized.isEmpty()) {
            return fallback;
        }
        return switch (normalized) {
            case "whitelist", "white", "include", "only" -> WHITELIST;
            case "blacklist", "black", "exclude", "except" -> BLACKLIST;
            default -> fallback;
        };
    }
}
