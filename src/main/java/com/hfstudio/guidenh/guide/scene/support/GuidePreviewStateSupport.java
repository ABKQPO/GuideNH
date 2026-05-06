package com.hfstudio.guidenh.guide.scene.support;

import com.hfstudio.guidenh.guide.scene.level.GuidebookLevel;
import com.hfstudio.guidenh.guide.scene.snapshot.PreviewPreparePipeline;

/**
 * Cross-mod entry point for preparing guide preview state. Actual logic lives in registered
 * {@link com.hfstudio.guidenh.guide.scene.snapshot.PreviewPrepareContributor}s.
 */
public final class GuidePreviewStateSupport {

    private GuidePreviewStateSupport() {}

    public static void prepare(GuidebookLevel level) {
        PreviewPreparePipeline.prepare(level);
    }
}
