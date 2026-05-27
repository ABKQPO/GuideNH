package com.hfstudio.guidenh.guide.scene.annotation.compiler;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.guide.compiler.PageCompiler;
import com.hfstudio.guidenh.guide.document.LytErrorSink;
import com.hfstudio.guidenh.guide.document.block.LytVBox;
import com.hfstudio.guidenh.guide.document.interaction.ContentTooltip;
import com.hfstudio.guidenh.guide.scene.CameraSettings;
import com.hfstudio.guidenh.guide.scene.LytGuidebookScene;
import com.hfstudio.guidenh.guide.scene.StructureLibSceneConditionParser;
import com.hfstudio.guidenh.guide.scene.annotation.SceneAnnotation;
import com.hfstudio.guidenh.guide.scene.element.SceneElementTagCompiler;
import com.hfstudio.guidenh.guide.scene.level.GuidebookLevel;
import com.hfstudio.guidenh.libs.mdast.mdx.model.MdxJsxElementFields;
import com.hfstudio.guidenh.libs.mdast.model.MdAstAnyContent;

public abstract class AnnotationTagCompiler implements SceneElementTagCompiler {

    public static final ThreadLocal<LytGuidebookScene> CURRENT_SCENE = new ThreadLocal<>();

    @Override
    public final void compile(GuidebookLevel level, CameraSettings camera, PageCompiler compiler,
        LytErrorSink errorSink, MdxJsxElementFields el) {
        var scene = CURRENT_SCENE.get();
        if (scene == null) {
            errorSink.appendError(compiler, "Annotation tag used outside <GameScene>", el);
            return;
        }
        SceneAnnotation annotation = createAnnotation(compiler, errorSink, el);
        if (annotation == null) return;
        applyTooltip(compiler, annotation, el);
        annotation.setStructureLibCondition(StructureLibSceneConditionParser.parse(compiler, errorSink, el));
        scene.addAnnotation(annotation);
    }

    static void applyTooltip(PageCompiler compiler, SceneAnnotation annotation, MdxJsxElementFields el) {
        boolean lineAnnotation = "LineAnnotation".equals(el.name());
        List<? extends MdAstAnyContent> children = lineAnnotation
            ? LineAnnotationElementCompiler.tooltipChildren(compiler, el)
            : el.children();
        if (children == null || children.isEmpty()) {
            return;
        }

        var contentBox = new LytVBox();
        if (lineAnnotation) {
            String tooltipSource = LineAnnotationElementCompiler.tooltipSource(compiler, el);
            if (tooltipSource != null && !tooltipSource.isEmpty()) {
                compiler.withSourceContext(tooltipSource, () -> compiler.compileBlockContext(children, contentBox));
            } else {
                compiler.compileBlockContext(children, contentBox);
            }
        } else {
            compiler.compileBlockTagChildren(el, contentBox);
        }
        if (!contentBox.getChildren()
            .isEmpty()) {
            annotation.setTooltip(new ContentTooltip(contentBox));
        }
    }

    @Nullable
    protected abstract SceneAnnotation createAnnotation(PageCompiler compiler, LytErrorSink errorSink,
        MdxJsxElementFields el);
}
