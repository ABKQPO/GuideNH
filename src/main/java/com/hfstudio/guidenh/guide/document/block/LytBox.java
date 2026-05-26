package com.hfstudio.guidenh.guide.document.block;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.guide.color.SymbolicColor;
import com.hfstudio.guidenh.guide.document.LytRect;
import com.hfstudio.guidenh.guide.layout.LayoutContext;
import com.hfstudio.guidenh.guide.render.RenderContext;

public abstract class LytBox extends LytBlock implements LytBlockContainer {

    protected final List<LytBlock> children = new ArrayList<>();

    protected int paddingLeft;
    protected int paddingTop;
    protected int paddingRight;
    protected int paddingBottom;

    private final BorderRenderer borderRenderer = new BorderRenderer();

    @Nullable
    private SymbolicColor backgroundColor;

    @Override
    public void removeChild(LytNode node) {
        if (node instanceof LytBlock block && block.parent == this) {
            children.remove(block);
            block.parent = null;
        }
    }

    @Override
    public void append(LytBlock block) {
        if (block.parent != null) {
            block.parent.removeChild(block);
        }
        block.parent = this;
        children.add(block);
    }

    @Override
    public void replaceChild(LytNode oldChild, LytNode newChild) {
        if (!(oldChild instanceof LytBlock)) return;
        if (!(newChild instanceof LytBlock)) return;
        LytBlock oldBlock = (LytBlock) oldChild;
        LytBlock newBlock = (LytBlock) newChild;
        int idx = children.indexOf(oldBlock);
        if (idx < 0) return;
        oldBlock.parent = null;
        if (newBlock.parent != null) {
            newBlock.parent.removeChild(newBlock);
        }
        newBlock.parent = this;
        children.set(idx, newBlock);
        LytDocument doc = getDocument();
        if (doc != null) {
            doc.invalidateLayout();
        }
    }

    public void clearContent() {
        for (var child : children) {
            child.parent = null;
        }
        children.clear();
    }

    protected abstract LytRect computeBoxLayout(LayoutContext context, int x, int y, int availableWidth);

    @Override
    protected final LytRect computeLayout(LayoutContext context, int x, int y, int availableWidth) {
        int borderTop = getBorderTop().width();
        int borderLeft = getBorderLeft().width();
        int borderRight = getBorderRight().width();
        int borderBottom = getBorderBottom().width();

        // Apply padding and border
        var innerLayout = computeBoxLayout(
            context,
            x + paddingLeft + borderLeft,
            y + paddingTop + borderTop,
            availableWidth - paddingLeft - paddingRight - borderLeft - borderRight);

        return innerLayout.expand(
            paddingLeft + borderLeft,
            paddingTop + borderTop,
            paddingRight + borderRight,
            paddingBottom + borderBottom);
    }

    @Override
    protected void onLayoutMoved(int deltaX, int deltaY) {
        for (var child : children) {
            child.moveLayoutPos(deltaX, deltaY);
        }
    }

    public final void setPadding(int padding) {
        paddingLeft = padding;
        paddingTop = padding;
        paddingRight = padding;
        paddingBottom = padding;
    }

    public void setPaddingLeft(int paddingLeft) {
        this.paddingLeft = paddingLeft;
    }

    public void setPaddingTop(int paddingTop) {
        this.paddingTop = paddingTop;
    }

    public void setPaddingRight(int paddingRight) {
        this.paddingRight = paddingRight;
    }

    public void setPaddingBottom(int paddingBottom) {
        this.paddingBottom = paddingBottom;
    }

    public @Nullable SymbolicColor getBackgroundColor() {
        return backgroundColor;
    }

    public void setBackgroundColor(@Nullable SymbolicColor backgroundColor) {
        this.backgroundColor = backgroundColor;
    }

    @Override
    public List<? extends LytNode> getChildren() {
        return children;
    }

    @Override
    public void render(RenderContext context) {
        if (backgroundColor != null) {
            context.fillRect(bounds, backgroundColor);
        }

        for (var child : children) {
            child.render(context);
        }

        // Only render borders when at least one side has a non-zero width; most boxes have no borders.
        if (getBorderTop().width() > 0 || getBorderLeft().width() > 0
            || getBorderRight().width() > 0
            || getBorderBottom().width() > 0) {
            borderRenderer
                .render(context, bounds, getBorderTop(), getBorderLeft(), getBorderRight(), getBorderBottom());
        }
    }
}
