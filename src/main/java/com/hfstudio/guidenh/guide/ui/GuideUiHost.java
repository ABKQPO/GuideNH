package com.hfstudio.guidenh.guide.ui;

import com.hfstudio.guidenh.guide.PageAnchor;

public interface GuideUiHost {

    void navigateTo(PageAnchor anchor);

    void close();

    default boolean copyCodeBlock(String text) {
        return false;
    }

    default boolean isCodeBlockWheelInteractionBlocked() {
        return false;
    }
}
