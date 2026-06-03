package com.hfstudio.guidenh.guide.document.block;

import com.hfstudio.guidenh.guide.color.SymbolicColor;
import com.hfstudio.guidenh.guide.document.DefaultStyles;
import com.hfstudio.guidenh.guide.document.LytRect;
import com.hfstudio.guidenh.guide.layout.LayoutContext;
import com.hfstudio.guidenh.guide.render.RenderContext;

public class LytHeading extends LytParagraph {

    private int depth = 1;
    // Horizontal offset from bounds.x() to the float-adjusted text start position
    private int separatorXOffset = 0;
    private int separatorWidth = 0;

    public LytHeading() {
        setMarginTop(5);
        setMarginBottom(5);
    }

    public int getDepth() {
        return depth;
    }

    public void setDepth(int depth) {
        this.depth = depth;
        var style = switch (depth) {
            case 1 -> DefaultStyles.HEADING1;
            case 2 -> DefaultStyles.HEADING2;
            case 3 -> DefaultStyles.HEADING3;
            case 4 -> DefaultStyles.HEADING4;
            case 5 -> DefaultStyles.HEADING5;
            case 6 -> DefaultStyles.HEADING6;
            default -> DefaultStyles.BODY_TEXT;
        };
        setStyle(style);
    }

    @Override
    public LytRect computeLayout(LayoutContext context, int x, int y, int availableWidth) {
        // Capture the active inline window so the separator follows the same wrapped line width
        // that the heading text uses and never paints under floating content on either side.
        int leftEdge = context.getLeftFloatRightEdgeOr(x);
        int rightEdge = context.getRightFloatLeftEdgeOr(x + availableWidth);
        int clampedLeftEdge = Math.max(x, leftEdge);
        int clampedRightEdge = Math.min(x + availableWidth, rightEdge);
        separatorXOffset = Math.max(0, clampedLeftEdge - x);
        separatorWidth = Math.max(0, clampedRightEdge - clampedLeftEdge);
        return super.computeLayout(context, x, y, availableWidth);
    }

    @Override
    public void render(RenderContext context) {
        super.render(context);

        if (depth == 1) {
            var bounds = getBounds();
            int sepX = bounds.x() + separatorXOffset;
            int sepW = Math.max(0, separatorWidth);
            context.fillRect(sepX, bounds.bottom() - 1, sepW, 1, SymbolicColor.HEADER1_SEPARATOR);
        } else if (depth == 2) {
            var bounds = getBounds();
            int sepX = bounds.x() + separatorXOffset;
            int sepW = Math.max(0, separatorWidth);
            context.fillRect(sepX, bounds.bottom() - 1, sepW, 1, SymbolicColor.HEADER2_SEPARATOR);
        }
    }
}
