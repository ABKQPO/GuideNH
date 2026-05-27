package com.hfstudio.guidenh.guide.internal.debug;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraftforge.client.GuiIngameForge;
import net.minecraftforge.client.event.RenderGameOverlayEvent;

import org.lwjgl.opengl.GL11;

import com.hfstudio.guidenh.config.ModConfig;
import com.hfstudio.guidenh.mixins.early.forge.AccessorGuiIngameForge;

public class GuideDebugOverlayRenderer {

    private int lastDisplayWidth = -1;
    private int lastDisplayHeight = -1;
    private int lastGuiScale = Integer.MIN_VALUE;
    private boolean lastUnicode;
    private ScaledResolution cachedScaledResolution;

    public boolean shouldRender(Minecraft minecraft) {
        return ModConfig.debug.enableDebugMode && minecraft != null
            && minecraft.gameSettings != null
            && minecraft.gameSettings.showDebugInfo
            && minecraft.fontRenderer != null
            && minecraft.entityRenderer != null
            && minecraft.thePlayer != null
            && minecraft.theWorld != null;
    }

    public void render(Minecraft minecraft, float partialTicks, int mouseX, int mouseY) {
        if (!shouldRender(minecraft)) {
            return;
        }
        if (!(minecraft.ingameGUI instanceof GuiIngameForge guiIngameForge)) {
            return;
        }

        ScaledResolution scaledResolution = scaledResolution(minecraft);
        RenderGameOverlayEvent eventParent = new RenderGameOverlayEvent(partialTicks, scaledResolution, mouseX, mouseY);
        AccessorGuiIngameForge accessor = (AccessorGuiIngameForge) guiIngameForge;
        ScaledResolution previousScaledResolution = accessor.getScaledResolution();
        FontRenderer previousFontRenderer = accessor.getFontRenderer();
        RenderGameOverlayEvent previousEventParent = accessor.getEventParent();

        GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_COLOR_BUFFER_BIT | GL11.GL_TRANSFORM_BIT);
        GL11.glPushMatrix();
        try {
            minecraft.entityRenderer.setupOverlayRendering();
            GL11.glDisable(GL11.GL_LIGHTING);
            GL11.glDisable(GL11.GL_DEPTH_TEST);
            GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);

            accessor.setScaledResolution(scaledResolution);
            accessor.setFontRenderer(minecraft.fontRenderer);
            accessor.setEventParent(eventParent);
            accessor.invokeRenderHudText(scaledResolution.getScaledWidth(), scaledResolution.getScaledHeight());
        } finally {
            accessor.setScaledResolution(previousScaledResolution);
            accessor.setFontRenderer(previousFontRenderer);
            accessor.setEventParent(previousEventParent);
            GL11.glPopMatrix();
            GL11.glPopAttrib();
        }
    }

    private ScaledResolution scaledResolution(Minecraft minecraft) {
        if (cachedScaledResolution == null || lastDisplayWidth != minecraft.displayWidth
            || lastDisplayHeight != minecraft.displayHeight
            || lastGuiScale != minecraft.gameSettings.guiScale
            || lastUnicode != minecraft.func_152349_b()) {
            cachedScaledResolution = new ScaledResolution(minecraft, minecraft.displayWidth, minecraft.displayHeight);
            lastDisplayWidth = minecraft.displayWidth;
            lastDisplayHeight = minecraft.displayHeight;
            lastGuiScale = minecraft.gameSettings.guiScale;
            lastUnicode = minecraft.func_152349_b();
        }
        return cachedScaledResolution;
    }
}
