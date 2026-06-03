package com.hfstudio.guidenh.guide.compiler;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.minecraft.util.ResourceLocation;

import org.jetbrains.annotations.Nullable;

import com.github.bsideup.jabel.Desugar;
import com.hfstudio.guidenh.guide.GuidePage;
import com.hfstudio.guidenh.guide.PageCollection;
import com.hfstudio.guidenh.guide.color.SymbolicColor;
import com.hfstudio.guidenh.guide.compiler.tags.CsvTableCompiler;
import com.hfstudio.guidenh.guide.document.block.LatexRenderOptions;
import com.hfstudio.guidenh.guide.document.block.LatexVerticalAlign;
import com.hfstudio.guidenh.guide.document.block.LytBlock;
import com.hfstudio.guidenh.guide.document.block.LytBlockContainer;
import com.hfstudio.guidenh.guide.document.block.LytDocument;
import com.hfstudio.guidenh.guide.document.block.LytDocumentFloat;
import com.hfstudio.guidenh.guide.document.block.LytFloatAwareBlock;
import com.hfstudio.guidenh.guide.document.block.LytHeading;
import com.hfstudio.guidenh.guide.document.block.LytLatexBlock;
import com.hfstudio.guidenh.guide.document.block.LytLatexDisplayBlock;
import com.hfstudio.guidenh.guide.document.block.LytParagraph;
import com.hfstudio.guidenh.guide.document.block.table.LytTable;
import com.hfstudio.guidenh.guide.document.flow.LytFlowContent;
import com.hfstudio.guidenh.guide.document.flow.LytFlowInlineBlock;
import com.hfstudio.guidenh.guide.document.flow.LytFlowLink;
import com.hfstudio.guidenh.guide.document.flow.LytFlowParent;
import com.hfstudio.guidenh.guide.document.flow.LytFlowSpan;
import com.hfstudio.guidenh.guide.document.flow.LytFlowText;
import com.hfstudio.guidenh.guide.extensions.Extension;
import com.hfstudio.guidenh.guide.extensions.ExtensionCollection;
import com.hfstudio.guidenh.guide.extensions.ExtensionPoint;
import com.hfstudio.guidenh.guide.indices.PageIndex;
import com.hfstudio.guidenh.guide.internal.GuideRegistry;
import com.hfstudio.guidenh.guide.internal.markdown.FootnotePreprocessor;
import com.hfstudio.guidenh.guide.internal.markdown.MarkdownActionLink;
import com.hfstudio.guidenh.guide.internal.markdown.MarkdownHtmlRuntimeNormalizer;
import com.hfstudio.guidenh.guide.internal.markdown.MarkdownLatexShorthand;
import com.hfstudio.guidenh.guide.internal.markdown.MarkdownLiteralAutolink;
import com.hfstudio.guidenh.guide.internal.markdown.MdAstToMdxConverter;
import com.hfstudio.guidenh.guide.internal.util.GuideStringLines;
import com.hfstudio.guidenh.guide.internal.util.LangUtil;
import com.hfstudio.guidenh.guide.sound.GuideSoundParsers;
import com.hfstudio.guidenh.guide.style.TextAlignment;
import com.hfstudio.guidenh.guide.style.WhiteSpaceMode;
import com.hfstudio.guidenh.libs.mdast.MdAst;
import com.hfstudio.guidenh.libs.mdast.MdAstYamlFrontmatter;
import com.hfstudio.guidenh.libs.mdast.MdastOptions;
import com.hfstudio.guidenh.libs.mdast.gfm.model.GfmTable;
import com.hfstudio.guidenh.libs.mdast.gfm.model.GfmTableRow;
import com.hfstudio.guidenh.libs.mdast.mdx.model.MdxJsxElementFields;
import com.hfstudio.guidenh.libs.mdast.mdx.model.MdxJsxFlowElement;
import com.hfstudio.guidenh.libs.mdast.mdx.model.MdxJsxTextElement;
import com.hfstudio.guidenh.libs.mdast.model.MdAstAnyContent;
import com.hfstudio.guidenh.libs.mdast.model.MdAstDefinition;
import com.hfstudio.guidenh.libs.mdast.model.MdAstNode;
import com.hfstudio.guidenh.libs.mdast.model.MdAstParagraph;
import com.hfstudio.guidenh.libs.mdast.model.MdAstParent;
import com.hfstudio.guidenh.libs.mdast.model.MdAstPosition;
import com.hfstudio.guidenh.libs.mdast.model.MdAstRoot;
import com.hfstudio.guidenh.libs.mdast.model.MdAstText;
import com.hfstudio.guidenh.libs.mdx.MdxCommentMasker;
import com.hfstudio.guidenh.libs.micromark.ParseException;
import com.hfstudio.guidenh.libs.unist.UnistNode;
import com.hfstudio.guidenh.libs.unist.UnistPoint;
import com.hfstudio.guidenh.libs.unist.UnistPosition;

import cpw.mods.fml.common.FMLLog;

public class PageCompiler {

    /**
     * Default gap between block-level elements. Set as margin.
     */
    public static final int DEFAULT_ELEMENT_SPACING = 5;
    public static final MdastOptions PARSE_OPTIONS = GuideMarkdownOptions.runtime();
    public static final int DEFAULT_MARK_BACKGROUND_COLOR = 0xFF8A6A00;
    private static final Pattern TABLE_ATTRIBUTE_LINE = Pattern.compile("^\\{:\\s*(.+?)\\s*}$");
    private static PageLinkResolver pageLinkResolver = PageCompiler::defaultPageExistsForLink;
    private static final State<List<SourceSlice>> SOURCE_SLICE_STACK = new State<>(
        "source_slice_stack",
        castClass(List.class),
        Collections.emptyList());

    private final PageCollection pages;
    private final ExtensionCollection extensions;
    private final String sourcePack;
    private final String language;
    private final ResourceLocation pageId;
    private final String pageContent;
    private final Map<String, MdAstDefinition> definitions = new HashMap<>();

    private final Map<String, TagCompiler> tagCompilers = new HashMap<>();

    // Data associated with the current page being compiled, this is used by
    // compilers to communicate with each other within the current page.
    private final Map<State<?>, Object> compilerState = new IdentityHashMap<>();
    private final Map<MdxJsxElementFields, BlockTagChildrenCacheEntry> blockTagChildrenCache = new IdentityHashMap<>();
    private final Map<String, ParsedGuidePage> inlineMarkdownParseCache = new HashMap<>();

    public PageCompiler(PageCollection pages, ExtensionCollection extensions, String sourcePack,
        ResourceLocation pageId, String pageContent) {
        this(pages, extensions, sourcePack, LangUtil.ENGLISH_LANGUAGE, pageId, pageContent);
    }

    public PageCompiler(PageCollection pages, ExtensionCollection extensions, String sourcePack, String language,
        ResourceLocation pageId, String pageContent) {
        this.pages = pages;
        this.extensions = extensions;
        this.sourcePack = sourcePack;
        this.language = language;
        this.pageId = pageId;
        this.pageContent = pageContent;

        // Index available tag-compilers
        for (var tagCompiler : extensions.get(TagCompiler.EXTENSION_POINT)) {
            for (String tagName : tagCompiler.getTagNames()) {
                tagCompilers.put(tagName, tagCompiler);
            }
        }
    }

    @Deprecated
    public static ParsedGuidePage parse(String sourcePack, ResourceLocation id, InputStream in) throws IOException {
        return parse(sourcePack, "en_us", id, in);
    }

    public static ParsedGuidePage parse(String sourcePack, String language, ResourceLocation id, InputStream in)
        throws IOException {
        StringBuilder buffer = new StringBuilder();
        char[] chunk = new char[4096];
        try (var reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
            int n;
            while ((n = reader.read(chunk)) != -1) {
                buffer.append(chunk, 0, n);
            }
        }
        return parse(sourcePack, language, id, buffer.toString());
    }

    @Deprecated
    public static ParsedGuidePage parse(String sourcePack, ResourceLocation id, String pageContent) {
        return parse(sourcePack, "en_us", id, pageContent);
    }

    public static ParsedGuidePage parse(String sourcePack, String language, ResourceLocation id, String pageContent) {
        pageContent = pageContent != null ? pageContent : "";
        long parseStartedAt = System.nanoTime();
        long stageStartedAt = parseStartedAt;
        pageContent = normalizeLineEndings(pageContent);
        long normalizeNs = System.nanoTime() - stageStartedAt;

        stageStartedAt = System.nanoTime();
        pageContent = FootnotePreprocessor.preprocess(pageContent);
        long footnoteNs = System.nanoTime() - stageStartedAt;

        stageStartedAt = System.nanoTime();
        var sourceFrontmatter = parseFrontmatterFromSource(id, pageContent);
        long sourceFrontmatterNs = System.nanoTime() - stageStartedAt;

        stageStartedAt = System.nanoTime();
        MarkdownLatexShorthand.MaskResult latexMask = MarkdownLatexShorthand.mask(pageContent);
        long latexMaskNs = System.nanoTime() - stageStartedAt;

        stageStartedAt = System.nanoTime();
        String parseContent = MdxCommentMasker.mask(latexMask.source());
        long commentMaskNs = System.nanoTime() - stageStartedAt;

        MdAstRoot astRoot;
        String parseFailureMessage = null;
        UnistPoint parseFailureFrom = null;
        UnistPoint parseFailureTo = null;
        Frontmatter frontmatter;
        long markdownParseNs = 0L;
        long latexRestoreNs = 0L;
        long htmlNormalizeNs = 0L;
        long mdAstConvertNs = 0L;
        try {
            stageStartedAt = System.nanoTime();
            astRoot = MdAst.fromMarkdown(parseContent, PARSE_OPTIONS);
            markdownParseNs = System.nanoTime() - stageStartedAt;

            stageStartedAt = System.nanoTime();
            MarkdownLatexShorthand.restore(astRoot, latexMask);
            latexRestoreNs = System.nanoTime() - stageStartedAt;

            stageStartedAt = System.nanoTime();
            MarkdownHtmlRuntimeNormalizer.normalize(astRoot);
            htmlNormalizeNs = System.nanoTime() - stageStartedAt;

            // Collect definitions before conversion (converter needs them
            // for link/image reference resolution).
            stageStartedAt = System.nanoTime();
            Map<String, MdAstDefinition> definitions = GuideMarkdownDefinitions.collect(astRoot);

            // Parse frontmatter BEFORE conversion — the converter removes
            // MdAstYamlFrontmatter from children.
            frontmatter = parseFrontmatter(id, astRoot);

            MdAstToMdxConverter.convert(astRoot, definitions);
            mdAstConvertNs = System.nanoTime() - stageStartedAt;
        } catch (RuntimeException t) {
            if (t instanceof ParseException e) {
                markdownParseNs = System.nanoTime() - stageStartedAt;
                parseFailureFrom = e.getFrom();
                parseFailureTo = e.getTo();
            }
            String errorMessage = formatParseFailureMessage(id, language, sourcePack, parseFailureFrom);
            FMLLog.getLogger()
                .error("[GuideNH] [PageCompiler] {}", errorMessage, t);
            parseFailureMessage = errorMessage + ": \n" + t;
            astRoot = buildErrorPage(parseFailureMessage);
            frontmatter = new Frontmatter(null, Collections.emptyMap());
        }

        long astFrontmatterNs = System.nanoTime() - stageStartedAt;
        if (parseFailureMessage != null && sourceFrontmatter.navigationEntry() != null) {
            frontmatter = sourceFrontmatter;
        }

        long totalNs = System.nanoTime() - parseStartedAt;
        FMLLog.getLogger()
            .info(
                "[GuideNH] [PageCompiler] Parsed page {} lang={} totalNs={} normalizeNs={} footnoteNs={} sourceFrontmatterNs={} latexMaskNs={} commentMaskNs={} markdownParseNs={} latexRestoreNs={} htmlNormalizeNs={} mdAstConvertNs={} astFrontmatterNs={} parseFailed={}",
                id,
                language,
                totalNs,
                normalizeNs,
                footnoteNs,
                sourceFrontmatterNs,
                latexMaskNs,
                commentMaskNs,
                markdownParseNs,
                latexRestoreNs,
                htmlNormalizeNs,
                mdAstConvertNs,
                astFrontmatterNs,
                parseFailureMessage != null);

        return new ParsedGuidePage(
            sourcePack,
            id,
            pageContent,
            astRoot,
            frontmatter,
            language,
            parseFailureMessage,
            parseFailureFrom,
            parseFailureTo);
    }

    /**
     * Lightweight parse that extracts only YAML frontmatter from the raw source,
     * deferring the full Micromark → mdast pipeline to first call of
     * {@link ParsedGuidePage#getAstRoot()}.
     *
     * <p>
     * F3+T reload uses this path so that index/navigation rebuilds —
     * which only need frontmatter — complete without paying Micromark cost.
     * </p>
     */
    public static ParsedGuidePage parseFrontmatterOnly(String sourcePack, String language, ResourceLocation id,
        String pageContent) {
        pageContent = pageContent != null ? pageContent : "";
        pageContent = normalizeLineEndings(pageContent);
        var sourceFrontmatter = parseFrontmatterFromSource(id, pageContent);

        return new ParsedGuidePage(
            sourcePack,
            id,
            pageContent,
            null, // astRoot — triggers lazy parse on first getAstRoot()
            sourceFrontmatter,
            language,
            null, // no parse failure yet
            null,
            null);
    }

    public static String normalizeLineEndings(String pageContent) {
        return GuideStringLines.normalizeLineEndings(pageContent);
    }

    private static String formatParseFailureMessage(ResourceLocation id, String language, String sourcePack,
        @Nullable UnistPoint position) {
        String positionText = "";
        if (position != null) {
            positionText = " at line " + position.line() + " column " + position.column();
        }
        return String.format(
            Locale.ROOT,
            "Failed to parse GuideME page %s (lang: %s)%s from resource pack %s",
            id,
            language,
            positionText,
            sourcePack);
    }

    public static MdAstRoot buildErrorPage(String errorText) {
        return buildErrorPage("PARSING ERROR", errorText);
    }

    public static MdAstRoot buildErrorPage(String headingText, String errorText) {
        var root = new MdAstRoot();

        var heading = new MdxJsxFlowElement();
        heading.setName("h1");
        heading.addAttribute("depth", 1);
        root.addChild(heading);
        var headingTextNode = new MdAstText();
        headingTextNode.setValue(headingText);
        heading.addChild(headingTextNode);

        var errorParagraph = new MdxJsxFlowElement();
        errorParagraph.setName("p");
        root.addChild(errorParagraph);
        var errorTextNode = new MdAstText();
        errorTextNode.setValue(errorText);
        errorParagraph.addChild(errorTextNode);

        return root;
    }

    public static GuidePage buildErrorGuidePage(PageCollection pages, ExtensionCollection extensions, String sourcePack,
        ResourceLocation id, String pageContent, String headingText, String errorText) {
        var errorRoot = buildErrorPage(headingText, errorText);
        var document = new PageCompiler(pages, extensions, sourcePack, id, pageContent).compile(errorRoot);
        var titleHeading = extractPageTitleHeading(document);
        return new GuidePage(sourcePack, id, document, titleHeading);
    }

    public static GuidePage compile(PageCollection pages, ExtensionCollection extensions, ParsedGuidePage parsedPage) {
        // Translate page tree over to layout pages
        var document = new PageCompiler(
            pages,
            extensions,
            parsedPage.getSourcePack(),
            parsedPage.getLanguage(),
            parsedPage.getId(),
            parsedPage.getSource()).compile(parsedPage.getAstRoot());
        var titleHeading = extractPageTitleHeading(document);
        FrontmatterPageMeta pageMeta = parsedPage.getFrontmatter() != null ? parsedPage.getFrontmatter()
            .parseMeta() : null;
        if (pageMeta != null && pageMeta.isEmpty()) pageMeta = null;
        return new GuidePage(parsedPage.getSourcePack(), parsedPage.getId(), document, titleHeading, pageMeta);
    }

    /**
     * Finds the first H1 {@link LytHeading} in the compiled document, removes it from the
     * document (so it is not rendered twice inside the content area when displayed in a
     * toolbar), and returns it. Non-heading blocks are skipped during the search. Returns
     * {@code null} when no H1 is present or when the first heading is not H1.
     */
    @Nullable
    private static LytHeading extractPageTitleHeading(LytDocument document) {
        for (var block : new ArrayList<>(document.getBlocks())) {
            if (block instanceof LytHeading heading) {
                if (heading.getDepth() == 1) {
                    document.removeChild(heading);
                    return heading;
                } else {
                    break;
                }
            }
        }
        return null;
    }

    public ExtensionCollection getExtensions() {
        return extensions;
    }

    public <T extends Extension> List<T> getExtensions(ExtensionPoint<T> extensionPoint) {
        return extensions.get(extensionPoint);
    }

    private LytDocument compile(MdAstRoot root) {
        definitions.clear();
        definitions.putAll(GuideMarkdownDefinitions.collect(root));
        var document = new LytDocument();
        document.setSourceNode(root);
        compileBlockContext(root, document);
        return document;
    }

    public static Frontmatter parseFrontmatter(ResourceLocation pageId, MdAstRoot root) {
        Frontmatter result = null;

        for (var child : root.children()) {
            if (child instanceof MdAstYamlFrontmatter frontmatter) {
                if (result != null) {
                    FMLLog.getLogger()
                        .error("[GuideNH] [PageCompiler] Found more than one frontmatter!");
                    continue;
                }
                try {
                    result = Frontmatter.parse(pageId, frontmatter.value);
                } catch (Exception e) {
                    FMLLog.getLogger()
                        .error("[GuideNH] [PageCompiler] Failed to parse frontmatter for page {}", pageId, e);
                    break;
                }
            }
        }

        return result != null ? result : new Frontmatter(null, Collections.emptyMap());
    }

    public static Frontmatter parseFrontmatterFromSource(ResourceLocation pageId, String pageContent) {
        // Strip UTF-8 BOM if present (resource pack files may include it)
        if (pageContent.startsWith("﻿")) {
            pageContent = pageContent.substring(1);
        }
        var yamlText = extractFrontmatterText(pageContent);
        if (yamlText == null) {
            return new Frontmatter(null, Collections.emptyMap());
        }

        try {
            return Frontmatter.parse(pageId, yamlText);
        } catch (Exception e) {
            FMLLog.getLogger()
                .error("[GuideNH] [PageCompiler] Failed to parse frontmatter for page {}", pageId, e);
            return new Frontmatter(null, Collections.emptyMap());
        }
    }

    public static @Nullable String extractFrontmatterText(String pageContent) {
        if (!pageContent.startsWith("---\n")) {
            return null;
        }

        int bodyStart = 4;
        int closingMarker = pageContent.indexOf("\n---\n", bodyStart);
        if (closingMarker >= 0) {
            return pageContent.substring(bodyStart, closingMarker);
        }

        if (pageContent.endsWith("\n---")) {
            return pageContent.substring(bodyStart, pageContent.length() - 4);
        }

        return null;
    }

    public void compileBlockContext(MdAstParent<?> markdownParent, LytBlockContainer layoutParent) {
        compileBlockContext(markdownParent.children(), layoutParent);
    }

    public void compileBlockTagChildren(MdxJsxElementFields element, LytBlockContainer layoutParent) {
        BlockTagChildrenCacheEntry cachedChildren = getBlockTagChildrenCacheEntry(element);
        if (cachedChildren.source() == null || cachedChildren.parsedPage() == null) {
            compileBlockContextInSourceContext(element.children(), layoutParent);
            return;
        }

        Map<String, MdAstDefinition> previousDefinitions = new HashMap<>(definitions);
        definitions.putAll(
            GuideMarkdownDefinitions.collect(
                cachedChildren.parsedPage()
                    .getAstRoot()));
        try {
            withSourceSlice(
                cachedChildren.source(),
                () -> compileBlockContext(
                    cachedChildren.parsedPage()
                        .getAstRoot(),
                    layoutParent));
        } finally {
            definitions.clear();
            definitions.putAll(previousDefinitions);
        }
    }

    public List<? extends MdAstAnyContent> reparseBlockTagChildren(MdxJsxElementFields element) {
        BlockTagChildrenCacheEntry cachedChildren = getBlockTagChildrenCacheEntry(element);
        if (cachedChildren.parsedPage() == null) {
            return element.children();
        }
        return cachedChildren.parsedPage()
            .getAstRoot()
            .children();
    }

    /**
     * Returns the verbatim, dedented source text between the opening and closing tag of a block
     * level MDX element, or {@code null} when the element has no source position information.
     * Useful for tag compilers whose body is parsed by a non-Markdown grammar (file trees, etc.).
     */
    public @Nullable String getBlockTagChildrenSource(MdxJsxElementFields element) {
        BlockTagChildrenCacheEntry cachedChildren = getBlockTagChildrenCacheEntry(element);
        if (cachedChildren.source() != null) {
            return cachedChildren.source();
        }
        return sourceForChildren(element.children());
    }

    /**
     * Parses {@code source} as a standalone markdown fragment and appends the resulting inline
     * (phrasing-level) content of its first paragraph into {@code layoutParent}. Block-level nodes
     * other than the leading paragraph are flattened to their inline content. Used by tag
     * compilers that need to render free-form rich-text fragments.
     */
    public void compileInlineMarkdown(String source, LytFlowParent layoutParent) {
        if (source == null || source.isEmpty()) {
            return;
        }
        ParsedGuidePage parsed = inlineMarkdownParseCache.get(source);
        if (parsed == null) {
            parsed = parse(sourcePack, "en_us", pageId, source);
            inlineMarkdownParseCache.put(source, parsed);
        }
        compileInlineFragment(
            parsed.getAstRoot()
                .children(),
            layoutParent);
    }

    public void compileInlineFragment(Collection<? extends MdAstAnyContent> children, LytFlowParent layoutParent) {
        for (MdAstAnyContent child : children) {
            if (child instanceof MdxJsxFlowElement el && "p".equals(el.name())) {
                compileFlowContext(el, layoutParent);
            } else if (child instanceof MdxJsxFlowElement el) {
                for (var nestedChild : el.children()) {
                    compileFlowContent(layoutParent, nestedChild);
                }
            } else if (child instanceof MdAstParent<?>nestedParent) {
                for (var nestedChild : nestedParent.children()) {
                    compileFlowContent(layoutParent, nestedChild);
                }
            } else {
                compileFlowContent(layoutParent, child);
            }
        }
    }

    public void compileBlockContextInSourceContext(List<? extends MdAstAnyContent> children,
        LytBlockContainer layoutParent) {
        withChildrenSourceContext(children, () -> compileBlockContext(children, layoutParent));
    }

    public void withBlockTagChildrenSourceContext(MdxJsxElementFields element, Runnable action) {
        BlockTagChildrenCacheEntry cachedChildren = getBlockTagChildrenCacheEntry(element);
        if (cachedChildren.source() != null) {
            withSourceSlice(cachedChildren.source(), action);
            return;
        }
        withChildrenSourceContext(element.children(), action);
    }

    public void withSourceContext(String sourceText, Runnable action) {
        if (sourceText == null) {
            action.run();
            return;
        }
        withSourceSlice(sourceText, action);
    }

    public void compileBlockContext(List<? extends MdAstAnyContent> children, LytBlockContainer layoutParent) {
        LytBlock previousLayoutChild = null;
        for (int i = 0; i < children.size(); i++) {
            var child = children.get(i);
            LytBlock layoutChild = null;

            if (child instanceof MdxJsxFlowElement el) {
                // Definition elements are metadata, not rendered
                if ("definition".equals(el.name())) {
                    layoutChild = null;
                } else {
                    var compiler = tagCompilers.get(el.name());
                    if (compiler == null) {
                        layoutChild = createErrorBlock("Unhandled MDX element in block context: " + el.name(), child);
                    } else {
                        layoutChild = null;
                        compiler.compileBlockContext(this, layoutParent, el);
                    }
                }
            } else if (child instanceof MdxJsxTextElement el) {
                // Inline element at block level — merge into previous paragraph when possible
                if (previousLayoutChild instanceof LytParagraph paragraph) {
                    var flowCompiler = tagCompilers.get(el.name());
                    if (flowCompiler != null) {
                        flowCompiler.compileFlowContext(this, paragraph, el);
                    }
                    continue;
                }
                var paragraph = new LytParagraph();
                var flowCompiler = tagCompilers.get(el.name());
                if (flowCompiler != null) {
                    flowCompiler.compileFlowContext(this, paragraph, el);
                }
                layoutChild = paragraph;
            } else if (child instanceof MdAstText text) {
                // Orphan text — merge into previous paragraph when possible
                if (previousLayoutChild instanceof LytParagraph paragraph) {
                    var flowText = new LytFlowText();
                    flowText.setText(text.value);
                    paragraph.append(flowText);
                    continue;
                }
                var paragraph = new LytParagraph();
                var flowText = new LytFlowText();
                flowText.setText(text.value);
                paragraph.append(flowText);
                layoutChild = paragraph;
            } else if (child instanceof MdAstDefinition) {
                layoutChild = null; // handled via <definition> element
            } else {
                layoutChild = createErrorBlock(
                    "Unhandled node in block context: " + child.getClass()
                        .getSimpleName(),
                    child);
            }

            if (layoutChild != null) {
                layoutChild = wrapFloatAwareIfNeeded(layoutChild);
                if (child instanceof MdAstNode astNode) {
                    layoutChild.setSourceNode(astNode);
                }
                layoutParent.append(layoutChild);
            }
            previousLayoutChild = layoutChild;
        }
    }

    private void compileParagraphBlock(MdAstParagraph astParagraph, LytBlockContainer parent) {
        var children = astParagraph.children();
        if (children.size() == 1 && children.get(0) instanceof MdAstText soleText) {
            String formula = MarkdownLatexShorthand.extractSoleDisplayFormula(soleText.value);
            if (formula != null) {
                var displayBlock = new LytLatexDisplayBlock(
                    formula,
                    LatexRenderOptions.builder()
                        .build());
                displayBlock.setMarginTop(DEFAULT_ELEMENT_SPACING);
                displayBlock.setMarginBottom(DEFAULT_ELEMENT_SPACING);
                parent.append(wrapFloatAwareIfNeeded(displayBlock));
                return;
            }
        }
        var paragraph = new LytParagraph();
        compileFlowContext(astParagraph, paragraph);
        paragraph.setMarginTop(DEFAULT_ELEMENT_SPACING);
        paragraph.setMarginBottom(DEFAULT_ELEMENT_SPACING);
        if (astParagraph.children()
            .isEmpty() && paragraph.isEmpty()) {
            return;
        }
        parent.append(wrapFloatAwareIfNeeded(paragraph));
    }

    private LytBlock compileTable(GfmTable astTable, List<Integer> widthHints) {
        var table = new LytTable();
        table.setMarginBottom(DEFAULT_ELEMENT_SPACING);

        var astRows = astTable.children();
        // The GFM table parser swallows a trailing kramdown attribute line such as
        // `{: widths="..." }` as an extra row; drop it during rendering so it does
        // not appear as the last visible row of the table.
        int rowCount = astRows.size();
        if (rowCount > 0) {
            var lastRow = astRows.get(rowCount - 1);
            String lastRowText = getTableRowText(lastRow);
            if (lastRowText != null && TABLE_ATTRIBUTE_LINE.matcher(lastRowText.trim())
                .matches()) {
                if (widthHints == null || widthHints.isEmpty()) {
                    Matcher matcher = TABLE_ATTRIBUTE_LINE.matcher(lastRowText.trim());
                    if (matcher.matches()) {
                        widthHints = parseWidthHintsFromMetaExpression(matcher.group(1));
                    }
                }
                rowCount--;
            }
        }

        boolean firstRow = true;
        int rowIndex = 0;
        for (int rowI = 0; rowI < rowCount; rowI++) {
            var astRow = astRows.get(rowI);
            var row = table.appendRow();
            if (firstRow) {
                row.modifyStyle(style -> style.bold(true));
                firstRow = false;
            }

            var astCells = astRow.children();
            for (int i = 0; i < astCells.size(); i++) {
                if (rowIndex == 0 && i < widthHints.size() && widthHints.get(i) > 0) {
                    table.getOrCreateColumn(i)
                        .setPreferredWidth(widthHints.get(i));
                }
                var cell = row.appendCell();
                // Apply alignment
                if (astTable.align != null && i < astTable.align.size()) {
                    switch (astTable.align.get(i)) {
                        case CENTER -> cell.modifyStyle(style -> style.alignment(TextAlignment.CENTER));
                        case RIGHT -> cell.modifyStyle(style -> style.alignment(TextAlignment.RIGHT));
                    }
                }

                compileBlockContext(astCells.get(i), cell);
            }
            rowIndex++;
        }

        return wrapFloatAwareIfNeeded(table);
    }

    public static LytBlock wrapFloatAwareIfNeeded(LytBlock block) {
        if (block instanceof LytParagraph || block instanceof LytDocumentFloat || block instanceof LytFloatAwareBlock) {
            return block;
        }
        return new LytFloatAwareBlock(block);
    }

    private @Nullable String getTableRowText(GfmTableRow row) {
        StringBuilder sb = new StringBuilder();
        for (var cell : row.children()) {
            if (sb.length() > 0) {
                sb.append(' ');
            }
            sb.append(cell.toText());
        }
        String text = sb.toString();
        return text.isEmpty() ? null : text;
    }

    public void compileFlowContext(MdAstParent<?> markdownParent, LytFlowParent layoutParent) {
        compileFlowContext(markdownParent.children(), layoutParent);
    }

    public void compileFlowContext(Collection<? extends MdAstAnyContent> children, LytFlowParent layoutParent) {
        for (var child : children) {
            compileFlowContent(layoutParent, child);
        }
    }

    private void compileFlowContent(LytFlowParent layoutParent, MdAstAnyContent content) {
        LytFlowContent layoutChild = null;

        if (content instanceof MdAstText astText) {
            if (compileActionLinks(layoutParent, astText.value)) {
                layoutChild = null;
            } else if (compileLiteralAutolinks(layoutParent, astText.value)) {
                layoutChild = null;
            } else if (compileInlineDollarLatex(layoutParent, astText.value)) {
                layoutChild = null;
            } else {
                var text = new LytFlowText();
                text.setText(astText.value);
                layoutChild = text;
            }
        } else if (content instanceof MdxJsxTextElement el) {
            // "span" wraps residual inline HTML — pass through children
            if ("span".equals(el.name())) {
                compileFlowContext(el, layoutParent);
                layoutChild = null;
            } else {
                var compiler = tagCompilers.get(el.name());
                if (compiler == null) {
                    layoutChild = createErrorFlowContent(
                        "Unhandled MDX element in flow context: " + el.name(),
                        content);
                } else {
                    layoutChild = null;
                    compiler.compileFlowContext(this, layoutParent, el);
                }
            }
        } else {
            layoutChild = createErrorFlowContent(
                "Unhandled node in flow context: " + content.getClass()
                    .getSimpleName(),
                content);
        }

        if (layoutChild != null) {
            layoutParent.append(layoutChild);
        }
    }

    private boolean compileActionLinks(LytFlowParent layoutParent, String text) {
        if (!MarkdownActionLink.mayContain(text)) {
            return false;
        }

        List<MarkdownActionLink.Segment> segments = MarkdownActionLink.split(text);
        ArrayList<LytFlowContent> rendered = new ArrayList<>(segments.size());
        boolean foundSoundLink = false;
        for (var segment : segments) {
            if (!segment.isLink()) {
                if (!segment.text()
                    .isEmpty()) {
                    rendered.add(LytFlowText.of(segment.text()));
                }
                continue;
            }

            var sound = GuideSoundParsers.parseActionUri(this, segment.href());
            if (sound == null) {
                rendered.add(LytFlowText.of("&[" + segment.text() + "](" + segment.href() + ")"));
                continue;
            }

            var link = new LytFlowLink();
            link.setClickSoundSpec(sound);
            link.setClickCallback(uiHost -> {});
            link.appendText(segment.text());
            rendered.add(link);
            foundSoundLink = true;
        }
        if (!foundSoundLink) {
            return false;
        }

        for (var content : rendered) {
            layoutParent.append(content);
        }
        return true;
    }

    private boolean compileLiteralAutolinks(LytFlowParent layoutParent, String text) {
        if (!MarkdownLiteralAutolink.mayContainLiteralAutolink(text)) {
            return false;
        }

        List<MarkdownLiteralAutolink.Segment> segments = MarkdownLiteralAutolink.split(text);
        boolean foundLink = false;
        for (var segment : segments) {
            if (segment.isLink()) {
                foundLink = true;
                LytFlowLink link = new LytFlowLink();
                link.appendText(segment.text());
                link.setExternalUrl(MarkdownLiteralAutolink.toUri(segment.href()));
                layoutParent.append(link);
            } else if (!segment.text()
                .isEmpty()) {
                    layoutParent.appendText(segment.text());
                }
        }
        return foundLink;
    }

    private boolean compileInlineDollarLatex(LytFlowParent layoutParent, String text) {
        if (!MarkdownLatexShorthand.mayContain(text)) {
            return false;
        }
        List<MarkdownLatexShorthand.Segment> segments = MarkdownLatexShorthand.split(text);
        boolean foundFormula = false;
        for (var segment : segments) {
            if (segment.isFormula()) {
                foundFormula = true;
                var block = new LytLatexBlock(
                    segment.getValue(),
                    LatexRenderOptions.builder()
                        .valign(LatexVerticalAlign.BASELINE)
                        .build());
                layoutParent.append(LytFlowInlineBlock.of(block));
            } else if (!segment.getValue()
                .isEmpty()) {
                    layoutParent.appendText(segment.getValue());
                }
        }
        return foundFormula;
    }

    private List<Integer> parseWidthHintsFromMetaExpression(String metaExpression) {
        for (String token : splitMetaTokens(metaExpression)) {
            int equalsIndex = token.indexOf('=');
            if (equalsIndex <= 0 || equalsIndex == token.length() - 1) {
                continue;
            }

            String key = token.substring(0, equalsIndex);
            String value = stripOptionalQuotes(token.substring(equalsIndex + 1));
            if ("widths".equals(key)) {
                return CsvTableCompiler.parseWidthHints(value);
            }
        }
        return Collections.emptyList();
    }

    private List<String> splitMetaTokens(String meta) {
        List<String> tokens = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;
        char quote = 0;
        for (int i = 0; i < meta.length(); i++) {
            char ch = meta.charAt(i);
            if ((ch == '"' || ch == '\'') && (!inQuotes || ch == quote)) {
                if (inQuotes && ch == quote) {
                    inQuotes = false;
                    quote = 0;
                } else if (!inQuotes) {
                    inQuotes = true;
                    quote = ch;
                }
                current.append(ch);
                continue;
            }
            if (Character.isWhitespace(ch) && !inQuotes) {
                if (current.length() > 0) {
                    tokens.add(current.toString());
                    current.setLength(0);
                }
                continue;
            }
            current.append(ch);
        }
        if (current.length() > 0) {
            tokens.add(current.toString());
        }
        return tokens;
    }

    private String stripOptionalQuotes(String value) {
        if (value.length() >= 2) {
            char first = value.charAt(0);
            char last = value.charAt(value.length() - 1);
            if ((first == '"' && last == '"') || (first == '\'' && last == '\'')) {
                return value.substring(1, value.length() - 1);
            }
        }
        return value;
    }

    private @Nullable BlockTagChildSource extractBlockTagChildrenSource(MdxJsxElementFields element) {
        String sourceText = getCurrentSourceText();
        String body = MdxBlockTagSourceExtractor.extractRawBody(element, sourceText);
        if (body == null && !Objects.equals(sourceText, pageContent)) {
            body = MdxBlockTagSourceExtractor.extractRawBody(element, pageContent);
        }
        if (body == null) {
            return null;
        }

        return new BlockTagChildSource(dedentBlockTagBody(body));
    }

    private BlockTagChildrenCacheEntry getBlockTagChildrenCacheEntry(MdxJsxElementFields element) {
        BlockTagChildrenCacheEntry cachedEntry = blockTagChildrenCache.get(element);
        if (cachedEntry != null) {
            return cachedEntry;
        }

        BlockTagChildSource extractedSource = extractBlockTagChildrenSource(element);
        if (extractedSource == null) {
            cachedEntry = new BlockTagChildrenCacheEntry(null, null);
        } else {
            cachedEntry = new BlockTagChildrenCacheEntry(
                extractedSource.source(),
                parse(sourcePack, "en_us", pageId, extractedSource.source()));
        }
        blockTagChildrenCache.put(element, cachedEntry);
        return cachedEntry;
    }

    private String dedentBlockTagBody(String body) {
        String normalized = normalizeLineEndings(body);
        if (normalized.isEmpty()) {
            return normalized;
        }

        List<String> lines = GuideStringLines.splitLines(normalized);
        int firstContentLine = 0;
        while (firstContentLine < lines.size() && lines.get(firstContentLine)
            .trim()
            .isEmpty()) {
            firstContentLine++;
        }

        int minIndent = Integer.MAX_VALUE;
        for (int i = firstContentLine; i < lines.size(); i++) {
            String line = lines.get(i);
            if (line.trim()
                .isEmpty()) {
                continue;
            }
            minIndent = Math.min(minIndent, leadingWhitespaceWidth(line));
        }
        if (minIndent == Integer.MAX_VALUE) {
            minIndent = 0;
        }

        StringBuilder result = new StringBuilder(normalized.length());
        for (int i = firstContentLine; i < lines.size(); i++) {
            if (i > firstContentLine) {
                result.append('\n');
            }
            result.append(removeLeadingWhitespace(lines.get(i), minIndent));
        }

        while (result.length() > 0 && result.charAt(result.length() - 1) == '\n') {
            result.setLength(result.length() - 1);
        }
        if (Objects.equals(body, normalized) && body.endsWith("\n")) {
            result.append('\n');
        }
        return result.toString();
    }

    private int leadingWhitespaceWidth(String line) {
        int width = 0;
        for (int i = 0; i < line.length(); i++) {
            char ch = line.charAt(i);
            if (ch == ' ') {
                width++;
            } else if (ch == '\t') {
                width += 4;
            } else {
                break;
            }
        }
        return width;
    }

    private String removeLeadingWhitespace(String line, int widthToRemove) {
        int index = 0;
        int removed = 0;
        while (index < line.length() && removed < widthToRemove) {
            char ch = line.charAt(index);
            if (ch == ' ') {
                removed++;
                index++;
            } else if (ch == '\t') {
                removed += 4;
                index++;
            } else {
                break;
            }
        }
        return line.substring(index);
    }

    public LytBlock createErrorBlock(String text, UnistNode child) {
        var paragraph = new LytParagraph();
        paragraph.append(createErrorFlowContent(text, child));
        return paragraph;
    }

    public LytFlowContent createErrorFlowContent(String text, UnistNode child) {
        LytFlowSpan span = new LytFlowSpan();
        span.modifyStyle(
            style -> style.color(SymbolicColor.ERROR_TEXT)
                .whiteSpace(WhiteSpaceMode.PRE));

        // Find the position in the source
        var position = child.position();
        if (position != null) {
            var pos = position.start();
            String sourceText = getCurrentSourceText();
            var startOfLine = sourceText.lastIndexOf('\n', pos.offset()) + 1;
            var endOfLine = sourceText.indexOf('\n', pos.offset() + 1);
            if (endOfLine == -1) {
                endOfLine = sourceText.length();
            }
            var line = sourceText.substring(startOfLine, endOfLine);

            text += " " + child.type() + " (" + MdAstPosition.stringify(pos) + ")";

            span.appendText(text);
            span.appendBreak();

            span.appendText(line);
            span.appendBreak();

            String tildes = new String(new char[pos.column() - 1]).replace('\0', '~');
            span.appendText(tildes + "^");
            span.appendBreak();

            FMLLog.getLogger()
                .warn("[GuideNH] [PageCompiler] {}\n{}\n{}\n", text, line, tildes + "^");
        } else {
            FMLLog.getLogger()
                .warn("[GuideNH] [PageCompiler] {}\n", text);
        }

        return span;
    }

    public ResourceLocation resolveId(String idText) {
        return IdUtils.resolveId(idText, pageId.getResourceDomain());
    }

    /**
     * Get the current page id.
     */
    public ResourceLocation getPageId() {
        return pageId;
    }

    public ResourceLocation getGuideId() {
        return pages.getId();
    }

    public String getLanguage() {
        return language;
    }

    public String getSourcePack() {
        return sourcePack;
    }

    public PageCollection getPageCollection() {
        return pages;
    }

    public boolean pageExistsForLink(ResourceLocation guideId, ResourceLocation pageId) {
        return pageLinkResolver.pageExists(this, guideId, pageId);
    }

    public static void setPageLinkResolver(PageLinkResolver resolver) {
        pageLinkResolver = Objects.requireNonNull(resolver, "resolver");
    }

    public static void resetPageLinkResolver() {
        pageLinkResolver = PageCompiler::defaultPageExistsForLink;
    }

    private static boolean defaultPageExistsForLink(PageCompiler compiler, ResourceLocation guideId,
        ResourceLocation pageId) {
        PageCollection pages = compiler.getPageCollection();
        if (guideId.equals(pages.getId())) {
            return pages.pageExists(pageId);
        }
        var guide = GuideRegistry.getById(guideId);
        return guide != null && guide.pageExists(pageId);
    }

    public interface PageLinkResolver {

        boolean pageExists(PageCompiler compiler, ResourceLocation guideId, ResourceLocation pageId);
    }

    public byte @Nullable [] loadAsset(ResourceLocation imageId) {
        return pages.loadAsset(imageId);
    }

    public <T extends PageIndex> T getIndex(Class<T> clazz) {
        return pages.getIndex(clazz);
    }

    public <T> T getCompilerState(State<T> state) {
        var current = compilerState.getOrDefault(state, state.defaultValue);
        return state.dataClass.cast(current);
    }

    public <T> void setCompilerState(State<T> state, T value) {
        compilerState.put(state, value);
    }

    public <T> void clearCompilerState(State<T> state) {
        compilerState.remove(state);
    }

    public String getCurrentSourceText() {
        List<SourceSlice> sourceSlices = getCompilerState(SOURCE_SLICE_STACK);
        if (!sourceSlices.isEmpty()) {
            return sourceSlices.get(sourceSlices.size() - 1)
                .source();
        }
        return pageContent;
    }

    public void withChildrenSourceContext(List<? extends MdAstAnyContent> children, Runnable action) {
        String sourceText = sourceForChildren(children);
        if (sourceText == null) {
            action.run();
            return;
        }
        withSourceSlice(sourceText, action);
    }

    private @Nullable String sourceForChildren(List<? extends MdAstAnyContent> children) {
        if (children == null || children.isEmpty()) {
            return null;
        }

        UnistPosition firstPosition = null;
        UnistPosition lastPosition = null;
        for (MdAstAnyContent child : children) {
            UnistPosition position = child.position();
            if (position == null || position.start() == null || position.end() == null) {
                return null;
            }
            if (firstPosition == null) {
                firstPosition = position;
            }
            lastPosition = position;
        }

        if (firstPosition == null || lastPosition == null) {
            return null;
        }

        String sourceText = getCurrentSourceText();
        int sourceStart = firstPosition.start()
            .offset();
        int sourceEnd = lastPosition.end()
            .offset();
        if (sourceStart < 0 || sourceEnd <= sourceStart || sourceEnd > sourceText.length()) {
            return null;
        }
        return sourceText.substring(sourceStart, sourceEnd);
    }

    private void withSourceSlice(String sourceText, Runnable action) {
        List<SourceSlice> currentSlices = getCompilerState(SOURCE_SLICE_STACK);
        List<SourceSlice> nextSlices = new ArrayList<>(currentSlices.size() + 1);
        nextSlices.addAll(currentSlices);
        nextSlices.add(new SourceSlice(sourceText));
        setCompilerState(SOURCE_SLICE_STACK, nextSlices);
        try {
            action.run();
        } finally {
            if (currentSlices.isEmpty()) {
                clearCompilerState(SOURCE_SLICE_STACK);
            } else {
                setCompilerState(SOURCE_SLICE_STACK, currentSlices);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> Class<T> castClass(Class<?> rawClass) {
        return (Class<T>) rawClass;
    }

    @Desugar
    private record BlockTagChildSource(String source) {}

    @Desugar
    private record BlockTagChildrenCacheEntry(@Nullable String source, @Nullable ParsedGuidePage parsedPage) {}

    @Desugar
    private record SourceSlice(String source) {}

    @Desugar
    public record State<T> (String name, Class<T> dataClass, T defaultValue) {}
}
