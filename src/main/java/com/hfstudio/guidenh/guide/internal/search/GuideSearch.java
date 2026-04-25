package com.hfstudio.guidenh.guide.internal.search;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nullable;

import net.minecraft.util.ResourceLocation;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.highlight.Highlighter;
import org.apache.lucene.search.highlight.InvalidTokenOffsetsException;
import org.apache.lucene.search.highlight.QueryScorer;
import org.apache.lucene.store.ByteBuffersDirectory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.bsideup.jabel.Desugar;
import com.hfstudio.guidenh.guide.Guide;
import com.hfstudio.guidenh.guide.Guides;
import com.hfstudio.guidenh.guide.compiler.IndexingSink;
import com.hfstudio.guidenh.guide.compiler.ParsedGuidePage;
import com.hfstudio.guidenh.guide.document.DefaultStyles;
import com.hfstudio.guidenh.guide.document.flow.LytFlowContent;
import com.hfstudio.guidenh.guide.document.flow.LytFlowSpan;
import com.hfstudio.guidenh.guide.internal.util.LangUtil;
import com.hfstudio.guidenh.libs.mdast.model.MdAstHeading;
import com.hfstudio.guidenh.libs.unist.UnistNode;

/**
 * Manages the in-memory Lucene index for guide search.
 */
public class GuideSearch implements AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(GuideSearch.class);

    /** Maximum indexing time budget per tick. */
    private static final long TIME_PER_TICK = TimeUnit.MILLISECONDS.toNanos(5);

    private final ByteBuffersDirectory directory = new ByteBuffersDirectory();

    private final Analyzer analyzer;
    private final IndexWriter indexWriter;
    private IndexReader indexReader;
    private IndexSearcher indexSearcher;
    private final List<GuideIndexingTask> pendingTasks = new ArrayList<>();
    private Instant indexingStarted;
    private int pagesIndexed;
    private final Set<String> warnedAboutLanguage = Collections.synchronizedSet(new HashSet<>());
    private final Set<String> indexedLanguages = Collections.synchronizedSet(new HashSet<>());

    public GuideSearch() {
        analyzer = new LanguageSpecificAnalyzerWrapper();
        var config = new IndexWriterConfig(analyzer);
        try {
            indexWriter = new IndexWriter(directory, config);
            // Commit once so DirectoryReader can open the in-memory index immediately.
            indexWriter.flush();
            indexWriter.commit();
            indexReader = DirectoryReader.open(directory);
            indexSearcher = new IndexSearcher(indexReader);
        } catch (IOException e) {
            // ByteBuffersDirectory keeps this in memory, so initialization failures are unexpected.
            throw new UncheckedIOException("Failed to create index writer.", e);
        }
    }

    public void index(Guide guide) {
        try {
            indexWriter.deleteDocuments(
                new Term(
                    IndexSchema.FIELD_GUIDE_ID,
                    guide.getId()
                        .toString()));
        } catch (IOException e) {
            LOG.error("Failed to delete all documents before re-indexing.", e);
        }

        if (pendingTasks.isEmpty()) {
            indexingStarted = Instant.now();
            pagesIndexed = 0;
        }
        pendingTasks.removeIf(
            t -> t.guide.getId()
                .equals(guide.getId()));
        pendingTasks.add(new GuideIndexingTask(guide, new ArrayList<>(guide.getPages())));
    }

    public void indexAll() {
        for (var guide : Guides.getAll()) {
            index(guide);
        }
    }

    public void processWork() {
        if (pendingTasks.isEmpty()) {
            return;
        }

        long start = System.nanoTime();

        var guideTaskIt = pendingTasks.iterator();
        while (guideTaskIt.hasNext()) {
            if (isTimeElapsed(start)) {
                return;
            }

            var guideTask = guideTaskIt.next();
            var guide = guideTask.guide();

            var pageIt = guideTask.pendingPages.iterator();
            while (pageIt.hasNext()) {
                if (isTimeElapsed(start)) {
                    return;
                }

                var page = pageIt.next();

                var pageDoc = createPageDocument(guideTask.guide(), page);
                if (pageDoc != null) {
                    try {
                        indexWriter.addDocument(pageDoc);
                    } catch (IOException e) {
                        LOG.error("Failed to index document {}{}", guide, page, e);
                    }

                    var searchLang = pageDoc.get(IndexSchema.FIELD_SEARCH_LANG);
                    if (searchLang != null) {
                        indexedLanguages.add(searchLang);
                    }
                }
                pagesIndexed++;
                pageIt.remove();
            }

            guideTaskIt.remove();
        }

        try {
            indexWriter.flush();
            indexWriter.commit();
            refreshIndexReader();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        LOG.info("Indexing of {} pages finished in {}", pagesIndexed, Duration.between(indexingStarted, Instant.now()));
    }

    private boolean isTimeElapsed(long start) {
        return System.nanoTime() - start >= TIME_PER_TICK;
    }

    private void refreshIndexReader() throws IOException {
        var newReader = DirectoryReader.open(directory);
        var oldReader = indexReader;
        indexReader = newReader;
        indexSearcher = new IndexSearcher(newReader);
        oldReader.close();
    }

    public List<SearchResult> searchGuide(String queryText, @Nullable Guide onlyFromGuide) {
        if (queryText.isEmpty()) {
            return Collections.emptyList();
        }

        var searchLanguage = getLuceneLanguageFromMinecraft(LangUtil.getCurrentLanguage());
        var indexSearcher = this.indexSearcher;

        Query query;
        try {
            query = GuideQueryParser.parse(queryText, analyzer, indexedLanguages);
        } catch (Exception e) {
            LOG.debug("Failed to parse search query: '{}'", queryText, e);
            return Collections.emptyList();
        }

        // Add an exact guide filter without changing the parsed query.
        if (onlyFromGuide != null) {
            query = new BooleanQuery.Builder().add(query, BooleanClause.Occur.MUST)
                .add(
                    new TermQuery(
                        new Term(
                            IndexSchema.FIELD_GUIDE_ID,
                            onlyFromGuide.getId()
                                .toString())),
                    BooleanClause.Occur.FILTER)
                .build();
        }

        LOG.debug("Running GuideME search query: {}", query);

        TopDocs topDocs;
        try {
            topDocs = indexSearcher.search(query, 25);
        } catch (IOException e) {
            LOG.error("Failed to search for '{}'", queryText, e);
            return Collections.emptyList();
        }

        var result = new ArrayList<SearchResult>(topDocs.scoreDocs.length);
        var highlighter = new Highlighter(new QueryScorer(query));
        try {
            for (ScoreDoc scoreDoc : topDocs.scoreDocs) {
                var document = indexSearcher.doc(scoreDoc.doc);
                var guideId = new ResourceLocation(document.get(IndexSchema.FIELD_GUIDE_ID));
                var pageId = new ResourceLocation(document.get(IndexSchema.FIELD_PAGE_ID));

                var guide = Guides.getById(guideId);
                if (guide == null) {
                    LOG.warn("Search index produced guide id {} which couldn't be found.", guideId);
                    continue;
                }

                var page = guide.getParsedPage(pageId);
                if (page == null) {
                    LOG.warn("Search index produced page {} in guide {}, which couldn't be found.", pageId, guideId);
                    continue;
                }

                String bestFragment = "";
                try {
                    bestFragment = highlighter.getBestFragment(
                        analyzer,
                        IndexSchema.getTextField(searchLanguage),
                        document.get(IndexSchema.FIELD_TEXT));
                    if (bestFragment == null) {
                        bestFragment = "";
                    }
                } catch (InvalidTokenOffsetsException e) {
                    LOG.error("Cannot determine text to highlight for result", e);
                }

                var pageTitle = document.get(IndexSchema.FIELD_TITLE);
                result.add(new SearchResult(guideId, pageId, pageTitle, buildHighlightedFragment(bestFragment)));
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        return result;
    }

    private static boolean isStartOfHighlight(CharSequence text, int i) {
        return (i + 3 <= text.length()) && text.charAt(i) == '<'
            && text.charAt(i + 1) == 'B'
            && text.charAt(i + 2) == '>';
    }

    private static boolean isEndOfHighlight(CharSequence text, int i) {
        return (i + 4 <= text.length()) && text.charAt(i) == '<'
            && text.charAt(i + 1) == '/'
            && text.charAt(i + 2) == 'B'
            && text.charAt(i + 3) == '>';
    }

    @Nullable
    private Document createPageDocument(Guide guide, ParsedGuidePage page) {
        var pageText = getSearchableText(guide, page);
        var pageTitle = getPageTitle(guide, page);

        var searchLang = getLuceneLanguageFromMinecraft(page.getLanguage());

        var doc = new Document();
        doc.add(
            new StringField(
                IndexSchema.FIELD_GUIDE_ID,
                guide.getId()
                    .toString(),
                Field.Store.YES));
        doc.add(
            new StoredField(
                IndexSchema.FIELD_PAGE_ID,
                page.getId()
                    .toString()));
        doc.add(new StoredField(IndexSchema.FIELD_LANG, page.getLanguage()));
        doc.add(new StoredField(IndexSchema.FIELD_SEARCH_LANG, searchLang));

        // Keep the original strings for result display and Lucene highlighter output.
        doc.add(new StoredField(IndexSchema.FIELD_TITLE, pageTitle));
        doc.add(new StoredField(IndexSchema.FIELD_TEXT, pageText));

        doc.add(new TextField(IndexSchema.getTitleField(searchLang), pageTitle, Field.Store.NO));
        doc.add(new TextField(IndexSchema.getTextField(searchLang), pageText, Field.Store.NO));
        return doc;
    }

    private static LytFlowContent buildHighlightedFragment(String bestFragment) {
        var root = new LytFlowSpan();
        LytFlowSpan currentSpan = root;
        int startOfSegment = 0;
        for (int i = 0; i < bestFragment.length(); i++) {
            if (isStartOfHighlight(bestFragment, i)) {
                appendFragmentText(currentSpan, bestFragment, startOfSegment, i);
                startOfSegment = i + 3;
                var highlightedSpan = new LytFlowSpan();
                highlightedSpan.setStyle(DefaultStyles.SEARCH_RESULT_HIGHLIGHT);
                currentSpan.append(highlightedSpan);
                currentSpan = highlightedSpan;
            } else if (isEndOfHighlight(bestFragment, i)) {
                appendFragmentText(currentSpan, bestFragment, startOfSegment, i);
                startOfSegment = i + 4;
                currentSpan = Objects.requireNonNull((LytFlowSpan) currentSpan.getFlowParent());
            }
        }
        appendFragmentText(currentSpan, bestFragment, startOfSegment, bestFragment.length());
        return root;
    }

    private static void appendFragmentText(LytFlowSpan span, String fragment, int startInclusive, int endExclusive) {
        if (endExclusive > startInclusive) {
            span.appendText(fragment.substring(startInclusive, endExclusive));
        }
    }

    private String getLuceneLanguageFromMinecraft(String language) {
        var luceneLang = Analyzers.MINECRAFT_TO_LUCENE_LANG.get(language);
        if (luceneLang == null) {
            if (warnedAboutLanguage.add(language)) {
                LOG.warn("Minecraft language '{}' is unknown, so search falls back to english.", language);
            }
            return Analyzers.LANG_ENGLISH;
        }
        return luceneLang;
    }

    private static String getPageTitle(Guide guide, ParsedGuidePage page) {

        // Frontmatter navigation title wins.
        var navigationEntry = page.getFrontmatter()
            .navigationEntry();
        if (navigationEntry != null) {
            return navigationEntry.title();
        }

        // Fall back to the first level-1 heading.
        for (var child : page.getAstRoot()
            .children()) {
            if (child instanceof MdAstHeading heading && heading.depth == 1) {
                var pageTitle = new StringBuilder();
                var sink = new IndexingSink() {

                    @Override
                    public void appendText(UnistNode parent, String text) {
                        pageTitle.append(text);
                    }

                    @Override
                    public void appendBreak() {
                        pageTitle.append(' ');
                    }
                };
                new PageIndexer(guide, guide.getExtensions(), page.getId()).indexContent(heading.children(), sink);
                return pageTitle.toString();
            }
        }

        return page.getId()
            .toString();
    }

    private static String getSearchableText(Guide guide, ParsedGuidePage page) {
        var searchableText = new StringBuilder();

        var sink = new IndexingSink() {

            @Override
            public void appendText(UnistNode parent, String text) {
                searchableText.append(text);
            }

            @Override
            public void appendBreak() {
                searchableText.append('\n');
            }
        };
        new PageIndexer(guide, guide.getExtensions(), page.getId()).index(page.getAstRoot(), sink);
        return searchableText.toString();
    }

    @Override
    public void close() throws IOException {
        IOException suppressed = null;
        try {
            indexWriter.close();
        } catch (IOException e) {
            suppressed = e;
        }
        try {
            indexReader.close();
        } catch (IOException e) {
            if (suppressed != null) {
                suppressed.addSuppressed(e);
            } else {
                suppressed = e;
            }
        }
        try {
            directory.close();
        } catch (IOException e) {
            if (suppressed != null) {
                e.addSuppressed(suppressed);
            }
            throw e;
        }
        if (suppressed != null) {
            throw suppressed;
        }
    }

    @Desugar
    record GuideIndexingTask(Guide guide, List<ParsedGuidePage> pendingPages) {}

    @Desugar
    public record SearchResult(ResourceLocation guideId, ResourceLocation pageId, String pageTitle,
        LytFlowContent text) {

        public SearchResult {
            Objects.requireNonNull(guideId, "guideId");
            Objects.requireNonNull(pageId, "pageId");
            Objects.requireNonNull(pageTitle, "pageTitle");
            Objects.requireNonNull(text, "text");
        }
    }
}
