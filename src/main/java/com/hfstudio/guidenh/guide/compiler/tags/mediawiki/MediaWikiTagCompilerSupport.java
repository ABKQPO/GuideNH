package com.hfstudio.guidenh.guide.compiler.tags.mediawiki;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.guide.Guide;
import com.hfstudio.guidenh.guide.color.SymbolicColor;
import com.hfstudio.guidenh.guide.compiler.IndexingContext;
import com.hfstudio.guidenh.guide.compiler.IndexingSink;
import com.hfstudio.guidenh.guide.compiler.PageCompiler;
import com.hfstudio.guidenh.guide.document.block.LytBlock;
import com.hfstudio.guidenh.guide.document.block.LytBlockContainer;
import com.hfstudio.guidenh.guide.indices.CategoryIndex;
import com.hfstudio.guidenh.guide.internal.GuidebookText;
import com.hfstudio.guidenh.guide.mediawiki.MediaWikiGeneratedListBlock;
import com.hfstudio.guidenh.guide.mediawiki.MediaWikiGuideAggregator;
import com.hfstudio.guidenh.guide.mediawiki.MediaWikiListContext;
import com.hfstudio.guidenh.guide.mediawiki.MediaWikiListContextProvider;
import com.hfstudio.guidenh.guide.mediawiki.MediaWikiListEntry;
import com.hfstudio.guidenh.guide.mediawiki.MediaWikiListPlanner;
import com.hfstudio.guidenh.guide.mediawiki.MediaWikiSpecialGeneratedBlock;
import com.hfstudio.guidenh.guide.mediawiki.MediaWikiSpecialGroupedEntry;
import com.hfstudio.guidenh.guide.mediawiki.MediaWikiSpecialListEntry;
import com.hfstudio.guidenh.guide.mediawiki.MediaWikiSpecialPageKind;
import com.hfstudio.guidenh.guide.mediawiki.MediaWikiSpecialPageQuery;
import com.hfstudio.guidenh.guide.mediawiki.MediaWikiSpecialPageResult;
import com.hfstudio.guidenh.guide.style.BorderStyle;
import com.hfstudio.guidenh.libs.mdast.mdx.model.MdxJsxElementFields;
import com.hfstudio.guidenh.libs.unist.UnistNode;

public class MediaWikiTagCompilerSupport {

    private MediaWikiTagCompilerSupport() {}

    public static @Nullable Guide resolveGuide(PageCompiler compiler, LytBlockContainer parent,
        MdxJsxElementFields el) {
        if (compiler.getPageCollection() instanceof Guide guide) {
            return guide;
        }
        parent.appendError(compiler, "MediaWiki tags require a guide-backed page collection", el);
        return null;
    }

    public static @Nullable Guide resolveGuide(IndexingContext indexer) {
        return indexer.getPageCollection() instanceof Guide guide ? guide : null;
    }

    public static MediaWikiListContext createListContext(Guide guide, CategoryIndex categoryIndex) {
        if (guide instanceof MediaWikiListContextProvider provider) {
            MediaWikiListContext providedContext = provider.getMediaWikiListContext();
            if (providedContext != null) {
                return providedContext;
            }
        }
        MediaWikiGuideAggregator aggregatedGuide = MediaWikiGuideAggregator.create(guide);
        CategoryIndex effectiveCategoryIndex = aggregatedGuide.getIndex(CategoryIndex.class);
        return MediaWikiListContext.create(
            aggregatedGuide,
            aggregatedGuide.getPages(),
            aggregatedGuide.getNavigationTree(),
            effectiveCategoryIndex);
    }

    public static MediaWikiGeneratedListBlock createBlock(List<MediaWikiListEntry> entries, int rows,
        String emptyText) {
        var block = new MediaWikiGeneratedListBlock();
        block.setFullWidth(true);
        block.setBorderTop(new BorderStyle(SymbolicColor.TABLE_BORDER, 1));
        block.setBorderBottom(new BorderStyle(SymbolicColor.TABLE_BORDER, 1));
        block.setEntries(entries);
        block.setRows(MediaWikiListPlanner.sanitizeRows(rows));
        block.setEmptyText(emptyText);
        return block;
    }

    public static int readRows(MdxJsxElementFields el) {
        String rawRows = el.getAttributeString("rows", null);
        if (rawRows == null || rawRows.trim()
            .isEmpty()) {
            return MediaWikiListPlanner.DEFAULT_ROWS;
        }
        try {
            return MediaWikiListPlanner.sanitizeRows(Integer.parseInt(rawRows.trim()));
        } catch (NumberFormatException ignored) {
            return MediaWikiListPlanner.DEFAULT_ROWS;
        }
    }

    public static MediaWikiSpecialPageQuery readSpecialQuery(MdxJsxElementFields el) {
        Map<String, String> parameters = new LinkedHashMap<>();
        appendSpecialQueryParameter(
            parameters,
            MediaWikiSpecialPageQuery.PARAM_PAGE,
            el.getAttributeString("page", null));
        appendSpecialQueryParameter(
            parameters,
            MediaWikiSpecialPageQuery.PARAM_PREFIX,
            el.getAttributeString("prefix", null));
        appendSpecialQueryParameter(
            parameters,
            MediaWikiSpecialPageQuery.PARAM_LANGUAGE,
            el.getAttributeString("language", null));
        String searchText = sanitizeOptionalText(el.getAttributeString("query", null));
        return new MediaWikiSpecialPageQuery(
            searchText != null ? searchText : "",
            MediaWikiSpecialPageQuery.PAGE_SIZE,
            parameters);
    }

    private static void appendSpecialQueryParameter(Map<String, String> parameters, String key,
        @Nullable String value) {
        String sanitizedValue = sanitizeOptionalText(value);
        if (sanitizedValue != null) {
            parameters.put(key, sanitizedValue);
        }
    }

    private static @Nullable String sanitizeOptionalText(@Nullable String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    public static void indexEntries(IndexingSink sink, UnistNode parent, List<MediaWikiListEntry> entries) {
        for (MediaWikiListEntry entry : entries) {
            sink.appendText(parent, entry.title());
            sink.appendBreak();
        }
    }

    public static LytBlock createSpecialBlock(MediaWikiSpecialPageResult result, int rows) {
        var block = new MediaWikiSpecialGeneratedBlock();
        block.setFullWidth(true);
        block.setBorderTop(new BorderStyle(SymbolicColor.TABLE_BORDER, 1));
        block.setBorderBottom(new BorderStyle(SymbolicColor.TABLE_BORDER, 1));
        block.setResult(result);
        block.setRows(MediaWikiListPlanner.sanitizeRows(rows));
        block.setEmptyText(GuidebookText.MediaWikiNoPages.text());
        return block;
    }

    public static LytBlock createSpecialBlock(MediaWikiSpecialPageResult result, int rows, MediaWikiListContext context,
        MediaWikiSpecialPageQuery query, com.hfstudio.guidenh.guide.mediawiki.MediaWikiSpecialPageResolver resolver) {
        var block = (MediaWikiSpecialGeneratedBlock) createSpecialBlock(result, rows);
        if (result != null && context != null && resolver != null) {
            block.setResolverContext(context, result.definition(), resolver, query != null ? query.parameters() : null);
        }
        return block;
    }

    public static void indexSpecialResult(IndexingSink sink, UnistNode parent, MediaWikiSpecialPageResult result) {
        if (result == null) {
            return;
        }
        if (result.kind() == MediaWikiSpecialPageKind.GROUPED
            || result.kind() == MediaWikiSpecialPageKind.GROUP_INDEX) {
            for (MediaWikiSpecialGroupedEntry group : result.groupedEntries()) {
                sink.appendText(parent, group.title());
                sink.appendBreak();
                for (MediaWikiSpecialListEntry child : group.children()) {
                    sink.appendText(parent, child.title());
                    sink.appendBreak();
                }
            }
            return;
        }
        for (MediaWikiSpecialListEntry entry : result.flatEntries()) {
            sink.appendText(parent, entry.title());
            sink.appendBreak();
        }
    }

}
