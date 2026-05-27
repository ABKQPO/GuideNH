package com.hfstudio.guidenh.guide.internal.recipe;

import java.util.List;
import java.util.Optional;

import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumChatFormatting;

import org.jetbrains.annotations.Nullable;
import org.lwjgl.opengl.GL11;

import com.hfstudio.guidenh.guide.document.LytRect;
import com.hfstudio.guidenh.guide.document.block.LytBlock;
import com.hfstudio.guidenh.guide.document.interaction.GuideTooltip;
import com.hfstudio.guidenh.guide.document.interaction.InteractiveElement;
import com.hfstudio.guidenh.guide.document.interaction.TextTooltip;
import com.hfstudio.guidenh.guide.internal.GuidebookText;
import com.hfstudio.guidenh.guide.internal.screen.GuideIconButton;
import com.hfstudio.guidenh.guide.layout.LayoutContext;
import com.hfstudio.guidenh.guide.render.RenderContext;
import com.hfstudio.guidenh.guide.render.VanillaRenderContext;
import com.hfstudio.guidenh.guide.ui.GuideUiHost;
import com.hfstudio.guidenh.integration.api.GuideNhIntegrationRegistry;
import com.hfstudio.guidenh.integration.api.RecipeSlot;
import com.hfstudio.guidenh.integration.nei.GuideScreenNeiBridge.EditorAccess;
import com.hfstudio.guidenh.integration.nei.NeiGuideNavigation;
import com.hfstudio.guidenh.integration.neicustomdiagram.NeiCustomDiagramBridge;

/**
 * A document block that frames and renders a single NEI recipe using the handler's own
 * {@code drawBackground/drawForeground/drawExtras}. Layout:
 *
 * <pre>
 *   +-- window.png nine-patch outer frame ----------+
 *   |  [icon12] handlerName                         |  <- title bar
 *   |  +-----------------------------------------+  |
 *   |  |  NEI-drawn recipe background + slots    |  |
 *   |  +-----------------------------------------+  |
 *   +------------------------------------------------+
 * </pre>
 *
 * Handler-reported width is trimmed down to the tight slot bounding box plus a small margin so
 * recipes like shaped 3x3 don't waste ~70px of blank background. The handler icon shows the NEI
 * "recipe pool" category; hovering a slot yields an {@link NeiItemTooltip} carrying the extra
 * tooltip lines that NEI's tab normally contributes. The icon itself has no tooltip.
 */
public class LytNeiRecipeBox extends LytBlock implements InteractiveElement {

    public static final int FRAME_BORDER = 4;
    public static final int ICON_SIZE = 8;
    public static final int TITLE_PAD_TOP = 2;
    public static final int TITLE_PAD_BOTTOM = 2;
    public static final int TITLE_GAP_AFTER_ICON = 3;
    public static final int TITLE_GAP_BEFORE_ACTION = 3;
    public static final int BODY_MARGIN = 2;
    public static final int SLOT_SIZE = 16;
    public static final int DEFAULT_BODY_HEIGHT = 65;
    public static final int FALLBACK_BODY_WIDTH = 166;
    public static final int ACTION_BUTTON_SIZE = 12;
    private static final String GREGTECH_DEFAULT_NEI_HANDLER = "gregtech.nei.GTNEIDefaultHandler";
    private static final int GREGTECH_WINDOW_TOP_BLEED = 11;

    private final Object handler;
    private final int recipeIndex;
    private final String handlerName;
    private final @Nullable ItemStack iconStack;
    /** Raw {@code codechicken.nei.drawable.DrawableResource} (kept opaque via Object). */
    private final @Nullable Object iconImage;
    private final int iconImageW;
    private final int iconImageH;
    private final int bodyWidth;
    private final int bodyHeight;
    private final int bodyTopInset;
    private final int bodyYShift;
    private final int titleHeight;
    private final boolean recipeJumpEnabled;
    private final @Nullable GuideTooltip actionButtonTooltip;
    /**
     * Whether {@code handler.getOtherStacks(recipeIndex)} throws at call-time. When true,
     * {@code drawForeground}/{@code drawExtras} are skipped so that GTNH-NEI's safe-wrapper
     * never logs "Error in getOtherStacks" spam for broken third-party handlers.
     */
    private final boolean otherStacksBroken;
    private boolean actionButtonHovered;

    public LytNeiRecipeBox(Object handler, int recipeIndex) {
        this(handler, recipeIndex, true);
    }

    public LytNeiRecipeBox(Object handler, int recipeIndex, boolean recipeJumpEnabled) {
        this.handler = handler;
        this.recipeIndex = recipeIndex;
        this.recipeJumpEnabled = recipeJumpEnabled;
        GuideNhIntegrationRegistry registry = GuideNhIntegrationRegistry.global();
        this.handlerName = stripFormatting(registry.lookupRecipeHandlerName(handler));
        ItemStack stack = registry.lookupRecipeHandlerIcon(handler);
        this.iconStack = stack;
        // Prefer an ItemStack icon when present (classic behaviour); fall back to the
        // DrawableResource the handler may have registered via HandlerInfo.setImage / .setDisplayImage.
        Object img = stack == null ? registry.lookupRecipeHandlerImage(handler) : null;
        this.iconImage = img;
        this.iconImageW = img != null ? Math.max(1, registry.lookupRecipeDrawableWidth(img)) : 0;
        this.iconImageH = img != null ? Math.max(1, registry.lookupRecipeDrawableHeight(img)) : 0;

        int handlerW = registry.lookupRecipeHandlerWidth(handler);
        int handlerH = registry.lookupRecipeHandlerHeight(handler);
        int recipeH = registry.lookupRecipeHeight(handler, recipeIndex);
        if (handlerW <= 0) handlerW = FALLBACK_BODY_WIDTH;
        if (handlerH <= 0) handlerH = DEFAULT_BODY_HEIGHT;

        // Respect the handler's declared background size verbatim; tight-fitting by slot bbox
        // caused visible clipping for some handlers and was reverted.
        this.bodyWidth = handlerW;
        this.bodyYShift = Math.max(0, registry.lookupRecipeHandlerYShift(handler));
        this.bodyTopInset = resolveBodyTopInset(
            handler.getClass()
                .getName(),
            bodyYShift);
        this.bodyHeight = NeiRecipeLayoutMetrics.resolveBodyHeight(handlerH, recipeH, DEFAULT_BODY_HEIGHT);

        int fh = Minecraft.getMinecraft().fontRenderer.FONT_HEIGHT;
        this.titleHeight = Math.max(ICON_SIZE, fh) + TITLE_PAD_TOP + TITLE_PAD_BOTTOM;
        this.actionButtonTooltip = recipeJumpEnabled ? new TextTooltip(GuidebookText.OpenRecipeInNei.text()) : null;
        this.otherStacksBroken = registry.recipeOtherStacksThrows(handler, recipeIndex);
    }

    public static String stripFormatting(String s) {
        return s == null ? "" : EnumChatFormatting.getTextWithoutFormattingCodes(s);
    }

    public Object getHandler() {
        return handler;
    }

    public int getRecipeIndex() {
        return recipeIndex;
    }

    @Override
    protected LytRect computeLayout(LayoutContext context, int x, int y, int availableWidth) {
        int titleWidth = iconSize() + (iconSize() > 0 ? TITLE_GAP_AFTER_ICON : 0) + titleTextWidth();
        if (recipeJumpEnabled) {
            titleWidth += TITLE_GAP_BEFORE_ACTION + ACTION_BUTTON_SIZE;
        }
        int innerW = Math.max(bodyWidth, titleWidth);
        int w = FRAME_BORDER + innerW + FRAME_BORDER;
        int h = FRAME_BORDER + titleHeight + BODY_MARGIN + bodyTopInset + bodyHeight + bodyYShift + FRAME_BORDER;
        return new LytRect(x, y, w, h);
    }

    @Override
    protected void onLayoutMoved(int deltaX, int deltaY) {}

    private int iconSize() {
        // HandlerInfo icons sometimes carry stackSize=0; accept any non-null stack.
        // Also reserve icon space when the handler only exposes a DrawableResource image.
        return (iconStack != null || iconImage != null) ? ICON_SIZE : 0;
    }

    private int titleTextWidth() {
        if (handlerName.isEmpty()) return 0;
        return Minecraft.getMinecraft().fontRenderer.getStringWidth(handlerName);
    }

    @Override
    public void render(RenderContext context) {
        int x = bounds.x();
        int y = bounds.y();
        int w = bounds.width();
        int h = bounds.height();

        int fh = Minecraft.getMinecraft().fontRenderer.FONT_HEIGHT;
        int innerLeft = x + FRAME_BORDER;
        int innerTop = y + FRAME_BORDER;
        int titleRowTop = innerTop + TITLE_PAD_TOP;
        int innerRight = x + w - FRAME_BORDER;
        int bodyX = innerLeft;
        int bodyY = innerTop + titleHeight + BODY_MARGIN + bodyTopInset;

        WindowNinePatch.drawWindow(context.lightDarkMode(), x, y, w, h);

        NeiAnimationTicker.ensureUpdating(handler);
        if (NeiCustomDiagramBridge.isDiagramGroupHandler(handler) && context instanceof VanillaRenderContext vrc) {
            LytRect bodyAbs = vrc.toScreenRect(new LytRect(bodyX, bodyY, bodyWidth, bodyHeight));
            NeiCustomDiagramBridge.renderEmbedded(
                handler,
                recipeIndex,
                bodyX,
                bodyY + bodyYShift,
                bodyAbs.x(),
                bodyAbs.y(),
                bodyAbs.width(),
                bodyAbs.height());
        } else {
            NeiHandlerRenderer.render(
                handler,
                recipeIndex,
                bodyX,
                bodyY + bodyYShift,
                bodyX,
                bodyY,
                bodyWidth,
                bodyHeight,
                -1,
                -1,
                otherStacksBroken);
        }
        context.restoreExternalRenderState();
        drawWindowChromeOverlay(context, x, y, w, h, bodyX, bodyY, bodyWidth, bodyHeight);
        drawTitleRow(context, innerLeft, innerRight, titleRowTop, fh);
    }

    private void drawTitleRow(RenderContext context, int innerLeft, int innerRight, int titleRowTop, int fontHeight) {
        int titleContentHeight = Math.max(ICON_SIZE, fontHeight);
        int iconY = titleRowTop + (titleContentHeight - ICON_SIZE) / 2;
        if (iconStack != null) {
            drawScaledItem(context, iconStack, innerLeft, iconY, ICON_SIZE);
        } else if (iconImage != null) {
            drawScaledImage(iconImage, innerLeft, iconY, ICON_SIZE, iconImageW, iconImageH);
        }
        if (!handlerName.isEmpty()) {
            int textX = innerLeft + iconSize() + (iconSize() > 0 ? TITLE_GAP_AFTER_ICON : 0);
            int textY = titleRowTop + (Math.max(ICON_SIZE, fontHeight) - fontHeight) / 2;
            Minecraft.getMinecraft().fontRenderer.drawString(handlerName, textX, textY, 0xFF000000);
        }
        LytRect actionButtonBounds = getActionButtonBounds();
        if (recipeJumpEnabled && actionButtonBounds != null) {
            drawActionButton(actionButtonBounds);
        }
    }

    private void drawActionButton(LytRect buttonBounds) {
        GuideIconButton.drawIcon(
            Minecraft.getMinecraft(),
            GuideIconButton.Role.OPEN_NEI_RECIPE,
            buttonBounds.x(),
            buttonBounds.y(),
            buttonBounds.width(),
            buttonBounds.height(),
            GuideIconButton.resolveIconColor(true, actionButtonHovered, false));
    }

    private void drawWindowChromeOverlay(RenderContext context, int windowX, int windowY, int windowW, int windowH,
        int bodyX, int bodyY, int bodyW, int bodyH) {
        int topHeight = Math.max(0, bodyY - windowY);
        int bottomY = bodyY + bodyH;
        int bottomHeight = Math.max(0, windowY + windowH - bottomY);
        int leftWidth = Math.max(0, bodyX - windowX);
        int rightX = bodyX + bodyW;
        int rightWidth = Math.max(0, windowX + windowW - rightX);

        drawWindowOverlayStrip(
            context,
            windowX,
            windowY,
            windowW,
            windowH,
            new LytRect(windowX, windowY, windowW, topHeight));
        drawWindowOverlayStrip(
            context,
            windowX,
            windowY,
            windowW,
            windowH,
            new LytRect(windowX, bottomY, windowW, bottomHeight));
        drawWindowOverlayStrip(
            context,
            windowX,
            windowY,
            windowW,
            windowH,
            new LytRect(windowX, bodyY, leftWidth, bodyH));
        drawWindowOverlayStrip(
            context,
            windowX,
            windowY,
            windowW,
            windowH,
            new LytRect(rightX, bodyY, rightWidth, bodyH));
    }

    private void drawWindowOverlayStrip(RenderContext context, int windowX, int windowY, int windowW, int windowH,
        LytRect strip) {
        if (strip.width() <= 0 || strip.height() <= 0) {
            return;
        }
        context.pushLocalScissor(strip);
        try {
            WindowNinePatch.drawWindow(context.lightDarkMode(), windowX, windowY, windowW, windowH);
        } finally {
            context.popScissor();
        }
    }

    public static void drawScaledItem(RenderContext context, ItemStack stack, int x, int y, int size) {
        float scale = size / 16f;
        GL11.glPushMatrix();
        try {
            GL11.glTranslatef(x, y, 0f);
            GL11.glScalef(scale, scale, 1f);
            context.renderItem(stack, 0, 0);
        } finally {
            GL11.glPopMatrix();
        }
    }

    /**
     * Draw an opaque recipe handler image scaled to a square of {@code size} pixels, preserving
     * aspect ratio and centering the shorter axis within the icon box.
     */
    public static void drawScaledImage(Object image, int x, int y, int size, int nativeW, int nativeH) {
        if (nativeW <= 0 || nativeH <= 0) return;
        float scale = Math.min(size / (float) nativeW, size / (float) nativeH);
        int drawW = Math.round(nativeW * scale);
        int drawH = Math.round(nativeH * scale);
        int offX = (size - drawW) / 2;
        int offY = (size - drawH) / 2;
        GL11.glPushMatrix();
        try {
            GL11.glTranslatef(x + offX, y + offY, 0f);
            GL11.glScalef(scale, scale, 1f);
            GL11.glColor4f(1f, 1f, 1f, 1f);
            GuideNhIntegrationRegistry.global()
                .drawRecipeDrawable(image, 0, 0);
        } finally {
            GL11.glPopMatrix();
            // DrawableResource.draw leaves the color/texture state reasonable, but make sure no
            // leftover tint poisons later blits in the same frame.
            GL11.glColor4f(1f, 1f, 1f, 1f);
        }
    }

    @Override
    public Optional<GuideTooltip> getTooltip(float mx, float my) {
        GuideNhIntegrationRegistry registry = GuideNhIntegrationRegistry.global();
        actionButtonHovered = isActionButtonHit(mx, my);
        if (!registry.isRecipeIntegrationAvailable()) return Optional.empty();
        int px = (int) mx;
        int py = (int) my;
        if (actionButtonHovered) {
            return Optional.ofNullable(actionButtonTooltip);
        }

        int bodyX = bounds.x() + FRAME_BORDER;
        int bodyTop = bounds.y() + FRAME_BORDER + titleHeight + BODY_MARGIN + bodyTopInset;
        int bodyY = bodyTop + bodyYShift;

        GuideTooltip customDiagramTooltip = NeiCustomDiagramBridge
            .getEmbeddedTooltip(handler, recipeIndex, px - bodyX, py - bodyY);
        if (customDiagramTooltip != null) {
            return Optional.of(customDiagramTooltip);
        }

        ItemStack hit = findSlotHit(registry.readRecipeIngredientSlots(handler, recipeIndex), bodyX, bodyY, px, py);
        if (hit == null) {
            hit = findSlotHit(registry.readRecipeOtherSlots(handler, recipeIndex), bodyX, bodyY, px, py);
        }
        if (hit == null) {
            RecipeSlot result = registry.readRecipeResultSlot(handler, recipeIndex);
            if (result != null) {
                ItemStack shown = pickVisibleStack(result);
                if (shown != null && isOver(bodyX + result.x(), bodyY + result.y(), SLOT_SIZE, SLOT_SIZE, px, py)) {
                    hit = shown;
                }
            }
        }
        if (hit == null) return Optional.empty();
        return Optional.of(new NeiItemTooltip(hit, handler, recipeIndex));
    }

    @Override
    public void onMouseLeave() {
        actionButtonHovered = false;
    }

    @Override
    public boolean mouseClicked(GuideUiHost screen, int x, int y, int button, boolean doubleClick) {
        if (button != 0 || !recipeJumpEnabled || !isActionButtonHit(x, y)) {
            return false;
        }
        EditorAccess editorAccess = screen instanceof EditorAccess access ? access : null;
        return NeiGuideNavigation
            .openExactCraftingRecipe(editorAccess, handler, recipeIndex, resolveDisplayedResultStack());
    }

    public static @Nullable ItemStack findSlotHit(List<RecipeSlot> slots, int originX, int originY, int px, int py) {
        for (RecipeSlot s : slots) {
            if (!isOver(originX + s.x(), originY + s.y(), SLOT_SIZE, SLOT_SIZE, px, py)) continue;
            ItemStack shown = pickVisibleStack(s);
            if (shown != null) return shown;
        }
        return null;
    }

    public static @Nullable ItemStack pickVisibleStack(RecipeSlot s) {
        if (s == null || s.stacks()
            .isEmpty()) return null;
        for (int i = 0, n = s.stacks()
            .size(); i < n; i++) {
            ItemStack st = s.stacks()
                .get(i);
            if (st != null && st.stackSize > 0) return st;
        }
        return null;
    }

    public static boolean isOver(int x, int y, int w, int h, int px, int py) {
        return px >= x && px < x + w && py >= y && py < y + h;
    }

    private boolean isActionButtonHit(float x, float y) {
        LytRect actionButtonBounds = getActionButtonBounds();
        if (actionButtonBounds == null) {
            return false;
        }
        return actionButtonBounds.contains((int) x, (int) y);
    }

    private @Nullable LytRect getActionButtonBounds() {
        if (!recipeJumpEnabled || bounds == null) {
            return null;
        }
        int titleContentHeight = titleHeight - TITLE_PAD_TOP - TITLE_PAD_BOTTOM;
        int buttonX = bounds.x() + bounds.width() - FRAME_BORDER - ACTION_BUTTON_SIZE;
        int buttonY = bounds.y() + FRAME_BORDER + TITLE_PAD_TOP + (titleContentHeight - ACTION_BUTTON_SIZE) / 2;
        return new LytRect(buttonX, buttonY, ACTION_BUTTON_SIZE, ACTION_BUTTON_SIZE);
    }

    private @Nullable ItemStack resolveDisplayedResultStack() {
        RecipeSlot result = GuideNhIntegrationRegistry.global()
            .readRecipeResultSlot(handler, recipeIndex);
        ItemStack stack = pickVisibleStack(result);
        return stack != null ? stack.copy() : null;
    }

    private static int resolveBodyTopInset(String handlerClassName, int bodyYShift) {
        if (!GREGTECH_DEFAULT_NEI_HANDLER.equals(handlerClassName)) return 0;
        return Math.max(0, GREGTECH_WINDOW_TOP_BLEED - Math.max(bodyYShift, 0));
    }
}
