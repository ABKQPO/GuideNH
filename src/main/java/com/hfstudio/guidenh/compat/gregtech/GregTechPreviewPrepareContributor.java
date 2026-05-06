package com.hfstudio.guidenh.compat.gregtech;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.hfstudio.guidenh.compat.Mods;
import com.hfstudio.guidenh.guide.scene.level.GuidebookLevel;
import com.hfstudio.guidenh.guide.scene.snapshot.PreviewPrepareContributor;
import com.hfstudio.guidenh.guide.scene.support.GuideDebugLog;

public final class GregTechPreviewPrepareContributor implements PreviewPrepareContributor {

    private static final Logger LOG = LogManager.getLogger("GuideNH/ScenePreview");
    private static volatile boolean invokeFailureLogged;

    @Override
    public int priority() {
        return 10;
    }

    @Override
    public void prepare(GuidebookLevel level) {
        if (!Mods.GregTech.isModLoaded()) {
            return;
        }
        try {
            GregTechHelpers.preparePipeConnections(level);
        } catch (Throwable t) {
            if (!invokeFailureLogged) {
                invokeFailureLogged = true;
                GuideDebugLog
                    .warn(LOG, "GT5 pipe/cable connection preparation failed; pipe connections may be wrong", t);
            }
        }
    }
}
