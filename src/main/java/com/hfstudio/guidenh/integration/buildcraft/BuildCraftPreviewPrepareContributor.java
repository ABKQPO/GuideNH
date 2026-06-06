package com.hfstudio.guidenh.integration.buildcraft;

import com.hfstudio.guidenh.guide.scene.level.GuidebookLevel;
import com.hfstudio.guidenh.guide.scene.snapshot.PreviewPrepareContributor;
import com.hfstudio.guidenh.guide.scene.support.GuideDebugLog;
import com.hfstudio.guidenh.integration.Mods;

public class BuildCraftPreviewPrepareContributor implements PreviewPrepareContributor {

    private static volatile boolean invokeFailureLogged;

    @Override
    public int priority() {
        return 30;
    }

    @Override
    public void prepare(GuidebookLevel level) {
        if (!Mods.BuildCraftTransport.isModLoaded()) {
            return;
        }
        try {
            BuildCraftHelpers.prepare(level);
        } catch (Throwable t) {
            if (!invokeFailureLogged) {
                invokeFailureLogged = true;
                GuideDebugLog.warn("BuildCraft preview state preparation failed; pipe textures may be wrong", t);
            }
        }
    }
}
