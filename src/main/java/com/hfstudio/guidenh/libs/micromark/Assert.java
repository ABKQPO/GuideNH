package com.hfstudio.guidenh.libs.micromark;

public class Assert {

    private Assert() {}

    public static void check(boolean condition, String message) {
        if (!condition) {
            throw new IllegalStateException(message);
        }
    }
}
