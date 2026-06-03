package com.hfstudio.guidenh.guide.internal.host.scripts;

import java.nio.charset.StandardCharsets;

import net.minecraft.util.ResourceLocation;

import com.hfstudio.guidenh.guide.compiler.tags.MermaidCompiler.MermaidPlaceholder;
import com.hfstudio.guidenh.guide.document.block.LytMermaidMindmap;
import com.hfstudio.guidenh.guide.document.block.LytNode;
import com.hfstudio.guidenh.guide.document.block.LytParagraph;
import com.hfstudio.guidenh.guide.internal.host.EventType;
import com.hfstudio.guidenh.guide.internal.host.LytEvent;
import com.hfstudio.guidenh.guide.internal.host.LytScript;
import com.hfstudio.guidenh.guide.internal.host.ScriptContext;
import com.hfstudio.guidenh.guide.internal.host.ScriptType;
import com.hfstudio.guidenh.guide.internal.mermaid.MermaidMindmapParser;

import cpw.mods.fml.common.FMLLog;

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
            sourceText = MermaidMindmapParser.normalize(sourceText);
        }

        if (sourceText == null || sourceText.trim()
            .isEmpty()) {
            replaceWithError(ctx, "Source not found or empty");
            return;
        }

        try {
            var document = MermaidMindmapParser.parse(sourceText);
            LytMermaidMindmap block = new LytMermaidMindmap(
                document,
                sourceText,
                ph.nodeContentBlocks != null ? ph.nodeContentBlocks : java.util.Collections.emptyMap());
            if (ph.width > 0 || ph.height > 0) {
                block.setPreferredSize(ph.width, ph.height);
            }
            // Dispatch MOUNT events into NodeContent subtrees (Recipe/BlockImage placeholders)
            if (ph.nodeContentBlocks != null) {
                FMLLog.getLogger()
                    .info("[MermaidDebug] Dispatching into {} NodeContent blocks", ph.nodeContentBlocks.size());
                for (var entry : ph.nodeContentBlocks.entrySet()) {
                    var contentBlock = entry.getValue();
                    FMLLog.getLogger()
                        .info(
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
            FMLLog.getLogger()
                .warn("[GuideNH] [MermaidScript] Failed to parse Mermaid source: {}", sourceText, e);
            replaceWithError(ctx, "Failed to parse: " + e.getMessage());
        }
    }

    private void replaceWithError(ScriptContext ctx, String message) {
        ctx.replace(LytParagraph.error("[Mermaid] " + message));
    }
}
