package com.hfstudio.guidenh.guide.compiler;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.bsideup.jabel.Desugar;
import com.hfstudio.guidenh.guide.GuidePage;
import com.hfstudio.guidenh.guide.PageAnchor;
import com.hfstudio.guidenh.guide.PageCollection;
import com.hfstudio.guidenh.guide.color.SymbolicColor;
import com.hfstudio.guidenh.guide.compiler.tags.CsvTableCompiler;
import com.hfstudio.guidenh.guide.document.LytErrorSink;
import com.hfstudio.guidenh.guide.document.block.LytAlertBox;
import com.hfstudio.guidenh.guide.document.block.LytBlock;
import com.hfstudio.guidenh.guide.document.block.LytBlockContainer;
import com.hfstudio.guidenh.guide.document.block.LytCodeBlock;
import com.hfstudio.guidenh.guide.document.block.LytDocument;
import com.hfstudio.guidenh.guide.document.block.LytHeading;
import com.hfstudio.guidenh.guide.document.block.LytImage;
import com.hfstudio.guidenh.guide.document.block.LytList;
import com.hfstudio.guidenh.guide.document.block.LytListItem;
import com.hfstudio.guidenh.guide.document.block.LytMermaidMindmap;
import com.hfstudio.guidenh.guide.document.block.LytParagraph;
import com.hfstudio.guidenh.guide.document.block.LytTaskListItem;
import com.hfstudio.guidenh.guide.document.block.LytThematicBreak;
import com.hfstudio.guidenh.guide.document.block.LytVBox;
import com.hfstudio.guidenh.guide.document.block.table.LytTable;
import com.hfstudio.guidenh.guide.document.flow.LytFlowBreak;
import com.hfstudio.guidenh.guide.document.flow.LytFlowContent;
import com.hfstudio.guidenh.guide.document.flow.LytFlowInlineBlock;
import com.hfstudio.guidenh.guide.document.flow.LytFlowLink;
import com.hfstudio.guidenh.guide.document.flow.LytFlowParent;
import com.hfstudio.guidenh.guide.document.flow.LytFlowSpan;
import com.hfstudio.guidenh.guide.document.flow.LytFlowText;
import com.hfstudio.guidenh.guide.document.interaction.TextTooltip;
import com.hfstudio.guidenh.guide.extensions.Extension;
import com.hfstudio.guidenh.guide.extensions.ExtensionCollection;
import com.hfstudio.guidenh.guide.extensions.ExtensionPoint;
import com.hfstudio.guidenh.guide.indices.PageIndex;
import com.hfstudio.guidenh.guide.internal.csv.CsvTableParser;
import com.hfstudio.guidenh.guide.internal.markdown.CodeBlockLanguage;
import com.hfstudio.guidenh.guide.internal.markdown.CodeBlockLanguageDetector;
import com.hfstudio.guidenh.guide.internal.markdown.FootnotePreprocessor;
import com.hfstudio.guidenh.guide.internal.markdown.MarkdownHtmlRuntimeNormalizer;
import com.hfstudio.guidenh.guide.internal.markdown.MarkdownListSemantics;
import com.hfstudio.guidenh.guide.internal.markdown.MarkdownLiteralAutolink;
import com.hfstudio.guidenh.guide.internal.markdown.MarkdownRuntimeBlocks;
import com.hfstudio.guidenh.guide.internal.mermaid.MermaidMindmapParser;
import com.hfstudio.guidenh.guide.style.BorderStyle;
import com.hfstudio.guidenh.guide.style.TextAlignment;
import com.hfstudio.guidenh.guide.style.WhiteSpaceMode;
import com.hfstudio.guidenh.libs.mdast.MdAst;
import com.hfstudio.guidenh.libs.mdast.MdAstYamlFrontmatter;
import com.hfstudio.guidenh.libs.mdast.MdastOptions;
import com.hfstudio.guidenh.libs.mdast.gfm.model.GfmTable;
import com.hfstudio.guidenh.libs.mdast.gfmstrikethrough.MdAstDelete;
import com.hfstudio.guidenh.libs.mdast.mdx.model.MdxJsxElementFields;
import com.hfstudio.guidenh.libs.mdast.mdx.model.MdxJsxFlowElement;
import com.hfstudio.guidenh.libs.mdast.mdx.model.MdxJsxTextElement;
import com.hfstudio.guidenh.libs.mdast.model.MdAstAnyContent;
import com.hfstudio.guidenh.libs.mdast.model.MdAstBlockquote;
import com.hfstudio.guidenh.libs.mdast.model.MdAstBreak;
import com.hfstudio.guidenh.libs.mdast.model.MdAstCode;
import com.hfstudio.guidenh.libs.mdast.model.MdAstDefinition;
import com.hfstudio.guidenh.libs.mdast.model.MdAstEmphasis;
import com.hfstudio.guidenh.libs.mdast.model.MdAstHTML;
import com.hfstudio.guidenh.libs.mdast.model.MdAstHeading;
import com.hfstudio.guidenh.libs.mdast.model.MdAstImage;
import com.hfstudio.guidenh.libs.mdast.model.MdAstImageReference;
import com.hfstudio.guidenh.libs.mdast.model.MdAstInlineCode;
import com.hfstudio.guidenh.libs.mdast.model.MdAstLink;
import com.hfstudio.guidenh.libs.mdast.model.MdAstLinkReference;
import com.hfstudio.guidenh.libs.mdast.model.MdAstList;
import com.hfstudio.guidenh.libs.mdast.model.MdAstListItem;
import com.hfstudio.guidenh.libs.mdast.model.MdAstNode;
import com.hfstudio.guidenh.libs.mdast.model.MdAstParagraph;
import com.hfstudio.guidenh.libs.mdast.model.MdAstParent;
import com.hfstudio.guidenh.libs.mdast.model.MdAstPhrasingContent;
import com.hfstudio.guidenh.libs.mdast.model.MdAstPosition;
import com.hfstudio.guidenh.libs.mdast.model.MdAstRoot;
import com.hfstudio.guidenh.libs.mdast.model.MdAstStrong;
import com.hfstudio.guidenh.libs.mdast.model.MdAstText;
import com.hfstudio.guidenh.libs.mdast.model.MdAstThematicBreak;
import com.hfstudio.guidenh.libs.mdx.MdxCommentMasker;
import com.hfstudio.guidenh.libs.micromark.ParseException;
import com.hfstudio.guidenh.libs.unist.UnistNode;
import com.hfstudio.guidenh.libs.unist.UnistPosition;

public class PageCompiler {

    public static final Logger LOG = LoggerFactory.getLogger(PageCompiler.class);

    /**
     * Default gap between block-level elements. Set as margin.
     */
    public static final int DEFAULT_ELEMENT_SPACING = 5;
    public static final MdastOptions PARSE_OPTIONS = GuideMarkdownOptions.runtime();
    private static final Pattern TABLE_ATTRIBUTE_LINE = Pattern.compile("^\\{:\\s*(.+?)\\s*}$");
    private static final Pattern CODEBLOCK_META_HEIGHT = Pattern
        .compile("(^|\\s)height=(\"([^\"]+)\"|'([^']+)'|([^\\s]+))");
    private static final State<List<SourceSlice>> SOURCE_SLICE_STACK = new State<>(
        "source_slice_stack",
        castClass(List.class),
        Collections.emptyList());

    private final PageCollection pages;
    private final ExtensionCollection extensions;
    private final String sourcePack;
    private final ResourceLocation pageId;
    private final String pageContent;
    private final Map<String, MdAstDefinition> definitions = new HashMap<>();

    private final Map<String, TagCompiler> tagCompilers = new HashMap<>();

    // Data associated with the current page being compiled, this is used by
    // compilers to communicate with each other within the current page.
    private final Map<State<?>, Object> compilerState = new IdentityHashMap<>();

    public PageCompiler(PageCollection pages, ExtensionCollection extensions, String sourcePack,
        ResourceLocation pageId, String pageContent) {
        this.pages = pages;
        this.extensions = extensions;
        this.sourcePack = sourcePack;
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
        // Normalize line ending
        pageContent = normalizeLineEndings(pageContent);
        pageContent = FootnotePreprocessor.preprocess(pageContent);
        var sourceFrontmatter = parseFrontmatterFromSource(id, pageContent);
        String parseContent = MdxCommentMasker.mask(pageContent);

        MdAstRoot astRoot;
        String parseFailureMessage = null;
        try {
            astRoot = MdAst.fromMarkdown(parseContent, PARSE_OPTIONS);
            MarkdownHtmlRuntimeNormalizer.normalize(astRoot);
        } catch (ParseException e) {
            var position = "";
            if (e.getFrom() != null) {
                position = " at line " + e.getFrom()
                    .line()
                    + " column "
                    + e.getFrom()
                        .column();
            }
            var errorMessage = String.format(
                Locale.ROOT,
                "Failed to parse GuideME page %s (lang: %s)%s from resource pack %s",
                id,
                language,
                position,
                sourcePack);
            LOG.error("{}", errorMessage, e);
            parseFailureMessage = errorMessage + ": \n" + e;
            astRoot = buildErrorPage(parseFailureMessage);
        }

        // Find front-matter
        var frontmatter = parseFrontmatter(id, astRoot);
        if (parseFailureMessage != null && sourceFrontmatter.navigationEntry() != null) {
            frontmatter = sourceFrontmatter;
        }

        return new ParsedGuidePage(sourcePack, id, pageContent, astRoot, frontmatter, language, parseFailureMessage);
    }

    public static String normalizeLineEndings(String pageContent) {
        int firstCarriageReturn = pageContent.indexOf('\r');
        if (firstCarriageReturn == -1) {
            return pageContent;
        }

        StringBuilder normalized = new StringBuilder(pageContent.length());
        normalized.append(pageContent, 0, firstCarriageReturn);
        for (int i = firstCarriageReturn; i < pageContent.length(); i++) {
            char currentChar = pageContent.charAt(i);
            if (currentChar == '\r') {
                normalized.append('\n');
                if (i + 1 < pageContent.length() && pageContent.charAt(i + 1) == '\n') {
                    i++;
                }
            } else {
                normalized.append(currentChar);
            }
        }
        return normalized.toString();
    }

    public static MdAstRoot buildErrorPage(String errorText) {
        return buildErrorPage("PARSING ERROR", errorText);
    }

    public static MdAstRoot buildErrorPage(String headingText, String errorText) {
        var root = new MdAstRoot();

        var heading = new MdAstHeading();
        root.addChild(heading);

        heading.depth = 1;
        var headingTextNode = new MdAstText();
        headingTextNode.setValue(headingText);
        heading.addChild(headingTextNode);

        var errorParagraph = new MdAstParagraph();
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
        return new GuidePage(sourcePack, id, document);
    }

    public static GuidePage compile(PageCollection pages, ExtensionCollection extensions, ParsedGuidePage parsedPage) {
        // Translate page tree over to layout pages
        var document = new PageCompiler(pages, extensions, parsedPage.sourcePack, parsedPage.id, parsedPage.source)
            .compile(parsedPage.astRoot);

        return new GuidePage(parsedPage.sourcePack, parsedPage.id, document);
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
                    LOG.error("Found more than one frontmatter!"); // TODO: proper debugging
                    continue;
                }
                try {
                    result = Frontmatter.parse(pageId, frontmatter.value);
                } catch (Exception e) {
                    LOG.error("Failed to parse frontmatter for page {}", pageId, e);
                    break;
                }
            }
        }

        return result != null ? result : new Frontmatter(null, Collections.emptyMap());
    }

    public static Frontmatter parseFrontmatterFromSource(ResourceLocation pageId, String pageContent) {
        var yamlText = extractFrontmatterText(pageContent);
        if (yamlText == null) {
            return new Frontmatter(null, Collections.emptyMap());
        }

        try {
            return Frontmatter.parse(pageId, yamlText);
        } catch (Exception e) {
            LOG.error("Failed to parse frontmatter for page {}", pageId, e);
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
        BlockTagChildSource reparsed = extractBlockTagChildrenSource(element);
        if (reparsed == null) {
            compileBlockContextInSourceContext(element.children(), layoutParent);
            return;
        }

        ParsedGuidePage parsed = parse(sourcePack, "en_us", pageId, reparsed.source());
        Map<String, MdAstDefinition> previousDefinitions = new HashMap<>(definitions);
        definitions.putAll(GuideMarkdownDefinitions.collect(parsed.getAstRoot()));
        try {
            withSourceSlice(reparsed.source(), () -> compileBlockContext(parsed.getAstRoot(), layoutParent));
        } finally {
            definitions.clear();
            definitions.putAll(previousDefinitions);
        }
    }

    public List<? extends MdAstAnyContent> reparseBlockTagChildren(MdxJsxElementFields element) {
        BlockTagChildSource reparsed = extractBlockTagChildrenSource(element);
        if (reparsed == null) {
            return element.children();
        }
        ParsedGuidePage parsed = parse(sourcePack, "en_us", pageId, reparsed.source());
        return parsed.getAstRoot()
            .children();
    }

    public void compileBlockContextInSourceContext(List<? extends MdAstAnyContent> children,
        LytBlockContainer layoutParent) {
        withChildrenSourceContext(children, () -> compileBlockContext(children, layoutParent));
    }

    public void withBlockTagChildrenSourceContext(MdxJsxElementFields element, Runnable action) {
        BlockTagChildSource reparsed = extractBlockTagChildrenSource(element);
        if (reparsed != null) {
            withSourceSlice(reparsed.source(), action);
            return;
        }
        withChildrenSourceContext(element.children(), action);
    }

    public void compileBlockContext(List<? extends MdAstAnyContent> children, LytBlockContainer layoutParent) {
        LytBlock previousLayoutChild = null;
        for (int i = 0; i < children.size(); i++) {
            var child = children.get(i);
            LytBlock layoutChild;
            if (child instanceof MdAstThematicBreak) {
                layoutChild = new LytThematicBreak();
            } else if (child instanceof MdAstList astList) {
                layoutChild = compileList(astList);
            } else if (child instanceof MdAstCode astCode) {
                layoutChild = compileCodeBlock(astCode);
            } else if (child instanceof MdAstHeading astHeading) {
                var heading = new LytHeading();
                heading.setDepth(astHeading.depth);
                compileFlowContext(astHeading, heading);
                layoutChild = heading;
            } else if (child instanceof MdAstBlockquote astBlockquote) {
                layoutChild = compileBlockquote(astBlockquote);
            } else if (child instanceof MdAstParagraph astParagraph) {
                var paragraph = new LytParagraph();
                compileFlowContext(astParagraph, paragraph);
                paragraph.setMarginTop(DEFAULT_ELEMENT_SPACING);
                paragraph.setMarginBottom(DEFAULT_ELEMENT_SPACING);
                layoutChild = paragraph;
            } else if (child instanceof MdAstYamlFrontmatter) {
                // This is handled by compile directly
                layoutChild = null;
            } else if (child instanceof MdAstDefinition) {
                layoutChild = null;
            } else if (child instanceof GfmTable astTable) {
                MarkdownTableMeta meta = extractMarkdownTableMeta(children, i + 1);
                layoutChild = compileTable(astTable, meta.widthHints());
                if (meta.consumeChildCount() > 0) {
                    i += meta.consumeChildCount();
                }
            } else if (child instanceof MdAstHTML astHtml) {
                var paragraph = new LytParagraph();
                compileHtmlLiteral(paragraph, astHtml.value);
                layoutChild = paragraph;
            } else if (child instanceof MdxJsxFlowElement el) {
                var compiler = tagCompilers.get(el.name());
                if (compiler == null) {
                    layoutChild = createErrorBlock("Unhandled MDX element in block context", child);
                } else {
                    layoutChild = null;
                    compiler.compileBlockContext(this, layoutParent, el);
                }
            } else if (child instanceof MdAstPhrasingContent phrasingContent) {
                // Wrap in a paragraph with no margins, but try appending to an existing paragraph before this
                if (previousLayoutChild instanceof LytParagraph paragraph) {
                    compileFlowContent(paragraph, phrasingContent);
                    continue;
                } else {
                    var paragraph = new LytParagraph();
                    compileFlowContent(paragraph, phrasingContent);
                    layoutChild = paragraph;
                }
            } else {
                layoutChild = createErrorBlock("Unhandled Markdown node in block context", child);
            }

            if (layoutChild != null) {
                if (child instanceof MdAstNode astNode) {
                    layoutChild.setSourceNode(astNode);
                }
                layoutParent.append(layoutChild);
            }
            previousLayoutChild = layoutChild;
        }
    }

    private LytList compileList(MdAstList astList) {
        var list = new LytList(astList.ordered, astList.start);
        for (var listContent : astList.children()) {
            if (listContent instanceof MdAstListItem astListItem) {
                var taskMarker = MarkdownListSemantics.extractTaskMarker(astListItem.children());
                LytListItem listItem = taskMarker != null ? new LytTaskListItem() : new LytListItem();
                if (listItem instanceof LytTaskListItem taskListItem) {
                    taskListItem.setChecked(taskMarker.checked());
                }
                compileListItem(astListItem, listItem, taskMarker);

                // Fix up top/bottom margin for list item children
                var children = listItem.getChildren();
                if (!children.isEmpty()) {
                    var firstChild = children.get(0);
                    if (firstChild instanceof LytBlock firstBlock) {
                        firstBlock.setMarginTop(0);
                        firstBlock.setMarginBottom(0);
                    }
                }
                list.append(listItem);
            } else {
                list.append(createErrorBlock("Cannot handle list content", listContent));
            }
        }
        return list;
    }

    private LytBlock compileBlockquote(MdAstBlockquote astBlockquote) {
        var alert = MarkdownRuntimeBlocks.extractGithubAlert(astBlockquote);
        if (alert != null) {
            var alertBox = new LytAlertBox();
            alertBox.setTitle(
                alert.type()
                    .displayText(),
                alert.type());
            alertBox.setMarginTop(DEFAULT_ELEMENT_SPACING);
            alertBox.setMarginBottom(DEFAULT_ELEMENT_SPACING);
            compileAlertBody(alert, alertBox);
            normalizeBlockMargins(alertBox);
            return alertBox;
        }

        var blockquote = new LytVBox();
        blockquote.setBackgroundColor(SymbolicColor.BLOCKQUOTE_BACKGROUND);
        blockquote.setPadding(5);
        blockquote.setPaddingLeft(10);
        blockquote.setBorderLeft(new BorderStyle(SymbolicColor.TABLE_BORDER, 2));
        blockquote.setMarginTop(DEFAULT_ELEMENT_SPACING);
        blockquote.setMarginBottom(DEFAULT_ELEMENT_SPACING);
        compileBlockContext(astBlockquote, blockquote);
        normalizeBlockMargins(blockquote);
        shiftFirstParagraphDown(blockquote, 1);
        return blockquote;
    }

    private void normalizeBlockMargins(LytVBox box) {
        var boxChildren = box.getChildren();
        if (!boxChildren.isEmpty()) {
            if (boxChildren.get(0) instanceof LytParagraph firstParagraph) {
                firstParagraph.setMarginTop(0);
            }
            if (boxChildren.get(boxChildren.size() - 1) instanceof LytParagraph lastParagraph) {
                lastParagraph.setMarginBottom(0);
            }
        }
    }

    private void shiftFirstParagraphDown(LytVBox box, int pixels) {
        var boxChildren = box.getChildren();
        if (!boxChildren.isEmpty() && boxChildren.get(0) instanceof LytParagraph firstParagraph) {
            firstParagraph.setPaddingTop(firstParagraph.getPaddingTop() + pixels);
        }
    }

    private void compileListItem(MdAstListItem astListItem, LytListItem listItem,
        @Nullable MarkdownListSemantics.TaskMarker taskMarker) {
        if (taskMarker == null || astListItem.children()
            .isEmpty()
            || !(astListItem.children()
                .get(0) instanceof MdAstParagraph paragraph)) {
            compileBlockContext(astListItem, listItem);
            return;
        }

        var paragraphCopy = cloneParagraphWithLeadingTextOverride(paragraph, taskMarker.remainingText());
        compileParagraphBlock(paragraphCopy, listItem);
        for (int i = 1; i < astListItem.children()
            .size(); i++) {
            var child = astListItem.children()
                .get(i);
            compileBlockContext(Collections.singletonList(child), listItem);
        }
    }

    private void compileAlertBody(MarkdownRuntimeBlocks.GithubAlertBlock alert, LytAlertBox alertBox) {
        List<MdAstAnyContent> children = alert.children();
        if (!children.isEmpty() && children.get(0) instanceof MdAstParagraph paragraph) {
            var firstParagraph = cloneParagraphWithLeadingTextOverride(paragraph, alert.remainingText());
            if (!firstParagraph.children()
                .isEmpty()) {
                compileParagraphBlock(firstParagraph, alertBox);
            }
            for (int i = 1; i < children.size(); i++) {
                compileBlockContext(Collections.singletonList(children.get(i)), alertBox);
            }
            return;
        }
        compileBlockContext(children, alertBox);
    }

    private MdAstParagraph cloneParagraphWithLeadingTextOverride(MdAstParagraph original, String leadingText) {
        MdAstParagraph copy = new MdAstParagraph();
        boolean replaced = false;
        for (var child : original.children()) {
            if (!replaced && child instanceof MdAstText) {
                if (leadingText != null && !leadingText.isEmpty()) {
                    MdAstText text = new MdAstText();
                    text.setValue(leadingText);
                    copy.addChild(text);
                }
                replaced = true;
                continue;
            }
            if (child instanceof MdAstNode astNode) {
                copy.addChild(astNode);
            }
        }
        return copy;
    }

    private void compileParagraphBlock(MdAstParagraph astParagraph, LytBlockContainer parent) {
        var paragraph = new LytParagraph();
        compileFlowContext(astParagraph, paragraph);
        paragraph.setMarginTop(DEFAULT_ELEMENT_SPACING);
        paragraph.setMarginBottom(DEFAULT_ELEMENT_SPACING);
        if (astParagraph.children()
            .isEmpty() && paragraph.isEmpty()) {
            return;
        }
        parent.append(paragraph);
    }

    private LytBlock compileTable(GfmTable astTable, List<Integer> widthHints) {
        var table = new LytTable();
        table.setMarginBottom(DEFAULT_ELEMENT_SPACING);

        boolean firstRow = true;
        int rowIndex = 0;
        for (var astRow : astTable.children()) {
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

        return table;
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
        LytFlowContent layoutChild;
        if (content instanceof MdAstText astText) {
            if (compileLiteralAutolinks(layoutParent, astText.value)) {
                layoutChild = null;
            } else {
                var text = new LytFlowText();
                text.setText(astText.value);
                layoutChild = text;
            }
        } else if (content instanceof MdAstInlineCode astCode) {
            var text = new LytFlowText();
            text.setText(astCode.value);
            text.modifyStyle(
                style -> style.italic(true)
                    .whiteSpace(WhiteSpaceMode.PRE));
            layoutChild = text;
        } else if (content instanceof MdAstStrong astStrong) {
            var span = new LytFlowSpan();
            span.modifyStyle(style -> style.bold(true));
            compileFlowContext(astStrong, span);
            layoutChild = span;
        } else if (content instanceof MdAstEmphasis astEmphasis) {
            var span = new LytFlowSpan();
            span.modifyStyle(style -> style.italic(true));
            compileFlowContext(astEmphasis, span);
            layoutChild = span;
        } else if (content instanceof MdAstDelete astEmphasis) {
            var span = new LytFlowSpan();
            span.modifyStyle(style -> style.strikethrough(true));
            compileFlowContext(astEmphasis, span);
            layoutChild = span;
        } else if (content instanceof MdAstBreak) {
            layoutChild = new LytFlowBreak();
        } else if (content instanceof MdAstLink astLink) {
            layoutChild = compileLink(astLink, layoutParent);
        } else if (content instanceof MdAstLinkReference astLinkReference) {
            layoutChild = compileLinkReference(astLinkReference, layoutParent);
        } else if (content instanceof MdAstImage astImage) {
            var inlineBlock = new LytFlowInlineBlock();
            inlineBlock.setBlock(compileImage(astImage));
            layoutChild = inlineBlock;
        } else if (content instanceof MdAstImageReference astImageReference) {
            var inlineBlock = new LytFlowInlineBlock();
            inlineBlock.setBlock(compileImageReference(astImageReference));
            layoutChild = inlineBlock;
        } else if (content instanceof MdAstHTML astHtml) {
            layoutChild = compileHtmlInline(astHtml.value);
        } else if (content instanceof MdxJsxTextElement el) {
            var compiler = tagCompilers.get(el.name());
            if (compiler == null) {
                layoutChild = createErrorFlowContent("Unhandled MDX element in flow context", content);
            } else {
                layoutChild = null;
                compiler.compileFlowContext(this, layoutParent, el);
            }
        } else {
            layoutChild = createErrorFlowContent("Unhandled Markdown node in flow context", content);
        }

        if (layoutChild != null) {
            layoutParent.append(layoutChild);
        }
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

    private LytFlowContent compileLink(MdAstLink astLink, LytErrorSink errorSink) {
        var link = new LytFlowLink();
        if (astLink.title != null && !astLink.title.isEmpty()) {
            link.setTooltip(new TextTooltip(astLink.title));
        }
        if (astLink.url != null && !astLink.url.isEmpty()) {
            LinkParser.parseLink(this, astLink.url, new LinkParser.Visitor() {

                @Override
                public void handlePage(PageAnchor page) {
                    link.setPageLink(page);
                }

                @Override
                public void handleExternal(URI uri) {
                    link.setExternalUrl(uri);
                }

                @Override
                public void handleError(String error) {
                    errorSink.appendError(PageCompiler.this, error, astLink);
                }
            });
        }

        compileFlowContext(astLink, link);
        return link;
    }

    private LytFlowContent compileLinkReference(MdAstLinkReference astLinkReference, LytErrorSink errorSink) {
        MdAstDefinition definition = GuideMarkdownDefinitions.find(definitions, astLinkReference.identifier);
        if (definition == null) {
            return createErrorFlowContent("Missing link reference definition", astLinkReference);
        }

        MdAstLink link = new MdAstLink();
        link.url = definition.url;
        link.title = definition.title;
        for (var child : astLinkReference.children()) {
            if (child instanceof MdAstNode astChild) {
                link.addChild(astChild);
            }
        }
        return compileLink(link, errorSink);
    }

    private LytImage compileImageReference(MdAstImageReference astImageReference) {
        MdAstDefinition definition = GuideMarkdownDefinitions.find(definitions, astImageReference.identifier);
        if (definition == null) {
            LytImage image = new LytImage();
            image.setAlt(astImageReference.alt);
            image.setTitle("Missing image reference: " + astImageReference.identifier);
            return image;
        }

        MdAstImage image = new MdAstImage();
        image.setAlt(astImageReference.alt);
        image.setTitle(definition.title);
        image.setUrl(definition.url);
        return compileImage(image);
    }

    private LytBlock compileCodeBlock(MdAstCode astCode) {
        CodeBlockLanguage language = CodeBlockLanguageDetector.detect(astCode.lang, astCode.value);
        if (shouldRenderCsvTable(astCode, language)) {
            return compileCsvCodeBlock(astCode);
        }
        if ("mermaid".equals(language.id())) {
            LytMermaidMindmap mermaidBlock = tryCompileMermaidMindmap(astCode.value);
            if (mermaidBlock != null) {
                return mermaidBlock;
            }
        }

        LytCodeBlock codeBlock = new LytCodeBlock();
        codeBlock.setLanguageFenceName(astCode.lang != null ? astCode.lang : language.id());
        codeBlock.applyLanguage(language);
        codeBlock.setCodeText(astCode.value);
        Integer forcedHeight = parseCodeBlockHeight(astCode.meta);
        if (forcedHeight != null) {
            codeBlock.setForcedBodyHeight(forcedHeight);
        }
        return codeBlock;
    }

    private boolean shouldRenderCsvTable(MdAstCode astCode, CodeBlockLanguage language) {
        return astCode.lang != null && "csv".equals(language.id());
    }

    private LytBlock compileCsvCodeBlock(MdAstCode astCode) {
        String source = astCode.value;
        List<List<String>> rows = CsvTableParser.parse(source);
        if (rows.isEmpty()) {
            LytCodeBlock codeBlock = new LytCodeBlock();
            codeBlock.setLanguageFenceName("csv");
            codeBlock.applyLanguage(new CodeBlockLanguage("csv", "CSV"));
            codeBlock.setCodeText(source);
            return codeBlock;
        }

        CsvFenceMeta meta = parseCsvFenceMeta(astCode.meta);
        return CsvTableCompiler.buildTable(rows, meta.header(), meta.widthHints());
    }

    private CsvFenceMeta parseCsvFenceMeta(@Nullable String meta) {
        if (meta == null || meta.trim()
            .isEmpty()) {
            return new CsvFenceMeta(true, Collections.emptyList());
        }

        boolean header = true;
        List<Integer> widthHints = Collections.emptyList();
        for (String token : splitMetaTokens(meta)) {
            int equalsIndex = token.indexOf('=');
            if (equalsIndex <= 0 || equalsIndex == token.length() - 1) {
                continue;
            }

            String key = token.substring(0, equalsIndex);
            String value = stripOptionalQuotes(token.substring(equalsIndex + 1));
            if ("widths".equals(key)) {
                widthHints = CsvTableCompiler.parseWidthHints(value);
            } else if ("header".equals(key)) {
                header = !"false".equalsIgnoreCase(value);
            }
        }

        return new CsvFenceMeta(header, widthHints);
    }

    private MarkdownTableMeta extractMarkdownTableMeta(List<? extends MdAstAnyContent> children, int startIndex) {
        if (startIndex >= children.size()) {
            return extractMarkdownTableMetaFromSource(children, startIndex);
        }

        StringBuilder metaExpression = new StringBuilder();
        int consumed = 0;
        for (int index = startIndex; index < children.size(); index++) {
            MdAstAnyContent child = children.get(index);
            if (!(child instanceof MdAstParagraph paragraph)) {
                break;
            }

            String attributeText = getParagraphTextValue(paragraph);
            if (attributeText == null) {
                break;
            }

            Matcher matcher = TABLE_ATTRIBUTE_LINE.matcher(attributeText.trim());
            if (!matcher.matches()) {
                break;
            }

            if (metaExpression.length() > 0) {
                metaExpression.append(' ');
            }
            metaExpression.append(matcher.group(1));
            consumed++;
        }

        if (consumed == 0) {
            return extractMarkdownTableMetaFromSource(children, startIndex);
        }

        List<Integer> widthHints = parseWidthHintsFromMetaExpression(metaExpression.toString());
        if (widthHints.isEmpty()) {
            return new MarkdownTableMeta(Collections.emptyList(), consumed);
        }

        return new MarkdownTableMeta(widthHints, consumed);
    }

    private MarkdownTableMeta extractMarkdownTableMetaFromSource(List<? extends MdAstAnyContent> children,
        int startIndex) {
        if (startIndex <= 0 || startIndex > children.size()) {
            return new MarkdownTableMeta(Collections.emptyList(), 0);
        }

        MdAstAnyContent tableChild = children.get(startIndex - 1);
        if (!(tableChild instanceof MdAstNode tableNode) || tableNode.position() == null
            || tableNode.position()
                .end() == null) {
            return new MarkdownTableMeta(Collections.emptyList(), 0);
        }

        int endLine = tableNode.position()
            .end()
            .line();
        String sourceText = getCurrentSourceText();
        String[] lines = sourceText.split("\n", -1);
        if (endLine <= 0 || endLine > lines.length) {
            return new MarkdownTableMeta(Collections.emptyList(), 0);
        }

        for (int lineIndex = Math.max(0, endLine - 1); lineIndex < lines.length; lineIndex++) {
            String attributeLine = lines[lineIndex].trim();
            if (attributeLine.isEmpty()) {
                continue;
            }
            Matcher matcher = TABLE_ATTRIBUTE_LINE.matcher(attributeLine);
            if (matcher.matches()) {
                List<Integer> widthHints = parseWidthHintsFromMetaExpression(matcher.group(1));
                return new MarkdownTableMeta(widthHints, 0);
            }
            break;
        }
        return new MarkdownTableMeta(Collections.emptyList(), 0);
    }

    private @Nullable String getParagraphTextValue(MdAstParagraph paragraph) {
        String text = paragraph.toText();
        return text.isEmpty() ? null : text;
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
        List<String> tokens = new java.util.ArrayList<>();
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

    private @Nullable Integer parseCodeBlockHeight(@Nullable String meta) {
        if (meta == null || meta.trim()
            .isEmpty()) {
            return null;
        }
        Matcher matcher = CODEBLOCK_META_HEIGHT.matcher(meta);
        if (!matcher.find()) {
            return null;
        }
        String value = matcher.group(3) != null ? matcher.group(3)
            : matcher.group(4) != null ? matcher.group(4) : matcher.group(5);
        if (value == null || value.trim()
            .isEmpty()) {
            return null;
        }
        try {
            return Math.max(0, Integer.parseInt(value.trim()));
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private @Nullable BlockTagChildSource extractBlockTagChildrenSource(MdxJsxElementFields element) {
        UnistPosition position = element.position();
        if (position == null || position.start() == null || position.end() == null) {
            return null;
        }

        int sourceStart = position.start()
            .offset();
        int sourceEnd = position.end()
            .offset();
        String sourceText = getCurrentSourceText();
        if (sourceStart < 0 || sourceEnd <= sourceStart || sourceEnd > sourceText.length()) {
            return null;
        }

        String raw = sourceText.substring(sourceStart, sourceEnd);
        int openingTagEnd = raw.indexOf('>');
        int closingTagStart = raw.lastIndexOf("</");
        if (openingTagEnd < 0 || closingTagStart < 0 || closingTagStart <= openingTagEnd) {
            return null;
        }

        return new BlockTagChildSource(dedentBlockTagBody(raw.substring(openingTagEnd + 1, closingTagStart)));
    }

    private String dedentBlockTagBody(String body) {
        String normalized = normalizeLineEndings(body);
        if (normalized.isEmpty()) {
            return normalized;
        }

        String[] lines = normalized.split("\n", -1);
        int firstContentLine = 0;
        while (firstContentLine < lines.length && lines[firstContentLine].trim()
            .isEmpty()) {
            firstContentLine++;
        }

        int minIndent = Integer.MAX_VALUE;
        for (int i = firstContentLine; i < lines.length; i++) {
            String line = lines[i];
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
        for (int i = firstContentLine; i < lines.length; i++) {
            if (i > firstContentLine) {
                result.append('\n');
            }
            result.append(removeLeadingWhitespace(lines[i], minIndent));
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

    private @Nullable LytMermaidMindmap tryCompileMermaidMindmap(String source) {
        try {
            String normalized = MermaidMindmapParser.normalize(source);
            LytMermaidMindmap block = new LytMermaidMindmap(MermaidMindmapParser.parse(normalized), normalized);
            LOG.info("Compiled fenced Mermaid runtime block for page {} ({} chars)", pageId, normalized.length());
            return block;
        } catch (IllegalArgumentException e) {
            LOG.warn("Failed to compile fenced Mermaid runtime block for page {} from source: {}", pageId, source, e);
            return null;
        }
    }

    private void compileHtmlLiteral(LytParagraph paragraph, String html) {
        String stripped = stripHtmlTags(html);
        if (stripped.isEmpty()) {
            paragraph.appendText(html);
        } else {
            paragraph.appendText(stripped);
        }
        paragraph.setMarginTop(DEFAULT_ELEMENT_SPACING);
        paragraph.setMarginBottom(DEFAULT_ELEMENT_SPACING);
    }

    private LytFlowContent compileHtmlInline(String html) {
        LytFlowText text = new LytFlowText();
        text.setText(stripHtmlTags(html));
        return text;
    }

    private String stripHtmlTags(String html) {
        if (html == null || html.isEmpty()) {
            return "";
        }

        StringBuilder stripped = new StringBuilder(html.length());
        boolean inTag = false;
        for (int i = 0; i < html.length(); i++) {
            char current = html.charAt(i);
            if (current == '<') {
                inTag = true;
                continue;
            }
            if (current == '>') {
                inTag = false;
                continue;
            }
            if (!inTag) {
                stripped.append(current);
            }
        }
        return stripped.toString();
    }

    private LytImage compileImage(MdAstImage astImage) {
        var image = new LytImage();
        image.setTitle(astImage.title);
        image.setAlt(astImage.alt);
        try {
            var imageId = IdUtils.resolveLink(astImage.url, pageId);
            var imageContent = pages.loadAsset(imageId);
            if (imageContent == null) {
                LOG.error("Couldn't find image {}", astImage.url);
                image.setTitle("Missing image: " + astImage.url);
            }
            image.setImage(imageId, imageContent);
        } catch (IllegalArgumentException e) {
            LOG.error("Invalid image id: {}", astImage.url);
            image.setTitle("Invalid image URL: " + astImage.url);
        }
        return image;
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

            LOG.warn("{}\n{}\n{}\n", text, line, tildes + "^");
        } else {
            LOG.warn("{}\n", text);
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

    public PageCollection getPageCollection() {
        return pages;
    }

    public @Nullable byte[] loadAsset(ResourceLocation imageId) {
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
            UnistNode node = child;
            UnistPosition position = node.position();
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
    private record CsvFenceMeta(boolean header, List<Integer> widthHints) {}

    @Desugar
    private record MarkdownTableMeta(List<Integer> widthHints, int consumeChildCount) {}

    @Desugar
    private record BlockTagChildSource(String source) {}

    @Desugar
    private record SourceSlice(String source) {}

    @Desugar
    public record State<T> (String name, Class<T> dataClass, T defaultValue) {}
}
