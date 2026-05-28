package com.hfstudio.guidenh.guide.indices;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.oredict.OreDictionary;

import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.Nullable;

import com.google.gson.stream.JsonWriter;
import com.gtnewhorizon.gtnhlib.util.data.ItemId;
import com.hfstudio.guidenh.guide.GuidePageChange;
import com.hfstudio.guidenh.guide.PageAnchor;
import com.hfstudio.guidenh.guide.compiler.ParsedGuidePage;

import cpw.mods.fml.common.FMLLog;

/**
 * An index of Forge ore-dictionary names to the main guidebook page describing them.
 * <p/>
 * The {@code ore_ids} frontmatter list contains plain ore-dictionary entry names (e.g.
 * {@code ingotIron}). When an item stack is registered against any of those ore names the page
 * is considered a match.
 * <p/>
 * At rebuild time a precomputed {@code ItemId 閳?PageAnchor} map is built by expanding each
 * indexed ore name through {@link OreDictionary#getOres(String)}. This makes
 * {@link #findByStack(ItemStack)} an O(1) hash lookup instead of requiring a per-call scan of all
 * ore-dictionary IDs assigned to the hovered item, which can be expensive in large modpacks.
 */
public class OreIndex extends UniqueIndex<String, PageAnchor> {

    /**
     * Precomputed item (item + damage) 閳?page anchor map built during {@link #rebuild} /
     * {@link #update}. Keyed by exact damage value and also by {@link OreDictionary#WILDCARD_VALUE}
     * for stacks registered with wildcard meta, matching the two-step lookup used by
     * {@link ItemIndex#findByItem}.
     */
    private Map<ItemId, PageAnchor> itemCache = Map.of();

    public OreIndex() {
        super(
            "Ore Dictionary Index",
            OreIndex::getOreAnchors,
            JsonWriter::value,
            (writer, value) -> writer.value(value.toString()));
    }

    @Override
    public void rebuild(List<ParsedGuidePage> pages) {
        super.rebuild(pages);
        itemCache = buildItemCache(pages);
    }

    @Override
    public void update(List<ParsedGuidePage> allPages, List<GuidePageChange> changes) {
        super.update(allPages, changes);
        itemCache = buildItemCache(allPages);
    }

    /**
     * Expands every ore name that maps to a guide page into the concrete items registered under
     * that name via the Forge ore dictionary, and returns the resulting item-level map.
     */
    private Map<ItemId, PageAnchor> buildItemCache(List<ParsedGuidePage> pages) {
        Map<ItemId, PageAnchor> cache = new HashMap<>();
        for (var page : pages) {
            for (var entry : getOreAnchors(page)) {
                String oreName = entry.getKey();
                PageAnchor anchor = get(oreName);
                if (anchor == null) {
                    // ore name was rejected (e.g. duplicate) 閳?skip
                    continue;
                }
                for (ItemStack stack : OreDictionary.getOres(oreName)) {
                    if (stack == null || stack.getItem() == null) continue;
                    Item item = stack.getItem();
                    int meta = stack.getItemDamage();
                    cache.putIfAbsent(ItemId.createNoCopy(item, meta, null), anchor);
                }
            }
        }
        return cache;
    }

    /**
     * Looks up a page for the given stack using the precomputed item-level map. Falls back to the
     * wildcard-meta entry when no exact-meta entry is present, mirroring the two-step lookup that
     * {@link ItemIndex#findByItem} uses.
     */
    @Nullable
    public PageAnchor findByStack(@Nullable ItemStack stack) {
        if (stack == null || stack.getItem() == null) return null;
        Item item = stack.getItem();
        int meta = stack.getItemDamage();
        // Exact-meta lookup
        PageAnchor anchor = itemCache.get(ItemId.createNoCopy(item, meta, null));
        if (anchor != null) return anchor;
        // Wildcard-meta fallback (ore registered with WILDCARD_VALUE damage)
        if (meta != OreDictionary.WILDCARD_VALUE) {
            anchor = itemCache.get(ItemId.createNoCopy(item, OreDictionary.WILDCARD_VALUE, null));
        }
        return anchor;
    }

    public static List<Pair<String, PageAnchor>> getOreAnchors(ParsedGuidePage page) {
        var oreIdsNode = page.getFrontmatter()
            .additionalProperties()
            .get("ore_ids");
        if (oreIdsNode == null) {
            return List.of();
        }

        if (!(oreIdsNode instanceof List<?>oreIdList)) {
            FMLLog.getLogger()
                .warn("[GuideNH] [OreIndex] Page {} contains malformed ore_ids frontmatter", page.getId());
            return List.of();
        }

        var oreAnchors = new ArrayList<Pair<String, PageAnchor>>(oreIdList.size());

        for (var listEntry : oreIdList) {
            if (listEntry instanceof String oreName) {
                String trimmed = oreName.trim();
                if (trimmed.isEmpty()) {
                    FMLLog.getLogger()
                        .warn("[GuideNH] [OreIndex] Page {} contains an empty ore_ids frontmatter entry", page.getId());
                    continue;
                }
                oreAnchors.add(Pair.of(trimmed, new PageAnchor(page.getId(), null)));
            } else {
                FMLLog.getLogger()
                    .warn(
                        "[GuideNH] [OreIndex] Page {} contains a malformed ore_ids frontmatter entry: {}",
                        page.getId(),
                        listEntry);
            }
        }

        return oreAnchors;
    }
}
