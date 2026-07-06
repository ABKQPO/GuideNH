package com.hfstudio.guidenh.guide.document.block.shapes;

import java.util.EnumMap;
import java.util.Map;

import com.hfstudio.guidenh.guide.document.LytRect;
import com.hfstudio.guidenh.guide.internal.mermaid.MermaidNodeShape;
import com.hfstudio.guidenh.guide.render.RenderContext;

public final class FlowchartShapes {

    private static final Map<MermaidNodeShape, ShapeRenderer> RENDERERS = new EnumMap<>(MermaidNodeShape.class);

    static {
        RENDERERS.put(MermaidNodeShape.DEFAULT, new RectShape());
        RENDERERS.put(MermaidNodeShape.SQUARE, new RectShape());
        RENDERERS.put(MermaidNodeShape.ROUNDED, new RoundedRectShape());
        RENDERERS.put(MermaidNodeShape.STADIUM, new StadiumShape());
        RENDERERS.put(MermaidNodeShape.SUBPROCESS, new SubprocessShape());
        RENDERERS.put(MermaidNodeShape.DIAMOND, new DiamondShape());
        RENDERERS.put(MermaidNodeShape.CYLINDER, new CylinderShape());
        RENDERERS.put(MermaidNodeShape.HEXAGON, new HexagonShape());
        RENDERERS.put(MermaidNodeShape.CIRCLE, new CircleShape());
        RENDERERS.put(MermaidNodeShape.DOUBLE_CIRCLE, new DoubleCircleShape());
        RENDERERS.put(MermaidNodeShape.CLOUD, new CloudShape());
        RENDERERS.put(MermaidNodeShape.BANG, new BangShape());
        RENDERERS.put(MermaidNodeShape.ASYMMETRIC, new AsymmetricShape());
        RENDERERS.put(MermaidNodeShape.TRAPEZOID, new TrapezoidShape());
        RENDERERS.put(MermaidNodeShape.ELLIPSE, new CircleShape());
    }

    private FlowchartShapes() {}

    public static void render(RenderContext context, LytRect rect, MermaidNodeShape shape, int backgroundColor,
        int borderColor) {
        ShapeRenderer renderer = RENDERERS.get(shape);
        if (renderer != null) {
            renderer.render(context, rect, backgroundColor, borderColor);
        }
    }

    public static LytRect contentBounds(LytRect nodeRect, MermaidNodeShape shape, int cw, int ch, int padX, int padY) {
        ShapeRenderer renderer = RENDERERS.get(shape);
        return renderer != null ? renderer.contentBounds(nodeRect, cw, ch, padX, padY) : nodeRect;
    }

    public static LytRect minNodeRect(MermaidNodeShape shape, int cw, int ch, int padX, int padY) {
        ShapeRenderer renderer = RENDERERS.get(shape);
        return renderer != null ? renderer.minNodeRect(cw, ch, padX, padY)
            : new LytRect(0, 0, cw + 2 * padX, ch + 2 * padY);
    }

    public static boolean hasAccentBar(MermaidNodeShape shape) {
        return switch (shape) {
            case DEFAULT, SQUARE, ROUNDED, STADIUM, SUBPROCESS -> true;
            default -> false;
        };
    }
}
