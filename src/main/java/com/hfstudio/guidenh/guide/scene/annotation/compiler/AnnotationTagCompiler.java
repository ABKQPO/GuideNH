package com.hfstudio.guidenh.guide.scene.annotation.compiler;

import javax.annotation.Nullable;

import com.hfstudio.guidenh.guide.compiler.PageCompiler;
import com.hfstudio.guidenh.guide.document.LytErrorSink;
import com.hfstudio.guidenh.guide.document.block.LytVBox;
import com.hfstudio.guidenh.guide.document.interaction.ContentTooltip;
import com.hfstudio.guidenh.guide.scene.CameraSettings;
import com.hfstudio.guidenh.guide.scene.LytGuidebookScene;
import com.hfstudio.guidenh.guide.scene.annotation.SceneAnnotation;
import com.hfstudio.guidenh.guide.scene.element.SceneElementTagCompiler;
import com.hfstudio.guidenh.guide.scene.level.GuidebookLevel;
import com.hfstudio.guidenh.libs.mdast.mdx.model.MdxJsxElementFields;

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
        var children = el.children();
        if (children != null && !children.isEmpty()) {
            var contentBox = new LytVBox();
            compiler.compileBlockContext(children, contentBox);
            if (!contentBox.getChildren()
                .isEmpty()) {
                annotation.setTooltip(new ContentTooltip(contentBox));
            }
        }
        scene.addAnnotation(annotation);
    }

    @Nullable
    protected abstract SceneAnnotation createAnnotation(PageCompiler compiler, LytErrorSink errorSink,
        MdxJsxElementFields el);
}
