package com.hfstudio.guidenh.guide.scene.support;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.hfstudio.guidenh.compat.Mods;
import com.hfstudio.guidenh.compat.ae2.Ae2Helpers;
import com.hfstudio.guidenh.guide.scene.level.GuidebookLevel;

/**
 * Cross-mod entry point for preparing guide preview state. Each branch is gated by a
 * {@link Mods#isModLoaded()} check so the JVM never resolves the helper class (and thus
 * its mod-specific imports) when the corresponding mod is absent.
 */
public final class GuidePreviewStateSupport {

    public static final Logger LOG = LogManager.getLogger("GuideNH/ScenePreview");
    public static volatile boolean ae2InvokeFailureLogged;

    private GuidePreviewStateSupport() {}

    public static void prepare(GuidebookLevel level) {
        if (Mods.AE2.isModLoaded()) {
            try {
                Ae2Helpers.prepare(level);
            } catch (Throwable t) {
                if (!ae2InvokeFailureLogged) {
                    ae2InvokeFailureLogged = true;
                    GuideDebugLog
                        .warn(LOG, "AE2 preview state preparation failed; 3D cable preview may be incomplete", t);
                }
            }
        }
    }
}
