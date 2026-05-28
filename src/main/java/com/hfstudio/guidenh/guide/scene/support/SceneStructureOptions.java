package com.hfstudio.guidenh.guide.scene.support;

import com.hfstudio.guidenh.guide.compiler.PageCompiler;
import com.hfstudio.guidenh.guide.compiler.tags.MdxAttrs;
import com.hfstudio.guidenh.guide.document.LytErrorSink;
import com.hfstudio.guidenh.libs.mdast.mdx.model.MdxJsxElementFields;

public class SceneStructureOptions {

    protected SceneStructureOptions() {}

    public static boolean isFormed(PageCompiler compiler, LytErrorSink errorSink, MdxJsxElementFields el) {
        return MdxAttrs.getBoolean(compiler, errorSink, el, "formed", false);
    }
}
