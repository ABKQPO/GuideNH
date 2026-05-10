package com.hfstudio.guidenh.guide.scene.snapshot;

import com.hfstudio.guidenh.integration.Mods;
import com.hfstudio.guidenh.integration.ae2.Ae2PreviewPrepareContributor;
import com.hfstudio.guidenh.integration.buildcraft.BuildCraftPreviewPrepareContributor;
import com.hfstudio.guidenh.integration.gregtech.GregTechPreviewPrepareContributor;
import com.hfstudio.guidenh.integration.logisticspipes.LogisticsPipesPreviewPrepareContributor;
import com.hfstudio.guidenh.integration.preview.GuideCompatStructurePreviewBootstrap;
import com.hfstudio.guidenh.integration.tinkerconstruct.TinkersConstructPreviewPrepareContributor;

/**
 * Registers default structure snapshot / preview contributors. Call once from {@link com.hfstudio.guidenh.CommonProxy}
 * {@code preInit}.
 */
public final class GuideStructureSnapshotRegistration {

    private GuideStructureSnapshotRegistration() {}

    public static void registerAll() {
        GuideCompatStructurePreviewBootstrap.registerServerPreviewSupplements();
        StructureExportPipeline.register(new ServerPreviewSupplementStructureExportContributor());
        StructureImportPipeline.register(new ServerPreviewSupplementStructureImportContributor());

        PreviewPreparePipeline.register(new GregTechPreviewPrepareContributor());
        PreviewPreparePipeline.register(new BuildCraftPreviewPrepareContributor());
        PreviewPreparePipeline.register(new LogisticsPipesPreviewPrepareContributor());
        PreviewPreparePipeline.register(new TinkersConstructPreviewPrepareContributor());

        if (Mods.AE2.isModLoaded()) {
            PreviewPreparePipeline.register(new Ae2PreviewPrepareContributor());
        }
    }
}
