package com.hfstudio.guidenh.guide.color;

public enum LightDarkMode {

    LIGHT_MODE,
    DARK_MODE;

    public static LightDarkMode current() {
        return DARK_MODE;
    }
}
