package com.hfstudio.guidenh.config;

import com.gtnewhorizon.gtnhlib.config.Config;
import com.gtnewhorizon.gtnhlib.config.Config.Comment;
import com.gtnewhorizon.gtnhlib.config.Config.DefaultBoolean;
import com.gtnewhorizon.gtnhlib.config.Config.RequiresMcRestart;
import com.gtnewhorizon.gtnhlib.config.ConfigException;
import com.gtnewhorizon.gtnhlib.config.ConfigurationManager;
import com.hfstudio.guidenh.GuideNH;

@Config(modid = GuideNH.MODID, filename = "guidenh", configSubDirectory = "guidenh")
@Config.LangKeyPattern(pattern = "guideme.gui.config.%cat.%field", fullyQualified = true)
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

        @Comment("Whether 3D scene preview swaps mouse drag buttons. "
            + "When true, left drag rotates and right drag pans. "
            + "When false, left drag pans and right drag rotates. Default: true.")
        @DefaultBoolean(true)
        public boolean sceneSwapMouseButtons = true;

        @Comment("Whether scene editor snapping is enabled by default. "
            + "This preference is persisted immediately after toggling in the editor.")
        @DefaultBoolean(false)
        public boolean sceneEditorSnapEnabled = false;

        @Comment("Whether scene editor auto-pick is enabled by default. "
            + "This preference is persisted immediately after toggling in the editor.")
        @DefaultBoolean(false)
        public boolean sceneEditorAutoPickEnabled = false;

        @Comment("Whether point snapping is enabled in the scene editor by default.")
        @DefaultBoolean(true)
        public boolean sceneEditorSnapPointEnabled = true;

        @Comment("Whether line snapping is enabled in the scene editor by default.")
        @DefaultBoolean(false)
        public boolean sceneEditorSnapLineEnabled = false;

        @Comment("Whether face snapping is enabled in the scene editor by default.")
        @DefaultBoolean(false)
        public boolean sceneEditorSnapFaceEnabled = false;

        @Comment("Whether block-center snapping is enabled in the scene editor by default.")
        @DefaultBoolean(false)
        public boolean sceneEditorSnapCenterEnabled = false;

        @Comment("Maximum undo history entries kept by the scene editor.")
        public int sceneEditorUndoHistoryLimit = 15;

        @Comment("Expanded width of the markdown panel in the scene editor.")
        public int sceneEditorMarkdownPanelWidth = 208;

        @Comment("Whether the markdown panel wraps lines in the scene editor.")
        @DefaultBoolean(true)
        public boolean sceneEditorMarkdownWrapEnabled = true;

        @Comment("Maximum item columns per row when showing StructureLib block candidates in tooltips.")
        public int sceneStructureLibCandidateColumns = 6;

        @Comment("Whether 3D scene previews show the layer slider by default. "
            + "This can still be enabled per-scene from markdown. Default: true.")
        @DefaultBoolean(true)
        public boolean sceneLayerSliderEnabled = true;

        @Comment("How long page-wheel scrolling temporarily blocks 3D preview wheel interactions. "
            + "Value is in milliseconds. Default: 750.")
        public int sceneWheelInteractionDelayMillis = 750;
    }

    public static void save() {
        try {
            ConfigurationManager.save(ModConfig.class);
        } catch (Throwable t) {}
    }
}
