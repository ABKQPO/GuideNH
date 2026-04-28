package com.hfstudio.guidenh.guide.internal.editor.gui;

import com.hfstudio.guidenh.config.ModConfig;

public class SceneEditorMarkdownPanelState {

    private boolean expanded;
    private int openWidth;
    private boolean wrapEnabled;

    public SceneEditorMarkdownPanelState(boolean expanded, int openWidth, boolean wrapEnabled) {
        this.expanded = expanded;
        this.openWidth = openWidth;
        this.wrapEnabled = wrapEnabled;
    }

    public static SceneEditorMarkdownPanelState fromConfig(boolean expandedByDefault) {
        return new SceneEditorMarkdownPanelState(
            expandedByDefault,
            ModConfig.ui.sceneEditorMarkdownPanelWidth,
            ModConfig.ui.sceneEditorMarkdownWrapEnabled);
    }

    public boolean isExpanded() {
        return expanded;
    }

    public void setExpanded(boolean expanded) {
        this.expanded = expanded;
    }

    public int getOpenWidth() {
        return openWidth;
    }

    public void setOpenWidth(int openWidth, int minWidth, int maxWidth) {
        this.openWidth = clamp(openWidth, minWidth, maxWidth);
    }

    public void persistOpenWidth() {
        if (ModConfig.ui.sceneEditorMarkdownPanelWidth == this.openWidth) {
            return;
        }
        ModConfig.ui.sceneEditorMarkdownPanelWidth = this.openWidth;
        ModConfig.save();
    }

    public boolean isWrapEnabled() {
        return wrapEnabled;
    }

    public void setWrapEnabled(boolean wrapEnabled) {
        if (this.wrapEnabled == wrapEnabled) {
            return;
        }
        this.wrapEnabled = wrapEnabled;
        ModConfig.ui.sceneEditorMarkdownWrapEnabled = wrapEnabled;
        ModConfig.save();
    }

    public static int clamp(int value, int minValue, int maxValue) {
        if (value < minValue) {
            return minValue;
        }
        return value > maxValue ? maxValue : value;
    }
}
