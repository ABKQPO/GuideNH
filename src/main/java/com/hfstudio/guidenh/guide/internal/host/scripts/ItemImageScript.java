package com.hfstudio.guidenh.guide.internal.host.scripts;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import com.hfstudio.guidenh.guide.compiler.tags.ItemImageCompiler.ItemImagePlaceholder;
import com.hfstudio.guidenh.guide.document.block.LytItemImage;
import com.hfstudio.guidenh.guide.document.block.LytParagraph;
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
        if (stack == null) {
            replaceFlowError(ctx, isWrapped, "[ItemImage] Item not found: " + ph.itemId);
            return;
        }

        LytItemImage image = new LytItemImage(stack);
        image.setInline(true);
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
    private void replaceFlowError(ScriptContext ctx, boolean isWrapped, String message) {
        LytParagraph error = LytParagraph.error(message);
        if (isWrapped) {
            LytFlowInlineBlock wrapper = new LytFlowInlineBlock();
            wrapper.setBlock(error);
            ctx.replace(wrapper);
        } else {
            ctx.replace(error);
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
        return item != null ? new ItemStack(item, 1, ref.concreteMeta()) : null;
    }
}
