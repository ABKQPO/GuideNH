package com.hfstudio.guidenh.compat.ae2;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.hfstudio.guidenh.compat.Mods;
import com.hfstudio.guidenh.guide.scene.level.GuidebookLevel;
import com.hfstudio.guidenh.guide.scene.snapshot.PreviewPrepareContributor;
import com.hfstudio.guidenh.guide.scene.support.GuideDebugLog;

public final class Ae2PreviewPrepareContributor implements PreviewPrepareContributor {

    private static final Logger LOG = LogManager.getLogger("GuideNH/ScenePreview");
    private static volatile boolean invokeFailureLogged;

    @Override
    public int priority() {
        return 20;
    }

    @Override
    public void prepare(GuidebookLevel level) {
        if (!Mods.AE2.isModLoaded()) {
            return;
        }
        try {
            Ae2Helpers.prepare(level);
        } catch (Throwable t) {
            if (!invokeFailureLogged) {
                invokeFailureLogged = true;
                GuideDebugLog.warn(LOG, "AE2 preview state preparation failed; 3D cable preview may be incomplete", t);
            }
        }
    }
}
