package com.hfstudio.guidenh.guide.indices;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import net.minecraft.util.ResourceLocation;

import com.google.gson.stream.JsonWriter;
import com.hfstudio.guidenh.guide.Guide;
import com.hfstudio.guidenh.guide.GuidePageChange;
import com.hfstudio.guidenh.guide.PageAnchor;
import com.hfstudio.guidenh.guide.compiler.ParsedGuidePage;
import com.hfstudio.guidenh.guide.mediawiki.MediaWikiCategoryMember;
import com.hfstudio.guidenh.guide.mediawiki.MediaWikiCategoryParser;
import com.hfstudio.guidenh.guide.mediawiki.MediaWikiCategoryReference;

/**
 * Pages can declare to be part of multiple categories using the categories frontmatter.
 * <p/>
 * This index is installed by default on all {@linkplain Guide guides}.
 */
public class CategoryIndex implements PageIndex {

    private static final Comparator<String> CATEGORY_COMPARATOR = Comparator
        .comparing(value -> value.toLowerCase(Locale.ROOT));

    private final Map<String, List<MediaWikiCategoryMember>> categories = new HashMap<>();
    private final Map<String, String> canonicalCategoryNames = new HashMap<>();

    @Override
    public String getName() {
        return "Categories";
    }

    public List<PageAnchor> get(String categoryName) {
        List<MediaWikiCategoryMember> members = getMembers(categoryName);
        if (members.isEmpty()) {
            return List.of();
        }

        var anchors = new ArrayList<PageAnchor>(members.size());
        for (MediaWikiCategoryMember member : members) {
            anchors.add(member.pageAnchor());
        }
        return anchors;
    }

    public List<MediaWikiCategoryMember> getMembers(String categoryName) {
        List<MediaWikiCategoryMember> members = categories.get(resolveCanonicalCategoryName(categoryName));
        return members != null ? new ArrayList<>(members) : List.of();
    }

    public List<String> getCategoryNames() {
        var names = new ArrayList<>(categories.keySet());
        names.sort(CATEGORY_COMPARATOR);
        return names;
    }

    @Override
    public boolean supportsUpdate() {
        return true;
    }

    @Override
    public void rebuild(List<ParsedGuidePage> pages) {
        categories.clear();
        canonicalCategoryNames.clear();
        for (ParsedGuidePage page : pages) {
            indexPage(page);
        }
        sortMembers();
    }

    @Override
    public void update(List<ParsedGuidePage> allPages, List<GuidePageChange> changes) {
        if (changes.isEmpty()) {
            return;
        }

        var idsToRemove = new HashSet<ResourceLocation>(changes.size());
        for (GuidePageChange change : changes) {
            idsToRemove.add(change.pageId());
        }

        categories.values()
            .removeIf(members -> {
                members.removeIf(
                    member -> idsToRemove.contains(
                        member.pageAnchor()
                            .pageId()));
                return members.isEmpty();
            });
        rebuildCanonicalCategoryNames();

        for (GuidePageChange change : changes) {
            if (change.newPage() != null) {
                indexPage(change.newPage());
            }
        }

        sortMembers();
    }

    @Override
    public void export(JsonWriter writer) throws IOException {
        writer.beginArray();
        for (String categoryName : getCategoryNames()) {
            writer.value(categoryName);
            writer.beginArray();
            for (MediaWikiCategoryMember member : getMembers(categoryName)) {
                writer.value(
                    member.pageAnchor()
                        .toString());
            }
            writer.endArray();
        }
        writer.endArray();
    }

    public static List<MediaWikiCategoryMember> getCategories(ParsedGuidePage page) {
        var categories = new ArrayList<MediaWikiCategoryMember>();
        PageAnchor anchor = PageAnchor.page(page.getId());
        for (MediaWikiCategoryReference category : MediaWikiCategoryParser.parseReferences(page)) {
            categories.add(new MediaWikiCategoryMember(category.categoryName(), category.sortKey(), anchor));
        }
        return categories;
    }

    private void indexPage(ParsedGuidePage page) {
        for (MediaWikiCategoryMember member : getCategories(page)) {
            String canonicalCategoryName = canonicalizeCategoryName(member.categoryName());
            categories.computeIfAbsent(canonicalCategoryName, ignored -> new ArrayList<>())
                .add(new MediaWikiCategoryMember(canonicalCategoryName, member.sortKey(), member.pageAnchor()));
        }
    }

    private void sortMembers() {
        Map<String, List<MediaWikiCategoryMember>> sorted = new LinkedHashMap<>();
        for (String categoryName : getCategoryNames()) {
            List<MediaWikiCategoryMember> members = categories.get(categoryName);
            if (members == null || members.isEmpty()) {
                continue;
            }
            members.sort(
                Comparator.comparing((MediaWikiCategoryMember member) -> normalize(member.sortKey()))
                    .thenComparing(
                        member -> member.pageAnchor()
                            .pageId()
                            .toString()));
            sorted.put(categoryName, members);
        }
        categories.clear();
        categories.putAll(sorted);
    }

    private static String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }

    private void rebuildCanonicalCategoryNames() {
        canonicalCategoryNames.clear();
        for (String categoryName : categories.keySet()) {
            canonicalCategoryNames.put(normalize(categoryName), categoryName);
        }
    }

    private String canonicalizeCategoryName(String categoryName) {
        String normalizedCategoryName = normalize(categoryName);
        String canonicalCategoryName = canonicalCategoryNames.get(normalizedCategoryName);
        if (canonicalCategoryName != null) {
            return canonicalCategoryName;
        }
        canonicalCategoryNames.put(normalizedCategoryName, categoryName);
        return categoryName;
    }

    private String resolveCanonicalCategoryName(String categoryName) {
        if (categoryName == null) {
            return null;
        }
        return canonicalCategoryNames.getOrDefault(normalize(categoryName), categoryName);
    }
}
