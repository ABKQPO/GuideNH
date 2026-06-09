package com.hfstudio.guidenh.guide.render.emoji;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;

public class GuideEmojiTextureCache {

    public static final int MAX_CACHE_ENTRIES = 512;
    public static final int GLYPH_IMAGE_SIZE = 48;
    public static final int RENDER_FONT_SIZE = 38;

    private final GuideEmojiFontResolver fontResolver;
    private final Map<String, GuideEmojiGlyph> glyphs = new LinkedHashMap<>(MAX_CACHE_ENTRIES, 0.75f, true) {

        @Override
        protected boolean removeEldestEntry(Entry<String, GuideEmojiGlyph> eldest) {
            return size() > MAX_CACHE_ENTRIES;
        }
    };
    private int nextTextureId;

    public GuideEmojiTextureCache(GuideEmojiFontResolver fontResolver) {
        this.fontResolver = fontResolver;
    }

    public GuideEmojiGlyph getGlyph(String emoji, int vanillaLineHeight) {
        if (emoji == null || emoji.isEmpty()) {
            return unsupported(vanillaLineHeight);
        }
        GuideEmojiGlyph cached = glyphs.get(emoji);
        if (cached != null) {
            return cached;
        }

        var glyph = createGlyph(emoji, vanillaLineHeight);
        glyphs.put(emoji, glyph);
        return glyph;
    }

    public int getAdvance(String emoji, int vanillaLineHeight) {
        var font = fontResolver.resolveFont();
        if (font.isEmpty() || !fontResolver.canDisplay(font.get(), emoji)) {
            return vanillaLineHeight;
        }
        return Math.max(vanillaLineHeight, measureAdvance(font.get(), emoji, vanillaLineHeight));
    }

    public void clear() {
        glyphs.clear();
    }

    private GuideEmojiGlyph createGlyph(String emoji, int vanillaLineHeight) {
        Optional<Font> resolvedFont = fontResolver.resolveFont();
        if (resolvedFont.isEmpty() || !fontResolver.canDisplay(resolvedFont.get(), emoji)) {
            return unsupported(vanillaLineHeight);
        }

        Font font = resolvedFont.get()
            .deriveFont(Font.PLAIN, RENDER_FONT_SIZE);
        var image = new BufferedImage(GLYPH_IMAGE_SIZE, GLYPH_IMAGE_SIZE, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        try {
            applyRenderingHints(graphics);
            graphics.setFont(font);
            FontMetrics metrics = graphics.getFontMetrics();
            int textWidth = Math.max(1, metrics.stringWidth(emoji));
            int x = Math.max(0, (GLYPH_IMAGE_SIZE - textWidth) / 2);
            int y = Math.max(metrics.getAscent(), (GLYPH_IMAGE_SIZE - metrics.getHeight()) / 2 + metrics.getAscent());
            graphics.setColor(Color.WHITE);
            graphics.drawString(emoji, x, y);
        } finally {
            graphics.dispose();
        }

        int drawSize = Math.max(1, vanillaLineHeight);
        var texture = Minecraft.getMinecraft()
            .getTextureManager()
            .getDynamicTextureLocation("guidenh_emoji_" + nextTextureId++, new DynamicTexture(image));
        return new GuideEmojiGlyph(texture, GLYPH_IMAGE_SIZE, GLYPH_IMAGE_SIZE, drawSize, drawSize, 1, true);
    }

    private static GuideEmojiGlyph unsupported(int vanillaLineHeight) {
        int drawSize = Math.max(1, vanillaLineHeight);
        return new GuideEmojiGlyph(null, 1, 1, drawSize, drawSize, 0, false);
    }

    private static int measureAdvance(Font font, String emoji, int fallback) {
        var image = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        try {
            graphics.setFont(font.deriveFont(Font.PLAIN, RENDER_FONT_SIZE));
            int measured = graphics.getFontMetrics()
                .stringWidth(emoji);
            if (measured <= 0) {
                return fallback;
            }
            return Math.max(1, Math.round(measured * (fallback / (float) RENDER_FONT_SIZE)));
        } finally {
            graphics.dispose();
        }
    }

    private static void applyRenderingHints(Graphics2D graphics) {
        graphics
            .setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        graphics.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_QUALITY);
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
    }
}
