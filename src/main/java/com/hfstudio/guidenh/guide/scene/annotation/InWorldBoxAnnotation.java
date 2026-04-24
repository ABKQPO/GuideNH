package com.hfstudio.guidenh.guide.scene.annotation;

import org.joml.Vector3f;

import com.hfstudio.guidenh.guide.color.ColorValue;

public final class InWorldBoxAnnotation extends InWorldAnnotation {

    public static final float DEFAULT_THICKNESS = 0.5f / 16f;

    private final Vector3f min;
    private final Vector3f max;
    private final ColorValue color;
    private final float thickness;

    public InWorldBoxAnnotation(Vector3f min, Vector3f max, ColorValue color, float thickness) {
        this.min = min;
        this.max = max;
        this.color = color;
        this.thickness = thickness;
    }

    public InWorldBoxAnnotation(Vector3f min, Vector3f max, ColorValue color) {
        this(min, max, color, DEFAULT_THICKNESS);
    }

    public static InWorldBoxAnnotation forBlock(int x, int y, int z, ColorValue color) {
        return new InWorldBoxAnnotation(new Vector3f(x, y, z), new Vector3f(x + 1, y + 1, z + 1), color);
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

    public float thickness() {
        return thickness;
    }
}
