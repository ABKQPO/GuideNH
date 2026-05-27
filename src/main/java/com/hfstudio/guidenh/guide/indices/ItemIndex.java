package com.hfstudio.guidenh.guide.indices;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.oredict.OreDictionary;

import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.Nullable;

import com.gtnewhorizon.gtnhlib.util.data.ItemId;
import com.hfstudio.guidenh.guide.PageAnchor;
import com.hfstudio.guidenh.guide.compiler.IdUtils;
import com.hfstudio.guidenh.guide.compiler.ParsedGuidePage;

import cpw.mods.fml.common.FMLLog;

/**
 * An index of Minecraft items to the main guidebook page describing it.
 * <p/>
 * Supports per-meta entries: an {@code item_ids} frontmatter list may contain either
 * {@code modid:name} (matches any metadata) or {@code modid:name:meta} (exact match only).
 */
public class ItemIndex extends UniqueIndex<ItemId, PageAnchor> {

    public ItemIndex() {
        super(
            "Item Index",
            ItemIndex::getItemAnchors,
            (writer, value) -> writer.value(formatKey(value)),
            (writer, value) -> writer.value(value.toString()));
    }

    /**
     * Looks up a page for the given stack: exact-meta first, then wildcard-meta fallback.
     */
    @Nullable
    public PageAnchor findByStack(@Nullable ItemStack stack) {
        if (stack == null) return null;
        Item item = stack.getItem();
        if (item == null) return null;
        return findByItem(item, stack.getItemDamage());
    }

    /**
     * Looks up a page by (item, meta): exact-meta first, then wildcard-meta fallback.
     */
    @Nullable
    public PageAnchor findByItem(Item item, int meta) {
        if (item == null) return null;
        PageAnchor exact = get(ItemId.createNoCopy(item, meta, null));
        if (exact != null) return exact;
        if (meta != OreDictionary.WILDCARD_VALUE) {
            return get(ItemId.createNoCopy(item, OreDictionary.WILDCARD_VALUE, null));
        }
        return null;
    }

    public static String formatKey(ItemId key) {
        Object name = Item.itemRegistry.getNameForObject(key.getItem());
        String base = name != null ? name.toString() : "unknown";
        return key.getItemMeta() == OreDictionary.WILDCARD_VALUE ? base : base + ":" + key.getItemMeta();
    }

    public static List<Pair<ItemId, PageAnchor>> getItemAnchors(ParsedGuidePage page) {
        var itemIdsNode = page.getFrontmatter()
            .additionalProperties()
            .get("item_ids");
        if (itemIdsNode == null) {
            return Collections.emptyList();
        }

        List<?> itemIdList = normalizeItemIdEntries(page, itemIdsNode);
        if (itemIdList == null) {
            FMLLog.getLogger()
                .warn("[GuideNH] [ItemIndex] Page {} contains malformed item_ids frontmatter", page.getId());
            return Collections.emptyList();
        }

        var itemAnchors = new ArrayList<Pair<ItemId, PageAnchor>>(itemIdList.size());

        for (var listEntry : itemIdList) {
            if (listEntry instanceof String itemIdStr) {
                // Allow an optional "#anchor" suffix to link to a specific heading.
                String anchor = null;
                int hashIdx = itemIdStr.indexOf('#');
                if (hashIdx != -1) {
                    anchor = itemIdStr.substring(hashIdx + 1);
                    itemIdStr = itemIdStr.substring(0, hashIdx);
                }

                ItemId itemId;
                try {
                    itemId = IdUtils.resolveItemId(
                        itemIdStr,
                        page.getId()
                            .getResourceDomain());
                } catch (IllegalArgumentException e) {
                    FMLLog.getLogger()
                        .warn(
                            "[GuideNH] [ItemIndex] Page {} contains a malformed item_ids frontmatter entry: {}",
                            page.getId(),
                            listEntry);
                    continue;
                }

                if (itemId == null) {
                    FMLLog.getLogger()
                        .warn(
                            "[GuideNH] [ItemIndex] Page {} references an unknown item {} in its item_ids frontmatter",
                            page.getId(),
                            listEntry);
                    continue;
                }

                itemAnchors.add(Pair.of(itemId, new PageAnchor(page.getId(), anchor)));
            } else {
                FMLLog.getLogger()
                    .warn(
                        "[GuideNH] [ItemIndex] Page {} contains a malformed item_ids frontmatter entry: {}",
                        page.getId(),
                        listEntry);
            }
        }

        return itemAnchors;
    }

    @Nullable
    private static List<?> normalizeItemIdEntries(ParsedGuidePage page, Object itemIdsNode) {
        if (itemIdsNode instanceof List<?>itemIdList) {
            return itemIdList;
        }
        if (itemIdsNode instanceof String itemIdEntry) {
            String trimmed = itemIdEntry.trim();
            if (trimmed.isEmpty()) {
                FMLLog.getLogger()
                    .warn("[GuideNH] [ItemIndex] Page {} contains an empty item_ids frontmatter entry", page.getId());
                return Collections.emptyList();
            }
            return Collections.singletonList(trimmed);
        }
        return null;
    }
}
