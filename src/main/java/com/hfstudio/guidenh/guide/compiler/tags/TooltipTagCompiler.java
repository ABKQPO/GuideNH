package com.hfstudio.guidenh.guide.compiler.tags;

import java.util.Collections;
import java.util.Set;

import com.hfstudio.guidenh.guide.compiler.PageCompiler;
import com.hfstudio.guidenh.guide.compiler.TagCompiler;
import com.hfstudio.guidenh.guide.document.block.LytBlockContainer;
import com.hfstudio.guidenh.guide.document.block.LytParagraph;
import com.hfstudio.guidenh.guide.document.block.LytVBox;
import com.hfstudio.guidenh.guide.document.flow.LytFlowParent;
import com.hfstudio.guidenh.guide.document.flow.LytTooltipSpan;
import com.hfstudio.guidenh.guide.document.interaction.ContentTooltip;
import com.hfstudio.guidenh.libs.mdast.mdx.model.MdxJsxElementFields;
import com.hfstudio.guidenh.libs.mdast.mdx.model.MdxJsxFlowElement;
import com.hfstudio.guidenh.libs.mdast.mdx.model.MdxJsxTextElement;

public class TooltipTagCompiler implements TagCompiler {

    @Override
    public Set<String> getTagNames() {
        return Collections.singleton("Tooltip");
    }

    @Override
    public void compileFlowContext(PageCompiler compiler, LytFlowParent parent, MdxJsxTextElement el) {
        compileCommon(compiler, parent, el);
    }

    @Override
    public void compileBlockContext(PageCompiler compiler, LytBlockContainer parent, MdxJsxFlowElement el) {
        var paragraph = new LytParagraph();
        compileCommon(compiler, paragraph, el);
        parent.append(paragraph);
    }

    private void compileCommon(PageCompiler compiler, LytFlowParent parent, MdxJsxElementFields el) {
        var label = el.getAttributeString("label", "");
        if (label.isEmpty()) label = "tooltip";

        var contentBox = new LytVBox();
        compiler.compileBlockTagChildren(el, contentBox);

        var span = new LytTooltipSpan();
        span.modifyStyle(style -> style.underlined(true));
        span.appendText(label);
        span.setTooltip(new ContentTooltip(contentBox));
        parent.append(span);
    }
}
