package com.hfstudio.guidenh.integration.tinkerconstruct;

import com.hfstudio.guidenh.guide.scene.level.GuidebookLevel;
import com.hfstudio.guidenh.guide.scene.snapshot.PreviewPrepareContributor;
import com.hfstudio.guidenh.guide.scene.support.GuideDebugLog;

public class TinkersConstructPreviewPrepareContributor implements PreviewPrepareContributor {

    private static volatile boolean invokeFailureLogged;

    @Override
    public int priority() {
        return 30;
    }

    @Override
    public void prepare(GuidebookLevel level) {
        try {
            TinkersConstructHelpers.prepareSmelteryPreview(level);
        } catch (Throwable t) {
            if (!invokeFailureLogged) {
                invokeFailureLogged = true;
                GuideDebugLog.warn(
                    "TinkersConstruct smeltery preview preparation failed; smeltery may render in inactive state",
                    t);
            }
        }
    }
}
