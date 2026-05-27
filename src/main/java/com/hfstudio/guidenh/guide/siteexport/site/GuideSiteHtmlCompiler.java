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
import com.hfstudio.guidenh.guide.internal.markdown.MarkdownListSemantics;
import com.hfstudio.guidenh.guide.internal.markdown.MarkdownRuntimeBlocks;
import com.hfstudio.guidenh.guide.internal.markdown.MarkdownRuntimeBlocks.BlockquoteDirective;
import com.hfstudio.guidenh.guide.internal.markdown.MarkdownRuntimeBlocks.QuoteIconSpec;
import com.hfstudio.guidenh.guide.internal.mermaid.MermaidMindmapDocument;
import com.hfstudio.guidenh.guide.internal.mermaid.MermaidMindmapParser;
import com.hfstudio.guidenh.guide.sound.GuideSoundSpec;
import com.hfstudio.guidenh.guide.sound.GuideSoundTrigger;
import com.hfstudio.guidenh.libs.mdast.gfm.model.GfmTable;
import com.hfstudio.guidenh.libs.mdast.gfm.model.GfmTableCell;
import com.hfstudio.guidenh.libs.mdast.gfm.model.GfmTableRow;
import com.hfstudio.guidenh.libs.mdast.gfmstrikethrough.MdAstDelete;
import com.hfstudio.guidenh.libs.mdast.guidemark.MdAstMark;
import com.hfstudio.guidenh.libs.mdast.guideunderline.MdAstDottedUnderline;
import com.hfstudio.guidenh.libs.mdast.guideunderline.MdAstUnderline;
import com.hfstudio.guidenh.libs.mdast.guideunderline.MdAstWavyUnderline;
import com.hfstudio.guidenh.libs.mdast.mdx.model.MdxJsxAttribute;
import com.hfstudio.guidenh.libs.mdast.mdx.model.MdxJsxAttributeNode;
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
import com.hfstudio.guidenh.libs.mdast.model.MdAstNode;
import com.hfstudio.guidenh.libs.mdast.model.MdAstParagraph;
import com.hfstudio.guidenh.libs.mdast.model.MdAstParent;
import com.hfstudio.guidenh.libs.mdast.model.MdAstPhrasingContent;
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
            if (child instanceof MdAstParagraph paragraph) {
                html.append(
                    compileChildren(paragraph.children(), templates, defaultNamespace, currentPageId, sceneResolver));
            } else if (child instanceof MdAstParent<?>nestedParent && !(child instanceof MdAstPhrasingContent)) {
                html.append(
                    compileChildren(
                        nestedParent.children(),
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
        if (node instanceof MdAstParagraph paragraph) {
            String displayFormula = extractSoleDisplayLatex(paragraph);
            if (displayFormula != null) {
                return renderLatex(displayFormula, null, 1.0f, 100.0f, false, null, 0, 0, true, templates);
            }
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
            return compileText(text.value(), templates, defaultNamespace, currentPageId);
        }
        if (node instanceof MdAstDelete deleted) {
            return "<del>"
                + compileChildren(deleted.children(), templates, defaultNamespace, currentPageId, sceneResolver)
                + "</del>";
        }
        if (node instanceof MdAstMark mark) {
            return "<mark class=\"guide-mark\">"
                + compileChildren(mark.children(), templates, defaultNamespace, currentPageId, sceneResolver)
                + "</mark>";
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
        if (node instanceof MdAstLink link && isSoundActionHref(link.url())) {
            return compileSoundLink(
                link.url(),
                compileChildren(link.children(), templates, defaultNamespace, currentPageId, sceneResolver),
                defaultNamespace,
                currentPageId);
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
        if (node instanceof MdxJsxFlowElement flowElement && isHtmlBreakElement(flowElement)) {
            return compileHtmlBreak(flowElement);
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
            return compileFloatingImage(flowElement, templates, defaultNamespace, currentPageId, sceneResolver);
        }
        if (node instanceof MdxJsxFlowElement flowElement && isLatexElement(flowElement)) {
            return compileLatex(flowElement, true, templates);
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
        if (node instanceof MdxJsxTextElement textElement && isHtmlBreakElement(textElement)) {
            return compileHtmlBreak(textElement);
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
            return compileFloatingImage(textElement, templates, defaultNamespace, currentPageId, sceneResolver);
        }
        if (node instanceof MdxJsxTextElement textElement && isLatexElement(textElement)) {
            return compileLatex(textElement, false, templates);
        }
        if (node instanceof MdxJsxTextElement textElement) {
            String rendered = mdxTagRenderer
                .render(textElement, defaultNamespace, currentPageId, templates, sceneResolver, this);
            if (rendered != null) {
                return rendered;
            }
        }
        if (node instanceof MdAstListItem listItem) {
            return compileListItem(listItem, templates, defaultNamespace, currentPageId, sceneResolver);
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

    @Nullable
    private String extractSoleDisplayLatex(MdAstParagraph paragraph) {
        if (paragraph.children()
            .size() != 1
            || !(paragraph.children()
                .get(0) instanceof MdAstText text)) {
            return null;
        }
        return MarkdownLatexShorthand.extractSoleDisplayFormula(text.value());
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

    private String compileListItem(MdAstListItem listItem, GuideSiteTemplateRegistry templates, String defaultNamespace,
        @Nullable ResourceLocation currentPageId, SceneResolver sceneResolver) {
        var taskMarker = MarkdownListSemantics.extractTaskMarker(listItem.children());
        if (taskMarker == null) {
            return "<li>"
                + compileChildren(listItem.children(), templates, defaultNamespace, currentPageId, sceneResolver)
                + "</li>";
        }

        StringBuilder html = new StringBuilder();
        html.append("<li class=\"guide-task-list-item\">")
            .append("<input class=\"guide-task-list-checkbox\" type=\"checkbox\" disabled");
        if (taskMarker.checked()) {
            html.append(" checked");
        }
        html.append(">")
            .append("<div class=\"guide-task-list-content\">")
            .append(
                compileTaskListItemChildren(
                    listItem,
                    taskMarker,
                    templates,
                    defaultNamespace,
                    currentPageId,
                    sceneResolver))
            .append("</div></li>");
        return html.toString();
    }

    private String compileTaskListItemChildren(MdAstListItem listItem, MarkdownListSemantics.TaskMarker taskMarker,
        GuideSiteTemplateRegistry templates, String defaultNamespace, @Nullable ResourceLocation currentPageId,
        SceneResolver sceneResolver) {
        if (listItem.children()
            .isEmpty()
            || !(listItem.children()
                .get(0) instanceof MdAstParagraph paragraph)) {
            return compileChildren(listItem.children(), templates, defaultNamespace, currentPageId, sceneResolver);
        }

        StringBuilder html = new StringBuilder();
        html.append(
            compileNode(
                cloneParagraphWithLeadingTextOverride(paragraph, taskMarker.remainingText()),
                templates,
                defaultNamespace,
                currentPageId,
                sceneResolver));
        for (int i = 1; i < listItem.children()
            .size(); i++) {
            html.append(
                compileNode(
                    listItem.children()
                        .get(i),
                    templates,
                    defaultNamespace,
                    currentPageId,
                    sceneResolver));
        }
        return html.toString();
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

    private String compileCodeBlock(MdAstCode code) {
        String lang = code.lang != null ? code.lang.toLowerCase(Locale.ROOT) : "";
        String meta = code.meta != null ? code.meta : "";
        switch (lang) {
            case "csv" -> {
                return GuideSiteGraphRenderer.renderCsvTable(code.value != null ? code.value : "", true);
            }
            case "tree", "filetree" -> {
                return GuideSiteGraphRenderer.renderFileTree(code.value != null ? code.value : "");
            }
            case "mermaid" -> {
                String src = code.value != null ? code.value : "";
                try {
                    MermaidMindmapDocument doc = MermaidMindmapParser.parse(src);
                    return GuideSiteGraphRenderer.renderMermaidTree(doc);
                } catch (Exception ignored) {
                    return "<pre><code class=\"language-mermaid\">" + escapeHtml(src) + "</code></pre>";
                }
            }
            case "funcgraph", "functiongraph" -> {
                String src = code.value != null ? code.value : "";
                LytFunctionGraph graph = FunctionGraphFenceParser.parse(src);
                return GuideSiteGraphRenderer.renderFunctionGraph(graph);
            }
        }
        // Forced viewport: ```<lang> width=220 height=96 - emits a sized scrollable container.
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
                .append(
                    directive.alertType()
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
        QuoteIconSpec icon = directive.icon();
        if ((title != null && !title.isEmpty()) || icon != null) {
            html.append("<div class=\"guide-quote-title\">");
            if (icon != null) {
                String iconHtml = renderQuoteIcon(icon, templates, defaultNamespace, currentPageId, sceneResolver);
                if (!iconHtml.isEmpty()) {
                    html.append(iconHtml);
                }
            }
            if (title != null && !title.isEmpty()) {
                html.append("<span class=\"guide-quote-title-text\">")
                    .append(escapeHtml(title))
                    .append("</span>");
            }
            html.append("</div>");
        }
        html.append("<div class=\"guide-quote-body\">")
            .append(compileDirectiveBody(directive, templates, defaultNamespace, currentPageId, sceneResolver))
            .append("</div></blockquote>");
        return html.toString();
    }

    private String compileDirectiveBody(BlockquoteDirective directive, GuideSiteTemplateRegistry templates,
        String defaultNamespace, @Nullable ResourceLocation currentPageId, SceneResolver sceneResolver) {
        List<? extends MdAstAnyContent> children = directive.children();
        if (!children.isEmpty() && directive.firstParagraph() != null
            && children.get(0) == directive.firstParagraph()) {
            MdAstParagraph firstParagraph = cloneParagraphWithLeadingTextOverride(
                directive.firstParagraph(),
                directive.remainingText());
            StringBuilder html = new StringBuilder();
            if (!firstParagraph.children()
                .isEmpty()) {
                html.append(compileNode(firstParagraph, templates, defaultNamespace, currentPageId, sceneResolver));
            }
            for (int index = 1; index < children.size(); index++) {
                html.append(
                    compileNode(children.get(index), templates, defaultNamespace, currentPageId, sceneResolver));
            }
            return html.toString();
        }
        return compileChildren(children, templates, defaultNamespace, currentPageId, sceneResolver);
    }

    /**
     * Render the {@code icon=} / {@code iconPng=} / {@code iconItem=} marker shown in the quote
     * title. Falls back to a plain text glyph when the requested icon kind cannot be resolved so
     * the heading never collapses to nothing.
     */
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

    private String compileTable(GfmTable table, GuideSiteTemplateRegistry templates, String defaultNamespace,
        @Nullable ResourceLocation currentPageId, SceneResolver sceneResolver) {
        StringBuilder html = new StringBuilder();
        html.append("<div class=\"guide-table-wrap\"><table class=\"guide-table\">");
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
        html.append("</table></div>");
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
