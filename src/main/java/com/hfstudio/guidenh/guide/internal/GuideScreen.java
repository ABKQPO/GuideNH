package com.hfstudio.guidenh.guide.internal;

import java.awt.Desktop;
import java.net.URI;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

import com.hfstudio.guidenh.config.ModConfig;
import com.hfstudio.guidenh.guide.GuidePage;
import com.hfstudio.guidenh.guide.GuidePageIcon;
import com.hfstudio.guidenh.guide.PageAnchor;
import com.hfstudio.guidenh.guide.color.LightDarkMode;
import com.hfstudio.guidenh.guide.color.SymbolicColor;
import com.hfstudio.guidenh.guide.document.LytRect;
import com.hfstudio.guidenh.guide.document.block.LytDocument;
import com.hfstudio.guidenh.guide.document.block.LytHeading;
import com.hfstudio.guidenh.guide.document.block.LytNode;
import com.hfstudio.guidenh.guide.document.block.LytSlot;
import com.hfstudio.guidenh.guide.document.block.LytVisitor;
import com.hfstudio.guidenh.guide.document.flow.LytFlowContent;
import com.hfstudio.guidenh.guide.document.interaction.ContentTooltip;
import com.hfstudio.guidenh.guide.document.interaction.DocumentDragTarget;
import com.hfstudio.guidenh.guide.document.interaction.GuideTooltip;
import com.hfstudio.guidenh.guide.document.interaction.InteractiveElement;
import com.hfstudio.guidenh.guide.document.interaction.ItemTooltip;
import com.hfstudio.guidenh.guide.document.interaction.TextTooltip;
import com.hfstudio.guidenh.guide.internal.markdown.CodeBlockClipboardService;
import com.hfstudio.guidenh.guide.internal.screen.GuideIconButton;
import com.hfstudio.guidenh.guide.internal.screen.GuideNavBar;
import com.hfstudio.guidenh.guide.internal.search.GuideSearchPage;
import com.hfstudio.guidenh.guide.internal.search.GuideSearchResultDocumentBuilder;
import com.hfstudio.guidenh.guide.internal.search.GuideSearchSnippetFormatter;
import com.hfstudio.guidenh.guide.internal.tooltip.GuideItemTooltipLines;
import com.hfstudio.guidenh.guide.internal.tooltip.GuideItemTooltipRenderSupport;
import com.hfstudio.guidenh.guide.layout.LayoutContext;
import com.hfstudio.guidenh.guide.layout.MinecraftFontMetrics;
import com.hfstudio.guidenh.guide.render.VanillaRenderContext;
import com.hfstudio.guidenh.guide.scene.LytGuidebookScene;
import com.hfstudio.guidenh.guide.scene.support.GuideBlockDisplayResolver;
import com.hfstudio.guidenh.guide.scene.support.GuideEntityDisplayResolver;
import com.hfstudio.guidenh.guide.ui.GuideUiHost;

public class GuideScreen extends GuiScreen implements GuideUiHost, GuiYesNoCallback {

    public static final Logger LOG = LogManager.getLogger("GuideNH/GuideScreen");

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
    public static final int TOOLBAR_H = 16;
    public static final int TOOLBAR_GAP = 3;
    private boolean fullWidth;

    private final GuideNavBar navBar = new GuideNavBar();
    private final MinecraftFontMetrics layoutFontMetrics = new MinecraftFontMetrics();
    private final CodeBlockClipboardService codeBlockClipboardService = new CodeBlockClipboardService();

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
    private LytGuidebookScene hoveredScene;

    @Nullable
    private GuiTextField searchField;
    @Nullable
    private LytDocument searchDocument;
    @Nullable
    private String cachedSearchQuery;
    private String currentPageTitle = "";
    @Nullable
    private LytDocument layoutDocument;

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
        final int docX;
        final int docY;
        @Nullable
        final LytDocument.HitTestResult hit;
        @Nullable
        final LytGuidebookScene scene;
        @Nullable
        final SceneButtonHit sceneButtonHit;

        private DocumentInteractionState(LytDocument document, int mouseX, int mouseY, int contentX, int contentY,
            int contentW, int contentH, int scrollY, int docX, int docY, @Nullable LytDocument.HitTestResult hit,
            @Nullable LytGuidebookScene scene, @Nullable SceneButtonHit sceneButtonHit) {
            this.document = document;
            this.mouseX = mouseX;
            this.mouseY = mouseY;
            this.contentX = contentX;
            this.contentY = contentY;
            this.contentW = contentW;
            this.contentH = contentH;
            this.scrollY = scrollY;
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
        try {
            this.fullWidth = ModConfig.ui.fullWidth;
        } catch (Throwable ignored) {
            this.fullWidth = false;
        }
    }

    public static void open(ResourceLocation guideId, @Nullable PageAnchor anchor) {
        var guide = GuideRegistry.getById(guideId);
        if (guide == null) {
            LOG.warn("GuideScreen.open: no guide registered with id {}", guideId);
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

    public ResourceLocation getCurrentPageId() {
        return currentAnchor.pageId();
    }

    public boolean isShowingGuide(ResourceLocation guideId) {
        return guide.getId()
            .equals(guideId);
    }

    public void reloadPage() {
        var reloadedGuide = GuideRegistry.getById(guide.getId());
        if (reloadedGuide != null) {
            guide = reloadedGuide;
        }
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
        recomputePanelBounds();
        rebuildToolbar();
        if (document == null) {
            loadCurrentPage();
        }
        ensureLayout();
        clampScroll();
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
    }

    private void rebuildToolbar() {
        this.buttonList.clear();
        int btnY = panelY;
        int btnRight = panelX + panelW - PANEL_PADDING;
        btnClose = new GuideIconButton(0, btnRight - 16, btnY, GuideIconButton.Role.CLOSE);
        btnFullWidth = new GuideIconButton(
            1,
            btnRight - (16 + TOOLBAR_GAP) * 2 + TOOLBAR_GAP,
            btnY,
            fullWidth ? GuideIconButton.Role.CLOSE_FULL_WIDTH_VIEW : GuideIconButton.Role.OPEN_FULL_WIDTH_VIEW);
        btnForward = new GuideIconButton(
            2,
            btnRight - (16 + TOOLBAR_GAP) * 3 + TOOLBAR_GAP,
            btnY,
            GuideIconButton.Role.FORWARD);
        btnBack = new GuideIconButton(
            3,
            btnRight - (16 + TOOLBAR_GAP) * 4 + TOOLBAR_GAP,
            btnY,
            GuideIconButton.Role.BACK);
        btnSearch = new GuideIconButton(
            4,
            isSearchPage() ? getSearchToolbarIconX() : getRightSearchButtonX(),
            btnY,
            GuideIconButton.Role.SEARCH);
        this.buttonList.add(btnSearch);
        this.buttonList.add(btnBack);
        this.buttonList.add(btnForward);
        this.buttonList.add(btnFullWidth);
        this.buttonList.add(btnClose);
        updateToolbarButtonState();
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
        }
    }

    private void loadCurrentPage() {
        clearInteractionState();
        layoutDocument = null;
        lastLayoutWidth = -1;
        if (isSearchPage()) {
            currentPage = null;
            document = null;
            rebuildSearchDocumentIfNeeded(true);
        } else {
            searchField = null;
            try {
                currentPage = guide.getPage(currentAnchor.pageId());
            } catch (Throwable t) {
                LOG.error("Failed to compile guide page {}", currentAnchor.pageId(), t);
                currentPage = null;
            }
            if (currentPage != null) {
                document = currentPage.document();
            } else {
                document = null;
            }
        }
        refreshCurrentPageTitle();
        scrollY = 0;
        updateToolbarButtonState();
    }

    private void ensureLayout() {
        var activeDocument = getActiveDocument();
        if (activeDocument == null) return;
        if (!activeDocument.hasLayout() || layoutDocument != activeDocument || lastLayoutWidth != contentW) {
            clearInteractionState();
            activeDocument.updateLayout(new LayoutContext(layoutFontMetrics), contentW);
            layoutDocument = activeDocument;
            lastLayoutWidth = contentW;
        }
    }

    private void refreshCurrentPageTitle() {
        if (currentAnchor == null) {
            currentPageTitle = "";
            return;
        }

        if (isSearchPage()) {
            currentPageTitle = GuidebookText.Search.text();
            return;
        }

        String resolvedTitle = extractPageTitleFromDocument();

        if (resolvedTitle == null || resolvedTitle.isEmpty()) {
            try {
                var node = guide.getNavigationTree()
                    .getNodeById(currentAnchor.pageId());
                if (node != null) {
                    resolvedTitle = node.title();
                }
            } catch (Throwable ignored) {}
        }

        if (resolvedTitle == null || resolvedTitle.isEmpty()) {
            resolvedTitle = currentAnchor.pageId()
                .toString();
        }
        currentPageTitle = resolvedTitle;
    }

    /**
     * Finds the first H1 {@link LytHeading} in the current document, removes it from the
     * document (so it is not rendered twice inside the content area), and returns its plain
     * text. Returns {@code null} when no H1 is present.
     */
    @Nullable
    private String extractPageTitleFromDocument() {
        if (document == null) {
            return null;
        }
        for (var block : document.getBlocks()) {
            if (block instanceof LytHeading heading) {
                if (heading.getDepth() == 1) {
                    document.removeChild(heading);
                    return heading.getTextContent();
                } else {
                    // Any non-H1 heading as the first block stops the search
                    break;
                }
            } else {
                // Non-heading first block: stop searching
                break;
            }
        }
        return null;
    }

    private int getContentHeight() {
        var activeDocument = getActiveDocument();
        return activeDocument != null ? activeDocument.getContentHeight() : 0;
    }

    private int getDocumentViewportY() {
        return contentY;
    }

    private int getDocumentViewportHeight() {
        return contentH;
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
        drawTiledBackground();
        recomputePanelBounds();
        ensureSearchField();
        rebuildSearchDocumentIfNeeded(false);
        ensureLayout();
        clampScroll();

        drawRect(panelX, panelY, panelX + panelW, panelY + panelH, BG_COLOR);
        drawBorder(panelX, panelY, panelW, panelH, BG_BORDER);

        drawRect(panelX, panelY + TOOLBAR_H, panelX + panelW, panelY + TOOLBAR_H + 1, 0xFF2A2A2A);

        updateSceneHover(mouseX, mouseY);

        var activeDocument = getActiveDocument();
        if (activeDocument != null) {
            if (isCenteredSearchStateDocument(activeDocument)) {
                drawCenteredSearchStateMessage(activeDocument);
            } else {
                renderDocument(mouseX, mouseY);
            }
        } else {
            drawPageMissingMessage();
        }

        if (getMaxScroll() > 0) {
            drawScrollbar();
        }

        int navX = panelX;
        int navY = panelY + TOOLBAR_H + 1;
        int navH = Math.max(20, panelH - TOOLBAR_H - 1);
        navBar.setBounds(navX, navY, navH);
        navBar.update(mouseX, mouseY, guide.getNavigationTree());
        navBar.render(mc, currentAnchor != null ? currentAnchor.pageId() : null, mouseX, mouseY, guide);

        /*
         * Redraw toolbar strip after the document so stray NEI/GL output cannot obscure the title or search box;
         * icon buttons are still drawn afterward by GuiScreen.drawScreen.
         */
        drawRect(panelX, panelY, panelX + panelW, panelY + TOOLBAR_H, BG_COLOR);
        drawRect(panelX, panelY + TOOLBAR_H, panelX + panelW, panelY + TOOLBAR_H + 1, 0xFF2A2A2A);
        drawPageTitle();
        if (searchField != null) {
            drawSearchField();
        }

        super.drawScreen(mouseX, mouseY, partialTicks);

        drawButtonTooltip(mouseX, mouseY);
    }

    private void drawPageTitle() {
        if (currentAnchor == null || isSearchPage()) return;
        int reservedRight = (16 + TOOLBAR_GAP) * 5 + PANEL_PADDING + 4;
        int maxW = Math.max(20, panelW - PANEL_PADDING - reservedRight);
        String draw = currentPageTitle;
        if (fontRendererObj.getStringWidth(draw) > maxW) {
            draw = fontRendererObj.trimStringToWidth(draw, maxW - 4) + ASCII_ELLIPSIS;
        }
        fontRendererObj.drawString(draw, panelX + PANEL_PADDING, panelY + 4, 0xFFFFFFFF, true);
    }

    private void drawButtonTooltip(int mouseX, int mouseY) {
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
        var sceneButtonHit = getSceneButtonHit(mouseX, mouseY);
        if (sceneButtonHit != null) {
            drawTooltipText(sceneButtonHit.role.tooltip(), mouseX, mouseY);
            return;
        }
        if (navBar.isOpen() && navBar.contains(mouseX, mouseY)) return;
        var interaction = getDocumentInteractionState(mouseX, mouseY);
        if (interaction != null) {
            drawDocumentHoverTooltip(interaction, mouseX, mouseY);
        }
    }

    private void drawDocumentHoverTooltip(DocumentInteractionState interaction, int mouseX, int mouseY) {
        var hit = interaction.hit;
        if (hit == null) return;

        var scene = interaction.scene;
        if (scene != null) {
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
                        renderGuideTooltip(a.getTooltip(), mouseX, mouseY);
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
                    renderGuideTooltip(tooltip, mouseX, mouseY);
                    return;
                }
            }
            var hb = scene.getHoveredBlock();
            var hoveredEntity = scene.getHoveredEntity();
            if (hoveredEntity != null) {
                String name = GuideEntityDisplayResolver.resolveDisplayName(hoveredEntity);
                if (name != null) {
                    drawTooltipText(name, mouseX, mouseY);
                    return;
                }
            }
            if (hb != null) {
                GuideTooltip tooltip = resolveSceneBlockTooltip(scene, hb[0], hb[1], hb[2]);
                if (tooltip != null) {
                    renderGuideTooltip(tooltip, mouseX, mouseY);
                    return;
                }
            }
        }

        var fc = hit.content();
        while (fc != null) {
            var tip = tryGetTooltip(fc, interaction.docX, interaction.docY);
            if (tip.isPresent()) {
                renderGuideTooltip(tip.get(), mouseX, mouseY);
                return;
            }
            fc = fc.getFlowParent();
        }
        if (hit.node() != null) {
            for (LytNode current = hit.node(); current != null; current = current.getParent()) {
                var tip = tryGetTooltip(current, interaction.docX, interaction.docY);
                if (tip.isPresent()) {
                    renderGuideTooltip(tip.get(), mouseX, mouseY);
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

    private void renderGuideTooltip(GuideTooltip tooltip, int mouseX, int mouseY) {
        if (tooltip instanceof ItemTooltip it) {
            renderItemTooltip(it, mouseX, mouseY);
            return;
        }
        if (tooltip instanceof TextTooltip tt) {
            drawTooltipText(tt.getText(), mouseX, mouseY);
            return;
        }
        if (tooltip instanceof ContentTooltip ct) {
            drawContentTooltip(ct, mouseX, mouseY);
        }
    }

    private void renderItemTooltip(ItemTooltip tooltip, int mouseX, int mouseY) {
        ItemStack stack = tooltip.getStack();
        if (stack == null) {
            return;
        }

        if (GuideItemTooltipRenderSupport.shouldUseVanillaRenderer(tooltip)) {
            renderToolTip(stack, mouseX, mouseY);
            return;
        }

        List<String> lines = GuideItemTooltipLines.build(tooltip, mc);
        FontRenderer font = GuideItemTooltipRenderSupport.resolveFont(stack, mc.fontRenderer);
        drawHoveringText(lines, mouseX, mouseY, font);
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

    private void drawContentTooltip(ContentTooltip ct, int mouseX, int mouseY) {
        int pad = 4;
        int maxW = Math.max(80, (this.width * 4) / 5);
        var box = ct.layout(maxW);
        int w = box.width();
        int h = box.height();
        int x = mouseX + 12;
        int y = mouseY - 12;
        if (x + w + pad > this.width) x = mouseX - w - 12;
        if (x - pad < 0) x = pad;
        if (y + h + pad > this.height) y = this.height - h - pad;
        if (y - pad < 0) y = pad;
        drawRect(x - pad, y - pad, x + w + pad, y + h + pad, 0xF0100010);
        drawBorder(x - pad, y - pad, w + pad * 2, h + pad * 2, 0xFF5000FF);
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
            LOG.warn("Error rendering ContentTooltip", t);
        } finally {
            GL11.glPopMatrix();
            ctx.restoreExternalRenderState();
        }
    }

    private void drawTooltipText(String text, int mouseX, int mouseY) {
        FontRenderer fr = mc.fontRenderer;
        String norm = (text.indexOf('\\') >= 0) ? text.replace("\\n", "\n") : text;
        int pad = 3;
        int hardMaxWidth = Math.max(40, this.width - 24);
        int preferredWrapWidth = Math.max(80, this.width / 2);
        int wrapWidth = Math.min(hardMaxWidth, preferredWrapWidth);
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
        int tw = 0;
        for (String ln : lines) tw = Math.max(tw, fr.getStringWidth(ln));
        int th = lines.size() * (fr.FONT_HEIGHT + 1) - 1;
        int x = mouseX + 12;
        int y = mouseY - 12;
        if (x + tw + pad > this.width) {
            x = mouseX - tw - 12;
        }
        if (x - pad < 0) x = pad;
        if (y + th + pad > this.height) {
            y = this.height - th - pad;
        }
        if (y - pad < 0) y = pad;
        drawRect(x - pad, y - pad, x + tw + pad, y + th + pad, 0xF0100010);
        drawBorder(x - pad, y - pad, tw + pad * 2, th + pad * 2, 0xFF5000FF);
        int ly = y;
        for (String ln : lines) {
            fr.drawStringWithShadow(ln, x, ly, 0xFFFFFFFF);
            ly += fr.FONT_HEIGHT + 1;
        }
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

        var interaction = getDocumentInteractionState(mouseX, mouseY);
        activeDocument.setHoveredElement(interaction != null ? interaction.hit : null);

        ctx.pushScissor(cachedScissorRect);
        GL11.glPushMatrix();
        GL11.glTranslatef(contentX, documentRenderY - scrollY, 0f);
        try {
            activeDocument.render(ctx);
        } catch (Throwable t) {
            LOG.error("Error rendering guide document {}", currentAnchor.pageId(), t);
        } finally {
            GL11.glPopMatrix();
            ctx.restoreExternalRenderState();
            ctx.popScissor();
        }
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
        return isSearchPage() && GuideSearchResultDocumentBuilder.isCenteredStateDocument(activeDocument);
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
        return isSearchPage() || currentAnchor == null || !guide.isPageFailed(currentAnchor.pageId());
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
        int barX = panelX + panelW - 6;
        int barW = 4;
        int barY = getDocumentViewportY();
        int barH = getDocumentViewportHeight();
        drawRect(barX, barY, barX + barW, barY + barH, 0x40FFFFFF);

        int total = getContentHeight();
        int thumbH = Math.max(16, (int) ((long) barH * barH / Math.max(1, total)));
        int maxScroll = getMaxScroll();
        int thumbY = maxScroll > 0 ? barY + (int) ((long) (barH - thumbH) * scrollY / maxScroll) : barY;
        int thumbColor = draggingScrollbar ? 0xFFFFFFFF : 0xFFCCCCCC;
        drawRect(barX, thumbY, barX + barW, thumbY + thumbH, thumbColor);
    }

    private int[] scrollbarThumbRect() {
        int barX = panelX + panelW - 6;
        int barY = getDocumentViewportY();
        int barH = getDocumentViewportHeight();
        int total = getContentHeight();
        int thumbH = Math.max(16, (int) ((long) barH * barH / Math.max(1, total)));
        int maxScroll = getMaxScroll();
        int thumbY = maxScroll > 0 ? barY + (int) ((long) (barH - thumbH) * scrollY / maxScroll) : barY;
        return new int[] { barX, thumbY, 4, thumbH, barY, barH };
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
            LytGuidebookScene scene = sceneAt(mouseX, mouseY);
            boolean sceneWheelBlocked = isSceneWheelInteractionBlocked(now);
            if (scene != null && scene.isInteractive()
                && !sceneWheelBlocked
                && (scene.containsBottomControlSlider(mouseX, mouseY) || ModConfig.ui.sceneWheelZoom)) {
                scene.scroll(mouseX, mouseY, dwheel);
                return;
            }

            var interaction = getDocumentInteractionState(mouseX, mouseY);
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
        if (button == 0 && navBar.contains(mouseX, mouseY)) {
            var target = navBar.mouseClicked(mouseX, mouseY, currentAnchor != null ? currentAnchor.pageId() : null);
            if (target != null) {
                navigateTo(PageAnchor.page(target));
                mc.getSoundHandler()
                    .playSound(PositionedSoundRecord.func_147674_a(new ResourceLocation("gui.button.press"), 1.0F));
            }
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
                if (button == 0 || button == 1) {
                    for (LytNode current = hit.node(); current != null; current = current.getParent()) {
                        if (current instanceof DocumentDragTarget dragTarget
                            && dragTarget.beginDrag(docX, docY, button)) {
                            activeDocumentDragTarget = dragTarget;
                            return;
                        }
                    }
                    LytGuidebookScene scene = interaction != null ? interaction.scene : findSceneAncestor(hit.node());
                    if (scene != null) {
                        if (button == 0) {
                            var sceneButtonHit = interaction != null ? interaction.sceneButtonHit : null;
                            if (sceneButtonHit != null && sceneButtonHit.scene == scene) {
                                scene.activateSceneButton(sceneButtonHit.role);
                                mc.getSoundHandler()
                                    .playSound(
                                        PositionedSoundRecord
                                            .func_147674_a(new ResourceLocation("gui.button.press"), 1.0F));
                                return;
                            }
                        }
                        activeScene = scene;
                        scene.startDrag(mouseX, mouseY, button);
                        return;
                    }
                    if (button == 0 && getMaxScroll() > 0) {
                        draggingDocument = true;
                        dragLastMouseY = mouseY;
                        return;
                    }
                }
            } else if (button == 0 && getMaxScroll() > 0) {
                draggingDocument = true;
                dragLastMouseY = mouseY;
                return;
            }
        }
        super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    protected void mouseClickMove(int mouseX, int mouseY, int clickedMouseButton, long timeSinceLastClick) {
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
        var interaction = getDocumentInteractionState(mouseX, mouseY);
        return interaction != null ? interaction.scene : null;
    }

    public static SceneButtonHit findSceneButtonHit(LytNode node, int mouseX, int mouseY) {
        if (node instanceof LytGuidebookScene scene && scene.isInteractive()) {
            var role = scene.sceneButtonAt(mouseX, mouseY);
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
        int docX = mouseX - contentX;
        int docY = mouseY - getDocumentRenderY(activeDocument) + scrollY;
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
        var interaction = getDocumentInteractionState(mouseX, mouseY);
        return interaction != null ? interaction.sceneButtonHit : null;
    }

    private void clearInteractionState() {
        clearHoveredScene();
        if (activeDocumentDragTarget != null) {
            activeDocumentDragTarget.endDrag();
            activeDocumentDragTarget = null;
        }
        draggingDocument = false;
        if (document != null) {
            document.setHoveredElement(null);
        }
        if (searchDocument != null && searchDocument != document) {
            searchDocument.setHoveredElement(null);
        }
        cachedInteractionState = null;
    }

    private int[] screenToDocumentPoint(int mouseX, int mouseY) {
        var activeDocument = getActiveDocument();
        if (activeDocument == null) {
            return new int[] { mouseX - contentX, mouseY - getDocumentViewportY() + scrollY };
        }
        return new int[] { mouseX - contentX, mouseY - getDocumentRenderY(activeDocument) + scrollY };
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
        currentPage = null;
        document = null;
        layoutDocument = null;
        lastLayoutWidth = -1;
        scrollY = 0;
        loadCurrentPage();
        ensureLayout();
        clampScroll();
    }

    @Override
    public void close() {
        mc.displayGuiScreen(null);
        if (mc.currentScreen == null) {
            mc.setIngameFocus();
        }
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
            LOG.error("Failed to copy code block", e);
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
            LOG.warn("Failed to open external guide link {}", uri, e);
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

    private int getRightSearchButtonX() {
        int btnRight = panelX + panelW - PANEL_PADDING;
        return btnRight - (GuideIconButton.WIDTH + TOOLBAR_GAP) * 5 + TOOLBAR_GAP;
    }

    private int getSearchToolbarIconX() {
        return panelX + PANEL_PADDING;
    }

    private int getSearchToolbarFieldX() {
        return getSearchToolbarIconX() + GuideIconButton.WIDTH + TOOLBAR_GAP;
    }

    private int getSearchToolbarFieldWidth(int fieldX) {
        int fieldRight = getRightSearchButtonX() - TOOLBAR_GAP;
        return Math.max(20, fieldRight - fieldX);
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
            LOG.warn("Search failed", t);
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
