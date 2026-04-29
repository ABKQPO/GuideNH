package com.hfstudio.guidenh.guide.scene.annotation.compiler;

import java.util.Collections;
import java.util.Set;

import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import com.hfstudio.guidenh.guide.color.ConstantColor;
import com.hfstudio.guidenh.guide.compiler.PageCompiler;
import com.hfstudio.guidenh.guide.compiler.tags.MdxAttrs;
import com.hfstudio.guidenh.guide.document.LytErrorSink;
import com.hfstudio.guidenh.guide.scene.annotation.InWorldLineAnnotation;
import com.hfstudio.guidenh.guide.scene.annotation.SceneAnnotation;
import com.hfstudio.guidenh.libs.mdast.mdx.model.MdxJsxElementFields;

/**
 * {@code <LineAnnotation from="x y z" to="x y z" color="..." thickness="..." alwaysOnTop />}。
 */
public class LineAnnotationElementCompiler extends AnnotationTagCompiler {

    @Override
    public Set<String> getTagNames() {
        return Collections.singleton("LineAnnotation");
    }

    @Override
    @Nullable
    protected SceneAnnotation createAnnotation(PageCompiler compiler, LytErrorSink errorSink, MdxJsxElementFields el) {
        var from = MdxAttrs.getVector3(compiler, errorSink, el, "from", new Vector3f());
        var to = MdxAttrs.getVector3(compiler, errorSink, el, "to", new Vector3f());
        var color = MdxAttrs.getColor(compiler, errorSink, el, "color", ConstantColor.WHITE);
        var thickness = MdxAttrs
            .getFloat(compiler, errorSink, el, "thickness", InWorldLineAnnotation.DEFAULT_THICKNESS);
        var alwaysOnTop = MdxAttrs.getBoolean(compiler, errorSink, el, "alwaysOnTop", false);

        var ann = new InWorldLineAnnotation(from, to, color, thickness);
        ann.setAlwaysOnTop(alwaysOnTop);
        return ann;
    }
}
