package com.hfstudio.guidenh.guide.render;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.Tessellator;

import org.lwjgl.opengl.GL11;

import com.gtnewhorizon.gtnhlib.util.font.FontRendering;
import com.hfstudio.guidenh.guide.render.emoji.GuideEmojiFontResolver;
import com.hfstudio.guidenh.guide.render.emoji.GuideEmojiParser;
import com.hfstudio.guidenh.guide.render.emoji.GuideEmojiRun;
import com.hfstudio.guidenh.guide.render.emoji.GuideEmojiTextureCache;
import com.hfstudio.guidenh.guide.style.ResolvedTextStyle;

public class GuideTextRenderer {

    private static final GuideEmojiParser EMOJI_PARSER = new GuideEmojiParser();
    private static final GuideEmojiFontResolver FONT_RESOLVER = new GuideEmojiFontResolver();
    private static final GuideEmojiTextureCache EMOJI_CACHE = new GuideEmojiTextureCache(FONT_RESOLVER);

    protected GuideTextRenderer() {}

    public static boolean containsEmoji(String text) {
        return EMOJI_PARSER.containsEmoji(text);
    }

    public static int drawString(FontRenderer fontRenderer, String text, int x, int y, int color) {
        return drawString(fontRenderer, text, x, y, color, false);
    }

    public static int drawString(FontRenderer fontRenderer, String text, int x, int y, int color, boolean dropShadow) {
        if (fontRenderer == null || text == null || text.isEmpty()) {
            return 0;
        }
        if (!containsEmoji(text)) {
            return dropShadow ? fontRenderer.drawString(text, x, y, color, true)
                : fontRenderer.drawString(text, x, y, color);
        }
        return Math.round(drawMixedText(fontRenderer, text, x, y, color, dropShadow, 1f));
    }

    public static void drawStyled(FontRenderer fontRenderer, String text, int x, int y, ResolvedTextStyle style,
        int color) {
        if (fontRenderer == null || text == null || text.isEmpty()) {
            return;
        }
        String drawn = GuideFontCompat.prepareRenderedText(text, style);
        float scale = style != null ? style.fontScale() : 1f;
        boolean dropShadow = style != null && style.dropShadow();
        if (!containsEmoji(drawn)) {
            drawVanillaStyled(fontRenderer, drawn, x, y, color, scale, dropShadow);
            return;
        }
        if (Math.abs(scale - 1f) > 1e-4f) {
            GL11.glPushMatrix();
            GL11.glTranslatef(x, y, 0f);
            GL11.glScalef(scale, scale, 1f);
            drawMixedText(fontRenderer, drawn, 0, 0, color, dropShadow, 1f);
            GL11.glPopMatrix();
        } else {
            drawMixedText(fontRenderer, drawn, x, y, color, dropShadow, 1f);
        }
    }

    public static int getStringWidth(FontRenderer fontRenderer, String text) {
        if (fontRenderer == null || text == null || text.isEmpty()) {
            return 0;
        }
        if (!containsEmoji(text)) {
            return FontRendering.getStringWidth(text, fontRenderer);
        }
        return Math.round(measureMixedText(fontRenderer, text));
    }

    public static int getStringWidth(FontRenderer fontRenderer, String text, ResolvedTextStyle style) {
        String styled = GuideFontCompat.buildStyledText(text, style);
        int rawWidth = getStringWidth(fontRenderer, styled);
        if (style != null && style.italic() && text != null && !text.isEmpty()) {
            rawWidth += 2;
        }
        float scale = style != null ? style.fontScale() : 1f;
        return Math.round(rawWidth * scale);
    }

    public static int getPreparedStringWidth(FontRenderer fontRenderer, String preparedText, ResolvedTextStyle style) {
        int rawWidth = getStringWidth(fontRenderer, preparedText);
        if (style != null && style.italic() && preparedText != null && !preparedText.isEmpty()) {
            rawWidth += 2;
        }
        float scale = style != null ? style.fontScale() : 1f;
        return Math.round(rawWidth * scale);
    }

    public static float getRenderedAdvance(FontRenderer fontRenderer, int codePoint, boolean bold,
        boolean hasVisibleGlyphBefore) {
        if (GuideEmojiParser.isEmojiBase(codePoint)) {
            float width = EMOJI_CACHE
                .getAdvance(new String(Character.toChars(codePoint)), fontRenderer.FONT_HEIGHT + 1);
            if (hasVisibleGlyphBefore) {
                width += GuideFontCompat.getGlyphSpacing(fontRenderer);
            }
            if (bold) {
                width += 1f;
            }
            return width;
        }
        return GuideFontCompat.getRenderedAdvance(fontRenderer, codePoint, bold, hasVisibleGlyphBefore);
    }

    public static String trimStringToWidth(FontRenderer fontRenderer, String text, int width) {
        if (fontRenderer == null || text == null || text.isEmpty() || width <= 0) {
            return "";
        }
        if (!containsEmoji(text)) {
            return fontRenderer.trimStringToWidth(text, width);
        }
        int end = 0;
        float used = 0f;
        while (end < text.length()) {
            if (GuideFontCompat.isFormattingCodeStart(text, end)) {
                int next = end + 2;
                end = Math.min(next, text.length());
                continue;
            }
            int next = EMOJI_PARSER.findEmojiEnd(text, end);
            float advance;
            if (next > end) {
                advance = EMOJI_CACHE.getAdvance(text.substring(end, next), fontRenderer.FONT_HEIGHT + 1);
            } else {
                int codePoint = text.codePointAt(end);
                next = end + Character.charCount(codePoint);
                advance = GuideFontCompat.getCharacterWidth(fontRenderer, codePoint);
            }
            if (used + advance > width) {
                break;
            }
            used += Math.max(0f, advance);
            end = next;
        }
        return text.substring(0, end);
    }

    public static List<String> listFormattedStringToWidth(FontRenderer fontRenderer, String text, int width) {
        if (fontRenderer == null || text == null || text.isEmpty()) {
            return List.of("");
        }
        if (!containsEmoji(text)) {
            return fontRenderer.listFormattedStringToWidth(text, width);
        }
        var lines = new ArrayList<String>();
        int offset = 0;
        while (offset < text.length()) {
            String remaining = text.substring(offset);
            String line = trimStringToWidth(fontRenderer, remaining, width);
            if (line.isEmpty()) {
                int codePoint = remaining.codePointAt(0);
                line = remaining.substring(0, Character.charCount(codePoint));
            }
            lines.add(line);
            offset += line.length();
            while (offset < text.length() && Character.isWhitespace(text.charAt(offset))) {
                offset++;
            }
        }
        return lines;
    }

    private static float drawMixedText(FontRenderer fontRenderer, String text, int x, int y, int color,
        boolean dropShadow, float scale) {
        var runs = EMOJI_PARSER.findEmojiRuns(text);
        if (runs.isEmpty()) {
            return drawVanilla(fontRenderer, text, x, y, color, dropShadow);
        }

        float cursor = x;
        int offset = 0;
        String activeFormat = "";
        for (var run : runs) {
            if (run.start() > offset) {
                String plain = text.substring(offset, run.start());
                cursor += drawVanilla(fontRenderer, activeFormat + plain, Math.round(cursor), y, color, dropShadow);
                activeFormat = updateActiveFormat(activeFormat, plain);
            }
            cursor += drawEmoji(fontRenderer, run, cursor, y, scale);
            offset = run.end();
        }
        if (offset < text.length()) {
            String plain = text.substring(offset);
            cursor += drawVanilla(fontRenderer, activeFormat + plain, Math.round(cursor), y, color, dropShadow);
        }
        return cursor - x;
    }

    private static float measureMixedText(FontRenderer fontRenderer, String text) {
        var runs = EMOJI_PARSER.findEmojiRuns(text);
        if (runs.isEmpty()) {
            return FontRendering.getStringWidth(text, fontRenderer);
        }

        float width = 0f;
        int offset = 0;
        for (var run : runs) {
            if (run.start() > offset) {
                width += FontRendering.getStringWidth(text.substring(offset, run.start()), fontRenderer);
            }
            width += EMOJI_CACHE.getAdvance(run.text(), fontRenderer.FONT_HEIGHT + 1);
            offset = run.end();
        }
        if (offset < text.length()) {
            width += FontRendering.getStringWidth(text.substring(offset), fontRenderer);
        }
        return width;
    }

    private static float drawEmoji(FontRenderer fontRenderer, GuideEmojiRun run, float x, int y, float scale) {
        var glyph = EMOJI_CACHE.getGlyph(run.text(), fontRenderer.FONT_HEIGHT + 1);
        if (!glyph.supported() || glyph.texture() == null) {
            return glyph.drawWidth() * scale;
        }

        int drawWidth = Math.max(1, Math.round(glyph.drawWidth() * scale));
        int drawHeight = Math.max(1, Math.round(glyph.drawHeight() * scale));
        int drawX = Math.round(x);
        int drawY = y - glyph.baselineOffset();
        Minecraft.getMinecraft()
            .getTextureManager()
            .bindTexture(glyph.texture());
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glColor4f(1f, 1f, 1f, 1f);
        var tess = Tessellator.instance;
        tess.startDrawingQuads();
        tess.addVertexWithUV(drawX, drawY + drawHeight, 0, 0f, 1f);
        tess.addVertexWithUV(drawX + drawWidth, drawY + drawHeight, 0, 1f, 1f);
        tess.addVertexWithUV(drawX + drawWidth, drawY, 0, 1f, 0f);
        tess.addVertexWithUV(drawX, drawY, 0, 0f, 0f);
        tess.draw();
        GL11.glColor4f(1f, 1f, 1f, 1f);
        return drawWidth;
    }

    private static int drawVanilla(FontRenderer fontRenderer, String text, int x, int y, int color,
        boolean dropShadow) {
        if (text == null || text.isEmpty()) {
            return 0;
        }
        if (dropShadow) {
            return fontRenderer.drawStringWithShadow(text, x, y, color) - x;
        }
        return fontRenderer.drawString(text, x, y, color) - x;
    }

    private static void drawVanillaStyled(FontRenderer fontRenderer, String drawn, int x, int y, int color, float scale,
        boolean dropShadow) {
        boolean scaled = Math.abs(scale - 1f) > 1e-4f;
        if (scaled) {
            GL11.glPushMatrix();
            GL11.glTranslatef(x, y, 0f);
            GL11.glScalef(scale, scale, 1f);
            if (dropShadow) {
                fontRenderer.drawStringWithShadow(drawn, 0, 0, color);
            } else {
                fontRenderer.drawString(drawn, 0, 0, color);
            }
            GL11.glPopMatrix();
        } else if (dropShadow) {
            fontRenderer.drawStringWithShadow(drawn, x, y, color);
        } else {
            fontRenderer.drawString(drawn, x, y, color);
        }
    }

    private static String updateActiveFormat(String currentFormat, String text) {
        if (text == null || text.isEmpty()) {
            return currentFormat;
        }
        String activeFormat = currentFormat;
        for (int index = 0; index + 1 < text.length(); index++) {
            if (!GuideFontCompat.isFormattingCodeStart(text, index)) {
                continue;
            }
            char code = Character.toLowerCase(text.charAt(index + 1));
            if (code == 'r' || isColorCode(code)) {
                activeFormat = "";
            } else if (isStyleCode(code) && activeFormat.indexOf(code) < 0) {
                activeFormat += GuideFontCompat.FORMATTING_CHAR;
                activeFormat += code;
            }
            index++;
        }
        return activeFormat;
    }

    private static boolean isColorCode(char code) {
        return code >= '0' && code <= '9' || code >= 'a' && code <= 'f';
    }

    private static boolean isStyleCode(char code) {
        return code == 'k' || code == 'l' || code == 'm' || code == 'n' || code == 'o';
    }

    public static void drawHorizontalLine(int x, int y, int width, int color) {
        Gui.drawRect(x, y, x + width, y + 1, color);
    }
}
