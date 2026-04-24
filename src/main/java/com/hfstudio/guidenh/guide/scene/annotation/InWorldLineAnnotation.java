package com.hfstudio.guidenh.guide.scene.annotation;

import org.joml.Vector3f;

import com.hfstudio.guidenh.guide.color.ColorValue;

public final class InWorldLineAnnotation extends InWorldAnnotation {

    public static final float DEFAULT_THICKNESS = 0.5f / 16f;

    private final Vector3f from;
    private final Vector3f to;
    private final ColorValue color;
    private final float thickness;

    public InWorldLineAnnotation(Vector3f from, Vector3f to, ColorValue color, float thickness) {
        this.from = from;
        this.to = to;
        this.color = color;
        this.thickness = thickness;
    }

    public InWorldLineAnnotation(Vector3f from, Vector3f to, ColorValue color) {
        this(from, to, color, DEFAULT_THICKNESS);
    }

    public Vector3f from() {
        return from;
    }

    public Vector3f to() {
        return to;
    }

    public ColorValue color() {
        return color;
    }

    public float thickness() {
        return thickness;
    }
}
