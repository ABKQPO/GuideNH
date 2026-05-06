package com.hfstudio.guidenh.guide.scene.snapshot;

import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class StructureImportPipeline {

    private static final Logger LOG = LogManager.getLogger("GuideNH/StructureImport");

    private static final List<StructureImportContributor> CONTRIBUTORS = new CopyOnWriteArrayList<>();

    private StructureImportPipeline() {}

    public static void register(StructureImportContributor contributor) {
        CONTRIBUTORS.add(contributor);
        CONTRIBUTORS.sort(Comparator.comparingInt(StructureImportContributor::priority));
    }

    public static void apply(ImportBlockContext ctx) {
        for (StructureImportContributor c : CONTRIBUTORS) {
            try {
                c.apply(ctx);
            } catch (Throwable t) {
                LOG.warn("Structure import apply failed: {}", c.getClass().getName(), t);
            }
        }
    }
}
