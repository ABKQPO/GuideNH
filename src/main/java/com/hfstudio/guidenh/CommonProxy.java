package com.hfstudio.guidenh;

import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLLoadCompleteEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.registry.GameRegistry;

public class CommonProxy {

    public void preInit(FMLPreInitializationEvent event) {
        GameRegistry.registerItem(GuideNH.GUIDE_ITEM, "guide");
        GameRegistry.registerItem(GuideNH.REGION_WAND, "region_wand");
    }

    public void init(FMLInitializationEvent event) {}

    public void postInit(FMLPostInitializationEvent event) {}

    public void completeInit(FMLLoadCompleteEvent event) {}
}
