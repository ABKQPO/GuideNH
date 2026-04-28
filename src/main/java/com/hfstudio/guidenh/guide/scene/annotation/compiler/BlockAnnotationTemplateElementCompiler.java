package com.hfstudio.guidenh.guide.scene.annotation.compiler;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.hfstudio.guidenh.guide.compiler.PageCompiler;
import com.hfstudio.guidenh.guide.compiler.tags.MdxAttrs;
import com.hfstudio.guidenh.guide.document.LytErrorSink;
import com.hfstudio.guidenh.guide.scene.CameraSettings;
import com.hfstudio.guidenh.guide.scene.annotation.SceneAnnotation;
import com.hfstudio.guidenh.guide.scene.element.SceneElementTagCompiler;
import com.hfstudio.guidenh.guide.scene.level.GuidebookLevel;
import com.hfstudio.guidenh.guide.scene.support.BlockAnnotationTemplateExpander;
import com.hfstudio.guidenh.guide.scene.support.GuideBlockMatcher;
import com.hfstudio.guidenh.libs.mdast.mdx.model.MdxJsxElementFields;
import com.hfstudio.guidenh.libs.mdast.model.MdAstNode;
import com.hfstudio.guidenh.libs.mdast.model.MdAstParent;
import com.hfstudio.guidenh.libs.unist.UnistNode;

public class BlockAnnotationTemplateElementCompiler implements SceneElementTagCompiler {

    public static final Map<String, AnnotationTagCompiler> TEMPLATE_ANNOTATION_COMPILERS = createTemplateCompilers();

    @Override
    public Set<String> getTagNames() {
        return Collections.singleton("BlockAnnotationTemplate");
    }

    @Override
    public void compile(GuidebookLevel level, CameraSettings camera, PageCompiler compiler, LytErrorSink errorSink,
        MdxJsxElementFields el) {
        var scene = AnnotationTagCompiler.CURRENT_SCENE.get();
        if (scene == null) {
            errorSink.appendError(compiler, "BlockAnnotationTemplate used outside <GameScene>", el);
            return;
        }

        String rawId = MdxAttrs.getString(compiler, errorSink, el, "id", null);
        if (rawId == null || rawId.trim()
            .isEmpty()) {
            errorSink.appendError(compiler, "Missing id attribute.", el);
            return;
        }

        GuideBlockMatcher matcher;
        try {
            matcher = GuideBlockMatcher.parse(rawId);
        } catch (IllegalArgumentException e) {
            errorSink.appendError(compiler, e.getMessage(), el);
            return;
        }

        List<SceneAnnotation> templateAnnotations = collectTemplateAnnotations(compiler, errorSink, el);
        for (SceneAnnotation annotation : BlockAnnotationTemplateExpander.expand(level, matcher, templateAnnotations)) {
            scene.addAnnotation(annotation);
        }
    }

    public static List<SceneAnnotation> collectTemplateAnnotations(PageCompiler compiler, LytErrorSink errorSink,
        MdxJsxElementFields el) {
        List<SceneAnnotation> templateAnnotations = new ArrayList<>();
        for (Object child : el.children()) {
            if (child instanceof UnistNode childNode) {
                collectTemplateAnnotationFromNode(compiler, errorSink, childNode, templateAnnotations);
            }
        }
        return templateAnnotations;
    }

    public static void collectTemplateAnnotationFromNode(PageCompiler compiler, LytErrorSink errorSink, UnistNode node,
        List<SceneAnnotation> templateAnnotations) {
        MdxJsxElementFields childElement = unwrapJsxElement(node);
        if (childElement != null) {
            AnnotationTagCompiler annotationCompiler = TEMPLATE_ANNOTATION_COMPILERS.get(childElement.name());
            if (annotationCompiler == null) {
                errorSink.appendError(
                    compiler,
                    "Unsupported BlockAnnotationTemplate child <" + childElement.name() + ">",
                    childElement);
                return;
            }

            SceneAnnotation annotation = annotationCompiler.createAnnotation(compiler, errorSink, childElement);
            if (annotation == null) {
                return;
            }

            AnnotationTagCompiler.applyTooltip(compiler, annotation, childElement);
            templateAnnotations.add(annotation);
            return;
        }

        if (node instanceof MdAstParent<?>parent) {
            for (Object child : parent.children()) {
                if (child instanceof UnistNode childNode) {
                    collectTemplateAnnotationFromNode(compiler, errorSink, childNode, templateAnnotations);
                }
            }
        }
    }

    public static MdxJsxElementFields unwrapJsxElement(UnistNode node) {
        if (node instanceof MdxJsxElementFields elementFields) {
            return elementFields;
        }
        if (!(node instanceof MdAstParent<?>parent)) {
            return null;
        }

        MdxJsxElementFields found = null;
        for (Object child : parent.children()) {
            if (!(child instanceof UnistNode childNode)) {
                continue;
            }
            if (isIgnorableNode(childNode)) {
                continue;
            }
            MdxJsxElementFields nested = unwrapJsxElement(childNode);
            if (nested == null) {
                return null;
            }
            if (found != null) {
                return null;
            }
            found = nested;
        }
        return found;
    }

    public static boolean isIgnorableNode(UnistNode node) {
        if (node instanceof MdxJsxElementFields) {
            return false;
        }
        if (node instanceof MdAstNode astNode) {
            return astNode.toText()
                .trim()
                .isEmpty();
        }
        return false;
    }

    public static Map<String, AnnotationTagCompiler> createTemplateCompilers() {
        Map<String, AnnotationTagCompiler> compilers = new HashMap<>();
        registerTemplateCompiler(compilers, new BlockAnnotationElementCompiler());
        registerTemplateCompiler(compilers, new BoxAnnotationElementCompiler());
        registerTemplateCompiler(compilers, new LineAnnotationElementCompiler());
        registerTemplateCompiler(compilers, new DiamondAnnotationElementCompiler());
        return Collections.unmodifiableMap(compilers);
    }

    public static void registerTemplateCompiler(Map<String, AnnotationTagCompiler> compilers,
        AnnotationTagCompiler compiler) {
        for (String tagName : compiler.getTagNames()) {
            compilers.put(tagName, compiler);
        }
    }
}
