package com.hfstudio.guidenh.guide.document.block;

public class LytList extends LytVBox {

    private final boolean ordered;
    private final int start;

    public LytList(boolean ordered, int start) {
        this.ordered = ordered;
        this.start = start;
    }

    public int getDepth() {
        int depth = 1;
        for (var node = getParent(); node != null; node = node.getParent()) {
            if (node instanceof LytList) {
                depth++;
            }
        }
        return depth;
    }

    public boolean isOrdered() {
        return ordered;
    }

    public int getStart() {
        return start;
    }
}
