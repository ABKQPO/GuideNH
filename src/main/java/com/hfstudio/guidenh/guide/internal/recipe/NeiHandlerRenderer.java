package com.hfstudio.guidenh.guide.internal.recipe;

import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.entity.RenderItem;
import net.minecraft.item.ItemStack;

import org.jetbrains.annotations.Nullable;
import org.lwjgl.opengl.GL11;

import com.hfstudio.guidenh.integration.api.GuideNhIntegrationRegistry;
import com.hfstudio.guidenh.integration.api.RecipeSlot;

/**
 * Invokes registered recipe handlers to render a recipe in-place. Relies on provider-side
 * drawBackground/drawForeground/drawExtras calls being independent of GUI state. All calls are
 * wrapped in try-catch so a misbehaving third-party handler cannot crash the guide UI.
 */
public class NeiHandlerRenderer {

    public static final RenderItem ITEM_RENDERER = new RenderItem();

    private NeiHandlerRenderer() {}

    /**
     * Render the background / foreground / extras for {@code handler} at screen offset
     * {@code (screenX, screenY)} and draw every positioned stack as a 16x16 item on top. Returns
     * the stack under {@code (mouseX, mouseY)} if any, else {@code null}.
     */
    public static @Nullable ItemStack render(Object handler, int recipeIndex, int screenX, int screenY, int clipX,
        int clipY, int clipWidth, int clipHeight, int mouseX, int mouseY) {
        return render(
            handler,
            recipeIndex,
            screenX,
            screenY,
            clipX,
            clipY,
            clipWidth,
            clipHeight,
            mouseX,
            mouseY,
            false);
    }

    /**
     * @param skipForeground when {@code true}, skips {@code drawForeground} and {@code drawExtras}.
     *                       Pass {@code true} for handlers whose {@code getOtherStacks} is known to
     *                       throw; those methods call GTNH-NEI's safe-wrapper internally, which
     *                       would log "Error in getOtherStacks" spam on every rendered frame.
     */
    public static @Nullable ItemStack render(Object handler, int recipeIndex, int screenX, int screenY, int clipX,
        int clipY, int clipWidth, int clipHeight, int mouseX, int mouseY, boolean skipForeground) {
        GuideNhIntegrationRegistry registry = GuideNhIntegrationRegistry.global();
        if (handler == null || !registry.canRenderRecipeHandler(handler)) return null;

        // DiagramGroup is rendered from LytNeiRecipeBox with absolute-GUI scissors (tooltip-safe).

        // Phase 1: NEI-native background + foreground + extras at translated origin.
        // drawForeground and drawExtras are skipped when getOtherStacks is broken, because
        // GTNH-NEI calls getOtherStacks inside its own safe-wrapper from those methods, which
        // would log errors on every render frame.
        GL11.glPushAttrib(
            GL11.GL_ENABLE_BIT | GL11.GL_COLOR_BUFFER_BIT | GL11.GL_CURRENT_BIT | GL11.GL_DEPTH_BUFFER_BIT
                | GL11.GL_LIGHTING_BIT | GL11.GL_SCISSOR_BIT | GL11.GL_TRANSFORM_BIT | GL11.GL_VIEWPORT_BIT);
        GL11.glPushMatrix();
        try {
            GL11.glTranslatef(screenX, screenY, 0f);
            GL11.glColor4f(1f, 1f, 1f, 1f);
            registry.renderRecipeHandler(handler, recipeIndex, skipForeground);
        } catch (Throwable ignored) {} finally {
            GL11.glPopMatrix();
            GL11.glPopAttrib();
            GL11.glColor4f(1f, 1f, 1f, 1f);
            GL11.glDisable(GL11.GL_LIGHTING);
            GL11.glDisable(GL11.GL_DEPTH_TEST);
            GL11.glEnable(GL11.GL_BLEND);
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        }

        // Phase 2: draw every positioned stack on top.
        ItemStack hovered = null;
        hovered = drawSlots(
            registry.readRecipeIngredientSlots(handler, recipeIndex),
            screenX,
            screenY,
            mouseX,
            mouseY,
            hovered);
        if (!skipForeground) {
            hovered = drawSlots(
                registry.readRecipeOtherSlots(handler, recipeIndex),
                screenX,
                screenY,
                mouseX,
                mouseY,
                hovered);
        }

        RecipeSlot result = registry.readRecipeResultSlot(handler, recipeIndex);
        if (result != null) {
            ItemStack shown = pickVisibleStack(result);
            if (shown != null) {
                drawStackWithCount(shown, screenX + result.x(), screenY + result.y());
                if (isOver(screenX + result.x(), screenY + result.y(), mouseX, mouseY)) {
                    hovered = shown;
                }
            }
        }
        return hovered;
    }

    private static @Nullable ItemStack drawSlots(List<RecipeSlot> slots, int screenX, int screenY, int mouseX,
        int mouseY, @Nullable ItemStack currentHovered) {
        ItemStack hovered = currentHovered;
        for (RecipeSlot s : slots) {
            ItemStack shown = pickVisibleStack(s);
            if (shown == null) continue;
            drawStackWithCount(shown, screenX + s.x(), screenY + s.y());
            if (isOver(screenX + s.x(), screenY + s.y(), mouseX, mouseY)) {
                hovered = shown;
            }
        }
        return hovered;
    }

    public static @Nullable ItemStack pickVisibleStack(RecipeSlot s) {
        if (s == null || s.stacks() == null
            || s.stacks()
                .isEmpty())
            return null;
        ItemStack zeroCountFallback = null;
        for (int i = 0, n = s.stacks()
            .size(); i < n; i++) {
            ItemStack st = s.stacks()
                .get(i);
            if (st == null) continue;
            if (st.stackSize > 0) return st;
            if (zeroCountFallback == null) zeroCountFallback = st;
        }
        return zeroCountFallback;
    }

    public static boolean isOver(int x, int y, int mouseX, int mouseY) {
        return mouseX >= x && mouseX < x + 16 && mouseY >= y && mouseY < y + 16;
    }

    /**
     * Draws item icon + count overlay. For stacks with count=0, skips the overlay to avoid
     * rendering an ugly "0" label; use {@link #drawItemIcon} to always suppress the overlay.
     */
    public static void drawItem(ItemStack stack, int x, int y) {
        drawItemInternal(stack, x, y, true);
    }

    /** Draws item icon + count overlay only when count > 0; icon-only for count=0 items. */
    public static void drawStackWithCount(ItemStack stack, int x, int y) {
        drawItemInternal(stack, x, y, stack.stackSize > 0);
    }

    public static void drawItemIcon(ItemStack stack, int x, int y) {
        drawItemInternal(stack, x, y, false);
    }

    private static void drawItemInternal(ItemStack stack, int x, int y, boolean drawOverlay) {
        Minecraft mc = Minecraft.getMinecraft();
        GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_CURRENT_BIT | GL11.GL_COLOR_BUFFER_BIT | GL11.GL_LIGHTING_BIT);
        try {
            GL11.glDisable(GL11.GL_BLEND);
            GL11.glColor4f(1f, 1f, 1f, 1f);
            RenderHelper.enableGUIStandardItemLighting();
            OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240f, 240f);
            OpenGlHelper.setActiveTexture(OpenGlHelper.defaultTexUnit);
            GL11.glEnable(GL11.GL_LIGHTING);
            GL11.glEnable(GL11.GL_NORMALIZE);
            GL11.glEnable(GL11.GL_DEPTH_TEST);
            GL11.glEnable(GL11.GL_ALPHA_TEST);
            ITEM_RENDERER.zLevel = 100f;
            ITEM_RENDERER.renderItemAndEffectIntoGUI(mc.fontRenderer, mc.getTextureManager(), stack, x, y);
            if (drawOverlay) {
                ITEM_RENDERER.renderItemOverlayIntoGUI(mc.fontRenderer, mc.getTextureManager(), stack, x, y);
            }
            ITEM_RENDERER.zLevel = 0f;
            RenderHelper.disableStandardItemLighting();
        } finally {
            GL11.glPopAttrib();
            OpenGlHelper.setActiveTexture(OpenGlHelper.defaultTexUnit);
            GL11.glEnable(GL11.GL_TEXTURE_2D);
            GL11.glDisable(GL11.GL_LIGHTING);
            GL11.glDisable(GL11.GL_DEPTH_TEST);
            GL11.glEnable(GL11.GL_BLEND);
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            GL11.glColor4f(1f, 1f, 1f, 1f);
        }
    }
}
