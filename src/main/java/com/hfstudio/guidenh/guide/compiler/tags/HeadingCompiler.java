package com.hfstudio.guidenh.guide.compiler.tags;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import com.hfstudio.guidenh.guide.compiler.PageCompiler;
import com.hfstudio.guidenh.guide.document.block.LytBlockContainer;
import com.hfstudio.guidenh.guide.document.block.LytHeading;
import com.hfstudio.guidenh.libs.mdast.mdx.model.MdxJsxElementFields;

public class HeadingCompiler extends BlockTagCompiler {

    private static final Set<String> TAG_NAMES = Collections.unmodifiableSet(
        new HashSet<>(Arrays.asList("h1", "h2", "h3", "h4", "h5", "h6")));

    @Override
    public Set<String> getTagNames() {
        return TAG_NAMES;
    }

    @Override
    protected void compile(PageCompiler compiler, LytBlockContainer parent, MdxJsxElementFields el) {
        LytHeading heading = new LytHeading();
        int depth = Integer.parseInt(el.getAttributeString("depth", "0"));
        heading.setDepth(depth);
        compiler.compileFlowContext(el.children(), heading);
        parent.append(heading);
    }
}
