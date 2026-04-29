package com.hfstudio.guidenh.guide.scene.annotation.compiler;

import java.util.Collections;
import java.util.Set;

import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import com.hfstudio.guidenh.guide.color.ConstantColor;
import com.hfstudio.guidenh.guide.compiler.PageCompiler;
import com.hfstudio.guidenh.guide.compiler.tags.MdxAttrs;
import com.hfstudio.guidenh.guide.document.LytErrorSink;
import com.hfstudio.guidenh.guide.scene.annotation.InWorldBoxAnnotation;
import com.hfstudio.guidenh.guide.scene.annotation.SceneAnnotation;
import com.hfstudio.guidenh.libs.mdast.mdx.model.MdxJsxElementFields;

/**
 * {@code <BoxAnnotation min="x y z" max="x y z" color="#AARRGGBB" thickness="0.03125" alwaysOnTop />}。
 */
public class BoxAnnotationElementCompiler extends AnnotationTagCompiler {

    @Override
    public Set<String> getTagNames() {
        return Collections.singleton("BoxAnnotation");
    }

    @Override
    @Nullable
    protected SceneAnnotation createAnnotation(PageCompiler compiler, LytErrorSink errorSink, MdxJsxElementFields el) {
        var min = MdxAttrs.getVector3(compiler, errorSink, el, "min", new Vector3f());
        var max = MdxAttrs.getVector3(compiler, errorSink, el, "max", new Vector3f());
        ensureMinMax(min, max);

        var color = MdxAttrs.getColor(compiler, errorSink, el, "color", ConstantColor.WHITE);
        var thickness = MdxAttrs.getFloat(compiler, errorSink, el, "thickness", InWorldBoxAnnotation.DEFAULT_THICKNESS);
        var alwaysOnTop = MdxAttrs.getBoolean(compiler, errorSink, el, "alwaysOnTop", false);

        var ann = new InWorldBoxAnnotation(min, max, color, thickness);
        ann.setAlwaysOnTop(alwaysOnTop);
        return ann;
    }

    public static void ensureMinMax(Vector3f min, Vector3f max) {
        if (min.x > max.x) {
            float t = min.x;
            min.x = max.x;
            max.x = t;
        }
        if (min.y > max.y) {
            float t = min.y;
            min.y = max.y;
            max.y = t;
        }
        if (min.z > max.z) {
            float t = min.z;
            min.z = max.z;
            max.z = t;
        }
    }
}
