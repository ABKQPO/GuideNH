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

public class BlockAnnotationElementCompiler extends AnnotationTagCompiler {

    @Override
    public Set<String> getTagNames() {
        return Collections.singleton("BlockAnnotation");
    }

    @Override
    @Nullable
    protected SceneAnnotation createAnnotation(PageCompiler compiler, LytErrorSink errorSink, MdxJsxElementFields el) {
        var pos = MdxAttrs.getVector3(compiler, errorSink, el, "pos", new Vector3f());
        var min = new Vector3f(pos.x, pos.y, pos.z);
        var max = new Vector3f(pos.x + 1f, pos.y + 1f, pos.z + 1f);

        var color = MdxAttrs.getColor(compiler, errorSink, el, "color", ConstantColor.WHITE);
        var thickness = MdxAttrs.getFloat(compiler, errorSink, el, "thickness", InWorldBoxAnnotation.DEFAULT_THICKNESS);
        var alwaysOnTop = MdxAttrs.getBoolean(compiler, errorSink, el, "alwaysOnTop", false);

        var ann = new InWorldBoxAnnotation(min, max, color, thickness);
        ann.setAlwaysOnTop(alwaysOnTop);
        return ann;
    }
}
