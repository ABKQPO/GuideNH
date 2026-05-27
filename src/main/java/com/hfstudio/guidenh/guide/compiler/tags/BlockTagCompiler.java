package com.hfstudio.guidenh.guide.compiler.tags;

import com.hfstudio.guidenh.guide.compiler.PageCompiler;
import com.hfstudio.guidenh.guide.compiler.TagCompiler;
import com.hfstudio.guidenh.guide.document.block.ContentAlign;
import com.hfstudio.guidenh.guide.document.block.ContentWrapMode;
import com.hfstudio.guidenh.guide.document.block.LytAlignedBlock;
import com.hfstudio.guidenh.guide.document.block.LytBlock;
import com.hfstudio.guidenh.guide.document.block.LytBlockContainer;
import com.hfstudio.guidenh.guide.document.block.LytDocumentFloat;
import com.hfstudio.guidenh.guide.document.flow.InlineBlockAlignment;
import com.hfstudio.guidenh.guide.document.flow.LytFlowInlineBlock;
import com.hfstudio.guidenh.guide.document.flow.LytFlowParent;
import com.hfstudio.guidenh.libs.mdast.mdx.model.MdxJsxElementFields;
import com.hfstudio.guidenh.libs.mdast.mdx.model.MdxJsxFlowElement;
import com.hfstudio.guidenh.libs.mdast.mdx.model.MdxJsxTextElement;

/**
 * Base class for tag compilers that produce block-level content.
 *
 * <p>
 * Block tags may optionally carry {@code wrap} and {@code align} attributes that apply
 * Word-style content embedding behaviour:
 *
 * <ul>
 * <li>{@code wrap} — one of {@code inline}, {@code square}, {@code tight}, {@code through},
 * {@code top-bottom}, {@code behind}, {@code front}. Defaults to {@code inline}.
 * <li>{@code align} — one of {@code left}, {@code center}, {@code right}. Defaults to
 * {@code left}.
 * </ul>
 *
 * <p>
 * When used in <em>block context</em>:
 * <ul>
 * <li>{@code wrap=square/tight/through} wraps the block in a {@link LytDocumentFloat} so
 * that subsequent paragraphs flow around it on the opposite side (left or right,
 * determined by {@code align}).
 * <li>Any other wrap mode with {@code align=center} or {@code align=right} wraps the block
 * in a {@link LytAlignedBlock} that repositions it horizontally without affecting text flow.
 * </ul>
 *
 * <p>
 * When used in <em>flow context</em> (inline inside a paragraph) the legacy {@code float}
 * attribute is also recognised for backwards compatibility with {@link FloatingImageCompiler}.
 */
public abstract class BlockTagCompiler implements TagCompiler {

    private static final int DEFAULT_FLOAT_MARGIN = 5;

    protected abstract void compile(PageCompiler compiler, LytBlockContainer parent, MdxJsxElementFields el);

    @Override
    public final void compileFlowContext(PageCompiler compiler, LytFlowParent parent, MdxJsxTextElement el) {
        compile(compiler, node -> {
            var alignment = resolveFlowAlignment(el);
            applyFlowFloatMargins(node, alignment);
            var inlineBlock = new LytFlowInlineBlock();
            inlineBlock.setBlock(node);
            inlineBlock.setAlignment(alignment);
            parent.append(inlineBlock);
        }, el);
    }

    @Override
    public final void compileBlockContext(PageCompiler compiler, LytBlockContainer parent, MdxJsxFlowElement el) {
        String wrapAttr = el.getAttributeString("wrap", null);
        String alignAttr = el.getAttributeString("align", null);
        var wrapMode = ContentWrapMode.fromString(wrapAttr);
        var align = ContentAlign.fromString(alignAttr);
        compile(compiler, node -> parent.append(applyBlockEmbed(node, wrapMode, align)), el);
    }

    private static InlineBlockAlignment resolveFlowAlignment(MdxJsxElementFields el) {
        String wrapAttr = el.getAttributeString("wrap", null);
        String alignAttr = el.getAttributeString("align", null);
        if (wrapAttr != null || alignAttr != null) {
            var wrapMode = ContentWrapMode.fromString(wrapAttr);
            if (wrapMode.isDocumentFloat()) {
                var align = ContentAlign.fromString(alignAttr);
                return align == ContentAlign.RIGHT ? InlineBlockAlignment.FLOAT_RIGHT : InlineBlockAlignment.FLOAT_LEFT;
            }
            return InlineBlockAlignment.INLINE;
        }
        return switch (el.getAttributeString("float", "none")) {
            case "left" -> InlineBlockAlignment.FLOAT_LEFT;
            case "right" -> InlineBlockAlignment.FLOAT_RIGHT;
            default -> InlineBlockAlignment.INLINE;
        };
    }

    private static void applyFlowFloatMargins(LytBlock node, InlineBlockAlignment alignment) {
        if (alignment == InlineBlockAlignment.FLOAT_LEFT) {
            node.setMarginRight(Math.max(node.getMarginRight(), DEFAULT_FLOAT_MARGIN));
            node.setMarginBottom(Math.max(node.getMarginBottom(), DEFAULT_FLOAT_MARGIN));
        } else if (alignment == InlineBlockAlignment.FLOAT_RIGHT) {
            node.setMarginLeft(Math.max(node.getMarginLeft(), DEFAULT_FLOAT_MARGIN));
            node.setMarginBottom(Math.max(node.getMarginBottom(), DEFAULT_FLOAT_MARGIN));
        }
    }

    private static LytBlock applyBlockEmbed(LytBlock node, ContentWrapMode wrapMode, ContentAlign align) {
        if (wrapMode.isDocumentFloat()) {
            return new LytDocumentFloat(node, align == ContentAlign.RIGHT);
        }
        if (align != ContentAlign.LEFT) {
            node = new LytAlignedBlock(node, align);
        }
        return PageCompiler.wrapFloatAwareIfNeeded(node);
    }
}
