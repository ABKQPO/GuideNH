package com.hfstudio.guidenh.guide.internal;

import java.awt.Desktop;
import java.net.URI;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiConfirmOpenLink;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.gui.GuiYesNoCallback;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;

import org.jetbrains.annotations.Nullable;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

import com.hfstudio.guidenh.client.command.GuideNhClientBridgeController;
import com.hfstudio.guidenh.client.hotkey.OpenGuideHotkey;
import com.hfstudio.guidenh.config.ModConfig;
import com.hfstudio.guidenh.guide.GuidePage;
import com.hfstudio.guidenh.guide.GuidePageIcon;
import com.hfstudio.guidenh.guide.PageAnchor;
import com.hfstudio.guidenh.guide.color.LightDarkMode;
import com.hfstudio.guidenh.guide.color.SymbolicColor;
import com.hfstudio.guidenh.guide.compiler.AnchorIndexer;
import com.hfstudio.guidenh.guide.compiler.FrontmatterPageMeta;
import com.hfstudio.guidenh.guide.compiler.PageCompiler;
import com.hfstudio.guidenh.guide.compiler.ParsedGuidePage;
import com.hfstudio.guidenh.guide.document.DefaultStyles;
import com.hfstudio.guidenh.guide.document.LytRect;
import com.hfstudio.guidenh.guide.document.block.LytDocument;
import com.hfstudio.guidenh.guide.document.block.LytHeading;
import com.hfstudio.guidenh.guide.document.block.LytNode;
import com.hfstudio.guidenh.guide.document.block.LytParagraph;
import com.hfstudio.guidenh.guide.document.block.LytSlot;
import com.hfstudio.guidenh.guide.document.block.LytVisitor;
import com.hfstudio.guidenh.guide.document.flow.LytFlowAnchor;
import com.hfstudio.guidenh.guide.document.flow.LytFlowContent;
import com.hfstudio.guidenh.guide.document.flow.LytFlowText;
import com.hfstudio.guidenh.guide.document.interaction.ContentTooltip;
import com.hfstudio.guidenh.guide.document.interaction.DocumentDragTarget;
import com.hfstudio.guidenh.guide.document.interaction.GuideTooltip;
import com.hfstudio.guidenh.guide.document.interaction.InteractiveElement;
import com.hfstudio.guidenh.guide.document.interaction.ItemTooltip;
import com.hfstudio.guidenh.guide.document.interaction.TextTooltip;
import com.hfstudio.guidenh.guide.indices.ItemMultiIndex;
import com.hfstudio.guidenh.guide.internal.editor.gui.SceneEditorMultilineTextArea;
import com.hfstudio.guidenh.guide.internal.editor.guide.GuideScreenEditorAction;
import com.hfstudio.guidenh.guide.internal.editor.guide.GuideScreenEditorConflictPrompt;
import com.hfstudio.guidenh.guide.internal.editor.guide.GuideScreenEditorContextMenu;
import com.hfstudio.guidenh.guide.internal.editor.guide.GuideScreenEditorFileStore;
import com.hfstudio.guidenh.guide.internal.editor.guide.GuideScreenEditorLayoutMode;
import com.hfstudio.guidenh.guide.internal.editor.guide.GuideScreenEditorMerge;
import com.hfstudio.guidenh.guide.internal.editor.guide.GuideScreenEditorState;
import com.hfstudio.guidenh.guide.internal.editor.guide.GuideScreenEditorTextActions;
import com.hfstudio.guidenh.guide.internal.editor.guide.GuideScreenEditorUndoHistory;
import com.hfstudio.guidenh.guide.internal.item.RegionWandItem;
import com.hfstudio.guidenh.guide.internal.markdown.CodeBlockClipboardService;
import com.hfstudio.guidenh.guide.internal.screen.GuideIconButton;
import com.hfstudio.guidenh.guide.internal.screen.GuideNavBar;
import com.hfstudio.guidenh.guide.internal.search.GuideItemLinksPage;
import com.hfstudio.guidenh.guide.internal.search.GuideSearchPage;
import com.hfstudio.guidenh.guide.internal.search.GuideSearchResultDocumentBuilder;
import com.hfstudio.guidenh.guide.internal.search.GuideSearchSnippetFormatter;
import com.hfstudio.guidenh.guide.internal.tooltip.GuideItemTooltipLines;
import com.hfstudio.guidenh.guide.internal.tooltip.GuideItemTooltipRenderSupport;
import com.hfstudio.guidenh.guide.internal.util.LangUtil;
import com.hfstudio.guidenh.guide.layout.LayoutContext;
import com.hfstudio.guidenh.guide.layout.MinecraftFontMetrics;
import com.hfstudio.guidenh.guide.navigation.NavigationNode;
import com.hfstudio.guidenh.guide.render.VanillaRenderContext;
import com.hfstudio.guidenh.guide.scene.LytGuidebookScene;
import com.hfstudio.guidenh.guide.scene.support.GuideBlockDisplayResolver;
import com.hfstudio.guidenh.guide.scene.support.GuideEntityDisplayResolver;
import com.hfstudio.guidenh.guide.ui.GuideUiHost;
import com.hfstudio.guidenh.libs.unist.UnistPoint;

import cpw.mods.fml.common.FMLLog;
import cpw.mods.fml.common.Loader;

public class GuideScreen extends GuiScreen implements GuideUiHost, GuiYesNoCallback {

    public static final int PANEL_MARGIN = 20;
    public static final int PANEL_PADDING = 8;

    public static final int BG_COLOR = 0xE0101010;
    public static final int BG_BORDER = 0xFF5A5A5A;

    public static final ResourceLocation BG_TEXTURE = new ResourceLocation(
        "guidenh",
        "textures/gui/sprites/background.png");

    public static float BACKGROUND_ALPHA = 0.7f;
    public static int BACKGROUND_DIM_COLOR = 0x34101018;

    private MutableGuide guide;
    private PageAnchor currentAnchor;
    @Nullable
    private GuidePage currentPage;
    @Nullable
    private LytDocument document;

    private final Deque<PageAnchor> history = new ArrayDeque<>();
    private final Deque<PageAnchor> forwardHistory = new ArrayDeque<>();

    private int scrollY;
    private boolean pendingAnchorScroll;
    private float currentZoom = 1.0f;
    private int lastLayoutWidth = -1;
    private long lastPageWheelScrollAtMillis;

    private int panelX, panelY, panelW, panelH;
    private int contentX, contentY, contentW, contentH;

    @Nullable
    private LytGuidebookScene activeScene;
    @Nullable
    private DocumentDragTarget activeDocumentDragTarget;

    private boolean draggingScrollbar = false;
    private int scrollbarGrabOffsetY = 0;
    private boolean draggingDocument = false;
    private int dragLastMouseY = 0;

    private GuideIconButton btnSearch, btnBack, btnForward, btnFullWidth, btnClose;
    private GuideIconButton btnGuideEditorToggle, btnGuideEditorNewPage, btnGuideEditorLayoutSplit,
        btnGuideEditorLayoutEditorOnly, btnGuideEditorLayoutPreviewOnly, btnGuideEditorAdvancedToggle;
    public static final int TOOLBAR_H = 16;
    public static final int TOOLBAR_GAP = 3;
    private static final int GUIDE_EDITOR_TOOLBAR_H = 16;
    private static final int GUIDE_EDITOR_MIN_SPLIT_PANE_W = 15;
    private static final int SCROLLBAR_W = SceneEditorMultilineTextArea.SCROLLBAR_SIZE;
    private static final int GUIDE_EDITOR_DIVIDER_HOVER_DELAY_MILLIS = 1000;
    private boolean fullWidth;

    private final GuideNavBar navBar = new GuideNavBar();
    private final MinecraftFontMetrics layoutFontMetrics = new MinecraftFontMetrics();
    private final CodeBlockClipboardService codeBlockClipboardService = new CodeBlockClipboardService();
    private final GuideScreenEditorFileStore guideEditorFileStore = GuideScreenEditorFileStore.createDefault();
    private final Map<Integer, GuideIconButton> guideEditorActionButtons = new LinkedHashMap<>();

    private final VanillaRenderContext reusableRenderCtx = new VanillaRenderContext(
        LightDarkMode.LIGHT_MODE,
        LytRect.empty(),
        0);
    private final VanillaRenderContext reusableContentTooltipCtx = new VanillaRenderContext(
        LightDarkMode.LIGHT_MODE,
        LytRect.empty(),
        0);

    // Reuse rect records on hot render paths when geometry has not changed.
    @Nullable
    private LytRect cachedViewportRect;
    @Nullable
    private LytRect cachedScissorRect;
    @Nullable
    private LytRect cachedContentTooltipViewport;
    @Nullable
    private DocumentInteractionState cachedInteractionState;
    @Nullable
    private DocumentInteractionState cachedGuideEditorPreviewInteractionState;
    @Nullable
    private LytGuidebookScene hoveredScene;

    @Nullable
    private GuiTextField searchField;
    @Nullable
    private LytDocument searchDocument;
    @Nullable
    private String cachedSearchQuery;

    // Tracks the item stack whose tooltip was rendered last frame, for the G-key disambiguation hotkey.
    @Nullable
    private ItemStack hoveredItemStack;
    // Tick counter for the in-guide G-key hold-to-open-item-links feature.
    private int itemLinksKeyTicksHeld = 0;
    // The item stack captured at the start of a G-key hold gesture; cleared when the key is released.
    @Nullable
    private ItemStack pendingItemLinksStack = null;
    private String currentPageTitle = "";
    private LytParagraph pageTitle;
    @Nullable
    private LytRect cachedTitleViewport;
    private int cachedTitleLayoutWidth = -1;
    @Nullable
    private LytDocument layoutDocument;
    @Nullable
    private String cachedBottomBarText;
    @Nullable
    private GuidePage cachedBottomBarPage;
    private int cachedBottomBarWidth;
    @Nullable
    private String cachedTooltipText;
    private int cachedTooltipWrapWidth;
    @Nullable
    private List<String> cachedTooltipLines;
    @Nullable
    private LytRect cachedPreviewViewport;
    @Nullable
    private LytRect cachedPreviewScissor;
    @Nullable
    private SceneEditorMultilineTextArea guideEditorTextArea;
    @Nullable
    private GuideScreenEditorContextMenu guideEditorContextMenu;
    @Nullable
    private ParsedGuidePage guideEditorDraftPage;
    @Nullable
    private GuidePage guideEditorPreviewPage;
    @Nullable
    private String guideEditorDraftSource;
    @Nullable
    private String guideEditorSavedSource;
    private boolean guideEditorDirty;
    private boolean guideEditorPreviewDirty = true;
    private long guideEditorNextSaveAtMillis;
    private long guideEditorNextPreviewCompileAtMillis;
    private long guideEditorNextExternalCheckAtMillis;
    private boolean guideEditorDraggingDivider;
    private int guideEditorDividerGrabOffset;
    private boolean guideEditorDraggingPreviewScrollbar;
    private int guideEditorPreviewScrollbarGrabOffset;
    private int guideEditorPreviewScrollY;
    private long guideEditorDividerHoverStartedAtMillis;
    private int guideEditorActionToolbarBottom;
    private int guideEditorEditorTop;
    private boolean guideEditorSuppressUndoRecording;
    private boolean guideEditorSuppressReloadFromEditorApply;
    private GuideScreenEditorLayoutMode guideEditorLayoutMode = GuideScreenEditorLayoutMode.SPLIT;
    private boolean guideEditorAdvancedToolbarVisible;
    private int guideEditorDividerPercent = GuideScreenEditorState.getDividerPercent();
    private final GuideScreenEditorUndoHistory guideEditorUndoHistory = new GuideScreenEditorUndoHistory(100);

    public static final int SEARCH_FIELD_H = 12;
    public static final int SEARCH_FIELD_GAP = 6;
    public static final int SEARCH_MAX_QUERY_LENGTH = 128;
    public static final int SEARCH_RESULT_ICON_AND_GAP = 22;
    public static final int SEARCH_RESULT_TITLE_GAP = 8;
    public static final int SEARCH_PATH_MAX_CHARS = 20;
    public static final String ASCII_ELLIPSIS = "...";
    public static final int SEARCH_TOOLBAR_FIELD_Y_OFFSET = 5;
    private static final int EXTERNAL_LINK_CONFIRM_ID = 1;

    @Nullable
    private URI pendingExternalUri;

    private final Set<LytGuidebookScene> registeredScenes = Collections.newSetFromMap(new IdentityHashMap<>());

    public static class SceneButtonHit {

        final LytGuidebookScene scene;
        final GuideIconButton.Role role;

        private SceneButtonHit(LytGuidebookScene scene, GuideIconButton.Role role) {
            this.scene = scene;
            this.role = role;
        }
    }

    public static class DocumentInteractionState {

        final LytDocument document;
        final int mouseX;
        final int mouseY;
        final int contentX;
        final int contentY;
        final int contentW;
        final int contentH;
        final int scrollY;
        final int tooltipX;
        final int tooltipY;
        final int tooltipW;
        final int tooltipH;
        final int docX;
        final int docY;
        @Nullable
        final LytDocument.HitTestResult hit;
        @Nullable
        final LytGuidebookScene scene;
        @Nullable
        final SceneButtonHit sceneButtonHit;

        private DocumentInteractionState(LytDocument document, int mouseX, int mouseY, int contentX, int contentY,
            int contentW, int contentH, int scrollY, int tooltipX, int tooltipY, int tooltipW, int tooltipH, int docX,
            int docY, @Nullable LytDocument.HitTestResult hit, @Nullable LytGuidebookScene scene,
            @Nullable SceneButtonHit sceneButtonHit) {
            this.document = document;
            this.mouseX = mouseX;
            this.mouseY = mouseY;
            this.contentX = contentX;
            this.contentY = contentY;
            this.contentW = contentW;
            this.contentH = contentH;
            this.scrollY = scrollY;
            this.tooltipX = tooltipX;
            this.tooltipY = tooltipY;
            this.tooltipW = tooltipW;
            this.tooltipH = tooltipH;
            this.docX = docX;
            this.docY = docY;
            this.hit = hit;
            this.scene = scene;
            this.sceneButtonHit = sceneButtonHit;
        }

        private boolean matches(LytDocument document, int mouseX, int mouseY, int contentX, int contentY, int contentW,
            int contentH, int scrollY) {
            return this.document == document && this.mouseX == mouseX
                && this.mouseY == mouseY
                && this.contentX == contentX
                && this.contentY == contentY
                && this.contentW == contentW
                && this.contentH == contentH
                && this.scrollY == scrollY;
        }
    }

    private GuideScreen(MutableGuide guide, PageAnchor anchor) {
        this.guide = guide;
        this.currentAnchor = anchor;
        this.pendingAnchorScroll = anchor != null && anchor.anchor() != null;
        pageTitle = new LytParagraph();
        pageTitle.setStyle(DefaultStyles.HEADING1);
        try {
            this.fullWidth = ModConfig.ui.fullWidth;
        } catch (Throwable ignored) {
            this.fullWidth = false;
        }
    }

    public static void open(ResourceLocation guideId, @Nullable PageAnchor anchor) {
        var guide = GuideRegistry.getById(guideId);
        if (guide == null) {
            FMLLog.warning("GuideScreen.open: no guide registered with id {}", guideId);
            return;
        }
        var initial = anchor != null ? anchor : PageAnchor.page(guide.getStartPage());
        var screen = new GuideScreen(guide, initial);
        Minecraft.getMinecraft()
            .displayGuiScreen(screen);
    }

    @Nullable
    public static GuideScreen current() {
        var screen = Minecraft.getMinecraft().currentScreen;
        return screen instanceof GuideScreen gs ? gs : null;
    }

    public static boolean toggleEditorModeFromCommand() {
        boolean enabled = !GuideScreenEditorState.isEnabled();
        GuideScreenEditorState.setEnabled(enabled);
        GuideScreen screen = current();
        if (screen != null) {
            screen.syncGuideEditorStateFromConfig();
            screen.refreshGuideEditorDraft(true);
            screen.rebuildToolbar();
            screen.ensureLayout();
            screen.clampScroll();
        }
        return enabled;
    }

    public ResourceLocation getCurrentPageId() {
        return currentAnchor.pageId();
    }

    public boolean isShowingGuide(ResourceLocation guideId) {
        return guide.getId()
            .equals(guideId);
    }

    public void reloadPage() {
        if (guideEditorSuppressReloadFromEditorApply) {
            return;
        }
        var reloadedGuide = GuideRegistry.getById(guide.getId());
        if (reloadedGuide != null) {
            guide = reloadedGuide;
        }
        syncGuideEditorStateFromConfig();
        clearInteractionState();
        currentPage = null;
        document = null;
        lastLayoutWidth = -1;
        loadCurrentPage();
        updateToolbarButtonState();
    }

    @Override
    public void initGui() {
        super.initGui();
        Keyboard.enableRepeatEvents(true);
        syncGuideEditorStateFromConfig();
        recomputePanelBounds();
        if (document == null) {
            loadCurrentPage();
        }
        rebuildToolbar();
        ensureGuideEditorTextArea();
        refreshGuideEditorDraft(true);
        ensureLayout();
        scrollToCurrentAnchor();
        clampScroll();
    }

    @Override
    public void updateScreen() {
        super.updateScreen();
        for (LytGuidebookScene scene : registeredScenes) {
            scene.ponderTick();
        }
        tickGuideEditorPreviewScenes();
        updateGuideEditorAutosave();
        int guideHotkey = OpenGuideHotkey.OPEN_GUIDE_KEY.getKeyCode();
        if (guideHotkey > 0 && OpenGuideHotkey.isKeyHeld() && hoveredItemStack != null) {
            pendingItemLinksStack = hoveredItemStack;
            if (itemLinksKeyTicksHeld < OpenGuideHotkey.TICKS_TO_OPEN
                && ++itemLinksKeyTicksHeld == OpenGuideHotkey.TICKS_TO_OPEN) {
                openItemLinksPage(pendingItemLinksStack);
                itemLinksKeyTicksHeld = 0;
                pendingItemLinksStack = null;
            }
        } else {
            if (itemLinksKeyTicksHeld > 0) {
                itemLinksKeyTicksHeld = Math.max(0, itemLinksKeyTicksHeld - 2);
            }
            pendingItemLinksStack = null;
        }
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }

    @Override
    public void confirmClicked(boolean result, int id) {
        if (id != EXTERNAL_LINK_CONFIRM_ID) {
            return;
        }

        URI uri = pendingExternalUri;
        pendingExternalUri = null;
        if (result && uri != null) {
            browseExternalUrl(uri);
        }
        mc.displayGuiScreen(this);
    }

    private void recomputePanelBounds() {
        int margin = fullWidth ? 0 : PANEL_MARGIN;
        panelX = margin;
        panelY = margin;
        panelW = Math.max(100, this.width - margin * 2);
        panelH = Math.max(100, this.height - margin * 2);
        int navClosed = GuideNavBar.WIDTH_CLOSED;
        contentX = panelX + PANEL_PADDING + navClosed;
        contentY = panelY + TOOLBAR_H + 2;
        contentW = Math.max(20, panelW - PANEL_PADDING * 2 - navClosed);
        contentH = Math.max(20, panelH - TOOLBAR_H - PANEL_PADDING - 2);
        if (hasBottomBar()) {
            contentH = Math.max(20, contentH - TOOLBAR_H);
        }
    }

    private boolean hasBottomBar() {
        return currentPage != null && !isSearchPage() && !isItemLinksPage() && !isGuideEditorActive();
    }

    private boolean isGuideEditorActive() {
        return GuideScreenEditorState.isEnabled() && currentPage != null && !isSearchPage() && !isItemLinksPage();
    }

    private void syncGuideEditorStateFromConfig() {
        guideEditorLayoutMode = GuideScreenEditorState.getLayoutMode();
        guideEditorAdvancedToolbarVisible = GuideScreenEditorState.isAdvancedToolbarVisible();
        guideEditorDividerPercent = GuideScreenEditorState.getDividerPercent();
    }

    private void setGuideEditorLayoutMode(GuideScreenEditorLayoutMode mode) {
        GuideScreenEditorState.setLayoutMode(mode);
        guideEditorLayoutMode = mode;
        if (mode == GuideScreenEditorLayoutMode.PREVIEW_ONLY && guideEditorTextArea != null) {
            guideEditorTextArea.setFocused(false);
        }
        guideEditorPreviewDirty = true;
    }

    private void toggleGuideEditorEnabled() {
        toggleEditorModeFromCommand();
    }

    private void toggleGuideEditorAdvancedButtons() {
        guideEditorAdvancedToolbarVisible = !guideEditorAdvancedToolbarVisible;
        GuideScreenEditorState.setAdvancedToolbarVisible(guideEditorAdvancedToolbarVisible);
        rebuildToolbar();
    }

    private void createGuideEditorPage() {
        if (!isGuideEditorActive()) {
            return;
        }
        ParsedGuidePage currentParsedPage = guide.getParsedPage(currentAnchor.pageId());
        if (currentParsedPage == null) {
            return;
        }

        String language = currentParsedPage.getLanguage();
        String titleText = resolveGuideEditorNewPageTitle();
        ResourceLocation parentId = resolveGuideEditorNewPageParent(currentParsedPage);
        ResourceLocation sourcePageId = resolveGuideEditorNewPageSource(currentParsedPage);
        ResourceLocation newPageId = resolveGuideEditorNewPageId(language, sourcePageId);
        String pageText = buildGuideEditorNewPageText(titleText, parentId);
        boolean canApplyImmediately = guideEditorFileStore.canSaveBesideSource(guide, sourcePageId, language);

        try {
            guideEditorFileStore.savePage(guide, newPageId, language, pageText, sourcePageId);
            if (!canApplyImmediately) {
                FMLLog.info(
                    "Created guide editor page {} in fallback resource pack {}. Enable that resource pack to load it.",
                    newPageId,
                    guideEditorFileStore.getPackRoot());
                return;
            }
            ParsedGuidePage parsedNewPage = PageCompiler
                .parse(currentParsedPage.getSourcePack(), language, newPageId, pageText);
            applyGuideEditorPageWithoutReload(parsedNewPage);
            guide.rebuildEditorNavigationState();
            navigateTo(PageAnchor.page(newPageId));
            refreshGuideEditorDraft(true);
            rebuildToolbar();
        } catch (Throwable t) {
            FMLLog.warning("Failed to create guide editor page {}", newPageId, t);
        }
    }

    private ResourceLocation resolveGuideEditorNewPageId(String language, @Nullable ResourceLocation sourcePageId) {
        String sourcePath = currentAnchor.pageId()
            .getResourcePath();
        int slash = sourcePath.lastIndexOf('/');
        String folder = slash >= 0 ? sourcePath.substring(0, slash) : "";
        String baseName = "new_guide";
        String candidate = folder.isEmpty() ? baseName + ".md" : folder + "/" + baseName + ".md";
        int index = 2;
        while (guideEditorFileStore
            .hasPage(guide, new ResourceLocation(guide.getDefaultNamespace(), candidate), language, sourcePageId)) {
            String suffix = "-" + index++;
            candidate = folder.isEmpty() ? baseName + suffix + ".md" : folder + "/" + baseName + suffix + ".md";
        }
        return new ResourceLocation(guide.getDefaultNamespace(), candidate);
    }

    private ResourceLocation resolveGuideEditorNewPageParent(ParsedGuidePage currentParsedPage) {
        if (currentParsedPage.getFrontmatter() != null && currentParsedPage.getFrontmatter()
            .navigationEntry() != null
            && currentParsedPage.getFrontmatter()
                .navigationEntry()
                .parent() != null) {
            return currentParsedPage.getFrontmatter()
                .navigationEntry()
                .parent();
        }
        return guide.getStartPage();
    }

    private ResourceLocation resolveGuideEditorNewPageSource(ParsedGuidePage currentParsedPage) {
        if (currentParsedPage.getFrontmatter() != null && currentParsedPage.getFrontmatter()
            .navigationEntry() != null
            && currentParsedPage.getFrontmatter()
                .navigationEntry()
                .parent() != null) {
            return currentParsedPage.getFrontmatter()
                .navigationEntry()
                .parent();
        }
        return currentParsedPage.getId();
    }

    private String resolveGuideEditorNewPageTitle() {
        String language = LangUtil.getCurrentLanguage();
        if (language != null && language.toLowerCase()
            .startsWith("zh")) {
            return "新指南";
        }
        return "New Guide";
    }

    private String buildGuideEditorNewPageText(String titleText, ResourceLocation parentId) {
        String dateText = LocalDateTime.now()
            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        StringBuilder builder = new StringBuilder();
        builder.append("---\n");
        builder.append("navigation:\n");
        builder.append("  title: ")
            .append(titleText)
            .append('\n');
        builder.append("  parent: ")
            .append(parentId.toString())
            .append('\n');
        builder.append("author: ")
            .append(GuideScreenEditorState.getDefaultAuthor())
            .append('\n');
        builder.append("date: ")
            .append(dateText)
            .append('\n');
        builder.append("---\n\n");
        return builder.toString();
    }

    private void ensureGuideEditorTextArea() {
        if (guideEditorTextArea == null) {
            guideEditorTextArea = new SceneEditorMultilineTextArea(fontRendererObj);
            guideEditorTextArea.setWrapEnabled(false);
        }
    }

    private void refreshGuideEditorDraft(boolean forceReload) {
        if (!isGuideEditorActive()) {
            return;
        }
        ParsedGuidePage parsedPage = guide.getParsedPage(currentAnchor.pageId());
        if (parsedPage == null) {
            guideEditorDraftSource = null;
            guideEditorDraftPage = null;
            guideEditorPreviewPage = null;
            return;
        }

        String text = forceReload ? null : guideEditorDraftSource;
        if (text == null) {
            text = guideEditorFileStore.readPageText(guide, currentAnchor.pageId(), parsedPage.getLanguage());
            if (text == null) {
                text = parsedPage.getSource();
            }
        }

        boolean preserveHistory = guideEditorTextArea != null && Objects.equals(guideEditorTextArea.getText(), text)
            && Objects.equals(guideEditorDraftSource, text);
        guideEditorDraftPage = parsedPage;
        guideEditorDraftSource = text;
        guideEditorSavedSource = text;
        guideEditorDirty = false;
        guideEditorPreviewDirty = true;
        guideEditorNextSaveAtMillis = 0L;
        guideEditorNextPreviewCompileAtMillis = 0L;
        guideEditorNextExternalCheckAtMillis = 0L;
        if (guideEditorTextArea != null && !preserveHistory) {
            guideEditorSuppressUndoRecording = true;
            try {
                guideEditorTextArea.applyEdit(text, 0, 0);
            } finally {
                guideEditorSuppressUndoRecording = false;
            }
            guideEditorTextArea.setFocused(true);
        }
        if (!preserveHistory) {
            guideEditorUndoHistory.reset(text, 0, 0);
        }
        cachedGuideEditorPreviewInteractionState = null;
        syncGuideEditorPreviewScrollFromEditor();
    }

    private void updateGuideEditorTextFromArea() {
        if (guideEditorTextArea == null || guideEditorSuppressUndoRecording) {
            return;
        }
        String text = guideEditorTextArea.getText();
        if (Objects.equals(text, guideEditorDraftSource)) {
            return;
        }
        guideEditorDraftSource = text;
        guideEditorDirty = !Objects.equals(text, guideEditorSavedSource);
        guideEditorPreviewDirty = true;
        guideEditorUndoHistory
            .push(text, guideEditorTextArea.getSelectionStart(), guideEditorTextArea.getSelectionEnd());
        long now = System.currentTimeMillis();
        guideEditorNextSaveAtMillis = now + GuideScreenEditorState.getAutosaveDelayMillis();
        guideEditorNextPreviewCompileAtMillis = now + 100L;
    }

    private void pushGuideEditorCurrentHistoryState() {
        if (guideEditorTextArea == null) {
            return;
        }
        pushGuideEditorHistoryState(
            guideEditorTextArea.getText(),
            guideEditorTextArea.getSelectionStart(),
            guideEditorTextArea.getSelectionEnd());
    }

    private void pushGuideEditorHistoryState(String text, int selectionStart, int selectionEnd) {
        guideEditorUndoHistory.push(text, selectionStart, selectionEnd);
    }

    private void markGuideEditorTextChanged() {
        if (guideEditorTextArea == null) {
            return;
        }
        guideEditorDraftSource = guideEditorTextArea.getText();
        guideEditorDirty = !Objects.equals(guideEditorDraftSource, guideEditorSavedSource);
        guideEditorPreviewDirty = true;
        long now = System.currentTimeMillis();
        guideEditorNextSaveAtMillis = now + GuideScreenEditorState.getAutosaveDelayMillis();
        guideEditorNextPreviewCompileAtMillis = now + 100L;
        syncGuideEditorPreviewScrollFromEditor();
    }

    private void runGuideEditorTextMutation(Runnable mutation) {
        if (guideEditorTextArea == null || mutation == null) {
            return;
        }
        String before = guideEditorTextArea.getText();
        int beforeSelectionStart = guideEditorTextArea.getSelectionStart();
        int beforeSelectionEnd = guideEditorTextArea.getSelectionEnd();
        mutation.run();
        if (Objects.equals(before, guideEditorTextArea.getText())) {
            return;
        }
        pushGuideEditorHistoryState(before, beforeSelectionStart, beforeSelectionEnd);
        markGuideEditorTextChanged();
        pushGuideEditorCurrentHistoryState();
    }

    private void updateGuideEditorAutosave() {
        if (!isGuideEditorActive()) {
            return;
        }
        pollGuideEditorExternalChanges();
        if (!guideEditorDirty) {
            return;
        }
        long now = System.currentTimeMillis();
        if (now >= guideEditorNextPreviewCompileAtMillis && guideEditorPreviewDirty) {
            rebuildGuideEditorPreview();
        }
        if (now < guideEditorNextSaveAtMillis) {
            return;
        }
        saveGuideEditorDraft();
    }

    private void rebuildGuideEditorPreview() {
        if (!isGuideEditorActive() || guideEditorDraftSource == null || guideEditorDraftPage == null) {
            return;
        }
        try {
            ParsedGuidePage parsedDraft = PageCompiler.parse(
                guideEditorDraftPage.getSourcePack(),
                guideEditorDraftPage.getLanguage(),
                currentAnchor.pageId(),
                guideEditorDraftSource);
            updateGuideEditorSyntaxWarning(parsedDraft);
            guideEditorPreviewPage = PageCompiler.compile(guide, guide.getExtensions(), parsedDraft);
            int previewWidth = getGuideEditorPreviewLayoutWidth();
            if (guideEditorPreviewPage != null && guideEditorPreviewPage.document() != null) {
                guideEditorPreviewPage.document()
                    .updateLayout(new LayoutContext(layoutFontMetrics), Math.max(1, previewWidth));
                guideEditorPreviewPage.prepareForDisplay();
            }
            guideEditorPreviewDirty = false;
            guideEditorDraftPage = parsedDraft;
            cachedGuideEditorPreviewInteractionState = null;
        } catch (Throwable t) {
            FMLLog.warning("Failed to compile guide editor preview for {}", currentAnchor.pageId(), t);
        }
    }

    private void updateGuideEditorSyntaxWarning(ParsedGuidePage parsedDraft) {
        if (guideEditorTextArea == null) {
            return;
        }
        if (!parsedDraft.hasParseFailure() || guideEditorDraftSource == null) {
            guideEditorTextArea.clearSyntaxWarning();
            return;
        }

        int startIndex = resolveGuideEditorOffset(guideEditorDraftSource, parsedDraft.getParseFailureFrom());
        int endIndex = resolveGuideEditorOffset(guideEditorDraftSource, parsedDraft.getParseFailureTo());
        if (startIndex < 0) {
            guideEditorTextArea.clearSyntaxWarning();
            return;
        }

        int safeStart = clampGuideEditorOffset(startIndex, guideEditorDraftSource.length());
        int safeEnd = clampGuideEditorOffset(endIndex, guideEditorDraftSource.length());
        if (safeEnd <= safeStart) {
            if (safeStart < guideEditorDraftSource.length()) {
                safeEnd = safeStart + 1;
            } else if (safeStart > 0) {
                safeStart--;
                safeEnd = safeStart + 1;
            } else {
                guideEditorTextArea.clearSyntaxWarning();
                return;
            }
        }

        guideEditorTextArea.setSyntaxWarning(safeStart, safeEnd);
    }

    private int resolveGuideEditorOffset(String text, @Nullable UnistPoint point) {
        if (text == null || text.isEmpty() || point == null) {
            return -1;
        }
        int targetLine = Math.max(1, point.line());
        int targetColumn = Math.max(1, point.column());
        int line = 1;
        int column = 1;
        for (int i = 0; i < text.length(); i++) {
            if (line == targetLine && column == targetColumn) {
                return i;
            }
            char current = text.charAt(i);
            if (current == '\r') {
                if (i + 1 < text.length() && text.charAt(i + 1) == '\n') {
                    i++;
                }
                line++;
                column = 1;
                continue;
            }
            if (current == '\n') {
                line++;
                column = 1;
                continue;
            }
            column++;
        }
        return text.length();
    }

    private int clampGuideEditorOffset(int index, int length) {
        if (index < 0) {
            return 0;
        }
        if (index > length) {
            return length;
        }
        return index;
    }

    private void saveGuideEditorDraft() {
        if (!isGuideEditorActive() || guideEditorDraftSource == null || guideEditorDraftPage == null) {
            return;
        }
        try {
            guideEditorFileStore.savePage(
                guide,
                currentAnchor.pageId(),
                guideEditorDraftPage.getLanguage(),
                guideEditorDraftSource,
                currentAnchor.pageId());
            ParsedGuidePage parsedDraft = PageCompiler.parse(
                guideEditorDraftPage.getSourcePack(),
                guideEditorDraftPage.getLanguage(),
                currentAnchor.pageId(),
                guideEditorDraftSource);
            applyGuideEditorPageWithoutReload(parsedDraft);
            guideEditorSavedSource = guideEditorDraftSource;
            guideEditorDirty = false;
            guideEditorPreviewDirty = true;
            guideEditorDraftPage = parsedDraft;
            guideEditorNextSaveAtMillis = 0L;
            guideEditorNextPreviewCompileAtMillis = 0L;
            guideEditorNextExternalCheckAtMillis = System.currentTimeMillis() + 250L;
            startBackgroundIndexRefresh();
        } catch (Throwable t) {
            FMLLog.warning("Failed to autosave guide editor page {}", currentAnchor.pageId(), t);
        }
    }

    private void pollGuideEditorExternalChanges() {
        if (!isGuideEditorActive() || guideEditorDraftPage == null) {
            return;
        }
        long now = System.currentTimeMillis();
        if (now < guideEditorNextExternalCheckAtMillis) {
            return;
        }
        guideEditorNextExternalCheckAtMillis = now + 250L;
        if (guideEditorDraftSource == null) {
            return;
        }

        String externalSource = guideEditorFileStore
            .readPageText(guide, currentAnchor.pageId(), guideEditorDraftPage.getLanguage());
        if (externalSource == null || Objects.equals(externalSource, guideEditorDraftSource)
            || Objects.equals(externalSource, guideEditorSavedSource)) {
            return;
        }
        if (!guideEditorDirty) {
            applyGuideEditorExternalSource(externalSource);
            return;
        }

        GuideScreenEditorMerge.Result merge = GuideScreenEditorMerge
            .merge(guideEditorSavedSource, guideEditorDraftSource, externalSource);
        if (merge.getKind() == GuideScreenEditorMerge.Result.Kind.CONFLICT) {
            openGuideEditorConflictPrompt(externalSource);
            return;
        }
        if (merge.getKind() == GuideScreenEditorMerge.Result.Kind.EXTERNAL_WINS) {
            applyGuideEditorExternalSource(merge.getText());
            return;
        }
        if (merge.getKind() == GuideScreenEditorMerge.Result.Kind.MERGED) {
            applyGuideEditorExternalSource(merge.getText());
        }
    }

    private void applyGuideEditorExternalSource(String source) {
        if (guideEditorTextArea == null) {
            return;
        }
        String safeSource = source != null ? source : "";
        int selectionStart = Math.min(guideEditorTextArea.getSelectionStart(), safeSource.length());
        int selectionEnd = Math.min(guideEditorTextArea.getSelectionEnd(), safeSource.length());
        guideEditorSuppressUndoRecording = true;
        try {
            guideEditorTextArea.applyEdit(safeSource, selectionStart, selectionEnd);
        } finally {
            guideEditorSuppressUndoRecording = false;
        }
        guideEditorDraftSource = safeSource;
        guideEditorSavedSource = safeSource;
        guideEditorDirty = false;
        guideEditorPreviewDirty = true;
        guideEditorNextSaveAtMillis = 0L;
        guideEditorNextPreviewCompileAtMillis = 0L;
        guideEditorNextExternalCheckAtMillis = System.currentTimeMillis() + 250L;
        guideEditorUndoHistory.reset(safeSource, selectionStart, selectionEnd);
        refreshGuideEditorPreviewState();
        syncGuideEditorPreviewScrollFromEditor();
    }

    private void refreshGuideEditorPreviewState() {
        if (guideEditorDraftSource == null || guideEditorDraftPage == null) {
            return;
        }
        try {
            guideEditorDraftPage = PageCompiler.parse(
                guideEditorDraftPage.getSourcePack(),
                guideEditorDraftPage.getLanguage(),
                currentAnchor.pageId(),
                guideEditorDraftSource);
            applyGuideEditorPageWithoutReload(guideEditorDraftPage);
            startBackgroundIndexRefresh();
        } catch (Throwable t) {
            FMLLog.warning("Failed to refresh guide editor draft state for {}", currentAnchor.pageId(), t);
        }
    }

    private void openGuideEditorConflictPrompt(final String externalSource) {
        mc.displayGuiScreen(
            new GuideScreenEditorConflictPrompt(
                this,
                GuidebookText.GuideEditorConflictTitle.text(),
                GuidebookText.GuideEditorConflictMessage.text(),
                new GuideScreenEditorConflictPrompt.Callback() {

                    @Override
                    public void keepExternal() {
                        applyGuideEditorExternalSource(externalSource);
                    }

                    @Override
                    public void keepLocal() {
                        saveGuideEditorDraft();
                    }
                }));
    }

    private void startBackgroundIndexRefresh() {
        Thread refreshThread = new Thread(new Runnable() {

            @Override
            public void run() {
                try {
                    guide.rebuildEditorNavigationState();
                } catch (Throwable t) {
                    FMLLog.warning("Guide editor index refresh failed", t);
                }
            }
        }, "GuideNH-GuideEditor-IndexRefresh");
        refreshThread.setDaemon(true);
        refreshThread.start();
    }

    private void applyGuideEditorPageWithoutReload(ParsedGuidePage parsedPage) {
        guideEditorSuppressReloadFromEditorApply = true;
        try {
            guide.applyEditorPage(parsedPage);
        } finally {
            guideEditorSuppressReloadFromEditorApply = false;
        }
    }

    private void handleGuideEditorActionButton(int actionId) {
        if (guideEditorTextArea == null || actionId < 0 || actionId >= GuideScreenEditorAction.values().length) {
            return;
        }
        performGuideEditorAction(GuideScreenEditorAction.values()[actionId]);
    }

    private void performGuideEditorAction(GuideScreenEditorAction action) {
        if (guideEditorTextArea == null || action == null) {
            return;
        }
        switch (action) {
            case UNDO: {
                ensureGuideEditorCurrentStateInUndoHistory();
                if (!guideEditorUndoHistory.canUndo()) {
                    return;
                }
                applyGuideEditorHistoryEntry(guideEditorUndoHistory.undo());
                return;
            }
            case REDO: {
                if (!guideEditorUndoHistory.canRedo()) {
                    return;
                }
                applyGuideEditorHistoryEntry(guideEditorUndoHistory.redo());
                return;
            }
            case CUT:
                runGuideEditorTextMutation(new Runnable() {

                    @Override
                    public void run() {
                        guideEditorTextArea.cutSelection();
                    }
                });
                return;
            case COPY:
                guideEditorTextArea.copySelection();
                return;
            case PASTE:
                runGuideEditorTextMutation(new Runnable() {

                    @Override
                    public void run() {
                        guideEditorTextArea.pasteClipboard();
                    }
                });
                return;
            case SELECT_ALL:
                guideEditorTextArea.selectAll();
                return;
            default:
                GuideScreenEditorTextActions.Result result = GuideScreenEditorTextActions.apply(
                    action,
                    guideEditorTextArea.getText(),
                    guideEditorTextArea.getSelectionStart(),
                    guideEditorTextArea.getSelectionEnd());
                runGuideEditorTextMutation(new Runnable() {

                    @Override
                    public void run() {
                        guideEditorTextArea
                            .applyEdit(result.getText(), result.getSelectionStart(), result.getSelectionEnd());
                    }
                });
                return;
        }
    }

    private void applyGuideEditorHistoryEntry(GuideScreenEditorUndoHistory.Entry entry) {
        if (entry == null || guideEditorTextArea == null) {
            return;
        }
        guideEditorSuppressUndoRecording = true;
        try {
            guideEditorTextArea.applyEdit(entry.getText(), entry.getSelectionStart(), entry.getSelectionEnd());
        } finally {
            guideEditorSuppressUndoRecording = false;
        }
        guideEditorDraftSource = entry.getText();
        guideEditorDirty = !Objects.equals(guideEditorDraftSource, guideEditorSavedSource);
        guideEditorPreviewDirty = true;
        long now = System.currentTimeMillis();
        if (guideEditorDirty) {
            guideEditorNextSaveAtMillis = now + GuideScreenEditorState.getAutosaveDelayMillis();
        } else {
            guideEditorNextSaveAtMillis = 0L;
        }
        guideEditorNextPreviewCompileAtMillis = now + 100L;
        syncGuideEditorPreviewScrollFromEditor();
    }

    private void ensureGuideEditorCurrentStateInUndoHistory() {
        if (guideEditorTextArea == null) {
            return;
        }
        GuideScreenEditorUndoHistory.Entry currentEntry = guideEditorUndoHistory.current();
        if (!Objects.equals(currentEntry.getText(), guideEditorTextArea.getText())) {
            pushGuideEditorCurrentHistoryState();
        }
    }

    private void openGuideEditorContextMenu(int mouseX, int mouseY) {
        if (!isGuideEditorActive()) {
            return;
        }
        if (guideEditorContextMenu == null) {
            guideEditorContextMenu = new GuideScreenEditorContextMenu(buildGuideEditorContextMenuEntries());
        }
        guideEditorContextMenu.open(mouseX, mouseY, width, height, fontRendererObj);
    }

    private void closeGuideEditorContextMenu() {
        if (guideEditorContextMenu != null) {
            guideEditorContextMenu.close();
        }
    }

    private List<GuideScreenEditorContextMenu.Entry> buildGuideEditorContextMenuEntries() {
        List<GuideScreenEditorContextMenu.Entry> editEntries = new ArrayList<>();
        editEntries.add(GuideScreenEditorContextMenu.Entry.action(GuideScreenEditorAction.UNDO));
        editEntries.add(GuideScreenEditorContextMenu.Entry.action(GuideScreenEditorAction.REDO));
        editEntries.add(GuideScreenEditorContextMenu.Entry.separator());
        editEntries.add(GuideScreenEditorContextMenu.Entry.action(GuideScreenEditorAction.CUT));
        editEntries.add(GuideScreenEditorContextMenu.Entry.action(GuideScreenEditorAction.COPY));
        editEntries.add(GuideScreenEditorContextMenu.Entry.action(GuideScreenEditorAction.PASTE));
        editEntries.add(GuideScreenEditorContextMenu.Entry.action(GuideScreenEditorAction.SELECT_ALL));

        List<GuideScreenEditorContextMenu.Entry> insertEntries = new ArrayList<>();
        insertEntries.add(GuideScreenEditorContextMenu.Entry.action(GuideScreenEditorAction.HEADING_1));
        insertEntries.add(GuideScreenEditorContextMenu.Entry.action(GuideScreenEditorAction.HEADING_2));
        insertEntries.add(GuideScreenEditorContextMenu.Entry.action(GuideScreenEditorAction.HEADING_3));
        insertEntries.add(GuideScreenEditorContextMenu.Entry.action(GuideScreenEditorAction.HEADING_4));
        insertEntries.add(GuideScreenEditorContextMenu.Entry.action(GuideScreenEditorAction.HEADING_5));
        insertEntries.add(GuideScreenEditorContextMenu.Entry.action(GuideScreenEditorAction.HEADING_6));
        insertEntries.add(GuideScreenEditorContextMenu.Entry.separator());
        insertEntries.add(GuideScreenEditorContextMenu.Entry.action(GuideScreenEditorAction.BOLD));
        insertEntries.add(GuideScreenEditorContextMenu.Entry.action(GuideScreenEditorAction.ITALIC));
        insertEntries.add(GuideScreenEditorContextMenu.Entry.action(GuideScreenEditorAction.STRIKETHROUGH));
        insertEntries.add(GuideScreenEditorContextMenu.Entry.action(GuideScreenEditorAction.UNDERLINE));
        insertEntries.add(GuideScreenEditorContextMenu.Entry.separator());
        List<GuideScreenEditorContextMenu.Entry> inlineEntries = new ArrayList<>();
        inlineEntries.add(GuideScreenEditorContextMenu.Entry.action(GuideScreenEditorAction.KBD));
        inlineEntries.add(GuideScreenEditorContextMenu.Entry.action(GuideScreenEditorAction.SUBSCRIPT));
        inlineEntries.add(GuideScreenEditorContextMenu.Entry.action(GuideScreenEditorAction.SUPERSCRIPT));
        inlineEntries.add(GuideScreenEditorContextMenu.Entry.action(GuideScreenEditorAction.FOOTNOTE));
        inlineEntries.add(GuideScreenEditorContextMenu.Entry.action(GuideScreenEditorAction.LATEX));
        inlineEntries.add(GuideScreenEditorContextMenu.Entry.action(GuideScreenEditorAction.LATEX_SHORTHAND));
        inlineEntries.add(GuideScreenEditorContextMenu.Entry.action(GuideScreenEditorAction.KEY_BIND));
        inlineEntries.add(GuideScreenEditorContextMenu.Entry.action(GuideScreenEditorAction.PLAYER_NAME));
        inlineEntries.add(GuideScreenEditorContextMenu.Entry.action(GuideScreenEditorAction.COLOR));
        inlineEntries.add(GuideScreenEditorContextMenu.Entry.action(GuideScreenEditorAction.BREAK));
        insertEntries.add(
            GuideScreenEditorContextMenu.Entry
                .submenu(GuidebookText.GuideEditorContextMenuInline.text(), inlineEntries));
        insertEntries.add(GuideScreenEditorContextMenu.Entry.separator());
        insertEntries.add(GuideScreenEditorContextMenu.Entry.action(GuideScreenEditorAction.LINK));
        insertEntries.add(GuideScreenEditorContextMenu.Entry.action(GuideScreenEditorAction.REFERENCE_LINK));
        insertEntries.add(GuideScreenEditorContextMenu.Entry.action(GuideScreenEditorAction.IMAGE));
        insertEntries.add(GuideScreenEditorContextMenu.Entry.action(GuideScreenEditorAction.REFERENCE_IMAGE));
        insertEntries.add(GuideScreenEditorContextMenu.Entry.action(GuideScreenEditorAction.INLINE_CODE));
        insertEntries.add(GuideScreenEditorContextMenu.Entry.action(GuideScreenEditorAction.CODE_BLOCK));
        insertEntries.add(GuideScreenEditorContextMenu.Entry.action(GuideScreenEditorAction.BLOCKQUOTE));
        insertEntries.add(GuideScreenEditorContextMenu.Entry.separator());
        insertEntries.add(GuideScreenEditorContextMenu.Entry.action(GuideScreenEditorAction.UNORDERED_LIST));
        insertEntries.add(GuideScreenEditorContextMenu.Entry.action(GuideScreenEditorAction.ORDERED_LIST));
        insertEntries.add(GuideScreenEditorContextMenu.Entry.action(GuideScreenEditorAction.TASK_LIST));
        insertEntries.add(GuideScreenEditorContextMenu.Entry.action(GuideScreenEditorAction.TABLE));
        insertEntries.add(GuideScreenEditorContextMenu.Entry.separator());
        List<GuideScreenEditorContextMenu.Entry> blockEntries = new ArrayList<>();
        blockEntries.add(GuideScreenEditorContextMenu.Entry.action(GuideScreenEditorAction.ALERT_NOTE));
        blockEntries.add(GuideScreenEditorContextMenu.Entry.action(GuideScreenEditorAction.ALERT_TIP));
        blockEntries.add(GuideScreenEditorContextMenu.Entry.action(GuideScreenEditorAction.ALERT_IMPORTANT));
        blockEntries.add(GuideScreenEditorContextMenu.Entry.action(GuideScreenEditorAction.ALERT_WARNING));
        blockEntries.add(GuideScreenEditorContextMenu.Entry.action(GuideScreenEditorAction.ALERT_CAUTION));
        blockEntries.add(GuideScreenEditorContextMenu.Entry.action(GuideScreenEditorAction.DETAILS));
        blockEntries.add(GuideScreenEditorContextMenu.Entry.action(GuideScreenEditorAction.QUOTE_CALLOUT));
        blockEntries.add(GuideScreenEditorContextMenu.Entry.action(GuideScreenEditorAction.QUOTE_ICON_TEXT));
        blockEntries.add(GuideScreenEditorContextMenu.Entry.action(GuideScreenEditorAction.QUOTE_ICON_ITEM));
        blockEntries.add(GuideScreenEditorContextMenu.Entry.action(GuideScreenEditorAction.QUOTE_ICON_PNG));
        blockEntries.add(GuideScreenEditorContextMenu.Entry.action(GuideScreenEditorAction.CSV_TABLE));
        blockEntries.add(GuideScreenEditorContextMenu.Entry.action(GuideScreenEditorAction.MERMAID));
        blockEntries.add(GuideScreenEditorContextMenu.Entry.action(GuideScreenEditorAction.FILE_TREE));
        blockEntries.add(GuideScreenEditorContextMenu.Entry.action(GuideScreenEditorAction.SUB_PAGES));
        blockEntries.add(GuideScreenEditorContextMenu.Entry.action(GuideScreenEditorAction.CATEGORY_INDEX));
        blockEntries.add(GuideScreenEditorContextMenu.Entry.action(GuideScreenEditorAction.FOOTNOTE_LIST));
        blockEntries.add(GuideScreenEditorContextMenu.Entry.action(GuideScreenEditorAction.ROW));
        blockEntries.add(GuideScreenEditorContextMenu.Entry.action(GuideScreenEditorAction.COLUMN));
        blockEntries.add(GuideScreenEditorContextMenu.Entry.action(GuideScreenEditorAction.DIV));
        blockEntries.add(GuideScreenEditorContextMenu.Entry.action(GuideScreenEditorAction.ITEM_GRID));
        blockEntries.add(GuideScreenEditorContextMenu.Entry.action(GuideScreenEditorAction.CSV_TABLE_IMPORT));
        blockEntries.add(GuideScreenEditorContextMenu.Entry.action(GuideScreenEditorAction.ANCHOR));
        blockEntries.add(GuideScreenEditorContextMenu.Entry.action(GuideScreenEditorAction.COLUMN_CHART));
        blockEntries.add(GuideScreenEditorContextMenu.Entry.action(GuideScreenEditorAction.BAR_CHART));
        blockEntries.add(GuideScreenEditorContextMenu.Entry.action(GuideScreenEditorAction.LINE_CHART));
        blockEntries.add(GuideScreenEditorContextMenu.Entry.action(GuideScreenEditorAction.PIE_CHART));
        blockEntries.add(GuideScreenEditorContextMenu.Entry.action(GuideScreenEditorAction.SCATTER_CHART));
        blockEntries.add(GuideScreenEditorContextMenu.Entry.action(GuideScreenEditorAction.CHART_SERIES));
        blockEntries.add(GuideScreenEditorContextMenu.Entry.action(GuideScreenEditorAction.CHART_LINE_SERIES));
        blockEntries.add(GuideScreenEditorContextMenu.Entry.action(GuideScreenEditorAction.CHART_SLICE));
        blockEntries.add(GuideScreenEditorContextMenu.Entry.action(GuideScreenEditorAction.CHART_PIE_INSET));
        blockEntries.add(GuideScreenEditorContextMenu.Entry.action(GuideScreenEditorAction.FUNCTION_GRAPH));
        blockEntries.add(GuideScreenEditorContextMenu.Entry.action(GuideScreenEditorAction.FUNCTION));
        blockEntries.add(GuideScreenEditorContextMenu.Entry.action(GuideScreenEditorAction.FUNCTION_PLOT));
        blockEntries.add(GuideScreenEditorContextMenu.Entry.action(GuideScreenEditorAction.FUNCTION_POINT));
        blockEntries.add(GuideScreenEditorContextMenu.Entry.action(GuideScreenEditorAction.FUNCTION_GRAPH_FENCE));
        blockEntries.add(GuideScreenEditorContextMenu.Entry.action(GuideScreenEditorAction.STRUCTURE));
        blockEntries.add(GuideScreenEditorContextMenu.Entry.separator());
        blockEntries.add(GuideScreenEditorContextMenu.Entry.action(GuideScreenEditorAction.GAME_SCENE));
        blockEntries.add(GuideScreenEditorContextMenu.Entry.action(GuideScreenEditorAction.SCENE_BLOCK));
        blockEntries.add(GuideScreenEditorContextMenu.Entry.action(GuideScreenEditorAction.SCENE_ENTITY));
        blockEntries.add(GuideScreenEditorContextMenu.Entry.action(GuideScreenEditorAction.ISOMETRIC_CAMERA));
        blockEntries.add(GuideScreenEditorContextMenu.Entry.action(GuideScreenEditorAction.BOX_ANNOTATION));
        blockEntries.add(GuideScreenEditorContextMenu.Entry.action(GuideScreenEditorAction.BLOCK_ANNOTATION));
        blockEntries.add(GuideScreenEditorContextMenu.Entry.action(GuideScreenEditorAction.LINE_ANNOTATION));
        blockEntries.add(GuideScreenEditorContextMenu.Entry.action(GuideScreenEditorAction.DIAMOND_ANNOTATION));
        blockEntries.add(GuideScreenEditorContextMenu.Entry.action(GuideScreenEditorAction.TEXT_ANNOTATION));
        blockEntries.add(GuideScreenEditorContextMenu.Entry.action(GuideScreenEditorAction.BLOCK_ANNOTATION_TEMPLATE));
        blockEntries.add(GuideScreenEditorContextMenu.Entry.separator());
        blockEntries.add(GuideScreenEditorContextMenu.Entry.action(GuideScreenEditorAction.IMPORT_STRUCTURE));
        blockEntries.add(GuideScreenEditorContextMenu.Entry.action(GuideScreenEditorAction.IMPORT_STRUCTURE_LIB));
        blockEntries.add(GuideScreenEditorContextMenu.Entry.action(GuideScreenEditorAction.IMPORT_PONDER));
        blockEntries.add(GuideScreenEditorContextMenu.Entry.action(GuideScreenEditorAction.PLACE_BLOCK));
        blockEntries.add(GuideScreenEditorContextMenu.Entry.action(GuideScreenEditorAction.REPLACE_BLOCK));
        blockEntries.add(GuideScreenEditorContextMenu.Entry.action(GuideScreenEditorAction.REMOVE_BLOCKS));
        insertEntries.add(
            GuideScreenEditorContextMenu.Entry
                .submenu(GuidebookText.GuideEditorContextMenuBlocks.text(), blockEntries));
        List<GuideScreenEditorContextMenu.Entry> embedEntries = new ArrayList<>();
        embedEntries.add(GuideScreenEditorContextMenu.Entry.action(GuideScreenEditorAction.TOOLTIP));
        embedEntries.add(GuideScreenEditorContextMenu.Entry.action(GuideScreenEditorAction.ITEM_IMAGE));
        embedEntries.add(GuideScreenEditorContextMenu.Entry.action(GuideScreenEditorAction.BLOCK_IMAGE));
        embedEntries.add(GuideScreenEditorContextMenu.Entry.action(GuideScreenEditorAction.ITEM_LINK));
        embedEntries.add(GuideScreenEditorContextMenu.Entry.action(GuideScreenEditorAction.COMMAND_LINK));
        embedEntries.add(GuideScreenEditorContextMenu.Entry.action(GuideScreenEditorAction.RECIPE));
        embedEntries.add(GuideScreenEditorContextMenu.Entry.action(GuideScreenEditorAction.RECIPE_FOR));
        embedEntries.add(GuideScreenEditorContextMenu.Entry.action(GuideScreenEditorAction.RECIPES_FOR));
        embedEntries.add(GuideScreenEditorContextMenu.Entry.action(GuideScreenEditorAction.FLOATING_IMAGE));
        embedEntries.add(GuideScreenEditorContextMenu.Entry.action(GuideScreenEditorAction.QUEST_LINK));
        embedEntries.add(GuideScreenEditorContextMenu.Entry.action(GuideScreenEditorAction.QUEST_CARD));
        embedEntries.add(GuideScreenEditorContextMenu.Entry.action(GuideScreenEditorAction.QUEST_IDS));
        embedEntries.add(GuideScreenEditorContextMenu.Entry.action(GuideScreenEditorAction.NAV_POSITION));
        embedEntries.add(GuideScreenEditorContextMenu.Entry.action(GuideScreenEditorAction.NAV_ICON));
        embedEntries.add(GuideScreenEditorContextMenu.Entry.action(GuideScreenEditorAction.NAV_ICON_TEXTURE));
        embedEntries.add(GuideScreenEditorContextMenu.Entry.action(GuideScreenEditorAction.NAV_ICONS));
        embedEntries.add(GuideScreenEditorContextMenu.Entry.action(GuideScreenEditorAction.NAV_ICON_TEXTURES));
        embedEntries.add(GuideScreenEditorContextMenu.Entry.action(GuideScreenEditorAction.NAV_REQUIRED_MODS));
        embedEntries.add(GuideScreenEditorContextMenu.Entry.action(GuideScreenEditorAction.PAGE_CATEGORIES));
        embedEntries.add(GuideScreenEditorContextMenu.Entry.action(GuideScreenEditorAction.PAGE_ITEM_IDS));
        embedEntries.add(GuideScreenEditorContextMenu.Entry.action(GuideScreenEditorAction.PAGE_ORE_IDS));
        embedEntries.add(GuideScreenEditorContextMenu.Entry.action(GuideScreenEditorAction.PAGE_METADATA));
        insertEntries.add(
            GuideScreenEditorContextMenu.Entry
                .submenu(GuidebookText.GuideEditorContextMenuEmbeds.text(), embedEntries));
        insertEntries.add(GuideScreenEditorContextMenu.Entry.action(GuideScreenEditorAction.THEMATIC_BREAK));

        List<GuideScreenEditorContextMenu.Entry> entries = new ArrayList<>();
        entries.add(
            GuideScreenEditorContextMenu.Entry.submenu(GuidebookText.GuideEditorContextMenuEdit.text(), editEntries));
        entries.add(
            GuideScreenEditorContextMenu.Entry
                .submenu(GuidebookText.GuideEditorContextMenuInsert.text(), insertEntries));
        return entries;
    }

    private int getGuideEditorPreviewLayoutWidth() {
        return Math.max(1, getGuideEditorPreviewPaneWidth() - SceneEditorMultilineTextArea.PADDING * 2);
    }

    private void rebuildToolbar() {
        this.buttonList.clear();
        int btnY = getToolbarButtonY();
        int btnRight = panelX + panelW - PANEL_PADDING;
        int leftX = panelX + PANEL_PADDING;
        int rightToolbarLeft = getRightToolbarButtonsLeft();
        btnSearch = new GuideIconButton(
            4,
            isSearchPage() ? leftX : rightToolbarLeft,
            btnY,
            GuideIconButton.Role.SEARCH);
        btnBack = new GuideIconButton(
            3,
            btnRight - (16 + TOOLBAR_GAP) * 4 + TOOLBAR_GAP,
            btnY,
            GuideIconButton.Role.BACK);
        btnForward = new GuideIconButton(
            2,
            btnRight - (16 + TOOLBAR_GAP) * 3 + TOOLBAR_GAP,
            btnY,
            GuideIconButton.Role.FORWARD);
        btnFullWidth = new GuideIconButton(
            1,
            btnRight - (16 + TOOLBAR_GAP) * 2 + TOOLBAR_GAP,
            btnY,
            fullWidth ? GuideIconButton.Role.CLOSE_FULL_WIDTH_VIEW : GuideIconButton.Role.OPEN_FULL_WIDTH_VIEW);
        btnClose = new GuideIconButton(0, btnRight - 16, btnY, GuideIconButton.Role.CLOSE);
        rebuildGuideEditorModeButtons(rightToolbarLeft - TOOLBAR_GAP, btnY);
        this.buttonList.add(btnSearch);
        this.buttonList.add(btnBack);
        this.buttonList.add(btnForward);
        this.buttonList.add(btnFullWidth);
        this.buttonList.add(btnClose);
        updateToolbarButtonState();
    }

    private int getToolbarButtonY() {
        return panelY + Math.max(0, (TOOLBAR_H - GuideIconButton.HEIGHT) / 2);
    }

    private void rebuildGuideEditorModeButtons(int rightX, int btnY) {
        if (!isGuideEditorActive()) {
            clearGuideEditorActionButtons();
            btnGuideEditorToggle = null;
            btnGuideEditorNewPage = null;
            btnGuideEditorLayoutSplit = null;
            btnGuideEditorLayoutEditorOnly = null;
            btnGuideEditorLayoutPreviewOnly = null;
            btnGuideEditorAdvancedToggle = null;
            return;
        }

        int x = rightX - GuideIconButton.WIDTH;
        btnGuideEditorAdvancedToggle = addEditorModeButton(
            105,
            x,
            btnY,
            GuideIconButton.Role.GUIDE_EDITOR_ADVANCED_TOGGLE);
        x -= GuideIconButton.WIDTH + TOOLBAR_GAP;
        btnGuideEditorLayoutPreviewOnly = addEditorModeButton(
            104,
            x,
            btnY,
            GuideIconButton.Role.GUIDE_EDITOR_LAYOUT_PREVIEW_ONLY);
        x -= GuideIconButton.WIDTH + TOOLBAR_GAP;
        btnGuideEditorLayoutEditorOnly = addEditorModeButton(
            103,
            x,
            btnY,
            GuideIconButton.Role.GUIDE_EDITOR_LAYOUT_EDITOR_ONLY);
        x -= GuideIconButton.WIDTH + TOOLBAR_GAP;
        btnGuideEditorLayoutSplit = addEditorModeButton(102, x, btnY, GuideIconButton.Role.GUIDE_EDITOR_LAYOUT_SPLIT);
        x -= GuideIconButton.WIDTH + TOOLBAR_GAP;
        btnGuideEditorNewPage = addEditorModeButton(101, x, btnY, GuideIconButton.Role.GUIDE_EDITOR_NEW_PAGE);
        x -= GuideIconButton.WIDTH + TOOLBAR_GAP;
        btnGuideEditorToggle = addEditorModeButton(100, x, btnY, GuideIconButton.Role.GUIDE_EDITOR_TOGGLE);
        rebuildGuideEditorActionButtons();
    }

    private GuideIconButton addEditorModeButton(int id, int x, int y, GuideIconButton.Role role) {
        GuideIconButton button = new GuideIconButton(id, x, y, role);
        this.buttonList.add(button);
        return button;
    }

    private void clearGuideEditorActionButtons() {
        guideEditorActionButtons.clear();
    }

    private void rebuildGuideEditorActionButtons() {
        guideEditorActionButtons.clear();
        if (!isGuideEditorActive()) {
            return;
        }

        for (GuideScreenEditorAction action : getGuideEditorActionOrder()) {
            int id = 2000 + action.ordinal();
            GuideIconButton button = new GuideIconButton(id, 0, 0, action.toRole());
            guideEditorActionButtons.put(id, button);
            buttonList.add(button);
        }
    }

    private List<GuideScreenEditorAction> getGuideEditorActionOrder() {
        List<GuideScreenEditorAction> actions = new ArrayList<>();
        actions.add(GuideScreenEditorAction.HEADING_1);
        actions.add(GuideScreenEditorAction.HEADING_2);
        actions.add(GuideScreenEditorAction.HEADING_3);
        actions.add(GuideScreenEditorAction.BOLD);
        actions.add(GuideScreenEditorAction.ITALIC);
        actions.add(GuideScreenEditorAction.KBD);
        actions.add(GuideScreenEditorAction.SUBSCRIPT);
        actions.add(GuideScreenEditorAction.SUPERSCRIPT);
        actions.add(GuideScreenEditorAction.FOOTNOTE);
        actions.add(GuideScreenEditorAction.LATEX);
        actions.add(GuideScreenEditorAction.COLOR);
        actions.add(GuideScreenEditorAction.LINK);
        actions.add(GuideScreenEditorAction.INLINE_CODE);
        actions.add(GuideScreenEditorAction.CODE_BLOCK);
        actions.add(GuideScreenEditorAction.BLOCKQUOTE);
        actions.add(GuideScreenEditorAction.UNORDERED_LIST);
        actions.add(GuideScreenEditorAction.ORDERED_LIST);
        actions.add(GuideScreenEditorAction.TASK_LIST);
        actions.add(GuideScreenEditorAction.TABLE);
        actions.add(GuideScreenEditorAction.THEMATIC_BREAK);
        if (guideEditorAdvancedToolbarVisible) {
            actions.add(GuideScreenEditorAction.HEADING_4);
            actions.add(GuideScreenEditorAction.HEADING_5);
            actions.add(GuideScreenEditorAction.HEADING_6);
            actions.add(GuideScreenEditorAction.STRIKETHROUGH);
            actions.add(GuideScreenEditorAction.UNDERLINE);
            actions.add(GuideScreenEditorAction.IMAGE);
            actions.add(GuideScreenEditorAction.REFERENCE_LINK);
            actions.add(GuideScreenEditorAction.REFERENCE_IMAGE);
            actions.add(GuideScreenEditorAction.ALERT_NOTE);
            actions.add(GuideScreenEditorAction.ALERT_TIP);
            actions.add(GuideScreenEditorAction.ALERT_IMPORTANT);
            actions.add(GuideScreenEditorAction.ALERT_WARNING);
            actions.add(GuideScreenEditorAction.ALERT_CAUTION);
            actions.add(GuideScreenEditorAction.DETAILS);
            actions.add(GuideScreenEditorAction.QUOTE_CALLOUT);
            actions.add(GuideScreenEditorAction.QUOTE_ICON_TEXT);
            actions.add(GuideScreenEditorAction.QUOTE_ICON_ITEM);
            actions.add(GuideScreenEditorAction.QUOTE_ICON_PNG);
            actions.add(GuideScreenEditorAction.KEY_BIND);
            actions.add(GuideScreenEditorAction.PLAYER_NAME);
            actions.add(GuideScreenEditorAction.BREAK);
            actions.add(GuideScreenEditorAction.TOOLTIP);
            actions.add(GuideScreenEditorAction.ITEM_IMAGE);
            actions.add(GuideScreenEditorAction.BLOCK_IMAGE);
            actions.add(GuideScreenEditorAction.ITEM_LINK);
            actions.add(GuideScreenEditorAction.CSV_TABLE);
            actions.add(GuideScreenEditorAction.COMMAND_LINK);
            actions.add(GuideScreenEditorAction.RECIPE);
            actions.add(GuideScreenEditorAction.RECIPE_FOR);
            actions.add(GuideScreenEditorAction.RECIPES_FOR);
            actions.add(GuideScreenEditorAction.FLOATING_IMAGE);
            actions.add(GuideScreenEditorAction.MERMAID);
            actions.add(GuideScreenEditorAction.FILE_TREE);
            actions.add(GuideScreenEditorAction.SUB_PAGES);
            actions.add(GuideScreenEditorAction.CATEGORY_INDEX);
            actions.add(GuideScreenEditorAction.FOOTNOTE_LIST);
            actions.add(GuideScreenEditorAction.ROW);
            actions.add(GuideScreenEditorAction.COLUMN);
            actions.add(GuideScreenEditorAction.DIV);
            actions.add(GuideScreenEditorAction.ITEM_GRID);
            actions.add(GuideScreenEditorAction.CSV_TABLE_IMPORT);
            actions.add(GuideScreenEditorAction.ANCHOR);
            actions.add(GuideScreenEditorAction.COLUMN_CHART);
            actions.add(GuideScreenEditorAction.BAR_CHART);
            actions.add(GuideScreenEditorAction.LINE_CHART);
            actions.add(GuideScreenEditorAction.PIE_CHART);
            actions.add(GuideScreenEditorAction.SCATTER_CHART);
            actions.add(GuideScreenEditorAction.CHART_SERIES);
            actions.add(GuideScreenEditorAction.CHART_LINE_SERIES);
            actions.add(GuideScreenEditorAction.CHART_SLICE);
            actions.add(GuideScreenEditorAction.CHART_PIE_INSET);
            actions.add(GuideScreenEditorAction.FUNCTION_GRAPH);
            actions.add(GuideScreenEditorAction.FUNCTION);
            actions.add(GuideScreenEditorAction.FUNCTION_PLOT);
            actions.add(GuideScreenEditorAction.FUNCTION_POINT);
            actions.add(GuideScreenEditorAction.FUNCTION_GRAPH_FENCE);
            actions.add(GuideScreenEditorAction.STRUCTURE);
            actions.add(GuideScreenEditorAction.GAME_SCENE);
            actions.add(GuideScreenEditorAction.SCENE_BLOCK);
            actions.add(GuideScreenEditorAction.SCENE_ENTITY);
            actions.add(GuideScreenEditorAction.ISOMETRIC_CAMERA);
            actions.add(GuideScreenEditorAction.BOX_ANNOTATION);
            actions.add(GuideScreenEditorAction.BLOCK_ANNOTATION);
            actions.add(GuideScreenEditorAction.LINE_ANNOTATION);
            actions.add(GuideScreenEditorAction.DIAMOND_ANNOTATION);
            actions.add(GuideScreenEditorAction.TEXT_ANNOTATION);
            actions.add(GuideScreenEditorAction.BLOCK_ANNOTATION_TEMPLATE);
            actions.add(GuideScreenEditorAction.IMPORT_STRUCTURE);
            actions.add(GuideScreenEditorAction.IMPORT_STRUCTURE_LIB);
            actions.add(GuideScreenEditorAction.IMPORT_PONDER);
            actions.add(GuideScreenEditorAction.PLACE_BLOCK);
            actions.add(GuideScreenEditorAction.REPLACE_BLOCK);
            actions.add(GuideScreenEditorAction.REMOVE_BLOCKS);
            actions.add(GuideScreenEditorAction.QUEST_LINK);
            actions.add(GuideScreenEditorAction.QUEST_CARD);
            actions.add(GuideScreenEditorAction.QUEST_IDS);
            actions.add(GuideScreenEditorAction.NAV_POSITION);
            actions.add(GuideScreenEditorAction.NAV_ICON);
            actions.add(GuideScreenEditorAction.NAV_ICON_TEXTURE);
            actions.add(GuideScreenEditorAction.NAV_ICONS);
            actions.add(GuideScreenEditorAction.NAV_ICON_TEXTURES);
            actions.add(GuideScreenEditorAction.NAV_REQUIRED_MODS);
            actions.add(GuideScreenEditorAction.PAGE_CATEGORIES);
            actions.add(GuideScreenEditorAction.PAGE_ITEM_IDS);
            actions.add(GuideScreenEditorAction.PAGE_ORE_IDS);
            actions.add(GuideScreenEditorAction.PAGE_METADATA);
            actions.add(GuideScreenEditorAction.LATEX_SHORTHAND);
            actions.add(GuideScreenEditorAction.UNDO);
            actions.add(GuideScreenEditorAction.REDO);
            actions.add(GuideScreenEditorAction.CUT);
            actions.add(GuideScreenEditorAction.COPY);
            actions.add(GuideScreenEditorAction.PASTE);
            actions.add(GuideScreenEditorAction.SELECT_ALL);
        }
        return actions;
    }

    @Override
    protected void actionPerformed(GuiButton btn) {
        if (btn == btnClose) {
            close();
        } else if (btn == btnBack) {
            if (!history.isEmpty()) {
                forwardHistory.push(currentAnchor);
                var prev = history.pop();
                navigateWithoutHistory(prev);
                rebuildToolbar();
            }
        } else if (btn == btnForward) {
            if (!forwardHistory.isEmpty()) {
                history.push(currentAnchor);
                var next = forwardHistory.pop();
                navigateWithoutHistory(next);
                rebuildToolbar();
            }
        } else if (btn == btnFullWidth) {
            fullWidth = !fullWidth;
            try {
                ModConfig.ui.fullWidth = fullWidth;
                ModConfig.save();
            } catch (Throwable ignored) {
                // Saving the preference is optional for toggling the UI.
            }
            recomputePanelBounds();
            rebuildToolbar();
            layoutDocument = null;
            lastLayoutWidth = -1;
            ensureLayout();
            clampScroll();
        } else if (btn == btnSearch) {
            if (isSearchPage()) {
                return;
            } else {
                navigateTo(GuideSearchPage.anchorForQuery(""));
                focusSearchField();
            }
        } else if (btn == btnGuideEditorToggle) {
            toggleGuideEditorEnabled();
        } else if (btn == btnGuideEditorNewPage) {
            createGuideEditorPage();
        } else if (btn == btnGuideEditorLayoutSplit) {
            setGuideEditorLayoutMode(GuideScreenEditorLayoutMode.SPLIT);
        } else if (btn == btnGuideEditorLayoutEditorOnly) {
            setGuideEditorLayoutMode(GuideScreenEditorLayoutMode.EDITOR_ONLY);
        } else if (btn == btnGuideEditorLayoutPreviewOnly) {
            setGuideEditorLayoutMode(GuideScreenEditorLayoutMode.PREVIEW_ONLY);
        } else if (btn == btnGuideEditorAdvancedToggle) {
            toggleGuideEditorAdvancedButtons();
        } else if (btn != null && btn.id >= 2000) {
            handleGuideEditorActionButton(btn.id - 2000);
        }
    }

    private void loadCurrentPage() {
        clearInteractionState();
        closeGuideEditorContextMenu();
        layoutDocument = null;
        lastLayoutWidth = -1;
        cachedBottomBarText = null;
        cachedBottomBarPage = null;
        if (isSearchPage()) {
            currentPage = null;
            document = null;
            rebuildSearchDocumentIfNeeded(true);
        } else if (isItemLinksPage()) {
            currentPage = null;
            searchField = null;
            ItemStack stack = GuideItemLinksPage.stackFromAnchor(currentAnchor);
            document = buildItemLinksDocument(stack);
        } else {
            searchField = null;
            try {
                currentPage = guide.getPage(currentAnchor.pageId());
            } catch (Throwable t) {
                FMLLog.severe("Failed to compile guide page {}", currentAnchor.pageId(), t);
                currentPage = null;
            }
            if (currentPage != null) {
                document = currentPage.document();
                registerPageScenes();
            } else {
                document = null;
            }
        }
        refreshCurrentPageTitle();
        scrollY = 0;
        if (isGuideEditorActive()) {
            refreshGuideEditorDraft(true);
        }
        updateToolbarButtonState();
    }

    private void registerPageScenes() {
        if (currentPage == null) return;
        var scenes = currentPage.scenes();
        for (int i = 0; i < scenes.size(); i++) {
            var scene = scenes.get(i);
            if (!registeredScenes.add(scene)) continue;
            var level = scene.getLevel();
            if (level.isEmpty()) continue;
            int[] bounds = level.getBounds();
            int sizeX = bounds[3] - bounds[0] + 1;
            int sizeY = bounds[4] - bounds[1] + 1;
            int sizeZ = bounds[5] - bounds[2] + 1;
            String label = currentAnchor != null ? currentAnchor.pageId() + "#" + i : "scene#" + i;
            String structureText = RegionWandItem
                .exportRegionAsStructureSnbt(level, bounds[0], bounds[1], bounds[2], sizeX, sizeY, sizeZ);
            if (structureText != null) {
                GuideNhClientBridgeController.getInstance()
                    .rememberScene(label, structureText);
            }
        }
    }

    private void tickGuideEditorPreviewScenes() {
        if (!isGuideEditorActive() || guideEditorPreviewPage == null) {
            return;
        }
        for (LytGuidebookScene scene : guideEditorPreviewPage.scenes()) {
            scene.ponderTick();
        }
    }

    private float resolveCurrentZoom() {
        float global = Math.max(0.1f, ModConfig.ui.contentZoom);
        float perPage = currentPage != null && currentPage.pageMeta() != null ? currentPage.pageMeta()
            .zoom() : 1.0f;
        return global * (perPage > 0f ? perPage : 1.0f);
    }

    private void ensureLayout() {
        var activeDocument = getActiveDocument();
        if (activeDocument == null) return;
        int layoutWidth = Math.max(1, Math.round(contentW / currentZoom));
        if (!activeDocument.hasLayout() || layoutDocument != activeDocument || lastLayoutWidth != layoutWidth) {
            clearInteractionState();
            activeDocument.updateLayout(new LayoutContext(layoutFontMetrics), layoutWidth);
            layoutDocument = activeDocument;
            lastLayoutWidth = layoutWidth;
        }
    }

    private void scrollToCurrentAnchor() {
        if (!pendingAnchorScroll) return;
        if (currentAnchor == null || currentAnchor.anchor() == null) return;
        if (document == null) return;
        pendingAnchorScroll = false;
        var target = new AnchorIndexer(document).get(currentAnchor.anchor());
        if (target == null) return;
        var blockNode = target.blockNode();
        var flowContent = target.flowContent();
        if (flowContent instanceof LytFlowAnchor flowAnchor) {
            var layoutY = flowAnchor.getLayoutY();
            if (layoutY.isPresent()) {
                scrollY = layoutY.getAsInt();
                return;
            }
        }
        LytRect bounds = blockNode.getBounds();
        if (bounds != null) {
            scrollY = bounds.y();
        }
    }

    private void refreshCurrentPageTitle() {
        pageTitle.clearContent();
        cachedTitleLayoutWidth = -1;

        if (currentAnchor == null) {
            currentPageTitle = "";
            return;
        }

        if (isSearchPage()) {
            currentPageTitle = GuidebookText.Search.text();
            return;
        }

        if (isItemLinksPage()) {
            ItemStack stack = GuideItemLinksPage.stackFromAnchor(currentAnchor);
            String itemName = stack != null ? stack.getDisplayName() : currentAnchor.anchor();
            currentPageTitle = GuidebookText.ItemLinksTitle.text(itemName);
            pageTitle.appendText(currentPageTitle);
            return;
        }

        LytHeading extracted = currentPage != null ? currentPage.titleHeading() : null;

        if (extracted != null) {
            for (var flowContent : extracted.getContent()) {
                pageTitle.append(flowContent);
            }
            currentPageTitle = extracted.getTextContent();
        } else {
            String resolvedTitle = null;
            try {
                var node = guide.getNavigationTree()
                    .getNodeById(currentAnchor.pageId());
                if (node != null) {
                    resolvedTitle = node.title();
                }
            } catch (Throwable ignored) {}

            if (resolvedTitle == null || resolvedTitle.isEmpty()) {
                resolvedTitle = currentAnchor.pageId()
                    .toString();
            }
            currentPageTitle = resolvedTitle;
            pageTitle.appendText(resolvedTitle);
        }
    }

    private int getContentHeight() {
        var activeDocument = getActiveDocument();
        return activeDocument != null ? activeDocument.getContentHeight() : 0;
    }

    private int getDocumentViewportY() {
        return contentY;
    }

    private int getDocumentViewportHeight() {
        // Return in layout units: divide screen pixels by zoom.
        return currentZoom == 1.0f ? contentH : Math.max(1, Math.round(contentH / currentZoom));
    }

    private int getMaxScroll() {
        return Math.max(0, getContentHeight() - getDocumentViewportHeight());
    }

    private void clampScroll() {
        int max = getMaxScroll();
        if (scrollY < 0) scrollY = 0;
        if (scrollY > max) scrollY = max;
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        hoveredItemStack = null;
        drawTiledBackground();
        recomputePanelBounds();
        ensureSearchField();
        rebuildSearchDocumentIfNeeded(false);
        currentZoom = resolveCurrentZoom();
        ensureLayout();
        clampScroll();

        int navX = panelX;
        int navY = panelY + TOOLBAR_H + 1;
        int bottomBarH = hasBottomBar() ? TOOLBAR_H : 0;
        int navH = Math.max(20, panelH - TOOLBAR_H - 1 - bottomBarH);
        navBar.setBounds(navX, navY, navH);
        navBar.update(mouseX, mouseY, guide.getNavigationTree());
        int contentMouseX = mouseX;
        int contentMouseY = mouseY;
        if (navBar.isOpen() && navBar.contains(mouseX, mouseY)) {
            contentMouseX = Integer.MIN_VALUE;
            contentMouseY = Integer.MIN_VALUE;
        }

        drawRect(panelX, panelY, panelX + panelW, panelY + panelH, BG_COLOR);
        drawBorder(panelX, panelY, panelW, panelH, BG_BORDER);

        drawRect(panelX, panelY + TOOLBAR_H, panelX + panelW, panelY + TOOLBAR_H + 1, 0xFF2A2A2A);

        if (!isGuideEditorActive()) {
            updateSceneHover(contentMouseX, contentMouseY);
        }
        pollActiveSceneDrag();

        if (isGuideEditorActive()) {
            drawGuideEditorScreen(contentMouseX, contentMouseY);
        } else {
            var activeDocument = getActiveDocument();
            if (activeDocument != null) {
                if (isCenteredSearchStateDocument(activeDocument)) {
                    drawCenteredSearchStateMessage(activeDocument);
                } else {
                    renderDocument(contentMouseX, contentMouseY);
                }
            } else {
                drawPageMissingMessage();
            }

            if (getMaxScroll() > 0) {
                drawScrollbar();
            }

            drawBottomBar();
        }

        drawRect(panelX, panelY, panelX + panelW, panelY + TOOLBAR_H, BG_COLOR);
        drawRect(panelX, panelY + TOOLBAR_H, panelX + panelW, panelY + TOOLBAR_H + 1, 0xFF2A2A2A);
        drawPageTitle();
        if (searchField != null) {
            drawSearchField();
        }

        super.drawScreen(contentMouseX, contentMouseY, partialTicks);
        navBar.render(mc, currentAnchor != null ? currentAnchor.pageId() : null, mouseX, mouseY, guide);
        if (isGuideEditorActive()) {
            drawGuideEditorContextMenu(mouseX, mouseY);
        }

        drawButtonTooltip(mouseX, mouseY);
    }

    private void drawBottomBar() {
        if (!hasBottomBar()) return;
        @Nullable
        FrontmatterPageMeta meta = currentPage.pageMeta();

        int barY = panelY + panelH - TOOLBAR_H;
        drawRect(panelX, barY, panelX + panelW, panelY + panelH, BG_COLOR);
        drawRect(panelX, barY, panelX + panelW, barY + 1, 0xFF2A2A2A);

        FontRenderer fr = mc.fontRenderer;
        if (cachedBottomBarText == null || cachedBottomBarPage != currentPage || cachedBottomBarWidth != this.width) {
            cachedBottomBarText = buildBottomBarText(meta);
            cachedBottomBarPage = currentPage;
            cachedBottomBarWidth = this.width;
        }
        String text = cachedBottomBarText;
        if (text.isEmpty()) return;

        int textRightX = panelX + panelW - 6 - 2;
        int textW = fr.getStringWidth(text);
        int textX = textRightX - textW;
        int textY = barY + (TOOLBAR_H - fr.FONT_HEIGHT) / 2 + 1;
        fr.drawString(text, textX, textY, 0xFFAAAAAA, false);
    }

    private void pollActiveSceneDrag() {
        if (activeScene != null) {
            activeScene.pollDrag();
        }
    }

    private void drawGuideEditorScreen(int mouseX, int mouseY) {
        ensureGuideEditorTextArea();
        updateGuideEditorTextFromArea();
        if (guideEditorPreviewDirty && System.currentTimeMillis() >= guideEditorNextPreviewCompileAtMillis) {
            rebuildGuideEditorPreview();
        }

        int toolbarY = panelY + TOOLBAR_H + 1;
        int toolbarBottom = layoutGuideEditorActionButtons(contentX, toolbarY, contentW);
        int editorTop = Math.max(toolbarBottom + 2, toolbarY + GUIDE_EDITOR_TOOLBAR_H + 2);
        guideEditorActionToolbarBottom = toolbarBottom;
        guideEditorEditorTop = editorTop;
        int editorBottom = panelY + panelH - PANEL_PADDING;
        int editorHeight = Math.max(20, editorBottom - editorTop);
        int editorAreaWidth = Math.max(20, contentW);

        int leftPaneWidth = resolveGuideEditorLeftPaneWidth(editorAreaWidth);
        int splitGap = guideEditorLayoutMode == GuideScreenEditorLayoutMode.SPLIT ? 1 : 0;
        int previewPaneX = contentX + leftPaneWidth + splitGap;
        int previewPaneWidth = Math.max(0, editorAreaWidth - leftPaneWidth - splitGap);
        updateGuideEditorPreviewHover(mouseX, mouseY);

        if (guideEditorLayoutMode != GuideScreenEditorLayoutMode.PREVIEW_ONLY && guideEditorTextArea != null) {
            guideEditorTextArea.setBounds(contentX, editorTop, Math.max(1, leftPaneWidth), editorHeight);
            guideEditorTextArea.draw(false);
        }

        if (guideEditorLayoutMode == GuideScreenEditorLayoutMode.SPLIT) {
            int dividerX = contentX + leftPaneWidth;
            drawRect(dividerX, editorTop, dividerX + 1, editorTop + editorHeight, resolveGuideEditorDividerColor());
        }

        if (guideEditorLayoutMode != GuideScreenEditorLayoutMode.EDITOR_ONLY && guideEditorPreviewPage != null) {
            renderGuideEditorPreview(previewPaneX, editorTop, previewPaneWidth, editorHeight);
        }

    }

    private int resolveGuideEditorDividerColor() {
        if (guideEditorDraggingDivider) {
            return 0xFF5EA8FF;
        }
        if (guideEditorDividerHoverStartedAtMillis <= 0L) {
            return 0xFF4A4A4A;
        }
        long elapsed = System.currentTimeMillis() - guideEditorDividerHoverStartedAtMillis;
        return elapsed >= GUIDE_EDITOR_DIVIDER_HOVER_DELAY_MILLIS ? 0xFF5EA8FF : 0xFF4A4A4A;
    }

    private void updateGuideEditorPreviewHover(int mouseX, int mouseY) {
        if (!isGuideEditorActive()) {
            return;
        }
        updateGuideEditorDividerHover(mouseX, mouseY);
        if (guideEditorLayoutMode == GuideScreenEditorLayoutMode.EDITOR_ONLY) {
            clearGuideEditorPreviewHover();
            return;
        }
        if (!isInsideGuideEditorPreview(mouseX, mouseY)) {
            clearGuideEditorPreviewHover();
            return;
        }
        var interaction = getGuideEditorPreviewInteractionState(mouseX, mouseY);
        LytDocument previewDocument = guideEditorPreviewPage != null ? guideEditorPreviewPage.document() : null;
        if (previewDocument != null) {
            previewDocument.setHoveredElement(interaction != null ? interaction.hit : null);
        }
        LytGuidebookScene scene = interaction != null ? interaction.scene : null;
        updateHoveredScene(scene, mouseX, mouseY);
    }

    private void updateGuideEditorDividerHover(int mouseX, int mouseY) {
        if (guideEditorLayoutMode != GuideScreenEditorLayoutMode.SPLIT) {
            guideEditorDividerHoverStartedAtMillis = 0L;
            return;
        }
        int dividerX = getGuideEditorDividerX();
        boolean hovering = Math.abs(mouseX - dividerX) <= 2 && mouseY >= getGuideEditorContentTop()
            && mouseY < getGuideEditorContentBottom();
        if (!hovering) {
            guideEditorDividerHoverStartedAtMillis = 0L;
        } else if (guideEditorDividerHoverStartedAtMillis <= 0L) {
            guideEditorDividerHoverStartedAtMillis = System.currentTimeMillis();
        }
    }

    private void clearGuideEditorPreviewHover() {
        if (guideEditorPreviewPage != null && guideEditorPreviewPage.document() != null) {
            guideEditorPreviewPage.document()
                .setHoveredElement(null);
        }
        if (hoveredScene != null && isGuideEditorActive()) {
            clearHoveredScene();
        }
    }

    private void drawGuideEditorContextMenu(int mouseX, int mouseY) {
        if (guideEditorContextMenu == null || !guideEditorContextMenu.isOpen()) {
            return;
        }
        guideEditorContextMenu.setViewport(this.width, this.height, fontRendererObj);
        guideEditorContextMenu.update(mouseX, mouseY, this.width, this.height, fontRendererObj);
        guideEditorContextMenu.draw(fontRendererObj, mouseX, mouseY);
    }

    private int layoutGuideEditorActionButtons(int startX, int startY, int availableWidth) {
        int x = startX;
        int y = startY;
        int rowHeight = GuideIconButton.HEIGHT + 2;
        int maxX = startX + Math.max(GuideIconButton.WIDTH, availableWidth);
        List<GuideScreenEditorAction> actions = getGuideEditorActionOrder();
        for (GuideScreenEditorAction action : actions) {
            int buttonId = 2000 + action.ordinal();
            GuideIconButton button = guideEditorActionButtons.get(buttonId);
            if (button == null) {
                continue;
            }
            if (x + GuideIconButton.WIDTH > maxX && x > startX) {
                x = startX;
                y += rowHeight;
            }
            button.xPosition = x;
            button.yPosition = y;
            button.visible = true;
            button.enabled = true;
            x += GuideIconButton.WIDTH + TOOLBAR_GAP;
        }
        return y + rowHeight;
    }

    private int resolveGuideEditorLeftPaneWidth(int editorAreaWidth) {
        if (guideEditorLayoutMode == GuideScreenEditorLayoutMode.PREVIEW_ONLY) {
            return 0;
        }
        if (guideEditorLayoutMode == GuideScreenEditorLayoutMode.EDITOR_ONLY) {
            return editorAreaWidth;
        }
        int divider = guideEditorDividerPercent;
        int leftPaneWidth = Math.round(editorAreaWidth * (divider / 100.0f));
        int maxLeft = Math.max(GUIDE_EDITOR_MIN_SPLIT_PANE_W, editorAreaWidth - GUIDE_EDITOR_MIN_SPLIT_PANE_W - 1);
        return Math.max(GUIDE_EDITOR_MIN_SPLIT_PANE_W, Math.min(maxLeft, leftPaneWidth));
    }

    private void renderGuideEditorPreview(int x, int y, int width, int height) {
        if (guideEditorPreviewPage == null) {
            return;
        }
        LytDocument previewDocument = guideEditorPreviewPage.document();
        if (previewDocument == null) {
            return;
        }
        int renderWidth = Math.max(1, width);
        int layoutWidth = Math.max(1, renderWidth - SCROLLBAR_W - 1);
        if (!previewDocument.hasLayout() || previewDocument.getAvailableWidth() != layoutWidth) {
            previewDocument.updateLayout(new LayoutContext(layoutFontMetrics), layoutWidth);
        }
        int renderHeight = Math.max(1, height);
        int maxScroll = Math.max(0, previewDocument.getContentHeight() - renderHeight);
        if (guideEditorPreviewScrollY > maxScroll) {
            guideEditorPreviewScrollY = maxScroll;
        }
        cachedPreviewViewport = cachedRect(
            cachedPreviewViewport,
            0,
            guideEditorPreviewScrollY,
            layoutWidth,
            renderHeight);
        cachedPreviewScissor = cachedRect(cachedPreviewScissor, x, y, renderWidth, renderHeight);
        reusableRenderCtx.setLightDarkMode(LightDarkMode.LIGHT_MODE);
        reusableRenderCtx.setViewport(cachedPreviewViewport);
        reusableRenderCtx.setScreenHeight(this.height);
        reusableRenderCtx.setDocumentOrigin(x, y);
        reusableRenderCtx.setScrollOffsetY(guideEditorPreviewScrollY);
        reusableRenderCtx.setZoom(1.0f);
        reusableRenderCtx.pushScissor(cachedPreviewScissor);
        GL11.glPushMatrix();
        GL11.glTranslatef(x, y, 0f);
        GL11.glTranslatef(0f, -(float) guideEditorPreviewScrollY, 0f);
        try {
            previewDocument.render(reusableRenderCtx);
        } catch (Throwable t) {
            FMLLog.warning("Failed to render guide editor preview", t);
        } finally {
            GL11.glPopMatrix();
            reusableRenderCtx.restoreExternalRenderState();
            reusableRenderCtx.popScissor();
            reusableRenderCtx.restoreExternalRenderState();
            GL11.glDisable(GL11.GL_SCISSOR_TEST);
            GL11.glEnable(GL11.GL_TEXTURE_2D);
            GL11.glColor4f(1f, 1f, 1f, 1f);
        }
        drawGuideEditorPreviewScrollbar(
            x + renderWidth - SCROLLBAR_W,
            y,
            renderHeight,
            previewDocument.getContentHeight());
    }

    private void drawGuideEditorPreviewScrollbar(int barX, int barY, int barH, int contentH) {
        if (barH <= 0 || contentH <= barH) {
            return;
        }
        int barW = SCROLLBAR_W;
        drawRect(barX, barY, barX + barW, barY + barH, 0x35101010);
        int thumbH = Math.max(16, (int) ((long) barH * barH / Math.max(1, contentH)));
        int maxScroll = Math.max(0, contentH - barH);
        int thumbY = maxScroll > 0 ? barY + (int) ((long) (barH - thumbH) * guideEditorPreviewScrollY / maxScroll)
            : barY;
        drawRect(barX, thumbY, barX + barW, thumbY + thumbH, 0xA0D8D8D8);
    }

    private boolean handleGuideEditorKey(char typedChar, int keyCode) {
        if (!isGuideEditorActive() || guideEditorTextArea == null) {
            return false;
        }
        if (guideEditorContextMenu != null && guideEditorContextMenu.isOpen()) {
            if (keyCode == Keyboard.KEY_ESCAPE) {
                closeGuideEditorContextMenu();
            }
            return true;
        }
        if (keyCode == Keyboard.KEY_ESCAPE && guideEditorTextArea.isFocused()) {
            guideEditorTextArea.setFocused(false);
            return true;
        }
        if ((keyCode == Keyboard.KEY_Z && GuiScreen.isShiftKeyDown())
            && (Keyboard.isKeyDown(Keyboard.KEY_LCONTROL) || Keyboard.isKeyDown(Keyboard.KEY_RCONTROL))) {
            performGuideEditorAction(GuideScreenEditorAction.REDO);
            return true;
        }
        if (keyCode == Keyboard.KEY_Y
            && (Keyboard.isKeyDown(Keyboard.KEY_LCONTROL) || Keyboard.isKeyDown(Keyboard.KEY_RCONTROL))) {
            performGuideEditorAction(GuideScreenEditorAction.REDO);
            return true;
        }
        if (keyCode == Keyboard.KEY_Z
            && (Keyboard.isKeyDown(Keyboard.KEY_LCONTROL) || Keyboard.isKeyDown(Keyboard.KEY_RCONTROL))) {
            performGuideEditorAction(GuideScreenEditorAction.UNDO);
            return true;
        }
        if (keyCode == Keyboard.KEY_S
            && (Keyboard.isKeyDown(Keyboard.KEY_LCONTROL) || Keyboard.isKeyDown(Keyboard.KEY_RCONTROL))) {
            saveGuideEditorDraft();
            return true;
        }
        if (!guideEditorTextArea.isFocused()) {
            return false;
        }
        final boolean[] handled = new boolean[1];
        runGuideEditorTextMutation(new Runnable() {

            @Override
            public void run() {
                handled[0] = guideEditorTextArea.keyTyped(typedChar, keyCode);
            }
        });
        if (handled[0]) {
            syncGuideEditorPreviewScrollFromEditor();
            return true;
        }
        return false;
    }

    private boolean handleGuideEditorMouseClicked(int mouseX, int mouseY, int button) {
        if (!isGuideEditorActive()) {
            return false;
        }
        if (guideEditorContextMenu != null && guideEditorContextMenu.isOpen()) {
            return guideEditorContextMenu
                .mouseClicked(mouseX, mouseY, button, new GuideScreenEditorContextMenu.Listener() {

                    @Override
                    public void onAction(GuideScreenEditorAction action) {
                        performGuideEditorAction(action);
                    }
                }, fontRendererObj, width, height);
        }
        if (!isInsideGuideEditorContent(mouseX, mouseY)) {
            if (guideEditorTextArea != null && !guideEditorTextArea.contains(mouseX, mouseY)
                && !isInsideGuideEditorToolbar(mouseX, mouseY)) {
                guideEditorTextArea.setFocused(false);
            }
            return false;
        }

        if (button == 1 && guideEditorTextArea != null && guideEditorTextArea.contains(mouseX, mouseY)) {
            guideEditorTextArea.setFocused(true);
            openGuideEditorContextMenu(mouseX, mouseY);
            return true;
        }

        if (guideEditorLayoutMode == GuideScreenEditorLayoutMode.SPLIT && button == 0) {
            int dividerX = getGuideEditorDividerX();
            if (Math.abs(mouseX - dividerX) <= 2) {
                guideEditorDraggingDivider = true;
                guideEditorDividerGrabOffset = mouseX - dividerX;
                return true;
            }
        }

        if (button == 0 && isInsideGuideEditorPreviewScrollbar(mouseX, mouseY)) {
            if (guideEditorTextArea != null) {
                guideEditorTextArea.setFocused(false);
            }
            guideEditorDraggingPreviewScrollbar = true;
            int barY = getGuideEditorContentTop();
            int barH = Math.max(1, getGuideEditorPreviewPaneHeight());
            int contentH = guideEditorPreviewPage.document()
                .getContentHeight();
            int thumbH = Math.max(16, (int) ((long) barH * barH / Math.max(1, contentH)));
            int maxScroll = Math.max(0, contentH - barH);
            int thumbY = maxScroll > 0 ? barY + (int) ((long) (barH - thumbH) * guideEditorPreviewScrollY / maxScroll)
                : barY;
            guideEditorPreviewScrollbarGrabOffset = mouseY - thumbY;
            updateGuideEditorPreviewScrollFromMouse(mouseY);
            syncGuideEditorEditorScrollFromPreview();
            return true;
        }

        if (guideEditorLayoutMode == GuideScreenEditorLayoutMode.PREVIEW_ONLY) {
            handleGuideEditorPreviewClick(mouseX, mouseY, button);
            return true;
        }

        if (guideEditorTextArea != null && guideEditorTextArea.contains(mouseX, mouseY)) {
            guideEditorTextArea.setFocused(true);
            boolean handled = guideEditorTextArea.mouseClicked(mouseX, mouseY, button);
            updateGuideEditorTextFromArea();
            syncGuideEditorPreviewScrollFromEditor();
            return handled;
        }

        if (isInsideGuideEditorPreview(mouseX, mouseY)) {
            handleGuideEditorPreviewClick(mouseX, mouseY, button);
            return true;
        }

        return true;
    }

    private boolean handleGuideEditorPreviewClick(int mouseX, int mouseY, int button) {
        if (guideEditorTextArea != null) {
            guideEditorTextArea.setFocused(false);
        }
        var interaction = getGuideEditorPreviewInteractionState(mouseX, mouseY);
        if (interaction == null || interaction.document == null || interaction.hit == null) {
            return true;
        }
        if (button == 0) {
            var sceneButtonHit = interaction.sceneButtonHit;
            if (sceneButtonHit != null) {
                sceneButtonHit.scene.activateSceneButton(sceneButtonHit.role);
                mc.getSoundHandler()
                    .playSound(PositionedSoundRecord.func_147674_a(new ResourceLocation("gui.button.press"), 1.0F));
                return true;
            }
        }
        boolean handled = activateDocumentInteraction(interaction, button);
        if (handled) {
            mc.getSoundHandler()
                .playSound(PositionedSoundRecord.func_147674_a(new ResourceLocation("guidenh:guide_click"), 1.0F));
        }
        return true;
    }

    private boolean isInsideGuideEditorToolbar(int mouseX, int mouseY) {
        if (!isGuideEditorActive()) {
            return false;
        }
        if (mouseX < panelX || mouseX >= panelX + panelW) {
            return false;
        }
        int topToolbarBottom = panelY + TOOLBAR_H + 1;
        int editorToolbarBottom = guideEditorActionToolbarBottom > 0 ? guideEditorActionToolbarBottom
            : topToolbarBottom + GUIDE_EDITOR_TOOLBAR_H + 2;
        return mouseY >= panelY && mouseY <= editorToolbarBottom;
    }

    private boolean handleGuideEditorMouseDragged(int mouseX, int mouseY, int button) {
        if (!isGuideEditorActive()) {
            return false;
        }
        if (guideEditorContextMenu != null && guideEditorContextMenu.isOpen()) {
            if (guideEditorContextMenu.mouseDragged(mouseX, mouseY, button, this.width, this.height, fontRendererObj)) {
                return true;
            }
            return true;
        }
        if (guideEditorDraggingDivider) {
            updateGuideEditorDividerFromMouse(mouseX);
            return true;
        }
        if (activeDocumentDragTarget != null) {
            int[] docPoint = screenToActiveDocumentPoint(mouseX, mouseY);
            activeDocumentDragTarget.dragTo(docPoint[0], docPoint[1]);
            return true;
        }
        if (activeScene != null) {
            activeScene.drag(mouseX, mouseY);
            return true;
        }
        if (guideEditorDraggingPreviewScrollbar) {
            updateGuideEditorPreviewScrollFromMouse(mouseY);
            syncGuideEditorEditorScrollFromPreview();
            return true;
        }
        if (guideEditorLayoutMode != GuideScreenEditorLayoutMode.PREVIEW_ONLY && guideEditorTextArea != null
            && guideEditorTextArea.mouseDragged(mouseX, mouseY, button)) {
            updateGuideEditorTextFromArea();
            syncGuideEditorPreviewScrollFromEditor();
            return true;
        }
        return false;
    }

    private boolean handleGuideEditorMouseReleased(int mouseX, int mouseY, int state) {
        if (!isGuideEditorActive()) {
            return false;
        }
        if (guideEditorContextMenu != null && guideEditorContextMenu.isOpen()) {
            guideEditorContextMenu.mouseReleased(state);
            return true;
        }
        if (guideEditorDraggingDivider && state != -1) {
            guideEditorDraggingDivider = false;
            GuideScreenEditorState.setDividerPercent(guideEditorDividerPercent);
            return true;
        }
        if (guideEditorDraggingPreviewScrollbar && state != -1) {
            guideEditorDraggingPreviewScrollbar = false;
            return true;
        }
        if (activeDocumentDragTarget != null && state != -1) {
            activeDocumentDragTarget.endDrag();
            activeDocumentDragTarget = null;
            return true;
        }
        if (activeScene != null && state != -1) {
            activeScene.endDrag();
            activeScene = null;
            return true;
        }
        if (guideEditorTextArea != null) {
            guideEditorTextArea.mouseReleased(state);
        }
        return false;
    }

    private boolean handleGuideEditorWheel(int mouseX, int mouseY, int dwheel) {
        if (!isGuideEditorActive()) {
            return false;
        }
        if (guideEditorContextMenu != null && guideEditorContextMenu.isOpen()) {
            guideEditorContextMenu.scrollWheel(mouseX, mouseY, dwheel, this.width, this.height, fontRendererObj);
            return true;
        }
        if (guideEditorLayoutMode != GuideScreenEditorLayoutMode.PREVIEW_ONLY && guideEditorTextArea != null
            && guideEditorTextArea.contains(mouseX, mouseY)) {
            guideEditorTextArea.scrollWheel(dwheel);
            updateGuideEditorTextFromArea();
            syncGuideEditorPreviewScrollFromEditor();
            return true;
        }
        if (isInsideGuideEditorPreview(mouseX, mouseY)) {
            scrollGuideEditorPreview(dwheel);
            syncGuideEditorEditorScrollFromPreview();
            return true;
        }
        return false;
    }

    private void updateGuideEditorDividerFromMouse(int mouseX) {
        if (guideEditorLayoutMode != GuideScreenEditorLayoutMode.SPLIT) {
            return;
        }
        int editorWidth = Math.max(1, contentW);
        int dividerX = mouseX - contentX - guideEditorDividerGrabOffset;
        int minLeft = GUIDE_EDITOR_MIN_SPLIT_PANE_W;
        int maxLeft = Math.max(minLeft, editorWidth - GUIDE_EDITOR_MIN_SPLIT_PANE_W - 1);
        int leftWidth = Math.max(minLeft, Math.min(maxLeft, dividerX));
        guideEditorDividerPercent = Math.round(leftWidth * 100.0f / editorWidth);
    }

    private void scrollGuideEditorPreview(int dwheel) {
        if (guideEditorPreviewPage == null || guideEditorPreviewPage.document() == null) {
            return;
        }
        int pageHeight = guideEditorPreviewPage.document()
            .getContentHeight();
        int viewportHeight = Math.max(1, getGuideEditorPreviewPaneHeight());
        int maxScroll = Math.max(0, pageHeight - viewportHeight);
        int step = GuiScreen.isShiftKeyDown() ? 60 : 20;
        guideEditorPreviewScrollY -= Integer.signum(dwheel) * step;
        if (guideEditorPreviewScrollY < 0) {
            guideEditorPreviewScrollY = 0;
        }
        if (guideEditorPreviewScrollY > maxScroll) {
            guideEditorPreviewScrollY = maxScroll;
        }
    }

    @Nullable
    private DocumentInteractionState getGuideEditorPreviewInteractionState(int mouseX, int mouseY) {
        if (guideEditorLayoutMode == GuideScreenEditorLayoutMode.EDITOR_ONLY || guideEditorPreviewPage == null
            || guideEditorPreviewPage.document() == null
            || !isInsideGuideEditorPreview(mouseX, mouseY)) {
            cachedGuideEditorPreviewInteractionState = null;
            return null;
        }
        LytDocument previewDocument = guideEditorPreviewPage.document();
        if (!previewDocument.hasLayout()) {
            cachedGuideEditorPreviewInteractionState = null;
            return null;
        }
        int previewX = getGuideEditorPreviewX();
        int previewY = getGuideEditorContentTop();
        int previewW = getGuideEditorPreviewPaneWidth();
        int previewH = getGuideEditorPreviewPaneHeight();
        DocumentInteractionState interaction = cachedGuideEditorPreviewInteractionState;
        if (interaction != null && interaction.matches(
            previewDocument,
            mouseX,
            mouseY,
            previewX,
            previewY,
            previewW,
            previewH,
            guideEditorPreviewScrollY)) {
            return interaction;
        }

        int docX = mouseX - previewX;
        int docY = mouseY - previewY + guideEditorPreviewScrollY;
        var hit = previewDocument.pick(docX, docY);
        var scene = hit != null ? findSceneAncestor(hit.node()) : null;
        SceneButtonHit sceneButtonHit = null;
        if (scene != null) {
            var role = scene.sceneButtonAt(mouseX, mouseY);
            if (role != null) {
                sceneButtonHit = new SceneButtonHit(scene, role);
            }
        } else {
            sceneButtonHit = findSceneButtonHit(previewDocument, mouseX, mouseY);
            if (sceneButtonHit != null) {
                scene = sceneButtonHit.scene;
            }
        }
        if (scene != null && !scene.containsSceneInteractiveTarget(mouseX, mouseY)) {
            scene = null;
        }
        interaction = new DocumentInteractionState(
            previewDocument,
            mouseX,
            mouseY,
            previewX,
            previewY,
            previewW,
            previewH,
            guideEditorPreviewScrollY,
            previewX,
            previewY,
            previewW,
            previewH,
            docX,
            docY,
            hit,
            scene,
            sceneButtonHit);
        cachedGuideEditorPreviewInteractionState = interaction;
        return interaction;
    }

    private void syncGuideEditorPreviewScrollFromEditor() {
        if (guideEditorTextArea == null || guideEditorPreviewPage == null
            || guideEditorPreviewPage.document() == null) {
            return;
        }
        int pageHeight = guideEditorPreviewPage.document()
            .getContentHeight();
        int viewportHeight = Math.max(1, getGuideEditorPreviewPaneHeight());
        int maxScroll = Math.max(0, pageHeight - viewportHeight);
        if (maxScroll <= 0) {
            guideEditorPreviewScrollY = 0;
            return;
        }
        float fraction = guideEditorTextArea.getVerticalScrollFraction();
        guideEditorPreviewScrollY = Math.max(0, Math.min(maxScroll, Math.round(maxScroll * fraction)));
    }

    private void syncGuideEditorEditorScrollFromPreview() {
        if (guideEditorTextArea == null || guideEditorPreviewPage == null
            || guideEditorPreviewPage.document() == null) {
            return;
        }
        int pageHeight = guideEditorPreviewPage.document()
            .getContentHeight();
        int viewportHeight = Math.max(1, getGuideEditorPreviewPaneHeight());
        int maxScroll = Math.max(0, pageHeight - viewportHeight);
        float fraction = maxScroll <= 0 ? 0f : guideEditorPreviewScrollY / (float) maxScroll;
        guideEditorTextArea.setVerticalScrollFraction(fraction);
    }

    private boolean isInsideGuideEditorPreviewScrollbar(int mouseX, int mouseY) {
        if (guideEditorLayoutMode == GuideScreenEditorLayoutMode.EDITOR_ONLY || guideEditorPreviewPage == null
            || guideEditorPreviewPage.document() == null) {
            return false;
        }
        int previewX = getGuideEditorPreviewX();
        int previewY = getGuideEditorContentTop();
        int previewW = getGuideEditorPreviewPaneWidth();
        int previewH = getGuideEditorPreviewPaneHeight();
        if (previewW <= 0 || previewH <= 0) {
            return false;
        }
        int contentH = guideEditorPreviewPage.document()
            .getContentHeight();
        if (contentH <= previewH) {
            return false;
        }
        int barX = previewX + previewW - SCROLLBAR_W;
        return mouseX >= barX && mouseX < barX + SCROLLBAR_W && mouseY >= previewY && mouseY < previewY + previewH;
    }

    private void updateGuideEditorPreviewScrollFromMouse(int mouseY) {
        if (guideEditorPreviewPage == null || guideEditorPreviewPage.document() == null) {
            return;
        }
        int pageHeight = guideEditorPreviewPage.document()
            .getContentHeight();
        int viewportHeight = Math.max(1, getGuideEditorPreviewPaneHeight());
        int maxScroll = Math.max(0, pageHeight - viewportHeight);
        if (maxScroll <= 0) {
            guideEditorPreviewScrollY = 0;
            return;
        }
        int barY = getGuideEditorContentTop();
        int barH = viewportHeight;
        int thumbH = Math.max(16, (int) ((long) barH * barH / Math.max(1, pageHeight)));
        int track = Math.max(1, barH - thumbH);
        int targetThumbY = mouseY - guideEditorPreviewScrollbarGrabOffset;
        int rel = targetThumbY - barY;
        if (rel < 0) {
            rel = 0;
        }
        if (rel > track) {
            rel = track;
        }
        guideEditorPreviewScrollY = (int) ((long) rel * maxScroll / track);
    }

    private boolean isInsideGuideEditorContent(int mouseX, int mouseY) {
        return mouseX >= contentX && mouseX < contentX + contentW
            && mouseY >= getGuideEditorContentTop()
            && mouseY < getGuideEditorContentBottom();
    }

    private boolean isInsideGuideEditorPreview(int mouseX, int mouseY) {
        if (guideEditorLayoutMode == GuideScreenEditorLayoutMode.EDITOR_ONLY) {
            return false;
        }
        int previewX = getGuideEditorPreviewX();
        int previewY = getGuideEditorContentTop();
        int previewW = getGuideEditorPreviewPaneWidth();
        int previewH = getGuideEditorPreviewPaneHeight();
        return mouseX >= previewX && mouseX < previewX + previewW && mouseY >= previewY && mouseY < previewY + previewH;
    }

    private int getGuideEditorContentTop() {
        return guideEditorEditorTop > 0 ? guideEditorEditorTop : panelY + TOOLBAR_H + 1 + GUIDE_EDITOR_TOOLBAR_H + 2;
    }

    private int getGuideEditorContentBottom() {
        return panelY + panelH - PANEL_PADDING;
    }

    private int getGuideEditorPreviewX() {
        int editorAreaWidth = Math.max(20, contentW);
        int leftPaneWidth = resolveGuideEditorLeftPaneWidth(editorAreaWidth);
        return contentX + leftPaneWidth + (guideEditorLayoutMode == GuideScreenEditorLayoutMode.SPLIT ? 1 : 0);
    }

    private int getGuideEditorPreviewPaneWidth() {
        int editorAreaWidth = Math.max(20, contentW);
        int leftPaneWidth = resolveGuideEditorLeftPaneWidth(editorAreaWidth);
        return Math.max(
            0,
            editorAreaWidth - leftPaneWidth - (guideEditorLayoutMode == GuideScreenEditorLayoutMode.SPLIT ? 1 : 0));
    }

    private int getGuideEditorPreviewPaneHeight() {
        return Math.max(20, getGuideEditorContentBottom() - getGuideEditorContentTop());
    }

    private int getGuideEditorDividerX() {
        int editorAreaWidth = Math.max(20, contentW);
        int leftPaneWidth = resolveGuideEditorLeftPaneWidth(editorAreaWidth);
        return contentX + leftPaneWidth;
    }

    private String buildBottomBarText(@Nullable FrontmatterPageMeta meta) {
        FontRenderer fr = mc.fontRenderer;
        int maxW = (int) (this.width * 0.8);

        String sourceDisplay = getSourceDisplayName(currentPage.sourcePack());
        List<String> authors = meta != null ? meta.authors() : Collections.emptyList();
        String dateVal = meta != null ? meta.date() : null;
        String updatedVal = meta != null ? meta.updated() : null;
        String authorsStr = buildAuthorsString(authors);

        // Width calculations use raw (non-italic) text so getStringWidth() is accurate
        String rawAuthorPart = authorsStr.isEmpty() ? "" : GuidebookText.PageMetaAuthor.text(authorsStr);
        String rawDatePart = dateVal != null ? GuidebookText.PageMetaDate.text(dateVal) : "";
        String rawUpdatedPart = updatedVal != null ? GuidebookText.PageMetaUpdated.text(updatedVal) : "";
        String rawFull = GuidebookText.PageMetaContentFrom.text(sourceDisplay) + rawAuthorPart
            + rawDatePart
            + rawUpdatedPart;

        if (fr.getStringWidth(rawFull) <= maxW) {
            return formatBottomBar(sourceDisplay, authorsStr, dateVal, updatedVal);
        }

        int prefixW = fr.getStringWidth(GuidebookText.PageMetaContentFrom.text(""));
        int ellipsisW = fr.getStringWidth("...");
        int rawSuffixW = fr.getStringWidth(rawAuthorPart + rawDatePart + rawUpdatedPart);
        int availableForSource = maxW - prefixW - rawSuffixW - ellipsisW;
        if (availableForSource > 0) {
            String truncSrc = truncateStringToWidth(fr, sourceDisplay, availableForSource);
            String attempt = GuidebookText.PageMetaContentFrom.text(truncSrc + "...") + rawAuthorPart
                + rawDatePart
                + rawUpdatedPart;
            if (fr.getStringWidth(attempt) <= maxW) {
                return formatBottomBar(truncSrc + "...", authorsStr, dateVal, updatedVal);
            }
        }

        if (!authors.isEmpty()) {
            String firstAuthor = authors.get(0);
            int authorPrefixW = fr.getStringWidth(GuidebookText.PageMetaAuthor.text(""));
            int availableForAuthor = maxW - prefixW
                - ellipsisW
                - authorPrefixW
                - ellipsisW
                - fr.getStringWidth(rawDatePart + rawUpdatedPart);
            String truncSrc2 = truncateStringToWidth(
                fr,
                sourceDisplay,
                availableForAuthor > 0 ? availableForAuthor : fr.getStringWidth(sourceDisplay));
            String rawSingleAuthorPart = GuidebookText.PageMetaAuthor.text(firstAuthor);
            String attempt2 = GuidebookText.PageMetaContentFrom.text(truncSrc2 + "...") + rawSingleAuthorPart
                + rawDatePart
                + rawUpdatedPart;
            if (fr.getStringWidth(attempt2) <= maxW) {
                return formatBottomBar(truncSrc2 + "...", firstAuthor, dateVal, updatedVal);
            }
        }

        return truncateStringToWidth(fr, rawFull, maxW);
    }

    /**
     * Builds the bottom-bar string with §o italic formatting around each placeholder value.
     * §r resets to white; §7 restores dark gray (#AAAAAA) to match the draw color.
     */
    private static String formatBottomBar(String sourceDisplay, String authorsStr, @Nullable String dateVal,
        @Nullable String updatedVal) {
        String authorPart = authorsStr.isEmpty() ? ""
            : GuidebookText.PageMetaAuthor.text("\u00A7o" + authorsStr + "\u00A7r\u00A77");
        String datePart = dateVal != null ? GuidebookText.PageMetaDate.text("\u00A7o" + dateVal + "\u00A7r\u00A77")
            : "";
        String updatedPart = updatedVal != null
            ? GuidebookText.PageMetaUpdated.text("\u00A7o" + updatedVal + "\u00A7r\u00A77")
            : "";
        return GuidebookText.PageMetaContentFrom.text("\u00A7o" + sourceDisplay + "\u00A7r\u00A77") + authorPart
            + datePart
            + updatedPart;
    }

    private static String buildAuthorsString(List<String> authors) {
        if (authors.isEmpty()) return "";
        if (authors.size() == 1) return authors.get(0);
        if (authors.size() == 2) return authors.get(0) + ", " + authors.get(1);
        return authors.get(0) + ", " + authors.get(1) + "...";
    }

    private static String truncateStringToWidth(FontRenderer fr, String text, int maxW) {
        if (fr.getStringWidth(text) <= maxW) return text;
        int accWidth = 0;
        for (int i = 0; i < text.length(); i++) {
            int cw = fr.getCharWidth(text.charAt(i));
            if (accWidth + cw > maxW) {
                return text.substring(0, i);
            }
            accWidth += cw;
        }
        return text;
    }

    private static String getSourceDisplayName(String sourcePack) {
        int colon = sourcePack.indexOf(':');
        String prefix = colon >= 0 ? sourcePack.substring(0, colon) : "";
        String namespace = colon >= 0 ? sourcePack.substring(colon + 1) : sourcePack;

        if ("resources".equals(prefix)) {
            try {
                var entries = Minecraft.getMinecraft()
                    .getResourcePackRepository()
                    .getRepositoryEntries();
                for (int i = entries.size() - 1; i >= 0; i--) {
                    var pack = entries.get(i)
                        .getResourcePack();
                    if (pack != null && pack.getResourceDomains()
                        .contains(namespace)) {
                        String packName = pack.getPackName();
                        if (packName.length() > 4 && packName.substring(packName.length() - 4)
                            .equalsIgnoreCase(".zip")) {
                            packName = packName.substring(0, packName.length() - 4);
                        }
                        return packName;
                    }
                }
            } catch (Throwable ignored) {}
        }

        // For "development:" or no matching user resource pack: fall back to FML mod name
        try {
            var mod = Loader.instance()
                .getIndexedModList()
                .get(namespace);
            if (mod != null) {
                return mod.getName();
            }
        } catch (Throwable ignored) {}
        return namespace;
    }

    private void drawPageTitle() {
        if (currentAnchor == null || isSearchPage()) return;
        if (pageTitle.isEmpty()) return;

        int reservedRight = (16 + TOOLBAR_GAP) * 5 + PANEL_PADDING + 4;
        int availableW = Math.max(20, panelW - PANEL_PADDING - reservedRight);
        int titleX = panelX + PANEL_PADDING;

        // Single-pass layout: position is applied via GL translate, not layout coordinates.
        // Re-layout only when available width changes; avoids LayoutContext allocation each frame.
        if (availableW != cachedTitleLayoutWidth) {
            var layoutCtx = new LayoutContext(layoutFontMetrics);
            pageTitle.layout(layoutCtx, 0, 0, availableW);
            cachedTitleLayoutWidth = availableW;
        }
        int titleH = pageTitle.getBounds()
            .height();
        int titleY = Math.max(0, (TOOLBAR_H - titleH) / 2) + panelY + 2;

        var ctx = reusableContentTooltipCtx;
        cachedTitleViewport = cachedRect(cachedTitleViewport, 0, 0, availableW, Math.max(titleH, TOOLBAR_H));
        ctx.setLightDarkMode(LightDarkMode.LIGHT_MODE);
        ctx.setViewport(cachedTitleViewport);
        ctx.setScreenHeight(this.height);
        ctx.setDocumentOrigin(titleX, titleY);
        ctx.setScrollOffsetY(0);
        GL11.glPushMatrix();
        GL11.glTranslatef(titleX, titleY, 0f);
        try {
            pageTitle.render(ctx);
        } finally {
            GL11.glPopMatrix();
            ctx.restoreExternalRenderState();
            GL11.glDisable(GL11.GL_SCISSOR_TEST);
            GL11.glEnable(GL11.GL_TEXTURE_2D);
            GL11.glColor4f(1f, 1f, 1f, 1f);
        }
    }

    private void drawButtonTooltip(int mouseX, int mouseY) {
        if (guideEditorContextMenu != null && guideEditorContextMenu.isOpen()) {
            String menuTooltip = guideEditorContextMenu.getHoveredTooltip();
            if (menuTooltip != null) {
                drawTooltipText(menuTooltip, mouseX, mouseY);
            }
            return;
        }
        if (navBar.isOpen() && navBar.contains(mouseX, mouseY)) return;
        for (var b : this.buttonList) {
            if (b instanceof GuideIconButton icon && icon.visible
                && mouseX >= icon.xPosition
                && mouseY >= icon.yPosition
                && mouseX < icon.xPosition + icon.width
                && mouseY < icon.yPosition + icon.height) {
                drawTooltipText(icon.getTooltip(), mouseX, mouseY);
                return;
            }
        }
        var interaction = getActiveInteractionState(mouseX, mouseY);
        var sceneButtonHit = interaction != null ? interaction.sceneButtonHit : null;
        if (sceneButtonHit != null) {
            drawTooltipText(sceneButtonHit.role.tooltip(), mouseX, mouseY, interaction);
            return;
        }
        if (interaction != null) {
            drawDocumentHoverTooltip(interaction, mouseX, mouseY);
        }
    }

    private void drawDocumentHoverTooltip(DocumentInteractionState interaction, int mouseX, int mouseY) {
        var hit = interaction.hit;
        if (hit == null) return;

        var scene = interaction.scene;
        if (scene != null) {
            GuideTooltip blockStatsTooltip = scene.createBlockStatsTooltip(mouseX, mouseY);
            if (blockStatsTooltip != null) {
                renderGuideTooltip(blockStatsTooltip, mouseX, mouseY, interaction);
                return;
            }
            // Annotation tooltips have absolute priority: when an annotation is hovered we
            // MUST short-circuit the rest of the scene-tooltip cascade so the underlying block
            // (whose hover detection uses screen-space AABBs and may overlap an annotation that
            // sits inside a block) cannot steal the tooltip slot. Even if the annotation has no
            // tooltip content of its own, we still want to suppress the block tooltip below.
            boolean anyAnnotationHovered = false;
            for (var a : scene.getAnnotations()) {
                if (a.isHovered()) {
                    anyAnnotationHovered = true;
                    if (a.getTooltip() != null) {
                        renderGuideTooltip(a.getTooltip(), mouseX, mouseY, interaction);
                        return;
                    }
                }
            }
            if (anyAnnotationHovered) {
                return;
            }
            var hoveredHatch = scene.getHoveredStructureLibHatch();
            if (hoveredHatch != null) {
                GuideTooltip tooltip = resolveSceneBlockTooltip(
                    scene,
                    hoveredHatch[0],
                    hoveredHatch[1],
                    hoveredHatch[2]);
                if (tooltip != null) {
                    renderGuideTooltip(tooltip, mouseX, mouseY, interaction);
                    return;
                }
            }
            var hb = scene.getHoveredBlock();
            var hoveredEntity = scene.getHoveredEntity();
            if (hoveredEntity != null) {
                String name = GuideEntityDisplayResolver.resolveDisplayName(hoveredEntity);
                if (name != null) {
                    drawTooltipText(name, mouseX, mouseY, interaction);
                    return;
                }
            }
            if (hb != null) {
                GuideTooltip tooltip = resolveSceneBlockTooltip(scene, hb[0], hb[1], hb[2]);
                if (tooltip != null) {
                    renderGuideTooltip(tooltip, mouseX, mouseY, interaction);
                    if (ModConfig.debug.enableDebugMode) {
                        drawDebugBlockCoordTooltip(hb, mouseX, mouseY);
                    }
                    return;
                }
            }
        }

        var fc = hit.content();
        while (fc != null) {
            var tip = tryGetTooltip(fc, interaction.docX, interaction.docY);
            if (tip.isPresent()) {
                renderGuideTooltip(tip.get(), mouseX, mouseY, interaction);
                return;
            }
            fc = fc.getFlowParent();
        }
        if (hit.node() != null) {
            for (LytNode current = hit.node(); current != null; current = current.getParent()) {
                var tip = tryGetTooltip(current, interaction.docX, interaction.docY);
                if (tip.isPresent()) {
                    renderGuideTooltip(tip.get(), mouseX, mouseY, interaction);
                    return;
                }
            }
        }
    }

    public static Optional<GuideTooltip> tryGetTooltip(Object obj, int x, int y) {
        try {
            if (obj instanceof InteractiveElement ie) {
                var t = ie.getTooltip(x, y);
                if (t.isPresent()) return t;
            }
            if (obj instanceof LytSlot slot) {
                return slot.getTooltip(x, y);
            }
        } catch (Throwable ignored) {}
        return Optional.empty();
    }

    private void renderGuideTooltip(GuideTooltip tooltip, int mouseX, int mouseY,
        @Nullable DocumentInteractionState interaction) {
        if (tooltip instanceof ItemTooltip it) {
            hoveredItemStack = it.getStack();
            renderItemTooltip(it, mouseX, mouseY, interaction);
            return;
        }
        if (tooltip instanceof TextTooltip tt) {
            drawTooltipText(tt.getText(), mouseX, mouseY, interaction);
            return;
        }
        if (tooltip instanceof ContentTooltip ct) {
            drawContentTooltip(ct, mouseX, mouseY, interaction);
        }
    }

    private void renderItemTooltip(ItemTooltip tooltip, int mouseX, int mouseY,
        @Nullable DocumentInteractionState interaction) {
        ItemStack stack = tooltip.getStack();
        if (stack == null) {
            return;
        }

        List<String> lines = GuideItemTooltipLines.build(tooltip, mc);
        FontRenderer font = GuideItemTooltipRenderSupport.resolveFont(stack, mc.fontRenderer);
        drawHoveringTextAtAdjustedPosition(lines, mouseX, mouseY, font, resolveTooltipBounds(interaction));
    }

    @Nullable
    private GuideTooltip resolveSceneBlockTooltip(LytGuidebookScene scene, int x, int y, int z) {
        String name = blockDisplayName(scene, x, y, z);
        if (name == null) {
            return null;
        }

        GuideTooltip structureLibTooltip = scene.createStructureLibTooltipForHoveredBlock(name, isShiftDown());
        if (structureLibTooltip != null && isShiftDown()) {
            return structureLibTooltip;
        }

        ItemStack stack = blockDisplayStack(scene, x, y, z);
        if (stack != null && stack.stackSize > 0) {
            return new ItemTooltip(stack);
        }

        if (structureLibTooltip != null) {
            return structureLibTooltip;
        }
        return new TextTooltip(name);
    }

    private void drawContentTooltip(ContentTooltip ct, int mouseX, int mouseY,
        @Nullable DocumentInteractionState interaction) {
        int pad = 4;
        LytRect bounds = resolveTooltipBounds(interaction);
        int maxW = Math.max(80, (bounds.width() * 4) / 5);
        var box = ct.layout(maxW);
        int w = box.width();
        int h = box.height();
        int x = mouseX + 12;
        int y = mouseY - 12;
        if (x + w + pad > bounds.right()) x = mouseX - w - 12;
        if (x - pad < bounds.x()) x = bounds.x() + pad;
        if (y + h + pad > bounds.bottom()) y = bounds.bottom() - h - pad;
        if (y - pad < bounds.y()) y = bounds.y() + pad;

        GL11.glDisable(GL11.GL_DEPTH_TEST);
        this.zLevel = 300.0F;
        this.itemRender.zLevel = 300.0F;
        int ctBgColor = 0xF0100010;
        int ctBorderTop = 0x505000FF;
        int ctBorderBottom = 0x5028007F;
        drawGradientRect(x - pad, y - pad, x + w + pad, y + h + pad, ctBgColor, ctBgColor);
        drawGradientRect(x - pad, y - pad - 1, x + w + pad, y - pad, ctBgColor, ctBgColor);
        drawGradientRect(x - pad, y + h + pad, x + w + pad, y + h + pad + 1, ctBgColor, ctBgColor);
        drawGradientRect(x - pad - 1, y - pad, x - pad, y + h + pad, ctBgColor, ctBgColor);
        drawGradientRect(x + w + pad, y - pad, x + w + pad + 1, y + h + pad, ctBgColor, ctBgColor);
        drawGradientRect(x - pad, y - pad, x + w + pad, y - pad + 1, ctBorderTop, ctBorderTop);
        drawGradientRect(x - pad, y + h + pad - 1, x + w + pad, y + h + pad, ctBorderBottom, ctBorderBottom);
        drawGradientRect(x - pad, y - pad + 1, x - pad + 1, y + h + pad - 1, ctBorderTop, ctBorderBottom);
        drawGradientRect(x + w + pad - 1, y - pad + 1, x + w + pad, y + h + pad - 1, ctBorderTop, ctBorderBottom);

        var ctx = reusableContentTooltipCtx;
        cachedContentTooltipViewport = cachedRect(cachedContentTooltipViewport, 0, 0, w, h);
        ctx.setLightDarkMode(LightDarkMode.LIGHT_MODE);
        ctx.setViewport(cachedContentTooltipViewport);
        ctx.setScreenHeight(this.height);
        ctx.setDocumentOrigin(x, y);
        ctx.setScrollOffsetY(0);
        GL11.glPushMatrix();
        GL11.glTranslatef(x, y, 300f);
        try {
            ct.getContent()
                .render(ctx);
        } catch (Throwable t) {
            FMLLog.warning("Error rendering ContentTooltip", t);
        } finally {
            GL11.glPopMatrix();
            ctx.restoreExternalRenderState();
            this.zLevel = 0.0F;
            this.itemRender.zLevel = 0.0F;
            GL11.glEnable(GL11.GL_DEPTH_TEST);
        }
    }

    /**
     * Renders a small coordinate tooltip above the block tooltip when debug mode is active.
     * Uses magnetic snapping: if there is not enough space above the cursor, the tooltip
     * moves to below the cursor area instead.
     */
    private void drawDebugBlockCoordTooltip(int[] pos, int mouseX, int mouseY) {
        FontRenderer fr = mc.fontRenderer;
        String coordText = pos[0] + ", " + pos[1] + ", " + pos[2];
        // §6 = gold color; the coordinate tooltip renders above the main block tooltip.
        // drawHoveringText(list, x, y, font) draws starting at (x+12, y-12).
        // We want the debug tooltip to appear above the default position (mouseY - 12).
        // Targeting 26px above the cursor leaves room above the typical single-line tooltip.
        int debugY = mouseY - 26;
        // Snap to below cursor when the tooltip would be cut off at the top of the screen.
        // The tooltip box top is at debugY - 12 - 4 = debugY - 16.
        if (debugY - 16 < 0) {
            debugY = mouseY + 26;
        }
        // Clamp bottom edge as well.
        int lineH = fr.FONT_HEIGHT + 8;
        if (debugY + lineH > this.height) {
            debugY = this.height - lineH;
        }
        drawHoveringText(Collections.singletonList("\u00a76" + coordText), mouseX, debugY, fr);
    }

    private void drawTooltipText(String text, int mouseX, int mouseY) {
        drawTooltipText(text, mouseX, mouseY, null);
    }

    private void drawTooltipText(String text, int mouseX, int mouseY, @Nullable DocumentInteractionState interaction) {
        FontRenderer fr = mc.fontRenderer;
        String norm = (text.indexOf('\\') >= 0) ? text.replace("\\n", "\n") : text;
        LytRect bounds = resolveTooltipBounds(interaction);
        int hardMaxWidth = Math.max(40, bounds.width() - 24);
        int preferredWrapWidth = Math.max(80, bounds.width() / 2);
        int wrapWidth = Math.min(hardMaxWidth, preferredWrapWidth);
        if (norm.equals(cachedTooltipText) && wrapWidth == cachedTooltipWrapWidth) {
            drawHoveringTextAtAdjustedPosition(cachedTooltipLines, mouseX, mouseY, fr, bounds);
            return;
        }
        List<String> lines = new ArrayList<>();
        for (String rawLine : norm.split("\n", -1)) {
            if (rawLine.isEmpty()) {
                lines.add("");
                continue;
            }
            if (fr.getStringWidth(rawLine) <= wrapWidth) {
                lines.add(rawLine);
                continue;
            }
            lines.addAll(fr.listFormattedStringToWidth(rawLine, wrapWidth));
        }
        cachedTooltipText = norm;
        cachedTooltipWrapWidth = wrapWidth;
        cachedTooltipLines = lines;
        drawHoveringTextAtAdjustedPosition(lines, mouseX, mouseY, fr, bounds);
    }

    private void drawHoveringTextAtAdjustedPosition(List<String> lines, int mouseX, int mouseY, FontRenderer font) {
        drawHoveringTextAtAdjustedPosition(lines, mouseX, mouseY, font, resolveTooltipBounds(null));
    }

    private void drawHoveringTextAtAdjustedPosition(List<String> lines, int mouseX, int mouseY, FontRenderer font,
        LytRect bounds) {
        if (lines == null || lines.isEmpty()) {
            return;
        }
        int tooltipWidth = 0;
        for (String line : lines) {
            tooltipWidth = Math.max(tooltipWidth, font.getStringWidth(line));
        }
        int tooltipHeight = lines.size() == 1 ? 8 : 8 + (lines.size() - 1) * 10;
        int adjustedX = mouseX;
        int drawLeft = adjustedX + 12;
        int drawRight = drawLeft + tooltipWidth;
        if (drawRight + 4 > bounds.right()) {
            adjustedX = mouseX - tooltipWidth - 24;
            drawLeft = adjustedX + 12;
        }
        if (drawLeft < bounds.x() + 4) {
            adjustedX = bounds.x() + 4 - 12;
        }

        int adjustedY = mouseY;
        int drawTop = adjustedY - 12;
        int drawBottom = drawTop + tooltipHeight + 8;
        if (drawBottom > bounds.bottom()) {
            adjustedY = bounds.bottom() - tooltipHeight - 8 + 12;
        }
        if (adjustedY - 16 < bounds.y() + 4) {
            adjustedY = bounds.y() + 20;
        }
        drawHoveringText(lines, adjustedX, adjustedY, font);
    }

    private LytRect resolveTooltipBounds(@Nullable DocumentInteractionState interaction) {
        if (interaction == null) {
            return new LytRect(0, 0, this.width, this.height);
        }
        return new LytRect(interaction.tooltipX, interaction.tooltipY, interaction.tooltipW, interaction.tooltipH);
    }

    private void renderDocument(int mouseX, int mouseY) {
        var activeDocument = getActiveDocument();
        int documentY = getDocumentViewportY();
        int documentH = getDocumentViewportHeight();
        if (activeDocument == null || documentH <= 0) {
            return;
        }
        int documentRenderY = getDocumentRenderY(activeDocument);

        var ctx = reusableRenderCtx;
        ctx.setLightDarkMode(LightDarkMode.LIGHT_MODE);
        cachedViewportRect = cachedRect(cachedViewportRect, 0, scrollY, contentW, documentH);
        cachedScissorRect = cachedRect(cachedScissorRect, contentX, documentY, contentW, documentH);
        ctx.setViewport(cachedViewportRect);
        ctx.setScreenHeight(this.height);
        ctx.setDocumentOrigin(contentX, documentRenderY);
        ctx.setScrollOffsetY(scrollY);
        ctx.setZoom(currentZoom);

        var interaction = getDocumentInteractionState(mouseX, mouseY);
        activeDocument.setHoveredElement(interaction != null ? interaction.hit : null);

        ctx.pushScissor(cachedScissorRect);
        GL11.glPushMatrix();
        GL11.glTranslatef(contentX, documentRenderY, 0f);
        if (currentZoom != 1.0f) {
            GL11.glScalef(currentZoom, currentZoom, 1f);
        }
        GL11.glTranslatef(0f, -(float) scrollY, 0f);
        try {
            activeDocument.render(ctx);
        } catch (Throwable t) {
            FMLLog.severe("Error rendering guide document {}", currentAnchor.pageId(), t);
        } finally {
            GL11.glPopMatrix();
            ctx.restoreExternalRenderState();
            ctx.popScissor();
        }
    }

    @Nullable
    private DocumentInteractionState getActiveInteractionState(int mouseX, int mouseY) {
        if (isGuideEditorActive()) {
            return isInsideGuideEditorPreview(mouseX, mouseY) ? getGuideEditorPreviewInteractionState(mouseX, mouseY)
                : null;
        }
        return getDocumentInteractionState(mouseX, mouseY);
    }

    private void drawSearchField() {
        if (searchField == null) return;

        searchField.drawTextBox();
        if (shouldDrawSearchPlaceholder()) {
            drawString(
                fontRendererObj,
                GuidebookText.SearchPlaceholder.text(),
                searchField.xPosition,
                searchField.yPosition,
                0xFF666666);
        }
    }

    private boolean shouldDrawSearchPlaceholder() {
        return searchField != null && searchField.getText()
            .isEmpty() && !searchField.isFocused();
    }

    private boolean isCenteredSearchStateDocument(@Nullable LytDocument activeDocument) {
        return (isSearchPage() || isItemLinksPage())
            && GuideSearchResultDocumentBuilder.isCenteredStateDocument(activeDocument);
    }

    private void drawCenteredSearchStateMessage(LytDocument activeDocument) {
        String message = activeDocument.getTextContent()
            .trim();
        if (message.isEmpty()) {
            return;
        }

        int areaX = panelX + PANEL_PADDING;
        int areaY = getDocumentViewportY();
        int areaW = Math.max(20, panelW - PANEL_PADDING * 2);
        int areaH = getDocumentViewportHeight();
        int textW = fontRendererObj.getStringWidth(message);
        int textX = areaX + Math.max(0, (areaW - textW) / 2);
        int textY = areaY + Math.max(0, (areaH - fontRendererObj.FONT_HEIGHT) / 2);
        fontRendererObj
            .drawString(message, textX, textY, SymbolicColor.BODY_TEXT.resolve(LightDarkMode.LIGHT_MODE), false);
    }

    public static LytRect cachedRect(@Nullable LytRect current, int x, int y, int w, int h) {
        if (current != null && current.x() == x && current.y() == y && current.width() == w && current.height() == h) {
            return current;
        }
        return new LytRect(x, y, w, h);
    }

    private void drawPageMissingMessage() {
        FontRenderer fr = mc.fontRenderer;
        String msg = GuidebookText.PageNotFound.text(currentAnchor.pageId());
        int tw = fr.getStringWidth(msg);
        fr.drawStringWithShadow(msg, panelX + (panelW - tw) / 2, panelY + panelH / 2 - fr.FONT_HEIGHT / 2, 0xFFFF5555);
    }

    private void updateToolbarButtonState() {
        if (btnBack != null) {
            btnBack.enabled = !history.isEmpty();
        }
        if (btnForward != null) {
            btnForward.enabled = !forwardHistory.isEmpty();
        }
        if (btnSearch != null) {
            btnSearch.enabled = canSearchCurrentView();
        }
    }

    private boolean canSearchCurrentView() {
        return isSearchPage() || isItemLinksPage()
            || currentAnchor == null
            || !guide.isPageFailed(currentAnchor.pageId());
    }

    private void drawTiledBackground() {
        drawRect(0, 0, this.width, this.height, BACKGROUND_DIM_COLOR);
        mc.getTextureManager()
            .bindTexture(BG_TEXTURE);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL11.GL_REPEAT);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL11.GL_REPEAT);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glColor4f(1f, 1f, 1f, Math.max(0f, Math.min(1f, BACKGROUND_ALPHA)));
        final float tile = 16f;
        float uMax = this.width / tile;
        float vMax = this.height / tile;
        var tess = Tessellator.instance;
        tess.startDrawingQuads();
        tess.addVertexWithUV(0, this.height, 0, 0, vMax);
        tess.addVertexWithUV(this.width, this.height, 0, uMax, vMax);
        tess.addVertexWithUV(this.width, 0, 0, uMax, 0);
        tess.addVertexWithUV(0, 0, 0, 0, 0);
        tess.draw();
        GL11.glColor4f(1f, 1f, 1f, 1f);
    }

    private void drawBorder(int x, int y, int w, int h, int color) {
        drawRect(x, y, x + w, y + 1, color);
        drawRect(x, y + h - 1, x + w, y + h, color);
        drawRect(x, y, x + 1, y + h, color);
        drawRect(x + w - 1, y, x + w, y + h, color);
    }

    private void drawScrollbar() {
        int barW = SCROLLBAR_W;
        int barX = panelX + panelW - 1 - barW;
        int barY = getDocumentViewportY();
        int barH = getDocumentViewportHeight();
        drawRect(barX, barY, barX + barW, barY + barH, 0x40FFFFFF);

        int total = getContentHeight();
        int viewportH = getDocumentViewportHeight();
        int thumbH = Math.max(16, (int) ((long) barH * viewportH / Math.max(1, total)));
        int maxScroll = getMaxScroll();
        int thumbY = maxScroll > 0 ? barY + (int) ((long) (barH - thumbH) * scrollY / maxScroll) : barY;
        int thumbColor = draggingScrollbar ? 0xFFFFFFFF : 0xFFCCCCCC;
        drawRect(barX, thumbY, barX + barW, thumbY + thumbH, thumbColor);
    }

    private int[] scrollbarThumbRect() {
        int barX = panelX + panelW - 1 - SCROLLBAR_W;
        int barY = getDocumentViewportY();
        int barH = getDocumentViewportHeight();
        int total = getContentHeight();
        int viewportH = getDocumentViewportHeight();
        int thumbH = Math.max(16, (int) ((long) barH * viewportH / Math.max(1, total)));
        int maxScroll = getMaxScroll();
        int thumbY = maxScroll > 0 ? barY + (int) ((long) (barH - thumbH) * scrollY / maxScroll) : barY;
        return new int[] { barX, thumbY, SCROLLBAR_W, thumbH, barY, barH };
    }

    private void updateScrollFromMouseY(int mouseY) {
        var r = scrollbarThumbRect();
        int barY = r[4];
        int barH = r[5];
        int thumbH = r[3];
        int maxScroll = getMaxScroll();
        if (maxScroll <= 0) return;
        int targetThumbY = mouseY - scrollbarGrabOffsetY;
        int track = Math.max(1, barH - thumbH);
        int rel = targetThumbY - barY;
        if (rel < 0) rel = 0;
        if (rel > track) rel = track;
        scrollY = (int) ((long) rel * maxScroll / track);
        clampScroll();
    }

    @Override
    public void handleMouseInput() {
        super.handleMouseInput();
        int dwheel = Mouse.getEventDWheel();
        if (dwheel != 0) {
            long now = System.currentTimeMillis();
            int mouseX = Mouse.getEventX() * this.width / mc.displayWidth;
            int mouseY = this.height - Mouse.getEventY() * this.height / mc.displayHeight - 1;
            if (navBar.isOpen() && navBar.contains(mouseX, mouseY)) {
                navBar.scroll(dwheel);
                return;
            }
            if (handleGuideEditorWheel(mouseX, mouseY, dwheel)) {
                return;
            }
            LytGuidebookScene scene = sceneAt(mouseX, mouseY);
            boolean sceneWheelBlocked = isSceneWheelInteractionBlocked(now);
            if (scene != null && scene.isInteractive()
                && !sceneWheelBlocked
                && (scene.containsBottomControlSlider(mouseX, mouseY) || ModConfig.ui.sceneWheelZoom)) {
                scene.scroll(mouseX, mouseY, dwheel);
                return;
            }

            var interaction = getActiveInteractionState(mouseX, mouseY);
            if (interaction != null && !isCodeBlockWheelInteractionBlocked()) {
                var hit = interaction.hit;
                if (hit != null) {
                    for (LytNode current = hit.node(); current != null; current = current.getParent()) {
                        if (current instanceof DocumentDragTarget dragTarget
                            && dragTarget.scroll(interaction.docX, interaction.docY, dwheel)) {
                            return;
                        }
                    }
                }
            }

            int step = GuiScreen.isShiftKeyDown() ? 60 : 20;
            scrollY -= Integer.signum(dwheel) * step;
            clampScroll();
            lastPageWheelScrollAtMillis = now;
        }
    }

    @Override
    public void handleKeyboardInput() {
        if (isCommittedCharacterEventForFocusedTextInput()) {
            keyTyped(Keyboard.getEventCharacter(), Keyboard.getEventKey());
            mc.func_152348_aa();
            return;
        }
        super.handleKeyboardInput();
    }

    private boolean isCommittedCharacterEventForFocusedTextInput() {
        return isFocusedTextInputReadyForCommittedCharacter() && Keyboard.getEventKey() == 0
            && Character.isDefined(Keyboard.getEventCharacter());
    }

    private boolean isFocusedTextInputReadyForCommittedCharacter() {
        return isGuideEditorActive() && guideEditorTextArea != null && guideEditorTextArea.isFocused()
            || searchField != null && searchField.isFocused();
    }

    private boolean isSceneWheelInteractionBlocked(long now) {
        int delayMillis = resolveSceneWheelInteractionDelayMillis();
        return delayMillis > 0 && now - lastPageWheelScrollAtMillis < delayMillis;
    }

    public static int resolveSceneWheelInteractionDelayMillis() {
        try {
            return Math.max(0, ModConfig.ui.sceneWheelInteractionDelayMillis);
        } catch (Throwable ignored) {
            return 750;
        }
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int button) {
        if (handleGuideEditorToolbarRightClick(mouseX, mouseY, button)) {
            return;
        }
        if (button == 0 && navBar.contains(mouseX, mouseY)) {
            var target = navBar.mouseClicked(mouseX, mouseY, currentAnchor != null ? currentAnchor.pageId() : null);
            if (target != null) {
                navigateTo(PageAnchor.page(target));
                mc.getSoundHandler()
                    .playSound(PositionedSoundRecord.func_147674_a(new ResourceLocation("gui.button.press"), 1.0F));
            }
            return;
        }
        if (searchField != null) {
            boolean insideField = isInsideSearchField(mouseX, mouseY);
            if (button == 0) {
                searchField.mouseClicked(mouseX, mouseY, button);
            }
            if (insideField) {
                if (button == 1) {
                    searchField.setFocused(true);
                    searchField.setText("");
                    updateSearchQuery("");
                }
                return;
            }
        }
        if (handleGuideEditorMouseClicked(mouseX, mouseY, button)) {
            return;
        }
        if (button == 0 && getMaxScroll() > 0) {
            var r = scrollbarThumbRect();
            int tx = r[0], ty = r[1], tw = r[2], th = r[3], barY = r[4], barH = r[5];
            if (mouseX >= tx && mouseX < tx + tw && mouseY >= barY && mouseY < barY + barH) {
                if (mouseY >= ty && mouseY < ty + th) {
                    scrollbarGrabOffsetY = mouseY - ty;
                } else {
                    scrollbarGrabOffsetY = th / 2;
                    updateScrollFromMouseY(mouseY);
                }
                draggingScrollbar = true;
                return;
            }
        }
        var activeDocument = getActiveDocument();
        if (activeDocument != null && isInsideDocument(mouseX, mouseY)) {
            var interaction = getDocumentInteractionState(mouseX, mouseY);
            if (button == 0) {
                var sceneButtonHit = interaction != null ? interaction.sceneButtonHit : null;
                if (sceneButtonHit != null) {
                    sceneButtonHit.scene.activateSceneButton(sceneButtonHit.role);
                    mc.getSoundHandler()
                        .playSound(PositionedSoundRecord.func_147674_a(new ResourceLocation("gui.button.press"), 1.0F));
                    return;
                }
            }
            int docX = interaction != null ? interaction.docX : mouseX - contentX;
            int docY = interaction != null ? interaction.docY : mouseY - getDocumentViewportY() + scrollY;
            var hit = interaction != null ? interaction.hit : activeDocument.pick(docX, docY);
            if (hit != null) {
                boolean handled = false;
                var fc = hit.content();
                while (fc != null && !handled) {
                    if (fc instanceof InteractiveElement ie) {
                        handled = ie.mouseClicked(this, docX, docY, button, false);
                        if (handled) break;
                    }
                    fc = fc.getFlowParent();
                }
                if (!handled) {
                    for (LytNode current = hit.node(); current != null && !handled; current = current.getParent()) {
                        if (current instanceof InteractiveElement ie) {
                            handled = ie.mouseClicked(this, docX, docY, button, false);
                        }
                    }
                }
                if (handled) {
                    mc.getSoundHandler()
                        .playSound(
                            PositionedSoundRecord.func_147674_a(new ResourceLocation("guidenh:guide_click"), 1.0F));
                    return;
                }
                if (startDocumentInteractionDrag(interaction, hit, docX, docY, mouseX, mouseY, button)) {
                    return;
                }
                if (button == 0 && getMaxScroll() > 0) {
                    draggingDocument = true;
                    dragLastMouseY = mouseY;
                    return;
                }
            } else if (button == 0 && getMaxScroll() > 0) {
                draggingDocument = true;
                dragLastMouseY = mouseY;
                return;
            }
        }
        super.mouseClicked(mouseX, mouseY, button);
    }

    private boolean handleGuideEditorToolbarRightClick(int mouseX, int mouseY, int button) {
        if (!isGuideEditorActive() || button != 1 || !isInsideGuideEditorToolbar(mouseX, mouseY)) {
            return false;
        }
        closeGuideEditorContextMenu();
        if (guideEditorTextArea != null) {
            guideEditorTextArea.setFocused(true);
        }
        openGuideEditorContextMenu(mouseX, mouseY);
        return true;
    }

    private boolean activateDocumentInteraction(DocumentInteractionState interaction, int button) {
        var hit = interaction.hit;
        if (hit == null) {
            return false;
        }
        boolean handled = false;
        var fc = hit.content();
        while (fc != null && !handled) {
            if (fc instanceof InteractiveElement ie) {
                handled = ie.mouseClicked(this, interaction.docX, interaction.docY, button, false);
                if (handled) break;
            }
            fc = fc.getFlowParent();
        }
        if (!handled) {
            for (LytNode current = hit.node(); current != null && !handled; current = current.getParent()) {
                if (current instanceof InteractiveElement ie) {
                    handled = ie.mouseClicked(this, interaction.docX, interaction.docY, button, false);
                }
            }
        }
        if (handled) {
            return true;
        }
        return startDocumentInteractionDrag(
            interaction,
            hit,
            interaction.docX,
            interaction.docY,
            interaction.mouseX,
            interaction.mouseY,
            button);
    }

    private boolean startDocumentInteractionDrag(DocumentInteractionState interaction, LytDocument.HitTestResult hit,
        int docX, int docY, int mouseX, int mouseY, int button) {
        if (button != 0 && button != 1) {
            return false;
        }
        for (LytNode current = hit.node(); current != null; current = current.getParent()) {
            if (current instanceof DocumentDragTarget dragTarget && dragTarget.beginDrag(docX, docY, button)) {
                activeDocumentDragTarget = dragTarget;
                return true;
            }
        }
        LytGuidebookScene scene = interaction != null ? interaction.scene : findSceneAncestor(hit.node());
        if (scene == null) {
            return false;
        }
        if (button == 0) {
            var sceneButtonHit = interaction != null ? interaction.sceneButtonHit : null;
            if (sceneButtonHit != null && sceneButtonHit.scene == scene) {
                scene.activateSceneButton(sceneButtonHit.role);
                mc.getSoundHandler()
                    .playSound(PositionedSoundRecord.func_147674_a(new ResourceLocation("gui.button.press"), 1.0F));
                return true;
            }
        }
        activeScene = scene;
        scene.startDrag(mouseX, mouseY, button);
        return true;
    }

    @Override
    protected void mouseClickMove(int mouseX, int mouseY, int clickedMouseButton, long timeSinceLastClick) {
        if (handleGuideEditorMouseDragged(mouseX, mouseY, clickedMouseButton)) {
            return;
        }
        if (draggingScrollbar) {
            updateScrollFromMouseY(mouseY);
            return;
        }
        if (activeDocumentDragTarget != null) {
            int[] docPoint = screenToDocumentPoint(mouseX, mouseY);
            activeDocumentDragTarget.dragTo(docPoint[0], docPoint[1]);
            return;
        }
        if (activeScene != null) {
            activeScene.drag(mouseX, mouseY);
            return;
        }
        if (draggingDocument) {
            int deltaY = mouseY - dragLastMouseY;
            dragLastMouseY = mouseY;
            scrollY -= deltaY;
            clampScroll();
            return;
        }
        super.mouseClickMove(mouseX, mouseY, clickedMouseButton, timeSinceLastClick);
    }

    @Override
    protected void mouseMovedOrUp(int mouseX, int mouseY, int state) {
        if (handleGuideEditorMouseReleased(mouseX, mouseY, state)) {
            return;
        }
        if (draggingScrollbar && state != -1) {
            draggingScrollbar = false;
            return;
        }
        if (activeDocumentDragTarget != null && state != -1) {
            activeDocumentDragTarget.endDrag();
            activeDocumentDragTarget = null;
            return;
        }
        if (activeScene != null && state != -1) {
            activeScene.endDrag();
            activeScene = null;
            return;
        }
        if (draggingDocument && state != -1) {
            draggingDocument = false;
            return;
        }
        super.mouseMovedOrUp(mouseX, mouseY, state);
    }

    @Nullable
    private LytGuidebookScene sceneAt(int mouseX, int mouseY) {
        var interaction = getActiveInteractionState(mouseX, mouseY);
        return interaction != null ? interaction.scene : null;
    }

    public static SceneButtonHit findSceneButtonHit(LytNode node, int mouseX, int mouseY) {
        if (node instanceof LytGuidebookScene scene && scene.isInteractive()) {
            var role = scene.sceneButtonAt(mouseX, mouseY);
            if (role != null) {
                return new SceneButtonHit(scene, role);
            }
            role = scene.ponderButtonAt(mouseX, mouseY);
            if (role != null) {
                return new SceneButtonHit(scene, role);
            }
        }
        var children = node.getChildren();
        if (children != null) {
            for (var c : children) {
                var r = findSceneButtonHit(c, mouseX, mouseY);
                if (r != null) return r;
            }
        }
        return null;
    }

    @Nullable
    public static LytGuidebookScene findSceneAncestor(@Nullable LytNode node) {
        var cur = node;
        while (cur != null) {
            if (cur instanceof LytGuidebookScene scene) return scene;
            cur = cur.getParent();
        }
        return null;
    }

    private void updateSceneHover(int mouseX, int mouseY) {
        clearHoveredScene();
        var interaction = getDocumentInteractionState(mouseX, mouseY);
        LytGuidebookScene scene = interaction != null ? interaction.scene : null;
        updateHoveredScene(scene, mouseX, mouseY);
    }

    private void updateHoveredScene(@Nullable LytGuidebookScene scene, int mouseX, int mouseY) {
        if (hoveredScene != scene) {
            clearHoveredScene();
        }
        if (scene != null) {
            hoveredScene = scene;
            if (scene.containsBottomControlSlider(mouseX, mouseY)) {
                scene.clearAnnotationHover();
                scene.setHoveredStructureLibHatch(null);
                scene.setHoveredBlock(null);
                scene.setHoveredEntity(null);
                return;
            }
            var ann = scene.updateAnnotationHover(mouseX, mouseY);
            if (ann != null) {
                scene.setHoveredStructureLibHatch(null);
                scene.setHoveredBlock(null);
                scene.setHoveredEntity(null);
                return;
            }
            scene.setHoveredStructureLibHatch(scene.pickStructureLibHatch(mouseX, mouseY));
            scene.updateHoveredSceneTarget(mouseX, mouseY);
        }
    }

    private void clearHoveredScene() {
        if (hoveredScene != null) {
            hoveredScene.setHoveredStructureLibHatch(null);
            hoveredScene.setHoveredBlock(null);
            hoveredScene.setHoveredEntity(null);
            hoveredScene.clearAnnotationHover();
            hoveredScene = null;
        }
    }

    @Nullable
    private DocumentInteractionState getDocumentInteractionState(int mouseX, int mouseY) {
        var activeDocument = getActiveDocument();
        if (activeDocument == null || !isInsideDocument(mouseX, mouseY)) {
            return null;
        }
        if (!activeDocument.hasLayout()) {
            cachedInteractionState = null;
            return null;
        }
        var interaction = cachedInteractionState;
        if (interaction != null && interaction.matches(
            activeDocument,
            mouseX,
            mouseY,
            contentX,
            getDocumentRenderY(activeDocument),
            contentW,
            getDocumentViewportHeight(),
            scrollY)) {
            return interaction;
        }
        int docX = Math.round((mouseX - contentX) / currentZoom);
        int docY = Math.round((mouseY - getDocumentRenderY(activeDocument)) / currentZoom) + scrollY;
        var hit = activeDocument.pick(docX, docY);
        var scene = hit != null ? findSceneAncestor(hit.node()) : null;
        SceneButtonHit sceneButtonHit = null;
        if (scene != null) {
            var role = scene.sceneButtonAt(mouseX, mouseY);
            if (role != null) {
                sceneButtonHit = new SceneButtonHit(scene, role);
            }
        } else {
            sceneButtonHit = findSceneButtonHit(activeDocument, mouseX, mouseY);
            if (sceneButtonHit != null) {
                scene = sceneButtonHit.scene;
            }
        }
        if (scene != null && !scene.containsSceneInteractiveTarget(mouseX, mouseY)) {
            scene = null;
        }
        interaction = new DocumentInteractionState(
            activeDocument,
            mouseX,
            mouseY,
            contentX,
            getDocumentRenderY(activeDocument),
            contentW,
            getDocumentViewportHeight(),
            scrollY,
            contentX,
            getDocumentViewportY(),
            contentW,
            getDocumentViewportHeight(),
            docX,
            docY,
            hit,
            scene,
            sceneButtonHit);
        cachedInteractionState = interaction;
        return interaction;
    }

    @Nullable
    private SceneButtonHit getSceneButtonHit(int mouseX, int mouseY) {
        var interaction = getActiveInteractionState(mouseX, mouseY);
        return interaction != null ? interaction.sceneButtonHit : null;
    }

    private void clearInteractionState() {
        clearHoveredScene();
        if (activeDocumentDragTarget != null) {
            activeDocumentDragTarget.endDrag();
            activeDocumentDragTarget = null;
        }
        draggingDocument = false;
        guideEditorDraggingDivider = false;
        guideEditorDraggingPreviewScrollbar = false;
        guideEditorPreviewScrollbarGrabOffset = 0;
        guideEditorDividerHoverStartedAtMillis = 0L;
        if (document != null) {
            document.setHoveredElement(null);
        }
        if (guideEditorPreviewPage != null && guideEditorPreviewPage.document() != null) {
            guideEditorPreviewPage.document()
                .setHoveredElement(null);
        }
        if (searchDocument != null && searchDocument != document) {
            searchDocument.setHoveredElement(null);
        }
        cachedInteractionState = null;
        cachedGuideEditorPreviewInteractionState = null;
    }

    private int[] screenToDocumentPoint(int mouseX, int mouseY) {
        var activeDocument = getActiveDocument();
        if (activeDocument == null) {
            return new int[] { Math.round((mouseX - contentX) / currentZoom),
                Math.round((mouseY - getDocumentViewportY()) / currentZoom) + scrollY };
        }
        return new int[] { Math.round((mouseX - contentX) / currentZoom),
            Math.round((mouseY - getDocumentRenderY(activeDocument)) / currentZoom) + scrollY };
    }

    private int[] screenToActiveDocumentPoint(int mouseX, int mouseY) {
        if (isGuideEditorActive() && isInsideGuideEditorPreview(mouseX, mouseY)) {
            return new int[] { mouseX - getGuideEditorPreviewX(),
                mouseY - getGuideEditorContentTop() + guideEditorPreviewScrollY };
        }
        return screenToDocumentPoint(mouseX, mouseY);
    }

    @Nullable
    public static String blockDisplayName(LytGuidebookScene scene, int x, int y, int z) {
        try {
            var hoveredHit = scene.getHoveredBlockHitResult();
            if (hoveredHit != null && hoveredHit.blockX == x && hoveredHit.blockY == y && hoveredHit.blockZ == z) {
                return GuideBlockDisplayResolver.resolveDisplayName(scene.getLevel(), x, y, z, hoveredHit);
            }
            return GuideBlockDisplayResolver.resolveDisplayName(scene.getLevel(), x, y, z);
        } catch (Throwable t) {
            return null;
        }
    }

    @Nullable
    public static ItemStack blockDisplayStack(LytGuidebookScene scene, int x, int y, int z) {
        try {
            var hoveredHit = scene.getHoveredBlockHitResult();
            if (hoveredHit != null && hoveredHit.blockX == x && hoveredHit.blockY == y && hoveredHit.blockZ == z) {
                return GuideBlockDisplayResolver.resolveDisplayStack(scene.getLevel(), x, y, z, hoveredHit);
            }
            return GuideBlockDisplayResolver.resolveDisplayStack(scene.getLevel(), x, y, z);
        } catch (Throwable t) {
            return null;
        }
    }

    public static boolean isShiftDown() {
        return Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) || Keyboard.isKeyDown(Keyboard.KEY_RSHIFT);
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) {
        if (handleSearchFieldKey(typedChar, keyCode)) return;
        if (handleGuideEditorKey(typedChar, keyCode)) return;
        if (keyCode == Keyboard.KEY_ESCAPE) {
            close();
            return;
        }
        if (keyCode == Keyboard.KEY_BACK) {
            if (!history.isEmpty()) {
                forwardHistory.push(currentAnchor);
                var prev = history.pop();
                navigateWithoutHistory(prev);
                rebuildToolbar();
            }
            return;
        }
        if (keyCode == Keyboard.KEY_HOME) {
            scrollY = 0;
            return;
        }
        if (keyCode == Keyboard.KEY_END) {
            scrollY = getMaxScroll();
            return;
        }
        if (keyCode == Keyboard.KEY_PRIOR) { // PageUp
            scrollY -= Math.max(1, getDocumentViewportHeight() - 20);
            clampScroll();
            return;
        }
        if (keyCode == Keyboard.KEY_NEXT) { // PageDown
            scrollY += Math.max(1, getDocumentViewportHeight() - 20);
            clampScroll();
            return;
        }
        super.keyTyped(typedChar, keyCode);
    }

    private boolean isInsideDocument(int mouseX, int mouseY) {
        int documentY = getDocumentViewportY();
        int documentH = getDocumentViewportHeight();
        return mouseX >= contentX && mouseX < contentX + contentW
            && mouseY >= documentY
            && mouseY < documentY + documentH;
    }

    // GuideUiHost
    @Override
    public void navigateTo(PageAnchor anchor) {
        if (anchor == null || anchor.equals(currentAnchor)) return;
        history.push(currentAnchor);
        forwardHistory.clear();
        navigateWithoutHistory(anchor);
        rebuildToolbar();
    }

    private void navigateWithoutHistory(PageAnchor anchor) {
        clearInteractionState();
        currentAnchor = anchor;
        pendingAnchorScroll = anchor != null && anchor.anchor() != null;
        currentPage = null;
        document = null;
        layoutDocument = null;
        lastLayoutWidth = -1;
        scrollY = 0;
        loadCurrentPage();
        ensureLayout();
        scrollToCurrentAnchor();
        clampScroll();
        if (isGuideEditorActive()) {
            refreshGuideEditorDraft(true);
        }
    }

    @Override
    public void close() {
        mc.displayGuiScreen(null);
        if (mc.currentScreen == null) {
            mc.setIngameFocus();
        }
    }

    @Override
    public void onGuiClosed() {
        super.onGuiClosed();
        Keyboard.enableRepeatEvents(false);
        document = null;
        layoutDocument = null;
        currentPage = null;
        activeScene = null;
        activeDocumentDragTarget = null;
        hoveredScene = null;
        searchDocument = null;
        cachedInteractionState = null;
        cachedViewportRect = null;
        cachedScissorRect = null;
        cachedContentTooltipViewport = null;
        cachedTitleViewport = null;
        cachedBottomBarText = null;
        cachedBottomBarPage = null;
        history.clear();
        forwardHistory.clear();
        registeredScenes.clear();
        guideEditorPreviewPage = null;
        guideEditorDraftPage = null;
        guideEditorDraftSource = null;
        guideEditorSavedSource = null;
        guideEditorTextArea = null;
        guideEditorContextMenu = null;
        cachedGuideEditorPreviewInteractionState = null;
    }

    @Override
    public void openExternalUrl(URI uri) {
        if (shouldConfirmExternalLinks()) {
            pendingExternalUri = uri;
            mc.displayGuiScreen(createExternalLinkConfirmScreen(uri));
            return;
        }

        browseExternalUrl(uri);
    }

    @Override
    public boolean copyCodeBlock(String text) {
        try {
            codeBlockClipboardService.copy(text);
            return true;
        } catch (Exception e) {
            FMLLog.severe("Failed to copy code block", e);
            return false;
        }
    }

    @Override
    public boolean isCodeBlockWheelInteractionBlocked() {
        return isSceneWheelInteractionBlocked(System.currentTimeMillis());
    }

    private boolean shouldConfirmExternalLinks() {
        try {
            return ModConfig.ui.confirmExternalLinks;
        } catch (Throwable ignored) {
            return true;
        }
    }

    private void browseExternalUrl(URI uri) {
        try {
            Desktop.getDesktop()
                .browse(uri);
        } catch (Exception e) {
            FMLLog.warning("Failed to open external guide link {}", uri, e);
        }
    }

    private GuiConfirmOpenLink createExternalLinkConfirmScreen(URI uri) {
        return new GuiConfirmOpenLink(this, uri.toString(), EXTERNAL_LINK_CONFIRM_ID, false) {

            @Override
            protected void keyTyped(char typedChar, int keyCode) {
                if (keyCode == Keyboard.KEY_ESCAPE) {
                    GuideScreen.this.confirmClicked(false, EXTERNAL_LINK_CONFIRM_ID);
                    return;
                }
                super.keyTyped(typedChar, keyCode);
            }
        };
    }

    private boolean isSearchPage() {
        return GuideSearchPage.isSearchAnchor(currentAnchor);
    }

    private boolean isItemLinksPage() {
        return GuideItemLinksPage.isItemLinksAnchor(currentAnchor);
    }

    private void ensureSearchField() {
        if (!isSearchPage()) {
            searchField = null;
            return;
        }

        int fieldX = getSearchToolbarFieldX();
        int fieldY = panelY + SEARCH_TOOLBAR_FIELD_Y_OFFSET;
        int fieldW = getSearchToolbarFieldWidth(fieldX);
        boolean focused = searchField != null && searchField.isFocused();
        String currentText = searchField != null ? searchField.getText()
            : GuideSearchPage.queryFromAnchor(currentAnchor);
        if (searchField == null || searchField.xPosition != fieldX
            || searchField.yPosition != fieldY
            || searchField.width != fieldW) {
            searchField = new GuiTextField(this.fontRendererObj, fieldX, fieldY, fieldW, SEARCH_FIELD_H);
            searchField.setMaxStringLength(SEARCH_MAX_QUERY_LENGTH);
            searchField.setEnableBackgroundDrawing(false);
            searchField.setText(currentText);
            searchField.setFocused(focused || currentText.isEmpty());
        }

        String query = GuideSearchPage.queryFromAnchor(currentAnchor);
        if (!Objects.equals(searchField.getText(), query)) {
            searchField.setText(query);
        }
    }

    private int getSearchButtonX() {
        return getRightToolbarButtonsLeft();
    }

    private int getSearchToolbarIconX() {
        return panelX + PANEL_PADDING;
    }

    private int getSearchToolbarFieldX() {
        return getSearchToolbarIconX() + GuideIconButton.WIDTH + TOOLBAR_GAP;
    }

    private int getSearchToolbarFieldWidth(int fieldX) {
        int fieldRight = getSearchButtonX() - TOOLBAR_GAP;
        if (isGuideEditorActive()) {
            fieldRight = Math.min(fieldRight, getGuideEditorModeButtonsLeft() - TOOLBAR_GAP);
        }
        return Math.max(20, fieldRight - fieldX);
    }

    private int getGuideEditorModeButtonsLeft() {
        return getRightToolbarButtonsLeft() - TOOLBAR_GAP - getGuideEditorModeButtonsWidth();
    }

    private int getGuideEditorModeButtonsWidth() {
        return GuideIconButton.WIDTH * 6 + TOOLBAR_GAP * 5;
    }

    private int getRightToolbarButtonsLeft() {
        return panelX + panelW - PANEL_PADDING - GuideIconButton.WIDTH * 5 - TOOLBAR_GAP * 4;
    }

    private void focusSearchField() {
        ensureSearchField();
        if (searchField != null) {
            searchField.setFocused(true);
        }
    }

    @Nullable
    private LytDocument getActiveDocument() {
        return isSearchPage() ? searchDocument : document;
    }

    private void rebuildSearchDocumentIfNeeded(boolean force) {
        if (!isSearchPage()) {
            return;
        }

        String query = GuideSearchPage.queryFromAnchor(currentAnchor);
        if (!force && searchDocument != null && Objects.equals(cachedSearchQuery, query)) {
            return;
        }

        clearInteractionState();
        cachedSearchQuery = query;
        searchDocument = buildSearchDocument(query);
        layoutDocument = null;
        lastLayoutWidth = -1;
    }

    private void openItemLinksPage(ItemStack stack) {
        var pages = guide.getIndex(ItemMultiIndex.class)
            .findAllByStack(stack);
        if (pages.isEmpty()) {
            return;
        }
        if (pages.size() == 1) {
            navigateTo(pages.get(0));
        } else {
            navigateTo(GuideItemLinksPage.anchorForStack(stack));
        }
    }

    private LytDocument buildItemLinksDocument(@Nullable ItemStack stack) {
        if (stack == null) {
            return GuideSearchResultDocumentBuilder.buildDocument(
                null,
                new ArrayList<>(),
                GuidebookText.ItemLinksEmpty.text(),
                GuidebookText.ItemLinksEmpty.text());
        }
        var anchors = guide.getIndex(ItemMultiIndex.class)
            .findAllByStack(stack);
        if (anchors.isEmpty()) {
            var doc = new LytDocument();
            doc.append(GuideSearchResultDocumentBuilder.buildCenteredMessage(GuidebookText.ItemLinksEmpty.text()));
            return doc;
        }
        var navTree = guide.getNavigationTree();
        var results = new ArrayList<GuideSearchResultDocumentBuilder.SearchPageResult>(anchors.size());
        for (var anchor : anchors) {
            NavigationNode node = navTree.getNodeById(anchor.pageId());
            String title = node != null && !node.title()
                .isEmpty() ? node.title()
                    : anchor.pageId()
                        .toString();
            GuidePageIcon icon = node != null ? node.icon() : null;
            int textColumnWidth = getSearchTextColumnWidth(icon != null);
            int pathWidth = getSearchPathWidth(textColumnWidth);
            results.add(
                new GuideSearchResultDocumentBuilder.SearchPageResult(
                    anchor,
                    icon,
                    clipRightForWidth(title, getSearchTitleWidth(textColumnWidth, pathWidth)),
                    clipSearchPath(resolveSearchResultPath(anchor.pageId()), pathWidth),
                    new LytFlowText()));
        }
        results.sort((a, b) -> {
            NavigationNode na = navTree.getNodeById(
                a.anchor()
                    .pageId());
            NavigationNode nb = navTree.getNodeById(
                b.anchor()
                    .pageId());
            int posA = na != null ? na.position() : Integer.MAX_VALUE;
            int posB = nb != null ? nb.position() : Integer.MAX_VALUE;
            if (posA != posB) return Integer.compare(posA, posB);
            return a.title()
                .compareToIgnoreCase(b.title());
        });
        var doc = new LytDocument();
        for (var result : results) {
            doc.append(GuideSearchResultDocumentBuilder.buildResultRow(result));
        }
        return doc;
    }

    private LytDocument buildSearchDocument(String query) {
        var results = new ArrayList<GuideSearchResultDocumentBuilder.SearchPageResult>();
        String normalizedQuery = GuideSearchPage.normalizeQuery(query);
        try {
            for (var result : GuideME.getSearch()
                .searchGuide(normalizedQuery, guide)) {
                GuidePageIcon icon = resolveSearchResultIcon(result.pageId());
                int textColumnWidth = getSearchTextColumnWidth(icon != null);
                int pathWidth = getSearchPathWidth(textColumnWidth);
                String rawTitle = result.pageTitle()
                    .isEmpty()
                        ? result.pageId()
                            .toString()
                        : result.pageTitle();
                String title = clipRightForWidth(rawTitle, getSearchTitleWidth(textColumnWidth, pathWidth));
                results.add(
                    new GuideSearchResultDocumentBuilder.SearchPageResult(
                        PageAnchor.page(result.pageId()),
                        icon,
                        title,
                        clipSearchPath(resolveSearchResultPath(result.pageId()), pathWidth),
                        clipSnippetForWidth(result.text(), getSearchSnippetLineWidth(textColumnWidth))));
            }
        } catch (Throwable t) {
            FMLLog.warning("Search failed", t);
        }

        return GuideSearchResultDocumentBuilder
            .buildDocument(query, results, GuidebookText.SearchNoQuery.text(), GuidebookText.SearchNoResults.text());
    }

    @Nullable
    private GuidePageIcon resolveSearchResultIcon(ResourceLocation pageId) {
        var node = guide.getNavigationTree()
            .getNodeById(pageId);
        return node != null ? node.icon() : null;
    }

    private String resolveSearchResultPath(ResourceLocation pageId) {
        return pageId.toString();
    }

    private int getSearchTextColumnWidth(boolean hasIcon) {
        return Math.max(80, contentW - (hasIcon ? SEARCH_RESULT_ICON_AND_GAP : 0));
    }

    private int getSearchPathWidth(int textColumnWidth) {
        return Math.max(192, textColumnWidth / 3);
    }

    private int getSearchTitleWidth(int textColumnWidth, int pathWidth) {
        return Math.max(60, textColumnWidth - pathWidth - SEARCH_RESULT_TITLE_GAP);
    }

    private int getSearchSnippetLineWidth(int textColumnWidth) {
        return textColumnWidth;
    }

    private String clipSearchPath(String value, int pathWidth) {
        return clipRightForWidth(clipRightForChars(value, SEARCH_PATH_MAX_CHARS), pathWidth);
    }

    private String clipRightForWidth(String value, int maxWidth) {
        if (value == null || value.isEmpty() || maxWidth <= 0 || fontRendererObj.getStringWidth(value) <= maxWidth) {
            return value;
        }

        String unclippedValue = value.endsWith(ASCII_ELLIPSIS) && value.length() > ASCII_ELLIPSIS.length()
            ? value.substring(0, value.length() - ASCII_ELLIPSIS.length())
            : value;
        int ellipsisWidth = fontRendererObj.getStringWidth(ASCII_ELLIPSIS);
        if (ellipsisWidth >= maxWidth) {
            return fontRendererObj.trimStringToWidth(ASCII_ELLIPSIS, maxWidth);
        }

        String head = fontRendererObj.trimStringToWidth(unclippedValue, maxWidth - ellipsisWidth);
        return head + ASCII_ELLIPSIS;
    }

    private String clipRightForChars(String value, int maxChars) {
        if (value == null || value.isEmpty() || maxChars <= 0 || value.length() <= maxChars) {
            return value;
        }

        if (maxChars <= ASCII_ELLIPSIS.length()) {
            return ASCII_ELLIPSIS.substring(0, maxChars);
        }

        return value.substring(0, maxChars - ASCII_ELLIPSIS.length()) + ASCII_ELLIPSIS;
    }

    private LytFlowContent clipSnippetForWidth(LytFlowContent snippet, int lineWidth) {
        if (lineWidth <= 0) {
            return GuideSearchSnippetFormatter.clipToVisibleChars(snippet, 0);
        }

        var plain = new StringBuilder();
        snippet.visit(new LytVisitor() {

            @Override
            public void text(String text) {
                plain.append(text);
            }
        });

        String plainText = plain.toString();
        if (plainText.isEmpty()) {
            return snippet;
        }

        String firstLine = fontRendererObj.trimStringToWidth(plainText, lineWidth);
        if (firstLine.length() >= plainText.length()) {
            return snippet;
        }

        String secondLine = fontRendererObj.trimStringToWidth(plainText.substring(firstLine.length()), lineWidth);
        return GuideSearchSnippetFormatter
            .clipToVisibleCharsWithEllipsis(snippet, firstLine.length() + secondLine.length());
    }

    private int getDocumentRenderY(LytDocument activeDocument) {
        return getDocumentViewportY() + getDocumentRenderOffsetY(activeDocument);
    }

    private int getDocumentRenderOffsetY(LytDocument activeDocument) {
        if (!GuideSearchResultDocumentBuilder.isCenteredStateDocument(activeDocument)) {
            return 0;
        }
        // Use layout-unit viewport height so centering is correct under any zoom level.
        return Math.max(0, (getDocumentViewportHeight() - activeDocument.getContentHeight()) / 2);
    }

    private boolean handleSearchFieldKey(char typedChar, int keyCode) {
        if (!isSearchPage() || searchField == null || keyCode == Keyboard.KEY_ESCAPE) {
            return false;
        }

        boolean focused = searchField.isFocused();
        if (!focused && !isSearchTypingKey(typedChar, keyCode)) {
            return false;
        }

        if (!focused) {
            searchField.setFocused(true);
        }

        String before = searchField.getText();
        searchField.textboxKeyTyped(typedChar, keyCode);
        String after = searchField.getText();
        if (!Objects.equals(before, after)) {
            updateSearchQuery(after);
        }

        return !focused || shouldConsumeFocusedSearchKey(typedChar, keyCode, before, after);
    }

    private void updateSearchQuery(String query) {
        var nextAnchor = GuideSearchPage.anchorForQuery(query);
        if (nextAnchor.equals(currentAnchor)) {
            rebuildSearchDocumentIfNeeded(false);
            return;
        }

        history.push(currentAnchor);
        forwardHistory.clear();
        clearInteractionState();
        currentAnchor = nextAnchor;
        currentPage = null;
        document = null;
        refreshCurrentPageTitle();
        rebuildSearchDocumentIfNeeded(true);
        scrollY = 0;
        rebuildToolbar();
    }

    private boolean isInsideSearchField(int mouseX, int mouseY) {
        return searchField != null && mouseX >= searchField.xPosition
            && mouseX < searchField.xPosition + searchField.width
            && mouseY >= searchField.yPosition
            && mouseY < searchField.yPosition + SEARCH_FIELD_H;
    }

    public static boolean shouldConsumeFocusedSearchKey(char typedChar, int keyCode, String before, String after) {
        return !Objects.equals(before, after) || isSearchEditingKey(typedChar, keyCode)
            || isCtrlKeyCombo(keyCode, Keyboard.KEY_A)
            || isCtrlKeyCombo(keyCode, Keyboard.KEY_C)
            || isCtrlKeyCombo(keyCode, Keyboard.KEY_V)
            || isCtrlKeyCombo(keyCode, Keyboard.KEY_X);
    }

    public static boolean isSearchEditingKey(char typedChar, int keyCode) {
        return typedChar >= 32 || keyCode == Keyboard.KEY_BACK
            || keyCode == Keyboard.KEY_DELETE
            || keyCode == Keyboard.KEY_LEFT
            || keyCode == Keyboard.KEY_RIGHT
            || keyCode == Keyboard.KEY_HOME
            || keyCode == Keyboard.KEY_END
            || keyCode == Keyboard.KEY_RETURN
            || keyCode == Keyboard.KEY_NUMPADENTER
            || keyCode == Keyboard.KEY_SPACE;
    }

    public static boolean isSearchTypingKey(char typedChar, int keyCode) {
        return typedChar >= 32 || isCtrlKeyCombo(keyCode, Keyboard.KEY_V);
    }

    public static boolean isCtrlKeyCombo(int keyCode, int expectedKeyCode) {
        return keyCode == expectedKeyCode
            && (Keyboard.isKeyDown(Keyboard.KEY_LCONTROL) || Keyboard.isKeyDown(Keyboard.KEY_RCONTROL));
    }
}
