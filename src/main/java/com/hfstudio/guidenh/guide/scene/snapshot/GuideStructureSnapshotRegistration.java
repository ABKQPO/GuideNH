package com.hfstudio.guidenh.guide.scene.snapshot;

import com.hfstudio.guidenh.compat.Mods;
import com.hfstudio.guidenh.compat.ae2.Ae2CableSidecarImportContributor;
import com.hfstudio.guidenh.compat.ae2.Ae2CableStreamExportContributor;
import com.hfstudio.guidenh.compat.ae2.Ae2PreviewPrepareContributor;
import com.hfstudio.guidenh.compat.buildcraft.BuildCraftPreviewPrepareContributor;
import com.hfstudio.guidenh.compat.gregtech.GregTechPreviewPrepareContributor;
import com.hfstudio.guidenh.compat.logisticspipes.LogisticsPipesPreviewPrepareContributor;

/**
 * Registers default structure snapshot / preview contributors. Call once from {@link com.hfstudio.guidenh.CommonProxy}
 * {@code preInit}.
 */
public final class GuideStructureSnapshotRegistration {

    private GuideStructureSnapshotRegistration() {}

    public static void registerAll() {
        PreviewPreparePipeline.register(new GregTechPreviewPrepareContributor());
        PreviewPreparePipeline.register(new BuildCraftPreviewPrepareContributor());
        PreviewPreparePipeline.register(new LogisticsPipesPreviewPrepareContributor());

        if (Mods.AE2.isModLoaded()) {
            StructureExportPipeline.register(new Ae2CableStreamExportContributor());
            StructureImportPipeline.register(new Ae2CableSidecarImportContributor());
            PreviewPreparePipeline.register(new Ae2PreviewPrepareContributor());
        }
    }
}
