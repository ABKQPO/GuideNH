package com.hfstudio.guidenh.guide.document.flow;

import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.guide.document.block.LytVisitor;
import com.hfstudio.guidenh.guide.style.Styleable;
import com.hfstudio.guidenh.guide.style.TextStyle;

public class LytFlowContent implements Styleable {

    private TextStyle style = TextStyle.EMPTY;
    private TextStyle hoverStyle = TextStyle.EMPTY;

    private LytFlowParent parent;

    public LytFlowParent getParent() {
        return parent;
    }

    public void setParent(LytFlowParent parent) {
        this.parent = parent;
    }

    /**
     * Gets the parent of this flow content that is itself flow content. Null if the parent is null or not flow content.
     */
    @Nullable
    public LytFlowContent getFlowParent() {
        return parent instanceof LytFlowContent ? (LytFlowContent) parent : null;
    }

    public boolean isInclusiveAncestor(LytFlowContent flowContent) {
        for (var content = flowContent; content != null; content = content.getFlowParent()) {
            if (content == this) {
                return true;
            }
        }
        return false;
    }

    @Override
    public TextStyle getStyle() {
        return style;
    }

    @Override
    public void setStyle(TextStyle style) {
        this.style = style;
    }

    @Override
    public TextStyle getHoverStyle() {
        return hoverStyle;
    }

    @Override
    public void setHoverStyle(TextStyle style) {
        this.hoverStyle = style;
    }

    @Override
    public @Nullable Styleable getStylingParent() {
        return getParent() instanceof Styleable ? (Styleable) getParent() : null;
    }

    public final void mouseClicked() {}

    public final void visit(LytVisitor visitor) {
        visitor.beforeFlowContent(this);
        visitChildren(visitor);
        visitor.afterFlowContent(this);
    }

    protected void visitChildren(LytVisitor visitor) {}
}
