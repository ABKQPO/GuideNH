package com.hfstudio.guidenh.guide.render;

import java.util.ArrayDeque;
import java.util.Deque;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.RenderItem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;

import org.lwjgl.opengl.GL11;

import com.hfstudio.guidenh.guide.color.ColorValue;
import com.hfstudio.guidenh.guide.color.LightDarkMode;
import com.hfstudio.guidenh.guide.document.LytRect;
import com.hfstudio.guidenh.guide.internal.util.DisplayScale;
import com.hfstudio.guidenh.guide.style.ResolvedTextStyle;

public class VanillaRenderContext implements RenderContext {

    public static final RenderItem ITEM_RENDERER = new RenderItem();

    private final FontRenderer fontRenderer;
    private int screenHeight;

    private LightDarkMode lightDarkMode;
    private LytRect viewport;

    private final Deque<LytRect> scissorStack = new ArrayDeque<>();

    // Reuse the style buffer across text segments.
    private final StringBuilder textStyleBuffer = new StringBuilder(32);

    private int documentOriginX = 0;
    private int documentOriginY = 0;

    private int scrollOffsetY = 0;

    public VanillaRenderContext(LightDarkMode mode, LytRect viewport, int screenHeight) {
        this.lightDarkMode = mode;
        this.viewport = viewport;
        this.screenHeight = screenHeight;
        this.fontRenderer = Minecraft.getMinecraft().fontRenderer;
    }

    public void setLightDarkMode(LightDarkMode mode) {
        this.lightDarkMode = mode;
    }

    public void setViewport(LytRect viewport) {
        this.viewport = viewport;
    }

    public void setDocumentOrigin(int absX, int absY) {
        this.documentOriginX = absX;
        this.documentOriginY = absY;
    }

    public int getDocumentOriginX() {
        return documentOriginX;
    }

    @Override
    public int getDocumentOriginY() {
        return documentOriginY;
    }

    public void setScrollOffsetY(int scrollOffsetY) {
        this.scrollOffsetY = scrollOffsetY;
    }

    @Override
    public int getScrollOffsetY() {
        return scrollOffsetY;
    }

    public int getScreenHeight() {
        return screenHeight;
    }

    public void setScreenHeight(int screenHeight) {
        this.screenHeight = screenHeight;
    }

    @Override
    public LightDarkMode lightDarkMode() {
        return lightDarkMode;
    }

    @Override
    public LytRect viewport() {
        return viewport;
    }

    @Override
    public int resolveColor(ColorValue ref) {
        return ref.resolve(lightDarkMode);
    }

    @Override
    public void fillRect(LytRect rect, int argbColor) {
        Gui.drawRect(rect.x(), rect.y(), rect.right(), rect.bottom(), argbColor);
    }

    @Override
    public void drawBorder(LytRect rect, int argbColor, int thickness) {
        Gui.drawRect(rect.x(), rect.y(), rect.right(), rect.y() + thickness, argbColor);
        Gui.drawRect(rect.x(), rect.bottom() - thickness, rect.right(), rect.bottom(), argbColor);
        Gui.drawRect(rect.x(), rect.y() + thickness, rect.x() + thickness, rect.bottom() - thickness, argbColor);
        Gui.drawRect(
            rect.right() - thickness,
            rect.y() + thickness,
            rect.right(),
            rect.bottom() - thickness,
            argbColor);
    }

    @Override
    public void drawText(String text, int x, int y, ResolvedTextStyle style) {
        if (text == null || text.isEmpty()) return;
        int color = resolveColor(style.color());
        if ((color >>> 24) == 0) {
            color |= 0xFF000000;
        }

        StringBuilder sb = null;
        if (style.bold() || style.italic() || style.strikethrough() || style.obfuscated()) {
            sb = textStyleBuffer;
            sb.setLength(0);
            if (style.bold()) sb.append("\u00a7l");
            if (style.italic()) sb.append("\u00a7o");
            if (style.strikethrough()) sb.append("\u00a7m");
            if (style.obfuscated()) sb.append("\u00a7k");
        }
        String drawn = sb != null ? sb.append(text)
            .toString() : text;

        float scale = style.fontScale();
        boolean scaled = Math.abs(scale - 1f) > 1e-4f;
        if (scaled) {
            GL11.glPushMatrix();
            GL11.glTranslatef(x, y, 0f);
            GL11.glScalef(scale, scale, 1f);
            if (style.dropShadow()) {
                fontRenderer.drawStringWithShadow(drawn, 0, 0, color);
            } else {
                fontRenderer.drawString(drawn, 0, 0, color);
            }
            GL11.glPopMatrix();
        } else if (style.dropShadow()) {
            fontRenderer.drawStringWithShadow(drawn, x, y, color);
        } else {
            fontRenderer.drawString(drawn, x, y, color);
        }

        if (style.underlined()) {
            int w = Math.round(fontRenderer.getStringWidth(drawn) * scale);
            int uy = y + Math.round((fontRenderer.FONT_HEIGHT) * scale) - 1;
            Gui.drawRect(x, uy, x + w, uy + 1, color);
        }
        if (style.wavyUnderline()) {
            int w = Math.round(fontRenderer.getStringWidth(drawn) * scale);
            int baseY = y + Math.round((fontRenderer.FONT_HEIGHT) * scale) - 1;
            // Draw a 2px-tall sine-like zig-zag using 1x1 rects: pattern of 4 px period.
            for (int i = 0; i < w; i++) {
                int phase = i & 3; // 0,1,2,3
                int dy = (phase == 0 || phase == 2) ? 0 : (phase == 1 ? -1 : 1);
                Gui.drawRect(x + i, baseY + dy, x + i + 1, baseY + dy + 1, color);
            }
        }
        if (style.dottedUnderline()) {
            int w = Math.round(fontRenderer.getStringWidth(drawn) * scale);
            int dy = y + Math.round((fontRenderer.FONT_HEIGHT) * scale) - 1;
            // Center a single 2x2 dot under each rendered character cell.
            int cursor = 0;
            int len = drawn.length();
            for (int i = 0; i < len; i++) {
                char c = drawn.charAt(i);
                if (c == '\u00a7' && i + 1 < len) {
                    i++;
                    continue;
                }
                int cw = Math.round(fontRenderer.getCharWidth(c) * scale);
                if (cw <= 0) {
                    cursor += cw;
                    continue;
                }
                int dotX = x + cursor + Math.max(0, (cw - 2) / 2);
                Gui.drawRect(dotX, dy, dotX + 2, dy + 2, color);
                cursor += cw;
            }
        }
    }

    @Override
    public int getStringWidth(String text, ResolvedTextStyle style) {
        int raw = fontRenderer.getStringWidth(text);
        if (style != null && style.bold() && text != null) {
            raw += countRenderedChars(text);
        }
        if (style != null && style.italic() && text != null && !text.isEmpty()) {
            raw += 2;
        }
        float scale = style != null ? style.fontScale() : 1f;
        return Math.round(raw * scale);
    }

    public static int countRenderedChars(String text) {
        int n = 0;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '\u00a7' && i + 1 < text.length()) {
                i++;
                continue;
            }
            n++;
        }
        return n;
    }

    @Override
    public int getLineHeight(ResolvedTextStyle style) {
        float scale = style != null ? style.fontScale() : 1f;
        return (int) Math.ceil((fontRenderer.FONT_HEIGHT + 1) * scale);
    }

    @Override
    public void renderItem(ItemStack stack, int x, int y) {
        if (stack == null) return;
        var mc = Minecraft.getMinecraft();
        // Avoid glPushAttrib/glPopAttrib: those are slow on modern drivers (cause pipeline
        // flushes). We instead explicitly restore every state we touch in the finally block.
        try {
            GL11.glDisable(GL11.GL_BLEND);
            GL11.glColor4f(1f, 1f, 1f, 1f);

            RenderHelper.enableGUIStandardItemLighting();
            GL11.glEnable(GL11.GL_LIGHTING);
            GL11.glEnable(GL11.GL_NORMALIZE);
            GL11.glEnable(GL11.GL_DEPTH_TEST);
            GL11.glEnable(GL11.GL_ALPHA_TEST);

            ITEM_RENDERER.zLevel = 100f;
            ITEM_RENDERER.renderItemAndEffectIntoGUI(fontRenderer, mc.getTextureManager(), stack, x, y);
            ITEM_RENDERER.renderItemOverlayIntoGUI(fontRenderer, mc.getTextureManager(), stack, x, y);
            ITEM_RENDERER.zLevel = 0f;
            RenderHelper.disableStandardItemLighting();
        } finally {
            OpenGlHelper.setActiveTexture(OpenGlHelper.defaultTexUnit);
            GL11.glEnable(GL11.GL_TEXTURE_2D);
            GL11.glDisable(GL11.GL_LIGHTING);
            GL11.glDisable(GL11.GL_NORMALIZE);
            GL11.glDisable(GL11.GL_DEPTH_TEST);
            GL11.glDisable(GL11.GL_ALPHA_TEST);
            GL11.glEnable(GL11.GL_BLEND);
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            GL11.glColor4f(1f, 1f, 1f, 1f);
        }
    }

    @Override
    public void blitTexture(ResourceLocation texture, int x, int y, int u, int v, int width, int height) {
        Minecraft.getMinecraft()
            .getTextureManager()
            .bindTexture(texture);
        GL11.glColor4f(1f, 1f, 1f, 1f);
        var tess = Tessellator.instance;
        float texW = 256f;
        float texH = 256f;
        tess.startDrawingQuads();
        tess.addVertexWithUV(x, y + height, 0, u / texW, (v + height) / texH);
        tess.addVertexWithUV(x + width, y + height, 0, (u + width) / texW, (v + height) / texH);
        tess.addVertexWithUV(x + width, y, 0, (u + width) / texW, v / texH);
        tess.addVertexWithUV(x, y, 0, u / texW, v / texH);
        tess.draw();
    }

    @Override
    public void fillTexturedRect(LytRect rect, GuidePageTexture texture) {
        if (texture == null || texture.isMissing()) {
            fillRect(rect, 0xFF333333);
            drawBorder(rect, 0xFFFF00FF, 1);
            return;
        }
        Minecraft.getMinecraft()
            .getTextureManager()
            .bindTexture(texture.getTexture());
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glColor4f(1f, 1f, 1f, 1f);
        int x = rect.x();
        int y = rect.y();
        int w = rect.width();
        int h = rect.height();
        var tess = Tessellator.instance;
        tess.startDrawingQuads();
        tess.addVertexWithUV(x, y + h, 0, 0f, 1f);
        tess.addVertexWithUV(x + w, y + h, 0, 1f, 1f);
        tess.addVertexWithUV(x + w, y, 0, 1f, 0f);
        tess.addVertexWithUV(x, y, 0, 0f, 0f);
        tess.draw();
    }

    @Override
    public void pushScissor(LytRect rect) {
        LytRect effective;
        if (!scissorStack.isEmpty()) {
            var parent = scissorStack.peek();
            int x1 = Math.max(rect.x(), parent.x());
            int y1 = Math.max(rect.y(), parent.y());
            int x2 = Math.min(rect.right(), parent.right());
            int y2 = Math.min(rect.bottom(), parent.bottom());
            int w = Math.max(0, x2 - x1);
            int h = Math.max(0, y2 - y1);
            if (x1 == rect.x() && y1 == rect.y() && w == rect.width() && h == rect.height()) {
                effective = rect;
            } else if (x1 == parent.x() && y1 == parent.y() && w == parent.width() && h == parent.height()) {
                effective = parent;
            } else {
                effective = new LytRect(x1, y1, w, h);
            }
        } else {
            effective = rect;
        }
        scissorStack.push(effective);
        applyScissor(effective);
        GL11.glEnable(GL11.GL_SCISSOR_TEST);
    }

    @Override
    public void popScissor() {
        scissorStack.pop();
        if (!scissorStack.isEmpty()) {
            applyScissor(scissorStack.peek());
        } else {
            GL11.glDisable(GL11.GL_SCISSOR_TEST);
        }
    }

    @Override
    public void restoreExternalRenderState() {
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        OpenGlHelper.setActiveTexture(OpenGlHelper.defaultTexUnit);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glEnable(GL11.GL_ALPHA_TEST);
        GL11.glAlphaFunc(GL11.GL_GREATER, 0.1f);
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glDepthMask(true);
        GL11.glColor4f(1f, 1f, 1f, 1f);
        RenderHelper.disableStandardItemLighting();

        if (!scissorStack.isEmpty()) {
            applyScissor(scissorStack.peek());
            GL11.glEnable(GL11.GL_SCISSOR_TEST);
        } else {
            GL11.glDisable(GL11.GL_SCISSOR_TEST);
        }
    }

    private void applyScissor(LytRect rect) {
        var mc = Minecraft.getMinecraft();
        int scale = DisplayScale.scaleFactor();
        int sx = rect.x() * scale;
        int sy = mc.displayHeight - rect.bottom() * scale;
        int sw = rect.width() * scale;
        int sh = rect.height() * scale;
        GL11.glScissor(sx, Math.max(0, sy), Math.max(0, sw), Math.max(0, sh));
    }

    private static final int CIRCLE_SEGMENTS = 32;

    private static void beginShapeDraw() {
        GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_CURRENT_BIT | GL11.GL_COLOR_BUFFER_BIT);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glDisable(GL11.GL_ALPHA_TEST);
        GL11.glDisable(GL11.GL_CULL_FACE);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
    }

    private static void endShapeDraw() {
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glColor4f(1f, 1f, 1f, 1f);
        GL11.glPopAttrib();
    }

    private static void applyArgb(int argb) {
        int a = (argb >>> 24) & 0xFF;
        int r = (argb >>> 16) & 0xFF;
        int g = (argb >>> 8) & 0xFF;
        int b = argb & 0xFF;
        if (a == 0) {
            a = 0xFF;
        }
        GL11.glColor4f(r / 255f, g / 255f, b / 255f, a / 255f);
    }

    /**
     * Apply color to the Tessellator's per-vertex color channel. Required because Tessellator.draw() in
     * 1.7.10 ignores the global GL color when hasColor is false; we must explicitly set the vertex color
     * via setColorRGBA_I (which is what vanilla Gui.drawRect does).
     */
    private static void tessColor(Tessellator tess, int argb) {
        int a = (argb >>> 24) & 0xFF;
        int r = (argb >>> 16) & 0xFF;
        int g = (argb >>> 8) & 0xFF;
        int b = argb & 0xFF;
        if (a == 0) {
            a = 0xFF;
        }
        tess.setColorRGBA_I((r << 16) | (g << 8) | b, a);
    }

    @Override
    public void drawLine(float x1, float y1, float x2, float y2, float thickness, int argbColor) {
        float dx = x2 - x1;
        float dy = y2 - y1;
        float len = (float) Math.sqrt(dx * dx + dy * dy);
        if (len < 1e-4f) {
            return;
        }
        float half = Math.max(0.5f, thickness * 0.5f);
        float nx = -dy / len * half;
        float ny = dx / len * half;
        beginShapeDraw();
        applyArgb(argbColor);
        var tess = Tessellator.instance;
        tess.startDrawingQuads();
        tessColor(tess, argbColor);
        tess.addVertex(x1 - nx, y1 - ny, 0);
        tess.addVertex(x2 - nx, y2 - ny, 0);
        tess.addVertex(x2 + nx, y2 + ny, 0);
        tess.addVertex(x1 + nx, y1 + ny, 0);
        tess.draw();
        endShapeDraw();
    }

    @Override
    public void fillTriangle(float x1, float y1, float x2, float y2, float x3, float y3, int argbColor) {
        beginShapeDraw();
        applyArgb(argbColor);
        var tess = Tessellator.instance;
        tess.startDrawing(GL11.GL_TRIANGLES);
        tessColor(tess, argbColor);
        tess.addVertex(x1, y1, 0);
        tess.addVertex(x2, y2, 0);
        tess.addVertex(x3, y3, 0);
        tess.draw();
        endShapeDraw();
    }

    @Override
    public void fillPolygon(float[] xs, float[] ys, int argbColor) {
        if (xs == null || ys == null || xs.length < 3 || ys.length < xs.length) {
            return;
        }
        beginShapeDraw();
        applyArgb(argbColor);
        var tess = Tessellator.instance;
        tess.startDrawing(GL11.GL_TRIANGLE_FAN);
        tessColor(tess, argbColor);
        tess.addVertex(xs[0], ys[0], 0);
        for (int i = 0; i < xs.length; i++) {
            tess.addVertex(xs[i], ys[i], 0);
        }
        tess.draw();
        endShapeDraw();
    }

    @Override
    public void fillCircle(float cx, float cy, float radius, int argbColor) {
        if (radius <= 0f) {
            return;
        }
        beginShapeDraw();
        applyArgb(argbColor);
        var tess = Tessellator.instance;
        tess.startDrawing(GL11.GL_TRIANGLE_FAN);
        tessColor(tess, argbColor);
        tess.addVertex(cx, cy, 0);
        for (int i = 0; i <= CIRCLE_SEGMENTS; i++) {
            double a = (Math.PI * 2.0 * i) / CIRCLE_SEGMENTS;
            tess.addVertex(cx + (float) (Math.cos(a) * radius), cy + (float) (Math.sin(a) * radius), 0);
        }
        tess.draw();
        endShapeDraw();
    }

    @Override
    public void drawCircleOutline(float cx, float cy, float radius, float thickness, int argbColor) {
        if (radius <= 0f) {
            return;
        }
        float half = Math.max(0.5f, thickness * 0.5f);
        float inner = Math.max(0f, radius - half);
        float outer = radius + half;
        beginShapeDraw();
        applyArgb(argbColor);
        var tess = Tessellator.instance;
        tess.startDrawing(GL11.GL_TRIANGLE_STRIP);
        tessColor(tess, argbColor);
        for (int i = 0; i <= CIRCLE_SEGMENTS; i++) {
            double a = (Math.PI * 2.0 * i) / CIRCLE_SEGMENTS;
            float cosA = (float) Math.cos(a);
            float sinA = (float) Math.sin(a);
            tess.addVertex(cx + cosA * inner, cy + sinA * inner, 0);
            tess.addVertex(cx + cosA * outer, cy + sinA * outer, 0);
        }
        tess.draw();
        endShapeDraw();
    }
}
