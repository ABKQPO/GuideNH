package com.hfstudio.guidenh.guide.layout.flow;

import com.hfstudio.guidenh.guide.color.ConstantColor;
import com.hfstudio.guidenh.guide.render.RenderContext;
import com.hfstudio.guidenh.guide.style.ResolvedTextStyle;

public class LineTextRun extends LineElement {

    public static final int INLINE_CODE_PAD_X = 3;
    public static final ConstantColor INLINE_CODE_BACKGROUND = new ConstantColor(0x1AF0F6FF, 0x1A6FB6FF);

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
        var style = containsMouse ? hoverStyle : revealedBySpoiler ? revealStyle : this.style;
        int backgroundHeight = Math.max(1, bounds.height());
        int backgroundY = bounds.y() - 1;
        if (style.inlineCode() && bounds.width() > 0 && bounds.height() > 0) {
            context.fillRect(
                bounds.x(),
                backgroundY,
                bounds.width(),
                backgroundHeight,
                context.resolveColor(INLINE_CODE_BACKGROUND));
        } else if (style.backgroundColor() != null && bounds.width() > 0 && bounds.height() > 0) {
            context
                .fillRect(bounds.x() - 1, backgroundY, bounds.width() + 2, backgroundHeight, style.backgroundColor());
        }
        int textX = style.inlineCode() ? bounds.x() + INLINE_CODE_PAD_X : bounds.x();
        context.drawText(text, textX, bounds.y(), style);
    }

    @Override
    public String toString() {
        return "TextRun[" + text + "]";
    }
}
