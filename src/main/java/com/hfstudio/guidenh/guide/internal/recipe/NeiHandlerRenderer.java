package com.hfstudio.guidenh.guide.internal.recipe;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.entity.RenderItem;
import net.minecraft.item.ItemStack;

import org.lwjgl.opengl.GL11;

/**
 * Invokes {@link NeiRecipeLookup}-bound handlers to render a recipe in-place. Relies on
 * {@code TemplateRecipeHandler.drawBackground/drawForeground/drawExtras} being independent of
 * {@code GuiRecipe} state (verified against NEI source). All calls are wrapped in try-catch so a
 * misbehaving third-party handler cannot crash the guide UI.
 */
public class NeiHandlerRenderer {

    public static final RenderItem ITEM_RENDERER = new RenderItem();

    private NeiHandlerRenderer() {}

    /**
     * Render the background / foreground / extras for {@code handler} at screen offset
     * {@code (screenX, screenY)} and draw every {@link codechicken.nei.PositionedStack} as a 16x16
     * item on top. Returns the stack under {@code (mouseX, mouseY)} if any, else {@code null}.
     */
    public static @Nullable ItemStack render(Object handler, int recipeIndex, int screenX, int screenY, int mouseX,
        int mouseY) {
        if (handler == null || !NeiRecipeLookup.isAvailable()) return null;

        // Phase 1: NEI-native background + foreground + extras at translated origin.
        GL11.glPushMatrix();
        try {
            GL11.glTranslatef(screenX, screenY, 0f);
            GL11.glColor4f(1f, 1f, 1f, 1f);
            NeiRecipeLookup.callDrawBackground(handler, recipeIndex);
            NeiRecipeLookup.callDrawForeground(handler, recipeIndex);
            NeiRecipeLookup.callDrawExtras(handler, recipeIndex);
        } catch (Throwable ignored) {} finally {
            GL11.glPopMatrix();
        }

        // Phase 2: draw every positioned stack on top.
        ItemStack hovered = null;
        hovered = drawSlots(
            NeiRecipeLookup.readIngredientSlots(handler, recipeIndex),
            screenX,
            screenY,
            mouseX,
            mouseY,
            hovered);
        hovered = drawSlots(
            NeiRecipeLookup.readOtherSlots(handler, recipeIndex),
            screenX,
            screenY,
            mouseX,
            mouseY,
            hovered);

        NeiRecipeLookup.Slot result = NeiRecipeLookup.readResultSlot(handler, recipeIndex);
        if (result != null) {
            ItemStack shown = pickVisibleStack(result);
            if (shown != null) {
                drawItem(shown, screenX + result.relx, screenY + result.rely);
                if (isOver(screenX + result.relx, screenY + result.rely, mouseX, mouseY)) {
                    hovered = shown;
                }
            }
        }
        return hovered;
    }

    public static @Nullable ItemStack drawSlots(List<NeiRecipeLookup.Slot> slots, int screenX, int screenY, int mouseX,
        int mouseY, @Nullable ItemStack currentHovered) {
        ItemStack hovered = currentHovered;
        for (NeiRecipeLookup.Slot s : slots) {
            ItemStack shown = pickVisibleStack(s);
            if (shown == null) continue;
            drawItem(shown, screenX + s.relx, screenY + s.rely);
            if (isOver(screenX + s.relx, screenY + s.rely, mouseX, mouseY)) {
                hovered = shown;
            }
        }
        return hovered;
    }

    public static @Nullable ItemStack pickVisibleStack(NeiRecipeLookup.Slot s) {
        if (s == null || s.stacks == null || s.stacks.isEmpty()) return null;
        // The current ItemStack rotates via handler.onUpdate() which mutates PositionedStack.item;
        // we re-read through the stacks list and just show the first non-empty entry.
        for (int i = 0, n = s.stacks.size(); i < n; i++) {
            ItemStack st = s.stacks.get(i);
            if (st != null && st.stackSize > 0) return st;
        }
        return null;
    }

    public static boolean isOver(int x, int y, int mouseX, int mouseY) {
        return mouseX >= x && mouseX < x + 16 && mouseY >= y && mouseY < y + 16;
    }

    public static void drawItem(ItemStack stack, int x, int y) {
        Minecraft mc = Minecraft.getMinecraft();
        GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_CURRENT_BIT | GL11.GL_COLOR_BUFFER_BIT | GL11.GL_LIGHTING_BIT);
        try {
            GL11.glDisable(GL11.GL_BLEND);
            GL11.glColor4f(1f, 1f, 1f, 1f);
            RenderHelper.enableGUIStandardItemLighting();
            GL11.glEnable(GL11.GL_LIGHTING);
            GL11.glEnable(GL11.GL_NORMALIZE);
            GL11.glEnable(GL11.GL_DEPTH_TEST);
            GL11.glEnable(GL11.GL_ALPHA_TEST);
            ITEM_RENDERER.zLevel = 100f;
            ITEM_RENDERER.renderItemAndEffectIntoGUI(mc.fontRenderer, mc.getTextureManager(), stack, x, y);
            ITEM_RENDERER.renderItemOverlayIntoGUI(mc.fontRenderer, mc.getTextureManager(), stack, x, y);
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
