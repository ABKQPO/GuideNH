package com.hfstudio.guidenh.guide.compiler.tags;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Set;

import net.minecraft.util.ResourceLocation;

import com.hfstudio.guidenh.guide.compiler.IdUtils;
import com.hfstudio.guidenh.guide.compiler.IndexingContext;
import com.hfstudio.guidenh.guide.compiler.IndexingSink;
import com.hfstudio.guidenh.guide.compiler.PageCompiler;
import com.hfstudio.guidenh.guide.document.block.LytBlockContainer;
import com.hfstudio.guidenh.guide.document.block.LytMermaidMindmap;
import com.hfstudio.guidenh.guide.internal.mermaid.MermaidMindmapParser;
import com.hfstudio.guidenh.libs.mdast.mdx.model.MdxJsxElementFields;
import com.hfstudio.guidenh.libs.mdast.model.MdAstAnyContent;
import com.hfstudio.guidenh.libs.mdast.model.MdAstBlockquote;
import com.hfstudio.guidenh.libs.mdast.model.MdAstBreak;
import com.hfstudio.guidenh.libs.mdast.model.MdAstCode;
import com.hfstudio.guidenh.libs.mdast.model.MdAstEmphasis;
import com.hfstudio.guidenh.libs.mdast.model.MdAstHTML;
import com.hfstudio.guidenh.libs.mdast.model.MdAstInlineCode;
import com.hfstudio.guidenh.libs.mdast.model.MdAstLink;
import com.hfstudio.guidenh.libs.mdast.model.MdAstLinkReference;
import com.hfstudio.guidenh.libs.mdast.model.MdAstList;
import com.hfstudio.guidenh.libs.mdast.model.MdAstListItem;
import com.hfstudio.guidenh.libs.mdast.model.MdAstParagraph;
import com.hfstudio.guidenh.libs.mdast.model.MdAstParent;
import com.hfstudio.guidenh.libs.mdast.model.MdAstStrong;
import com.hfstudio.guidenh.libs.mdast.model.MdAstText;

public class MermaidCompiler extends BlockTagCompiler {

    @Override
    public Set<String> getTagNames() {
        return Collections.singleton("Mermaid");
    }

    @Override
    protected void compile(PageCompiler compiler, LytBlockContainer parent, MdxJsxElementFields el) {
        String source = resolveSource(compiler, parent, el);
        if (source == null || source.trim()
            .isEmpty()) {
            parent.appendError(compiler, "Mermaid requires inline content or a non-empty src attribute.", el);
            return;
        }

        try {
            var document = MermaidMindmapParser.parse(source);
            parent.append(new LytMermaidMindmap(document, source));
        } catch (IllegalArgumentException e) {
            parent.appendError(compiler, "Unsupported Mermaid runtime block: " + e.getMessage(), el);
        }
    }

    @Override
    public void index(IndexingContext indexer, MdxJsxElementFields el, IndexingSink sink) {
        String source = resolveSource(indexer, el);
        if (source != null && !source.trim()
            .isEmpty()) {
            sink.appendText(el, source);
            sink.appendBreak();
        }
    }

    private String resolveSource(PageCompiler compiler, LytBlockContainer parent, MdxJsxElementFields el) {
        String src;
        try {
            src = MdxAttrs.getString(el, "src", null);
        } catch (MdxAttrs.AttributeException e) {
            parent.appendError(compiler, e.getMessage(), el);
            return null;
        }
        if (src != null && !src.trim()
            .isEmpty()) {
            return loadSource(compiler, src.trim());
        }
        return MermaidMindmapParser.normalize(extractInlineSource(el));
    }

    private String resolveSource(IndexingContext indexer, MdxJsxElementFields el) {
        String src;
        try {
            src = MdxAttrs.getString(el, "src", null);
        } catch (MdxAttrs.AttributeException e) {
            return null;
        }

        if (src != null && !src.trim()
            .isEmpty()) {
            return loadSource(indexer, src.trim());
        }
        return MermaidMindmapParser.normalize(extractInlineSource(el));
    }

    private String loadSource(PageCompiler compiler, String src) {
        try {
            ResourceLocation mermaidId = IdUtils.resolveLink(src, compiler.getPageId());
            byte[] data = compiler.loadAsset(mermaidId);
            return data != null ? MermaidMindmapParser.normalize(new String(data, StandardCharsets.UTF_8)) : null;
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private String loadSource(IndexingContext indexer, String src) {
        try {
            ResourceLocation mermaidId = IdUtils.resolveLink(src, indexer.getPageId());
            byte[] data = indexer.loadAsset(mermaidId);
            return data != null ? MermaidMindmapParser.normalize(new String(data, StandardCharsets.UTF_8)) : null;
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private String extractInlineSource(MdxJsxElementFields el) {
        StringBuilder builder = new StringBuilder();
        appendSource(builder, el.children(), false);
        return builder.toString()
            .trim();
    }

    private void appendSource(StringBuilder builder, Iterable<? extends MdAstAnyContent> children,
        boolean appendBreak) {
        boolean first = true;
        for (MdAstAnyContent child : children) {
            if (!first && appendBreak) {
                builder.append('\n');
            }
            appendSource(builder, child);
            first = false;
        }
    }

    private void appendSource(StringBuilder builder, MdAstAnyContent content) {
        if (content instanceof MdAstText text) {
            builder.append(text.value);
        } else if (content instanceof MdAstBreak) {
            builder.append('\n');
        } else if (content instanceof MdAstInlineCode code) {
            builder.append(code.value);
        } else if (content instanceof MdAstCode codeBlock) {
            builder.append(codeBlock.value);
        } else if (content instanceof MdAstHTML html) {
            builder.append(html.value);
        } else if (content instanceof MdAstLink link) {
            appendSource(builder, link.children(), false);
        } else if (content instanceof MdAstLinkReference reference) {
            appendSource(builder, reference.children(), false);
        } else if (content instanceof MdAstStrong strong) {
            appendSource(builder, strong.children(), false);
        } else if (content instanceof MdAstEmphasis emphasis) {
            appendSource(builder, emphasis.children(), false);
        } else if (content instanceof MdAstParagraph paragraph) {
            appendSource(builder, paragraph.children(), false);
            builder.append('\n');
        } else if (content instanceof MdAstBlockquote blockquote) {
            appendSource(builder, blockquote.children(), true);
            builder.append('\n');
        } else if (content instanceof MdAstList list) {
            for (MdAstAnyContent child : list.children()) {
                appendSource(builder, child);
                builder.append('\n');
            }
        } else if (content instanceof MdAstListItem listItem) {
            appendSource(builder, listItem.children(), true);
        } else if (content instanceof MdAstParent<?>parent) {
            appendSource(builder, parent.children(), false);
        }
    }
}
