package com.hfstudio.guidenh.config;

import com.gtnewhorizon.gtnhlib.config.Config;
import com.gtnewhorizon.gtnhlib.config.Config.Comment;
import com.gtnewhorizon.gtnhlib.config.Config.DefaultBoolean;
import com.gtnewhorizon.gtnhlib.config.Config.DefaultFloat;
import com.gtnewhorizon.gtnhlib.config.Config.RangeFloat;
import com.gtnewhorizon.gtnhlib.config.Config.RequiresMcRestart;
import com.gtnewhorizon.gtnhlib.config.Config.Sync;
import com.gtnewhorizon.gtnhlib.config.ConfigException;
import com.gtnewhorizon.gtnhlib.config.ConfigurationManager;
import com.hfstudio.guidenh.GuideNH;
import com.hfstudio.guidenh.guide.internal.item.RegionWandExportMode;

@Config(modid = GuideNH.MODID, filename = "guidenh", configSubDirectory = "guidenh")
@Config.LangKeyPattern(pattern = "guideme.gui.config.%cat.%field", fullyQualified = true)
@Comment("GuideNH configuration")
public class ModConfig {

    public static void registerConfig() throws ConfigException {
        ConfigurationManager.registerConfig(ModConfig.class);
    }

    public static final Debug debug = new Debug();
    public static final Ui ui = new Ui();
    public static final RuntimeBridge runtimeBridge = new RuntimeBridge();

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

        @Comment("Side margin ratio applied to each side of guide content while using the optional narrow reading mode "
            + "in full-width layout. Set to 0 to disable. A value of 0.15 leaves about 15% margin on both sides.")
        @DefaultFloat(0.0f)
        @RangeFloat(min = 0.0f, max = 0.45f)
        public float fullWidthNarrowReadingMarginRatio = 0.0f;

        @Comment("Whether the guide navigation sidebar is pinned open by default.")
        @DefaultBoolean(false)
        public boolean guideNavigationPinned = false;

        @Comment("Global content zoom factor for the guide screen. "
            + "Individual pages can override this with the 'zoom' frontmatter field. "
            + "Default: 1.0. Range: 0.5 to 3.0.")
        @DefaultFloat(1.0f)
        @RangeFloat(min = 0.5f, max = 3.0f)
        public float contentZoom = 1.0f;

        @Comment("Whether clicking external links in guide markdown should show a confirmation dialog first. "
            + "When false, external links open immediately. Default: true.")
        @DefaultBoolean(true)
        public boolean confirmExternalLinks = true;

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

        @Comment("Whether scene export features are available. "
            + "This controls the scene editor, structure export commands, and Region Wand selection/export. "
            + "On multiplayer servers this value is synced from the server. "
            + "If the server does not have GuideNH installed, scene export is disabled.")
        @DefaultBoolean(true)
        @Sync
        public boolean sceneExportEnabled = true;

        @Comment("Whether the Region Wand selection box remains visible after switching away from the wand.")
        @DefaultBoolean(true)
        public boolean regionWandPersistentSelectionRender = true;

        @Comment("Client-global Region Wand export mode used for selection exports.")
        public RegionWandExportMode regionWandExportMode = RegionWandExportMode.SNBT;

        @Comment("Maximum undo history entries kept by the scene editor.")
        public int sceneEditorUndoHistoryLimit = 15;

        @Comment("Default screenshot format used by the scene editor screenshot button.")
        public String sceneEditorScreenshotFormat = "png";

        @Comment("Default screenshot scale used by the scene editor screenshot button.")
        public int sceneEditorScreenshotScale = 8;

        @Comment("Expanded width of the markdown panel in the scene editor.")
        public int sceneEditorMarkdownPanelWidth = 208;

        @Comment("Whether the markdown panel wraps lines in the scene editor.")
        @DefaultBoolean(true)
        public boolean sceneEditorMarkdownWrapEnabled = true;

        @Comment("Maximum item columns per row when showing StructureLib block candidates in tooltips.")
        public int sceneStructureLibCandidateColumns = 10;

        @Comment("Maximum number of StructureLib block candidates shown per candidate grid in tooltips. "
            + "Candidates beyond this limit are hidden and an ellipsis '...' is appended instead. "
            + "Set to 0 to disable the limit. Default: 40.")
        public int sceneStructureLibCandidateMaxCount = 40;

        @Comment("Whether 3D scene previews show the layer slider by default. "
            + "This can still be enabled per-scene from markdown. Default: true.")
        @DefaultBoolean(true)
        public boolean sceneLayerSliderEnabled = true;

        @Comment("Whether 3D scene previews show the block statistics overlay by default.")
        @DefaultBoolean(false)
        public boolean sceneBlockStatsVisible = false;

        @Comment("Whether 3D scene previews show the block statistics toggle button by default.")
        @DefaultBoolean(true)
        public boolean sceneBlockStatsButtonEnabled = true;

        @Comment("Whether the guide screen editor mode starts enabled.")
        @DefaultBoolean(false)
        public boolean guideEditorEnabled = false;

        @Comment("Whether advanced guide editor toolbar buttons start visible.")
        @DefaultBoolean(false)
        public boolean guideEditorAdvancedToolbarVisible = false;

        @Comment("Whether GuideScreen editor shows the NEI item panel outside the guide window when not full-width.")
        @DefaultBoolean(false)
        public boolean guideEditorNeiItemPanelOutsideWindow = false;

        @Comment("Manual GuideScreen window width in non-full-width layout, as a percentage of screen width. "
            + "Set to 0 to use the automatic size. Valid values are clamped to 0 through 100.")
        public int nonFullWidthWindowWidthPercent = 0;

        @Comment("Manual GuideScreen window height in non-full-width layout, as a percentage of screen height. "
            + "Set to 0 to use the automatic size. Valid values are clamped to 0 through 100.")
        public int nonFullWidthWindowHeightPercent = 0;

        @Comment("Whether the guide editor saves shortly after edits and when closing the screen.")
        @DefaultBoolean(true)
        public boolean guideEditorAutosaveEnabled = true;

        @Comment("Guide editor layout mode: 0 split, 1 editor only, 2 preview only.")
        public int guideEditorLayoutMode = 0;

        @Comment("Guide editor split divider position as a percentage from the left.")
        public int guideEditorDividerPercent = 50;

        @Comment("Debounce delay used before guide editor autosave writes to disk, in milliseconds.")
        public int guideEditorAutosaveDelayMillis = 500;

        @Comment("Default author name inserted into new guide pages.")
        public String guideEditorDefaultAuthor = "GuideNH";

        @Comment("Last path used by the guide editor new page dialog.")
        public String guideEditorNewPagePath = "NewGuide.md";

        @Comment("How long page-wheel scrolling temporarily blocks 3D preview wheel interactions. "
            + "Value is in milliseconds. Default: 750.")
        public int sceneWheelInteractionDelayMillis = 750;

        @Comment("Maximum recommended guide pages shown on the home page.")
        public int homeRecommendedPageLimit = 30;

        @Comment("Maximum bookmarked guide pages shown on the home page.")
        public int homeBookmarkedPageLimit = 10;

        @Comment("Maximum recent history guide pages shown on the home page.")
        public int homeHistoryPageLimit = 10;

        @Comment("Client-global bookmarked guide page ids, serialized as a pipe-delimited list.")
        public String guideBookmarks = "";
    }

    public static int clampPositiveHomeLimit(int value, int fallback) {
        return value >= 1 ? value : fallback;
    }

    @Comment("Runtime bridge section. The bridge is disabled by default and requires a client restart.")
    public static class RuntimeBridge {

        @Comment("Whether the GuideNH runtime bridge WebSocket server is enabled. Default: false.")
        @DefaultBoolean(false)
        @RequiresMcRestart
        public boolean enabled = false;

        @Comment("Runtime bridge host. No default is provided; set this explicitly when enabling the bridge.")
        @RequiresMcRestart
        public String host = "";

        @Comment("Runtime bridge port. No default is provided; set this explicitly when enabling the bridge.")
        @RequiresMcRestart
        public int port = 0;

        @Comment("Runtime bridge authentication token. Empty values prevent the bridge from starting.")
        @RequiresMcRestart
        public String token = "";

        @Comment("Maximum accepted WebSocket message size in bytes.")
        @RequiresMcRestart
        public int maxMessageBytes = 262144;

        @Comment("Maximum semantic query page size.")
        @RequiresMcRestart
        public int maxPageSize = 200;

        @Comment("Maximum subscriptions per connection.")
        @RequiresMcRestart
        public int maxSubscriptions = 16;

        @Comment("Maximum concurrent runtime bridge connections.")
        @RequiresMcRestart
        public int maxConnections = 2;

        @Comment("Maximum semantic delta entries sent in one message.")
        @RequiresMcRestart
        public int maxDeltaEntries = 200;
    }

    public static void save() {
        try {
            ConfigurationManager.save(ModConfig.class);
        } catch (Throwable ignored) {}
    }
}
