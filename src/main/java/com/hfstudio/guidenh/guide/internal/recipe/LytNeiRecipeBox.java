package com.hfstudio.guidenh.guide.internal.recipe;

import java.util.List;
import java.util.Optional;

import javax.annotation.Nullable;

import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumChatFormatting;

import org.lwjgl.opengl.GL11;

import com.hfstudio.guidenh.guide.document.LytRect;
import com.hfstudio.guidenh.guide.document.block.LytBlock;
import com.hfstudio.guidenh.guide.document.interaction.GuideTooltip;
import com.hfstudio.guidenh.guide.document.interaction.InteractiveElement;
import com.hfstudio.guidenh.guide.layout.LayoutContext;
import com.hfstudio.guidenh.guide.render.RenderContext;

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
public final class LytNeiRecipeBox extends LytBlock implements InteractiveElement {

    private static final int FRAME_BORDER = 4;
    private static final int ICON_SIZE = 8;
    private static final int TITLE_PAD_TOP = 2;
    private static final int TITLE_PAD_BOTTOM = 2;
    private static final int TITLE_GAP_AFTER_ICON = 3;
    private static final int BODY_MARGIN = 2;
    private static final int SLOT_SIZE = 16;
    private static final int DEFAULT_BODY_HEIGHT = 65;
    private static final int FALLBACK_BODY_WIDTH = 166;

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
    private final int bodyYShift;
    private final int titleHeight;

    public LytNeiRecipeBox(Object handler, int recipeIndex) {
        this.handler = handler;
        this.recipeIndex = recipeIndex;
        this.handlerName = stripFormatting(NeiRecipeLookup.lookupHandlerName(handler));
        ItemStack stack = NeiRecipeLookup.lookupHandlerIcon(handler);
        this.iconStack = stack;
        // Prefer an ItemStack icon when present (classic behaviour); fall back to the
        // DrawableResource the handler may have registered via HandlerInfo.setImage / .setDisplayImage.
        Object img = stack == null ? NeiRecipeLookup.lookupHandlerImage(handler) : null;
        this.iconImage = img;
        this.iconImageW = img != null ? Math.max(1, NeiRecipeLookup.drawableWidth(img)) : 0;
        this.iconImageH = img != null ? Math.max(1, NeiRecipeLookup.drawableHeight(img)) : 0;

        int handlerW = NeiRecipeLookup.lookupHandlerWidth(handler);
        int handlerH = NeiRecipeLookup.lookupHandlerHeight(handler);
        int recipeH = NeiRecipeLookup.lookupRecipeHeight(handler, recipeIndex);
        if (handlerW <= 0) handlerW = FALLBACK_BODY_WIDTH;
        if (handlerH <= 0) handlerH = DEFAULT_BODY_HEIGHT;

        // Respect the handler's declared background size verbatim; tight-fitting by slot bbox
        // caused visible clipping for some handlers and was reverted.
        this.bodyWidth = handlerW;
        this.bodyYShift = Math.max(0, NeiRecipeLookup.lookupHandlerYShift(handler));
        this.bodyHeight = Math.max(1, recipeH > 0 ? recipeH : handlerH);

        int fh = Minecraft.getMinecraft().fontRenderer.FONT_HEIGHT;
        this.titleHeight = Math.max(ICON_SIZE, fh) + TITLE_PAD_TOP + TITLE_PAD_BOTTOM;
    }

    private static String stripFormatting(String s) {
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
        int innerW = Math.max(bodyWidth, iconSize() + (iconSize() > 0 ? TITLE_GAP_AFTER_ICON : 0) + titleTextWidth());
        int w = FRAME_BORDER + innerW + FRAME_BORDER;
        int h = FRAME_BORDER + titleHeight + BODY_MARGIN + bodyHeight + bodyYShift + FRAME_BORDER;
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

        WindowNinePatch.drawWindow(context.lightDarkMode(), x, y, w, h);

        int fh = Minecraft.getMinecraft().fontRenderer.FONT_HEIGHT;
        int innerLeft = x + FRAME_BORDER;
        int innerTop = y + FRAME_BORDER;
        int titleRowTop = innerTop + TITLE_PAD_TOP;
        int iconY = titleRowTop + (Math.max(ICON_SIZE, fh) - ICON_SIZE) / 2;
        if (iconStack != null) {
            drawScaledItem(context, iconStack, innerLeft, iconY, ICON_SIZE);
        } else if (iconImage != null) {
            drawScaledImage(iconImage, innerLeft, iconY, ICON_SIZE, iconImageW, iconImageH);
        }
        if (!handlerName.isEmpty()) {
            int textX = innerLeft + iconSize() + (iconSize() > 0 ? TITLE_GAP_AFTER_ICON : 0);
            int textY = titleRowTop + (Math.max(ICON_SIZE, fh) - fh) / 2;
            Minecraft.getMinecraft().fontRenderer.drawString(handlerName, textX, textY, 0xFF000000);
        }

        int bodyX = innerLeft;
        int bodyY = innerTop + titleHeight + BODY_MARGIN;
        NeiAnimationTicker.ensureUpdating(handler);
        NeiHandlerRenderer.render(handler, recipeIndex, bodyX, bodyY + bodyYShift, -1, -1);
    }

    private static void drawScaledItem(RenderContext context, ItemStack stack, int x, int y, int size) {
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
     * Draw a {@code DrawableResource} scaled to a square of {@code size} pixels, preserving aspect
     * ratio and centering the shorter axis within the icon box. {@code nativeW}/{@code nativeH}
     * come from {@link NeiRecipeLookup#drawableWidth}/{@code drawableHeight} so we avoid another
     * reflective call per frame.
     */
    private static void drawScaledImage(Object image, int x, int y, int size, int nativeW, int nativeH) {
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
            NeiRecipeLookup.drawHandlerImage(image, 0, 0);
        } finally {
            GL11.glPopMatrix();
            // DrawableResource.draw leaves the color/texture state reasonable, but make sure no
            // leftover tint poisons later blits in the same frame.
            GL11.glColor4f(1f, 1f, 1f, 1f);
        }
    }

    @Override
    public Optional<GuideTooltip> getTooltip(float mx, float my) {
        if (!NeiRecipeLookup.isAvailable()) return Optional.empty();
        int px = (int) mx;
        int py = (int) my;

        int bodyX = bounds.x() + FRAME_BORDER;
        int bodyY = bounds.y() + FRAME_BORDER + titleHeight + BODY_MARGIN + bodyYShift;

        ItemStack hit = findSlotHit(NeiRecipeLookup.readIngredientSlots(handler, recipeIndex), bodyX, bodyY, px, py);
        if (hit == null) {
            hit = findSlotHit(NeiRecipeLookup.readOtherSlots(handler, recipeIndex), bodyX, bodyY, px, py);
        }
        if (hit == null) {
            NeiRecipeLookup.Slot result = NeiRecipeLookup.readResultSlot(handler, recipeIndex);
            if (result != null) {
                ItemStack shown = pickVisibleStack(result);
                if (shown != null && isOver(bodyX + result.relx, bodyY + result.rely, SLOT_SIZE, SLOT_SIZE, px, py)) {
                    hit = shown;
                }
            }
        }
        if (hit == null) return Optional.empty();
        return Optional.of(new NeiItemTooltip(hit, handler, recipeIndex));
    }

    private static @Nullable ItemStack findSlotHit(List<NeiRecipeLookup.Slot> slots, int originX, int originY, int px,
        int py) {
        for (NeiRecipeLookup.Slot s : slots) {
            if (!isOver(originX + s.relx, originY + s.rely, SLOT_SIZE, SLOT_SIZE, px, py)) continue;
            ItemStack shown = pickVisibleStack(s);
            if (shown != null) return shown;
        }
        return null;
    }

    private static @Nullable ItemStack pickVisibleStack(NeiRecipeLookup.Slot s) {
        if (s == null || s.stacks == null || s.stacks.isEmpty()) return null;
        for (int i = 0, n = s.stacks.size(); i < n; i++) {
            ItemStack st = s.stacks.get(i);
            if (st != null && st.stackSize > 0) return st;
        }
        return null;
    }

    private static boolean isOver(int x, int y, int w, int h, int px, int py) {
        return px >= x && px < x + w && py >= y && py < y + h;
    }
}
