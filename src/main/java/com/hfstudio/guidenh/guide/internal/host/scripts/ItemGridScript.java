package com.hfstudio.guidenh.guide.internal.host.scripts;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import com.hfstudio.guidenh.guide.compiler.tags.ItemGridCompiler.ItemGridPlaceholder;
import com.hfstudio.guidenh.guide.document.block.LytItemGrid;
import com.hfstudio.guidenh.guide.document.block.LytParagraph;
import com.hfstudio.guidenh.guide.internal.host.EventType;
import com.hfstudio.guidenh.guide.internal.host.LytEvent;
import com.hfstudio.guidenh.guide.internal.host.LytScript;
import com.hfstudio.guidenh.guide.internal.host.ScriptContext;
import com.hfstudio.guidenh.guide.internal.host.ScriptType;

public class ItemGridScript implements LytScript {

    @Override
    public ScriptType type() { return ScriptType.JAVA; }

    @Override
    public String styleClass() { return "ItemGrid"; }

    @Override
    @SuppressWarnings("deprecation")
    public void onEvent(Object node, LytEvent event, ScriptContext ctx) {
        if (event.type() == EventType.MOUNT && node instanceof ItemGridPlaceholder ph) {
            LytItemGrid grid = new LytItemGrid();
            int resolved = 0;
            for (String itemId : ph.itemIds) {
                ItemStack stack = resolveItemId(itemId.trim());
                if (stack != null) {
                    grid.addItem(stack);
                    resolved++;
                }
            }
            if (resolved == 0) {
                ctx.replace(LytParagraph.error("[ItemGrid] No items to display"));
            } else {
                ctx.replace(grid);
            }
        }
    }

    @SuppressWarnings("deprecation")
    private static ItemStack resolveItemId(String itemId) {
        if (itemId == null || itemId.isEmpty()) return null;
        String ns = "minecraft";
        int idx = itemId.indexOf(':');
        if (idx >= 0) {
            ns = itemId.substring(0, idx);
        }
        com.hfstudio.guidenh.guide.compiler.IdUtils.ParsedItemRef ref =
            com.hfstudio.guidenh.guide.compiler.IdUtils.parseItemRef(itemId, ns);
        if (ref == null) return null;
        Item item = (Item) Item.itemRegistry.getObject(ref.rawKey());
        if (item == null) return null;
        ItemStack stack = new ItemStack(item, 1, ref.concreteMeta());
        if (ref.nbt() != null) {
            stack.stackTagCompound = (net.minecraft.nbt.NBTTagCompound) ref.nbt().copy();
        }
        return stack;
    }
}
