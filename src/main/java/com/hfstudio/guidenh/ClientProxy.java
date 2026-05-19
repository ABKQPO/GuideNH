package com.hfstudio.guidenh;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.IReloadableResourceManager;
import net.minecraftforge.client.ClientCommandHandler;
import net.minecraftforge.common.MinecraftForge;

import com.hfstudio.guidenh.bridge.GuideNhRuntimeBridge;
import com.hfstudio.guidenh.bridge.GuideNhRuntimeBridgeSettings;
import com.hfstudio.guidenh.client.RegionWandRenderer;
import com.hfstudio.guidenh.client.command.GuideNhClientBridgeController;
import com.hfstudio.guidenh.client.command.GuideNhClientCommand;
import com.hfstudio.guidenh.client.hotkey.CycleRegionWandModeHotkey;
import com.hfstudio.guidenh.client.hotkey.OpenGuideHomeHotkey;
import com.hfstudio.guidenh.client.hotkey.OpenGuideHotkey;
import com.hfstudio.guidenh.client.hotkey.OpenSceneEditorHotkey;
import com.hfstudio.guidenh.config.ModConfig;
import com.hfstudio.guidenh.guide.internal.GuideDevWatcherPump;
import com.hfstudio.guidenh.guide.internal.GuideDevelopmentResourcePackWatcher;
import com.hfstudio.guidenh.guide.internal.GuideME;
import com.hfstudio.guidenh.guide.internal.GuideOnStartup;
import com.hfstudio.guidenh.guide.internal.GuideRegistry;
import com.hfstudio.guidenh.guide.internal.GuideReloadListener;
import com.hfstudio.guidenh.guide.internal.GuideScreenMemory;
import com.hfstudio.guidenh.guide.internal.GuideWarmupPump;
import com.hfstudio.guidenh.guide.internal.home.GuideScreenHomeHistory;
import com.hfstudio.guidenh.guide.scene.level.GuidebookFakeWorld;
import com.hfstudio.guidenh.guide.scene.level.GuidebookLevel;
import com.hfstudio.guidenh.integration.GuideNhClientIntegrationBootstrap;
import com.hfstudio.guidenh.integration.Mods;
import com.hfstudio.guidenh.integration.ae2.network.Ae2NetworkRegistration;
import com.hfstudio.guidenh.integration.nei.GuideScreenNeiBridge;
import com.hfstudio.guidenh.network.GuideNhClientBridgeHandler;
import com.hfstudio.guidenh.network.GuideNhClientBridgeMessage;
import com.hfstudio.guidenh.network.GuideNhNetwork;
import com.hfstudio.guidenh.network.GuideNhRegionExportClientHandler;
import com.hfstudio.guidenh.network.GuideNhRegionExportReplyMessage;
import com.hfstudio.structurelibexport.StructureExportBootstrap;

import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLLoadCompleteEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.network.FMLNetworkEvent;
import cpw.mods.fml.relauncher.Side;

public class ClientProxy extends CommonProxy {

    private final GuideNhRuntimeBridge runtimeBridge = new GuideNhRuntimeBridge();

    @Override
    public void preInit(FMLPreInitializationEvent event) {
        super.preInit(event);
        GuidebookLevel.setPreviewWorldFactory(GuidebookFakeWorld::new);
        GuideNhClientIntegrationBootstrap.preInitClient();
        GuideME.initClientProxy();
        GuideNhNetwork.channel()
            .registerMessage(GuideNhClientBridgeHandler.class, GuideNhClientBridgeMessage.class, 2, Side.CLIENT);
        GuideNhNetwork.channel()
            .registerMessage(
                GuideNhRegionExportClientHandler.class,
                GuideNhRegionExportReplyMessage.class,
                8,
                Side.CLIENT);
        Ae2NetworkRegistration.registerClientMessages();
    }

    @Override
    public void init(FMLInitializationEvent event) {
        super.init(event);
        ((IReloadableResourceManager) Minecraft.getMinecraft()
            .getResourceManager()).registerReloadListener(new GuideReloadListener());
        ClientCommandHandler.instance.registerCommand(new GuideNhClientCommand());
        StructureExportBootstrap.registerClientCommands();
        GuideNhClientBridgeController.init();
        if (Mods.NotEnoughItems.isModLoaded()) {
            GuideScreenNeiBridge.init();
        }
        OpenGuideHomeHotkey.init();
        OpenGuideHotkey.init();
        OpenSceneEditorHotkey.init();
        CycleRegionWandModeHotkey.init();
        MinecraftForge.EVENT_BUS.register(new RegionWandRenderer());
        GuideWarmupPump.init();
        MinecraftForge.EVENT_BUS.register(this);
        GuideNH.LOG.info(
            "GuideNH runtime bridge configuration loaded. enabled={}, hostConfigured={}, port={}, tokenConfigured={}",
            ModConfig.runtimeBridge.enabled,
            ModConfig.runtimeBridge.host != null && !ModConfig.runtimeBridge.host.trim()
                .isEmpty(),
            ModConfig.runtimeBridge.port,
            ModConfig.runtimeBridge.token != null && !ModConfig.runtimeBridge.token.isEmpty());
        runtimeBridge.start(
            new GuideNhRuntimeBridgeSettings(
                ModConfig.runtimeBridge.enabled,
                ModConfig.runtimeBridge.host,
                ModConfig.runtimeBridge.port,
                ModConfig.runtimeBridge.token,
                ModConfig.runtimeBridge.maxMessageBytes,
                ModConfig.runtimeBridge.maxPageSize,
                ModConfig.runtimeBridge.maxSubscriptions,
                ModConfig.runtimeBridge.maxConnections,
                ModConfig.runtimeBridge.maxDeltaEntries));
    }

    @Override
    public void postInit(FMLPostInitializationEvent event) {
        super.postInit(event);
    }

    @Override
    public void completeInit(FMLLoadCompleteEvent event) {
        super.completeInit(event);
        GuideDevelopmentResourcePackWatcher.init();
        GuideDevWatcherPump.init();
        GuideOnStartup.init();
    }

    @SubscribeEvent
    public void onClientDisconnect(FMLNetworkEvent.ClientDisconnectionFromServerEvent event) {
        GuideNH.LOG.info("Minecraft client disconnected. Stopping GuideNH runtime bridge session state");
        runtimeBridge.stop();
        GuideME.closeSearch();
        GuideScreenMemory.clear();
        GuideScreenHomeHistory.shared()
            .clear();
        for (var guide : GuideRegistry.getAll()) {
            guide.resetWarmup();
        }
    }
}
