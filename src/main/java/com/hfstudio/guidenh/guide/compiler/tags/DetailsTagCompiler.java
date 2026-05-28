package com.hfstudio.guidenh.guide.compiler.tags;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import com.hfstudio.guidenh.guide.compiler.PageCompiler;
import com.hfstudio.guidenh.guide.document.block.LytBlockContainer;
import com.hfstudio.guidenh.guide.document.block.LytDetailsBlock;
import com.hfstudio.guidenh.guide.document.block.LytSizeBox;
import com.hfstudio.guidenh.libs.mdast.mdx.model.MdxJsxElementFields;
import com.hfstudio.guidenh.libs.mdast.mdx.model.MdxJsxFlowElement;
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

        String detailsBodySource = compiler.getBlockTagChildrenSource(el);
        List<? extends MdAstAnyContent> children = detailsBodySource != null ? compiler.reparseBlockTagChildren(el)
            : el.children();
        int bodyStart = 0;
        if (!children.isEmpty() && children.getFirst() instanceof MdxJsxFlowElement summaryElement
            && "summary".equals(summaryElement.name())) {
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
            if (detailsBodySource != null) {
                compiler.withSourceContext(
                    detailsBodySource,
                    () -> compiler.compileBlockContextInSourceContext(bodyChildren, details.getContentBox()));
            } else {
                compiler.compileBlockContextInSourceContext(bodyChildren, details.getContentBox());
            }
        }

        Integer width = readOptionalInt(el, "width");
        Integer height = readOptionalInt(el, "height");
        if (width != null || height != null) {
            LytSizeBox sizeBox = new LytSizeBox();
            if (width != null) {
                sizeBox.setPreferredWidth(width);
            }
            if (height != null) {
                sizeBox.setPreferredHeight(height);
            }
            sizeBox.append(details);
            parent.append(sizeBox);
            return;
        }
        parent.append(details);
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
