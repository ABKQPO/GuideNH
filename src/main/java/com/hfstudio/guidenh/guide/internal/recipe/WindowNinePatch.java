package com.hfstudio.guidenh.guide.internal.recipe;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.util.ResourceLocation;

import org.lwjgl.opengl.GL11;

import com.hfstudio.guidenh.guide.color.LightDarkMode;

/**
 * Draws the {@code window.png} and {@code window_inner.png} 9-slice frames (16x16 source, 4px
 * borders) at an arbitrary target rectangle. Corners keep their size, edges stretch, center
 * stretches. This is the same scheme used by GuideME's {@code PanelBlitter}/{@code GuiAssets}.
 */
public final class WindowNinePatch {

    public static final int TEX_SIZE = 16;
    public static final int BORDER = 4;

    private static final ResourceLocation WINDOW_LIGHT = new ResourceLocation(
        "guidenh",
        "textures/gui/sprites/window.png");
    private static final ResourceLocation WINDOW_DARK = new ResourceLocation(
        "guidenh",
        "textures/gui/sprites/window_darkmode.png");
    private static final ResourceLocation WINDOW_INNER_LIGHT = new ResourceLocation(
        "guidenh",
        "textures/gui/sprites/window_inner.png");
    private static final ResourceLocation WINDOW_INNER_DARK = new ResourceLocation(
        "guidenh",
        "textures/gui/sprites/window_inner_darkmode.png");

    private WindowNinePatch() {}

    public static void drawWindow(LightDarkMode mode, int x, int y, int w, int h) {
        draw(mode == LightDarkMode.DARK_MODE ? WINDOW_DARK : WINDOW_LIGHT, x, y, w, h);
    }

    public static void drawWindowInner(LightDarkMode mode, int x, int y, int w, int h) {
        draw(mode == LightDarkMode.DARK_MODE ? WINDOW_INNER_DARK : WINDOW_INNER_LIGHT, x, y, w, h);
    }

    public static void draw(ResourceLocation texture, int x, int y, int w, int h) {
        if (w < BORDER * 2 || h < BORDER * 2) return;
        Minecraft.getMinecraft()
            .getTextureManager()
            .bindTexture(texture);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glColor4f(1f, 1f, 1f, 1f);

        var tess = Tessellator.instance;
        tess.startDrawingQuads();

        final float t = 1f / TEX_SIZE;
        final float u0 = 0f;
        final float u1 = BORDER * t;
        final float u2 = (TEX_SIZE - BORDER) * t;
        final float u3 = 1f;
        final float v0 = 0f;
        final float v1 = BORDER * t;
        final float v2 = (TEX_SIZE - BORDER) * t;
        final float v3 = 1f;

        int x0 = x;
        int x1 = x + BORDER;
        int x2 = x + w - BORDER;
        int x3 = x + w;
        int y0 = y;
        int y1 = y + BORDER;
        int y2 = y + h - BORDER;
        int y3 = y + h;

        // Corners
        quad(tess, x0, y0, x1, y1, u0, v0, u1, v1);
        quad(tess, x2, y0, x3, y1, u2, v0, u3, v1);
        quad(tess, x0, y2, x1, y3, u0, v2, u1, v3);
        quad(tess, x2, y2, x3, y3, u2, v2, u3, v3);
        // Edges
        quad(tess, x1, y0, x2, y1, u1, v0, u2, v1); // top
        quad(tess, x1, y2, x2, y3, u1, v2, u2, v3); // bottom
        quad(tess, x0, y1, x1, y2, u0, v1, u1, v2); // left
        quad(tess, x2, y1, x3, y2, u2, v1, u3, v2); // right
        // Center
        quad(tess, x1, y1, x2, y2, u1, v1, u2, v2);

        tess.draw();
    }

    private static void quad(Tessellator tess, int x0, int y0, int x1, int y1, float u0, float v0, float u1, float v1) {
        tess.addVertexWithUV(x0, y1, 0, u0, v1);
        tess.addVertexWithUV(x1, y1, 0, u1, v1);
        tess.addVertexWithUV(x1, y0, 0, u1, v0);
        tess.addVertexWithUV(x0, y0, 0, u0, v0);
    }
}
