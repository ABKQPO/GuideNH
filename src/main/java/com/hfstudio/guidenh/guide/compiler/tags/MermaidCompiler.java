package com.hfstudio.guidenh.guide.compiler.tags;

import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.minecraft.util.ResourceLocation;

import com.hfstudio.guidenh.guide.compiler.IdUtils;
import com.hfstudio.guidenh.guide.compiler.IndexingContext;
import com.hfstudio.guidenh.guide.compiler.IndexingSink;
import com.hfstudio.guidenh.guide.compiler.PageCompiler;
import com.hfstudio.guidenh.guide.document.block.LytBlock;
import com.hfstudio.guidenh.guide.document.block.LytBlockContainer;
import com.hfstudio.guidenh.guide.document.block.LytMermaidMindmap;
import com.hfstudio.guidenh.guide.document.block.LytParagraph;
import com.hfstudio.guidenh.guide.document.block.LytVBox;
import com.hfstudio.guidenh.guide.internal.mermaid.MermaidMindmapNode;
import com.hfstudio.guidenh.guide.internal.mermaid.MermaidMindmapNodeContentExtractor;
import com.hfstudio.guidenh.guide.internal.mermaid.MermaidMindmapParser;
import com.hfstudio.guidenh.libs.mdast.mdx.model.MdxJsxElementFields;
import com.hfstudio.guidenh.libs.mdast.mdx.model.MdxJsxFlowElement;

import cpw.mods.fml.common.FMLLog;

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
            Map<String, LytBlock> nodeContentBlocks = compileNodeContentBlocks(
                compiler,
                parent,
                el,
                document.getRoot());
            LytMermaidMindmap block = new LytMermaidMindmap(document, source, nodeContentBlocks);
            int width = MdxAttrs.getInt(compiler, parent, el, "width", 0);
            int height = MdxAttrs.getInt(compiler, parent, el, "height", 0);
            if (width > 0 || height > 0) {
                block.setPreferredSize(width, height);
            }
            FMLLog.getLogger()
                .debug(
                    "[GuideNH] [MermaidCompiler] Compiled Mermaid runtime block for page {} with root='{}', children={}, sourceLength={}, width={}, height={}",
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
            FMLLog.getLogger()
                .warn(
                    "[GuideNH] [MermaidCompiler] Failed to compile Mermaid runtime block for page {} from source: {}",
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
        String rawTagBodySource = compiler.getBlockTagChildrenSource(el);
        if (rawTagBodySource != null && !rawTagBodySource.trim()
            .isEmpty()) {
            return MermaidMindmapNodeContentExtractor.stripExplicitNodeContentBlocks(rawTagBodySource);
        }
        return MermaidMindmapNodeContentExtractor.extractDiagramSource(el.children());
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
        return MermaidMindmapNodeContentExtractor.extractDiagramSource(el.children());
    }

    private String loadSource(PageCompiler compiler, String src) {
        try {
            ResourceLocation mermaidId = IdUtils.resolveLink(src, compiler.getPageId());
            byte[] data = compiler.loadAsset(mermaidId);
            if (data == null) {
                FMLLog.getLogger()
                    .warn(
                        "[GuideNH] [MermaidCompiler] Mermaid src '{}' for page {} could not be loaded as asset {}",
                        src,
                        compiler.getPageId(),
                        mermaidId);
                return null;
            }
            String loaded = MermaidMindmapParser.normalize(new String(data, StandardCharsets.UTF_8));
            FMLLog.getLogger()
                .debug(
                    "[GuideNH] [MermaidCompiler] Loaded Mermaid src '{}' for page {} as asset {} ({} chars)",
                    src,
                    compiler.getPageId(),
                    mermaidId,
                    loaded.length());
            return loaded;
        } catch (IllegalArgumentException e) {
            FMLLog.getLogger()
                .warn(
                    "[GuideNH] [MermaidCompiler] Failed to resolve Mermaid src '{}' for page {}",
                    src,
                    compiler.getPageId(),
                    e);
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

    private Map<String, LytBlock> compileNodeContentBlocks(PageCompiler compiler, LytBlockContainer parent,
        MdxJsxElementFields mermaidElement, MermaidMindmapNode root) {
        Map<String, MermaidMindmapNode> nodesById = indexNodesById(root);
        Map<String, MdxJsxFlowElement> explicitBlocksById = new LinkedHashMap<>();
        for (MdxJsxFlowElement child : MermaidMindmapNodeContentExtractor
            .collectNodeContentElements(mermaidElement.children())) {
            String id = MermaidMindmapNodeContentExtractor.readNodeContentId(child);
            if (id == null) {
                parent.appendError(compiler, "Mermaid <NodeContent> requires a non-empty id attribute.", child);
                continue;
            }
            if (!nodesById.containsKey(id)) {
                parent.appendError(compiler, "Mermaid <NodeContent> references unknown node id '" + id + "'.", child);
                continue;
            }
            if (explicitBlocksById.put(id, child) != null) {
                parent.appendError(compiler, "Duplicate Mermaid <NodeContent> id '" + id + "'.", child);
            }
        }

        Map<String, LytBlock> result = new LinkedHashMap<>();
        for (MermaidMindmapNode node : nodesById.values()) {
            LytBlock compiled = compileNodeContentBlock(compiler, explicitBlocksById.get(node.getId()), node);
            if (compiled != null) {
                result.put(node.getId(), compiled);
            }
        }
        return result;
    }

    private Map<String, MermaidMindmapNode> indexNodesById(MermaidMindmapNode root) {
        Map<String, MermaidMindmapNode> nodesById = new LinkedHashMap<>();
        ArrayDeque<MermaidMindmapNode> pending = new ArrayDeque<>();
        pending.add(root);
        while (!pending.isEmpty()) {
            MermaidMindmapNode node = pending.removeFirst();
            nodesById.putIfAbsent(node.getId(), node);
            List<MermaidMindmapNode> children = node.getChildren();
            for (MermaidMindmapNode child : children) {
                pending.addLast(child);
            }
        }
        return nodesById;
    }

    private LytBlock compileNodeContentBlock(PageCompiler compiler, MdxJsxFlowElement explicitContent,
        MermaidMindmapNode node) {
        if (explicitContent != null) {
            LytVBox box = new LytVBox();
            compiler.withBlockTagChildrenSourceContext(
                explicitContent,
                () -> compiler.compileBlockContext(explicitContent.children(), box));
            return box.getChildren()
                .isEmpty() ? null : box;
        }
        if (!shouldCompileRichInlineLabel(node)) {
            return null;
        }
        LytParagraph paragraph = new LytParagraph();
        compiler.withSourceContext(
            node.getLabelSource(),
            () -> compiler.compileInlineMarkdown(node.getLabelSource(), paragraph));
        return paragraph.isEmpty() ? null : paragraph;
    }

    private boolean shouldCompileRichInlineLabel(MermaidMindmapNode node) {
        String labelSource = node.getLabelSource();
        if (labelSource == null || labelSource.trim()
            .isEmpty()) {
            return false;
        }
        return !labelSource.equals(node.getText());
    }
}
