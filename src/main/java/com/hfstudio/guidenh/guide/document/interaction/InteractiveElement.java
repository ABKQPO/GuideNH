package com.hfstudio.guidenh.guide.document.interaction;

import java.util.Optional;

import com.hfstudio.guidenh.guide.ui.GuideUiHost;

public interface InteractiveElement {

    default boolean mouseClicked(GuideUiHost screen, int x, int y, int button, boolean doubleClick) {
        return false;
    }

    default boolean mouseReleased(GuideUiHost screen, int x, int y, int button) {
        return false;
    }

    default Optional<GuideTooltip> getTooltip(float x, float y) {
        return Optional.empty();
    }
}
