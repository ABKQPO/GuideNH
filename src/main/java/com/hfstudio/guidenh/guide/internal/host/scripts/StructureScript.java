package com.hfstudio.guidenh.guide.internal.host.scripts;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import com.hfstudio.guidenh.guide.compiler.tags.StructureViewCompiler.StructureEntry;
import com.hfstudio.guidenh.guide.compiler.tags.StructureViewCompiler.StructurePlaceholder;
import com.hfstudio.guidenh.guide.document.block.LytParagraph;
import com.hfstudio.guidenh.guide.document.block.LytStructureView;
import com.hfstudio.guidenh.guide.internal.host.EventType;
import com.hfstudio.guidenh.guide.internal.host.LytEvent;
import com.hfstudio.guidenh.guide.internal.host.LytScript;
import com.hfstudio.guidenh.guide.internal.host.ScriptContext;
import com.hfstudio.guidenh.guide.internal.host.ScriptType;

public class StructureScript implements LytScript {

    @Override
    public ScriptType type() { return ScriptType.JAVA; }

    @Override
    public String styleClass() { return "Structure"; }

    @Override
    @SuppressWarnings("deprecation")
    public void onEvent(Object node, LytEvent event, ScriptContext ctx) {
        if (event.type() == EventType.MOUNT && node instanceof StructurePlaceholder ph) {
            LytStructureView view = new LytStructureView();
            view.setViewSize(ph.width, ph.height);
            int resolved = 0;
            for (StructureEntry entry : ph.entries) {
                ItemStack stack = resolveEntry(entry.idSpec);
                if (stack != null) {
                    view.addBlock(entry.x, entry.y, entry.z, stack);
                    resolved++;
                }
            }
            if (resolved == 0) {
                ctx.replace(LytParagraph.error("[Structure] Structure has no valid blocks"));
            } else {
                ctx.replace(view);
            }
        }
    }

    @SuppressWarnings("deprecation")
    private static ItemStack resolveEntry(String idSpec) {
        if (idSpec == null || idSpec.isEmpty()) return null;
        com.hfstudio.guidenh.guide.compiler.IdUtils.ParsedItemRef ref =
            com.hfstudio.guidenh.guide.compiler.IdUtils.parseItemRef(idSpec, "minecraft");
        if (ref == null) return null;
        Item item = (Item) Item.itemRegistry.getObject(ref.rawKey());
        return item != null ? new ItemStack(item, 1, ref.concreteMeta()) : null;
    }
}
