package com.hfstudio.guidenh.guide.render;

import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;

import com.hfstudio.guidenh.guide.color.ColorValue;
import com.hfstudio.guidenh.guide.color.LightDarkMode;
import com.hfstudio.guidenh.guide.document.LytRect;
import com.hfstudio.guidenh.guide.style.ResolvedTextStyle;

public interface RenderContext {

    LightDarkMode lightDarkMode();

    default boolean isDarkMode() {
        return lightDarkMode() == LightDarkMode.DARK_MODE;
    }

    LytRect viewport();

    default boolean intersectsViewport(LytRect bounds) {
        return bounds.intersects(viewport());
    }

    default int getDocumentOriginX() {
        return 0;
    }

    default int getDocumentOriginY() {
        return 0;
    }

    default int getScrollOffsetY() {
        return 0;
    }

    default LytRect toScreenRect(LytRect rect) {
        return new LytRect(
            rect.x() + getDocumentOriginX(),
            rect.y() + getDocumentOriginY() - getScrollOffsetY(),
            rect.width(),
            rect.height());
    }

    int resolveColor(ColorValue ref);

    void fillRect(LytRect rect, int argbColor);

    void drawBorder(LytRect rect, int argbColor, int thickness);

    void drawText(String text, int x, int y, ResolvedTextStyle style);

    int getStringWidth(String text, ResolvedTextStyle style);

    int getLineHeight(ResolvedTextStyle style);

    void renderItem(ItemStack stack, int x, int y);

    void blitTexture(ResourceLocation texture, int x, int y, int u, int v, int width, int height);

    default void blitGuiSprite(LytRect rect, GuiSprite sprite) {
        if (sprite != null) {
            blitTexture(
                sprite.getTexture(),
                rect.x(),
                rect.y(),
                sprite.getU(),
                sprite.getV(),
                sprite.getWidth(),
                sprite.getHeight());
        }
    }

    void pushScissor(LytRect rect);

    default void pushLocalScissor(LytRect rect) {
        pushScissor(toScreenRect(rect));
    }

    void popScissor();

    default void restoreExternalRenderState() {}

    default void fillRect(LytRect rect, ColorValue color) {
        fillRect(rect, resolveColor(color));
    }

    default void fillRect(int x, int y, int width, int height, ColorValue color) {
        fillRect(new LytRect(x, y, width, height), resolveColor(color));
    }

    default int getWidth(String text, ResolvedTextStyle style) {
        return getStringWidth(text, style);
    }

    default void renderText(String text, ResolvedTextStyle style, float x, float y) {
        drawText(text, (int) x, (int) y, style);
    }

    default void fillIcon(LytRect rect, GuiSprite sprite) {
        if (sprite != null) {
            blitGuiSprite(rect, sprite);
        }
    }

    default void fillIcon(LytRect rect, GuiSprite sprite, ColorValue color) {
        fillIcon(rect, sprite);
    }

    default void fillTexturedRect(LytRect rect, GuidePageTexture texture) {
        if (texture != null && !texture.isMissing()) {
            blitTexture(texture.getTexture(), rect.x(), rect.y(), 0, 0, rect.width(), rect.height());
        }
    }
}
