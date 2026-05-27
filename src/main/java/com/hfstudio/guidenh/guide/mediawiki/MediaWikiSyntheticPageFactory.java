package com.hfstudio.guidenh.guide.mediawiki;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;

import net.minecraft.util.ResourceLocation;

import com.github.bsideup.jabel.Desugar;
import com.hfstudio.guidenh.guide.Guide;
import com.hfstudio.guidenh.guide.compiler.ParsedGuidePage;
import com.hfstudio.guidenh.guide.indices.CategoryIndex;
import com.hfstudio.guidenh.guide.internal.MutableGuide;

import cpw.mods.fml.common.FMLLog;

public class MediaWikiSyntheticPageFactory {

    private MediaWikiSyntheticPageFactory() {}

    public static Map<ResourceLocation, ParsedGuidePage> buildPages(Guide guide, Collection<ParsedGuidePage> pages,
        CategoryIndex categoryIndex, Map<ResourceLocation, SyntheticSourceSnapshot> cache, SyntheticPageParser parser) {
        long startNanos = System.nanoTime();
        String language = resolveLanguage(guide, pages);
        String namespace = guide.getDefaultNamespace();
        String sourcePack = guide.getId()
            .toString();

        var syntheticPages = new LinkedHashMap<ResourceLocation, ParsedGuidePage>();
        HashSet<ResourceLocation> seenIds = new HashSet<>();
        long specialStartNanos = System.nanoTime();
        for (MediaWikiSpecialDefinition definition : MediaWikiSpecialCatalog.definitions()) {
            ResourceLocation pageId = MediaWikiPageIds.specialPageId(namespace, definition.name());
            String source = buildSpecialSource(definition.name());
            syntheticPages.put(pageId, reuseOrParseSyntheticPage(cache, parser, pageId, sourcePack, language, source));
            seenIds.add(pageId);
        }
        long specialElapsedNanos = System.nanoTime() - specialStartNanos;

        long categoryStartNanos = System.nanoTime();
        for (String categoryName : categoryIndex.getCategoryNames()) {
            ResourceLocation pageId = MediaWikiPageIds.categoryPageId(namespace, categoryName);
            String source = buildCategorySource(categoryName);
            syntheticPages.put(pageId, reuseOrParseSyntheticPage(cache, parser, pageId, sourcePack, language, source));
            seenIds.add(pageId);
        }
        cache.keySet()
            .removeIf(pageId -> !seenIds.contains(pageId));
        long categoryElapsedNanos = System.nanoTime() - categoryStartNanos;
        long totalElapsedNanos = System.nanoTime() - startNanos;
        FMLLog.getLogger()
            .info(
                "[GuideNH] [MediaWikiSyntheticPageFactory] Built {} synthetic pages in {} ms (special: {} ms, category: {} ms)",
                syntheticPages.size(),
                nanosToMillis(totalElapsedNanos),
                nanosToMillis(specialElapsedNanos),
                nanosToMillis(categoryElapsedNanos));
        return syntheticPages;
    }

    private static ParsedGuidePage reuseOrParseSyntheticPage(Map<ResourceLocation, SyntheticSourceSnapshot> cache,
        SyntheticPageParser parser, ResourceLocation pageId, String sourcePack, String language, String source) {
        SyntheticSourceSnapshot cached = cache.get(pageId);
        if (cached != null && cached.sourcePack()
            .equals(sourcePack)
            && cached.language()
                .equals(language)
            && cached.source()
                .equals(source)) {
            return cached.parsedPage();
        }
        ParsedGuidePage parsedPage = parser.parse(pageId, sourcePack, language, source);
        cache.put(pageId, new SyntheticSourceSnapshot(sourcePack, language, source, parsedPage));
        return parsedPage;
    }

    private static String resolveLanguage(Guide guide, Collection<ParsedGuidePage> pages) {
        for (ParsedGuidePage page : pages) {
            if (page != null && page.getLanguage() != null
                && !page.getLanguage()
                    .isEmpty()) {
                return page.getLanguage();
            }
        }
        if (guide instanceof MutableGuide mutableGuide) {
            return mutableGuide.getDefaultLanguage();
        }
        return "en_us";
    }

    private static String buildCategorySource(String categoryName) {
        StringBuilder source = new StringBuilder();
        source.append("# ")
            .append(MediaWikiPageIds.toCategoryTitle(categoryName))
            .append("\n\n")
            .append("<Category name=\"")
            .append(escapeAttribute(categoryName))
            .append("\" rows=\"")
            .append(MediaWikiListPlanner.DEFAULT_ROWS)
            .append("\" />\n");
        return source.toString();
    }

    private static String buildSpecialSource(String specialName) {
        StringBuilder source = new StringBuilder();
        source.append("# ")
            .append(MediaWikiPageTitleResolver.resolveSpecialTitle(specialName))
            .append("\n\n")
            .append("<Special name=\"")
            .append(specialName)
            .append("\" rows=\"")
            .append(resolveSpecialRows(specialName))
            .append("\" />\n");
        return source.toString();
    }

    private static int resolveSpecialRows(String specialName) {
        if (MediaWikiSpecialPageIds.CATEGORY_TREE.equals(specialName)) {
            return 2;
        }
        if (MediaWikiSpecialPageIds.ALL_TRANSLATIONS.equals(specialName)
            || MediaWikiSpecialPageIds.LANGUAGE_STATISTICS.equals(specialName)
            || MediaWikiSpecialPageIds.PAGE_TRANSLATION.equals(specialName)
            || MediaWikiSpecialPageIds.SEARCH_TRANSLATIONS.equals(specialName)
            || MediaWikiSpecialPageIds.TRANSLATION_STATISTICS.equals(specialName)) {
            return 1;
        }
        return MediaWikiListPlanner.DEFAULT_ROWS;
    }

    private static long nanosToMillis(long nanos) {
        return nanos / 1_000_000L;
    }

    private static String escapeAttribute(String value) {
        return value.replace("&", "&amp;")
            .replace("\"", "&quot;");
    }

    @FunctionalInterface
    public interface SyntheticPageParser {

        ParsedGuidePage parse(ResourceLocation pageId, String sourcePack, String language, String source);
    }

    @Desugar
    public record SyntheticSourceSnapshot(String sourcePack, String language, String source,
        ParsedGuidePage parsedPage) {}
}
