package com.hfstudio.guidenh.guide.scene.element;

import java.util.Collections;
import java.util.Set;

import com.hfstudio.guidenh.guide.compiler.PageCompiler;
import com.hfstudio.guidenh.guide.compiler.tags.MdxAttrs;
import com.hfstudio.guidenh.guide.document.LytErrorSink;
import com.hfstudio.guidenh.guide.scene.CameraSettings;
import com.hfstudio.guidenh.guide.scene.level.GuidebookLevel;
import com.hfstudio.guidenh.guide.scene.support.GuideBlockMatcher;
import com.hfstudio.guidenh.guide.scene.support.RemoveBlocksExecutor;
import com.hfstudio.guidenh.libs.mdast.mdx.model.MdxJsxElementFields;

public class RemoveBlocksElementCompiler implements SceneElementTagCompiler {

    @Override
    public Set<String> getTagNames() {
        return Collections.singleton("RemoveBlocks");
    }

    @Override
    public void compile(GuidebookLevel level, CameraSettings camera, PageCompiler compiler, LytErrorSink errorSink,
        MdxJsxElementFields el) {
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

        RemoveBlocksExecutor.execute(level, matcher);
    }
}
