package com.hfstudio.guidenh.guide.compiler.tags;

import java.util.Collections;
import java.util.Set;

import com.hfstudio.guidenh.guide.compiler.PageCompiler;
import com.hfstudio.guidenh.guide.compiler.TagCompiler;
import com.hfstudio.guidenh.guide.document.block.LytBlockContainer;
import com.hfstudio.guidenh.libs.mdast.mdx.model.MdxJsxFlowElement;

public class DivTagCompiler implements TagCompiler {

    @Override
    public Set<String> getTagNames() {
        return Collections.singleton("div");
    }

    @Override
    public void compileBlockContext(PageCompiler compiler, LytBlockContainer parent, MdxJsxFlowElement el) {
        compiler.compileBlockTagChildren(el, parent);
    }
}
