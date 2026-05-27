package com.hfstudio.guidenh.guide.internal.host.scripts;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import com.hfstudio.guidenh.guide.compiler.tags.ItemImageCompiler.ItemImagePlaceholder;
import com.hfstudio.guidenh.guide.document.block.LytItemImage;
import com.hfstudio.guidenh.guide.document.flow.LytFlowInlineBlock;
import com.hfstudio.guidenh.guide.internal.host.EventType;
import com.hfstudio.guidenh.guide.internal.host.LytEvent;
import com.hfstudio.guidenh.guide.internal.host.LytScript;
import com.hfstudio.guidenh.guide.internal.host.ScriptContext;
import com.hfstudio.guidenh.guide.internal.host.ScriptType;

public class ItemImageScript implements LytScript {

    @Override
    public ScriptType type() { return ScriptType.JAVA; }

    @Override
    public String styleClass() { return "ItemImage"; }

    @Override
    @SuppressWarnings("deprecation")
    public void onEvent(Object node, LytEvent event, ScriptContext ctx) {
        if (event.type() != EventType.MOUNT) return;

        ItemImagePlaceholder ph;
        boolean isWrapped = node instanceof LytFlowInlineBlock w && w.getBlock() instanceof ItemImagePlaceholder p;
        if (isWrapped) {
            ph = (ItemImagePlaceholder) ((LytFlowInlineBlock) node).getBlock();
        } else if (node instanceof ItemImagePlaceholder p) {
            ph = p;
        } else {
            return;
        }

        ItemStack stack = resolveItemId(ph.itemId);
        if (stack == null) return;

        LytItemImage image = new LytItemImage(stack);
        image.setScale(ph.scale);
        image.setShowTooltip(ph.showTooltip);
        if (ph.showIcon != null) image.setShowIcon(ph.showIcon);
        if (ph.labelPosition != null) image.setLabelPosition(ph.labelPosition);
        if (ph.labelFormat != null) image.setLabelFormat(ph.labelFormat);
        if (ph.yOffset != null) image.setInlineYOffsetOverride(ph.yOffset);
        if (ph.labelYOffset != null) image.setLabelYOffsetOverride(ph.labelYOffset);

        if (isWrapped) {
            LytFlowInlineBlock newWrapper = new LytFlowInlineBlock();
            newWrapper.setBlock(image);
            ctx.replace(newWrapper);
        } else {
            ctx.replace(image);
        }
    }

    @SuppressWarnings("deprecation")
    private static ItemStack resolveItemId(String itemId) {
        if (itemId == null || itemId.isEmpty()) return null;
        int colonIdx = itemId.lastIndexOf(':');
        if (colonIdx < 0) return null;

        String rawKey = itemId.substring(0, colonIdx);
        int meta = 0;
        try { meta = Integer.parseInt(itemId.substring(colonIdx + 1)); } catch (NumberFormatException ignored) {}

        Item item = (Item) Item.itemRegistry.getObject(rawKey);
        return item != null ? new ItemStack(item, 1, meta) : null;
    }
}
