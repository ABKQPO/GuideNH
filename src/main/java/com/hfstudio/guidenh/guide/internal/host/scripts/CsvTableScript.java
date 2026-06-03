package com.hfstudio.guidenh.guide.internal.host.scripts;

import java.nio.charset.StandardCharsets;
import java.util.List;

import net.minecraft.util.ResourceLocation;

import com.hfstudio.guidenh.guide.compiler.tags.CsvTableCompiler;
import com.hfstudio.guidenh.guide.compiler.tags.CsvTableCompiler.CsvTablePlaceholder;
import com.hfstudio.guidenh.guide.document.block.LytBlock;
import com.hfstudio.guidenh.guide.document.block.LytParagraph;
import com.hfstudio.guidenh.guide.internal.csv.CsvTableParser;
import com.hfstudio.guidenh.guide.internal.host.EventType;
import com.hfstudio.guidenh.guide.internal.host.LytEvent;
import com.hfstudio.guidenh.guide.internal.host.LytScript;
import com.hfstudio.guidenh.guide.internal.host.ScriptContext;
import com.hfstudio.guidenh.guide.internal.host.ScriptType;

public class CsvTableScript implements LytScript {

    @Override
    public ScriptType type() {
        return ScriptType.JAVA;
    }

    @Override
    public String styleClass() {
        return "CsvTable";
    }

    @Override
    public boolean isAsync() {
        return true;
    }

    @Override
    public void onEvent(Object node, LytEvent event, ScriptContext ctx) {
        if (event.type() != EventType.MOUNT) return;
        if (!(node instanceof CsvTablePlaceholder ph)) return;

        ResourceLocation csvId;
        try {
            csvId = new ResourceLocation(ph.src);
        } catch (Exception e) {
            ctx.replace(LytParagraph.error("[CsvTable] Invalid CSV path: " + ph.src));
            return;
        }

        byte[] data = ctx.loadAsset(csvId);
        if (data == null) {
            ctx.replace(LytParagraph.error("[CsvTable] CSV not found: " + ph.src));
            return;
        }

        try {
            List<List<String>> rows = CsvTableParser.parse(new String(data, StandardCharsets.UTF_8));
            LytBlock table = CsvTableCompiler.buildTable(rows, ph.header, ph.widths);
            if (table != null) {
                ctx.replace(table);
            } else {
                ctx.replace(LytParagraph.error("[CsvTable] Failed to parse CSV: " + ph.src));
            }
        } catch (Exception e) {
            ctx.replace(LytParagraph.error("[CsvTable] Failed to parse CSV: " + ph.src));
        }
    }
}
