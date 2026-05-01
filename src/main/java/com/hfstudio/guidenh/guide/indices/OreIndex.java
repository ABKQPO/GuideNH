package com.hfstudio.guidenh.guide.indices;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.minecraft.item.ItemStack;
import net.minecraftforge.oredict.OreDictionary;

import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hfstudio.guidenh.guide.PageAnchor;
import com.hfstudio.guidenh.guide.compiler.ParsedGuidePage;

/**
 * An index of Forge ore-dictionary names to the main guidebook page describing them.
 * <p/>
 * The {@code ore_ids} frontmatter list contains plain ore-dictionary entry names (e.g.
 * {@code ingotIron}). When an item stack is registered against any of those ore names the page
 * is considered a match.
 */
public class OreIndex extends UniqueIndex<String, PageAnchor> {

    public static final Logger LOG = LoggerFactory.getLogger(OreIndex.class);

    public OreIndex() {
        super(
            "Ore Dictionary Index",
            OreIndex::getOreAnchors,
            (writer, value) -> writer.value(value),
            (writer, value) -> writer.value(value.toString()));
    }

    /**
     * Looks up a page for the given stack by scanning all ore-dictionary names registered to it.
     * Returns the first matching page (deterministic by ore-id iteration order from Forge).
     */
    @Nullable
    public PageAnchor findByStack(@Nullable ItemStack stack) {
        if (stack == null || stack.getItem() == null) return null;
        int[] oreIds = OreDictionary.getOreIDs(stack);
        if (oreIds == null || oreIds.length == 0) return null;
        for (int oreId : oreIds) {
            String name = OreDictionary.getOreName(oreId);
            if (name == null || name.isEmpty() || "Unknown".equals(name)) continue;
            PageAnchor anchor = get(name);
            if (anchor != null) return anchor;
        }
        return null;
    }

    public static List<Pair<String, PageAnchor>> getOreAnchors(ParsedGuidePage page) {
        var oreIdsNode = page.getFrontmatter()
            .additionalProperties()
            .get("ore_ids");
        if (oreIdsNode == null) {
            return Collections.emptyList();
        }

        if (!(oreIdsNode instanceof List<?>oreIdList)) {
            LOG.warn("Page {} contains malformed ore_ids frontmatter", page.getId());
            return Collections.emptyList();
        }

        var oreAnchors = new ArrayList<Pair<String, PageAnchor>>();

        for (var listEntry : oreIdList) {
            if (listEntry instanceof String oreName) {
                String trimmed = oreName.trim();
                if (trimmed.isEmpty()) {
                    LOG.warn("Page {} contains an empty ore_ids frontmatter entry", page.getId());
                    continue;
                }
                oreAnchors.add(Pair.of(trimmed, new PageAnchor(page.getId(), null)));
            } else {
                LOG.warn("Page {} contains a malformed ore_ids frontmatter entry: {}", page.getId(), listEntry);
            }
        }

        return oreAnchors;
    }
}
