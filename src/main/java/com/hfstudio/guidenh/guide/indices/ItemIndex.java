package com.hfstudio.guidenh.guide.indices;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.oredict.OreDictionary;

import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gtnewhorizon.gtnhlib.util.data.ItemId;
import com.hfstudio.guidenh.guide.PageAnchor;
import com.hfstudio.guidenh.guide.compiler.IdUtils;
import com.hfstudio.guidenh.guide.compiler.ParsedGuidePage;

/**
 * An index of Minecraft items to the main guidebook page describing it.
 * <p/>
 * Supports per-meta entries: an {@code item_ids} frontmatter list may contain either
 * {@code modid:name} (matches any metadata) or {@code modid:name:meta} (exact match only).
 */
public class ItemIndex extends UniqueIndex<ItemId, PageAnchor> {

    public static final Logger LOG = LoggerFactory.getLogger(ItemIndex.class);

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

        if (!(itemIdsNode instanceof List<?>itemIdList)) {
            LOG.warn("Page {} contains malformed item_ids frontmatter", page.getId());
            return Collections.emptyList();
        }

        var itemAnchors = new ArrayList<Pair<ItemId, PageAnchor>>(itemIdList.size());

        for (var listEntry : itemIdList) {
            if (listEntry instanceof String itemIdStr) {
                ItemId itemId;
                try {
                    itemId = IdUtils.resolveItemId(
                        itemIdStr,
                        page.getId()
                            .getResourceDomain());
                } catch (IllegalArgumentException e) {
                    LOG.warn("Page {} contains a malformed item_ids frontmatter entry: {}", page.getId(), listEntry);
                    continue;
                }

                if (itemId == null) {
                    LOG.warn(
                        "Page {} references an unknown item {} in its item_ids frontmatter",
                        page.getId(),
                        itemIdStr);
                    continue;
                }

                itemAnchors.add(Pair.of(itemId, new PageAnchor(page.getId(), null)));
            } else {
                LOG.warn("Page {} contains a malformed item_ids frontmatter entry: {}", page.getId(), listEntry);
            }
        }

        return itemAnchors;
    }
}
