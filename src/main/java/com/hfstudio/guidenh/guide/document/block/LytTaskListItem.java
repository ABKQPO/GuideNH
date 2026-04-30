package com.hfstudio.guidenh.guide.document.block;

import com.hfstudio.guidenh.guide.color.SymbolicColor;
import com.hfstudio.guidenh.guide.document.LytRect;
import com.hfstudio.guidenh.guide.layout.LayoutContext;
import com.hfstudio.guidenh.guide.render.RenderContext;

public class LytTaskListItem extends LytListItem {

    private boolean checked;

    public boolean isChecked() {
        return checked;
    }

    public void setChecked(boolean checked) {
        this.checked = checked;
    }

    @Override
    protected LytRect computeBoxLayout(LayoutContext context, int x, int y, int availableWidth) {
        int margin = LEVEL_MARGIN + 4;
        LytRect bounds = super.computeBoxLayout(context, x + 4, y, Math.max(1, availableWidth - margin));
        return bounds.expand(4, 0, 0, 0);
    }

    @Override
    public void render(RenderContext context) {
        LytRect bounds = getBounds();
        int boxSize = 7;
        int boxX = bounds.x() + 1;
        int boxY = bounds.y() + 1;
        context.drawBorder(new LytRect(boxX, boxY, boxSize, boxSize), context.resolveColor(SymbolicColor.BODY_TEXT), 1);
        if (checked) {
            context.fillRect(boxX + 2, boxY + 2, 3, 3, SymbolicColor.LINK);
        }
        super.render(context);
    }
}
