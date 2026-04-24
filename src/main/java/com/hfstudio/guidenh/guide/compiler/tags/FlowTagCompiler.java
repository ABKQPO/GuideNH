package com.hfstudio.guidenh.guide.compiler.tags;

import com.hfstudio.guidenh.guide.compiler.PageCompiler;
import com.hfstudio.guidenh.guide.compiler.TagCompiler;
import com.hfstudio.guidenh.guide.document.block.LytBlockContainer;
import com.hfstudio.guidenh.guide.document.block.LytParagraph;
import com.hfstudio.guidenh.guide.document.flow.LytFlowParent;
import com.hfstudio.guidenh.libs.mdast.mdx.model.MdxJsxElementFields;
import com.hfstudio.guidenh.libs.mdast.mdx.model.MdxJsxFlowElement;
import com.hfstudio.guidenh.libs.mdast.mdx.model.MdxJsxTextElement;

/**
 * Compiler base-class for tag compilers that compile flow content but allow the flow content to be used in block
 * context by wrapping it in a paragraph.
 */
public abstract class FlowTagCompiler implements TagCompiler {

    protected abstract void compile(PageCompiler compiler, LytFlowParent parent, MdxJsxElementFields el);

    @Override
    public void compileFlowContext(PageCompiler compiler, LytFlowParent parent, MdxJsxTextElement el) {
        compile(compiler, parent, el);
    }

    @Override
    public final void compileBlockContext(PageCompiler compiler, LytBlockContainer parent, MdxJsxFlowElement el) {
        var paragraph = new LytParagraph();
        compile(compiler, paragraph, el);
        parent.append(paragraph);
    }
}
