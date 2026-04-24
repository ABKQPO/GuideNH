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
        if (style != null && style.bold() && raw > 0) {
            raw += 1f;
        }
        return raw * (style != null ? style.fontScale() : 1f);
    }

    @Override
    public int getLineHeight(ResolvedTextStyle style) {
        float scale = style != null ? style.fontScale() : 1f;
        return (int) Math.ceil((font.FONT_HEIGHT + 1) * scale);
    }
}
