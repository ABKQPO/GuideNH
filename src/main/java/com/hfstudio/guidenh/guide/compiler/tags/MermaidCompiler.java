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
            String rawTagBodySource = compiler.getBlockTagChildrenSource(el);
            if (rawTagBodySource != null && !rawTagBodySource.trim()
                .isEmpty()) {
                sourceText = MermaidMindmapNodeContentExtractor.stripExplicitNodeContentBlocks(rawTagBodySource);
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
        placeholder.appendText("Loading Mermaid...");
        parent.append(placeholder);
    }

    @Override
    public void index(IndexingContext indexer, MdxJsxElementFields el, IndexingSink sink) {
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
            setStyle(LytParagraph.LOADING_STYLE);
        }
    }
}
