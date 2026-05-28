package com.hfstudio.guidenh.guide.internal.editor.guide;

import com.hfstudio.guidenh.config.ModConfig;
import com.hfstudio.guidenh.guide.internal.structure.GuideNhStructureExportAccess;

public class GuideScreenEditorState {

    private static final String DEFAULT_AUTHOR = "GuideNH";
    private static final String DEFAULT_NEW_PAGE_PATH = "NewGuide.md";

    private GuideScreenEditorState() {}

    public static boolean isEnabled() {
        return ModConfig.ui.guideEditorEnabled && GuideNhStructureExportAccess.canUseSceneExport();
    }

    public static void setEnabled(boolean enabled) {
        ModConfig.ui.guideEditorEnabled = enabled;
        ModConfig.save();
    }

    public static boolean isAdvancedToolbarVisible() {
        return ModConfig.ui.guideEditorAdvancedToolbarVisible;
    }

    public static void setAdvancedToolbarVisible(boolean visible) {
        ModConfig.ui.guideEditorAdvancedToolbarVisible = visible;
        ModConfig.save();
    }

    public static boolean isAutosaveEnabled() {
        return ModConfig.ui.guideEditorAutosaveEnabled;
    }

    public static void setAutosaveEnabled(boolean enabled) {
        ModConfig.ui.guideEditorAutosaveEnabled = enabled;
        ModConfig.save();
    }

    public static GuideScreenEditorLayoutMode getLayoutMode() {
        return GuideScreenEditorLayoutMode.fromConfig(ModConfig.ui.guideEditorLayoutMode);
    }

    public static void setLayoutMode(GuideScreenEditorLayoutMode mode) {
        ModConfig.ui.guideEditorLayoutMode = mode != null ? mode.toConfigValue() : 0;
        ModConfig.save();
    }

    public static int getDividerPercent() {
        return clamp(ModConfig.ui.guideEditorDividerPercent, 15, 85);
    }

    public static void setDividerPercent(int percent) {
        ModConfig.ui.guideEditorDividerPercent = clamp(percent, 15, 85);
        ModConfig.save();
    }

    public static int getAutosaveDelayMillis() {
        return Math.max(100, ModConfig.ui.guideEditorAutosaveDelayMillis);
    }

    public static String getDefaultAuthor() {
        String author = ModConfig.ui.guideEditorDefaultAuthor;
        return author != null && !author.trim()
            .isEmpty() ? author.trim() : DEFAULT_AUTHOR;
    }

    public static void setDefaultAuthor(String author) {
        ModConfig.ui.guideEditorDefaultAuthor = author != null ? author : DEFAULT_AUTHOR;
        ModConfig.save();
    }

    public static String getNewPagePath() {
        String path = ModConfig.ui.guideEditorNewPagePath;
        return path != null && !path.trim()
            .isEmpty() ? path.trim() : DEFAULT_NEW_PAGE_PATH;
    }

    public static void setNewPagePath(String path) {
        ModConfig.ui.guideEditorNewPagePath = path != null && !path.trim()
            .isEmpty() ? path.trim() : DEFAULT_NEW_PAGE_PATH;
        ModConfig.save();
    }

    private static int clamp(int value, int minValue, int maxValue) {
        if (value < minValue) {
            return minValue;
        }
        return Math.min(value, maxValue);
    }
}
