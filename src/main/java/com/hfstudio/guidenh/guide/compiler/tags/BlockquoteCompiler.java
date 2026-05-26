package com.hfstudio.guidenh.guide.compiler.tags;

import java.util.Collections;
import java.util.Set;

import com.hfstudio.guidenh.guide.compiler.PageCompiler;
import com.hfstudio.guidenh.guide.document.block.LytBlockContainer;
import com.hfstudio.guidenh.guide.document.block.LytParagraph;
import com.hfstudio.guidenh.guide.document.block.LytVBox;
import com.hfstudio.guidenh.guide.color.SymbolicColor;
import com.hfstudio.guidenh.guide.style.BorderStyle;
import com.hfstudio.guidenh.libs.mdast.mdx.model.MdxJsxElementFields;

public class BlockquoteCompiler extends BlockTagCompiler {

    @Override
    public Set<String> getTagNames() {
        return Collections.singleton("blockquote");
    }

    @Override
    protected void compile(PageCompiler compiler, LytBlockContainer parent, MdxJsxElementFields el) {
        LytVBox blockquote = new LytVBox();
        blockquote.setBackgroundColor(SymbolicColor.BLOCKQUOTE_BACKGROUND);
        blockquote.setPadding(5);
        blockquote.setPaddingLeft(10);
        blockquote.setBorderLeft(new BorderStyle(SymbolicColor.TABLE_BORDER, 2));
        blockquote.setMarginTop(PageCompiler.DEFAULT_ELEMENT_SPACING);
        blockquote.setMarginBottom(PageCompiler.DEFAULT_ELEMENT_SPACING);
        compiler.compileBlockContext(el.children(), blockquote);

        // Normalize block margins (equivalent to normalizeBlockMargins in PageCompiler)
        var children = blockquote.getChildren();
        if (!children.isEmpty()) {
            if (children.get(0) instanceof LytParagraph first) {
                first.setMarginTop(0);
            }
            if (children.get(children.size() - 1) instanceof LytParagraph last) {
                last.setMarginBottom(0);
            }
        }
        parent.append(blockquote);
    }
}
