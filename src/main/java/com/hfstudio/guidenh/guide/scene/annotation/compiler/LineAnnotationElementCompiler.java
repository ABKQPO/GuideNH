package com.hfstudio.guidenh.guide.scene.annotation.compiler;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import com.hfstudio.guidenh.guide.color.ColorValue;
import com.hfstudio.guidenh.guide.color.ConstantColor;
import com.hfstudio.guidenh.guide.compiler.PageCompiler;
import com.hfstudio.guidenh.guide.compiler.tags.MdxAttrs;
import com.hfstudio.guidenh.guide.document.LytErrorSink;
import com.hfstudio.guidenh.guide.scene.annotation.InWorldLineAnnotation;
import com.hfstudio.guidenh.guide.scene.annotation.LineAnnotationPointParser;
import com.hfstudio.guidenh.guide.scene.annotation.SceneAnnotation;
import com.hfstudio.guidenh.libs.mdast.mdx.model.MdxJsxElementFields;
import com.hfstudio.guidenh.libs.mdast.model.MdAstAnyContent;
import com.hfstudio.guidenh.libs.unist.UnistPosition;

/**
 * {@code <LineAnnotation from="x y z" to="x y z" color="..." thickness="..." alwaysOnTop />}銆?
 */
public class LineAnnotationElementCompiler extends AnnotationTagCompiler {

    @Override
    public Set<String> getTagNames() {
        return Collections.singleton("LineAnnotation");
    }

    @Override
    @Nullable
    protected SceneAnnotation createAnnotation(PageCompiler compiler, LytErrorSink errorSink, MdxJsxElementFields el) {
        List<Vector3f> points = parsePoints(compiler, errorSink, el);
        var color = MdxAttrs.getColor(compiler, errorSink, el, "color", ConstantColor.WHITE);
        var thickness = MdxAttrs
            .getFloat(compiler, errorSink, el, "thickness", InWorldLineAnnotation.DEFAULT_THICKNESS);
        var alwaysOnTop = MdxAttrs.getBoolean(compiler, errorSink, el, "alwaysOnTop", false);
        var showPoints = MdxAttrs.getBoolean(compiler, errorSink, el, "showPoints", false);
        var pointColor = MdxAttrs.getColor(compiler, errorSink, el, "pointColor", color);
        var pointSize = MdxAttrs
            .getFloat(compiler, errorSink, el, "pointSize", thickness * InWorldLineAnnotation.DEFAULT_POINT_SIZE_SCALE);

        var ann = new InWorldLineAnnotation(points, color, thickness);
        ann.setAlwaysOnTop(alwaysOnTop);
        ann.setShowPoints(showPoints);
        ann.setPointColor(pointColor);
        ann.setPointSize(pointSize);
        ann.setArrow(parseArrow(compiler, errorSink, el));
        collectPointStyles(compiler, errorSink, el, ann);
        return ann;
    }

    private static List<Vector3f> parsePoints(PageCompiler compiler, LytErrorSink errorSink, MdxJsxElementFields el) {
        String rawPoints = MdxAttrs.getString(compiler, errorSink, el, "points", null);
        if (rawPoints != null && !rawPoints.trim()
            .isEmpty()) {
            try {
                return LineAnnotationPointParser.parsePoints(rawPoints);
            } catch (IllegalArgumentException e) {
                errorSink.appendError(compiler, e.getMessage(), el);
                return Arrays.asList(new Vector3f(), new Vector3f());
            }
        }

        var from = MdxAttrs.getVector3(compiler, errorSink, el, "from", new Vector3f());
        var to = MdxAttrs.getVector3(compiler, errorSink, el, "to", new Vector3f());
        return Arrays.asList(from, to);
    }

    private static InWorldLineAnnotation.Arrow parseArrow(PageCompiler compiler, LytErrorSink errorSink,
        MdxJsxElementFields el) {
        String rawArrow = MdxAttrs.getString(compiler, errorSink, el, "arrow", null);
        try {
            return InWorldLineAnnotation.Arrow.fromSerializedName(rawArrow);
        } catch (IllegalArgumentException e) {
            errorSink.appendError(compiler, e.getMessage(), el);
            return InWorldLineAnnotation.Arrow.NONE;
        }
    }

    private static void collectPointStyles(PageCompiler compiler, LytErrorSink errorSink, MdxJsxElementFields el,
        InWorldLineAnnotation ann) {
        List<? extends MdAstAnyContent> children = compiler.reparseBlockTagChildren(el);
        for (MdAstAnyContent child : children) {
            MdxJsxElementFields pointElement = BlockAnnotationTemplateElementCompiler.unwrapJsxElement(child);
            if (pointElement == null || !"LinePoint".equals(pointElement.name())) {
                continue;
            }

            int index = MdxAttrs.getInt(compiler, errorSink, pointElement, "index", -1);
            if (index < 0 || index >= ann.points()
                .size()) {
                errorSink.appendError(
                    compiler,
                    "LinePoint index must be between 0 and " + (ann.points()
                        .size() - 1),
                    pointElement);
                continue;
            }
            Boolean show = pointElement.hasAttribute("show")
                ? MdxAttrs.getBoolean(compiler, errorSink, pointElement, "show", true)
                : null;
            ColorValue pointColor = pointElement.hasAttribute("color")
                ? MdxAttrs.getColor(compiler, errorSink, pointElement, "color", ann.pointColor())
                : null;
            Float pointSize = pointElement.hasAttribute("size")
                ? MdxAttrs.getFloat(compiler, errorSink, pointElement, "size", ann.pointSize())
                : null;
            ann.addPointStyle(new InWorldLineAnnotation.PointStyle(index, show, pointColor, pointSize));
        }
    }

    public static List<MdAstAnyContent> tooltipChildren(PageCompiler compiler, MdxJsxElementFields el) {
        List<MdAstAnyContent> tooltipChildren = new ArrayList<>();
        for (MdAstAnyContent child : compiler.reparseBlockTagChildren(el)) {
            MdxJsxElementFields pointElement = BlockAnnotationTemplateElementCompiler.unwrapJsxElement(child);
            if (pointElement != null && "LinePoint".equals(pointElement.name())) {
                continue;
            }
            tooltipChildren.add(child);
        }
        return tooltipChildren;
    }

    public static String tooltipSource(PageCompiler compiler, MdxJsxElementFields el) {
        String source = compiler.getBlockTagChildrenSource(el);
        if (source == null || source.isEmpty()) {
            return source;
        }
        List<? extends MdAstAnyContent> reparsedChildren = compiler.reparseBlockTagChildren(el);
        StringBuilder builder = new StringBuilder(source.length());
        boolean first = true;
        for (MdAstAnyContent child : reparsedChildren) {
            MdxJsxElementFields pointElement = BlockAnnotationTemplateElementCompiler.unwrapJsxElement(child);
            if (pointElement != null && "LinePoint".equals(pointElement.name())) {
                continue;
            }
            String childSource = sourceForNode(source, child.position());
            if (childSource == null || childSource.isEmpty()) {
                continue;
            }
            if (!first) {
                builder.append('\n');
            }
            builder.append(childSource);
            first = false;
        }
        return builder.length() > 0 ? builder.toString() : null;
    }

    @Nullable
    private static String sourceForNode(String source, @Nullable UnistPosition position) {
        if (position == null || position.start() == null || position.end() == null) {
            return null;
        }
        int start = position.start()
            .offset();
        int end = position.end()
            .offset();
        if (start < 0 || end <= start || end > source.length()) {
            return null;
        }
        return source.substring(start, end);
    }
}
