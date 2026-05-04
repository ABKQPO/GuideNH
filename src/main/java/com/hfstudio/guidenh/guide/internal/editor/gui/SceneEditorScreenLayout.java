package com.hfstudio.guidenh.guide.internal.editor.gui;

import com.github.bsideup.jabel.Desugar;
import com.hfstudio.guidenh.guide.document.LytRect;

public class SceneEditorScreenLayout {

    public static final int COLLAPSED_MARKDOWN_WIDTH = 10;
    public static final int TOOLBAR_Y = 8;
    public static final int TOOLBAR_SAFE_BOTTOM = 34;
    public static final int PANEL_HEADER_Y = 12;
    public static final int PANEL_CONTENT_TOP = 23;
    public static final int SIDE_PANEL_PADDING = 8;
    public static final int MARKDOWN_TOGGLE_WIDTH = 10;
    public static final int MARKDOWN_TOGGLE_HEIGHT = 40;
    public static final int MIN_LEFT_OPEN_WIDTH = 176;
    public static final int MAX_LEFT_OPEN_WIDTH = 420;
    public static final int MARKDOWN_RESIZE_HANDLE_WIDTH = 4;

    public static final int COLLAPSED_RIGHT_WIDTH = 10;
    public static final int MIN_RIGHT_WIDTH = 172;
    public static final int MAX_RIGHT_WIDTH = 216;
    public static final int MIN_PREVIEW_INTERACTION_WIDTH = 220;
    public static final int MARKDOWN_TEXT_TOP = 56;
    public static final int MARKDOWN_TEXT_BOTTOM_MARGIN = 64;
    public static final int MARKDOWN_FOOTER_HEIGHT = 18;
    public static final int MARKDOWN_FOOTER_BOTTOM_MARGIN = 10;

    private SceneEditorScreenLayout() {}

    public static Layout calculate(int screenWidth, int screenHeight, boolean markdownExpanded) {
        return calculate(screenWidth, screenHeight, markdownExpanded, MIN_LEFT_OPEN_WIDTH, true);
    }

    public static Layout calculate(int screenWidth, int screenHeight, boolean markdownExpanded, int markdownOpenWidth) {
        return calculate(screenWidth, screenHeight, markdownExpanded, markdownOpenWidth, true);
    }

    public static Layout calculate(int screenWidth, int screenHeight, boolean markdownExpanded, int markdownOpenWidth,
        boolean rightPanelExpanded) {
        int safeWidth = Math.max(360, screenWidth);
        int safeHeight = Math.max(220, screenHeight);

        int rightWidth = rightPanelExpanded ? clamp(safeWidth * 21 / 100, MIN_RIGHT_WIDTH, MAX_RIGHT_WIDTH)
            : COLLAPSED_RIGHT_WIDTH;
        int maxLeftWidth = Math.min(
            MAX_LEFT_OPEN_WIDTH,
            safeWidth - rightWidth - MIN_PREVIEW_INTERACTION_WIDTH - MARKDOWN_TOGGLE_WIDTH - 2);
        maxLeftWidth = Math.max(MIN_LEFT_OPEN_WIDTH, maxLeftWidth);
        int leftOpenWidth = clamp(markdownOpenWidth, MIN_LEFT_OPEN_WIDTH, maxLeftWidth);
        int leftWidth = markdownExpanded ? leftOpenWidth : COLLAPSED_MARKDOWN_WIDTH;

        int rightX = safeWidth - rightWidth;
        rightWidth = Math.max(0, safeWidth - rightX);

        LytRect leftPanel = new LytRect(0, 0, leftWidth, safeHeight);
        LytRect rightPanel = new LytRect(rightX, 0, rightWidth, safeHeight);
        LytRect previewRender = new LytRect(0, 0, safeWidth, safeHeight);
        LytRect previewInteraction = new LytRect(
            leftPanel.right(),
            Math.min(safeHeight, TOOLBAR_SAFE_BOTTOM),
            Math.max(0, rightPanel.x() - leftPanel.right()),
            Math.max(0, safeHeight - Math.min(safeHeight, TOOLBAR_SAFE_BOTTOM)));

        int toggleY = Math.max(0, safeHeight / 2 - MARKDOWN_TOGGLE_HEIGHT / 2);
        LytRect markdownToggle = new LytRect(
            Math.max(0, leftPanel.right() - 1),
            toggleY,
            MARKDOWN_TOGGLE_WIDTH,
            Math.min(MARKDOWN_TOGGLE_HEIGHT, safeHeight));

        LytRect rightToggle = new LytRect(
            Math.max(0, rightPanel.x() - MARKDOWN_TOGGLE_WIDTH + 1),
            toggleY,
            MARKDOWN_TOGGLE_WIDTH,
            Math.min(MARKDOWN_TOGGLE_HEIGHT, safeHeight));

        LytRect markdownFooter = markdownExpanded
            ? new LytRect(
                SIDE_PANEL_PADDING,
                Math.max(MARKDOWN_TEXT_TOP, safeHeight - MARKDOWN_FOOTER_HEIGHT - MARKDOWN_FOOTER_BOTTOM_MARGIN),
                Math.max(0, leftWidth - SIDE_PANEL_PADDING * 2),
                MARKDOWN_FOOTER_HEIGHT)
            : LytRect.empty();

        LytRect markdownContent = markdownExpanded
            ? new LytRect(
                SIDE_PANEL_PADDING,
                MARKDOWN_TEXT_TOP,
                Math.max(0, leftWidth - SIDE_PANEL_PADDING * 2),
                Math.max(0, safeHeight - MARKDOWN_TEXT_TOP - MARKDOWN_TEXT_BOTTOM_MARGIN))
            : LytRect.empty();

        LytRect markdownResizeHandle = markdownExpanded
            ? new LytRect(
                Math.max(0, leftPanel.right() - MARKDOWN_RESIZE_HANDLE_WIDTH / 2),
                Math.min(safeHeight, TOOLBAR_SAFE_BOTTOM),
                MARKDOWN_RESIZE_HANDLE_WIDTH,
                Math.max(0, safeHeight - Math.min(safeHeight, TOOLBAR_SAFE_BOTTOM)))
            : LytRect.empty();

        LytRect rightContent = new LytRect(
            rightPanel.x() + SIDE_PANEL_PADDING,
            PANEL_CONTENT_TOP,
            Math.max(0, rightPanel.width() - SIDE_PANEL_PADDING * 2),
            Math.max(0, safeHeight - PANEL_CONTENT_TOP - SIDE_PANEL_PADDING));

        return new Layout(
            leftPanel,
            rightPanel,
            previewRender,
            previewInteraction,
            markdownToggle,
            markdownContent,
            markdownFooter,
            markdownResizeHandle,
            rightContent,
            rightToggle);
    }

    public static int clamp(int value, int minValue, int maxValue) {
        if (value < minValue) {
            return minValue;
        }
        return value > maxValue ? maxValue : value;
    }

    @Desugar
    public record Layout(LytRect leftPanel, LytRect rightPanel, LytRect previewRender, LytRect previewInteraction,
        LytRect markdownToggle, LytRect markdownContent, LytRect markdownFooter, LytRect markdownResizeHandle,
        LytRect rightContent, LytRect rightToggle) {}
}
