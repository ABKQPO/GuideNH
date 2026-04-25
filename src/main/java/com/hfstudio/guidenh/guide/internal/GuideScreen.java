package com.hfstudio.guidenh.guide.internal;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

import javax.annotation.Nullable;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.RenderItem;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.ResourceLocation;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

import com.hfstudio.guidenh.config.ModConfig;
import com.hfstudio.guidenh.guide.GuidePage;
import com.hfstudio.guidenh.guide.PageAnchor;
import com.hfstudio.guidenh.guide.color.LightDarkMode;
import com.hfstudio.guidenh.guide.document.LytRect;
import com.hfstudio.guidenh.guide.document.block.LytDocument;
import com.hfstudio.guidenh.guide.document.block.LytNode;
import com.hfstudio.guidenh.guide.document.block.LytSlot;
import com.hfstudio.guidenh.guide.document.interaction.ContentTooltip;
import com.hfstudio.guidenh.guide.document.interaction.GuideTooltip;
import com.hfstudio.guidenh.guide.document.interaction.InteractiveElement;
import com.hfstudio.guidenh.guide.document.interaction.ItemTooltip;
import com.hfstudio.guidenh.guide.document.interaction.TextTooltip;
import com.hfstudio.guidenh.guide.internal.recipe.NeiItemTooltip;
import com.hfstudio.guidenh.guide.internal.screen.GuideIconButton;
import com.hfstudio.guidenh.guide.internal.screen.GuideNavBar;
import com.hfstudio.guidenh.guide.layout.LayoutContext;
import com.hfstudio.guidenh.guide.layout.MinecraftFontMetrics;
import com.hfstudio.guidenh.guide.render.VanillaRenderContext;
import com.hfstudio.guidenh.guide.scene.LytGuidebookScene;
import com.hfstudio.guidenh.guide.ui.GuideUiHost;

public final class GuideScreen extends GuiScreen implements GuideUiHost {

    private static final Logger LOG = LogManager.getLogger("GuideNH/GuideScreen");

    private static final int PANEL_MARGIN = 20;
    private static final int PANEL_PADDING = 8;

    private static final int BG_COLOR = 0xE0101010;
    private static final int BG_BORDER = 0xFF5A5A5A;

    private static final ResourceLocation BG_TEXTURE = new ResourceLocation(
        "guidenh",
        "textures/gui/sprites/background.png");

    public static float BACKGROUND_ALPHA = 0.35f;
    public static int BACKGROUND_DIM_COLOR = 0xC0101018;

    private final MutableGuide guide;
    private PageAnchor currentAnchor;
    @Nullable
    private GuidePage currentPage;
    @Nullable
    private LytDocument document;

    private final Deque<PageAnchor> history = new ArrayDeque<>();
    private final Deque<PageAnchor> forwardHistory = new ArrayDeque<>();

    private int scrollY;
    private int lastLayoutWidth = -1;

    private int panelX, panelY, panelW, panelH;
    private int contentX, contentY, contentW, contentH;

    @Nullable
    private LytGuidebookScene activeScene;

    private boolean draggingScrollbar = false;
    private int scrollbarGrabOffsetY = 0;

    private GuideIconButton btnSearch, btnBack, btnForward, btnFullWidth, btnClose;
    private static final int TOOLBAR_H = 16;
    private static final int TOOLBAR_GAP = 3;
    private boolean fullWidth;

    private final GuideNavBar navBar = new GuideNavBar();
    private final MinecraftFontMetrics layoutFontMetrics = new MinecraftFontMetrics();

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

    private boolean searchOpen = false;
    @Nullable
    private GuiTextField searchField;
    private final List<SearchResult> searchResults = new ArrayList<>();
    private int searchSelected = -1;
    private String currentPageTitle = "";

    private static final int SEARCH_PANEL_W = 260;
    private static final int SEARCH_PANEL_PAD = 8;
    private static final int SEARCH_FIELD_H = 18;
    private static final int SEARCH_ROW_H = 22;
    private static final int SEARCH_MAX_ROWS = 10;

    private static final class SearchResult {

        final PageAnchor anchor;
        @Nullable
        final ItemStack icon;
        final String title;
        final String subtitle;

        SearchResult(PageAnchor anchor, @Nullable ItemStack icon, String title, String subtitle) {
            this.anchor = anchor;
            this.icon = icon;
            this.title = title;
            this.subtitle = subtitle;
        }
    }

    private static final class SceneButtonHit {

        final LytGuidebookScene scene;
        final GuideIconButton.Role role;

        private SceneButtonHit(LytGuidebookScene scene, GuideIconButton.Role role) {
            this.scene = scene;
            this.role = role;
        }
    }

    private static final class DocumentInteractionState {

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

    public void reloadPage() {
        clearInteractionState();
        currentPage = null;
        document = null;
        lastLayoutWidth = -1;
        loadCurrentPage();
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
            btnRight - (16 + TOOLBAR_GAP) * 5 + TOOLBAR_GAP,
            btnY,
            GuideIconButton.Role.SEARCH);
        btnBack.enabled = !history.isEmpty();
        btnForward.enabled = !forwardHistory.isEmpty();
        this.buttonList.add(btnSearch);
        this.buttonList.add(btnBack);
        this.buttonList.add(btnForward);
        this.buttonList.add(btnFullWidth);
        this.buttonList.add(btnClose);
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
            lastLayoutWidth = -1;
            ensureLayout();
            clampScroll();
        } else if (btn == btnSearch) {
            openSearchOverlay();
        }
    }

    private void loadCurrentPage() {
        clearInteractionState();
        try {
            currentPage = guide.getPage(currentAnchor.pageId());
        } catch (Throwable t) {
            LOG.error("Failed to compile guide page {}", currentAnchor.pageId(), t);
            currentPage = null;
        }
        if (currentPage != null) {
            document = currentPage.document();
            lastLayoutWidth = -1;
        } else {
            document = null;
        }
        refreshCurrentPageTitle();
        scrollY = 0;
    }

    private void ensureLayout() {
        if (document == null) return;
        if (lastLayoutWidth != contentW) {
            document.updateLayout(new LayoutContext(layoutFontMetrics), contentW);
            lastLayoutWidth = contentW;
        }
    }

    private void refreshCurrentPageTitle() {
        if (currentAnchor == null) {
            currentPageTitle = "";
            return;
        }

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
    }

    private int getContentHeight() {
        return document != null ? document.getContentHeight() : 0;
    }

    private int getMaxScroll() {
        return Math.max(0, getContentHeight() - contentH);
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
        ensureLayout();
        clampScroll();

        drawRect(panelX, panelY, panelX + panelW, panelY + panelH, BG_COLOR);
        drawBorder(panelX, panelY, panelW, panelH, BG_BORDER);

        drawRect(panelX, panelY + TOOLBAR_H, panelX + panelW, panelY + TOOLBAR_H + 1, 0xFF2A2A2A);

        drawPageTitle();

        updateSceneHover(mouseX, mouseY);

        if (document != null) {
            renderDocument(mouseX, mouseY);
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
        navBar.render(mc, currentAnchor != null ? currentAnchor.pageId() : null, mouseX, mouseY);

        super.drawScreen(mouseX, mouseY, partialTicks);

        drawButtonTooltip(mouseX, mouseY);

        if (searchOpen) {
            drawSearchOverlay(mouseX, mouseY);
        }
    }

    private void drawPageTitle() {
        if (currentAnchor == null) return;
        int reservedRight = (16 + TOOLBAR_GAP) * 5 + PANEL_PADDING + 4;
        int maxW = Math.max(20, panelW - PANEL_PADDING - reservedRight);
        String draw = currentPageTitle;
        if (fontRendererObj.getStringWidth(draw) > maxW) {
            draw = fontRendererObj.trimStringToWidth(draw, maxW - 4) + "\u2026";
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
            for (var a : scene.getAnnotations()) {
                if (a.isHovered() && a.getTooltip() != null) {
                    renderGuideTooltip(a.getTooltip(), mouseX, mouseY);
                    return;
                }
            }
            var hb = scene.getHoveredBlock();
            if (hb != null) {
                String name = blockDisplayName(scene, hb[0], hb[1], hb[2]);
                if (name != null) {
                    drawTooltipText(name, mouseX, mouseY);
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
            var tip = tryGetTooltip(hit.node(), interaction.docX, interaction.docY);
            tip.ifPresent(t -> renderGuideTooltip(t, mouseX, mouseY));
        }
    }

    private static Optional<GuideTooltip> tryGetTooltip(Object obj, int x, int y) {
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
            var stack = it.getStack();
            if (stack == null || stack.stackSize == 0) return;
            List<String> lines;
            try {
                lines = stack.getTooltip(mc.thePlayer, mc.gameSettings.advancedItemTooltips);
            } catch (Throwable t) {
                lines = new ArrayList<>();
                lines.add(stack.getDisplayName());
            }
            var rarity = stack.getRarity();
            if (!lines.isEmpty() && rarity != null) {
                lines.set(0, rarity.rarityColor.toString() + lines.get(0));
            }
            for (int i = 1; i < lines.size(); i++) {
                lines.set(i, EnumChatFormatting.GRAY + lines.get(i));
            }
            if (tooltip instanceof NeiItemTooltip nit) {
                nit.appendExtraLines(lines);
            }
            drawHoveringText(lines, mouseX, mouseY, mc.fontRenderer);
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

    private void drawContentTooltip(ContentTooltip ct, int mouseX, int mouseY) {
        int pad = 4;
        int maxW = Math.max(80, this.width / 2);
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
        }
    }

    private void drawTooltipText(String text, int mouseX, int mouseY) {
        FontRenderer fr = mc.fontRenderer;
        String norm = (text.indexOf('\\') >= 0) ? text.replace("\\n", "\n") : text;
        String[] lines = norm.split("\n", -1);
        int tw = 0;
        for (String ln : lines) tw = Math.max(tw, fr.getStringWidth(ln));
        int th = lines.length * (fr.FONT_HEIGHT + 1) - 1;
        int pad = 3;
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
        var ctx = reusableRenderCtx;
        ctx.setLightDarkMode(LightDarkMode.LIGHT_MODE);
        cachedViewportRect = cachedRect(cachedViewportRect, 0, scrollY, contentW, contentH);
        cachedScissorRect = cachedRect(cachedScissorRect, contentX, contentY, contentW, contentH);
        ctx.setViewport(cachedViewportRect);
        ctx.setScreenHeight(this.height);
        ctx.setDocumentOrigin(contentX, contentY);
        ctx.setScrollOffsetY(scrollY);

        var interaction = getDocumentInteractionState(mouseX, mouseY);
        document.setHoveredElement(interaction != null ? interaction.hit : null);

        ctx.pushScissor(cachedScissorRect);
        GL11.glPushMatrix();
        GL11.glTranslatef(contentX, contentY - scrollY, 0f);
        try {
            document.render(ctx);
        } catch (Throwable t) {
            LOG.error("Error rendering guide document {}", currentAnchor.pageId(), t);
        } finally {
            GL11.glPopMatrix();
            ctx.popScissor();
        }
    }

    private static LytRect cachedRect(@Nullable LytRect current, int x, int y, int w, int h) {
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
        int barY = contentY;
        int barH = contentH;
        drawRect(barX, barY, barX + barW, barY + barH, 0x40FFFFFF);

        int total = getContentHeight();
        int thumbH = Math.max(16, (int) ((long) barH * contentH / Math.max(1, total)));
        int maxScroll = getMaxScroll();
        int thumbY = maxScroll > 0 ? barY + (int) ((long) (barH - thumbH) * scrollY / maxScroll) : barY;
        int thumbColor = draggingScrollbar ? 0xFFFFFFFF : 0xFFCCCCCC;
        drawRect(barX, thumbY, barX + barW, thumbY + thumbH, thumbColor);
    }

    private int[] scrollbarThumbRect() {
        int barX = panelX + panelW - 6;
        int barY = contentY;
        int barH = contentH;
        int total = getContentHeight();
        int thumbH = Math.max(16, (int) ((long) barH * contentH / Math.max(1, total)));
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
            int mouseX = Mouse.getEventX() * this.width / mc.displayWidth;
            int mouseY = this.height - Mouse.getEventY() * this.height / mc.displayHeight - 1;
            if (navBar.isOpen() && navBar.contains(mouseX, mouseY)) {
                navBar.scroll(dwheel);
                return;
            }
            if (ModConfig.ui.sceneWheelZoom) {
                LytGuidebookScene scene = sceneAt(mouseX, mouseY);
                if (scene != null && scene.isInteractive()) {
                    scene.scroll(dwheel);
                    return;
                }
            }

            int step = GuiScreen.isShiftKeyDown() ? 60 : 20;
            scrollY -= Integer.signum(dwheel) * step;
            clampScroll();
        }
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int button) {
        if (handleSearchMouseClick(mouseX, mouseY, button)) return;
        if (button == 0 && navBar.contains(mouseX, mouseY)) {
            var target = navBar.mouseClicked(mouseX, mouseY);
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
        if (document != null && isInsideContent(mouseX, mouseY)) {
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
            int docY = interaction != null ? interaction.docY : mouseY - contentY + scrollY;
            var hit = interaction != null ? interaction.hit : document.pick(docX, docY);
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
                if (!handled && hit.node() instanceof InteractiveElement ie) {
                    handled = ie.mouseClicked(this, docX, docY, button, false);
                }
                if (handled) {
                    mc.getSoundHandler()
                        .playSound(
                            PositionedSoundRecord.func_147674_a(new ResourceLocation("guidenh:guide_click"), 1.0F));
                    return;
                }
                if (button == 0 || button == 1) {
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
                }
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
        if (activeScene != null) {
            activeScene.drag(mouseX, mouseY);
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
        if (activeScene != null && state != -1) {
            activeScene.endDrag();
            activeScene = null;
            return;
        }
        super.mouseMovedOrUp(mouseX, mouseY, state);
    }

    @Nullable
    private LytGuidebookScene sceneAt(int mouseX, int mouseY) {
        var interaction = getDocumentInteractionState(mouseX, mouseY);
        return interaction != null ? interaction.scene : null;
    }

    private static SceneButtonHit findSceneButtonHit(LytNode node, int mouseX, int mouseY) {
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
    private static LytGuidebookScene findSceneAncestor(@Nullable LytNode node) {
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
            var ann = scene.updateAnnotationHover(mouseX, mouseY);
            if (ann != null) {
                return;
            }
            int[] picked = scene.pickBlock(mouseX, mouseY);
            scene.setHoveredBlock(picked);
        }
    }

    private void clearHoveredScene() {
        if (hoveredScene != null) {
            hoveredScene.setHoveredBlock(null);
            hoveredScene.clearAnnotationHover();
            hoveredScene = null;
        }
    }

    @Nullable
    private DocumentInteractionState getDocumentInteractionState(int mouseX, int mouseY) {
        if (document == null || !isInsideContent(mouseX, mouseY)) {
            return null;
        }
        var interaction = cachedInteractionState;
        if (interaction != null
            && interaction.matches(document, mouseX, mouseY, contentX, contentY, contentW, contentH, scrollY)) {
            return interaction;
        }
        int docX = mouseX - contentX;
        int docY = mouseY - contentY + scrollY;
        var hit = document.pick(docX, docY);
        var sceneButtonHit = findSceneButtonHit(document, mouseX, mouseY);
        var scene = hit != null ? findSceneAncestor(hit.node()) : null;
        if (scene != null && !scene.containsSceneViewport(mouseX, mouseY)) {
            scene = null;
        }
        interaction = new DocumentInteractionState(
            document,
            mouseX,
            mouseY,
            contentX,
            contentY,
            contentW,
            contentH,
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
        if (document != null) {
            document.setHoveredElement(null);
        }
        cachedInteractionState = null;
    }

    @Nullable
    private static String blockDisplayName(LytGuidebookScene scene, int x, int y, int z) {
        try {
            var level = scene.getLevel();
            var block = level.getBlock(x, y, z);
            if (block == null || block == Blocks.air) return null;
            int meta = level.getBlockMetadata(x, y, z);
            var item = Item.getItemFromBlock(block);
            if (item != null) {
                var stack = new ItemStack(item, 1, meta);
                return stack.getDisplayName();
            }
            String unloc = block.getLocalizedName();
            return unloc != null ? unloc : block.getUnlocalizedName();
        } catch (Throwable t) {
            return null;
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) {
        if (handleSearchKey(typedChar, keyCode)) return;
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
            scrollY -= contentH - 20;
            clampScroll();
            return;
        }
        if (keyCode == Keyboard.KEY_NEXT) { // PageDown
            scrollY += contentH - 20;
            clampScroll();
            return;
        }
        super.keyTyped(typedChar, keyCode);
    }

    private boolean isInsideContent(int mouseX, int mouseY) {
        return mouseX >= contentX && mouseX < contentX + contentW && mouseY >= contentY && mouseY < contentY + contentH;
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

    private void openSearchOverlay() {
        searchOpen = true;
        int fw = SEARCH_PANEL_W - SEARCH_PANEL_PAD * 2;
        int fx = (this.width - fw) / 2;
        int fy = 40 + SEARCH_PANEL_PAD;
        searchField = new GuiTextField(this.fontRendererObj, fx, fy, fw, SEARCH_FIELD_H);
        searchField.setMaxStringLength(128);
        searchField.setFocused(true);
        searchField.setText("");
        searchResults.clear();
        searchSelected = -1;
    }

    private void closeSearchOverlay() {
        searchOpen = false;
        searchField = null;
        searchResults.clear();
        searchSelected = -1;
    }

    @Nullable
    private static ItemStack resolveIcon(@Nullable ResourceLocation id) {
        if (id == null) return null;
        try {
            String key = id.toString();
            Item item = (Item) Item.itemRegistry.getObject(key);
            if (item != null) {
                return new ItemStack(item, 1, 0);
            }
            Block block = (Block) Block.blockRegistry.getObject(key);
            if (block != null && block != Blocks.air) {
                return new ItemStack(block, 1, 0);
            }
        } catch (Throwable ignored) {}
        return null;
    }

    private void rebuildSearchResults() {
        searchResults.clear();
        searchSelected = -1;
        if (searchField == null) return;
        String q = searchField.getText()
            .trim()
            .toLowerCase(Locale.ROOT);
        if (q.isEmpty()) return;
        try {
            var pages = guide.getPages();
            int limit = 30;
            for (var p : pages) {
                String title = null;
                ResourceLocation iconId = null;
                var fm = p.getFrontmatter();
                if (fm != null && fm.navigationEntry() != null) {
                    title = fm.navigationEntry()
                        .title();
                    iconId = fm.navigationEntry()
                        .iconItemId();
                }
                String idStr = p.getId()
                    .toString();
                String idLower = idStr.toLowerCase(Locale.ROOT);
                String titleLower = title != null ? title.toLowerCase(Locale.ROOT) : null;
                String displayTitle = (title != null ? title : idStr);
                if ((titleLower != null && titleLower.contains(q)) || idLower.contains(q)) {
                    var icon = resolveIcon(iconId);
                    String subtitle = (title != null) ? idStr : "";
                    searchResults.add(new SearchResult(new PageAnchor(p.getId(), null), icon, displayTitle, subtitle));
                    if (searchResults.size() >= limit) break;
                }
            }
            if (!searchResults.isEmpty()) {
                searchSelected = 0;
            }
        } catch (Throwable t) {
            LOG.warn("Search failed", t);
        }
    }

    private int searchListY() {
        return (searchField != null ? searchField.yPosition : 0) + SEARCH_FIELD_H + 6;
    }

    private void drawSearchOverlay(int mouseX, int mouseY) {
        drawRect(0, 0, this.width, this.height, 0xC0000000);
        if (searchField == null) return;
        int fx = searchField.xPosition;
        int fy = searchField.yPosition;
        int fw = searchField.width;

        int visibleRows = Math.min(searchResults.size(), SEARCH_MAX_ROWS);
        int listY = searchListY();
        int listH = visibleRows * SEARCH_ROW_H;
        int innerH = SEARCH_FIELD_H + 6 + Math.max(listH, 14);

        int panelX = fx - SEARCH_PANEL_PAD;
        int panelY = fy - SEARCH_PANEL_PAD;
        int panelW = fw + SEARCH_PANEL_PAD * 2;
        int panelH = innerH + SEARCH_PANEL_PAD * 2;

        drawRect(panelX, panelY, panelX + panelW, panelY + panelH, 0xEE181818);
        drawRect(panelX, panelY, panelX + panelW, panelY + 1, 0xFF555555);
        drawRect(panelX, panelY + panelH - 1, panelX + panelW, panelY + panelH, 0xFF555555);
        drawRect(panelX, panelY, panelX + 1, panelY + panelH, 0xFF555555);
        drawRect(panelX + panelW - 1, panelY, panelX + panelW, panelY + panelH, 0xFF555555);

        searchField.drawTextBox();
        if (searchField.getText()
            .isEmpty()) {
            drawString(fontRendererObj, GuidebookText.Search.text() + "...", fx + 4, fy + 5, 0xFF666666);
        }

        int ly = listY;
        for (int i = 0; i < visibleRows; i++) {
            var r = searchResults.get(i);
            boolean hovered = mouseX >= fx && mouseX < fx + fw && mouseY >= ly && mouseY < ly + SEARCH_ROW_H;
            boolean selected = (i == searchSelected);
            if (selected) {
                drawRect(fx, ly, fx + fw, ly + SEARCH_ROW_H, 0x60FFFFFF);
            } else if (hovered) {
                drawRect(fx, ly, fx + fw, ly + SEARCH_ROW_H, 0x30FFFFFF);
            }

            int iconX = fx + 3;
            int iconY = ly + 3;
            if (r.icon != null) {
                drawItemIcon(r.icon, iconX, iconY);
            } else {
                drawRect(iconX, iconY, iconX + 16, iconY + 16, 0xFF2A2A2A);
                drawRect(iconX, iconY, iconX + 16, iconY + 1, 0xFF3A3A3A);
                drawRect(iconX, iconY, iconX + 1, iconY + 16, 0xFF3A3A3A);
            }

            int textX = fx + 24;
            String title = r.title;
            int maxTextW = fw - 28;
            if (fontRendererObj.getStringWidth(title) > maxTextW) {
                title = fontRendererObj.trimStringToWidth(title, maxTextW - 6) + "...";
            }
            drawString(fontRendererObj, title, textX, ly + 3, selected ? 0xFFFFFFFF : 0xFF88BBFF);
            if (!r.subtitle.isEmpty()) {
                String sub = r.subtitle;
                if (fontRendererObj.getStringWidth(sub) > maxTextW) {
                    sub = fontRendererObj.trimStringToWidth(sub, maxTextW - 6) + "...";
                }
                drawString(fontRendererObj, sub, textX, ly + 13, 0xFF888888);
            }
            ly += SEARCH_ROW_H;
        }
        if (searchResults.isEmpty()) {
            String hint = searchField.getText()
                .trim()
                .isEmpty() ? GuidebookText.SearchPlaceholder.text() : GuidebookText.SearchNoMatch.text();
            drawString(fontRendererObj, hint, fx + 2, listY + 2, 0xFF888888);
        }
    }

    private void drawItemIcon(ItemStack stack, int x, int y) {
        try {
            RenderHelper.enableGUIStandardItemLighting();
            GL11.glEnable(GL11.GL_NORMALIZE);
            GL11.glEnable(GL11.GL_DEPTH_TEST);
            RenderItem ri = RenderItem.getInstance();
            ri.renderItemAndEffectIntoGUI(this.fontRendererObj, this.mc.getTextureManager(), stack, x, y);
            RenderHelper.disableStandardItemLighting();
            GL11.glDisable(GL11.GL_DEPTH_TEST);
        } catch (Throwable t) {
            drawRect(x, y, x + 16, y + 16, 0xFF444444);
        }
    }

    private boolean handleSearchMouseClick(int mouseX, int mouseY, int button) {
        if (!searchOpen || searchField == null) return false;
        int fx = searchField.xPosition;
        int fw = searchField.width;
        searchField.mouseClicked(mouseX, mouseY, button);

        int rowIndex = getSearchResultRowAt(mouseX, mouseY, fx, fw);
        if (rowIndex >= 0) {
            navigateToSearchResult(rowIndex);
        }
        return true;
    }

    private int getSearchResultRowAt(int mouseX, int mouseY, int listX, int listWidth) {
        if (mouseX < listX || mouseX >= listX + listWidth) {
            return -1;
        }

        int relativeY = mouseY - searchListY();
        if (relativeY < 0) {
            return -1;
        }

        int rowIndex = relativeY / SEARCH_ROW_H;
        int visibleRows = Math.min(searchResults.size(), SEARCH_MAX_ROWS);
        return rowIndex >= 0 && rowIndex < visibleRows ? rowIndex : -1;
    }

    private void navigateToSearchResult(int index) {
        if (index < 0 || index >= searchResults.size()) return;
        var anchor = searchResults.get(index).anchor;
        closeSearchOverlay();
        navigateTo(anchor);
    }

    private boolean handleSearchKey(char typedChar, int keyCode) {
        if (!searchOpen || searchField == null) return false;
        if (keyCode == Keyboard.KEY_ESCAPE) {
            closeSearchOverlay();
            return true;
        }
        if (keyCode == Keyboard.KEY_RETURN || keyCode == Keyboard.KEY_NUMPADENTER) {
            if (searchSelected >= 0) navigateToSearchResult(searchSelected);
            else closeSearchOverlay();
            return true;
        }
        if (keyCode == Keyboard.KEY_UP) {
            if (!searchResults.isEmpty()) {
                searchSelected = (searchSelected <= 0 ? searchResults.size() : searchSelected) - 1;
            }
            return true;
        }
        if (keyCode == Keyboard.KEY_DOWN) {
            if (!searchResults.isEmpty()) {
                searchSelected = (searchSelected + 1) % searchResults.size();
            }
            return true;
        }
        String before = searchField.getText();
        searchField.textboxKeyTyped(typedChar, keyCode);
        String after = searchField.getText();
        if (!Objects.equals(before, after)) {
            rebuildSearchResults();
        }
        return true;
    }
}
