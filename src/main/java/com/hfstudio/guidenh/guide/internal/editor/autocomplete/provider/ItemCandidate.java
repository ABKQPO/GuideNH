package com.hfstudio.guidenh.guide.internal.editor.autocomplete.provider;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.entity.RenderItem;
import net.minecraft.item.ItemStack;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

import com.hfstudio.guidenh.guide.render.GuideTextRenderer;

public class ItemCandidate implements AutocompleteCandidate {

    private final String id;
    private final ItemStack stack;
    private static final int ICON_SIZE = 16;
    private static final int TEXT_X = ICON_SIZE + 2;
    private static final int TEXT_COLOR = 0xFFF0F0F0;
    private static final RenderItem renderItem = new RenderItem();

    public ItemCandidate(String id, ItemStack stack) {
        this.id = id;
        this.stack = stack;
    }

    @Override
    public String displayText() {
        return id;
    }

    @Override
    public String replacementText() {
        return id;
    }

    @Override
    public int renderHeight() {
        return 16;
    }

    @Override
    public void render(FontRenderer fontRenderer, int x, int y, int width, boolean hovered) {
        GL11.glPushMatrix();
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        RenderHelper.enableGUIStandardItemLighting();
        GL11.glEnable(GL12.GL_RESCALE_NORMAL);
        renderItem.zLevel = 0;
        // renderItemAndEffectIntoGUI draws in a 16x16 box; center it vertically in our 16px row
        renderItem.renderItemAndEffectIntoGUI(
            Minecraft.getMinecraft().fontRenderer,
            Minecraft.getMinecraft()
                .getTextureManager(),
            stack,
            x,
            y - 1);
        RenderHelper.disableStandardItemLighting();
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glPopMatrix();
        GuideTextRenderer.drawString(fontRenderer, id, x + TEXT_X, y + 4, TEXT_COLOR);
    }
}
