package com.hfstudio.guidenh.guide.document.block;

import java.util.List;

import com.hfstudio.guidenh.guide.document.LytRect;
import com.hfstudio.guidenh.guide.layout.LayoutContext;
import com.hfstudio.guidenh.guide.render.RenderContext;

/**
 * Wraps a single inner block and positions it horizontally according to a {@link ContentAlign}
 * value within the available width.
 *
 * <p>
 * For {@link ContentAlign#LEFT} this wrapper is a transparent no-op: the inner block is laid
 * out at its natural position and the wrapper simply delegates. For {@link ContentAlign#CENTER}
 * and {@link ContentAlign#RIGHT} the inner block is first measured at its natural content width
 * and then repositioned by issuing a second layout pass at the correct horizontal offset.
 *
 * <p>
 * Example — a Recipe box centred in the page:
 * 
 * <pre>
 * {@code
 * <Recipe id="minecraft:stone" align="center" />
 * }
 * </pre>
 */
public class LytAlignedBlock extends LytBlock {

    private final LytBlock inner;
    private final ContentAlign align;

    /**
     * @param inner the block to align
     * @param align the desired horizontal alignment
     */
    public LytAlignedBlock(LytBlock inner, ContentAlign align) {
        this.inner = inner;
        inner.parent = this;
        this.align = align;
    }

    public LytBlock getInner() {
        return inner;
    }

    public ContentAlign getAlign() {
        return align;
    }

    @Override
    public List<? extends LytNode> getChildren() {
        return List.of(inner);
    }

    @Override
    protected LytRect computeLayout(LayoutContext context, int x, int y, int availableWidth) {
        if (align == ContentAlign.LEFT) {
            return inner.layout(context, x, y, availableWidth);
        }

        var naturalBounds = inner.layout(context, x, y, availableWidth);
        int innerWidth = naturalBounds.width();

        int newX;
        if (align == ContentAlign.CENTER) {
            newX = x + (availableWidth - innerWidth) / 2;
        } else {
            newX = x + availableWidth - innerWidth;
        }

        if (newX == x) {
            return naturalBounds;
        }

        inner.layout(context, newX, y, innerWidth);
        return inner.getBounds();
    }

    @Override
    protected void onLayoutMoved(int deltaX, int deltaY) {
        inner.moveLayoutPos(deltaX, deltaY);
    }

    @Override
    public LytNode pickNode(int x, int y) {
        return inner.pickNode(x, y);
    }

    @Override
    public void render(RenderContext context) {
        inner.render(context);
    }
}
