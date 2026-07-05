package com.hfstudio.guidenh.guide.internal.mermaid;

public enum MermaidNodeShape {

    DEFAULT,
    ROUNDED,
    SQUARE,
    STADIUM,
    SUBPROCESS,
    DIAMOND,
    CYLINDER,
    ASYMMETRIC,
    TRAPEZOID,
    HEXAGON,
    CIRCLE,
    DOUBLE_CIRCLE,
    ELLIPSE,
    BANG,
    CLOUD;

    public boolean isDiamond() {
        return this == DIAMOND;
    }
}
