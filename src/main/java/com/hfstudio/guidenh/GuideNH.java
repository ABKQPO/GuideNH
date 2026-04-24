package com.hfstudio.guidenh;

import static com.hfstudio.guidenh.GuideNH.MODID;
import static com.hfstudio.guidenh.GuideNH.MODNAME;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.hfstudio.guidenh.guide.internal.GuideCommand;
import com.hfstudio.guidenh.guide.internal.item.GuideItem;
import com.hfstudio.guidenh.guide.internal.item.RegionWandItem;

import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLLoadCompleteEvent;
import cpw.mods.fml.common.event.FMLMissingMappingsEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;

@Mod(
    modid = MODID,
    version = Tags.VERSION,
    name = MODNAME,
    guiFactory = "com.hfstudio.guidenh.config.GuideNHGuiFactory",
    acceptableRemoteVersions = "*",
    acceptedMinecraftVersions = "[1.7.10]")
public class GuideNH {

    @Mod.Instance(Tags.MODID)
    public static GuideNH instance;
    public static final String MODID = Tags.MODID;
    public static final String MODNAME = Tags.MODNAME;
    public static final String VERSION = Tags.VERSION;
    public static final String AUTHOR = "HFstudio";
    public static final Logger LOG = LogManager.getLogger(MODID);

    public static boolean debug = false;

    @SidedProxy(clientSide = "com.hfstudio.guidenh.ClientProxy", serverSide = "com.hfstudio.guidenh.CommonProxy")
    public static CommonProxy proxy;

    public static final GuideItem GUIDE_ITEM = new GuideItem();

    public static final RegionWandItem REGION_WAND = new RegionWandItem();

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        proxy.preInit(event);
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        proxy.init(event);
    }

    @Mod.EventHandler
    public void postInit(FMLPostInitializationEvent event) {
        proxy.postInit(event);
    }

    @Mod.EventHandler
    public void completeInit(FMLLoadCompleteEvent event) {
        proxy.completeInit(event);
    }

    @Mod.EventHandler
    public void serverStarting(FMLServerStartingEvent event) {
        event.registerServerCommand(new GuideCommand());
    }

    @Mod.EventHandler
    public void onMissingMappings(FMLMissingMappingsEvent event) {}
}
