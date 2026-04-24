package com.hfstudio.guidenh.guide.compiler.tags;

import com.hfstudio.guidenh.guide.compiler.PageCompiler;
import com.hfstudio.guidenh.guide.compiler.TagCompiler;
import com.hfstudio.guidenh.guide.document.block.LytBlockContainer;
import com.hfstudio.guidenh.guide.document.flow.InlineBlockAlignment;
import com.hfstudio.guidenh.guide.document.flow.LytFlowInlineBlock;
import com.hfstudio.guidenh.guide.document.flow.LytFlowParent;
import com.hfstudio.guidenh.libs.mdast.mdx.model.MdxJsxElementFields;
import com.hfstudio.guidenh.libs.mdast.mdx.model.MdxJsxFlowElement;
import com.hfstudio.guidenh.libs.mdast.mdx.model.MdxJsxTextElement;

/**
 * Compiler base-class for tag compilers that compile block content but allow the block content to be used in flow
 * context by wrapping it in an inline block.
 */
public abstract class BlockTagCompiler implements TagCompiler {

    protected abstract void compile(PageCompiler compiler, LytBlockContainer parent, MdxJsxElementFields el);

    @Override
    public final void compileFlowContext(PageCompiler compiler, LytFlowParent parent, MdxJsxTextElement el) {
        compile(compiler, node -> {
            var alignmentAttr = el.getAttributeString("float", "none");
            var alignment = switch (alignmentAttr) {
                case "left" -> InlineBlockAlignment.FLOAT_LEFT;
                case "right" -> InlineBlockAlignment.FLOAT_RIGHT;
                default -> InlineBlockAlignment.INLINE;
            };

            var inlineBlock = new LytFlowInlineBlock();
            inlineBlock.setBlock(node);
            inlineBlock.setAlignment(alignment);
            parent.append(inlineBlock);
        }, el);
    }

    @Override
    public final void compileBlockContext(PageCompiler compiler, LytBlockContainer parent, MdxJsxFlowElement el) {
        compile(compiler, parent, el);
    }
}
