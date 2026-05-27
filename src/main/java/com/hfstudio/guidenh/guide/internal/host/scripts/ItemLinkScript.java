package com.hfstudio.guidenh.guide.internal.host.scripts;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.guide.PageAnchor;
import com.hfstudio.guidenh.guide.document.flow.LytFlowLink;
import com.hfstudio.guidenh.guide.document.interaction.ItemTooltip;
import com.hfstudio.guidenh.guide.internal.host.EventType;
import com.hfstudio.guidenh.guide.internal.host.LytEvent;
import com.hfstudio.guidenh.guide.internal.host.LytScript;
import com.hfstudio.guidenh.guide.internal.host.ScriptContext;
import com.hfstudio.guidenh.guide.internal.host.ScriptType;

public class ItemLinkScript implements LytScript {

    @Override
    public ScriptType type() { return ScriptType.JAVA; }

    @Override
    public String styleClass() { return "ItemLink"; }

    @Override
    @SuppressWarnings("deprecation")
    public void onEvent(Object node, LytEvent event, ScriptContext ctx) {
        if (event.type() == EventType.MOUNT && node instanceof LytFlowLink link) {
            String itemId = (String) link.getData("itemId");
            Boolean showTooltip = (Boolean) link.getData("showTooltip");
            String linksTo = (String) link.getData("linksTo");

            ItemStack stack = resolveItemStack(itemId);
            if (stack == null) return;

            PageAnchor anchor = findLinkTarget(stack, linksTo);
            if (anchor != null) {
                link.setPageLink(anchor);
            }
            if (Boolean.TRUE.equals(showTooltip)) {
                link.setTooltip(new ItemTooltip(stack));
            }
        }
    }

    @SuppressWarnings("deprecation")
    private static ItemStack resolveItemStack(String itemId) {
        if (itemId == null || itemId.isEmpty()) return null;
        String rawKey;
        int meta = 0;
        int colonIdx = itemId.lastIndexOf(':');
        if (colonIdx >= 0) {
            String maybeMeta = itemId.substring(colonIdx + 1);
            try {
                meta = Integer.parseInt(maybeMeta);
                rawKey = itemId.substring(0, colonIdx);
            } catch (NumberFormatException e) {
                rawKey = itemId;
                meta = 0;
            }
        } else {
            return null;
        }
        Item item = (Item) Item.itemRegistry.getObject(rawKey);
        return item != null ? new ItemStack(item, 1, meta) : null;
    }

    @Nullable
    private static PageAnchor findLinkTarget(ItemStack stack, @Nullable String linksTo) {
        // FIXME: Index lookup requires per-guide PageCollection reference.
        // ItemLinkScript needs access to the guide's index registry via ScriptContext.
        // For now, manual linksTo will be parsed later; auto-index lookup deferred.
        return null;
    }
}
