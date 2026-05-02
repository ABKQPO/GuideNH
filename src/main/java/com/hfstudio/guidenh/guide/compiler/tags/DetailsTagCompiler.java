package com.hfstudio.guidenh.guide.compiler.tags;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import com.hfstudio.guidenh.guide.compiler.PageCompiler;
import com.hfstudio.guidenh.guide.document.block.LytBlockContainer;
import com.hfstudio.guidenh.guide.document.block.LytDetailsBlock;
import com.hfstudio.guidenh.libs.mdast.mdx.model.MdxJsxElementFields;
import com.hfstudio.guidenh.libs.mdast.mdx.model.MdxJsxFlowElement;
import com.hfstudio.guidenh.libs.mdast.model.MdAstAnyContent;
import com.hfstudio.guidenh.libs.mdast.model.MdAstParagraph;

public class DetailsTagCompiler extends BlockTagCompiler {

    @Override
    public Set<String> getTagNames() {
        return Collections.singleton("details");
    }

    @Override
    protected void compile(PageCompiler compiler, LytBlockContainer parent, MdxJsxElementFields el) {
        LytDetailsBlock details = new LytDetailsBlock();
        details.setMarginTop(PageCompiler.DEFAULT_ELEMENT_SPACING);
        details.setMarginBottom(PageCompiler.DEFAULT_ELEMENT_SPACING);
        details.setOpen(el.hasAttribute("open"));

        List<? extends MdAstAnyContent> children = el.children();
        int bodyStart = 0;
        if (!children.isEmpty() && children.get(0) instanceof MdxJsxFlowElement summaryElement
            && "summary".equals(summaryElement.name())) {
            String summaryText = collectText(summaryElement);
            if (!summaryText.trim()
                .isEmpty()) {
                details.setSummaryText(summaryText.trim());
            }
            bodyStart = 1;
        }
        if (bodyStart == 0) {
            details.setSummaryText("Details");
        }

        for (int i = bodyStart; i < children.size(); i++) {
            compiler.compileBlockContext(Collections.singletonList(children.get(i)), details.getContentBox());
        }
        parent.append(details);
    }

    private String collectText(MdxJsxFlowElement element) {
        StringBuilder buffer = new StringBuilder();
        for (MdAstAnyContent child : element.children()) {
            if (child instanceof MdAstParagraph paragraph) {
                paragraph.toText(buffer);
            }
        }
        return buffer.toString();
    }
}
