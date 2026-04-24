package com.hfstudio.guidenh.guide.document.interaction;

import com.hfstudio.guidenh.guide.document.LytRect;
import com.hfstudio.guidenh.guide.document.block.LytBlock;
import com.hfstudio.guidenh.guide.layout.LayoutContext;
import com.hfstudio.guidenh.guide.layout.MinecraftFontMetrics;

public class ContentTooltip implements GuideTooltip {

    private final LytBlock content;

    private int lastMaxWidth = -1;
    private LytRect layoutBox = LytRect.empty();

    public ContentTooltip(LytBlock content) {
        this.content = content;
    }

    public LytBlock getContent() {
        return content;
    }

    public LytRect layout(int maxWidth) {
        if (maxWidth != lastMaxWidth) {
            var ctx = new LayoutContext(new MinecraftFontMetrics());
            layoutBox = content.layout(ctx, 0, 0, Math.max(20, maxWidth));
            lastMaxWidth = maxWidth;
        }
        return layoutBox;
    }

    public LytRect getLayoutBox() {
        return layoutBox;
    }
}
