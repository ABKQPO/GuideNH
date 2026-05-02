package com.hfstudio.guidenh.guide.siteexport.site;

import java.util.List;
import java.util.Locale;

import net.minecraft.util.ResourceLocation;

import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.guide.color.ColorValue;
import com.hfstudio.guidenh.guide.color.LightDarkMode;
import com.hfstudio.guidenh.guide.compiler.ParsedGuidePage;
import com.hfstudio.guidenh.guide.compiler.tags.functiongraph.FunctionGraphFenceParser;
import com.hfstudio.guidenh.guide.document.block.functiongraph.LytFunctionGraph;
import com.hfstudio.guidenh.guide.internal.markdown.MarkdownRuntimeBlocks;
import com.hfstudio.guidenh.guide.internal.markdown.MarkdownRuntimeBlocks.BlockquoteDirective;
import com.hfstudio.guidenh.guide.internal.markdown.MarkdownRuntimeBlocks.QuoteIconSpec;
import com.hfstudio.guidenh.guide.internal.mermaid.MermaidMindmapDocument;
import com.hfstudio.guidenh.guide.internal.mermaid.MermaidMindmapParser;
import com.hfstudio.guidenh.libs.mdast.gfm.model.GfmTable;
import com.hfstudio.guidenh.libs.mdast.gfm.model.GfmTableCell;
import com.hfstudio.guidenh.libs.mdast.gfm.model.GfmTableRow;
import com.hfstudio.guidenh.libs.mdast.gfmstrikethrough.MdAstDelete;
import com.hfstudio.guidenh.libs.mdast.guideunderline.MdAstDottedUnderline;
import com.hfstudio.guidenh.libs.mdast.guideunderline.MdAstUnderline;
import com.hfstudio.guidenh.libs.mdast.guideunderline.MdAstWavyUnderline;
import com.hfstudio.guidenh.libs.mdast.mdx.model.MdxJsxElementFields;
import com.hfstudio.guidenh.libs.mdast.mdx.model.MdxJsxFlowElement;
import com.hfstudio.guidenh.libs.mdast.mdx.model.MdxJsxTextElement;
import com.hfstudio.guidenh.libs.mdast.model.MdAstAnyContent;
import com.hfstudio.guidenh.libs.mdast.model.MdAstBlockquote;
import com.hfstudio.guidenh.libs.mdast.model.MdAstBreak;
import com.hfstudio.guidenh.libs.mdast.model.MdAstCode;
import com.hfstudio.guidenh.libs.mdast.model.MdAstEmphasis;
import com.hfstudio.guidenh.libs.mdast.model.MdAstHeading;
import com.hfstudio.guidenh.libs.mdast.model.MdAstImage;
import com.hfstudio.guidenh.libs.mdast.model.MdAstInlineCode;
import com.hfstudio.guidenh.libs.mdast.model.MdAstLink;
import com.hfstudio.guidenh.libs.mdast.model.MdAstList;
import com.hfstudio.guidenh.libs.mdast.model.MdAstListItem;
import com.hfstudio.guidenh.libs.mdast.model.MdAstParagraph;
import com.hfstudio.guidenh.libs.mdast.model.MdAstParent;
import com.hfstudio.guidenh.libs.mdast.model.MdAstStrong;
import com.hfstudio.guidenh.libs.mdast.model.MdAstText;
import com.hfstudio.guidenh.libs.mdast.model.MdAstThematicBreak;
import com.hfstudio.guidenh.libs.micromark.extensions.gfm.Align;

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
        this(
            recipeTagRenderer,
            new GuideSiteSceneTagRenderer(recipeTagRenderer, imageResolver, mdxTagRenderer),
            imageResolver,
            mdxTagRenderer);
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
        this.recipeTagRenderer = recipeTagRenderer;
        this.sceneTagRenderer = sceneTagRenderer;
        this.imageResolver = imageResolver;
        this.mdxTagRenderer = mdxTagRenderer;
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
        if (node instanceof MdAstParagraph paragraph) {
            return "<p>"
                + compileChildren(paragraph.children(), templates, defaultNamespace, currentPageId, sceneResolver)
                + "</p>";
        }
        if (node instanceof MdAstHeading heading) {
            return compileHeading(heading, templates, defaultNamespace, currentPageId, sceneResolver);
        }
        if (node instanceof MdAstBlockquote blockquote) {
            return compileBlockquote(blockquote, templates, defaultNamespace, currentPageId, sceneResolver);
        }
        if (node instanceof MdAstList list) {
            return compileList(list, templates, defaultNamespace, currentPageId, sceneResolver);
        }
        if (node instanceof MdAstCode code) {
            return compileCodeBlock(code);
        }
        if (node instanceof GfmTable table) {
            return compileTable(table, templates, defaultNamespace, currentPageId, sceneResolver);
        }
        if (node instanceof MdAstThematicBreak) {
            return "<hr>";
        }
        if (node instanceof MdAstImage image) {
            return compileMarkdownImage(image, currentPageId);
        }
        if (node instanceof MdAstText text) {
            return escapeHtml(text.value());
        }
        if (node instanceof MdAstDelete deleted) {
            return "<del>"
                + compileChildren(deleted.children(), templates, defaultNamespace, currentPageId, sceneResolver)
                + "</del>";
        }
        if (node instanceof MdAstUnderline underline) {
            return "<span class=\"guide-underline\">"
                + compileChildren(underline.children(), templates, defaultNamespace, currentPageId, sceneResolver)
                + "</span>";
        }
        if (node instanceof MdAstWavyUnderline wavy) {
            return "<span class=\"guide-wavy-underline\">"
                + compileChildren(wavy.children(), templates, defaultNamespace, currentPageId, sceneResolver)
                + "</span>";
        }
        if (node instanceof MdAstDottedUnderline dotted) {
            return "<span class=\"guide-emphasis-dot\">"
                + compileChildren(dotted.children(), templates, defaultNamespace, currentPageId, sceneResolver)
                + "</span>";
        }
        if (node instanceof MdAstStrong strong) {
            return "<strong>"
                + compileChildren(strong.children(), templates, defaultNamespace, currentPageId, sceneResolver)
                + "</strong>";
        }
        if (node instanceof MdAstEmphasis emphasis) {
            return "<em>"
                + compileChildren(emphasis.children(), templates, defaultNamespace, currentPageId, sceneResolver)
                + "</em>";
        }
        if (node instanceof MdAstInlineCode inlineCode) {
            return "<code>" + escapeHtml(inlineCode.value()) + "</code>";
        }
        if (node instanceof MdAstBreak) {
            return "<br>";
        }
        if (node instanceof MdAstLink link) {
            return "<a href=\"" + escapeAttribute(GuideSiteHrefResolver.resolveRawHref(currentPageId, link.url()))
                + "\">"
                + compileChildren(link.children(), templates, defaultNamespace, currentPageId, sceneResolver)
                + "</a>";
        }
        if (node instanceof MdxJsxFlowElement flowElement && isHtmlAnchorElement(flowElement)) {
            return compileHtmlAnchor(flowElement, templates, defaultNamespace, currentPageId, sceneResolver);
        }
        if (node instanceof MdxJsxFlowElement flowElement && isTooltipElement(flowElement)) {
            return "<p>" + compileTooltip(flowElement, templates, defaultNamespace, currentPageId, sceneResolver)
                + "</p>";
        }
        if (node instanceof MdxJsxFlowElement flowElement && isRecipeElement(flowElement)) {
            return compileRecipe(flowElement, defaultNamespace);
        }
        if (node instanceof MdxJsxFlowElement flowElement && isSceneElement(flowElement)) {
            return compileScene(flowElement, templates, defaultNamespace, currentPageId, sceneResolver);
        }
        if (node instanceof MdxJsxFlowElement flowElement && isFloatingImageElement(flowElement)) {
            return compileFloatingImage(flowElement, currentPageId);
        }
        if (node instanceof MdxJsxFlowElement flowElement) {
            String rendered = mdxTagRenderer
                .render(flowElement, defaultNamespace, currentPageId, templates, sceneResolver, this);
            if (rendered != null) {
                return rendered;
            }
        }
        if (node instanceof MdxJsxTextElement textElement && isHtmlAnchorElement(textElement)) {
            return compileHtmlAnchor(textElement, templates, defaultNamespace, currentPageId, sceneResolver);
        }
        if (node instanceof MdxJsxTextElement textElement && isTooltipElement(textElement)) {
            return compileTooltip(textElement, templates, defaultNamespace, currentPageId, sceneResolver);
        }
        if (node instanceof MdxJsxTextElement textElement && isRecipeElement(textElement)) {
            return compileRecipe(textElement, defaultNamespace);
        }
        if (node instanceof MdxJsxTextElement textElement && isSceneElement(textElement)) {
            return compileScene(textElement, templates, defaultNamespace, currentPageId, sceneResolver);
        }
        if (node instanceof MdxJsxTextElement textElement && isFloatingImageElement(textElement)) {
            return compileFloatingImage(textElement, currentPageId);
        }
        if (node instanceof MdxJsxTextElement textElement) {
            String rendered = mdxTagRenderer
                .render(textElement, defaultNamespace, currentPageId, templates, sceneResolver, this);
            if (rendered != null) {
                return rendered;
            }
        }
        if (node instanceof MdAstListItem listItem) {
            return "<li>"
                + compileChildren(listItem.children(), templates, defaultNamespace, currentPageId, sceneResolver)
                + "</li>";
        }
        if (node instanceof GfmTableRow row) {
            return compileTableRow(row, templates, defaultNamespace, currentPageId, sceneResolver, false, null);
        }
        if (node instanceof GfmTableCell cell) {
            return "<td>" + compileChildren(cell.children(), templates, defaultNamespace, currentPageId, sceneResolver)
                + "</td>";
        }
        if (node instanceof MdAstParent<?>parent) {
            return compileChildren(parent.children(), templates, defaultNamespace, currentPageId, sceneResolver);
        }
        return "";
    }

    private String compileTooltip(MdxJsxElementFields element, GuideSiteTemplateRegistry templates,
        String defaultNamespace, @Nullable ResourceLocation currentPageId, SceneResolver sceneResolver) {
        String label = element.getAttributeString("label", "");
        if (label == null || label.isEmpty()) {
            label = "tooltip";
        }

        String templateId = templates
            .create(compileChildren(element.children(), templates, defaultNamespace, currentPageId, sceneResolver));
        return "<span class=\"guide-tooltip\" data-template=\"" + escapeAttribute(templateId)
            + "\">"
            + escapeHtml(label)
            + "</span>";
    }

    private boolean isHtmlAnchorElement(MdxJsxElementFields element) {
        return "a".equals(element.name());
    }

    private boolean isTooltipElement(MdxJsxElementFields element) {
        return "Tooltip".equals(element.name());
    }

    private boolean isRecipeElement(MdxJsxElementFields element) {
        return "Recipe".equals(element.name()) || "RecipeFor".equals(element.name())
            || "RecipesFor".equals(element.name());
    }

    private boolean isSceneElement(MdxJsxElementFields element) {
        return "GameScene".equals(element.name()) || "Scene".equals(element.name());
    }

    private boolean isFloatingImageElement(MdxJsxElementFields element) {
        return "FloatingImage".equals(element.name());
    }

    private String compileRecipe(MdxJsxElementFields element, String defaultNamespace) {
        return recipeTagRenderer.render(element, defaultNamespace);
    }

    private String compileScene(MdxJsxElementFields element, GuideSiteTemplateRegistry templates,
        String defaultNamespace, @Nullable ResourceLocation currentPageId, SceneResolver sceneResolver) {
        return sceneTagRenderer.render(element, defaultNamespace, currentPageId, templates, sceneResolver.nextScene());
    }

    private String compileMarkdownImage(MdAstImage image, @Nullable ResourceLocation currentPageId) {
        return buildImageTag(
            "guide-image",
            resolveImageSource(image.url(), currentPageId),
            image.alt(),
            image.title(),
            null,
            null,
            null);
    }

    private String compileFloatingImage(MdxJsxElementFields element, @Nullable ResourceLocation currentPageId) {
        String title = element.getAttributeString("title", null);
        String alt = element.getAttributeString("alt", title != null ? title : "");
        Integer width = parsePositiveInt(element.getAttributeString("width", null));
        Integer height = parsePositiveInt(element.getAttributeString("height", null));

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

        return buildImageTag(
            "guide-image guide-floating-image",
            resolveImageSource(element.getAttributeString("src", ""), currentPageId),
            alt,
            title,
            style.toString(),
            width,
            height);
    }

    private String compileHeading(MdAstHeading heading, GuideSiteTemplateRegistry templates, String defaultNamespace,
        @Nullable ResourceLocation currentPageId, SceneResolver sceneResolver) {
        int depth = heading.depth <= 0 ? 1 : Math.min(heading.depth, 6);
        String body = compileChildren(heading.children(), templates, defaultNamespace, currentPageId, sceneResolver);
        String anchor = GuideSiteHrefResolver.headingAnchor(heading.toText());
        if (anchor == null || anchor.isEmpty()) {
            return "<h" + depth + ">" + body + "</h" + depth + ">";
        }
        return "<h" + depth + " id=\"" + escapeAttribute(anchor) + "\">" + body + "</h" + depth + ">";
    }

    private String compileHtmlAnchor(MdxJsxElementFields element, GuideSiteTemplateRegistry templates,
        String defaultNamespace, @Nullable ResourceLocation currentPageId, SceneResolver sceneResolver) {
        String href = element.getAttributeString("href", "");
        String anchorName = element.getAttributeString("name", "");
        String body = compileChildren(element.children(), templates, defaultNamespace, currentPageId, sceneResolver);

        if (href != null && !href.isEmpty()) {
            StringBuilder html = new StringBuilder();
            html.append("<a href=\"")
                .append(escapeAttribute(GuideSiteHrefResolver.resolveRawHref(currentPageId, href)))
                .append("\"");
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

        if (anchorName != null && !anchorName.isEmpty()) {
            if (body.isEmpty()) {
                return "<span id=\"" + escapeAttribute(anchorName) + "\"></span>";
            }
            return "<span id=\"" + escapeAttribute(anchorName) + "\">" + body + "</span>";
        }

        return body;
    }

    private String compileList(MdAstList list, GuideSiteTemplateRegistry templates, String defaultNamespace,
        @Nullable ResourceLocation currentPageId, SceneResolver sceneResolver) {
        String tagName = list.ordered ? "ol" : "ul";
        StringBuilder html = new StringBuilder();
        html.append("<")
            .append(tagName);
        if (list.ordered && list.start > 1) {
            html.append(" start=\"")
                .append(list.start)
                .append("\"");
        }
        html.append(">");
        html.append(compileChildren(list.children(), templates, defaultNamespace, currentPageId, sceneResolver));
        html.append("</")
            .append(tagName)
            .append(">");
        return html.toString();
    }

    private String compileCodeBlock(MdAstCode code) {
        String lang = code.lang != null ? code.lang.toLowerCase(Locale.ROOT) : "";
        String meta = code.meta != null ? code.meta : "";
        if ("csv".equals(lang)) {
            return GuideSiteGraphRenderer.renderCsvTable(code.value != null ? code.value : "", true);
        }
        if ("tree".equals(lang) || "filetree".equals(lang)) {
            return GuideSiteGraphRenderer.renderFileTree(code.value != null ? code.value : "");
        }
        if ("mermaid".equals(lang)) {
            String src = code.value != null ? code.value : "";
            try {
                MermaidMindmapDocument doc = MermaidMindmapParser.parse(src);
                return GuideSiteGraphRenderer.renderMermaidTree(doc);
            } catch (Exception ignored) {
                return "<pre><code class=\"language-mermaid\">" + escapeHtml(src) + "</code></pre>";
            }
        }
        if ("funcgraph".equals(lang) || "functiongraph".equals(lang)) {
            String src = code.value != null ? code.value : "";
            LytFunctionGraph graph = FunctionGraphFenceParser.parse(src);
            return GuideSiteGraphRenderer.renderFunctionGraph(graph);
        }
        // Forced viewport: ```<lang> width=220 height=96 — emits a sized scrollable container.
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
        if (code.lang != null && !code.lang.isEmpty()) {
            html.append(" class=\"language-")
                .append(escapeAttribute(code.lang))
                .append("\"");
        }
        html.append(">")
            .append(escapeHtml(code.value != null ? code.value : ""))
            .append("</code></pre>");
        return html.toString();
    }

    private static @Nullable Integer parseMetaInt(String meta, String key) {
        if (meta == null || meta.isEmpty()) {
            return null;
        }
        // Accept tokens like `width=220`, `width="220"`, `width='220'`.
        java.util.regex.Matcher m = java.util.regex.Pattern
            .compile("(?:^|\\s)" + java.util.regex.Pattern.quote(key) + "\\s*=\\s*\"?'?([0-9]+)\"?'?")
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

    private String compileBlockquote(MdAstBlockquote blockquote, GuideSiteTemplateRegistry templates,
        String defaultNamespace, @Nullable ResourceLocation currentPageId, SceneResolver sceneResolver) {
        BlockquoteDirective directive = MarkdownRuntimeBlocks.parseBlockquoteDirective(blockquote);
        if (directive == null) {
            return "<blockquote>"
                + compileChildren(blockquote.children(), templates, defaultNamespace, currentPageId, sceneResolver)
                + "</blockquote>";
        }
        StringBuilder cls = new StringBuilder("guide-quote");
        if (directive.alertType() != null) {
            cls.append(" guide-alert guide-alert-")
                .append(directive.alertType()
                    .name()
                    .toLowerCase(Locale.ROOT));
        }
        StringBuilder style = new StringBuilder();
        ColorValue accent = directive.accentColor();
        if (accent != null) {
            style.append("--guide-quote-accent:")
                .append(toCssColor(accent))
                .append(";");
        }
        StringBuilder html = new StringBuilder();
        html.append("<blockquote class=\"")
            .append(escapeAttribute(cls.toString()))
            .append("\"");
        if (style.length() > 0) {
            html.append(" style=\"")
                .append(escapeAttribute(style.toString()))
                .append("\"");
        }
        html.append(">");
        String title = directive.title();
        if (title != null && !title.isEmpty()) {
            html.append("<div class=\"guide-quote-title\">");
            QuoteIconSpec icon = directive.icon();
            if (icon != null && icon.kind() == MarkdownRuntimeBlocks.QuoteIconKind.TEXT) {
                html.append("<span class=\"guide-quote-icon\">")
                    .append(escapeHtml(icon.value()))
                    .append("</span>");
            }
            html.append("<span class=\"guide-quote-title-text\">")
                .append(escapeHtml(title))
                .append("</span></div>");
        }
        html.append("<div class=\"guide-quote-body\">")
            .append(compileChildren(blockquote.children(), templates, defaultNamespace, currentPageId, sceneResolver))
            .append("</div></blockquote>");
        return html.toString();
    }

    private static String toCssColor(ColorValue color) {
        int argb = color.resolve(LightDarkMode.LIGHT_MODE);
        int a = (argb >>> 24) & 0xFF;
        int r = (argb >>> 16) & 0xFF;
        int g = (argb >>> 8) & 0xFF;
        int b = argb & 0xFF;
        return "rgba(" + r + "," + g + "," + b + "," + (a / 255.0f) + ")";
    }

    private String compileTable(GfmTable table, GuideSiteTemplateRegistry templates, String defaultNamespace,
        @Nullable ResourceLocation currentPageId, SceneResolver sceneResolver) {
        StringBuilder html = new StringBuilder();
        html.append("<table>");
        List<GfmTableRow> rows = table.children();
        if (!rows.isEmpty()) {
            html.append("<thead>");
            html.append(
                compileTableRow(
                    rows.get(0),
                    templates,
                    defaultNamespace,
                    currentPageId,
                    sceneResolver,
                    true,
                    table.align));
            html.append("</thead>");
        }
        if (rows.size() > 1) {
            html.append("<tbody>");
            for (int i = 1; i < rows.size(); i++) {
                html.append(
                    compileTableRow(
                        rows.get(i),
                        templates,
                        defaultNamespace,
                        currentPageId,
                        sceneResolver,
                        false,
                        table.align));
            }
            html.append("</tbody>");
        }
        html.append("</table>");
        return html.toString();
    }

    private String compileTableRow(GfmTableRow row, GuideSiteTemplateRegistry templates, String defaultNamespace,
        @Nullable ResourceLocation currentPageId, SceneResolver sceneResolver, boolean header,
        @Nullable List<Align> align) {
        StringBuilder html = new StringBuilder();
        html.append("<tr>");
        List<GfmTableCell> cells = row.children();
        for (int i = 0; i < cells.size(); i++) {
            GfmTableCell cell = cells.get(i);
            String tagName = header ? "th" : "td";
            html.append("<")
                .append(tagName);
            String alignCss = tableAlignCss(align, i);
            if (alignCss != null) {
                html.append(" style=\"")
                    .append(escapeAttribute(alignCss))
                    .append("\"");
            }
            html.append(">")
                .append(compileChildren(cell.children(), templates, defaultNamespace, currentPageId, sceneResolver))
                .append("</")
                .append(tagName)
                .append(">");
        }
        html.append("</tr>");
        return html.toString();
    }

    @Nullable
    private String tableAlignCss(@Nullable List<Align> align, int index) {
        if (align == null || index < 0 || index >= align.size()) {
            return null;
        }
        Align value = align.get(index);
        if (value == null || value == Align.NONE) {
            return null;
        }
        if (value == Align.LEFT) {
            return "text-align:left;";
        }
        if (value == Align.CENTER) {
            return "text-align:center;";
        }
        if (value == Align.RIGHT) {
            return "text-align:right;";
        }
        return null;
    }

    private String resolveImageSource(String rawUrl, @Nullable ResourceLocation currentPageId) {
        String resolved = imageResolver.resolve(rawUrl, currentPageId);
        if (resolved == null || resolved.isEmpty()) {
            return rawUrl != null ? rawUrl : "";
        }
        return resolved;
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
