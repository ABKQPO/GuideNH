package com.hfstudio.guidenh.guide.scene.snapshot;

import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import com.hfstudio.guidenh.guide.scene.level.GuidebookLevel;
import com.hfstudio.guidenh.guide.scene.support.GuideDebugLog;

public class PreviewPreparePipeline {

    private static final Comparator<PreviewPrepareContributor> PRIORITY_COMPARATOR = Comparator
        .comparingInt(PreviewPrepareContributor::priority);

    private static final List<PreviewPrepareContributor> CONTRIBUTORS = new CopyOnWriteArrayList<>();

    protected PreviewPreparePipeline() {}

    public static void register(PreviewPrepareContributor contributor) {
        CONTRIBUTORS.add(contributor);
        CONTRIBUTORS.sort(PRIORITY_COMPARATOR);
    }

    public static void prepare(GuidebookLevel level) {
        for (PreviewPrepareContributor c : CONTRIBUTORS) {
            try {
                c.prepare(level);
            } catch (Throwable t) {
                GuideDebugLog.warn(
                    "Preview prepare failed: {}",
                    c.getClass()
                        .getName(),
                    t);
            }
        }
    }
}
