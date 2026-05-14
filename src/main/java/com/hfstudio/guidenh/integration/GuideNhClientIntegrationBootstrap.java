package com.hfstudio.guidenh.integration;

import com.hfstudio.guidenh.guide.scene.snapshot.GuideStructureSnapshotRegistration;
import com.hfstudio.guidenh.integration.ae2.Ae2BlockStatsProvider;
import com.hfstudio.guidenh.integration.ae2.Ae2FakeWorldIntegration;
import com.hfstudio.guidenh.integration.ae2.Ae2PreviewPrepareContributor;
import com.hfstudio.guidenh.integration.api.GuideNhIntegrationRegistry;
import com.hfstudio.guidenh.integration.api.client.GuideNhClientIntegrationRegistry;
import com.hfstudio.guidenh.integration.bartworks.BartWorksFakeWorldIntegration;
import com.hfstudio.guidenh.integration.betterquesting.BetterQuestingQuestHoverProvider;
import com.hfstudio.guidenh.integration.buildcraft.BuildCraftBlockDisplayProvider;
import com.hfstudio.guidenh.integration.buildcraft.BuildCraftPreviewPrepareContributor;
import com.hfstudio.guidenh.integration.carpentersblocks.CarpentersBlocksBlockDisplayNameProvider;
import com.hfstudio.guidenh.integration.carpentersblocks.CarpentersBlocksBlockDisplayProvider;
import com.hfstudio.guidenh.integration.carpentersblocks.CarpentersBlocksBlockStatsProvider;
import com.hfstudio.guidenh.integration.distanthorizons.DistantHorizonsFakeWorldIntegration;
import com.hfstudio.guidenh.integration.etfuturum.EtFuturumPreviewPlayerElytraProvider;
import com.hfstudio.guidenh.integration.etfuturum.EtFuturumSlimArmProvider;
import com.hfstudio.guidenh.integration.forgemultipart.ForgeMultipartBlockStatsProvider;
import com.hfstudio.guidenh.integration.forgemultipart.ForgeMultipartPreviewBlockRenderProvider;
import com.hfstudio.guidenh.integration.forgemultipart.ForgeMultipartPreviewTileEntityFinalizer;
import com.hfstudio.guidenh.integration.forgemultipart.ForgeMultipartPreviewTileEntityProvider;
import com.hfstudio.guidenh.integration.gregtech.GregTechFakeWorldIntegration;
import com.hfstudio.guidenh.integration.gregtech.GregTechPreviewPrepareContributor;
import com.hfstudio.guidenh.integration.gregtech.GregTechStructureLibControllerIntegration;
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
import com.hfstudio.guidenh.integration.railcraft.RailcraftPreviewPrepareContributor;
import com.hfstudio.guidenh.integration.simpleskinbackport.SimpleSkinBackportPreviewPlayerModelProvider;
import com.hfstudio.guidenh.integration.simpleskinbackport.SimpleSkinBackportSlimArmProvider;
import com.hfstudio.guidenh.integration.structurelib.StructureLibControllerIntegrationRegistry;
import com.hfstudio.guidenh.integration.tinkerconstruct.TinkersConstructPreviewPrepareContributor;

public class GuideNhClientIntegrationBootstrap {

    private GuideNhClientIntegrationBootstrap() {}

    public static void preInitClient() {
        registerBlockDisplayProviders();
        registerPreviewTileEntityProviders();
        registerFakeWorldIntegrations();
        registerRecipeProviders();
        registerBlockStatsProviders();
        registerPreviewPrepareContributors();
        registerStructureLibControllerIntegrations();

        GuideStructureSnapshotRegistration.registerPreviewPrepareContributors();

        if (Mods.SimpleSkinBackport.isModLoaded()) {
            GuideNhClientIntegrationRegistry.global()
                .registerPreviewPlayerSlimArmProvider(new SimpleSkinBackportSlimArmProvider());

            GuideNhClientIntegrationRegistry.global()
                .registerPreviewPlayerModelProvider(new SimpleSkinBackportPreviewPlayerModelProvider());
        }

        if (Mods.EtFuturum.isModLoaded()) {
            GuideNhClientIntegrationRegistry.global()
                .registerPreviewPlayerSlimArmProvider(new EtFuturumSlimArmProvider());

            GuideNhClientIntegrationRegistry.global()
                .registerPreviewPlayerElytraProvider(new EtFuturumPreviewPlayerElytraProvider());
        }

        if (Mods.ForgeMultipart.isModLoaded()) {
            GuideNhClientIntegrationRegistry.global()
                .registerPreviewBlockRenderProvider(new ForgeMultipartPreviewBlockRenderProvider());
        }

        if (Mods.BetterQuesting.isModLoaded()) {
            GuideNhClientIntegrationRegistry.global()
                .registerQuestHoverProvider(new BetterQuestingQuestHoverProvider());
        }
    }

    public static void registerBlockDisplayProviders() {
        if (Mods.LogisticsPipes.isModLoaded()) {
            GuideNhIntegrationRegistry.global()
                .registerBlockDisplayProvider(new LogisticsPipesBlockDisplayProvider());
        }

        if (Mods.BuildCraftTransport.isModLoaded()) {
            GuideNhIntegrationRegistry.global()
                .registerBlockDisplayProvider(new BuildCraftBlockDisplayProvider());
        }

        if (Mods.CarpentersBlocks.isModLoaded()) {
            GuideNhIntegrationRegistry.global()
                .registerBlockDisplayProvider(new CarpentersBlocksBlockDisplayProvider());

            GuideNhIntegrationRegistry.global()
                .registerBlockDisplayNameProvider(new CarpentersBlocksBlockDisplayNameProvider());
        }
    }

    public static void registerPreviewTileEntityProviders() {
        if (Mods.ForgeMultipart.isModLoaded()) {
            GuideNhIntegrationRegistry.global()
                .registerPreviewTileEntityProvider(new ForgeMultipartPreviewTileEntityProvider());

            GuideNhIntegrationRegistry.global()
                .registerPreviewTileEntityFinalizer(new ForgeMultipartPreviewTileEntityFinalizer());
        }
    }

    public static void registerFakeWorldIntegrations() {
        if (Mods.GregTech.isModLoaded()) {
            GuideNhIntegrationRegistry.global()
                .registerFakeWorldIntegration(new GregTechFakeWorldIntegration());
        }

        if (Mods.BartWorks.isModLoaded()) {
            GuideNhIntegrationRegistry.global()
                .registerFakeWorldIntegration(new BartWorksFakeWorldIntegration());
        }

        if (Mods.AE2.isModLoaded()) {
            GuideNhIntegrationRegistry.global()
                .registerFakeWorldIntegration(new Ae2FakeWorldIntegration());
        }

        if (Mods.DistantHorizons.isModLoaded()) {
            GuideNhIntegrationRegistry.global()
                .registerFakeWorldIntegration(new DistantHorizonsFakeWorldIntegration());
        }
    }

    public static void registerRecipeProviders() {
        if (Mods.NotEnoughItems.isModLoaded()) {
            GuideNhIntegrationRegistry.global()
                .registerRawRecipeHandlerProvider(new NeiRawRecipeHandlerProvider());

            GuideNhIntegrationRegistry.global()
                .registerRecipeEntryProvider(new NeiRecipeEntryProvider());

            GuideNhIntegrationRegistry.global()
                .registerRecipeItemTooltipProvider(new NeiRecipeItemTooltipProvider());

            GuideNhIntegrationRegistry.global()
                .registerRecipeAnimationUpdateProvider(new NeiRecipeAnimationUpdateProvider());

            GuideNhIntegrationRegistry.global()
                .registerRecipeHandlerMetadataProvider(new NeiRecipeHandlerMetadataProvider());

            GuideNhIntegrationRegistry.global()
                .registerRecipeHandlerSlotProvider(new NeiRecipeHandlerSlotProvider());

            GuideNhIntegrationRegistry.global()
                .registerRecipeAvailabilityProvider(new NeiRecipeAvailabilityProvider());

            GuideNhIntegrationRegistry.global()
                .registerRecipeDrawableRenderProvider(new NeiRecipeDrawableRenderProvider());

            GuideNhIntegrationRegistry.global()
                .registerRecipeHandlerRenderProvider(new NeiRecipeHandlerRenderProvider());
        }
    }

    public static void registerBlockStatsProviders() {
        if (Mods.AE2.isModLoaded()) {
            GuideNhIntegrationRegistry.global()
                .registerBlockStatsProvider(new Ae2BlockStatsProvider());
        }

        if (Mods.ForgeMultipart.isModLoaded()) {
            GuideNhIntegrationRegistry.global()
                .registerBlockStatsProvider(new ForgeMultipartBlockStatsProvider());
        }

        if (Mods.CarpentersBlocks.isModLoaded()) {
            GuideNhIntegrationRegistry.global()
                .registerBlockStatsProvider(new CarpentersBlocksBlockStatsProvider());
        }
    }

    public static void registerPreviewPrepareContributors() {
        if (Mods.GregTech.isModLoaded()) {
            GuideNhIntegrationRegistry.global()
                .registerPreviewPrepareContributor(new GregTechPreviewPrepareContributor());
        }

        if (Mods.BuildCraftTransport.isModLoaded()) {
            GuideNhIntegrationRegistry.global()
                .registerPreviewPrepareContributor(new BuildCraftPreviewPrepareContributor());
        }

        if (Mods.LogisticsPipes.isModLoaded()) {
            GuideNhIntegrationRegistry.global()
                .registerPreviewPrepareContributor(new LogisticsPipesPreviewPrepareContributor());
        }

        if (Mods.TinkersConstruct.isModLoaded()) {
            GuideNhIntegrationRegistry.global()
                .registerPreviewPrepareContributor(new TinkersConstructPreviewPrepareContributor());
        }

        if (Mods.Railcraft.isModLoaded()) {
            GuideNhIntegrationRegistry.global()
                .registerPreviewPrepareContributor(new RailcraftPreviewPrepareContributor());
        }

        if (Mods.AE2.isModLoaded()) {
            GuideNhIntegrationRegistry.global()
                .registerPreviewPrepareContributor(new Ae2PreviewPrepareContributor());
        }
    }

    public static void registerStructureLibControllerIntegrations() {
        if (Mods.StructureLib.isModLoaded() && Mods.GregTech.isModLoaded()) {
            GregTechStructureLibControllerIntegration gregTechIntegration = new GregTechStructureLibControllerIntegration();

            StructureLibControllerIntegrationRegistry.global()
                .registerDiscoveryIntegration(gregTechIntegration);

            StructureLibControllerIntegrationRegistry.global()
                .registerPlacementIntegration(gregTechIntegration);

            StructureLibControllerIntegrationRegistry.global()
                .registerPreviewItemProvider(gregTechIntegration);

            StructureLibControllerIntegrationRegistry.global()
                .registerPreviewStateSynchronizer(gregTechIntegration);
        }
    }
}
