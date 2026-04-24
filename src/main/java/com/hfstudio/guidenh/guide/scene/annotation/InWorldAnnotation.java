package com.hfstudio.guidenh.guide.scene.annotation;

public abstract class InWorldAnnotation extends SceneAnnotation {

    private boolean alwaysOnTop;

    public boolean isAlwaysOnTop() {
        return alwaysOnTop;
    }

    public void setAlwaysOnTop(boolean alwaysOnTop) {
        this.alwaysOnTop = alwaysOnTop;
    }
}
