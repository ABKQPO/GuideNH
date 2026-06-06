package com.hfstudio.guidenh.integration.ae2;

import com.hfstudio.guidenh.guide.scene.level.GuidebookLevel;
import com.hfstudio.guidenh.guide.scene.snapshot.PreviewPrepareContributor;
import com.hfstudio.guidenh.guide.scene.support.GuideDebugLog;
import com.hfstudio.guidenh.integration.Mods;

public class Ae2PreviewPrepareContributor implements PreviewPrepareContributor {

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
                GuideDebugLog.warn("AE2 preview state preparation failed; 3D cable preview may be incomplete", t);
            }
        }
    }
}
