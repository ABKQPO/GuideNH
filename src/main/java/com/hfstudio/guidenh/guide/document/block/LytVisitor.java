package com.hfstudio.guidenh.guide.document.block;

import com.hfstudio.guidenh.guide.document.flow.LytFlowContent;

public interface LytVisitor {

    default Result beforeNode(LytNode node) {
        return Result.CONTINUE;
    }

    default Result afterNode(LytNode node) {
        return Result.CONTINUE;
    }

    default Result beforeFlowContent(LytFlowContent content) {
        return Result.CONTINUE;
    }

    default Result afterFlowContent(LytFlowContent content) {
        return Result.CONTINUE;
    }

    default void text(String text) {}

    enum Result {
        CONTINUE,
        SKIP_CHILDREN,
        STOP
    }
}
