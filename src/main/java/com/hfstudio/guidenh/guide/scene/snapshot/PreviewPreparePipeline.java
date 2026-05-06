package com.hfstudio.guidenh.guide.scene.snapshot;

import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.hfstudio.guidenh.guide.scene.level.GuidebookLevel;

public final class PreviewPreparePipeline {

    private static final Logger LOG = LogManager.getLogger("GuideNH/ScenePreview");

    private static final List<PreviewPrepareContributor> CONTRIBUTORS = new CopyOnWriteArrayList<>();

    private PreviewPreparePipeline() {}

    public static void register(PreviewPrepareContributor contributor) {
        CONTRIBUTORS.add(contributor);
        CONTRIBUTORS.sort(Comparator.comparingInt(PreviewPrepareContributor::priority));
    }

    public static void prepare(GuidebookLevel level) {
        for (PreviewPrepareContributor c : CONTRIBUTORS) {
            try {
                c.prepare(level);
            } catch (Throwable t) {
                LOG.warn(
                    "Preview prepare failed: {}",
                    c.getClass()
                        .getName(),
                    t);
            }
        }
    }
}
