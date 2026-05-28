package com.hfstudio.guidenh.guide.scene.annotation;

import java.util.List;
import java.util.Locale;

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
import com.hfstudio.guidenh.guide.scene.element.ImportPonderElementCompiler;

/**
 * Speech-bubble text label rendered as a 2D overlay anchored to a world position or at a fixed
 * screen-space offset. Supports optional word-wrapping via a {@code maxWidth} pixel limit, and
 * an optional highlight box (drawn as a companion in-world annotation when used via
 * {@link ImportPonderElementCompiler}).
 *
 * <p>
 * The fade value inherited from {@link OverlayAnnotation} is applied to all drawn colours so
 * the annotation smoothly fades in when its keyframe first becomes active.
 */
public class TextAnnotation extends OverlayAnnotation {

    public static final int PADDING_X = 4;
    public static final int PADDING_Y = 3;
    public static final int LINE_GAP = 2;
    public static final int CONNECTOR_HEIGHT = 6;
    public static final int DEFAULT_BACKGROUND_ALPHA = 0xCC;
    private static final int BACKGROUND_RGB = 0x0E0E20;

    private final Vector3f worldPos;
    private final String text;
    private final int maxWidth;
    private final ColorValue borderColor;
    private final boolean independent;
    private final float screenYOffset;
    private ConnectorSide connectorSide = ConnectorSide.BOTTOM;
    private int connectorOffset;
    private int connectorLength = CONNECTOR_HEIGHT;
    private int backgroundAlpha = DEFAULT_BACKGROUND_ALPHA;

    @Nullable
    private List<String> resolvedLines;
    private int resolvedWrapWidth = Integer.MIN_VALUE;

    @Nullable
    private LytParagraph richContent;

    @Nullable
    private LayoutMeasure cachedMeasure;
    private int cachedMeasureWrapWidth = Integer.MIN_VALUE;

    public TextAnnotation(Vector3f worldPos, String text, int borderArgb) {
        this(worldPos, text, new ConstantColor(borderArgb), 0);
    }

    public TextAnnotation(Vector3f worldPos, String text, int borderArgb, int maxWidth) {
        this(worldPos, text, new ConstantColor(borderArgb), maxWidth);
    }

    public TextAnnotation(Vector3f worldPos, String text, ColorValue borderColor) {
        this(worldPos, text, borderColor, 0);
    }

    public TextAnnotation(Vector3f worldPos, String text, ColorValue borderColor, int maxWidth) {
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
    public TextAnnotation(String text, int borderArgb, float screenYOffset, int maxWidth) {
        this(text, new ConstantColor(borderArgb), screenYOffset, maxWidth);
    }

    public TextAnnotation(String text, ColorValue borderColor, float screenYOffset, int maxWidth) {
        this.worldPos = new Vector3f(0, 0, 0);
        this.text = text;
        this.borderColor = borderColor;
        this.maxWidth = maxWidth;
        this.independent = true;
        this.screenYOffset = screenYOffset;
    }

    /** Convenience constructor for independent mode with no word-wrapping. */
    public TextAnnotation(String text, int borderArgb, float screenYOffset) {
        this(text, new ConstantColor(borderArgb), screenYOffset, 0);
    }

    public TextAnnotation(String text, ColorValue borderColor, float screenYOffset) {
        this(text, borderColor, screenYOffset, 0);
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
        this.cachedMeasure = null;
        this.cachedMeasureWrapWidth = Integer.MIN_VALUE;
    }

    public int getBackgroundAlpha() {
        return backgroundAlpha;
    }

    public void setBackgroundAlpha(int backgroundAlpha) {
        this.backgroundAlpha = clampAlpha(backgroundAlpha);
    }

    public void setConnector(ConnectorSide connectorSide, int connectorOffset, int connectorLength) {
        this.connectorSide = connectorSide != null ? connectorSide : ConnectorSide.BOTTOM;
        this.connectorOffset = connectorOffset;
        this.connectorLength = Math.max(0, connectorLength);
    }

    public ColorValue getBorderColor() {
        return borderColor;
    }

    public Vector3f getWorldPos() {
        return worldPos;
    }

    public boolean isIndependent() {
        return independent;
    }

    public String getText() {
        return text;
    }

    public int getMaxWidth() {
        return maxWidth;
    }

    public float getScreenYOffset() {
        return screenYOffset;
    }

    public ConnectorSide getConnectorSide() {
        return connectorSide;
    }

    public int getConnectorOffset() {
        return connectorOffset;
    }

    public int getConnectorLength() {
        return connectorLength;
    }

    @Nullable
    public LytParagraph getRichContent() {
        return richContent;
    }

    private List<String> getLines(FontRenderer fr, int contentWidth) {
        if (resolvedLines != null && resolvedWrapWidth == contentWidth) {
            return resolvedLines;
        }
        if (text == null || text.isEmpty()) {
            resolvedLines = List.of();
            resolvedWrapWidth = contentWidth;
            return resolvedLines;
        }
        if (contentWidth <= 0) {
            resolvedLines = List.of(text);
            resolvedWrapWidth = contentWidth;
            return resolvedLines;
        }
        List<String> wrapped = fr.listFormattedStringToWidth(text, contentWidth);
        resolvedLines = wrapped.isEmpty() ? List.of(text) : wrapped;
        resolvedWrapWidth = contentWidth;
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
        LayoutMeasure measure = measureLayout(viewport.width());
        return constrainBubbleRect(bubbleRect(cx, cy, measure.boxWidth(), measure.boxHeight()), viewport);
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

        LytRect localViewport = new LytRect(
            viewport.x() - docOx,
            viewport.y() - docOy + scroll,
            viewport.width(),
            viewport.height());
        float fade = getFade();
        int borderArgb = borderColor.resolve(context.lightDarkMode());
        LayoutMeasure measure = measureLayout(localViewport.width());

        if (richContent != null) {
            LytRect bubble = constrainBubbleRect(
                bubbleRect(cx, cy, measure.boxWidth(), measure.boxHeight()),
                localViewport);
            int bx = bubble.x();
            int by = bubble.y();
            LayoutContext layoutContext = new LayoutContext(new MinecraftFontMetrics());

            GL11.glDisable(GL11.GL_DEPTH_TEST);
            GL11.glDisable(GL11.GL_TEXTURE_2D);
            GL11.glEnable(GL11.GL_BLEND);
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

            drawFilledRect(
                bx - 1,
                by - 1,
                bx + measure.boxWidth() + 1,
                by + measure.boxHeight() + 1,
                applyFade(borderArgb, fade));
            drawFilledRect(
                bx,
                by,
                bx + measure.boxWidth(),
                by + measure.boxHeight(),
                applyFade(getBackgroundArgb(), fade));
            drawConnector(cx, cy, bubble, applyFade(borderArgb, fade));

            richContent.layout(layoutContext, bx + PADDING_X, by + PADDING_Y, measure.availableWidth());
            GL11.glEnable(GL11.GL_TEXTURE_2D);
            richContent.render(context);
            GL11.glColor4f(1f, 1f, 1f, 1f);
            GL11.glDisable(GL11.GL_BLEND);
            return;
        }

        Minecraft mc = Minecraft.getMinecraft();
        FontRenderer fr = mc.fontRenderer;
        List<String> lines = measure.lines();
        if (lines.isEmpty()) return;

        LytRect bubble = constrainBubbleRect(
            bubbleRect(cx, cy, measure.boxWidth(), measure.boxHeight()),
            localViewport);
        int bx = bubble.x();
        int by = bubble.y();

        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

        drawFilledRect(
            bx - 1,
            by - 1,
            bx + measure.boxWidth() + 1,
            by + measure.boxHeight() + 1,
            applyFade(borderArgb, fade));
        drawFilledRect(bx, by, bx + measure.boxWidth(), by + measure.boxHeight(), applyFade(getBackgroundArgb(), fade));
        drawConnector(cx, cy, bubble, applyFade(borderArgb, fade));

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

    private int getBackgroundArgb() {
        return (backgroundAlpha << 24) | BACKGROUND_RGB;
    }

    private LayoutMeasure measureLayout(int viewportWidth) {
        int wrapWidth = resolveContentWrapWidth(viewportWidth);
        if (cachedMeasure != null && cachedMeasureWrapWidth == wrapWidth) {
            return cachedMeasure;
        }
        if (richContent != null) {
            LayoutContext layoutContext = new LayoutContext(new MinecraftFontMetrics());
            int availableWidth = wrapWidth > 0 ? wrapWidth : Integer.MAX_VALUE;
            LytRect contentBounds = richContent.layout(layoutContext, 0, 0, availableWidth);
            cachedMeasure = new LayoutMeasure(
                contentBounds.width() + PADDING_X * 2,
                contentBounds.height() + PADDING_Y * 2,
                List.of(),
                availableWidth);
            cachedMeasureWrapWidth = wrapWidth;
            return cachedMeasure;
        }

        Minecraft mc = Minecraft.getMinecraft();
        FontRenderer fr = mc.fontRenderer;
        List<String> lines = getLines(fr, wrapWidth);
        int lineCount = Math.max(1, lines.size());
        int maxLineW = 0;
        for (String line : lines) {
            maxLineW = Math.max(maxLineW, fr.getStringWidth(line));
        }
        cachedMeasure = new LayoutMeasure(
            maxLineW + PADDING_X * 2,
            fr.FONT_HEIGHT * lineCount + LINE_GAP * (lineCount - 1) + PADDING_Y * 2,
            lines,
            wrapWidth);
        cachedMeasureWrapWidth = wrapWidth;
        return cachedMeasure;
    }

    private int resolveContentWrapWidth(int viewportWidth) {
        int viewportContentWidth = Math.max(1, viewportWidth - PADDING_X * 2 - 2);
        if (maxWidth > 0) {
            return Math.max(1, Math.min(maxWidth, viewportContentWidth));
        }
        return viewportContentWidth;
    }

    private LytRect bubbleRect(int anchorX, int anchorY, int boxW, int boxH) {
        return switch (connectorSide) {
            case TOP -> new LytRect(anchorX - boxW / 2 - connectorOffset, anchorY + connectorLength + 1, boxW, boxH);
            case LEFT -> new LytRect(anchorX + connectorLength + 1, anchorY - boxH / 2 - connectorOffset, boxW, boxH);
            case RIGHT -> new LytRect(
                anchorX - boxW - connectorLength - 1,
                anchorY - boxH / 2 - connectorOffset,
                boxW,
                boxH);
            case NONE -> new LytRect(anchorX - boxW / 2 - connectorOffset, anchorY - boxH - 1, boxW, boxH);
            case BOTTOM -> new LytRect(
                anchorX - boxW / 2 - connectorOffset,
                anchorY - boxH - connectorLength - 1,
                boxW,
                boxH);
        };
    }

    private LytRect constrainBubbleRect(LytRect bubble, LytRect viewport) {
        if (bubble == null || viewport == null || viewport.isEmpty()) {
            return bubble;
        }
        int constrainedX = bubble.x();
        int constrainedY = bubble.y();
        int maxX = viewport.right() - bubble.width();
        int maxY = viewport.bottom() - bubble.height();
        constrainedX = Math.max(viewport.x(), Math.min(maxX, constrainedX));
        constrainedY = Math.max(viewport.y(), Math.min(maxY, constrainedY));
        return new LytRect(constrainedX, constrainedY, bubble.width(), bubble.height());
    }

    private void drawConnector(int anchorX, int anchorY, LytRect bubble, int argb) {
        if (independent || connectorSide == ConnectorSide.NONE || connectorLength <= 0) {
            return;
        }
        switch (connectorSide) {
            case TOP -> drawFilledRect(anchorX - 1, anchorY, anchorX + 1, bubble.y(), argb);
            case LEFT -> drawFilledRect(anchorX, anchorY - 1, bubble.x(), anchorY + 1, argb);
            case RIGHT -> drawFilledRect(bubble.right(), anchorY - 1, anchorX, anchorY + 1, argb);
            case BOTTOM -> drawFilledRect(anchorX - 1, bubble.bottom(), anchorX + 1, anchorY, argb);
            case NONE -> {}
        }
    }

    private static int clampAlpha(int value) {
        return Math.max(0, Math.min(255, value));
    }

    private static class LayoutMeasure {

        private final int boxWidth;
        private final int boxHeight;
        private final List<String> lines;
        private final int availableWidth;

        private LayoutMeasure(int boxWidth, int boxHeight, List<String> lines, int availableWidth) {
            this.boxWidth = boxWidth;
            this.boxHeight = boxHeight;
            this.lines = lines;
            this.availableWidth = availableWidth;
        }

        private int boxWidth() {
            return boxWidth;
        }

        private int boxHeight() {
            return boxHeight;
        }

        private List<String> lines() {
            return lines;
        }

        private int availableWidth() {
            return availableWidth;
        }
    }

    public enum ConnectorSide {

        BOTTOM("bottom"),
        TOP("top"),
        LEFT("left"),
        RIGHT("right"),
        NONE("none");

        private final String serializedName;

        ConnectorSide(String serializedName) {
            this.serializedName = serializedName;
        }

        public String serializedName() {
            return serializedName;
        }

        public static ConnectorSide fromSerializedName(String raw) {
            if (raw == null || raw.trim()
                .isEmpty()) {
                return BOTTOM;
            }
            String normalized = raw.trim()
                .toLowerCase(Locale.ROOT);
            for (ConnectorSide side : values()) {
                if (side.serializedName.equals(normalized)) {
                    return side;
                }
            }
            throw new IllegalArgumentException("connectorSide must be bottom, top, left, right, or none.");
        }
    }
}
