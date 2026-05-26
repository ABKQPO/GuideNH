package com.hfstudio.guidenh.integration.gregtech;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.hfstudio.guidenh.guide.scene.level.GuidebookLevel;
import com.hfstudio.guidenh.guide.scene.snapshot.PreviewPrepareContributor;
import com.hfstudio.guidenh.guide.scene.support.GuideDebugLog;
import com.hfstudio.guidenh.integration.Mods;

public class GregTechPreviewPrepareContributor implements PreviewPrepareContributor {

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
            GregTechPreviewMultiblockSupport.synchronizePreviewState(level);
        } catch (Throwable t) {
            if (!invokeFailureLogged) {
                invokeFailureLogged = true;
                GuideDebugLog.warn(
                    LOG,
                    "GT5 preview preparation failed; pipe connections or multiblock formed state may be wrong",
                    t);
            }
        }
    }
}
