package com.hfstudio.guidenh.guide.compiler.tags;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import com.hfstudio.guidenh.guide.compiler.PageCompiler;
import com.hfstudio.guidenh.guide.compiler.tags.DetailsContentExtractor.DetailsContent;
import com.hfstudio.guidenh.guide.document.block.LytBlockContainer;
import com.hfstudio.guidenh.guide.document.block.LytDetailsBlock;
import com.hfstudio.guidenh.libs.mdast.mdx.model.MdxJsxElementFields;
import com.hfstudio.guidenh.libs.mdast.model.MdAstAnyContent;

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
        details.setFallbackSummaryText("Details");

        String childrenSource = compiler.getBlockTagChildrenSource(el);
        if (childrenSource != null) {
            DetailsContent extracted = DetailsContentExtractor.extract(childrenSource);
            if (extracted.summaryMarkdown() != null) {
                details.getSummaryBox()
                    .clearContent();
                compiler.compileInlineMarkdown(extracted.summaryMarkdown(), details.getSummaryBox());
                if (details.getSummaryBox()
                    .isEmpty()) {
                    details.setFallbackSummaryText("Details");
                }
            }
            compiler.compileBlockMarkdown(extracted.bodyMarkdown(), details.getContentBox());
        } else {
            compileAstChildren(compiler, details, el.children());
        }

        Integer width = readOptionalInt(el, "width");
        Integer height = readOptionalInt(el, "height");
        if (width != null) {
            details.setPreferredWidth(width);
        }
        if (height != null) {
            details.setPreferredContentHeight(height);
        }
        parent.append(details);
    }

    private void compileAstChildren(PageCompiler compiler, LytDetailsBlock details,
        List<? extends MdAstAnyContent> children) {
        int bodyStart = 0;
        MdxJsxElementFields summaryElement = findLeadingSummary(children);
        if (summaryElement != null) {
            details.getSummaryBox()
                .clearContent();
            compiler.compileInlineFragment(summaryElement.children(), details.getSummaryBox());
            if (details.getSummaryBox()
                .isEmpty()) {
                details.setFallbackSummaryText("Details");
            }
            bodyStart = 1;
        }

        if (bodyStart < children.size()) {
            List<? extends MdAstAnyContent> bodyChildren = children.subList(bodyStart, children.size());
            compiler.compileBlockContextInSourceContext(bodyChildren, details.getContentBox());
        }
    }

    private MdxJsxElementFields findLeadingSummary(List<? extends MdAstAnyContent> children) {
        if (children.isEmpty()) {
            return null;
        }
        if (children.getFirst() instanceof MdxJsxElementFields summaryElement
            && "summary".equals(summaryElement.name())) {
            return summaryElement;
        }
        return null;
    }

    private Integer readOptionalInt(MdxJsxElementFields el, String name) {
        String raw = el.getAttributeString(name, null);
        if (raw == null || raw.trim()
            .isEmpty()) {
            return null;
        }
        try {
            return Math.max(0, Integer.parseInt(raw.trim()));
        } catch (NumberFormatException ignored) {
            return null;
        }
    }
}
