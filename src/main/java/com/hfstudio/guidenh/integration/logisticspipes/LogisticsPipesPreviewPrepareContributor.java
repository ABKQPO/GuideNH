package com.hfstudio.guidenh.integration.logisticspipes;

import com.hfstudio.guidenh.guide.scene.level.GuidebookLevel;
import com.hfstudio.guidenh.guide.scene.snapshot.PreviewPrepareContributor;
import com.hfstudio.guidenh.guide.scene.support.GuideDebugLog;
import com.hfstudio.guidenh.integration.Mods;

public class LogisticsPipesPreviewPrepareContributor implements PreviewPrepareContributor {

    public static volatile boolean invokeFailureLogged;

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
                GuideDebugLog.warn("LogisticsPipes preview state preparation failed; pipe rendering may be wrong", t);
            }
        }
    }
}
