package com.hfstudio.guidenh.guide.scene.snapshot;

import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class StructureExportPipeline {

    private static final Logger LOG = LogManager.getLogger("GuideNH/StructureExport");
    private static final Comparator<StructureExportContributor> PRIORITY_COMPARATOR = Comparator
        .comparingInt(StructureExportContributor::priority);

    private static final List<StructureExportContributor> CONTRIBUTORS = new CopyOnWriteArrayList<>();

    protected StructureExportPipeline() {}

    public static void register(StructureExportContributor contributor) {
        CONTRIBUTORS.add(contributor);
        CONTRIBUTORS.sort(PRIORITY_COMPARATOR);
    }

    public static void beginExport(ExportSession session) {
        for (StructureExportContributor c : CONTRIBUTORS) {
            try {
                c.beginExport(session);
            } catch (Throwable t) {
                LOG.warn(
                    "Structure export beginExport failed: {}",
                    c.getClass()
                        .getName(),
                    t);
            }
        }
    }

    public static void contributeBlock(ExportBlockContext ctx) {
        for (StructureExportContributor c : CONTRIBUTORS) {
            try {
                c.contributeBlock(ctx);
            } catch (Throwable t) {
                LOG.warn(
                    "Structure export contributeBlock failed: {}",
                    c.getClass()
                        .getName(),
                    t);
            }
        }
    }

    public static void endExport(ExportSession session) {
        for (StructureExportContributor c : CONTRIBUTORS) {
            try {
                c.endExport(session);
            } catch (Throwable t) {
                LOG.warn(
                    "Structure export endExport failed: {}",
                    c.getClass()
                        .getName(),
                    t);
            }
        }
    }
}
