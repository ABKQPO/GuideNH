package com.hfstudio.guidenh.integration;

import com.hfstudio.guidenh.integration.api.client.GuideNhClientIntegrationRegistry;
import com.hfstudio.guidenh.integration.betterquesting.BetterQuestingQuestHoverProvider;
import com.hfstudio.guidenh.integration.etfuturum.EtFuturumPreviewPlayerElytraProvider;
import com.hfstudio.guidenh.integration.etfuturum.EtFuturumSlimArmProvider;
import com.hfstudio.guidenh.integration.forgemultipart.ForgeMultipartPreviewBlockRenderProvider;
import com.hfstudio.guidenh.integration.simpleskinbackport.SimpleSkinBackportPreviewPlayerModelProvider;
import com.hfstudio.guidenh.integration.simpleskinbackport.SimpleSkinBackportSlimArmProvider;

public final class GuideNhClientIntegrationBootstrap {

    private GuideNhClientIntegrationBootstrap() {}

    public static void preInitClient() {
        GuideNhClientIntegrationRegistry.global()
            .registerPreviewPlayerSlimArmProvider(new SimpleSkinBackportSlimArmProvider());
        GuideNhClientIntegrationRegistry.global()
            .registerPreviewPlayerSlimArmProvider(new EtFuturumSlimArmProvider());
        GuideNhClientIntegrationRegistry.global()
            .registerPreviewPlayerModelProvider(new SimpleSkinBackportPreviewPlayerModelProvider());
        GuideNhClientIntegrationRegistry.global()
            .registerPreviewPlayerElytraProvider(new EtFuturumPreviewPlayerElytraProvider());
        GuideNhClientIntegrationRegistry.global()
            .registerPreviewBlockRenderProvider(new ForgeMultipartPreviewBlockRenderProvider());
        GuideNhClientIntegrationRegistry.global()
            .registerQuestHoverProvider(new BetterQuestingQuestHoverProvider());
    }
}
