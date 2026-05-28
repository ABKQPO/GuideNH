package com.hfstudio.guidenh.guide.sound;

import java.util.Locale;

public enum GuideSoundTrigger {

    CLICK,
    HOVER,
    ENTER;

    public static GuideSoundTrigger parse(String value, GuideSoundTrigger defaultValue) {
        if (value == null || value.trim()
            .isEmpty()) {
            return defaultValue;
        }
        String normalized = value.trim()
            .toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "click" -> CLICK;
            case "hover" -> HOVER;
            case "enter" -> ENTER;
            default -> defaultValue;
        };
    }
}
