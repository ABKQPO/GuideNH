package com.hfstudio.guidenh.guide.compiler.tags;

import java.util.Collections;
import java.util.Set;

import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.guide.color.SymbolicColor;
import com.hfstudio.guidenh.guide.compiler.PageCompiler;
import com.hfstudio.guidenh.guide.document.block.LytAlertBox;
import com.hfstudio.guidenh.guide.document.block.LytBlockContainer;
import com.hfstudio.guidenh.guide.document.block.LytNode;
import com.hfstudio.guidenh.guide.document.block.LytParagraph;
import com.hfstudio.guidenh.guide.document.block.LytQuoteBox;
import com.hfstudio.guidenh.guide.document.block.LytVBox;
import com.hfstudio.guidenh.guide.document.flow.LytFlowContent;
import com.hfstudio.guidenh.guide.internal.markdown.MarkdownRuntimeBlocks;
import com.hfstudio.guidenh.guide.internal.markdown.MarkdownRuntimeBlocks.BlockquoteDirective;
import com.hfstudio.guidenh.guide.internal.markdown.MarkdownRuntimeBlocks.QuoteIconSpec;
import com.hfstudio.guidenh.guide.style.BorderStyle;
import com.hfstudio.guidenh.libs.mdast.mdx.model.MdxJsxElementFields;

public class BlockquoteCompiler extends BlockTagCompiler {

    @Override
    public Set<String> getTagNames() {
        return Collections.singleton("blockquote");
    }

    @Override
    protected void compile(PageCompiler compiler, LytBlockContainer parent, MdxJsxElementFields el) {
        BlockquoteDirective directive = MarkdownRuntimeBlocks.parseBlockquoteDirective(el);
        if (directive != null && directive.alertType() != null) {
            LytAlertBox alertBox = new LytAlertBox();
            alertBox.setTitle(
                directive.alertType()
                    .displayText(),
                directive.alertType());
            alertBox.setMarginTop(PageCompiler.DEFAULT_ELEMENT_SPACING);
            alertBox.setMarginBottom(PageCompiler.DEFAULT_ELEMENT_SPACING);
            compileDirectiveBody(compiler, directive, alertBox);
            normalizeBlockMargins(alertBox);
            parent.append(PageCompiler.wrapFloatAwareIfNeeded(alertBox));
            return;
        }

        if (directive != null && (directive.title() != null || directive.icon() != null)) {
            LytQuoteBox quoteBox = new LytQuoteBox();
            quoteBox.setQuoteStyle(directive.accentColor(), directive.title(), buildQuoteIcon(directive.icon()));
            quoteBox.setMarginTop(PageCompiler.DEFAULT_ELEMENT_SPACING);
            quoteBox.setMarginBottom(PageCompiler.DEFAULT_ELEMENT_SPACING);
            compileDirectiveBody(compiler, directive, quoteBox);
            normalizeBlockMargins(quoteBox);
            shiftFirstParagraphDown(quoteBox, 1);
            parent.append(PageCompiler.wrapFloatAwareIfNeeded(quoteBox));
            return;
        }

        // Plain blockquote
        LytVBox blockquote = new LytVBox();
        blockquote.setBackgroundColor(SymbolicColor.BLOCKQUOTE_BACKGROUND);
        blockquote.setPadding(5);
        blockquote.setPaddingLeft(10);
        blockquote.setBorderLeft(new BorderStyle(SymbolicColor.TABLE_BORDER, 2));
        blockquote.setMarginTop(PageCompiler.DEFAULT_ELEMENT_SPACING);
        blockquote.setMarginBottom(PageCompiler.DEFAULT_ELEMENT_SPACING);
        compiler.compileBlockContext(el.children(), blockquote);
        normalizeBlockMargins(blockquote);
        shiftFirstParagraphDown(blockquote, 1);
        parent.append(PageCompiler.wrapFloatAwareIfNeeded(blockquote));
    }

    private void compileDirectiveBody(PageCompiler compiler, BlockquoteDirective directive, LytBlockContainer parent) {
        // When there's a remainingText override and the first paragraph is still present
        // at the head of the children list, replace its leading text.
        // Otherwise — just compile children normally.
        if (!directive.children()
            .isEmpty() && directive.firstParagraph() != null
            && directive.children()
                .get(0) == directive.firstParagraph()
            && directive.remainingText() != null
            && !directive.remainingText()
                .isEmpty()) {
            // Clone the first paragraph with the remaining text overriding the leading text
            compiler.compileBlockContext(Collections.singletonList(directive.firstParagraph()), parent);
            for (int i = 1; i < directive.children()
                .size(); i++) {
                compiler.compileBlockContext(
                    Collections.singletonList(
                        directive.children()
                            .get(i)),
                    parent);
            }
        } else {
            compiler.compileBlockContext(directive.children(), parent);
        }
    }

    private void normalizeBlockMargins(LytNode box) {
        var boxChildren = box.getChildren();
        if (!boxChildren.isEmpty()) {
            if (boxChildren.get(0) instanceof LytParagraph) {
                ((LytParagraph) boxChildren.get(0)).setMarginTop(0);
            }
            if (boxChildren.get(boxChildren.size() - 1) instanceof LytParagraph) {
                ((LytParagraph) boxChildren.get(boxChildren.size() - 1)).setMarginBottom(0);
            }
        }
    }

    private void shiftFirstParagraphDown(LytNode box, int pixels) {
        var boxChildren = box.getChildren();
        if (!boxChildren.isEmpty() && boxChildren.get(0) instanceof LytParagraph) {
            LytParagraph first = (LytParagraph) boxChildren.get(0);
            first.setPaddingTop(first.getPaddingTop() + pixels);
        }
    }

    @Nullable
    private LytFlowContent buildQuoteIcon(@Nullable QuoteIconSpec icon) {
        // The original buildQuoteIcon resolved item stacks from icon specs.
        // For now return null — icon rendering will be added in a later phase.
        return null;
    }
}
