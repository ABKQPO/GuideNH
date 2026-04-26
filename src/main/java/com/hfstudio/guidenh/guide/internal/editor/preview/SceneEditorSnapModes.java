package com.hfstudio.guidenh.guide.internal.editor.preview;

public final class SceneEditorSnapModes {

    private static final SceneEditorSnapModes DEFAULT = new SceneEditorSnapModes(true, false, false, false);

    private final boolean pointEnabled;
    private final boolean lineEnabled;
    private final boolean faceEnabled;
    private final boolean centerEnabled;

    public SceneEditorSnapModes(boolean pointEnabled, boolean lineEnabled, boolean faceEnabled, boolean centerEnabled) {
        this.pointEnabled = pointEnabled;
        this.lineEnabled = lineEnabled;
        this.faceEnabled = faceEnabled;
        this.centerEnabled = centerEnabled;
    }

    public static SceneEditorSnapModes defaultModes() {
        return DEFAULT;
    }

    public boolean isPointEnabled() {
        return pointEnabled;
    }

    public boolean isLineEnabled() {
        return lineEnabled;
    }

    public boolean isFaceEnabled() {
        return faceEnabled;
    }

    public boolean isCenterEnabled() {
        return centerEnabled;
    }

    public boolean hasEnabledMode() {
        return pointEnabled || lineEnabled || faceEnabled || centerEnabled;
    }
}
