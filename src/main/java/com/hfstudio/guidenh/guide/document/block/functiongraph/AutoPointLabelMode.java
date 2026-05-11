package com.hfstudio.guidenh.guide.document.block.functiongraph;

import java.util.Locale;

public enum AutoPointLabelMode {

    NONE,
    X,
    Y,
    XY;

    public static AutoPointLabelMode fromString(String value, AutoPointLabelMode fallback) {
        if (value == null) {
            return fallback;
        }
        String normalized = value.trim()
            .toLowerCase(Locale.ROOT);
        if (normalized.isEmpty()) {
            return fallback;
        }
        return switch (normalized) {
            case "none", "false", "off" -> NONE;
            case "x" -> X;
            case "y" -> Y;
            case "xy", "both", "true", "on" -> XY;
            default -> fallback;
        };
    }
}
