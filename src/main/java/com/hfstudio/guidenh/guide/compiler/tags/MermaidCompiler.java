package com.hfstudio.guidenh.guide.compiler.tags;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import net.minecraft.util.ResourceLocation;

import com.hfstudio.guidenh.guide.compiler.IdUtils;
import com.hfstudio.guidenh.guide.compiler.IndexingContext;
import com.hfstudio.guidenh.guide.compiler.IndexingSink;
import com.hfstudio.guidenh.guide.compiler.PageCompiler;
import com.hfstudio.guidenh.guide.document.block.LytBlock;
import com.hfstudio.guidenh.guide.document.block.LytBlockContainer;
import com.hfstudio.guidenh.guide.document.block.LytParagraph;
import com.hfstudio.guidenh.guide.document.block.LytVBox;
import com.hfstudio.guidenh.guide.internal.mermaid.MermaidMindmapNodeContentExtractor;
import com.hfstudio.guidenh.guide.internal.mermaid.MermaidMindmapParser;
import com.hfstudio.guidenh.libs.mdast.mdx.model.MdxJsxElementFields;
import com.hfstudio.guidenh.libs.mdast.mdx.model.MdxJsxFlowElement;

public class MermaidCompiler extends BlockTagCompiler {

    @Override
    public Set<String> getTagNames() {
        return Collections.singleton("Mermaid");
    }

    @Override
    protected void compile(PageCompiler compiler, LytBlockContainer parent, MdxJsxElementFields el) {
        String src = null;
        String sourceText = null;

        String srcStr;
        try {
            srcStr = MdxAttrs.getString(el, "src", null);
        } catch (MdxAttrs.AttributeException e) {
            parent.appendError(compiler, e.getMessage(), el);
            return;
        }

        if (srcStr != null && !srcStr.trim()
            .isEmpty()) {
            ResourceLocation mermaidId;
            try {
                mermaidId = IdUtils.resolveLink(srcStr.trim(), compiler.getPageId());
            } catch (IllegalArgumentException e) {
                parent.appendError(compiler, "Malformed Mermaid src: " + srcStr, el);
                return;
            }
            src = mermaidId.toString();
        } else {
            // Prefer raw source text so Mermaid DSL syntax (brackets, links, etc.)
            // is not consumed by markdown parsing. PR #24.
            String rawSource = compiler.getBlockTagChildrenSource(el);
            if (rawSource != null) {
                sourceText = MermaidMindmapParser.normalize(stripNodeContentBlocks(rawSource));
            } else {
                sourceText = MermaidMindmapNodeContentExtractor.extractDiagramSource(el.children());
            }
        }

        if ((sourceText == null || sourceText.trim()
            .isEmpty()) && src == null) {
            parent.appendError(compiler, "Mermaid requires inline content or a non-empty src attribute.", el);
            return;
        }

        int width = MdxAttrs.getInt(compiler, parent, el, "width", 0);
        int height = MdxAttrs.getInt(compiler, parent, el, "height", 0);

        Map<String, LytBlock> nodeContentBlocks = compileNodeContentBlocks(compiler, parent, el);

        MermaidPlaceholder placeholder = new MermaidPlaceholder(src, sourceText, width, height, nodeContentBlocks);
        placeholder.appendText("[Mermaid]");
        parent.append(placeholder);
    }

    @Override
    public void index(IndexingContext indexer, MdxJsxElementFields el, IndexingSink sink) {
        // NB: Phase 2 loaded src-based Mermaid content and indexed the actual diagram source.
        // Phase 3 src-based content is resolved at MOUNT time by MermaidScript, so index() only
        // indexes the src path string. Inline content (no src attribute) is still indexed here.
        // Full indexing for src-based mermaid requires a post-mount indexing pass (TBD).
        String src;
        try {
            src = MdxAttrs.getString(el, "src", null);
        } catch (MdxAttrs.AttributeException e) {
            src = null;
        }

        if (src != null && !src.trim()
            .isEmpty()) {
            sink.appendText(el, src);
            sink.appendBreak();
        } else {
            String inlineSource = MermaidMindmapNodeContentExtractor.extractDiagramSource(el.children());
            if (inlineSource != null && !inlineSource.trim()
                .isEmpty()) {
                sink.appendText(el, inlineSource);
                sink.appendBreak();
            }
        }
    }

    private Map<String, LytBlock> compileNodeContentBlocks(PageCompiler compiler, LytBlockContainer parent,
        MdxJsxElementFields mermaidElement) {
        // NB: Phase 2 cross-referenced NodeContent IDs against the parsed MermaidMindmapNode tree
        // (via indexNodesById), validated unknown IDs, and provided inline-markdown fallback for
        // nodes without explicit NodeContent. Phase 3 defers tree construction to MermaidScript
        // (MOUNT time), so cross-validation must happen at runtime. See MermaidScript for the
        // runtime counterpart.
        Map<String, LytBlock> result = new LinkedHashMap<>();
        for (MdxJsxFlowElement child : MermaidMindmapNodeContentExtractor
            .collectNodeContentElements(mermaidElement.children())) {
            String id = MermaidMindmapNodeContentExtractor.readNodeContentId(child);
            if (id == null) {
                parent.appendError(compiler, "Mermaid <NodeContent> requires a non-empty id attribute.", child);
                continue;
            }
            LytBlock compiled = compileNodeContentBlock(compiler, child);
            if (compiled != null) {
                result.put(id, compiled);
            }
        }
        return result;
    }

    private LytBlock compileNodeContentBlock(PageCompiler compiler, MdxJsxFlowElement explicitContent) {
        if (explicitContent == null) {
            return null;
        }
        LytVBox box = new LytVBox();
        compiler.withBlockTagChildrenSourceContext(
            explicitContent,
            () -> compiler.compileBlockContext(explicitContent.children(), box));
        return box.getChildren()
            .isEmpty() ? null : box;
    }

    public static class MermaidPlaceholder extends LytParagraph {

        public final String src;
        public final String sourceText;
        public final int width;
        public final int height;
        public final Map<String, LytBlock> nodeContentBlocks;

        public MermaidPlaceholder(String src, String sourceText, int width, int height,
            Map<String, LytBlock> nodeContentBlocks) {
            this.src = src;
            this.sourceText = sourceText;
            this.width = width;
            this.height = height;
            this.nodeContentBlocks = nodeContentBlocks;
            setStyleClass("Mermaid");
            setStyle(LytParagraph.PLACEHOLDER_STYLE);
        }
    }

    private static String stripNodeContentBlocks(String source) {
        StringBuilder result = new StringBuilder(source.length());
        int depth = 0;
        for (int i = 0; i < source.length(); i++) {
            char c = source.charAt(i);
            if (depth == 0 && source.startsWith("<", i)) {
                int tagEnd = source.indexOf('>', i);
                if (tagEnd > i) {
                    String tag = source.substring(i, tagEnd + 1);
                    if (tag.startsWith("<NodeContent")) {
                        depth = 1;
                        i = tagEnd;
                        continue;
                    }
                }
            }
            if (depth > 0) {
                if (source.startsWith("</NodeContent>", i)) {
                    depth--;
                    if (depth == 0) {
                        i += "</NodeContent>".length() - 1;
                        continue;
                    }
                } else if (source.startsWith("<NodeContent", i)) {
                    depth++;
                }
                continue;
            }
            result.append(c);
        }
        return result.toString();
    }
}
