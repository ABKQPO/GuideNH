package com.hfstudio.guidenh.compat.buildcraft;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.hfstudio.guidenh.compat.Mods;
import com.hfstudio.guidenh.guide.scene.level.GuidebookLevel;
import com.hfstudio.guidenh.guide.scene.snapshot.PreviewPrepareContributor;
import com.hfstudio.guidenh.guide.scene.support.GuideDebugLog;

public final class BuildCraftPreviewPrepareContributor implements PreviewPrepareContributor {

    private static final Logger LOG = LogManager.getLogger("GuideNH/ScenePreview");
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
                GuideDebugLog.warn(LOG, "BuildCraft preview state preparation failed; pipe textures may be wrong", t);
            }
        }
    }
}
