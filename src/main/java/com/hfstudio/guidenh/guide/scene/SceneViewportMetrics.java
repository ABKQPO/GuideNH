package com.hfstudio.guidenh.guide.scene;

public class SceneViewportMetrics {

    private final float minScreenX;
    private final float maxScreenX;
    private final float minScreenY;
    private final float maxScreenY;

    public SceneViewportMetrics(float minScreenX, float maxScreenX, float minScreenY, float maxScreenY) {
        this.minScreenX = minScreenX;
        this.maxScreenX = maxScreenX;
        this.minScreenY = minScreenY;
        this.maxScreenY = maxScreenY;
    }

    public float minScreenX() {
        return minScreenX;
    }

    public float maxScreenX() {
        return maxScreenX;
    }

    public float minScreenY() {
        return minScreenY;
    }

    public float maxScreenY() {
        return maxScreenY;
    }

    public float spanX() {
        return maxScreenX - minScreenX;
    }

    public float spanY() {
        return maxScreenY - minScreenY;
    }
}
