package com.hfstudio.guidenh.guide.internal.host.scripts;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import com.hfstudio.guidenh.guide.compiler.tags.ItemGridCompiler.ItemGridPlaceholder;
import com.hfstudio.guidenh.guide.document.block.LytItemGrid;
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
            for (String itemId : ph.itemIds) {
                ItemStack stack = resolveItemId(itemId.trim());
                if (stack != null) {
                    grid.addItem(stack);
                }
            }
            ctx.replace(grid);
        }
    }

    @SuppressWarnings("deprecation")
    private static ItemStack resolveItemId(String itemId) {
        int colonIdx = itemId.lastIndexOf(':');
        if (colonIdx < 0) return null;

        String rawKey = itemId.substring(0, colonIdx);
        int meta = 0;
        try { meta = Integer.parseInt(itemId.substring(colonIdx + 1)); } catch (NumberFormatException ignored) {}

        Item item = (Item) Item.itemRegistry.getObject(rawKey);
        return item != null ? new ItemStack(item, 1, meta) : null;
    }
}
