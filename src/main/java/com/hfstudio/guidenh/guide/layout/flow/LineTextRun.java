package com.hfstudio.guidenh.guide.layout.flow;

import com.hfstudio.guidenh.guide.render.RenderContext;
import com.hfstudio.guidenh.guide.style.ResolvedTextStyle;

public class LineTextRun extends LineElement {

    public final String text;
    public final ResolvedTextStyle style;
    public final ResolvedTextStyle hoverStyle;

    public LineTextRun(String text, ResolvedTextStyle style, ResolvedTextStyle hoverStyle) {
        this.text = text;
        this.style = style;
        this.hoverStyle = hoverStyle;
    }

    @Override
    public void render(RenderContext context) {
        var style = containsMouse ? this.hoverStyle : this.style;
        if (style.backgroundColor() != null && bounds.width() > 0 && bounds.height() > 0) {
            context.fillRect(bounds.expand(1, 1, 1, 0), style.backgroundColor());
        }
        context.drawText(text, bounds.x(), bounds.y(), style);
    }

    @Override
    public String toString() {
        return "TextRun[" + text + "]";
    }
}
