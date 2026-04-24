package com.hfstudio.guidenh.config;

import com.gtnewhorizon.gtnhlib.config.Config;
import com.gtnewhorizon.gtnhlib.config.Config.Comment;
import com.gtnewhorizon.gtnhlib.config.Config.DefaultBoolean;
import com.gtnewhorizon.gtnhlib.config.Config.RequiresMcRestart;
import com.gtnewhorizon.gtnhlib.config.ConfigException;
import com.gtnewhorizon.gtnhlib.config.ConfigurationManager;
import com.hfstudio.guidenh.GuideNH;

@Config(modid = GuideNH.MODID, filename = "guidenh", configSubDirectory = "guidenh")
@Config.LangKeyPattern(pattern = "guidenh.gui.config.%cat.%field", fullyQualified = true)
@Comment("GuideNH configuration")
public class ModConfig {

    public static void registerConfig() throws ConfigException {
        ConfigurationManager.registerConfig(ModConfig.class);
    }

    public static final Debug debug = new Debug();
    public static final Ui ui = new Ui();

    @Comment("Debug section")
    public static class Debug {

        @Comment("Enable Debug Print Log")
        @DefaultBoolean(false)
        @RequiresMcRestart
        public boolean enableDebugMode = false;
    }

    @Comment("UI section (persisted across sessions)")
    public static class Ui {

        @Comment("Whether guide book opens in full-width layout (no side margins)")
        @DefaultBoolean(false)
        public boolean fullWidth = false;

        @Comment("Whether mouse wheel scroll zooms the 3D scene preview (while cursor is over it). "
            + "When false, scroll always goes to page scroll. Default: true.")
        @DefaultBoolean(true)
        public boolean sceneWheelZoom = true;
    }

    public static void save() {
        try {
            ConfigurationManager.save(ModConfig.class);
        } catch (Throwable t) {}
    }
}
