package com.hfstudio.guidenh.guide.compiler.tags;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Set;

import net.minecraft.util.ResourceLocation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    private static final Logger LOG = LoggerFactory.getLogger(MermaidCompiler.class);

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
            LytMermaidMindmap block = new LytMermaidMindmap(document, source);
            int width = MdxAttrs.getInt(compiler, parent, el, "width", 0);
            int height = MdxAttrs.getInt(compiler, parent, el, "height", 0);
            if (width > 0 || height > 0) {
                block.setPreferredSize(width, height);
            }
            LOG.info(
                "Compiled Mermaid runtime block for page {} with root='{}', children={}, sourceLength={}, width={}, height={}",
                compiler.getPageId(),
                document.getRoot()
                    .getText(),
                document.getRoot()
                    .getChildren()
                    .size(),
                source.length(),
                width,
                height);
            parent.append(block);
        } catch (IllegalArgumentException e) {
            LOG.warn(
                "Failed to compile Mermaid runtime block for page {} from source: {}",
                compiler.getPageId(),
                source,
                e);
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
            if (data == null) {
                LOG.warn(
                    "Mermaid src '{}' for page {} could not be loaded as asset {}",
                    src,
                    compiler.getPageId(),
                    mermaidId);
                return null;
            }
            String loaded = MermaidMindmapParser.normalize(new String(data, StandardCharsets.UTF_8));
            LOG.info(
                "Loaded Mermaid src '{}' for page {} as asset {} ({} chars)",
                src,
                compiler.getPageId(),
                mermaidId,
                loaded.length());
            return loaded;
        } catch (IllegalArgumentException e) {
            LOG.warn("Failed to resolve Mermaid src '{}' for page {}", src, compiler.getPageId(), e);
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
