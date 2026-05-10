package com.hfstudio.guidenh.integration;

import com.hfstudio.guidenh.guide.scene.snapshot.GuideStructureSnapshotRegistration;
import com.hfstudio.guidenh.integration.api.GuideNhIntegrationRegistry;
import com.hfstudio.guidenh.integration.buildcraft.BuildCraftBlockDisplayProvider;
import com.hfstudio.guidenh.integration.gregtech.GregTechHelpers;
import com.hfstudio.guidenh.integration.logisticspipes.LogisticsPipesBlockDisplayProvider;

public final class GuideNhIntegrationBootstrap {

    private GuideNhIntegrationBootstrap() {}

    public static void preInitCommon() {
        GuideNhIntegrationRegistry.global()
            .registerItemStackNormalizationProvider(GregTechHelpers::applyOreDictUnification);
        GuideNhIntegrationRegistry.global()
            .registerBlockDisplayProvider(new LogisticsPipesBlockDisplayProvider());
        GuideNhIntegrationRegistry.global()
            .registerBlockDisplayProvider(new BuildCraftBlockDisplayProvider());
        GuideStructureSnapshotRegistration.registerAll();
    }
}
