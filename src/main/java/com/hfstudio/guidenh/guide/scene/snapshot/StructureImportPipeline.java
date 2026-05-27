package com.hfstudio.guidenh.guide.scene.snapshot;

import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import com.hfstudio.guidenh.guide.scene.support.GuideDebugLog;

public class StructureImportPipeline {

    private static final Comparator<StructureImportContributor> PRIORITY_COMPARATOR = Comparator
        .comparingInt(StructureImportContributor::priority);

    private static final List<StructureImportContributor> CONTRIBUTORS = new CopyOnWriteArrayList<>();

    protected StructureImportPipeline() {}

    public static void register(StructureImportContributor contributor) {
        CONTRIBUTORS.add(contributor);
        CONTRIBUTORS.sort(PRIORITY_COMPARATOR);
    }

    public static void apply(ImportBlockContext ctx) {
        for (StructureImportContributor c : CONTRIBUTORS) {
            try {
                c.apply(ctx);
            } catch (Throwable t) {
                GuideDebugLog.warn(
                    "Structure import apply failed: {}",
                    c.getClass()
                        .getName(),
                    t);
            }
        }
    }
}
