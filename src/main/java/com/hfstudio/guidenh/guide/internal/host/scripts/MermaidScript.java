package com.hfstudio.guidenh.guide.internal.host.scripts;

import java.nio.charset.StandardCharsets;

import net.minecraft.util.ResourceLocation;

import com.hfstudio.guidenh.guide.compiler.tags.MermaidCompiler.MermaidPlaceholder;
import com.hfstudio.guidenh.guide.document.block.LytCodeBlock;
import com.hfstudio.guidenh.guide.document.block.LytMermaidFlowchart;
import com.hfstudio.guidenh.guide.document.block.LytMermaidMindmap;
import com.hfstudio.guidenh.guide.document.block.LytNode;
import com.hfstudio.guidenh.guide.document.block.LytParagraph;
import com.hfstudio.guidenh.guide.internal.host.EventType;
import com.hfstudio.guidenh.guide.internal.host.LytEvent;
import com.hfstudio.guidenh.guide.internal.host.LytScript;
import com.hfstudio.guidenh.guide.internal.host.ScriptContext;
import com.hfstudio.guidenh.guide.internal.host.ScriptType;
import com.hfstudio.guidenh.guide.internal.mermaid.MermaidDiagramType;
import com.hfstudio.guidenh.guide.internal.mermaid.MermaidSourceExtractor;
import com.hfstudio.guidenh.guide.internal.mermaid.flowchart.FlowchartParser;
import com.hfstudio.guidenh.guide.internal.mermaid.mindmap.MindmapParser;
import com.hfstudio.guidenh.guide.scene.support.GuideDebugLog;

public class MermaidScript implements LytScript {

    @Override
    public ScriptType type() {
        return ScriptType.JAVA;
    }

    @Override
    public String styleClass() {
        return "Mermaid";
    }

    @Override
    public boolean isAsync() {
        return true;
    }

    @Override
    public void onEvent(Object node, LytEvent event, ScriptContext ctx) {
        if (event.type() != EventType.MOUNT) return;
        if (!(node instanceof MermaidPlaceholder ph)) return;

        String sourceText = ph.sourceText;
        if (sourceText == null && ph.src != null) {
            ResourceLocation srcId;
            try {
                srcId = new ResourceLocation(ph.src);
            } catch (Exception e) {
                replaceWithError(ctx, "Invalid source path: " + ph.src);
                return;
            }
            byte[] data = ctx.loadAsset(srcId);
            if (data != null) {
                sourceText = new String(data, StandardCharsets.UTF_8);
            }
        }

        if (sourceText != null) {
            sourceText = MermaidSourceExtractor.normalize(sourceText);
        }

        if (sourceText == null || sourceText.trim()
            .isEmpty()) {
            replaceWithError(ctx, "Source not found or empty");
            return;
        }

        MermaidDiagramType diagramType = ph.diagramType;
        if (diagramType == MermaidDiagramType.UNKNOWN) {
            diagramType = MermaidDiagramType.detect(sourceText);
        }

        switch (diagramType) {
            case MINDMAP -> renderMindmap(ctx, ph, sourceText);
            case FLOWCHART -> renderFlowchart(ctx, sourceText, ph);
            case UNKNOWN -> renderUnknown(ctx, sourceText, ph);
        }
    }

    private void renderMindmap(ScriptContext ctx, MermaidPlaceholder ph, String sourceText) {
        try {
            var document = MindmapParser.parse(sourceText);
            LytMermaidMindmap block = new LytMermaidMindmap(
                document,
                sourceText,
                ph.nodeContentBlocks != null ? ph.nodeContentBlocks : java.util.Collections.emptyMap());
            if (ph.width > 0 || ph.height > 0) {
                block.setPreferredSize(ph.width, ph.height);
            }
            if (ph.nodeContentBlocks != null) {
                GuideDebugLog
                    .debug("[MermaidDebug] Dispatching into {} NodeContent blocks", ph.nodeContentBlocks.size());
                for (var entry : ph.nodeContentBlocks.entrySet()) {
                    var contentBlock = entry.getValue();
                    GuideDebugLog.debug(
                        "[MermaidDebug] NodeContent '{}' block type={} children={}",
                        entry.getKey(),
                        contentBlock.getClass()
                            .getSimpleName(),
                        contentBlock instanceof LytNode n ? n.getChildren()
                            .size() : -1);
                    if (contentBlock instanceof LytNode root) {
                        ctx.dispatchSubtree(root);
                    }
                }
            }
            ctx.replace(block);
        } catch (IllegalArgumentException e) {
            GuideDebugLog.error("[GuideNH] [MermaidScript] Failed to parse Mermaid source: {}", sourceText, e);
            replaceWithError(ctx, "Failed to parse: " + e.getMessage());
        }
    }

    private void renderFlowchart(ScriptContext ctx, String sourceText, MermaidPlaceholder ph) {
        try {
            var document = FlowchartParser.parse(sourceText);
            LytMermaidFlowchart block = new LytMermaidFlowchart(
                document,
                sourceText,
                ph.nodeContentBlocks != null ? ph.nodeContentBlocks : java.util.Collections.emptyMap());
            if (ph.width > 0 || ph.height > 0) {
                block.setPreferredSize(ph.width, ph.height);
            }
            if (ph.nodeContentBlocks != null) {
                for (var entry : ph.nodeContentBlocks.entrySet()) {
                    var contentBlock = entry.getValue();
                    if (contentBlock instanceof LytNode root) {
                        ctx.dispatchSubtree(root);
                    }
                }
            }
            ctx.replace(block);
        } catch (IllegalArgumentException e) {
            GuideDebugLog.error("[GuideNH] [MermaidScript] Failed to parse flowchart source: {}", sourceText, e);
            replaceWithError(ctx, "Failed to parse flowchart: " + e.getMessage());
        }
    }

    private void renderUnknown(ScriptContext ctx, String sourceText, MermaidPlaceholder ph) {
        LytCodeBlock codeBlock = new LytCodeBlock();
        codeBlock.setCodeContent("mermaid", sourceText);
        codeBlock.setLanguageDisplayName("Mermaid (stub)");
        if (ph.width > 0 || ph.height > 0) {
            codeBlock.setPreferredBodyWidth(ph.width);
            if (ph.height > 0) {
                codeBlock.setForcedBodyHeight(ph.height);
            }
        }
        ctx.replace(codeBlock);
    }

    private void replaceWithError(ScriptContext ctx, String message) {
        ctx.replace(LytParagraph.error("[Mermaid] " + message));
    }
}
