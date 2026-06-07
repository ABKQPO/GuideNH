package com.hfstudio.guidenh.guide.layout.flow;

import com.hfstudio.guidenh.guide.color.ColorValue;
import com.hfstudio.guidenh.guide.color.LightDarkMode;
import com.hfstudio.guidenh.guide.document.LytRect;
import com.hfstudio.guidenh.guide.render.RenderContext;
import com.hfstudio.guidenh.guide.style.ResolvedTextStyle;

public class LineTextRun extends LineElement {

    public static final int INLINE_CODE_PAD_X = 3;
    public static final int INLINE_CODE_EXTRA_WIDTH = INLINE_CODE_PAD_X * 2;
    public static final int INLINE_CODE_BACKGROUND_LIGHT = 0x1AF0F6FF;
    public static final int INLINE_CODE_BACKGROUND_DARK = 0x1A6FB6FF;

    public final String text;
    public final ResolvedTextStyle style;
    public final ResolvedTextStyle revealStyle;
    public final ResolvedTextStyle hoverStyle;

    public LineTextRun(String text, ResolvedTextStyle style, ResolvedTextStyle revealStyle,
        ResolvedTextStyle hoverStyle) {
        this.text = text;
        this.style = style;
        this.revealStyle = revealStyle;
        this.hoverStyle = hoverStyle;
    }

    @Override
    public void render(RenderContext context) {
        var resolvedStyle = containsMouse ? hoverStyle : revealedBySpoiler ? revealStyle : this.style;
        boolean inlineCode = resolvedStyle.inlineCode();
        ColorValue backgroundColor = resolvedStyle.backgroundColor();
        if (!inlineCode && backgroundColor == null) {
            context.drawText(text, bounds.x(), bounds.y(), resolvedStyle);
            return;
        }
        LytRect rect = bounds;
        int width = rect.width();
        int height = rect.height();
        if (width > 0 && height > 0) {
            int backgroundY = rect.y() - 1;
            if (inlineCode) {
                int backgroundColorArgb = context.lightDarkMode() == LightDarkMode.DARK_MODE
                    ? INLINE_CODE_BACKGROUND_DARK
                    : INLINE_CODE_BACKGROUND_LIGHT;
                context.fillRect(rect.x(), backgroundY, width, height, backgroundColorArgb);
            } else {
                context.fillRect(rect.x() - 1, backgroundY, width + 2, height, backgroundColor);
            }
        }
        int textX = inlineCode ? rect.x() + INLINE_CODE_PAD_X : rect.x();
        context.drawText(text, textX, rect.y(), resolvedStyle);
    }

    @Override
    public String toString() {
        return "TextRun[" + text + "]";
    }
}
