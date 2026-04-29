package com.hfstudio.guidenh.guide.scene.annotation;

import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.guide.document.interaction.GuideTooltip;
import com.hfstudio.guidenh.guide.document.interaction.TextTooltip;

public abstract class SceneAnnotation {

    @Nullable
    private GuideTooltip tooltip;

    private boolean hovered;

    @Nullable
    public GuideTooltip getTooltip() {
        return tooltip;
    }

    public void setTooltip(@Nullable GuideTooltip tooltip) {
        this.tooltip = tooltip;
    }

    public void setTooltipText(@Nullable String text) {
        this.tooltip = (text != null && !text.isEmpty()) ? new TextTooltip(text) : null;
    }

    public boolean hasTooltip() {
        return tooltip != null;
    }

    public boolean isHovered() {
        return hovered;
    }

    public void setHovered(boolean hovered) {
        this.hovered = hovered;
    }
}
