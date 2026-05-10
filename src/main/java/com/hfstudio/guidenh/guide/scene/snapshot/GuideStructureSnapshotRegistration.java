package com.hfstudio.guidenh.guide.scene.snapshot;

import com.hfstudio.guidenh.CommonProxy;
import com.hfstudio.guidenh.integration.api.GuideNhIntegrationRegistry;
import com.hfstudio.guidenh.integration.preview.GuideCompatStructurePreviewBootstrap;

/**
 * Registers default structure snapshot / preview contributors. Call once from {@link CommonProxy} {@code preInit}.
 */
public final class GuideStructureSnapshotRegistration {

    private GuideStructureSnapshotRegistration() {}

    public static void registerAll() {
        GuideCompatStructurePreviewBootstrap.registerServerPreviewSupplements();
        StructureExportPipeline.register(new ServerPreviewSupplementStructureExportContributor());
        StructureImportPipeline.register(new ServerPreviewSupplementStructureImportContributor());

        for (PreviewPrepareContributor contributor : GuideNhIntegrationRegistry.global()
            .previewPrepareContributors()) {
            PreviewPreparePipeline.register(contributor);
        }
    }
}
