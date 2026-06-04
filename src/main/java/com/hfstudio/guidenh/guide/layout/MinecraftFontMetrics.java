package com.hfstudio.guidenh.guide.layout;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;

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
        char ch = codePoint <= 0xFFFF ? (char) codePoint : '?';
        float raw = font.getCharWidth(ch);
        if (style == null) {
            return raw;
        }
        if (style.bold() && raw > 0) {
            raw += 1f;
        }
        float scale = style.fontScale();
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
