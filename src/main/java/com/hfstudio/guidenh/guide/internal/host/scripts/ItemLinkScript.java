package com.hfstudio.guidenh.guide.internal.host.scripts;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.oredict.OreDictionary;

import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.guide.PageAnchor;
import com.hfstudio.guidenh.guide.document.flow.LytFlowLink;
import com.hfstudio.guidenh.guide.document.interaction.ItemTooltip;
import com.hfstudio.guidenh.guide.internal.host.EventType;
import com.hfstudio.guidenh.guide.internal.host.LytEvent;
import com.hfstudio.guidenh.guide.internal.host.LytScript;
import com.hfstudio.guidenh.guide.internal.host.ScriptContext;
import com.hfstudio.guidenh.guide.internal.host.ScriptType;
import com.hfstudio.guidenh.guide.indices.ItemIndex;
import com.hfstudio.guidenh.guide.indices.OreIndex;

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
            String ore = (String) link.getData("ore");
            Boolean showTooltip = (Boolean) link.getData("showTooltip");
            String linksTo = (String) link.getData("linksTo");

            ItemStack stack = resolveItemStack(itemId);
            if (stack == null && ore != null && !ore.isEmpty()) {
                java.util.List<ItemStack> ores = OreDictionary.getOres(ore);
                if (!ores.isEmpty()) {
                    stack = ores.get(0);
                }
            }
            if (stack == null) return;

            PageAnchor anchor = findLinkTarget(stack, linksTo, ctx);
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
    private static PageAnchor findLinkTarget(ItemStack stack, @Nullable String linksTo, ScriptContext ctx) {
        if (linksTo != null && !linksTo.isEmpty()) {
            try {
                ResourceLocation pageId = new ResourceLocation(linksTo);
                return PageAnchor.page(pageId);
            } catch (Exception ignored) {}
        }
        ItemIndex itemIdx = ctx.getIndex(ItemIndex.class);
        if (itemIdx != null) {
            PageAnchor anchor = itemIdx.findByStack(stack);
            if (anchor != null) return anchor;
        }
        OreIndex oreIdx = ctx.getIndex(OreIndex.class);
        if (oreIdx != null) {
            return oreIdx.findByStack(stack);
        }
        return null;
    }
}
