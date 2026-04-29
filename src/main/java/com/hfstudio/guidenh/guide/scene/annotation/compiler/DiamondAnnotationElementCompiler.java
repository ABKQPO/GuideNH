package com.hfstudio.guidenh.guide.scene.annotation.compiler;

import java.util.Collections;
import java.util.Set;

import javax.annotation.Nullable;

import org.joml.Vector3f;

import com.hfstudio.guidenh.guide.color.ConstantColor;
import com.hfstudio.guidenh.guide.compiler.PageCompiler;
import com.hfstudio.guidenh.guide.compiler.tags.MdxAttrs;
import com.hfstudio.guidenh.guide.document.LytErrorSink;
import com.hfstudio.guidenh.guide.scene.annotation.DiamondAnnotation;
import com.hfstudio.guidenh.guide.scene.annotation.SceneAnnotation;
import com.hfstudio.guidenh.libs.mdast.mdx.model.MdxJsxElementFields;

/**
 * {@code <DiamondAnnotation pos="x y z" color="..." alwaysOnTop />}.
 * Default color is bright green when {@code color} is omitted.
 */
public class DiamondAnnotationElementCompiler extends AnnotationTagCompiler {

    /** Default green used when the MDX tag omits the {@code color} attribute. */
    public static final ConstantColor DEFAULT_DIAMOND_COLOR = new ConstantColor(0xFF00E000);

    @Override
    public Set<String> getTagNames() {
        return Collections.singleton("DiamondAnnotation");
    }

    @Override
    @Nullable
    protected SceneAnnotation createAnnotation(PageCompiler compiler, LytErrorSink errorSink, MdxJsxElementFields el) {
        var pos = MdxAttrs.getVector3(compiler, errorSink, el, "pos", new Vector3f());
        var color = MdxAttrs.getColor(compiler, errorSink, el, "color", DEFAULT_DIAMOND_COLOR);
        var alwaysOnTop = MdxAttrs.getBoolean(compiler, errorSink, el, "alwaysOnTop", false);
        var annotation = new DiamondAnnotation(pos, color);
        annotation.setAlwaysOnTop(alwaysOnTop);
        return annotation;
    }
}
