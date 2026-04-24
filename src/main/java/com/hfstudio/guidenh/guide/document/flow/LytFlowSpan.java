package com.hfstudio.guidenh.guide.document.flow;

import java.util.ArrayList;
import java.util.List;

import com.hfstudio.guidenh.guide.document.block.LytVisitor;
import com.hfstudio.guidenh.guide.style.Styleable;

/**
 * Attaches properties to a span of {@link LytFlowContent}, such as links or formatting.
 */
public class LytFlowSpan extends LytFlowContent implements LytFlowParent, Styleable {

    private final List<LytFlowContent> children = new ArrayList<>();

    public List<LytFlowContent> getChildren() {
        return children;
    }

    public void append(LytFlowContent child) {
        if (child.getParent() != null) {
            throw new IllegalStateException("Child is already owned by other span");
        }
        child.setParent(this);
        children.add(child);
    }

    @Override
    protected void visitChildren(LytVisitor visitor) {
        for (var child : children) {
            child.visit(visitor);
        }
    }
}
