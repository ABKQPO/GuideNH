package com.hfstudio.guidenh.guide.internal.host.scripts;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.oredict.OreDictionary;

import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.guide.PageAnchor;
import com.hfstudio.guidenh.guide.compiler.IdUtils;
import com.hfstudio.guidenh.guide.document.block.LytItemImage;
import com.hfstudio.guidenh.guide.document.block.LytParagraph;
import com.hfstudio.guidenh.guide.document.flow.LytFlowContent;
import com.hfstudio.guidenh.guide.document.flow.LytFlowInlineBlock;
import com.hfstudio.guidenh.guide.document.flow.LytFlowLink;
import com.hfstudio.guidenh.guide.document.flow.LytTooltipSpan;
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
            String iconPosition = (String) link.getData("showIcon");
            String currentPage = (String) link.getData("pageId");

            // Neither target specified
            if ((itemId == null || itemId.isEmpty()) && (ore == null || ore.isEmpty())) {
                replaceFlowError(ctx, "[ItemLink] Link has no target");
                return;
            }

            ItemStack stack = resolveItemStack(itemId);
            if (stack == null && ore != null && !ore.isEmpty()) {
                java.util.List<ItemStack> ores = OreDictionary.getOres(ore);
                if (!ores.isEmpty()) {
                    stack = ores.get(0);
                }
            }
            if (stack == null) {
                String detail = (itemId != null && !itemId.isEmpty()) ? itemId : ore;
                replaceFlowError(ctx, "[ItemLink] Link target not found: " + detail);
                return;
            }

            PageAnchor anchor = findLinkTarget(stack, linksTo, ctx);

            // Handle fragment-only link (#heading)
            if (anchor == null && linksTo != null && linksTo.startsWith("#") && linksTo.length() > 1) {
                if (currentPage != null) {
                    try {
                        anchor = new PageAnchor(new ResourceLocation(currentPage), linksTo.substring(1));
                    } catch (Exception ignored) {}
                }
            }

            // Same-page detection
            if (anchor != null && currentPage != null && anchor.pageId().toString().equals(currentPage)) {
                LytTooltipSpan span = new LytTooltipSpan();
                span.setStyleClass("ItemLink");
                java.util.List<LytFlowContent> linkChildren = new java.util.ArrayList<>(link.getChildren());
                link.getChildren().clear();
                for (LytFlowContent child : linkChildren) {
                    child.setParent(null);
                    span.append(child);
                }
                if (Boolean.TRUE.equals(showTooltip)) {
                    span.setTooltip(new ItemTooltip(stack));
                }
                span.modifyStyle(style -> style.italic(true));
                ctx.replace(span);
                return;
            }

            if (anchor != null) {
                link.setPageLink(anchor);
            }
            if (Boolean.TRUE.equals(showTooltip)) {
                link.setTooltip(new ItemTooltip(stack));
            }

            // Show icon support
            if (iconPosition != null && !iconPosition.isEmpty() && itemId != null) {
                ItemStack iconStack = resolveItemStack(itemId);
                if (iconStack != null) {
                    var img = new LytItemImage(iconStack);
                    img.setScale(1f);
                    img.setInline(true);
                    img.setShowTooltip(Boolean.TRUE.equals(showTooltip));
                    var wrapper = new LytFlowInlineBlock();
                    wrapper.setBlock(img);
                    if ("left".equals(iconPosition)) {
                        link.getChildren().add(0, wrapper);
                        wrapper.setParent(link);
                    } else {
                        link.append(wrapper);
                    }
                }
            }
        }
    }

    @SuppressWarnings("deprecation")
    private static ItemStack resolveItemStack(String itemId) {
        if (itemId == null || itemId.isEmpty()) return null;
        IdUtils.ParsedItemRef ref = IdUtils.parseItemRef(itemId, "minecraft");
        if (ref == null) return null;
        Item item = (Item) Item.itemRegistry.getObject(ref.rawKey());
        return item != null ? new ItemStack(item, 1, ref.concreteMeta()) : null;
    }

    @Nullable
    private static PageAnchor findLinkTarget(ItemStack stack, @Nullable String linksTo, ScriptContext ctx) {
        if (linksTo != null && !linksTo.isEmpty()) {
            String pagePart = linksTo;
            String anchorPart = null;
            int hashIdx = linksTo.indexOf('#');
            if (hashIdx >= 0) {
                pagePart = linksTo.substring(0, hashIdx);
                anchorPart = linksTo.substring(hashIdx + 1);
            }
            if (!pagePart.isEmpty()) {
                try {
                    ResourceLocation pageId = new ResourceLocation(pagePart);
                    return anchorPart != null ? new PageAnchor(pageId, anchorPart) : PageAnchor.page(pageId);
                } catch (Exception ignored) {}
            }
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

    private void replaceFlowError(ScriptContext ctx, String message) {
        LytFlowInlineBlock wrapper = new LytFlowInlineBlock();
        wrapper.setBlock(LytParagraph.error(message));
        ctx.replace(wrapper);
    }
}
