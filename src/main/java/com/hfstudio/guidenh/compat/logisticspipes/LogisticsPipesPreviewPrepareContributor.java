package com.hfstudio.guidenh.compat.logisticspipes;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.hfstudio.guidenh.compat.Mods;
import com.hfstudio.guidenh.guide.scene.level.GuidebookLevel;
import com.hfstudio.guidenh.guide.scene.snapshot.PreviewPrepareContributor;
import com.hfstudio.guidenh.guide.scene.support.GuideDebugLog;

public final class LogisticsPipesPreviewPrepareContributor implements PreviewPrepareContributor {

    private static final Logger LOG = LogManager.getLogger("GuideNH/ScenePreview");
    private static volatile boolean invokeFailureLogged;

    @Override
    public int priority() {
        return 40;
    }

    @Override
    public void prepare(GuidebookLevel level) {
        if (!Mods.LogisticsPipes.isModLoaded()) {
            return;
        }
        try {
            LogisticsPipesHelpers.prepare(level);
        } catch (Throwable t) {
            if (!invokeFailureLogged) {
                invokeFailureLogged = true;
                GuideDebugLog
                    .warn(LOG, "LogisticsPipes preview state preparation failed; pipe rendering may be wrong", t);
            }
        }
    }
}
