package com.hfstudio.guidenh.guide.internal.host.scripts;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import com.hfstudio.guidenh.guide.compiler.tags.StructureViewCompiler.StructureEntry;
import com.hfstudio.guidenh.guide.compiler.tags.StructureViewCompiler.StructurePlaceholder;
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
            for (StructureEntry entry : ph.entries) {
                ItemStack stack = resolveEntry(entry.idSpec);
                if (stack != null) {
                    view.addBlock(entry.x, entry.y, entry.z, stack);
                }
            }
            ctx.replace(view);
        }
    }

    @SuppressWarnings("deprecation")
    private static ItemStack resolveEntry(String idSpec) {
        int colonIdx = idSpec.lastIndexOf(':');
        if (colonIdx < 0) return null;

        String rawKey = idSpec.substring(0, colonIdx);
        int meta = 0;
        try { meta = Integer.parseInt(idSpec.substring(colonIdx + 1)); } catch (NumberFormatException ignored) {}

        Item item = (Item) Item.itemRegistry.getObject(rawKey);
        return item != null ? new ItemStack(item, 1, meta) : null;
    }
}
