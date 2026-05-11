package com.hfstudio.guidenh.guide.latex;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.client.renderer.Tessellator;

import org.lwjgl.opengl.GL11;
import org.scilab.forge.jlatexmath.ParseException;
import org.scilab.forge.jlatexmath.TeXConstants;
import org.scilab.forge.jlatexmath.TeXFormula;
import org.scilab.forge.jlatexmath.TeXIcon;

import cpw.mods.fml.common.FMLLog;

public class GuideLatexRenderer {

    public static final GuideLatexRenderer INSTANCE = new GuideLatexRenderer();

    private static final int DEFAULT_FILL_COLOR_ARGB = 0xFFFFFFFF;

    /** Calibration formula used to determine a reference character height at a given sourceScale. */
    private static final String CALIBRATION_FORMULA = "x";
    private static final int MAX_REF_HEIGHT_ENTRIES = 16;

    /** Maps sourceScale float (rounded to string) -> calibrated reference height in pixels. */
    private final ConcurrentHashMap<String, Integer> refHeightCache = new ConcurrentHashMap<>();

    protected GuideLatexRenderer() {}

    /**
     * Returns the calibrated reference height (in pixels) for {@code "x"} rendered at
     * {@code sourceScale}. Subsequent calls with the same scale are instant (cached).
     * Safe to call from any thread; does not touch OpenGL.
     *
     * @param sourceScale jlatexmath render size parameter
     * @return pixel height of a lower-case "x" glyph at the given scale
     */
    public int calibrateRefHeight(float sourceScale) {
        String key = GuideLatexTextureCache.buildScaleKey(sourceScale);
        Integer height = refHeightCache.computeIfAbsent(key, k -> {
            try {
                TeXFormula formula = new TeXFormula(CALIBRATION_FORMULA);
                TeXIcon icon = formula.new TeXIconBuilder().setStyle(TeXConstants.STYLE_DISPLAY)
                    .setSize(sourceScale)
                    .setFGColor(new Color(DEFAULT_FILL_COLOR_ARGB, true))
                    .build();
                icon.setInsets(new Insets(2, 2, 2, 2));
                int h = icon.getIconHeight();
                return Math.max(1, h);
            } catch (ParseException e) {
                FMLLog.getLogger()
                    .warn("[GuideNH/LaTeX] Failed to calibrate reference height for scale {}", sourceScale, e);
                return 16;
            }
        });
        trimRefHeightCacheIfNeeded();
        return height;
    }

    private void trimRefHeightCacheIfNeeded() {
        if (refHeightCache.size() <= MAX_REF_HEIGHT_ENTRIES) {
            return;
        }
        for (Map.Entry<String, Integer> entry : refHeightCache.entrySet()) {
            if (refHeightCache.size() <= MAX_REF_HEIGHT_ENTRIES) {
                return;
            }
            refHeightCache.remove(entry.getKey(), entry.getValue());
        }
    }

    /**
     * Returns the pixel dimensions {@code [widthPx, heightPx, depthPx]} of {@code formula} rendered at
     * {@code sourceScale}, or {@code null} if the formula is invalid/failed.
     *
     * <p>
     * {@code depthPx} is the typographic depth in jlatexmath pixels, the number of pixels the formula
     * extends <em>below</em> its math baseline (e.g. denominators in fractions). For formulas with no
     * descenders (letters, superscripts) this is {@code 0}.
     *
     * <p>
     * Safe to call from any thread; does NOT upload any OpenGL texture.
     *
     * @param formula       LaTeX source string
     * @param fillColorArgb ARGB colour (only used for cache key uniformity; does not affect size)
     * @param sourceScale   jlatexmath render size parameter
     * @return [widthPx, heightPx, depthPx] or null on parse failure
     */
    public int[] measureSize(String formula, int fillColorArgb, float sourceScale) {
        if (formula == null || formula.isEmpty()) {
            return null;
        }
        if (GuideLatexTextureCache.INSTANCE.hasFailed(formula)) {
            return null;
        }

        String sizeKey = GuideLatexTextureCache.buildSizeCacheKey(formula, sourceScale);
        int[] cached = GuideLatexTextureCache.INSTANCE.getSize(sizeKey);
        if (cached != null) {
            return cached;
        }

        try {
            TeXFormula texFormula = new TeXFormula(formula);
            TeXIcon icon = texFormula.new TeXIconBuilder().setStyle(TeXConstants.STYLE_DISPLAY)
                .setSize(sourceScale)
                .setFGColor(new Color(fillColorArgb, true))
                .build();
            icon.setInsets(new Insets(2, 2, 2, 2));
            int w = icon.getIconWidth();
            int h = icon.getIconHeight();
            int d = getIconDepthPx(icon);
            GuideLatexTextureCache.INSTANCE.putSize(sizeKey, w, h, d);
            return new int[] { w, h, d };
        } catch (ParseException e) {
            FMLLog.getLogger()
                .warn("[GuideNH/LaTeX] Parse error measuring '{}': {}", formula, e.getMessage());
            GuideLatexTextureCache.INSTANCE.markFailed(formula, e.getMessage());
            return null;
        } catch (Exception e) {
            FMLLog.getLogger()
                .warn("[GuideNH/LaTeX] Unexpected error measuring '{}': {}", formula, e.getMessage(), e);
            GuideLatexTextureCache.INSTANCE.markFailed(formula, e.getMessage());
            return null;
        }
    }

    /**
     * Returns (and caches) the OpenGL texture for {@code formula}.
     * Must be called from the Minecraft render thread.
     *
     * @param formula       LaTeX source string
     * @param fillColorArgb ARGB colour for the glyph pixels
     * @param sourceScale   jlatexmath render quality (e.g. 100.0f)
     * @return [textureId, widthPx, heightPx] or null on failure
     */
    public int[] getOrCreateTexture(String formula, int fillColorArgb, float sourceScale) {
        if (formula == null || formula.isEmpty()) {
            return null;
        }
        if (GuideLatexTextureCache.INSTANCE.hasFailed(formula)) {
            return null;
        }

        String texKey = GuideLatexTextureCache.buildTextureCacheKey(formula, fillColorArgb, sourceScale);
        int[] cached = GuideLatexTextureCache.INSTANCE.getTexture(texKey);
        if (cached != null) {
            return cached;
        }

        try {
            TeXFormula texFormula = new TeXFormula(formula);
            TeXIcon icon = texFormula.new TeXIconBuilder().setStyle(TeXConstants.STYLE_DISPLAY)
                .setSize(sourceScale)
                .setFGColor(new Color(fillColorArgb, true))
                .build();
            icon.setInsets(new Insets(2, 2, 2, 2));
            icon.setForeground(new Color(fillColorArgb, true));

            BufferedImage image = renderToImage(icon);
            int w = image.getWidth();
            int h = image.getHeight();

            int textureId = uploadToGL(image, w, h);
            GuideLatexTextureCache.INSTANCE.putTexture(texKey, textureId, w, h);

            String sizeKey = GuideLatexTextureCache.buildSizeCacheKey(formula, sourceScale);
            int d = getIconDepthPx(icon);
            GuideLatexTextureCache.INSTANCE.putSize(sizeKey, w, h, d);

            return new int[] { textureId, w, h };
        } catch (ParseException e) {
            FMLLog.getLogger()
                .warn("[GuideNH/LaTeX] Parse error rendering '{}': {}", formula, e.getMessage());
            GuideLatexTextureCache.INSTANCE.markFailed(formula, e.getMessage());
            return null;
        } catch (Exception e) {
            FMLLog.getLogger()
                .warn("[GuideNH/LaTeX] Unexpected error rendering '{}': {}", formula, e.getMessage(), e);
            GuideLatexTextureCache.INSTANCE.markFailed(
                formula,
                e.getMessage() == null ? e.getClass()
                    .getSimpleName() : e.getMessage());
            return null;
        }
    }

    /**
     * Renders a previously created texture as a quad at the specified screen position.
     * Must be called from the Minecraft render thread.
     *
     * @param x         screen X (document-relative)
     * @param y         screen Y (document-relative)
     * @param displayW  rendered display width in GUI units
     * @param displayH  rendered display height in GUI units
     * @param textureId OpenGL texture ID obtained from {@link #getOrCreateTexture}
     */
    public void renderLatex(int x, int y, int displayW, int displayH, int textureId) {
        GL11.glPushAttrib(GL11.GL_TEXTURE_BIT | GL11.GL_ENABLE_BIT | GL11.GL_COLOR_BUFFER_BIT);
        try {
            GL11.glEnable(GL11.GL_TEXTURE_2D);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureId);
            GL11.glEnable(GL11.GL_BLEND);
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            GL11.glColor4f(1f, 1f, 1f, 1f);

            Tessellator tess = Tessellator.instance;
            tess.startDrawingQuads();
            tess.addVertexWithUV(x, y + displayH, 0, 0.0, 1.0);
            tess.addVertexWithUV(x + displayW, y + displayH, 0, 1.0, 1.0);
            tess.addVertexWithUV(x + displayW, y, 0, 1.0, 0.0);
            tess.addVertexWithUV(x, y, 0, 0.0, 0.0);
            tess.draw();
        } finally {
            GL11.glPopAttrib();
        }
    }

    private static int getIconDepthPx(TeXIcon icon) {
        return Math.max(0, (int) Math.ceil(icon.getTrueIconDepth()));
    }

    private BufferedImage renderToImage(TeXIcon icon) {
        int w = icon.getIconWidth();
        int h = icon.getIconHeight();

        BufferedImage image = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g.setRenderingHint(
                RenderingHints.KEY_ALPHA_INTERPOLATION,
                RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
            g.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_QUALITY);
            g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);

            g.setColor(new Color(0, 0, 0, 0));
            g.fillRect(0, 0, w, h);

            icon.paintIcon(null, g, 0, 0);
        } finally {
            g.dispose();
        }
        return image;
    }

    private static int uploadToGL(BufferedImage image, int w, int h) {
        int[] pixels = new int[w * h];
        image.getRGB(0, 0, w, h, pixels, 0, w);

        ByteBuffer buffer = ByteBuffer.allocateDirect(w * h * 4);
        for (int pixel : pixels) {
            buffer.put((byte) ((pixel >> 16) & 0xFF));
            buffer.put((byte) ((pixel >> 8) & 0xFF));
            buffer.put((byte) (pixel & 0xFF));
            buffer.put((byte) ((pixel >> 24) & 0xFF));
        }
        buffer.flip();

        int textureId = GL11.glGenTextures();
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureId);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL11.GL_CLAMP);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL11.GL_CLAMP);
        GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA8, w, h, 0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, buffer);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
        return textureId;
    }
}
