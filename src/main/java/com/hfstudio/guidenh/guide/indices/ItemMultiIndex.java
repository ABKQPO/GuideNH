package com.hfstudio.guidenh.guide.indices;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.oredict.OreDictionary;

import com.gtnewhorizon.gtnhlib.util.data.ItemId;
import com.hfstudio.guidenh.guide.PageAnchor;

/**
 * An index that maps each Minecraft item to ALL guide pages that reference it via
 * {@code item_ids} frontmatter. Unlike {@link ItemIndex}, duplicate entries are kept rather
 * than silently discarded, so a single item can be bound to multiple pages.
 */
public class ItemMultiIndex extends MultiValuedIndex<ItemId, PageAnchor> {

    public ItemMultiIndex() {
        super(
            "Item Multi-Index",
            ItemIndex::getItemAnchors,
            (writer, value) -> writer.value(ItemIndex.formatKey(value)),
            (writer, value) -> writer.value(value.toString()));
    }

    /**
     * Returns all pages matching the given stack. Exact-meta entries appear before
     * wildcard-meta entries. Returns an empty list if the stack is null or has no item.
     */
    public List<PageAnchor> findAllByStack(ItemStack stack) {
        if (stack == null) return List.of();
        Item item = stack.getItem();
        if (item == null) return List.of();
        return findAllByItem(item, stack.getItemDamage());
    }

    /**
     * Returns all pages matching the given item and meta. Exact-meta entries appear first,
     * followed by wildcard-meta entries.
     */
    public List<PageAnchor> findAllByItem(Item item, int meta) {
        if (item == null) return List.of();
        if (meta == OreDictionary.WILDCARD_VALUE) {
            return get(ItemId.createNoCopy(item, OreDictionary.WILDCARD_VALUE, null));
        }
        var exact = get(ItemId.createNoCopy(item, meta, null));
        var wildcard = get(ItemId.createNoCopy(item, OreDictionary.WILDCARD_VALUE, null));
        if (exact.isEmpty()) return wildcard;
        if (wildcard.isEmpty()) return exact;
        var combined = new ArrayList<PageAnchor>(exact.size() + wildcard.size());
        combined.addAll(exact);
        combined.addAll(wildcard);
        return combined;
    }
}
