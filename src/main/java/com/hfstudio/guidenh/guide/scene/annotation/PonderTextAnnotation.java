package com.hfstudio.guidenh.guide.scene.annotation;

import java.util.Collections;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.Tessellator;

import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL11;

import com.hfstudio.guidenh.guide.color.ColorValue;
import com.hfstudio.guidenh.guide.color.ConstantColor;
import com.hfstudio.guidenh.guide.document.LytRect;
import com.hfstudio.guidenh.guide.document.block.LytParagraph;
import com.hfstudio.guidenh.guide.layout.LayoutContext;
import com.hfstudio.guidenh.guide.layout.MinecraftFontMetrics;
import com.hfstudio.guidenh.guide.render.RenderContext;
import com.hfstudio.guidenh.guide.render.VanillaRenderContext;
import com.hfstudio.guidenh.guide.scene.CameraSettings;

/**
 * Speech-bubble text label rendered as a 2D overlay anchored to a world position or at a fixed
 * screen-space offset. Supports optional word-wrapping via a {@code maxWidth} pixel limit, and
 * an optional highlight box (drawn as a companion in-world annotation when used via
 * {@link com.hfstudio.guidenh.guide.scene.element.ImportPonderElementCompiler}).
 *
 * <p>
 * The fade value inherited from {@link OverlayAnnotation} is applied to all drawn colours so
 * the annotation smoothly fades in when its keyframe first becomes active.
 */
public class PonderTextAnnotation extends OverlayAnnotation {

    public static final int PADDING_X = 4;
    public static final int PADDING_Y = 3;
    public static final int LINE_GAP = 2;
    public static final int CONNECTOR_HEIGHT = 6;
    private static final int BG_ARGB = 0xCC0E0E20;

    private final Vector3f worldPos;
    private final String text;
    private final int maxWidth;
    private final ColorValue borderColor;
    private final boolean independent;
    private final float screenYOffset;

    @Nullable
    private List<String> resolvedLines;

    @Nullable
    private LytParagraph richContent;

    public PonderTextAnnotation(Vector3f worldPos, String text, int borderArgb) {
        this(worldPos, text, new ConstantColor(borderArgb), 0);
    }

    public PonderTextAnnotation(Vector3f worldPos, String text, int borderArgb, int maxWidth) {
        this(worldPos, text, new ConstantColor(borderArgb), maxWidth);
    }

    public PonderTextAnnotation(Vector3f worldPos, String text, ColorValue borderColor) {
        this(worldPos, text, borderColor, 0);
    }

    public PonderTextAnnotation(Vector3f worldPos, String text, ColorValue borderColor, int maxWidth) {
        this.worldPos = worldPos;
        this.text = text;
        this.borderColor = borderColor;
        this.maxWidth = maxWidth;
        this.independent = false;
        this.screenYOffset = 0f;
    }

    /**
     * Creates an independent (screen-space) text annotation positioned at {@code screenYOffset}
     * pixels from the scene centre. An optional {@code maxWidth} &gt; 0 enables word-wrapping.
     */
    public PonderTextAnnotation(String text, int borderArgb, float screenYOffset, int maxWidth) {
        this.worldPos = new Vector3f(0, 0, 0);
        this.text = text;
        this.borderColor = new ConstantColor(borderArgb);
        this.maxWidth = maxWidth;
        this.independent = true;
        this.screenYOffset = screenYOffset;
    }

    /** Convenience constructor for independent mode with no word-wrapping. */
    public PonderTextAnnotation(String text, int borderArgb, float screenYOffset) {
        this(text, borderArgb, screenYOffset, 0);
    }

    /**
     * Attaches a pre-compiled rich-text paragraph to this annotation. When set, the paragraph is
     * used for both size measurement and rendering instead of the plain-text fallback. The paragraph
     * style is forced to white with drop-shadow so it looks consistent on the dark bubble background.
     */
    public void setRichContent(LytParagraph para) {
        para.modifyStyle(
            s -> s.dropShadow(true)
                .color(ConstantColor.WHITE));
        this.richContent = para;
    }

    public Vector3f getWorldPos() {
        return worldPos;
    }

    public String getText() {
        return text;
    }

    @SuppressWarnings("unchecked")
    private List<String> getLines(FontRenderer fr) {
        if (resolvedLines != null) {
            return resolvedLines;
        }
        if (text == null || text.isEmpty()) {
            resolvedLines = Collections.emptyList();
            return resolvedLines;
        }
        if (maxWidth <= 0) {
            resolvedLines = Collections.singletonList(text);
            return resolvedLines;
        }
        List<String> wrapped = fr.listFormattedStringToWidth(text, maxWidth);
        resolvedLines = wrapped.isEmpty() ? Collections.singletonList(text) : wrapped;
        return resolvedLines;
    }

    @Override
    public LytRect getBoundingRect(CameraSettings camera, LytRect viewport) {
        int cx, cy;
        if (independent) {
            cx = viewport.x() + viewport.width() / 2;
            cy = viewport.y() + viewport.height() / 2 + Math.round(screenYOffset);
        } else {
            Vector3f screen = camera.worldToScreen(worldPos.x, worldPos.y, worldPos.z);
            cx = viewport.x() + viewport.width() / 2 + Math.round(screen.x);
            cy = viewport.y() + viewport.height() / 2 + Math.round(screen.y);
        }
        int boxW, boxH;
        if (richContent != null) {
            var lctx = new LayoutContext(new MinecraftFontMetrics());
            int availW = maxWidth > 0 ? maxWidth : 10000;
            LytRect cb = richContent.layout(lctx, 0, 0, availW);
            boxW = cb.width() + PADDING_X * 2;
            boxH = cb.height() + PADDING_Y * 2;
        } else {
            Minecraft mc = Minecraft.getMinecraft();
            FontRenderer fr = mc.fontRenderer;
            List<String> lines = getLines(fr);
            int lineCount = Math.max(1, lines.size());
            int maxLineW = 0;
            for (String line : lines) {
                int w = fr.getStringWidth(line);
                if (w > maxLineW) maxLineW = w;
            }
            boxW = maxLineW + PADDING_X * 2;
            boxH = fr.FONT_HEIGHT * lineCount + LINE_GAP * (lineCount - 1) + PADDING_Y * 2;
        }
        return new LytRect(cx - boxW / 2, cy - boxH - CONNECTOR_HEIGHT - 1, boxW, boxH);
    }

    @Override
    public void render(CameraSettings camera, RenderContext context, LytRect viewport) {
        if (text == null || text.isEmpty()) return;

        int docOx = 0, docOy = 0, scroll = 0;
        if (context instanceof VanillaRenderContext vrc) {
            docOx = vrc.getDocumentOriginX();
            docOy = vrc.getDocumentOriginY();
            scroll = vrc.getScrollOffsetY();
        }

        int cx, cy;
        if (independent) {
            cx = viewport.x() + viewport.width() / 2 - docOx;
            cy = viewport.y() + viewport.height() / 2 + Math.round(screenYOffset) - docOy + scroll;
        } else {
            Vector3f screen = camera.worldToScreen(worldPos.x, worldPos.y, worldPos.z);
            cx = viewport.x() + viewport.width() / 2 + Math.round(screen.x) - docOx;
            cy = viewport.y() + viewport.height() / 2 + Math.round(screen.y) - docOy + scroll;
        }

        float fade = getFade();
        int borderArgb = borderColor.resolve(context.lightDarkMode());

        if (richContent != null) {
            var lctx = new LayoutContext(new MinecraftFontMetrics());
            int availW = maxWidth > 0 ? maxWidth : 10000;
            LytRect cb = richContent.layout(lctx, 0, 0, availW);
            int boxW = cb.width() + PADDING_X * 2;
            int boxH = cb.height() + PADDING_Y * 2;
            int bx = cx - boxW / 2;
            int by = cy - boxH - CONNECTOR_HEIGHT - 1;

            GL11.glDisable(GL11.GL_DEPTH_TEST);
            GL11.glDisable(GL11.GL_TEXTURE_2D);
            GL11.glEnable(GL11.GL_BLEND);
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

            drawFilledRect(bx - 1, by - 1, bx + boxW + 1, by + boxH + 1, applyFade(borderArgb, fade));
            drawFilledRect(bx, by, bx + boxW, by + boxH, applyFade(BG_ARGB, fade));
            if (!independent) {
                drawFilledRect(cx - 1, by + boxH, cx + 1, cy, applyFade(borderArgb, fade));
            }

            richContent.layout(lctx, bx + PADDING_X, by + PADDING_Y, availW);
            GL11.glEnable(GL11.GL_TEXTURE_2D);
            richContent.render(context);
            GL11.glColor4f(1f, 1f, 1f, 1f);
            GL11.glDisable(GL11.GL_BLEND);
            return;
        }

        Minecraft mc = Minecraft.getMinecraft();
        FontRenderer fr = mc.fontRenderer;
        List<String> lines = getLines(fr);
        if (lines.isEmpty()) return;

        int lineCount = lines.size();
        int maxLineW = 0;
        for (String line : lines) {
            int w = fr.getStringWidth(line);
            if (w > maxLineW) maxLineW = w;
        }
        int boxW = maxLineW + PADDING_X * 2;
        int boxH = fr.FONT_HEIGHT * lineCount + LINE_GAP * (lineCount - 1) + PADDING_Y * 2;

        int bx = cx - boxW / 2;
        int by = cy - boxH - CONNECTOR_HEIGHT - 1;

        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

        drawFilledRect(bx - 1, by - 1, bx + boxW + 1, by + boxH + 1, applyFade(borderArgb, fade));
        drawFilledRect(bx, by, bx + boxW, by + boxH, applyFade(BG_ARGB, fade));
        if (!independent) {
            drawFilledRect(cx - 1, by + boxH, cx + 1, cy, applyFade(borderArgb, fade));
        }

        GL11.glEnable(GL11.GL_TEXTURE_2D);

        int textArgb = applyFade(0xFFFFFFFF, fade);
        int lineY = by + PADDING_Y;
        for (String line : lines) {
            fr.drawStringWithShadow(line, bx + PADDING_X, lineY, textArgb);
            lineY += fr.FONT_HEIGHT + LINE_GAP;
        }

        GL11.glColor4f(1f, 1f, 1f, 1f);
        GL11.glDisable(GL11.GL_BLEND);
    }

    public static void drawFilledRect(int x1, int y1, int x2, int y2, int argb) {
        float a = ((argb >>> 24) & 0xFF) / 255f;
        float r = ((argb >>> 16) & 0xFF) / 255f;
        float g = ((argb >>> 8) & 0xFF) / 255f;
        float b = (argb & 0xFF) / 255f;
        GL11.glColor4f(r, g, b, a);
        var tess = Tessellator.instance;
        tess.startDrawingQuads();
        tess.addVertexWithUV(x1, y2, 0, 0, 1);
        tess.addVertexWithUV(x2, y2, 0, 1, 1);
        tess.addVertexWithUV(x2, y1, 0, 1, 0);
        tess.addVertexWithUV(x1, y1, 0, 0, 0);
        tess.draw();
    }
}
