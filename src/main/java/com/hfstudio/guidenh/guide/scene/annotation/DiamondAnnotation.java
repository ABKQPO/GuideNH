package com.hfstudio.guidenh.guide.scene.annotation;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.util.ResourceLocation;

import org.joml.Vector3f;
import org.lwjgl.opengl.GL11;

import com.hfstudio.guidenh.guide.color.ColorValue;
import com.hfstudio.guidenh.guide.color.ConstantColor;
import com.hfstudio.guidenh.guide.document.LytRect;
import com.hfstudio.guidenh.guide.render.RenderContext;
import com.hfstudio.guidenh.guide.render.VanillaRenderContext;
import com.hfstudio.guidenh.guide.scene.CameraSettings;

public class DiamondAnnotation extends OverlayAnnotation {

    private static final ResourceLocation TEXTURE = new ResourceLocation("guidenh", "textures/guide/diamond.png");

    private static final int SIZE = 16;

    private final Vector3f pos;
    private final ColorValue color;
    private final ColorValue outerColor;
    private boolean alwaysOnTop;

    public DiamondAnnotation(Vector3f pos, ColorValue color) {
        this.pos = pos;
        this.color = color;
        this.outerColor = new ConstantColor(0xFFCCCCCC);
        this.alwaysOnTop = false;
    }

    public Vector3f getPos() {
        return pos;
    }

    public ColorValue getColor() {
        return color;
    }

    public boolean isAlwaysOnTop() {
        return alwaysOnTop;
    }

    public void setAlwaysOnTop(boolean alwaysOnTop) {
        this.alwaysOnTop = alwaysOnTop;
    }

    @Override
    public LytRect getBoundingRect(CameraSettings camera, LytRect viewport) {
        Vector3f screen = camera.worldToScreen(pos.x, pos.y, pos.z);
        int cx = viewport.x() + viewport.width() / 2 + Math.round(screen.x);
        int cy = viewport.y() + viewport.height() / 2 + Math.round(screen.y);
        return new LytRect(cx - SIZE / 2, cy - SIZE / 2, SIZE, SIZE);
    }

    @Override
    public void render(CameraSettings camera, RenderContext context, LytRect viewport) {
        var rect = getBoundingRect(camera, viewport);
        int docOx = 0, docOy = 0, scroll = 0;
        if (context instanceof VanillaRenderContext vrc) {
            docOx = vrc.getDocumentOriginX();
            docOy = vrc.getDocumentOriginY();
            scroll = vrc.getScrollOffsetY();
        }
        LytRect drawRect = new LytRect(rect.x() - docOx, rect.y() - docOy + scroll, rect.width(), rect.height());

        int outerArgb = outerColor.resolve(context.lightDarkMode());
        int innerArgb = color.resolve(context.lightDarkMode());
        if (isHovered()) {
            outerArgb = lighten(outerArgb, 20);
            innerArgb = lighten(innerArgb, 20);
        }

        OpenGlHelper.setActiveTexture(OpenGlHelper.defaultTexUnit);
        Minecraft.getMinecraft()
            .getTextureManager()
            .bindTexture(TEXTURE);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_ALPHA_TEST);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glDisable(GL11.GL_LIGHTING);

        drawTintedQuad(drawRect, outerArgb, 0f, 0f, 0.5f, 1f);
        drawTintedQuad(drawRect, innerArgb, 0.5f, 0f, 1f, 1f);

        if (isHovered()) {
            drawTintedQuad(drawRect, 0x4CFFFFFF, 0f, 0f, 0.5f, 1f);
        }

        GL11.glColor4f(1f, 1f, 1f, 1f);
    }

    private static void drawTintedQuad(LytRect rect, int argb, float u0, float v0, float u1, float v1) {
        float a = ((argb >>> 24) & 0xFF) / 255f;
        float r = ((argb >>> 16) & 0xFF) / 255f;
        float g = ((argb >>> 8) & 0xFF) / 255f;
        float b = (argb & 0xFF) / 255f;
        GL11.glColor4f(r, g, b, a);
        int x = rect.x();
        int y = rect.y();
        int w = rect.width();
        int h = rect.height();
        var tess = Tessellator.instance;
        tess.startDrawingQuads();
        tess.addVertexWithUV(x, y + h, 0, u0, v1);
        tess.addVertexWithUV(x + w, y + h, 0, u1, v1);
        tess.addVertexWithUV(x + w, y, 0, u1, v0);
        tess.addVertexWithUV(x, y, 0, u0, v0);
        tess.draw();
    }

    private static int lighten(int argb, int percent) {
        int a = (argb >>> 24) & 0xFF;
        int r = Math.min(255, ((argb >>> 16) & 0xFF) + percent * 255 / 100);
        int g = Math.min(255, ((argb >>> 8) & 0xFF) + percent * 255 / 100);
        int b = Math.min(255, (argb & 0xFF) + percent * 255 / 100);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }
}
