package com.hfstudio.guidenh.integration.railcraft;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.hfstudio.guidenh.guide.scene.level.GuidebookLevel;
import com.hfstudio.guidenh.guide.scene.snapshot.PreviewPrepareContributor;
import com.hfstudio.guidenh.guide.scene.support.GuideDebugLog;
import com.hfstudio.guidenh.integration.Mods;

public class RailcraftPreviewPrepareContributor implements PreviewPrepareContributor {

    private static final Logger LOG = LogManager.getLogger("GuideNH/ScenePreview");
    private static volatile boolean invokeFailureLogged;

    @Override
    public int priority() {
        return 35;
    }

    @Override
    public void prepare(GuidebookLevel level) {
        if (!Mods.Railcraft.isModLoaded()) {
            return;
        }
        try {
            RailcraftPreviewHelpers.prepareMultiblocks(level);
        } catch (Throwable t) {
            if (!invokeFailureLogged) {
                invokeFailureLogged = true;
                GuideDebugLog.warn(
                    LOG,
                    "Railcraft multiblock preview preparation failed; multiblock textures may be inactive",
                    t);
            }
        }
    }
}
