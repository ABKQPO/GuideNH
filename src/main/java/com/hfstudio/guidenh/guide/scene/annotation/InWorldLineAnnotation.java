package com.hfstudio.guidenh.guide.scene.annotation;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import com.hfstudio.guidenh.guide.color.ColorValue;

public class InWorldLineAnnotation extends InWorldAnnotation {

    public static float DEFAULT_THICKNESS = 1f;
    public static float DEFAULT_POINT_SIZE_SCALE = 1.25f;

    private final List<Vector3f> points;
    private final ColorValue color;
    private final float thickness;
    private Arrow arrow = Arrow.NONE;
    private boolean showPoints;
    private ColorValue pointColor;
    private float pointSize;
    private final List<PointStyle> pointStyles = new ArrayList<>();

    public InWorldLineAnnotation(Vector3f from, Vector3f to, ColorValue color, float thickness) {
        this(List.of(from, to), color, thickness);
    }

    public InWorldLineAnnotation(List<Vector3f> points, ColorValue color, float thickness) {
        if (points == null || points.size() < 2) {
            throw new IllegalArgumentException("Line annotation requires at least two points.");
        }
        List<Vector3f> copied = new ArrayList<>(points.size());
        for (Vector3f point : points) {
            copied.add(new Vector3f(point));
        }
        this.points = List.copyOf(copied);
        this.color = color;
        this.thickness = thickness;
        this.pointColor = color;
        this.pointSize = thickness * DEFAULT_POINT_SIZE_SCALE;
    }

    public InWorldLineAnnotation(Vector3f from, Vector3f to, ColorValue color) {
        this(from, to, color, DEFAULT_THICKNESS);
    }

    public Vector3f from() {
        return points.get(0);
    }

    public Vector3f to() {
        return points.get(points.size() - 1);
    }

    public List<Vector3f> points() {
        return points;
    }

    public ColorValue color() {
        return color;
    }

    public float thickness() {
        return thickness;
    }

    public Arrow arrow() {
        return arrow;
    }

    public void setArrow(Arrow arrow) {
        this.arrow = arrow != null ? arrow : Arrow.NONE;
    }

    public boolean showPoints() {
        return showPoints;
    }

    public void setShowPoints(boolean showPoints) {
        this.showPoints = showPoints;
    }

    public ColorValue pointColor() {
        return pointColor;
    }

    public void setPointColor(ColorValue pointColor) {
        this.pointColor = pointColor != null ? pointColor : color;
    }

    public float pointSize() {
        return pointSize;
    }

    public void setPointSize(float pointSize) {
        this.pointSize = pointSize;
    }

    public void addPointStyle(PointStyle style) {
        if (style != null) {
            pointStyles.add(style);
        }
    }

    public List<PointStyle> pointStyles() {
        return List.copyOf(pointStyles);
    }

    @Nullable
    public PointStyle pointStyleFor(int pointIndex) {
        PointStyle result = null;
        for (PointStyle style : pointStyles) {
            if (style.index() == pointIndex) {
                result = style;
            }
        }
        return result;
    }

    public enum Arrow {

        NONE("none"),
        START("start"),
        END("end");

        private final String serializedName;

        Arrow(String serializedName) {
            this.serializedName = serializedName;
        }

        public String serializedName() {
            return serializedName;
        }

        public static Arrow fromSerializedName(String raw) {
            if (raw == null || raw.trim()
                .isEmpty()) {
                return NONE;
            }
            String normalized = raw.trim()
                .toLowerCase(Locale.ROOT);
            for (Arrow arrow : values()) {
                if (arrow.serializedName.equals(normalized)) {
                    return arrow;
                }
            }
            throw new IllegalArgumentException("arrow must be 'start' or 'end'.");
        }
    }

    public static class PointStyle {

        private final int index;
        @Nullable
        private final Boolean show;
        @Nullable
        private final ColorValue color;
        @Nullable
        private final Float size;

        public PointStyle(int index, @Nullable Boolean show, @Nullable ColorValue color, @Nullable Float size) {
            this.index = index;
            this.show = show;
            this.color = color;
            this.size = size;
        }

        public int index() {
            return index;
        }

        @Nullable
        public Boolean show() {
            return show;
        }

        @Nullable
        public ColorValue color() {
            return color;
        }

        @Nullable
        public Float size() {
            return size;
        }
    }
}
