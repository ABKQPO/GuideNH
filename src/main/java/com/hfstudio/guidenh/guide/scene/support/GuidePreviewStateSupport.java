package com.hfstudio.guidenh.guide.scene.support;

import java.lang.reflect.Method;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.hfstudio.guidenh.guide.scene.level.GuidebookLevel;

public class GuidePreviewStateSupport {

    private static final Logger LOG = LogManager.getLogger("GuideNH/ScenePreview");
    private static final String AE2_SUPPORT_CLASS = "com.hfstudio.guidenh.guide.scene.support.ae2.GuideAe2PreviewSupport";

    private static volatile Method ae2PrepareMethod;
    private static volatile boolean ae2ResolutionAttempted;
    private static volatile boolean ae2LoadFailureLogged;
    private static volatile boolean ae2InvokeFailureLogged;

    private GuidePreviewStateSupport() {}

    public static void prepare(GuidebookLevel level) {
        Method ae2Method = resolveAe2PrepareMethod();
        if (ae2Method == null) {
            return;
        }
        try {
            ae2Method.invoke(null, level);
        } catch (Throwable t) {
            if (!ae2InvokeFailureLogged) {
                ae2InvokeFailureLogged = true;
                LOG.warn("AE2 preview state preparation failed; 3D cable preview may be incomplete", t);
            }
        }
    }

    private static Method resolveAe2PrepareMethod() {
        if (ae2ResolutionAttempted) {
            return ae2PrepareMethod;
        }
        ae2ResolutionAttempted = true;
        try {
            Class<?> supportClass = Class.forName(AE2_SUPPORT_CLASS);
            ae2PrepareMethod = supportClass.getMethod("prepare", GuidebookLevel.class);
        } catch (Throwable t) {
            ae2PrepareMethod = null;
            if (!ae2LoadFailureLogged) {
                ae2LoadFailureLogged = true;
                LOG.warn("AE2 preview support is unavailable; continuing without AE2 preview state sync", t);
            }
        }
        return ae2PrepareMethod;
    }
}
