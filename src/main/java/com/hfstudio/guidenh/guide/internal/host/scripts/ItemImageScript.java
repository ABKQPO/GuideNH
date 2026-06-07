package com.hfstudio.guidenh.guide.internal.host.scripts;

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
import com.hfstudio.guidenh.guide.internal.item.GuideDisplayItemStacks;

public class ItemImageScript implements LytScript {

    @Override
    public ScriptType type() {
        return ScriptType.JAVA;
    }

    @Override
    public String styleClass() {
        return "ItemImage";
    }

    @Override
    public void onEvent(Object node, LytEvent event, ScriptContext ctx) {
        if (event.type() != EventType.MOUNT) return;

        ItemImagePlaceholder ph = LytFlowInlineBlock.unwrapPlaceholder(node, ItemImagePlaceholder.class);
        if (ph == null) return;

        ItemStack stack = resolveItemId(ph.itemId);
        if (stack == null) {
            // Fallback to ore dictionary if direct item lookup fails
            if (ph.ore != null) {
                stack = GuideDisplayItemStacks.resolveOreStack(ph.ore);
            }
            if (stack == null) {
                replaceFlowError(ctx, "[ItemImage] Item not found: " + ph.itemId);
                return;
            }
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

        ctx.replace(image);
    }

    private void replaceFlowError(ScriptContext ctx, String message) {
        LytParagraph error = LytParagraph.error(message);
        ctx.replace(error);
    }

    private static ItemStack resolveItemId(String itemId) {
        return GuideDisplayItemStacks.resolveItemStack(itemId, "minecraft");
    }
}
