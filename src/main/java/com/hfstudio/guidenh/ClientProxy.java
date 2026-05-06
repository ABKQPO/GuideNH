package com.hfstudio.guidenh;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.IReloadableResourceManager;
import net.minecraftforge.client.ClientCommandHandler;
import net.minecraftforge.common.MinecraftForge;

import com.hfstudio.guidenh.client.RegionWandRenderer;
import com.hfstudio.guidenh.client.command.GuideNhClientBridgeController;
import com.hfstudio.guidenh.client.command.GuideNhClientCommand;
import com.hfstudio.guidenh.client.hotkey.OpenGuideHotkey;
import com.hfstudio.guidenh.client.hotkey.OpenSceneEditorHotkey;
import com.hfstudio.guidenh.guide.internal.GuideDevWatcherPump;
import com.hfstudio.guidenh.guide.internal.GuideME;
import com.hfstudio.guidenh.guide.internal.GuideOnStartup;
import com.hfstudio.guidenh.guide.internal.GuideReloadListener;
import com.hfstudio.guidenh.guide.internal.GuideWarmupPump;
import com.hfstudio.guidenh.network.GuideNhAe2CableBatchClientHandler;
import com.hfstudio.guidenh.network.GuideNhAe2CableBatchReplyMessage;
import com.hfstudio.guidenh.network.GuideNhClientBridgeHandler;
import com.hfstudio.guidenh.network.GuideNhClientBridgeMessage;
import com.hfstudio.guidenh.network.GuideNhNetwork;

import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLLoadCompleteEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.relauncher.Side;

public class ClientProxy extends CommonProxy {

    @Override
    public void preInit(FMLPreInitializationEvent event) {
        super.preInit(event);
        GuideME.initClientProxy();
        GuideNhNetwork.channel()
            .registerMessage(GuideNhClientBridgeHandler.class, GuideNhClientBridgeMessage.class, 2, Side.CLIENT);
        GuideNhNetwork.channel()
            .registerMessage(GuideNhAe2CableBatchClientHandler.class, GuideNhAe2CableBatchReplyMessage.class, 4, Side.CLIENT);
    }

    @Override
    public void init(FMLInitializationEvent event) {
        super.init(event);
        ((IReloadableResourceManager) Minecraft.getMinecraft()
            .getResourceManager()).registerReloadListener(new GuideReloadListener());
        ClientCommandHandler.instance.registerCommand(new GuideNhClientCommand());
        GuideNhClientBridgeController.init();
        OpenGuideHotkey.init();
        OpenSceneEditorHotkey.init();
        MinecraftForge.EVENT_BUS.register(new RegionWandRenderer());
        GuideWarmupPump.init();
    }

    @Override
    public void postInit(FMLPostInitializationEvent event) {
        super.postInit(event);
    }

    @Override
    public void completeInit(FMLLoadCompleteEvent event) {
        super.completeInit(event);
        GuideDevWatcherPump.init();
        GuideOnStartup.init();
    }
}
