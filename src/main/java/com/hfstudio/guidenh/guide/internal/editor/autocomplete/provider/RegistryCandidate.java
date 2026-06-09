package com.hfstudio.guidenh.guide.internal.editor.autocomplete.provider;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.entity.RenderItem;
import net.minecraft.item.ItemStack;

import org.jetbrains.annotations.Nullable;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

import com.hfstudio.guidenh.guide.render.GuideTextRenderer;

/** Candidate displaying a registry key with optional item icon and subtitle. */
public class RegistryCandidate implements AutocompleteCandidate {

    private final String key;
    @Nullable
    private final String subtitle;
    @Nullable
    private final ItemStack icon;
    private static final int ICON_SIZE = 16;
    private static final int TEXT_X = ICON_SIZE + 2;
    private static final int TEXT_COLOR = 0xFFF0F0F0;
    private static final int SUBTITLE_COLOR = 0xFFA0A0A0;
    private static final RenderItem renderItem = new RenderItem();

    public RegistryCandidate(String key) {
        this(key, null, null);
    }

    public RegistryCandidate(String key, @Nullable String subtitle) {
        this(key, subtitle, null);
    }

    public RegistryCandidate(String key, @Nullable ItemStack icon) {
        this(key, null, icon);
    }

    public RegistryCandidate(String key, @Nullable String subtitle, @Nullable ItemStack icon) {
        this.key = key;
        this.subtitle = subtitle;
        this.icon = icon;
    }

    @Override
    public String displayText() {
        return key;
    }

    @Override
    public String replacementText() {
        return key;
    }

    @Override
    public int renderHeight() {
        return subtitle != null ? 28 : 16;
    }

    @Override
    public int renderWidth(FontRenderer fr) {
        return icon != null ? ICON_SIZE + 4 + GuideTextRenderer.getStringWidth(fr, key) : 0;
    }

    @Override
    public void render(FontRenderer fontRenderer, int x, int y, int width, boolean hovered) {
        int textX = x;
        if (icon != null) {
            GL11.glPushMatrix();
            GL11.glEnable(GL11.GL_BLEND);
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            RenderHelper.enableGUIStandardItemLighting();
            GL11.glEnable(GL12.GL_RESCALE_NORMAL);
            renderItem.zLevel = 0;
            renderItem.renderItemAndEffectIntoGUI(
                Minecraft.getMinecraft().fontRenderer,
                Minecraft.getMinecraft()
                    .getTextureManager(),
                icon,
                x,
                y - 1);
            RenderHelper.disableStandardItemLighting();
            GL11.glDisable(GL11.GL_BLEND);
            GL11.glPopMatrix();
            textX = x + TEXT_X;
        }
        GuideTextRenderer.drawString(fontRenderer, key, textX, y + 3, TEXT_COLOR);
        if (subtitle != null) {
            GuideTextRenderer.drawString(fontRenderer, subtitle, textX + 4, y + 14, SUBTITLE_COLOR);
        }
    }
}
