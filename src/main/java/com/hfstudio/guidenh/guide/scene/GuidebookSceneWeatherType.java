package com.hfstudio.guidenh.guide.scene;

import java.util.Locale;

import org.jetbrains.annotations.Nullable;

public enum GuidebookSceneWeatherType {

    RAIN,
    SNOW;

    public static GuidebookSceneWeatherType fromSerializedName(@Nullable String value) {
        if (value == null) {
            return RAIN;
        }
        return "snow".equals(
            value.trim()
                .toLowerCase(Locale.ROOT)) ? SNOW : RAIN;
    }

    public String getSerializedName() {
        return this == SNOW ? "snow" : "rain";
    }
}
