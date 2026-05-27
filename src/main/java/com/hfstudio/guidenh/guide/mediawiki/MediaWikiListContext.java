package com.hfstudio.guidenh.guide.mediawiki;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import net.minecraft.util.ResourceLocation;

import org.jetbrains.annotations.Nullable;

import com.github.bsideup.jabel.Desugar;
import com.hfstudio.guidenh.guide.Guide;
import com.hfstudio.guidenh.guide.compiler.ParsedGuidePage;
import com.hfstudio.guidenh.guide.indices.CategoryIndex;
import com.hfstudio.guidenh.guide.navigation.NavigationTree;

@Desugar
public record MediaWikiListContext(Guide guide, Map<ResourceLocation, ParsedGuidePage> parsedPagesById,
    NavigationTree navigationTree, CategoryIndex categoryIndex, MediaWikiSpecialDataIndex specialDataIndex) {

    private static final Set<ResourceLocation> EMPTY_PAGE_IDS = Collections.emptySet();

    public MediaWikiListContext {
        Objects.requireNonNull(guide, "guide");
        Objects.requireNonNull(parsedPagesById, "parsedPagesById");
        Objects.requireNonNull(navigationTree, "navigationTree");
        Objects.requireNonNull(categoryIndex, "categoryIndex");
        Objects.requireNonNull(specialDataIndex, "specialDataIndex");
        parsedPagesById = Collections.unmodifiableMap(new LinkedHashMap<>(parsedPagesById));
    }

    public static MediaWikiListContext create(Guide guide, Collection<ParsedGuidePage> pages,
        NavigationTree navigationTree, CategoryIndex categoryIndex) {
        MediaWikiSpecialDataIndex specialDataIndex = new MediaWikiSpecialDataIndexer()
            .build(guide, pages, categoryIndex);
        return create(guide, pages, navigationTree, categoryIndex, specialDataIndex);
    }

    public static MediaWikiListContext create(Guide guide, Collection<ParsedGuidePage> pages,
        NavigationTree navigationTree, CategoryIndex categoryIndex, MediaWikiSpecialDataIndex specialDataIndex) {
        Map<ResourceLocation, ParsedGuidePage> parsedPagesById = new LinkedHashMap<>();
        if (pages != null) {
            for (ParsedGuidePage page : pages) {
                if (page != null) {
                    parsedPagesById.putIfAbsent(page.getId(), page);
                }
            }
        }
        return new MediaWikiListContext(guide, parsedPagesById, navigationTree, categoryIndex, specialDataIndex);
    }

    public Collection<ParsedGuidePage> pages() {
        return parsedPagesById.values();
    }

    public Set<ResourceLocation> pageIds() {
        return parsedPagesById.isEmpty() ? EMPTY_PAGE_IDS : parsedPagesById.keySet();
    }

    public @Nullable ParsedGuidePage getParsedPage(ResourceLocation pageId) {
        return parsedPagesById.get(pageId);
    }
}
