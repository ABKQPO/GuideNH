package com.hfstudio.guidenh.guide.scene.support;

import java.lang.reflect.Method;

import com.hfstudio.guidenh.guide.scene.level.GuidebookLevel;

public final class GuidePreviewStateSupport {

    private static final String AE2_SUPPORT_CLASS =
        "com.hfstudio.guidenh.guide.scene.support.ae2.GuideAe2PreviewSupport";

    private static volatile Method ae2PrepareMethod;
    private static volatile boolean ae2ResolutionAttempted;

    private GuidePreviewStateSupport() {}

    public static void prepare(GuidebookLevel level) {
        Method ae2Method = resolveAe2PrepareMethod();
        if (ae2Method == null) {
            return;
        }
        try {
            ae2Method.invoke(null, level);
        } catch (Throwable ignored) {}
    }

    private static Method resolveAe2PrepareMethod() {
        if (ae2ResolutionAttempted) {
            return ae2PrepareMethod;
        }
        ae2ResolutionAttempted = true;
        try {
            Class<?> supportClass = Class.forName(AE2_SUPPORT_CLASS);
            ae2PrepareMethod = supportClass.getMethod("prepare", GuidebookLevel.class);
        } catch (Throwable ignored) {
            ae2PrepareMethod = null;
        }
        return ae2PrepareMethod;
    }
}
