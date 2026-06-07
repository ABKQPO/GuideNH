package com.hfstudio.guidenh.guide.layout;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;

import com.hfstudio.guidenh.guide.render.GuideFontCompat;
import com.hfstudio.guidenh.guide.style.ResolvedTextStyle;

public class MinecraftFontMetrics implements FontMetrics {

    private final FontRenderer font;

    public MinecraftFontMetrics() {
        this(Minecraft.getMinecraft().fontRenderer);
    }

    public MinecraftFontMetrics(FontRenderer font) {
        this.font = font;
    }

    @Override
    public float getAdvance(int codePoint, ResolvedTextStyle style) {
        boolean bold = style != null && style.bold();
        float raw = GuideFontCompat.getRenderedAdvance(font, codePoint, bold, false);
        if (raw <= 0f) {
            return 0f;
        }
        float scale = style != null ? style.fontScale() : 1f;
        return scale == 1f ? raw : raw * scale;
    }

    @Override
    public int getStringWidth(String text, ResolvedTextStyle style) {
        return GuideFontCompat.getStringWidth(font, text, style);
    }

    @Override
    public float getRenderedAdvance(int codePoint, ResolvedTextStyle style, boolean hasVisibleGlyphBefore) {
        boolean bold = style != null && style.bold();
        float raw = GuideFontCompat.getRenderedAdvance(font, codePoint, bold, hasVisibleGlyphBefore);
        if (raw <= 0f) {
            return 0f;
        }
        float scale = style != null ? style.fontScale() : 1f;
        return scale == 1f ? raw : raw * scale;
    }

    @Override
    public int getLineHeight(ResolvedTextStyle style) {
        if (style == null) {
            return font.FONT_HEIGHT + 1;
        }
        float scale = style.fontScale();
        if (scale == 1f) {
            return font.FONT_HEIGHT + 1;
        }
        return (int) Math.ceil((font.FONT_HEIGHT + 1) * scale);
    }
}
