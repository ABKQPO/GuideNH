package com.hfstudio.guidenh.guide.render;

import net.minecraft.client.gui.FontRenderer;

import com.gtnewhorizon.gtnhlib.util.font.FontRendering;
import com.gtnewhorizon.gtnhlib.util.font.IFontParameters;
import com.hfstudio.guidenh.guide.style.ResolvedTextStyle;

public class GuideFontCompat {

    public static final char FORMATTING_CHAR = '\u00a7';
    private static final String FORMAT_BOLD = "\u00a7l";
    private static final String FORMAT_ITALIC = "\u00a7o";
    private static final String FORMAT_STRIKETHROUGH = "\u00a7m";
    private static final String FORMAT_OBFUSCATED = "\u00a7k";

    protected GuideFontCompat() {}

    public static String preprocessText(String text) {
        return text == null ? null : FontRendering.preprocessText(text);
    }

    public static String buildStyledText(String text, ResolvedTextStyle style) {
        if (text == null || text.isEmpty() || style == null) {
            return text;
        }
        StringBuilder builder = null;
        if (style.bold() || style.italic() || style.strikethrough() || style.obfuscated()) {
            builder = new StringBuilder(text.length() + 8);
            if (style.bold()) {
                builder.append(FORMAT_BOLD);
            }
            if (style.italic()) {
                builder.append(FORMAT_ITALIC);
            }
            if (style.strikethrough()) {
                builder.append(FORMAT_STRIKETHROUGH);
            }
            if (style.obfuscated()) {
                builder.append(FORMAT_OBFUSCATED);
            }
        }
        return builder != null ? builder.append(text)
            .toString() : text;
    }

    public static String prepareRenderedText(String text, ResolvedTextStyle style) {
        return preprocessText(buildStyledText(text, style));
    }

    public static int getStringWidth(FontRenderer fontRenderer, String text) {
        return FontRendering.getStringWidth(text, fontRenderer);
    }

    public static int getStringWidth(FontRenderer fontRenderer, String text, ResolvedTextStyle style) {
        int rawWidth = getStringWidth(fontRenderer, buildStyledText(text, style));
        if (style != null && style.italic() && text != null && !text.isEmpty()) {
            rawWidth += 2;
        }
        float scale = style != null ? style.fontScale() : 1f;
        return Math.round(rawWidth * scale);
    }

    public static float getCharacterWidth(FontRenderer fontRenderer, int codePoint) {
        char character = codePoint <= Character.MAX_VALUE ? (char) codePoint : '?';
        if (fontRenderer instanceof IFontParameters parameters) {
            return parameters.getCharWidthFine(character);
        }
        return fontRenderer.getCharWidth(character);
    }

    public static float getGlyphSpacing(FontRenderer fontRenderer) {
        if (fontRenderer instanceof IFontParameters parameters) {
            return parameters.getGlyphSpacing();
        }
        return 0f;
    }

    public static float getRenderedAdvance(FontRenderer fontRenderer, int codePoint, boolean bold,
        boolean hasVisibleGlyphBefore) {
        float width = getCharacterWidth(fontRenderer, codePoint);
        if (width <= 0f) {
            return width;
        }
        if (hasVisibleGlyphBefore) {
            width += getGlyphSpacing(fontRenderer);
        }
        if (bold) {
            width += 1f;
        }
        return width;
    }

    public static boolean isFormattingCodeStart(CharSequence text, int index) {
        return text != null && index >= 0 && index + 1 < text.length() && text.charAt(index) == FORMATTING_CHAR;
    }

    public static boolean determineBold(boolean wasBold, char formatChar) {
        char normalized = Character.toLowerCase(formatChar);
        if (normalized == 'l') {
            return true;
        }
        boolean numeric = normalized >= '0' && normalized <= '9';
        boolean color = normalized >= 'a' && normalized <= 'f';
        if (normalized == 'r' || numeric || color) {
            return false;
        }
        return wasBold;
    }
}
