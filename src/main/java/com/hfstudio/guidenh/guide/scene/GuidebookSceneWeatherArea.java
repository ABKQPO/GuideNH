package com.hfstudio.guidenh.guide.scene;

public class GuidebookSceneWeatherArea {

    private final int minX;
    private final int minZ;
    private final int maxX;
    private final int maxZ;

    public GuidebookSceneWeatherArea(int minX, int minZ, int maxX, int maxZ) {
        this.minX = minX;
        this.minZ = minZ;
        this.maxX = maxX;
        this.maxZ = maxZ;
    }

    public int getMinX() {
        return minX;
    }

    public int getMinZ() {
        return minZ;
    }

    public int getMaxX() {
        return maxX;
    }

    public int getMaxZ() {
        return maxZ;
    }

    public float getCenterX() {
        return (minX + maxX + 1) * 0.5f;
    }

    public float getCenterZ() {
        return (minZ + maxZ + 1) * 0.5f;
    }

    public float getRadius() {
        float width = Math.max(1.0f, maxX - minX + 1);
        float depth = Math.max(1.0f, maxZ - minZ + 1);
        return Math.max(1.0f, (float) Math.sqrt(width * width + depth * depth) * 0.5f);
    }
}
