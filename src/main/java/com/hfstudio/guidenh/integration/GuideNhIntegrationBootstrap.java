package com.hfstudio.guidenh.integration;

import com.hfstudio.guidenh.guide.scene.snapshot.GuideStructureSnapshotRegistration;
import com.hfstudio.guidenh.integration.ae2.Ae2BlockStatsProvider;
import com.hfstudio.guidenh.integration.ae2.Ae2FakeWorldIntegration;
import com.hfstudio.guidenh.integration.ae2.Ae2PreviewPrepareContributor;
import com.hfstudio.guidenh.integration.api.GuideNhIntegrationRegistry;
import com.hfstudio.guidenh.integration.api.IntegrationModDescriptor;
import com.hfstudio.guidenh.integration.betterquesting.BqCompat;
import com.hfstudio.guidenh.integration.buildcraft.BuildCraftBlockDisplayProvider;
import com.hfstudio.guidenh.integration.buildcraft.BuildCraftPreviewPrepareContributor;
import com.hfstudio.guidenh.integration.carpentersblocks.CarpentersBlocksBlockDisplayNameProvider;
import com.hfstudio.guidenh.integration.carpentersblocks.CarpentersBlocksBlockDisplayProvider;
import com.hfstudio.guidenh.integration.carpentersblocks.CarpentersBlocksBlockStatsProvider;
import com.hfstudio.guidenh.integration.distanthorizons.DistantHorizonsFakeWorldIntegration;
import com.hfstudio.guidenh.integration.forgemultipart.ForgeMultipartBlockExportIdProvider;
import com.hfstudio.guidenh.integration.forgemultipart.ForgeMultipartBlockStatsProvider;
import com.hfstudio.guidenh.integration.forgemultipart.ForgeMultipartPreviewTileEntityFinalizer;
import com.hfstudio.guidenh.integration.forgemultipart.ForgeMultipartPreviewTileEntityProvider;
import com.hfstudio.guidenh.integration.gregtech.GregTechFakeWorldIntegration;
import com.hfstudio.guidenh.integration.gregtech.GregTechHelpers;
import com.hfstudio.guidenh.integration.gregtech.GregTechPreviewPrepareContributor;
import com.hfstudio.guidenh.integration.logisticspipes.LogisticsPipesBlockDisplayProvider;
import com.hfstudio.guidenh.integration.logisticspipes.LogisticsPipesPreviewPrepareContributor;
import com.hfstudio.guidenh.integration.nei.NeiRawRecipeHandlerProvider;
import com.hfstudio.guidenh.integration.nei.NeiRecipeAnimationUpdateProvider;
import com.hfstudio.guidenh.integration.nei.NeiRecipeAvailabilityProvider;
import com.hfstudio.guidenh.integration.nei.NeiRecipeDrawableRenderProvider;
import com.hfstudio.guidenh.integration.nei.NeiRecipeEntryProvider;
import com.hfstudio.guidenh.integration.nei.NeiRecipeHandlerMetadataProvider;
import com.hfstudio.guidenh.integration.nei.NeiRecipeHandlerRenderProvider;
import com.hfstudio.guidenh.integration.nei.NeiRecipeHandlerSlotProvider;
import com.hfstudio.guidenh.integration.nei.NeiRecipeItemTooltipProvider;
import com.hfstudio.guidenh.integration.tinkerconstruct.TinkersConstructPreviewPrepareContributor;

public final class GuideNhIntegrationBootstrap {

    private GuideNhIntegrationBootstrap() {}

    public static void preInitCommon() {
        registerKnownModDescriptors();
        GuideNhIntegrationRegistry.global()
            .registerItemStackNormalizationProvider(GregTechHelpers::applyOreDictUnification);
        GuideNhIntegrationRegistry.global()
            .registerBlockDisplayProvider(new LogisticsPipesBlockDisplayProvider());
        GuideNhIntegrationRegistry.global()
            .registerBlockDisplayProvider(new BuildCraftBlockDisplayProvider());
        GuideNhIntegrationRegistry.global()
            .registerBlockDisplayProvider(new CarpentersBlocksBlockDisplayProvider());
        GuideNhIntegrationRegistry.global()
            .registerBlockDisplayNameProvider(new CarpentersBlocksBlockDisplayNameProvider());
        GuideNhIntegrationRegistry.global()
            .registerBlockExportIdProvider(new ForgeMultipartBlockExportIdProvider());
        GuideNhIntegrationRegistry.global()
            .registerPreviewTileEntityProvider(new ForgeMultipartPreviewTileEntityProvider());
        GuideNhIntegrationRegistry.global()
            .registerPreviewTileEntityFinalizer(new ForgeMultipartPreviewTileEntityFinalizer());
        registerFakeWorldIntegrations();
        registerGuideHooks();
        registerRecipeHandlerProviders();
        registerRecipeEntryProviders();
        registerRecipeTooltipProviders();
        registerRecipeAnimationUpdateProviders();
        registerRecipeHandlerMetadataProviders();
        registerRecipeHandlerSlotProviders();
        registerRecipeAvailabilityProviders();
        registerRecipeDrawableRenderProviders();
        registerRecipeHandlerRenderProviders();
        registerBlockStatsProviders();
        registerPreviewPrepareContributors();
        GuideStructureSnapshotRegistration.registerAll();
    }

    public static void registerKnownModDescriptors() {
        for (Mods mod : Mods.values()) {
            GuideNhIntegrationRegistry.global()
                .registerModDescriptor(
                    new IntegrationModDescriptor(mod.getID(), mod.getResourceLocation(), mod::isModLoaded));
        }
    }

    public static void registerPreviewPrepareContributors() {
        GuideNhIntegrationRegistry.global()
            .registerPreviewPrepareContributor(new GregTechPreviewPrepareContributor());
        GuideNhIntegrationRegistry.global()
            .registerPreviewPrepareContributor(new BuildCraftPreviewPrepareContributor());
        GuideNhIntegrationRegistry.global()
            .registerPreviewPrepareContributor(new LogisticsPipesPreviewPrepareContributor());
        GuideNhIntegrationRegistry.global()
            .registerPreviewPrepareContributor(new TinkersConstructPreviewPrepareContributor());
        if (Mods.AE2.isModLoaded()) {
            GuideNhIntegrationRegistry.global()
                .registerPreviewPrepareContributor(new Ae2PreviewPrepareContributor());
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

    public static void registerRecipeHandlerProviders() {
        GuideNhIntegrationRegistry.global()
            .registerRawRecipeHandlerProvider(new NeiRawRecipeHandlerProvider());
    }

    public static void registerRecipeTooltipProviders() {
        GuideNhIntegrationRegistry.global()
            .registerRecipeItemTooltipProvider(new NeiRecipeItemTooltipProvider());
    }

    public static void registerRecipeEntryProviders() {
        GuideNhIntegrationRegistry.global()
            .registerRecipeEntryProvider(new NeiRecipeEntryProvider());
    }

    public static void registerRecipeAnimationUpdateProviders() {
        GuideNhIntegrationRegistry.global()
            .registerRecipeAnimationUpdateProvider(new NeiRecipeAnimationUpdateProvider());
    }

    public static void registerRecipeHandlerMetadataProviders() {
        GuideNhIntegrationRegistry.global()
            .registerRecipeHandlerMetadataProvider(new NeiRecipeHandlerMetadataProvider());
    }

    public static void registerRecipeHandlerSlotProviders() {
        GuideNhIntegrationRegistry.global()
            .registerRecipeHandlerSlotProvider(new NeiRecipeHandlerSlotProvider());
    }

    public static void registerRecipeAvailabilityProviders() {
        GuideNhIntegrationRegistry.global()
            .registerRecipeAvailabilityProvider(new NeiRecipeAvailabilityProvider());
    }

    public static void registerRecipeDrawableRenderProviders() {
        GuideNhIntegrationRegistry.global()
            .registerRecipeDrawableRenderProvider(new NeiRecipeDrawableRenderProvider());
    }

    public static void registerRecipeHandlerRenderProviders() {
        GuideNhIntegrationRegistry.global()
            .registerRecipeHandlerRenderProvider(new NeiRecipeHandlerRenderProvider());
    }

    public static void registerBlockStatsProviders() {
        GuideNhIntegrationRegistry.global()
            .registerBlockStatsProvider(new Ae2BlockStatsProvider());
        GuideNhIntegrationRegistry.global()
            .registerBlockStatsProvider(new ForgeMultipartBlockStatsProvider());
        GuideNhIntegrationRegistry.global()
            .registerBlockStatsProvider(new CarpentersBlocksBlockStatsProvider());
    }

    public static void registerFakeWorldIntegrations() {
        GuideNhIntegrationRegistry.global()
            .registerFakeWorldIntegration(new GregTechFakeWorldIntegration());
        GuideNhIntegrationRegistry.global()
            .registerFakeWorldIntegration(new Ae2FakeWorldIntegration());
        GuideNhIntegrationRegistry.global()
            .registerFakeWorldIntegration(new DistantHorizonsFakeWorldIntegration());
    }
}
