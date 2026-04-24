package com.hfstudio.guidenh.config;

import net.minecraft.client.gui.GuiScreen;

import com.gtnewhorizon.gtnhlib.config.ConfigException;
import com.gtnewhorizon.gtnhlib.config.SimpleGuiConfig;
import com.hfstudio.guidenh.GuideNH;

public class GuideNHGuiConfig extends SimpleGuiConfig {

    public GuideNHGuiConfig(GuiScreen parentScreen) throws ConfigException {
        super(parentScreen, GuideNH.MODID, GuideNH.MODNAME, true, ModConfig.class);
    }
}
