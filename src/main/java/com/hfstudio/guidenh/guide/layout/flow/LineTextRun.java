package com.hfstudio.guidenh.guide.layout.flow;

import com.hfstudio.guidenh.guide.render.RenderContext;
import com.hfstudio.guidenh.guide.style.ResolvedTextStyle;

public class LineTextRun extends LineElement {

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
        if (style.backgroundColor() != null && bounds.width() > 0 && bounds.height() > 0) {
            context.fillRect(
                bounds.x() - 1,
                bounds.y() - 1,
                bounds.width() + 2,
                bounds.height() + 1,
                style.backgroundColor());
        }
        context.drawText(text, bounds.x(), bounds.y(), style);
    }

    @Override
    public String toString() {
        return "TextRun[" + text + "]";
    }
}
