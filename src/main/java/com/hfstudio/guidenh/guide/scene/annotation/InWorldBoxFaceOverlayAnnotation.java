package com.hfstudio.guidenh.guide.scene.annotation;

import org.joml.Vector3f;

import com.hfstudio.guidenh.guide.color.ColorValue;

public class InWorldBoxFaceOverlayAnnotation extends InWorldAnnotation {

    private final Vector3f min;
    private final Vector3f max;
    private ColorValue color;

    public InWorldBoxFaceOverlayAnnotation(Vector3f min, Vector3f max, ColorValue color) {
        this.min = min;
        this.max = max;
        this.color = color;
    }

    public InWorldBoxFaceOverlayAnnotation(float minX, float minY, float minZ, float maxX, float maxY, float maxZ,
        ColorValue color) {
        this(new Vector3f(minX, minY, minZ), new Vector3f(maxX, maxY, maxZ), color);
    }

    public Vector3f min() {
        return min;
    }

    public Vector3f max() {
        return max;
    }

    public ColorValue color() {
        return color;
    }

    public void setColor(ColorValue color) {
        this.color = color;
    }

    public void setBounds(float minX, float minY, float minZ, float maxX, float maxY, float maxZ) {
        this.min.set(minX, minY, minZ);
        this.max.set(maxX, maxY, maxZ);
    }

    public boolean shouldDrawNegativeXFace() {
        return true;
    }

    public boolean shouldDrawPositiveXFace() {
        return true;
    }

    public boolean shouldDrawNegativeYFace() {
        return true;
    }

    public boolean shouldDrawPositiveYFace() {
        return true;
    }

    public boolean shouldDrawNegativeZFace() {
        return true;
    }

    public boolean shouldDrawPositiveZFace() {
        return true;
    }
}
