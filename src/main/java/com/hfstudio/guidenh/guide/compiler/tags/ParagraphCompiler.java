package com.hfstudio.guidenh.guide.compiler.tags;

import java.util.Collections;
import java.util.Set;

import com.hfstudio.guidenh.guide.compiler.PageCompiler;
import com.hfstudio.guidenh.guide.document.block.LytBlockContainer;
import com.hfstudio.guidenh.guide.document.block.LytParagraph;
import com.hfstudio.guidenh.libs.mdast.mdx.model.MdxJsxElementFields;

public class ParagraphCompiler extends BlockTagCompiler {

    @Override
    public Set<String> getTagNames() {
        return Collections.singleton("p");
    }

    @Override
    protected void compile(PageCompiler compiler, LytBlockContainer parent, MdxJsxElementFields el) {
        LytParagraph paragraph = new LytParagraph();
        compiler.compileFlowContext(el.children(), paragraph);
        paragraph.setMarginTop(PageCompiler.DEFAULT_ELEMENT_SPACING);
        paragraph.setMarginBottom(PageCompiler.DEFAULT_ELEMENT_SPACING);
        parent.append(paragraph);
    }
}
