package com.hfstudio.guidenh.integration;

import com.hfstudio.guidenh.guide.scene.snapshot.GuideStructureSnapshotRegistration;
import com.hfstudio.guidenh.integration.api.GuideNhIntegrationRegistry;
import com.hfstudio.guidenh.integration.api.IntegrationModDescriptor;
import com.hfstudio.guidenh.integration.betterquesting.BqCompat;
import com.hfstudio.guidenh.integration.forgemultipart.ForgeMultipartBlockExportIdProvider;
import com.hfstudio.guidenh.integration.gregtech.GregTechHelpers;

public class GuideNhIntegrationBootstrap {

    private GuideNhIntegrationBootstrap() {}

    public static void preInitCommon() {
        registerKnownModDescriptors();
        GuideNhIntegrationRegistry.global()
            .registerItemStackNormalizationProvider(GregTechHelpers::applyOreDictUnification);
        GuideNhIntegrationRegistry.global()
            .registerBlockExportIdProvider(new ForgeMultipartBlockExportIdProvider());
        registerGuideHooks();
        GuideStructureSnapshotRegistration.registerAll();
    }

    public static void registerKnownModDescriptors() {
        for (Mods mod : Mods.values()) {
            GuideNhIntegrationRegistry.global()
                .registerModDescriptor(
                    new IntegrationModDescriptor(mod.getID(), mod.getResourceLocation(), mod::isModLoaded));
        }
    }

    public static void registerGuideHooks() {
        if (Mods.BetterQuesting.isModLoaded()) {
            GuideNhIntegrationRegistry.global()
                .registerGuideBuilderIntegrationHook(BqCompat::attachQuestIndex);
            GuideNhIntegrationRegistry.global()
                .registerTagCompilerProvider(BqCompat::appendCompilers);
        }
    }

}
