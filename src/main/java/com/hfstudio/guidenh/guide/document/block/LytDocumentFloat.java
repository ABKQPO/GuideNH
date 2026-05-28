package com.hfstudio.guidenh.guide.document.block;

import java.util.List;

import com.hfstudio.guidenh.guide.document.LytRect;
import com.hfstudio.guidenh.guide.layout.LayoutContext;
import com.hfstudio.guidenh.guide.render.RenderContext;

/**
 * A document-level float block that wraps an inner block and registers it as a left-side or
 * right-side CSS-style float in the shared {@link LayoutContext}.
 *
 * <p>
 * Subsequent paragraphs and flow-containers that are laid out with the same
 * {@link LayoutContext} will automatically wrap their text around the floated content, because
 * the float bounds are visible to every call to
 * {@link LayoutContext#getLeftFloatRightEdge()} /
 * {@link LayoutContext#getRightFloatLeftEdge()}.
 *
 * <p>
 * This block always reports <em>zero height</em> so that the next block in the document
 * begins at the same vertical position as the float. The inner content visually extends
 * downward into the following paragraphs, exactly like CSS {@code float: left / right}.
 *
 * <p>
 * Example — left-floating GameScene that surrounding text wraps around:
 * 
 * <pre>
 * {@code
 * <GameScene wrap="square" align="left" width="200" height="150">
 *   <Block id="minecraft:stone" />
 * </GameScene>
 *
 * Some paragraph text that will flow to the right of the scene...
 * }
 * </pre>
 */
public class LytDocumentFloat extends LytBlock {

    private static final int FLOAT_GAP = 5;

    private final LytBlock inner;
    private final boolean floatRight;

    /**
     * @param inner      the block to float
     * @param floatRight {@code true} to float to the right, {@code false} to float to the left
     */
    public LytDocumentFloat(LytBlock inner, boolean floatRight) {
        this.inner = inner;
        inner.parent = this;
        this.floatRight = floatRight;
    }

    public LytBlock getInner() {
        return inner;
    }

    public boolean isFloatRight() {
        return floatRight;
    }

    @Override
    public List<? extends LytNode> getChildren() {
        return List.of(inner);
    }

    @Override
    public boolean isCulled(LytRect viewport) {
        return inner.isCulled(viewport);
    }

    @Override
    protected LytRect computeLayout(LayoutContext context, int x, int y, int availableWidth) {
        if (floatRight) {
            var naturalBounds = inner.layout(context, x, y, availableWidth);
            int innerWidth = naturalBounds.width();
            int rx = x + availableWidth - innerWidth;
            inner.layout(context, rx, y, innerWidth);
            context.addRightFloat(
                new LytRect(rx - FLOAT_GAP, y, innerWidth + FLOAT_GAP, naturalBounds.height() + FLOAT_GAP));
        } else {
            var innerBounds = inner.layout(context, x, y, availableWidth);
            context.addLeftFloat(new LytRect(x, y, innerBounds.width() + FLOAT_GAP, innerBounds.height() + FLOAT_GAP));
        }
        return new LytRect(x, y, 0, 0);
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
