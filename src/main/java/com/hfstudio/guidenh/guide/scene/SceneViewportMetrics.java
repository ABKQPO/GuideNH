package com.hfstudio.guidenh.guide.scene;

/**
 * Pure-function utility for measuring scene content extent in screen space
 * by projecting level bounding-box corners through a camera.
 */
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

    /**
     * Projects the 8 corners of the given axis-aligned bounding box through
     * the camera and returns the screen-space extent.
     * Max bounds are extended by +1.0 on each axis to cover full block faces.
     */
    public static SceneViewportMetrics measure(CameraSettings camera, int[] bounds) {
        float lx = bounds[0];
        float ly = bounds[1];
        float lz = bounds[2];
        float hx = bounds[3] + 1f;
        float hy = bounds[4] + 1f;
        float hz = bounds[5] + 1f;
        float minSX = Float.MAX_VALUE;
        float maxSX = -Float.MAX_VALUE;
        float minSY = Float.MAX_VALUE;
        float maxSY = -Float.MAX_VALUE;
        for (int corner = 0; corner < 8; corner++) {
            float wx = (corner & 1) == 0 ? lx : hx;
            float wy = (corner & 2) == 0 ? ly : hy;
            float wz = (corner & 4) == 0 ? lz : hz;
            var sp = camera.worldToScreen(wx, wy, wz);
            if (sp.x < minSX) minSX = sp.x;
            if (sp.x > maxSX) maxSX = sp.x;
            if (sp.y < minSY) minSY = sp.y;
            if (sp.y > maxSY) maxSY = sp.y;
        }
        return new SceneViewportMetrics(minSX, maxSX, minSY, maxSY);
    }

    /** Auto-size: clamps dimension between 64 and 512 with 16px padding. */
    public static int clampDimension(float span) {
        return Math.max(64, Math.min(512, (int) Math.ceil(span) + 16));
    }
}
