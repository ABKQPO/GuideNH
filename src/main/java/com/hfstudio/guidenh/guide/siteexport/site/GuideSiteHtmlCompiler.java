package com.hfstudio.guidenh.guide.siteexport.site;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.minecraft.util.ResourceLocation;

import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.guide.color.ColorValue;
import com.hfstudio.guidenh.guide.color.LightDarkMode;
import com.hfstudio.guidenh.guide.compiler.PageCompiler;
import com.hfstudio.guidenh.guide.compiler.ParsedGuidePage;
import com.hfstudio.guidenh.guide.compiler.tags.MdxAttrs;
import com.hfstudio.guidenh.guide.compiler.tags.functiongraph.FunctionGraphFenceParser;
import com.hfstudio.guidenh.guide.document.block.functiongraph.LytFunctionGraph;
import com.hfstudio.guidenh.guide.document.interaction.TextTooltip;
import com.hfstudio.guidenh.guide.internal.markdown.MarkdownActionLink;
import com.hfstudio.guidenh.guide.internal.markdown.MarkdownLatexShorthand;
import com.hfstudio.guidenh.guide.internal.markdown.MarkdownRuntimeBlocks;
import com.hfstudio.guidenh.guide.internal.markdown.MarkdownRuntimeBlocks.BlockquoteDirective;
import com.hfstudio.guidenh.guide.internal.markdown.MarkdownRuntimeBlocks.QuoteIconSpec;
import com.hfstudio.guidenh.guide.internal.mermaid.MermaidMindmapDocument;
import com.hfstudio.guidenh.guide.internal.mermaid.MermaidMindmapParser;
import com.hfstudio.guidenh.guide.sound.GuideSoundSpec;
import com.hfstudio.guidenh.guide.sound.GuideSoundTrigger;
import com.hfstudio.guidenh.libs.mdast.mdx.model.MdxJsxAttribute;
import com.hfstudio.guidenh.libs.mdast.mdx.model.MdxJsxAttributeNode;
import com.hfstudio.guidenh.libs.mdast.mdx.model.MdxJsxElementFields;
import com.hfstudio.guidenh.libs.mdast.mdx.model.MdxJsxFlowElement;
import com.hfstudio.guidenh.libs.mdast.mdx.model.MdxJsxTextElement;
import com.hfstudio.guidenh.libs.mdast.model.MdAstAnyContent;
import com.hfstudio.guidenh.libs.mdast.model.MdAstParent;
import com.hfstudio.guidenh.libs.mdast.model.MdAstText;

public class GuideSiteHtmlCompiler {

    public interface RecipeTagRenderer {

        default String render(MdxJsxElementFields element, String defaultNamespace) {
            String recipeId = element.getAttributeString("id", "");
            String fallbackText = element.getAttributeString("fallbackText", "");
            return render(recipeId, fallbackText, defaultNamespace);
        }

        String render(String recipeId, String fallbackText, String defaultNamespace);
    }

    public interface SceneTagRenderer {

        String render(MdxJsxElementFields element, String defaultNamespace, @Nullable ResourceLocation currentPageId,
            GuideSiteTemplateRegistry templates, @Nullable GuideSiteExportedScene exportedScene);
    }

    public interface ImageResolver {

        String resolve(String rawUrl, @Nullable ResourceLocation currentPageId);
    }

    public interface MdxTagRenderer {

        @Nullable
        String render(MdxJsxElementFields element, String defaultNamespace, @Nullable ResourceLocation currentPageId,
            GuideSiteTemplateRegistry templates, SceneResolver sceneResolver, GuideSiteHtmlCompiler compiler);
    }

    public interface SceneResolver {

        @Nullable
        GuideSiteExportedScene nextScene();
    }

    private final RecipeTagRenderer recipeTagRenderer;
    private final SceneTagRenderer sceneTagRenderer;
    private final ImageResolver imageResolver;
    private final MdxTagRenderer mdxTagRenderer;
    private final GuideSiteLatexExporter latexExporter;
    @Nullable
    private final GuideSitePageAssetExporter assetExporter;

    public GuideSiteHtmlCompiler() {
        this(new GuideSiteRecipeTagRenderer(), passthroughImageResolver(), noopMdxTagRenderer());
    }

    public GuideSiteHtmlCompiler(RecipeTagRenderer recipeTagRenderer) {
        this(recipeTagRenderer, passthroughImageResolver(), noopMdxTagRenderer());
    }

    public GuideSiteHtmlCompiler(RecipeTagRenderer recipeTagRenderer, ImageResolver imageResolver) {
        this(recipeTagRenderer, imageResolver, noopMdxTagRenderer());
    }

    public GuideSiteHtmlCompiler(RecipeTagRenderer recipeTagRenderer, ImageResolver imageResolver,
        MdxTagRenderer mdxTagRenderer) {
        this(recipeTagRenderer, imageResolver, mdxTagRenderer, null);
    }

    public GuideSiteHtmlCompiler(RecipeTagRenderer recipeTagRenderer, ImageResolver imageResolver,
        MdxTagRenderer mdxTagRenderer, @Nullable GuideSiteLatexExporter latexExporter) {
        this(recipeTagRenderer, imageResolver, mdxTagRenderer, latexExporter, null, GuideSiteItemIconResolver.NONE);
    }

    public GuideSiteHtmlCompiler(RecipeTagRenderer recipeTagRenderer, ImageResolver imageResolver,
        MdxTagRenderer mdxTagRenderer, @Nullable GuideSiteLatexExporter latexExporter,
        GuideSiteItemIconResolver itemIconResolver) {
        this(
            recipeTagRenderer,
            new GuideSiteSceneTagRenderer(
                recipeTagRenderer,
                imageResolver,
                mdxTagRenderer,
                latexExporter,
                null,
                itemIconResolver),
            imageResolver,
            mdxTagRenderer,
            latexExporter,
            null);
    }

    public GuideSiteHtmlCompiler(RecipeTagRenderer recipeTagRenderer, ImageResolver imageResolver,
        MdxTagRenderer mdxTagRenderer, @Nullable GuideSiteLatexExporter latexExporter,
        @Nullable GuideSitePageAssetExporter assetExporter) {
        this(
            recipeTagRenderer,
            imageResolver,
            mdxTagRenderer,
            latexExporter,
            assetExporter,
            GuideSiteItemIconResolver.NONE);
    }

    public GuideSiteHtmlCompiler(RecipeTagRenderer recipeTagRenderer, ImageResolver imageResolver,
        MdxTagRenderer mdxTagRenderer, @Nullable GuideSiteLatexExporter latexExporter,
        @Nullable GuideSitePageAssetExporter assetExporter, GuideSiteItemIconResolver itemIconResolver) {
        this(
            recipeTagRenderer,
            new GuideSiteSceneTagRenderer(
                recipeTagRenderer,
                imageResolver,
                mdxTagRenderer,
                latexExporter,
                assetExporter,
                itemIconResolver),
            imageResolver,
            mdxTagRenderer,
            latexExporter,
            assetExporter);
    }

    public GuideSiteHtmlCompiler(RecipeTagRenderer recipeTagRenderer, SceneTagRenderer sceneTagRenderer) {
        this(recipeTagRenderer, sceneTagRenderer, passthroughImageResolver(), noopMdxTagRenderer());
    }

    public GuideSiteHtmlCompiler(RecipeTagRenderer recipeTagRenderer, SceneTagRenderer sceneTagRenderer,
        ImageResolver imageResolver) {
        this(recipeTagRenderer, sceneTagRenderer, imageResolver, noopMdxTagRenderer());
    }

    public GuideSiteHtmlCompiler(RecipeTagRenderer recipeTagRenderer, SceneTagRenderer sceneTagRenderer,
        ImageResolver imageResolver, MdxTagRenderer mdxTagRenderer) {
        this(recipeTagRenderer, sceneTagRenderer, imageResolver, mdxTagRenderer, null);
    }

    public GuideSiteHtmlCompiler(RecipeTagRenderer recipeTagRenderer, SceneTagRenderer sceneTagRenderer,
        ImageResolver imageResolver, MdxTagRenderer mdxTagRenderer, @Nullable GuideSiteLatexExporter latexExporter) {
        this(recipeTagRenderer, sceneTagRenderer, imageResolver, mdxTagRenderer, latexExporter, null);
    }

    public GuideSiteHtmlCompiler(RecipeTagRenderer recipeTagRenderer, SceneTagRenderer sceneTagRenderer,
        ImageResolver imageResolver, MdxTagRenderer mdxTagRenderer, @Nullable GuideSiteLatexExporter latexExporter,
        @Nullable GuideSitePageAssetExporter assetExporter) {
        this.recipeTagRenderer = recipeTagRenderer;
        this.sceneTagRenderer = sceneTagRenderer;
        this.imageResolver = imageResolver;
        this.mdxTagRenderer = mdxTagRenderer;
        this.latexExporter = latexExporter;
        this.assetExporter = assetExporter;
    }

    public String compileBody(ParsedGuidePage parsed, GuideSiteTemplateRegistry templates) {
        return compileBody(parsed, templates, () -> null);
    }

    public String compileBody(ParsedGuidePage parsed, GuideSiteTemplateRegistry templates,
        SceneResolver sceneResolver) {
        return compileChildren(
            parsed.getAstRoot()
                .children(),
            templates,
            parsed.getId()
                .getResourceDomain(),
            parsed.getId(),
            sceneResolver);
    }

    public String compileFragment(List<? extends MdAstAnyContent> children, GuideSiteTemplateRegistry templates,
        String defaultNamespace) {
        return compileFragment(children, templates, defaultNamespace, (ResourceLocation) null);
    }

    public String compileFragment(List<? extends MdAstAnyContent> children, GuideSiteTemplateRegistry templates,
        String defaultNamespace, @Nullable ResourceLocation currentPageId) {
        return compileFragment(children, templates, defaultNamespace, () -> null, currentPageId);
    }

    public String compileFragment(List<? extends MdAstAnyContent> children, GuideSiteTemplateRegistry templates,
        String defaultNamespace, SceneResolver sceneResolver) {
        return compileFragment(children, templates, defaultNamespace, sceneResolver, null);
    }

    public String compileFragment(List<? extends MdAstAnyContent> children, GuideSiteTemplateRegistry templates,
        String defaultNamespace, SceneResolver sceneResolver, @Nullable ResourceLocation currentPageId) {
        return compileChildren(children, templates, defaultNamespace, currentPageId, sceneResolver);
    }

    public String compileInlineFragment(List<? extends MdAstAnyContent> children, GuideSiteTemplateRegistry templates,
        String defaultNamespace, SceneResolver sceneResolver, @Nullable ResourceLocation currentPageId) {
        StringBuilder html = new StringBuilder();
        for (MdAstAnyContent child : children) {
            if (child instanceof MdxJsxFlowElement && "p".equals(((MdxJsxFlowElement) child).name())) {
                html.append(
                    compileChildren(
                        ((MdxJsxFlowElement) child).children(),
                        templates,
                        defaultNamespace,
                        currentPageId,
                        sceneResolver));
            } else if (child instanceof MdxJsxFlowElement) {
                html.append(
                    compileChildren(
                        ((MdxJsxFlowElement) child).children(),
                        templates,
                        defaultNamespace,
                        currentPageId,
                        sceneResolver));
            } else {
                html.append(compileNode(child, templates, defaultNamespace, currentPageId, sceneResolver));
            }
        }
        return html.toString();
    }

    private String compileChildren(List<? extends MdAstAnyContent> children, GuideSiteTemplateRegistry templates,
        String defaultNamespace, @Nullable ResourceLocation currentPageId, SceneResolver sceneResolver) {
        StringBuilder html = new StringBuilder();
        for (MdAstAnyContent child : children) {
            html.append(compileNode(child, templates, defaultNamespace, currentPageId, sceneResolver));
        }
        return html.toString();
    }

    private String compileNode(MdAstAnyContent node, GuideSiteTemplateRegistry templates, String defaultNamespace,
        @Nullable ResourceLocation currentPageId, SceneResolver sceneResolver) {
        if (node instanceof MdAstText) {
            return compileText(((MdAstText) node).value(), templates, defaultNamespace, currentPageId);
        }
        if (node instanceof MdxJsxElementFields) {
            return compileMdxElement(
                (MdxJsxElementFields) node,
                templates,
                defaultNamespace,
                currentPageId,
                sceneResolver);
        }
        if (node instanceof MdAstParent) {
            return compileChildren(
                ((MdAstParent<?>) node).children(),
                templates,
                defaultNamespace,
                currentPageId,
                sceneResolver);
        }
        return "";
    }

    private String compileMdxElement(MdxJsxElementFields el, GuideSiteTemplateRegistry templates,
        String defaultNamespace, @Nullable ResourceLocation currentPageId, SceneResolver sceneResolver) {
        // Block-level elements
        if ("p".equals(el.name())) {
            return compileParagraph(el, templates, defaultNamespace, currentPageId, sceneResolver);
        }
        if (isHeadingName(el.name())) {
            return compileHeadingMdx(el, templates, defaultNamespace, currentPageId, sceneResolver);
        }
        if ("blockquote".equals(el.name())) {
            return compileBlockquoteMdx(el, templates, defaultNamespace, currentPageId, sceneResolver);
        }
        if ("ul".equals(el.name()) || "ol".equals(el.name())) {
            return compileListMdx(el, templates, defaultNamespace, currentPageId, sceneResolver);
        }
        if ("li".equals(el.name())) {
            return compileListItemMdx(el, templates, defaultNamespace, currentPageId, sceneResolver);
        }
        if ("pre".equals(el.name())) {
            return compileCodeBlockMdx(el);
        }
        if ("table".equals(el.name())) {
            return compileTableMdx(el, templates, defaultNamespace, currentPageId, sceneResolver);
        }
        if ("tr".equals(el.name())) {
            return compileTableRowMdx(el, templates, defaultNamespace, currentPageId, sceneResolver);
        }
        if ("td".equals(el.name()) || "th".equals(el.name())) {
            return "<td>" + compileChildren(el.children(), templates, defaultNamespace, currentPageId, sceneResolver)
                + "</td>";
        }
        if ("hr".equals(el.name())) {
            return "<hr>";
        }
        // Inline elements
        if ("strong".equals(el.name())) {
            return "<strong>"
                + compileChildren(el.children(), templates, defaultNamespace, currentPageId, sceneResolver)
                + "</strong>";
        }
        if ("em".equals(el.name())) {
            return "<em>" + compileChildren(el.children(), templates, defaultNamespace, currentPageId, sceneResolver)
                + "</em>";
        }
        if ("del".equals(el.name())) {
            return "<del>" + compileChildren(el.children(), templates, defaultNamespace, currentPageId, sceneResolver)
                + "</del>";
        }
        if ("u".equals(el.name())) {
            return "<span class=\"guide-underline\">"
                + compileChildren(el.children(), templates, defaultNamespace, currentPageId, sceneResolver)
                + "</span>";
        }
        if ("wavy".equals(el.name())) {
            return "<span class=\"guide-wavy-underline\">"
                + compileChildren(el.children(), templates, defaultNamespace, currentPageId, sceneResolver)
                + "</span>";
        }
        if ("dotted".equals(el.name())) {
            return "<span class=\"guide-emphasis-dot\">"
                + compileChildren(el.children(), templates, defaultNamespace, currentPageId, sceneResolver)
                + "</span>";
        }
        if ("mark".equals(el.name())) {
            return "<mark class=\"guide-mark\">"
                + compileChildren(el.children(), templates, defaultNamespace, currentPageId, sceneResolver)
                + "</mark>";
        }
        if ("code".equals(el.name())) {
            return "<code>" + escapeHtml(extractTextFromElement(el)) + "</code>";
        }
        if ("br".equals(el.name())) {
            return "<br>";
        }
        if ("a".equals(el.name())) {
            return compileAnchorMdx(el, templates, defaultNamespace, currentPageId, sceneResolver);
        }
        if ("img".equals(el.name())) {
            return compileImageMdx(el, currentPageId);
        }
        if ("definition".equals(el.name())) {
            return "";
        }
        // Custom MDX tags (existing handlers)
        if (el instanceof MdxJsxFlowElement) {
            return compileCustomFlowElement(
                (MdxJsxFlowElement) el,
                templates,
                defaultNamespace,
                currentPageId,
                sceneResolver);
        }
        if (el instanceof MdxJsxTextElement) {
            return compileCustomTextElement(
                (MdxJsxTextElement) el,
                templates,
                defaultNamespace,
                currentPageId,
                sceneResolver);
        }
        return compileChildren(el.children(), templates, defaultNamespace, currentPageId, sceneResolver);
    }

    private boolean isHeadingName(@Nullable String name) {
        return name != null && name.length() == 2
            && name.charAt(0) == 'h'
            && name.charAt(1) >= '1'
            && name.charAt(1) <= '6';
    }

    private String compileCustomFlowElement(MdxJsxFlowElement flowElement, GuideSiteTemplateRegistry templates,
        String defaultNamespace, @Nullable ResourceLocation currentPageId, SceneResolver sceneResolver) {
        if (isHtmlAnchorElement(flowElement))
            return compileHtmlAnchor(flowElement, templates, defaultNamespace, currentPageId, sceneResolver);
        if (isHtmlBreakElement(flowElement)) return compileHtmlBreak(flowElement);
        if (isTooltipElement(flowElement))
            return "<p>" + compileTooltip(flowElement, templates, defaultNamespace, currentPageId, sceneResolver)
                + "</p>";
        if (isRecipeElement(flowElement)) return compileRecipe(flowElement, defaultNamespace);
        if (isSceneElement(flowElement))
            return compileScene(flowElement, templates, defaultNamespace, currentPageId, sceneResolver);
        if (isFloatingImageElement(flowElement))
            return compileFloatingImage(flowElement, templates, defaultNamespace, currentPageId, sceneResolver);
        if (isLatexElement(flowElement)) return compileLatex(flowElement, true, templates);
        String rendered = mdxTagRenderer
            .render(flowElement, defaultNamespace, currentPageId, templates, sceneResolver, this);
        if (rendered != null) return rendered;
        return compileChildren(flowElement.children(), templates, defaultNamespace, currentPageId, sceneResolver);
    }

    private String compileCustomTextElement(MdxJsxTextElement textElement, GuideSiteTemplateRegistry templates,
        String defaultNamespace, @Nullable ResourceLocation currentPageId, SceneResolver sceneResolver) {
        if (isHtmlAnchorElement(textElement))
            return compileHtmlAnchor(textElement, templates, defaultNamespace, currentPageId, sceneResolver);
        if (isHtmlBreakElement(textElement)) return compileHtmlBreak(textElement);
        if (isTooltipElement(textElement))
            return compileTooltip(textElement, templates, defaultNamespace, currentPageId, sceneResolver);
        if (isRecipeElement(textElement)) return compileRecipe(textElement, defaultNamespace);
        if (isSceneElement(textElement))
            return compileScene(textElement, templates, defaultNamespace, currentPageId, sceneResolver);
        if (isFloatingImageElement(textElement))
            return compileFloatingImage(textElement, templates, defaultNamespace, currentPageId, sceneResolver);
        if (isLatexElement(textElement)) return compileLatex(textElement, false, templates);
        String rendered = mdxTagRenderer
            .render(textElement, defaultNamespace, currentPageId, templates, sceneResolver, this);
        if (rendered != null) return rendered;
        return compileChildren(textElement.children(), templates, defaultNamespace, currentPageId, sceneResolver);
    }

    // Mdx-adapted helper methods

    private String compileParagraph(MdxJsxElementFields el, GuideSiteTemplateRegistry templates,
        String defaultNamespace, @Nullable ResourceLocation currentPageId, SceneResolver sceneResolver) {
        String displayFormula = extractSoleDisplayLatexFromElement(el);
        if (displayFormula != null) {
            return renderLatex(displayFormula, null, 1.0f, 100.0f, false, null, 0, 0, true, templates);
        }
        return "<p>" + compileChildren(el.children(), templates, defaultNamespace, currentPageId, sceneResolver)
            + "</p>";
    }

    private String compileHeadingMdx(MdxJsxElementFields el, GuideSiteTemplateRegistry templates,
        String defaultNamespace, @Nullable ResourceLocation currentPageId, SceneResolver sceneResolver) {
        int depth = Integer.parseInt(el.getAttributeString("depth", "1"));
        depth = depth <= 0 ? 1 : Math.min(depth, 6);
        String body = compileChildren(el.children(), templates, defaultNamespace, currentPageId, sceneResolver);
        String anchor = GuideSiteHrefResolver.headingAnchor(extractTextFromElement(el));
        if (anchor == null || anchor.isEmpty()) {
            return "<h" + depth + ">" + body + "</h" + depth + ">";
        }
        return "<h" + depth + " id=\"" + escapeAttribute(anchor) + "\">" + body + "</h" + depth + ">";
    }

    private String compileBlockquoteMdx(MdxJsxElementFields el, GuideSiteTemplateRegistry templates,
        String defaultNamespace, @Nullable ResourceLocation currentPageId, SceneResolver sceneResolver) {
        // Use the MdxJsx-adapted MarkdownRuntimeBlocks
        BlockquoteDirective directive = MarkdownRuntimeBlocks.parseBlockquoteDirective(el);
        if (directive != null && directive.alertType() != null) {
            return compileAlertBoxMdx(directive, templates, defaultNamespace, currentPageId, sceneResolver);
        }
        if (directive != null) {
            return compileQuoteBoxMdx(directive, templates, defaultNamespace, currentPageId, sceneResolver);
        }
        return "<blockquote>"
            + compileChildren(el.children(), templates, defaultNamespace, currentPageId, sceneResolver)
            + "</blockquote>";
    }

    private String compileAlertBoxMdx(BlockquoteDirective directive, GuideSiteTemplateRegistry templates,
        String defaultNamespace, @Nullable ResourceLocation currentPageId, SceneResolver sceneResolver) {
        String body = compileChildren(directive.children(), templates, defaultNamespace, currentPageId, sceneResolver);
        String typeName = directive.alertType()
            .displayText();
        return "<div class=\"alert alert-" + directive.alertType()
            .name()
            .toLowerCase(Locale.ROOT) + "\"><strong>" + escapeHtml(typeName) + "</strong>" + body + "</div>";
    }

    private String compileQuoteBoxMdx(BlockquoteDirective directive, GuideSiteTemplateRegistry templates,
        String defaultNamespace, @Nullable ResourceLocation currentPageId, SceneResolver sceneResolver) {
        String body = compileChildren(directive.children(), templates, defaultNamespace, currentPageId, sceneResolver);
        StringBuilder html = new StringBuilder("<blockquote class=\"guide-quote\"");
        if (directive.accentColor() != null) {
            html.append(" style=\"border-color: ")
                .append(toCssColor(directive.accentColor()))
                .append("\"");
        }
        html.append(">");
        if (directive.title() != null) {
            html.append("<strong>")
                .append(escapeHtml(directive.title()))
                .append("</strong><br>");
        }
        html.append(body)
            .append("</blockquote>");
        return html.toString();
    }

    private String compileListMdx(MdxJsxElementFields el, GuideSiteTemplateRegistry templates, String defaultNamespace,
        @Nullable ResourceLocation currentPageId, SceneResolver sceneResolver) {
        String tag = "ol".equals(el.name()) ? "ol" : "ul";
        String startAttr = "";
        if ("ol".equals(el.name())) {
            String startStr = el.getAttributeString("start", "1");
            if (!"1".equals(startStr)) {
                startAttr = " start=\"" + escapeAttribute(startStr) + "\"";
            }
        }
        return "<" + tag
            + startAttr
            + ">"
            + compileChildren(el.children(), templates, defaultNamespace, currentPageId, sceneResolver)
            + "</"
            + tag
            + ">";
    }

    private String compileListItemMdx(MdxJsxElementFields el, GuideSiteTemplateRegistry templates,
        String defaultNamespace, @Nullable ResourceLocation currentPageId, SceneResolver sceneResolver) {
        return "<li>" + compileChildren(el.children(), templates, defaultNamespace, currentPageId, sceneResolver)
            + "</li>";
    }

    private String compileCodeBlockMdx(MdxJsxElementFields el) {
        String codeText = extractTextFromElement(el);
        String lang = el.getAttributeString("lang", null);
        String meta = el.getAttributeString("meta", null);
        if (lang != null) {
            lang = lang.toLowerCase(Locale.ROOT);
        }

        // Sub-language rendering
        if ("csv".equals(lang)) {
            return GuideSiteGraphRenderer.renderCsvTable(codeText, true);
        }
        if ("tree".equals(lang) || "filetree".equals(lang)) {
            return GuideSiteGraphRenderer.renderFileTree(codeText);
        }
        if ("mermaid".equals(lang)) {
            try {
                MermaidMindmapDocument doc = MermaidMindmapParser.parse(codeText);
                return GuideSiteGraphRenderer.renderMermaidTree(doc);
            } catch (Exception ignored) {
                return "<pre><code class=\"language-mermaid\">" + escapeHtml(codeText) + "</code></pre>";
            }
        }
        if ("funcgraph".equals(lang) || "functiongraph".equals(lang)) {
            try {
                LytFunctionGraph graph = FunctionGraphFenceParser.parse(codeText);
                return GuideSiteGraphRenderer.renderFunctionGraph(graph);
            } catch (RuntimeException ignored) {
                return "<pre><code class=\"language-" + escapeAttribute(lang)
                    + "\">"
                    + escapeHtml(codeText)
                    + "</code></pre>";
            }
        }

        // Plain code block with optional size constraints
        Integer width = parseMetaInt(meta, "width");
        Integer height = parseMetaInt(meta, "height");
        StringBuilder html = new StringBuilder();
        html.append("<pre");
        if (width != null || height != null) {
            html.append(" class=\"guide-code-sized\" style=\"");
            if (width != null) {
                html.append("width:")
                    .append(width)
                    .append("px;max-width:100%;");
            }
            if (height != null) {
                html.append("height:")
                    .append(height)
                    .append("px;overflow:auto;");
            }
            html.append("\"");
        }
        html.append("><code");
        if (lang != null && !lang.isEmpty()) {
            html.append(" class=\"language-")
                .append(escapeAttribute(lang))
                .append("\"");
        }
        html.append(">")
            .append(escapeHtml(codeText))
            .append("</code></pre>");
        return html.toString();
    }

    @Nullable
    private static Integer parseMetaInt(String meta, String key) {
        if (meta == null || meta.isEmpty()) {
            return null;
        }
        Matcher m = Pattern.compile("(?:^|\\s)" + Pattern.quote(key) + "\\s*=\\s*\"?'?([0-9]+)\"?'?")
            .matcher(meta);
        if (m.find()) {
            try {
                return Integer.parseInt(m.group(1));
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private String compileTableMdx(MdxJsxElementFields el, GuideSiteTemplateRegistry templates, String defaultNamespace,
        @Nullable ResourceLocation currentPageId, SceneResolver sceneResolver) {
        StringBuilder html = new StringBuilder("<table>");
        String alignStr = el.getAttributeString("align", "");
        String[] aligns = alignStr.isEmpty() ? new String[0] : alignStr.split(",");
        boolean firstRow = true;
        for (Object child : el.children()) {
            if (child instanceof MdxJsxFlowElement && "tr".equals(((MdxJsxFlowElement) child).name())) {
                MdxJsxFlowElement tr = (MdxJsxFlowElement) child;
                html.append("<tr>");
                int cellIdx = 0;
                for (Object cellChild : tr.children()) {
                    if (cellChild instanceof MdxJsxFlowElement && "td".equals(((MdxJsxFlowElement) cellChild).name())) {
                        String tag = firstRow ? "th" : "td";
                        String align = "";
                        if (cellIdx < aligns.length) {
                            String a = aligns[cellIdx].trim();
                            if ("center".equals(a) || "right".equals(a)) {
                                align = " style=\"text-align:" + a + "\"";
                            }
                        }
                        html.append("<")
                            .append(tag)
                            .append(align)
                            .append(">");
                        html.append(
                            compileChildren(
                                ((MdxJsxFlowElement) cellChild).children(),
                                templates,
                                defaultNamespace,
                                currentPageId,
                                sceneResolver));
                        html.append("</")
                            .append(tag)
                            .append(">");
                        cellIdx++;
                    }
                }
                html.append("</tr>");
                firstRow = false;
            }
        }
        html.append("</table>");
        return html.toString();
    }

    private String compileTableRowMdx(MdxJsxElementFields el, GuideSiteTemplateRegistry templates,
        String defaultNamespace, @Nullable ResourceLocation currentPageId, SceneResolver sceneResolver) {
        return "<tr>" + compileChildren(el.children(), templates, defaultNamespace, currentPageId, sceneResolver)
            + "</tr>";
    }

    private String compileAnchorMdx(MdxJsxElementFields el, GuideSiteTemplateRegistry templates,
        String defaultNamespace, @Nullable ResourceLocation currentPageId, SceneResolver sceneResolver) {
        String href = el.getAttributeString("href", "");
        String body = compileChildren(el.children(), templates, defaultNamespace, currentPageId, sceneResolver);
        if (!href.isEmpty() && isSoundActionHref(href)) {
            return compileSoundLink(href, body, defaultNamespace, currentPageId);
        }
        if (!href.isEmpty()) {
            return "<a href=\"" + escapeAttribute(GuideSiteHrefResolver.resolveRawHref(currentPageId, href))
                + "\">"
                + body
                + "</a>";
        }
        return body;
    }

    private String compileImageMdx(MdxJsxElementFields el, @Nullable ResourceLocation currentPageId) {
        String src = el.getAttributeString("src", "");
        String alt = el.getAttributeString("alt", "");
        String title = el.getAttributeString("title", "");
        String resolvedSrc = GuideSiteHrefResolver.resolveRawHref(currentPageId, src);
        StringBuilder html = new StringBuilder("<img src=\"");
        html.append(escapeAttribute(resolvedSrc))
            .append("\"");
        if (!alt.isEmpty()) html.append(" alt=\"")
            .append(escapeAttribute(alt))
            .append("\"");
        if (!title.isEmpty()) html.append(" title=\"")
            .append(escapeAttribute(title))
            .append("\"");
        html.append(">");
        return html.toString();
    }

    private static String extractTextFromElement(MdxJsxElementFields el) {
        StringBuilder sb = new StringBuilder();
        collectTextFromChildren(el, sb);
        return sb.toString();
    }

    private static void collectTextFromChildren(MdxJsxElementFields el, StringBuilder sb) {
        for (Object child : el.children()) {
            if (child instanceof MdAstText) {
                sb.append(((MdAstText) child).value);
            } else if (child instanceof MdxJsxElementFields) {
                collectTextFromChildren((MdxJsxElementFields) child, sb);
            }
        }
    }

    @Nullable
    private String extractSoleDisplayLatexFromElement(MdxJsxElementFields el) {
        if (el.children()
            .size() != 1
            || !(el.children()
                .get(0) instanceof MdAstText)) {
            return null;
        }
        return MarkdownLatexShorthand.extractSoleDisplayFormula(
            ((MdAstText) el.children()
                .get(0)).value);
    }

    private String compileTooltip(MdxJsxElementFields element, GuideSiteTemplateRegistry templates,
        String defaultNamespace, @Nullable ResourceLocation currentPageId, SceneResolver sceneResolver) {
        String label = element.getAttributeString("label", "");
        if (label == null || label.isEmpty()) {
            String trigger = compileChildren(
                element.children(),
                templates,
                defaultNamespace,
                currentPageId,
                sceneResolver);
            if (trigger != null && !trigger.trim()
                .isEmpty()) {
                label = null;
            } else {
                label = "tooltip";
            }
        }

        String tooltipHtml = compileTooltipContent(element, templates, defaultNamespace, currentPageId, sceneResolver);
        String templateId = templates.create(tooltipHtml);
        return "<span class=\"guide-tooltip\" data-template=\"" + escapeAttribute(templateId)
            + "\">"
            + (label != null ? escapeHtml(label)
                : compileChildren(element.children(), templates, defaultNamespace, currentPageId, sceneResolver))
            + "</span>";
    }

    private String compileTooltipContent(MdxJsxElementFields element, GuideSiteTemplateRegistry templates,
        String defaultNamespace, @Nullable ResourceLocation currentPageId, SceneResolver sceneResolver) {
        GuideSiteHtmlCompiler tooltipCompiler = new GuideSiteHtmlCompiler(
            recipeTagRenderer,
            tooltipSceneTagRenderer(),
            imageResolver,
            mdxTagRenderer,
            latexExporter,
            assetExporter);
        MdxJsxAttribute content = element.getAttribute("content");
        if (content != null && content.hasExpressionValue()) {
            String source = normalizeTooltipContentExpression(content.getExpressionValue());
            if (!source.isEmpty()) {
                ResourceLocation parsePageId = currentPageId != null ? currentPageId
                    : new ResourceLocation(
                        defaultNamespace != null ? defaultNamespace : "guidenh",
                        "site_export_tooltip");
                ParsedGuidePage parsed = PageCompiler.parse("site-export-tooltip", "en_us", parsePageId, source);
                return tooltipCompiler.compileFragment(
                    parsed.getAstRoot()
                        .children(),
                    templates,
                    defaultNamespace,
                    sceneResolver,
                    currentPageId);
            }
        }
        return tooltipCompiler
            .compileFragment(element.children(), templates, defaultNamespace, sceneResolver, currentPageId);
    }

    private SceneTagRenderer tooltipSceneTagRenderer() {
        return (element, defaultNamespace, currentPageId, templates, exportedScene) -> {
            int width = parseSceneDimension(element.getAttributeString("width", null), 256);
            int height = parseSceneDimension(element.getAttributeString("height", null), 192);
            return GuideSiteSceneTagRenderer.renderStaticScenePlaceholder(width, height, exportedScene);
        };
    }

    private int parseSceneDimension(@Nullable String raw, int fallback) {
        Integer value = parsePositiveInt(raw);
        return value != null ? value : fallback;
    }

    private String normalizeTooltipContentExpression(String expression) {
        if (expression == null) {
            return "";
        }
        String trimmed = expression.trim();
        if (trimmed.startsWith("<>") && trimmed.endsWith("</>")) {
            return trimmed.substring(2, trimmed.length() - 3)
                .trim();
        }
        return trimmed;
    }

    private boolean isHtmlAnchorElement(MdxJsxElementFields element) {
        return "a".equals(element.name());
    }

    private boolean isHtmlBreakElement(MdxJsxElementFields element) {
        return "br".equals(element.name());
    }

    private boolean isTooltipElement(MdxJsxElementFields element) {
        return "Tooltip".equals(element.name());
    }

    private boolean isRecipeElement(MdxJsxElementFields element) {
        return "Recipe".equals(element.name()) || "RecipeFor".equals(element.name())
            || "RecipeUsage".equals(element.name())
            || "RecipesFor".equals(element.name());
    }

    private boolean isSceneElement(MdxJsxElementFields element) {
        return "GameScene".equals(element.name()) || "Scene".equals(element.name());
    }

    private boolean isFloatingImageElement(MdxJsxElementFields element) {
        return "FloatingImage".equals(element.name());
    }

    private boolean isLatexElement(MdxJsxElementFields element) {
        return "Latex".equals(element.name());
    }

    private String compileRecipe(MdxJsxElementFields element, String defaultNamespace) {
        return recipeTagRenderer.render(element, defaultNamespace);
    }

    private String compileScene(MdxJsxElementFields element, GuideSiteTemplateRegistry templates,
        String defaultNamespace, @Nullable ResourceLocation currentPageId, SceneResolver sceneResolver) {
        return sceneTagRenderer.render(element, defaultNamespace, currentPageId, templates, sceneResolver.nextScene());
    }

    private String compileFloatingImage(MdxJsxElementFields element, GuideSiteTemplateRegistry templates,
        String defaultNamespace, @Nullable ResourceLocation currentPageId, SceneResolver sceneResolver) {
        String rawSrc = element.getAttributeString("src", "");
        if (!hasText(rawSrc)) {
            return renderExportError("FloatingImage requires a non-empty src attribute.");
        }
        String title = element.getAttributeString("title", null);
        String alt = element.getAttributeString("alt", title != null ? title : "");
        Integer width = parsePositiveInt(element.getAttributeString("width", null));
        Integer height = parsePositiveInt(element.getAttributeString("height", null));
        String src = resolveImageSource(rawSrc, currentPageId);

        StringBuilder style = new StringBuilder();
        if ("right".equals(element.getAttributeString("align", "left"))) {
            style.append("float:right;margin:0 0 5px 5px;");
        } else {
            style.append("float:left;margin:0 5px 5px 0;");
        }
        if (width != null) {
            style.append("width:")
                .append(width)
                .append("px;");
        }
        if (height != null) {
            style.append("height:")
                .append(height)
                .append("px;");
        }

        List<ImageAnnotationExport> annotations = collectImageAnnotations(
            element,
            templates,
            defaultNamespace,
            currentPageId,
            sceneResolver,
            width,
            height);
        if (annotations.isEmpty()) {
            return buildImageTag("guide-image guide-floating-image", src, alt, title, style.toString(), width, height);
        }

        StringBuilder html = new StringBuilder();
        html.append("<span class=\"guide-floating-image-wrap\" style=\"")
            .append(escapeAttribute(style.toString()))
            .append("\">");
        html.append(buildImageTag("guide-image guide-floating-image", src, alt, title, null, width, height));
        for (ImageAnnotationExport annotation : annotations) {
            html.append("<span class=\"guide-image-annotation");
            if (annotation.templateId != null) {
                html.append(" guide-tooltip");
            }
            html.append("\"");
            if (annotation.templateId != null) {
                html.append(" data-template=\"")
                    .append(escapeAttribute(annotation.templateId))
                    .append("\"");
            }
            if (annotation.sound != null) {
                GuideSiteSoundExport.appendDataAttributes(
                    html,
                    annotation.sound,
                    annotation.soundTrigger,
                    annotation.soundSrc,
                    this::escapeAttribute);
            }
            appendOptionalDataAttribute(html, "data-source-x", annotation.sourceX);
            appendOptionalDataAttribute(html, "data-source-y", annotation.sourceY);
            appendOptionalDataAttribute(html, "data-source-width", annotation.sourceWidth);
            appendOptionalDataAttribute(html, "data-source-height", annotation.sourceHeight);
            html.append(" style=\"")
                .append(escapeAttribute(annotation.style))
                .append("\"></span>");
        }
        html.append("</span>");
        return html.toString();
    }

    private List<ImageAnnotationExport> collectImageAnnotations(MdxJsxElementFields element,
        GuideSiteTemplateRegistry templates, String defaultNamespace, @Nullable ResourceLocation currentPageId,
        SceneResolver sceneResolver, @Nullable Integer imageWidth, @Nullable Integer imageHeight) {
        List<ImageAnnotationExport> annotations = new ArrayList<>();
        ImageAnnotationExport wholeImageSound = buildWholeImageSoundAnnotation(
            element,
            imageWidth,
            imageHeight,
            currentPageId);
        if (wholeImageSound != null) {
            annotations.add(wholeImageSound);
        }
        for (MdAstAnyContent child : element.children()) {
            if (!(child instanceof MdxJsxElementFields childElement)
                || (!"ImageAnnotation".equals(childElement.name()) && !"SoundArea".equals(childElement.name()))) {
                continue;
            }
            String tooltipHtml = "ImageAnnotation".equals(childElement.name())
                ? compileChildren(childElement.children(), templates, defaultNamespace, currentPageId, sceneResolver)
                : "";
            String templateId = tooltipHtml.trim()
                .isEmpty() ? null : templates.create(tooltipHtml);
            annotations
                .add(buildImageAnnotation(childElement, templateId, imageWidth, imageHeight, currentPageId, "src"));
        }
        return annotations;
    }

    @Nullable
    private ImageAnnotationExport buildWholeImageSoundAnnotation(MdxJsxElementFields element,
        @Nullable Integer imageWidth, @Nullable Integer imageHeight, @Nullable ResourceLocation currentPageId) {
        String sound = element.getAttributeString("sound", null);
        String soundSrc = element.getAttributeString("soundSrc", null);
        if (!hasText(sound) && !hasText(soundSrc)) {
            return null;
        }
        return buildImageAnnotation(element, null, imageWidth, imageHeight, currentPageId, "soundSrc");
    }

    private ImageAnnotationExport buildImageAnnotation(MdxJsxElementFields element, @Nullable String templateId,
        @Nullable Integer imageWidth, @Nullable Integer imageHeight, @Nullable ResourceLocation currentPageId,
        String soundSourceAttributeName) {
        Integer x = parsePositiveOrZeroInt(element.getAttributeString("x", null));
        Integer y = parsePositiveOrZeroInt(element.getAttributeString("y", null));
        Integer w = parsePositiveInt(element.getAttributeString("w", null));
        Integer h = parsePositiveInt(element.getAttributeString("h", null));
        boolean wholeImage = x == null && y == null && w == null && h == null;

        StringBuilder style = new StringBuilder();
        if (wholeImage) {
            style.append("left:0;top:0;width:100%;height:100%;");
        } else {
            style.append("left:0;top:0;width:1px;height:1px;");
        }
        if (readBoolean(element, "border", false)) {
            style.append("border:")
                .append(Math.max(1, readInt(element, "borderThickness", 1)))
                .append("px solid ")
                .append(escapeCssColor(element.getAttributeString("borderColor", "#FFFFFFFF")))
                .append(";");
        }
        GuideSiteSoundExport.MdxSoundAttributes soundAttributes = soundAttributes(element, soundSourceAttributeName);
        GuideSoundSpec sound = GuideSiteSoundExport.parse(
            soundAttributes,
            currentPageId != null ? currentPageId.getResourceDomain() : "guidenh",
            currentPageId);
        String soundSrc = sound != null
            ? GuideSiteSoundExport.exportSource(sound, soundAttributes, currentPageId, assetExporter)
            : "";
        return new ImageAnnotationExport(
            templateId,
            style.toString(),
            wholeImage ? null : x != null ? x : 0,
            wholeImage ? null : y != null ? y : 0,
            wholeImage ? null : w != null ? w : 1,
            wholeImage ? null : h != null ? h : 1,
            sound,
            sound != null
                ? GuideSoundTrigger.parse(element.getAttributeString("trigger", null), GuideSoundTrigger.CLICK)
                : GuideSoundTrigger.CLICK,
            soundSrc);
    }

    private GuideSiteSoundExport.MdxSoundAttributes soundAttributes(MdxJsxElementFields element,
        String soundSourceAttributeName) {
        return new GuideSiteSoundExport.MdxSoundAttributes() {

            @Override
            public @Nullable String value(String name) {
                if ("src".equals(name) && !"src".equals(soundSourceAttributeName)) {
                    return element.getAttributeString(soundSourceAttributeName, null);
                }
                return element.getAttributeString(name, null);
            }
        };
    }

    private void appendOptionalDataAttribute(StringBuilder html, String name, @Nullable Integer value) {
        if (value == null) {
            return;
        }
        html.append(" ")
            .append(name)
            .append("=\"")
            .append(value)
            .append("\"");
    }

    private String compileLatex(MdxJsxElementFields element, boolean display, GuideSiteTemplateRegistry templates) {
        String formula = element.getAttributeString("formula", null);
        if (formula == null || formula.trim()
            .isEmpty()) {
            return "<span class=\"guide-export-error\">Latex tag requires a formula</span>";
        }
        String color = element.getAttributeString("color", null);
        float scale = parseFloat(element.getAttributeString("scale", null), 1.0f);
        float sourceScale = parseFloat(element.getAttributeString("sourceScale", null), 100.0f);
        boolean showTooltip = readBoolean(element, "showTooltip", false);
        String valign = element.getAttributeString("valign", null);
        int offsetX = readInt(element, "offsetX", 0);
        int offsetY = readInt(element, "offsetY", 0);
        return renderLatex(
            formula,
            color,
            scale,
            sourceScale,
            showTooltip,
            valign,
            offsetX,
            offsetY,
            display,
            templates);
    }

    private String renderLatex(String formula, @Nullable String color, float scale, float sourceScale,
        boolean showTooltip, @Nullable String valign, int offsetX, int offsetY, boolean display,
        GuideSiteTemplateRegistry templates) {
        String tag = display ? "div" : "span";
        StringBuilder classes = new StringBuilder(
            display ? "guide-latex guide-latex-display" : "guide-latex guide-latex-inline");
        if (valign != null && !valign.trim()
            .isEmpty()) {
            classes.append(" guide-latex-valign-")
                .append(
                    sanitizeCssToken(
                        valign.trim()
                            .toLowerCase(Locale.ROOT)));
        }

        StringBuilder style = new StringBuilder();
        float safeScale = Math.max(0.1f, scale);
        int fillColorArgb = parseLatexColorArgb(color);
        GuideSiteLatexExporter.ExportedLatex exported = latexExporter != null
            ? latexExporter.export(formula, fillColorArgb, Math.max(16f, sourceScale))
            : null;
        if (exported != null) {
            appendCssPx(style, "width", displayWidth(exported, safeScale));
            appendCssPx(style, "height", displayHeight(exported, safeScale));
            if (!display && isLatexBaselineAlign(valign)) {
                style.append("vertical-align:bottom;");
            }
        } else {
            String cssColor = parseLatexCssColor(color);
            if (cssColor != null) {
                style.append("color:")
                    .append(cssColor)
                    .append(";");
            }
        }
        int visualOffsetY = offsetY;
        if (exported != null && !display && isLatexBaselineAlign(valign)) {
            visualOffsetY += displayDepth(exported, safeScale);
        }
        if (offsetX != 0 || visualOffsetY != 0) {
            style.append("transform:translate(")
                .append(offsetX)
                .append("px,")
                .append(visualOffsetY)
                .append("px);");
        }

        String templateId = null;
        if (showTooltip) {
            templateId = templates.create("<code>" + escapeHtml(formula) + "</code>");
        }

        StringBuilder html = new StringBuilder();
        html.append("<")
            .append(tag)
            .append(" class=\"")
            .append(classes)
            .append("\"");
        if (templateId != null) {
            html.append(" data-template=\"")
                .append(escapeAttribute(templateId))
                .append("\"");
        }
        if (style.length() > 0) {
            html.append(" style=\"")
                .append(escapeAttribute(style.toString()))
                .append("\"");
        }
        html.append(">")
            .append(renderLatexBody(formula, exported))
            .append("</")
            .append(tag)
            .append(">");
        return html.toString();
    }

    private String renderLatexBody(String formula, @Nullable GuideSiteLatexExporter.ExportedLatex exported) {
        if (exported == null) {
            return escapeHtml(formula);
        }
        return "<img class=\"guide-latex-image\" src=\"" + escapeAttribute(exported.src())
            + "\" alt=\""
            + escapeAttribute(formula)
            + "\">";
    }

    private int displayWidth(GuideSiteLatexExporter.ExportedLatex exported, float scale) {
        return Math.max(1, (int) Math.ceil((double) exported.widthPx() * 20.0d * scale / exported.referenceHeightPx()));
    }

    private int displayHeight(GuideSiteLatexExporter.ExportedLatex exported, float scale) {
        return Math
            .max(1, (int) Math.ceil((double) exported.heightPx() * 20.0d * scale / exported.referenceHeightPx()));
    }

    private int displayDepth(GuideSiteLatexExporter.ExportedLatex exported, float scale) {
        return Math.max(0, (int) Math.ceil((double) exported.depthPx() * 20.0d * scale / exported.referenceHeightPx()));
    }

    private boolean isLatexBaselineAlign(@Nullable String valign) {
        return valign == null || valign.trim()
            .isEmpty() || "baseline".equalsIgnoreCase(valign.trim());
    }

    private void appendCssPx(StringBuilder style, String property, int value) {
        style.append(property)
            .append(':')
            .append(value)
            .append("px;");
    }

    private String compileText(String text, GuideSiteTemplateRegistry templates, String defaultNamespace,
        @Nullable ResourceLocation currentPageId) {
        if (!MarkdownActionLink.mayContain(text) && !MarkdownLatexShorthand.mayContain(text)) {
            return escapeHtml(text);
        }
        StringBuilder html = new StringBuilder();
        for (MarkdownActionLink.Segment actionSegment : MarkdownActionLink.split(text)) {
            if (actionSegment.isLink() && isSoundActionHref(actionSegment.href())) {
                html.append(
                    compileSoundLink(
                        actionSegment.href(),
                        escapeHtml(actionSegment.text()),
                        defaultNamespace,
                        currentPageId));
            } else {
                html.append(compileLatexText(actionSegment.text(), templates));
            }
        }
        return html.toString();
    }

    private String compileLatexText(String text, GuideSiteTemplateRegistry templates) {
        if (!MarkdownLatexShorthand.mayContain(text)) {
            return escapeHtml(text);
        }
        StringBuilder html = new StringBuilder();
        for (MarkdownLatexShorthand.Segment segment : MarkdownLatexShorthand.split(text)) {
            if (segment.isFormula()) {
                html.append(renderLatex(segment.getValue(), null, 1.0f, 100.0f, false, null, 0, 0, false, templates));
            } else {
                html.append(escapeHtml(segment.getValue()));
            }
        }
        return html.toString();
    }

    private boolean isSoundActionHref(String href) {
        return href != null && (href.startsWith("sound:") || href.startsWith("sound-src:"));
    }

    private String compileSoundLink(String href, String body, String defaultNamespace,
        @Nullable ResourceLocation currentPageId) {
        SoundHrefAttributes attributes = SoundHrefAttributes.parse(href);
        GuideSoundSpec sound = GuideSiteSoundExport.parse(attributes, defaultNamespace, currentPageId);
        if (sound == null) {
            return body;
        }
        String src = GuideSiteSoundExport.exportSource(sound, attributes, currentPageId, assetExporter);
        StringBuilder html = new StringBuilder();
        html.append("<span class=\"guide-sound-link\" tabindex=\"0\" role=\"button\"");
        GuideSiteSoundExport.appendDataAttributes(html, sound, GuideSoundTrigger.CLICK, src, this::escapeAttribute);
        html.append(">")
            .append(body)
            .append("</span>");
        return html.toString();
    }

    private String compileHtmlAnchor(MdxJsxElementFields element, GuideSiteTemplateRegistry templates,
        String defaultNamespace, @Nullable ResourceLocation currentPageId, SceneResolver sceneResolver) {
        String href = element.getAttributeString("href", "");
        String anchorName = element.getAttributeString("name", "");
        String title = element.getAttributeString("title", "");
        String body = compileChildren(element.children(), templates, defaultNamespace, currentPageId, sceneResolver);
        String templateId = createTextTooltipTemplate(title, templates, currentPageId);

        if (href != null && !href.isEmpty()) {
            StringBuilder html = new StringBuilder();
            html.append("<a href=\"")
                .append(escapeAttribute(GuideSiteHrefResolver.resolveRawHref(currentPageId, href)))
                .append("\"");
            if (templateId != null) {
                html.append(" class=\"guide-tooltip\" data-template=\"")
                    .append(escapeAttribute(templateId))
                    .append("\"");
            }
            if (anchorName != null && !anchorName.isEmpty()) {
                html.append(" id=\"")
                    .append(escapeAttribute(anchorName))
                    .append("\"");
            }
            html.append(">")
                .append(body)
                .append("</a>");
            return html.toString();
        }

        if (templateId != null) {
            StringBuilder html = new StringBuilder();
            html.append("<span class=\"guide-link guide-tooltip\" data-template=\"")
                .append(escapeAttribute(templateId))
                .append("\"");
            if (anchorName != null && !anchorName.isEmpty()) {
                html.append(" id=\"")
                    .append(escapeAttribute(anchorName))
                    .append("\"");
            }
            html.append(">")
                .append(body)
                .append("</span>");
            return html.toString();
        }

        if (anchorName != null && !anchorName.isEmpty()) {
            if (body.isEmpty()) {
                return "<span id=\"" + escapeAttribute(anchorName) + "\"></span>";
            }
            return "<span id=\"" + escapeAttribute(anchorName) + "\">" + body + "</span>";
        }

        return body;
    }

    private String compileHtmlBreak(MdxJsxElementFields element) {
        String clear = element.getAttributeString("clear", "none");
        if ("left".equals(clear) || "right".equals(clear)) {
            return "<br style=\"clear:" + clear + "\">";
        }
        if ("all".equals(clear)) {
            return "<br style=\"clear:both\">";
        }
        if ("none".equals(clear) || clear == null || clear.isEmpty()) {
            return "<br>";
        }
        return "<span class=\"guide-export-error\">Invalid 'clear' attribute</span><br>";
    }

    @Nullable
    private String createTextTooltipTemplate(@Nullable String text, GuideSiteTemplateRegistry templates,
        @Nullable ResourceLocation currentPageId) {
        if (text == null || text.isEmpty()) {
            return null;
        }
        String html = GuideSiteSceneAnnotationSerializer.renderTooltipHtml(new TextTooltip(text), currentPageId);
        return html == null || html.trim()
            .isEmpty() ? null : templates.create(html);
    }

    private String renderQuoteIcon(QuoteIconSpec icon, GuideSiteTemplateRegistry templates, String defaultNamespace,
        @Nullable ResourceLocation currentPageId, SceneResolver sceneResolver) {
        String value = icon.value();
        if (value == null || value.isEmpty()) {
            return "";
        }
        switch (icon.kind()) {
            case PNG: {
                String resolved = imageResolver.resolve(value, currentPageId);
                if (resolved == null || resolved.isEmpty()) {
                    return "<span class=\"guide-quote-icon\">" + escapeHtml(value) + "</span>";
                }
                return "<span class=\"guide-quote-icon guide-quote-icon-image\"><img src=\"" + escapeAttribute(resolved)
                    + "\" alt=\"\" decoding=\"async\"></span>";
            }
            case ITEM: {
                // Synthesize an <ItemImage id="..." /> tag and dispatch through the MDX renderer so
                // the icon is exported with the same path/tooltip handling as inline ItemImages.
                List<MdxJsxAttributeNode> attrs = new ArrayList<>();
                attrs.add(new MdxJsxAttribute("id", value));
                MdxJsxFlowElement synthetic = new MdxJsxFlowElement("ItemImage", attrs);
                String rendered = mdxTagRenderer
                    .render(synthetic, defaultNamespace, currentPageId, templates, sceneResolver, this);
                if (rendered == null || rendered.isEmpty()) {
                    return "<span class=\"guide-quote-icon\">" + escapeHtml(value) + "</span>";
                }
                return "<span class=\"guide-quote-icon guide-quote-icon-item\">" + rendered + "</span>";
            }
            case TEXT:
            default:
                return "<span class=\"guide-quote-icon\">" + escapeHtml(value) + "</span>";
        }
    }

    private static String toCssColor(ColorValue color) {
        int argb = color.resolve(LightDarkMode.LIGHT_MODE);
        int a = (argb >>> 24) & 0xFF;
        int r = (argb >>> 16) & 0xFF;
        int g = (argb >>> 8) & 0xFF;
        int b = argb & 0xFF;
        return "rgba(" + r + "," + g + "," + b + "," + (a / 255.0f) + ")";
    }

    private boolean hasText(@Nullable String value) {
        return value != null && !value.trim()
            .isEmpty();
    }

    private String renderExportError(String message) {
        return "<span class=\"guide-export-error\">" + escapeHtml(message) + "</span>";
    }

    private String resolveImageSource(String rawUrl, @Nullable ResourceLocation currentPageId) {
        String resolved = imageResolver.resolve(rawUrl, currentPageId);
        if (resolved == null || resolved.isEmpty()) {
            return rawUrl != null ? rawUrl : "";
        }
        return resolved;
    }

    @Nullable
    private String parseLatexCssColor(@Nullable String raw) {
        if (raw == null || raw.trim()
            .isEmpty()) {
            return null;
        }
        return escapeCssColor(raw);
    }

    private int parseLatexColorArgb(@Nullable String raw) {
        if (raw == null || raw.trim()
            .isEmpty()) {
            return 0xFFFFFFFF;
        }
        String trimmed = raw.trim();
        if (trimmed.startsWith("#")) {
            trimmed = trimmed.substring(1);
        }
        try {
            if (trimmed.length() == 6) {
                return 0xFF000000 | Integer.parseUnsignedInt(trimmed, 16);
            }
            if (trimmed.length() == 8) {
                return (int) Long.parseLong(trimmed, 16);
            }
        } catch (NumberFormatException ignored) {}
        return 0xFFFFFFFF;
    }

    private String escapeCssColor(String raw) {
        if (raw == null) {
            return "transparent";
        }
        String trimmed = raw.trim();
        if (trimmed.matches("#[0-9a-fA-F]{6}")) {
            return trimmed;
        }
        if (trimmed.matches("#[0-9a-fA-F]{8}")) {
            int alpha = Integer.parseInt(trimmed.substring(1, 3), 16);
            int red = Integer.parseInt(trimmed.substring(3, 5), 16);
            int green = Integer.parseInt(trimmed.substring(5, 7), 16);
            int blue = Integer.parseInt(trimmed.substring(7, 9), 16);
            return "rgba(" + red + "," + green + "," + blue + "," + (alpha / 255.0f) + ")";
        }
        if ("transparent".equalsIgnoreCase(trimmed)) {
            return "transparent";
        }
        return "#ffffffff";
    }

    @Nullable
    private Integer parsePositiveInt(@Nullable String raw) {
        if (raw == null || raw.isEmpty()) {
            return null;
        }
        try {
            int value = Integer.parseInt(raw.trim());
            return value > 0 ? value : null;
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    @Nullable
    private Integer parsePositiveOrZeroInt(@Nullable String raw) {
        if (raw == null || raw.isEmpty()) {
            return null;
        }
        try {
            int value = Integer.parseInt(raw.trim());
            return value >= 0 ? value : null;
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private int readInt(MdxJsxElementFields element, String name, int fallback) {
        String raw = element.getAttributeString(name, null);
        if (raw == null || raw.trim()
            .isEmpty()) {
            return fallback;
        }
        try {
            return Integer.parseInt(raw.trim());
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private float parseFloat(@Nullable String raw, float fallback) {
        if (raw == null || raw.trim()
            .isEmpty()) {
            return fallback;
        }
        try {
            return Float.parseFloat(raw.trim());
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private boolean readBoolean(MdxJsxElementFields element, String name, boolean fallback) {
        try {
            return MdxAttrs.getBoolean(element, name, fallback);
        } catch (MdxAttrs.AttributeException ignored) {
            return fallback;
        }
    }

    private String sanitizeCssToken(String value) {
        if (value == null || value.isEmpty()) {
            return "";
        }
        StringBuilder result = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            if ((ch >= 'a' && ch <= 'z') || (ch >= '0' && ch <= '9') || ch == '-') {
                result.append(ch);
            }
        }
        return result.toString();
    }

    private String buildImageTag(String cssClass, String src, @Nullable String alt, @Nullable String title,
        @Nullable String style, @Nullable Integer width, @Nullable Integer height) {
        StringBuilder html = new StringBuilder();
        html.append("<img class=\"")
            .append(escapeAttribute(cssClass))
            .append("\" src=\"")
            .append(escapeAttribute(src))
            .append("\" alt=\"")
            .append(escapeAttribute(alt != null ? alt : ""))
            .append("\"");
        if (title != null && !title.isEmpty()) {
            html.append(" title=\"")
                .append(escapeAttribute(title))
                .append("\"");
        }
        if (style != null && !style.isEmpty()) {
            html.append(" style=\"")
                .append(escapeAttribute(style))
                .append("\"");
        }
        if (width != null) {
            html.append(" width=\"")
                .append(width)
                .append("\"");
        }
        if (height != null) {
            html.append(" height=\"")
                .append(height)
                .append("\"");
        }
        html.append(" loading=\"lazy\" decoding=\"async\">");
        return html.toString();
    }

    private static ImageResolver passthroughImageResolver() {
        return (rawUrl, currentPageId) -> rawUrl != null ? rawUrl : "";
    }

    private static MdxTagRenderer noopMdxTagRenderer() {
        return (element, defaultNamespace, currentPageId, templates, sceneResolver, compiler) -> null;
    }

    private static class ImageAnnotationExport {

        @Nullable
        private final String templateId;
        private final String style;
        @Nullable
        private final Integer sourceX;
        @Nullable
        private final Integer sourceY;
        @Nullable
        private final Integer sourceWidth;
        @Nullable
        private final Integer sourceHeight;
        @Nullable
        private final GuideSoundSpec sound;
        private final GuideSoundTrigger soundTrigger;
        private final String soundSrc;

        private ImageAnnotationExport(@Nullable String templateId, String style, @Nullable Integer sourceX,
            @Nullable Integer sourceY, @Nullable Integer sourceWidth, @Nullable Integer sourceHeight,
            @Nullable GuideSoundSpec sound, GuideSoundTrigger soundTrigger, String soundSrc) {
            this.templateId = templateId;
            this.style = style;
            this.sourceX = sourceX;
            this.sourceY = sourceY;
            this.sourceWidth = sourceWidth;
            this.sourceHeight = sourceHeight;
            this.sound = sound;
            this.soundTrigger = soundTrigger;
            this.soundSrc = soundSrc != null ? soundSrc : "";
        }
    }

    private static class SoundHrefAttributes implements GuideSiteSoundExport.MdxSoundAttributes {

        private final String sound;
        private final String source;
        private final String query;

        private SoundHrefAttributes(String sound, String source, String query) {
            this.sound = sound;
            this.source = source;
            this.query = query;
        }

        private static SoundHrefAttributes parse(String href) {
            String value = href != null ? href : "";
            boolean sourceMode = value.startsWith("sound-src:");
            String payload = sourceMode ? value.substring("sound-src:".length())
                : value.startsWith("sound:") ? value.substring("sound:".length()) : "";
            String path = payload;
            String query = "";
            int queryIndex = payload.indexOf('?');
            if (queryIndex >= 0) {
                path = payload.substring(0, queryIndex);
                query = payload.substring(queryIndex + 1);
            }
            return new SoundHrefAttributes(
                sourceMode ? null : decodeUriPart(path),
                sourceMode ? decodeUriPart(path) : null,
                query);
        }

        @Override
        public @Nullable String value(String name) {
            if ("sound".equals(name)) {
                return sound;
            }
            if ("src".equals(name)) {
                return source;
            }
            return queryValue(name);
        }

        @Nullable
        private String queryValue(String name) {
            if (query == null || query.isEmpty()) {
                return null;
            }
            String[] pairs = query.split("&");
            for (String pair : pairs) {
                int eq = pair.indexOf('=');
                if (eq <= 0) {
                    continue;
                }
                if (name.equals(decodeUriPart(pair.substring(0, eq)))) {
                    return decodeUriPart(pair.substring(eq + 1));
                }
            }
            return null;
        }

        private static String decodeUriPart(String value) {
            try {
                return URLDecoder.decode(value, "UTF-8");
            } catch (UnsupportedEncodingException | IllegalArgumentException ignored) {
                return value.replace("%20", " ");
            }
        }
    }

    private String escapeAttribute(String text) {
        return escapeHtml(text).replace("\"", "&quot;");
    }

    private String escapeHtml(String text) {
        return text.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;");
    }
}
