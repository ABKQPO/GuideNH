package com.hfstudio.guidenh.guide.ui;

import java.net.URI;

import com.hfstudio.guidenh.guide.PageAnchor;

public interface GuideUiHost {

    void navigateTo(PageAnchor anchor);

    void close();

    default void openExternalUrl(URI uri) {}

    default boolean copyCodeBlock(String text) {
        return false;
    }

    default boolean isCodeBlockWheelInteractionBlocked() {
        return false;
    }
}
