package com.hfstudio.guidenh.guide.siteexport.site;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;

import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.guide.Guide;
import com.hfstudio.guidenh.guide.GuidePageIcon;
import com.hfstudio.guidenh.guide.PageAnchor;
import com.hfstudio.guidenh.guide.color.ColorValue;
import com.hfstudio.guidenh.guide.color.ConstantColor;
import com.hfstudio.guidenh.guide.color.LightDarkMode;
import com.hfstudio.guidenh.guide.color.SymbolicColor;
import com.hfstudio.guidenh.guide.color.SymbolicColorResolver;
import com.hfstudio.guidenh.guide.compiler.FrontmatterNavigation;
import com.hfstudio.guidenh.guide.compiler.GuideItemReferenceResolver;
import com.hfstudio.guidenh.guide.compiler.IdUtils;
import com.hfstudio.guidenh.guide.compiler.PageCompiler;
import com.hfstudio.guidenh.guide.compiler.ParsedGuidePage;
import com.hfstudio.guidenh.guide.compiler.tags.CommandLinkCompiler;
import com.hfstudio.guidenh.guide.compiler.tags.ItemImageCompiler;
import com.hfstudio.guidenh.guide.compiler.tags.KeyBindTagCompiler;
import com.hfstudio.guidenh.guide.compiler.tags.MdxAttrs;
import com.hfstudio.guidenh.guide.compiler.tags.StructureViewCompiler;
import com.hfstudio.guidenh.guide.compiler.tags.SubPagesCompiler;
import com.hfstudio.guidenh.guide.compiler.tags.chart.ChartAttrParser;
import com.hfstudio.guidenh.guide.compiler.tags.functiongraph.FunctionGraphAttrs;
import com.hfstudio.guidenh.guide.document.block.LytStructureView;
import com.hfstudio.guidenh.guide.document.block.chart.CornerLegendPosition;
import com.hfstudio.guidenh.guide.document.block.functiongraph.AutoPointSpec;
import com.hfstudio.guidenh.guide.document.block.functiongraph.DomainPredicate;
import com.hfstudio.guidenh.guide.document.block.functiongraph.FunctionExprParser;
import com.hfstudio.guidenh.guide.document.block.functiongraph.FunctionGraphPalette;
import com.hfstudio.guidenh.guide.document.block.functiongraph.FunctionPlot;
import com.hfstudio.guidenh.guide.document.block.functiongraph.MarkedPoint;
import com.hfstudio.guidenh.guide.document.interaction.ItemTooltip;
import com.hfstudio.guidenh.guide.document.interaction.TextTooltip;
import com.hfstudio.guidenh.guide.indices.ItemIndex;
import com.hfstudio.guidenh.guide.internal.GuidebookText;
import com.hfstudio.guidenh.guide.internal.markdown.FileTreeParser;
import com.hfstudio.guidenh.guide.internal.markdown.FileTreeParser.FileTreeEntry;
import com.hfstudio.guidenh.guide.internal.markdown.FileTreeParser.FileTreeIcon;
import com.hfstudio.guidenh.guide.internal.markdown.FileTreeParser.FileTreeModel;
import com.hfstudio.guidenh.guide.internal.markdown.FileTreeParser.SlotKind;
import com.hfstudio.guidenh.guide.internal.mermaid.MermaidMindmapDocument;
import com.hfstudio.guidenh.guide.internal.mermaid.MermaidMindmapNode;
import com.hfstudio.guidenh.guide.internal.mermaid.MermaidMindmapNodeContentExtractor;
import com.hfstudio.guidenh.guide.internal.mermaid.MermaidMindmapParser;
import com.hfstudio.guidenh.guide.internal.util.GuideStringLines;
import com.hfstudio.guidenh.guide.mediawiki.MediaWikiExternalLinkSupport;
import com.hfstudio.guidenh.guide.mediawiki.MediaWikiListContext;
import com.hfstudio.guidenh.guide.mediawiki.MediaWikiListEntry;
import com.hfstudio.guidenh.guide.mediawiki.MediaWikiListPlanner;
import com.hfstudio.guidenh.guide.mediawiki.MediaWikiPageListBuilder;
import com.hfstudio.guidenh.guide.mediawiki.MediaWikiSpecialGroupedEntry;
import com.hfstudio.guidenh.guide.mediawiki.MediaWikiSpecialListEntry;
import com.hfstudio.guidenh.guide.mediawiki.MediaWikiSpecialPageKind;
import com.hfstudio.guidenh.guide.mediawiki.MediaWikiSpecialPageQuery;
import com.hfstudio.guidenh.guide.mediawiki.MediaWikiSpecialPageResolver;
import com.hfstudio.guidenh.guide.mediawiki.MediaWikiSpecialPageResult;
import com.hfstudio.guidenh.guide.navigation.NavigationNode;
import com.hfstudio.guidenh.guide.navigation.NavigationTree;
import com.hfstudio.guidenh.guide.sound.GuideSoundSpec;
import com.hfstudio.guidenh.guide.sound.GuideSoundTrigger;
import com.hfstudio.guidenh.libs.mdast.mdx.model.MdxJsxAttribute;
import com.hfstudio.guidenh.libs.mdast.mdx.model.MdxJsxAttributeNode;
import com.hfstudio.guidenh.libs.mdast.mdx.model.MdxJsxElementFields;
import com.hfstudio.guidenh.libs.mdast.mdx.model.MdxJsxFlowElement;
import com.hfstudio.guidenh.libs.mdast.model.MdAstAnyContent;
import com.hfstudio.guidenh.libs.mdast.model.MdAstBreak;
import com.hfstudio.guidenh.libs.mdast.model.MdAstLiteral;
import com.hfstudio.guidenh.libs.mdast.model.MdAstParent;

public class GuideSiteMdxTagRenderer implements GuideSiteHtmlCompiler.MdxTagRenderer {

    private static final int SPECIAL_PAGES_GROUP_COLUMNS = 2;

    private static final Comparator<StructureBlockView> STRUCTURE_DRAW_ORDER = Comparator
        .comparingInt((StructureBlockView block) -> block.y)
        .thenComparing(
            Comparator.comparingInt((StructureBlockView block) -> block.z)
                .reversed())
        .thenComparingInt(block -> block.x);

    private final Guide guide;
    private final Map<ResourceLocation, ParsedGuidePage> parsedPagesById;
    private final NavigationTree navigationTree;
    @Nullable
    private final GuideSitePageAssetExporter assetExporter;
    @Nullable
    private final Map<ResourceLocation, GuideSitePageAssetExporter> assetExportersByGuideId;
    @Nullable
    private final MediaWikiListContext mediaWikiListContext;
    private final GuideSiteItemIconResolver itemIconResolver;
    @Nullable
    private Map<String, PageAnchor> itemAnchorsByItemId;
    private final MediaWikiSpecialPageResolver specialPageResolver = new MediaWikiSpecialPageResolver();

    public GuideSiteMdxTagRenderer(Guide guide, Map<ResourceLocation, ParsedGuidePage> parsedPagesById,
        NavigationTree navigationTree) {
        this(guide, parsedPagesById, navigationTree, null, GuideSiteItemIconResolver.NONE, null, null);
    }

    public GuideSiteMdxTagRenderer(Guide guide, Map<ResourceLocation, ParsedGuidePage> parsedPagesById,
        NavigationTree navigationTree, GuideSiteItemIconResolver itemIconResolver) {
        this(guide, parsedPagesById, navigationTree, null, itemIconResolver, null, null);
    }

    public GuideSiteMdxTagRenderer(Guide guide, Map<ResourceLocation, ParsedGuidePage> parsedPagesById,
        NavigationTree navigationTree, @Nullable GuideSitePageAssetExporter assetExporter,
        GuideSiteItemIconResolver itemIconResolver) {
        this(guide, parsedPagesById, navigationTree, assetExporter, itemIconResolver, null, null);
    }

    public GuideSiteMdxTagRenderer(Guide guide, Map<ResourceLocation, ParsedGuidePage> parsedPagesById,
        NavigationTree navigationTree, @Nullable GuideSitePageAssetExporter assetExporter,
        GuideSiteItemIconResolver itemIconResolver,
        @Nullable Map<ResourceLocation, GuideSitePageAssetExporter> assetExportersByGuideId) {
        this(guide, parsedPagesById, navigationTree, assetExporter, itemIconResolver, assetExportersByGuideId, null);
    }

    public GuideSiteMdxTagRenderer(Guide guide, Map<ResourceLocation, ParsedGuidePage> parsedPagesById,
        NavigationTree navigationTree, @Nullable GuideSitePageAssetExporter assetExporter,
        GuideSiteItemIconResolver itemIconResolver,
        @Nullable Map<ResourceLocation, GuideSitePageAssetExporter> assetExportersByGuideId,
        @Nullable MediaWikiListContext mediaWikiListContext) {
        this.guide = guide;
        this.parsedPagesById = parsedPagesById;
        this.navigationTree = navigationTree;
        this.assetExporter = assetExporter;
        this.assetExportersByGuideId = assetExportersByGuideId;
        this.mediaWikiListContext = mediaWikiListContext;
        this.itemIconResolver = itemIconResolver != null ? itemIconResolver : GuideSiteItemIconResolver.NONE;
    }

    @Override
    public @Nullable String render(MdxJsxElementFields element, String defaultNamespace,
        @Nullable ResourceLocation currentPageId, GuideSiteTemplateRegistry templates,
        GuideSiteHtmlCompiler.SceneResolver sceneResolver, GuideSiteHtmlCompiler compiler) {
        String name = element.name();
        if ("ItemImage".equals(name)) {
            return renderItemImage(element, defaultNamespace, currentPageId, templates, true);
        }
        if ("BlockImage".equals(name)) {
            return renderBlockImage(element, defaultNamespace, currentPageId, templates, sceneResolver);
        }
        if ("ItemGrid".equals(name)) {
            return renderItemGrid(element, defaultNamespace, currentPageId, templates);
        }
        if ("ItemLink".equals(name)) {
            return renderItemLink(element, defaultNamespace, currentPageId, templates);
        }
        if ("SubPages".equals(name)) {
            return renderSubPages(element, defaultNamespace, currentPageId);
        }
        if ("Category".equals(name)) {
            return renderCategory(element, currentPageId);
        }
        if ("Special".equals(name)) {
            return renderSpecial(element, currentPageId);
        }
        if ("Color".equals(name)) {
            return renderColor(element, defaultNamespace, currentPageId, templates, sceneResolver, compiler);
        }
        if ("mark".equals(name)) {
            return renderMark(element, defaultNamespace, currentPageId, templates, sceneResolver, compiler);
        }
        if ("Row".equals(name)) {
            return renderLayoutBox(element, defaultNamespace, currentPageId, templates, sceneResolver, compiler, true);
        }
        if ("Column".equals(name)) {
            return renderLayoutBox(element, defaultNamespace, currentPageId, templates, sceneResolver, compiler, false);
        }
        if ("Structure".equals(name)) {
            return renderStructure(element, currentPageId, templates);
        }
        if ("br".equals(name)) {
            return "<br>";
        }
        if ("PlayerName".equals(name)) {
            return escapeHtml(resolvePlayerName());
        }
        if ("KeyBind".equals(name)) {
            return escapeHtml(resolveKeyBindLabel(element));
        }
        if ("Comment".equals(name)) {
            return "";
        }
        if ("CommandLink".equals(name)) {
            return renderCommandLink(element, defaultNamespace, currentPageId, templates, sceneResolver, compiler);
        }
        if ("SoundLink".equals(name)) {
            return renderSoundLink(element, defaultNamespace, currentPageId, templates, sceneResolver, compiler);
        }
        if ("div".equals(name)) {
            return "<div>"
                + compiler
                    .compileFragment(element.children(), templates, defaultNamespace, sceneResolver, currentPageId)
                + "</div>";
        }
        if ("kbd".equals(name)) {
            return "<kbd>"
                + compiler
                    .compileFragment(element.children(), templates, defaultNamespace, sceneResolver, currentPageId)
                + "</kbd>";
        }
        if ("sub".equals(name)) {
            return "<sub>"
                + compiler
                    .compileFragment(element.children(), templates, defaultNamespace, sceneResolver, currentPageId)
                + "</sub>";
        }
        if ("sup".equals(name)) {
            return "<sup>"
                + compiler
                    .compileFragment(element.children(), templates, defaultNamespace, sceneResolver, currentPageId)
                + "</sup>";
        }
        if ("details".equals(name)) {
            return renderDetails(element, defaultNamespace, currentPageId, templates, sceneResolver, compiler);
        }
        if ("FileTree".equals(name)) {
            return renderFileTree(element, defaultNamespace, currentPageId, templates, sceneResolver, compiler);
        }
        if ("FootnoteList".equals(name)) {
            return "<div class=\"guide-footnote-list\">"
                + compiler
                    .compileFragment(element.children(), templates, defaultNamespace, sceneResolver, currentPageId)
                + "</div>";
        }
        if ("Mermaid".equals(name)) {
            return renderMermaid(element, defaultNamespace, currentPageId, templates, sceneResolver, compiler);
        }
        if ("CsvTable".equals(name)) {
            return renderCsvTable(element, currentPageId);
        }
        if ("ColumnChart".equals(name)) {
            return renderColumnChart(element);
        }
        if ("BarChart".equals(name)) {
            return renderBarChart(element);
        }
        if ("LineChart".equals(name)) {
            return renderLineChart(element);
        }
        if ("PieChart".equals(name)) {
            return renderPieChart(element);
        }
        if ("ScatterChart".equals(name)) {
            return renderScatterChart(element);
        }
        if ("FunctionGraph".equals(name) || "Function".equals(name)) {
            return renderFunctionGraphTag(element);
        }
        if ("QuestLink".equals(name)) {
            return renderQuestLink(element);
        }
        if ("QuestCard".equals(name)) {
            return renderQuestCard(element);
        }
        return null;
    }

    private String renderQuestLink(MdxJsxElementFields element) {
        String id = readOptional(element, "id");
        if (id == null || id.trim()
            .isEmpty()) {
            return renderError("QuestLink requires an id");
        }
        String text = readOptional(element, "text");
        String label = text != null && !text.trim()
            .isEmpty() ? text : "Quest " + id;
        return "<span class=\"guide-quest-link\" data-quest-id=\"" + escapeAttribute(id)
            + "\">"
            + escapeHtml(label)
            + "</span>";
    }

    private String renderSoundLink(MdxJsxElementFields element, String defaultNamespace,
        @Nullable ResourceLocation currentPageId, GuideSiteTemplateRegistry templates,
        GuideSiteHtmlCompiler.SceneResolver sceneResolver, GuideSiteHtmlCompiler compiler) {
        String body = compiler
            .compileInlineFragment(element.children(), templates, defaultNamespace, sceneResolver, currentPageId);
        GuideSoundSpec sound = GuideSiteSoundExport
            .parse(name -> readOptional(element, name), defaultNamespace, currentPageId);
        if (sound == null) {
            return "<span class=\"guide-sound-link\">" + body + "</span>";
        }
        String src = GuideSiteSoundExport
            .exportSource(sound, name -> readOptional(element, name), currentPageId, assetExporter);
        GuideSoundTrigger trigger = GuideSoundTrigger.parse(readOptional(element, "trigger"), GuideSoundTrigger.CLICK);
        StringBuilder html = new StringBuilder();
        html.append("<span class=\"guide-sound-link\" tabindex=\"0\" role=\"button\"");
        GuideSiteSoundExport.appendDataAttributes(html, sound, trigger, src, this::escapeAttribute);
        html.append(">")
            .append(body)
            .append("</span>");
        return html.toString();
    }

    private String renderQuestCard(MdxJsxElementFields element) {
        String id = readOptional(element, "id");
        if (id == null || id.trim()
            .isEmpty()) {
            return renderError("QuestCard requires an id");
        }
        boolean showDesc = readBoolean(element, "show_desc", true);
        String title = readOptional(element, "title");
        if (title == null || title.trim()
            .isEmpty()) {
            title = "Quest " + id;
        }
        StringBuilder html = new StringBuilder();
        html.append("<div class=\"guide-quest-card\" data-quest-id=\"")
            .append(escapeAttribute(id))
            .append("\"><div class=\"guide-quest-card-title\">")
            .append(escapeHtml(title))
            .append("</div>");
        if (showDesc) {
            html.append("<div class=\"guide-quest-card-meta\">")
                .append(escapeHtml(id))
                .append("</div>");
        }
        html.append("</div>");
        return html.toString();
    }

    private String renderColor(MdxJsxElementFields element, String defaultNamespace,
        @Nullable ResourceLocation currentPageId, GuideSiteTemplateRegistry templates,
        GuideSiteHtmlCompiler.SceneResolver sceneResolver, GuideSiteHtmlCompiler compiler) {
        String color = resolveCssColor(element, defaultNamespace);
        if (color == null) {
            return renderError("Malformed color value");
        }

        return "<span class=\"guide-inline-color\" style=\"color:" + escapeAttribute(color)
            + ";\">"
            + compiler.compileFragment(element.children(), templates, defaultNamespace, sceneResolver, currentPageId)
            + "</span>";
    }

    private String renderMark(MdxJsxElementFields element, String defaultNamespace,
        @Nullable ResourceLocation currentPageId, GuideSiteTemplateRegistry templates,
        GuideSiteHtmlCompiler.SceneResolver sceneResolver, GuideSiteHtmlCompiler compiler) {
        String rawColor = readOptional(element, "color");
        String color = rawColor != null ? parseLiteralColor(rawColor) : null;
        if (color == null) {
            color = toCssColor(new ConstantColor(PageCompiler.DEFAULT_MARK_BACKGROUND_COLOR));
        }

        return "<mark class=\"guide-mark\" style=\"background-color:" + escapeAttribute(color)
            + ";\">"
            + compiler.compileFragment(element.children(), templates, defaultNamespace, sceneResolver, currentPageId)
            + "</mark>";
    }

    private String renderLayoutBox(MdxJsxElementFields element, String defaultNamespace,
        @Nullable ResourceLocation currentPageId, GuideSiteTemplateRegistry templates,
        GuideSiteHtmlCompiler.SceneResolver sceneResolver, GuideSiteHtmlCompiler compiler, boolean row) {
        StringBuilder style = new StringBuilder();
        style.append("--guide-layout-gap:")
            .append(Math.max(0, readInt(element, "gap", 5)))
            .append("px;");
        style.append("align-items:")
            .append(resolveFlexAlignment(readOptional(element, "alignItems")))
            .append(";");
        if (readBoolean(element, "fullWidth", false)) {
            style.append("width:100%;");
        }

        String body = compiler
            .compileFragment(element.children(), templates, defaultNamespace, sceneResolver, currentPageId);
        return "<div class=\"guide-layout guide-layout-" + (row ? "row" : "column")
            + "\" style=\""
            + escapeAttribute(style.toString())
            + "\">"
            + body
            + "</div>";
    }

    private String renderStructure(MdxJsxElementFields element, @Nullable ResourceLocation currentPageId,
        GuideSiteTemplateRegistry templates) {
        int width = Math.max(32, readInt(element, "width", LytStructureView.DEFAULT_WIDTH));
        int height = Math.max(32, readInt(element, "height", LytStructureView.DEFAULT_HEIGHT));

        List<StructureBlockView> blocks = parseStructureBlocks(element, currentPageId, templates);
        if (blocks == null) {
            return renderError("Malformed structure");
        }

        StringBuilder html = new StringBuilder();
        html.append("<div class=\"guide-structure-view\" style=\"width:")
            .append(width)
            .append("px;max-width:100%;\">");
        html.append("<div class=\"guide-structure-stage\" style=\"height:")
            .append(height)
            .append("px;\">");

        if (blocks.isEmpty()) {
            html.append("<div class=\"guide-structure-empty\">No blocks</div>");
            html.append("</div></div>");
            return html.toString();
        }

        int sxMin = Integer.MAX_VALUE;
        int sxMax = Integer.MIN_VALUE;
        int syMin = Integer.MAX_VALUE;
        int syMax = Integer.MIN_VALUE;
        int maxZ = Integer.MIN_VALUE;
        for (StructureBlockView block : blocks) {
            if (block.projectedX < sxMin) sxMin = block.projectedX;
            if (block.projectedX + LytStructureView.ICON > sxMax) sxMax = block.projectedX + LytStructureView.ICON;
            if (block.projectedY < syMin) syMin = block.projectedY;
            if (block.projectedY + LytStructureView.ICON > syMax) syMax = block.projectedY + LytStructureView.ICON;
            if (block.z > maxZ) maxZ = block.z;
        }

        int contentWidth = sxMax - sxMin;
        int contentHeight = syMax - syMin;
        int offsetX = (width - contentWidth) / 2 - sxMin;
        int offsetY = (height - contentHeight) / 2 - syMin;

        Collections.sort(blocks, STRUCTURE_DRAW_ORDER);
        LinkedHashMap<String, StructureLegendEntry> legendEntries = new LinkedHashMap<>();

        for (StructureBlockView block : blocks) {
            int left = block.projectedX + offsetX + LytStructureView.ICON / 2;
            int top = block.projectedY + offsetY + LytStructureView.ICON / 2;
            int zIndex = 1000 + block.y * 100 + (maxZ - block.z) * 10 + block.x;

            html.append("<span class=\"guide-structure-block");
            if (block.templateId != null) {
                html.append(" guide-tooltip");
            }
            html.append("\" style=\"left:")
                .append(left)
                .append("px;top:")
                .append(top)
                .append("px;z-index:")
                .append(zIndex)
                .append(";\"");
            if (block.templateId != null) {
                html.append(" data-template=\"")
                    .append(escapeAttribute(block.templateId))
                    .append("\"");
            }
            html.append("><span class=\"guide-structure-block-face\">");
            if (block.item.hasIcon()) {
                GuideSiteItemHtml.appendIcon(html, block.item, "guide-structure-block-icon");
            } else {
                html.append("<span class=\"guide-structure-block-label\">")
                    .append(escapeHtml(block.abbreviation))
                    .append("</span>");
            }
            html.append("</span></span>");

            StructureLegendEntry legendEntry = legendEntries.get(block.item.itemId());
            if (legendEntry == null) {
                legendEntries.put(
                    block.item.itemId(),
                    new StructureLegendEntry(block.item, block.abbreviation, block.templateId));
            } else {
                legendEntry.count++;
            }
        }

        html.append("</div>");
        html.append("<div class=\"guide-structure-legend\">");
        for (StructureLegendEntry legendEntry : legendEntries.values()) {
            html.append("<span class=\"guide-structure-legend-item");
            if (legendEntry.templateId != null) {
                html.append(" guide-tooltip");
            }
            html.append("\"");
            if (legendEntry.templateId != null) {
                html.append(" data-template=\"")
                    .append(escapeAttribute(legendEntry.templateId))
                    .append("\"");
            }
            html.append("><span class=\"guide-structure-legend-swatch\">");
            if (legendEntry.item.hasIcon()) {
                GuideSiteItemHtml.appendIcon(html, legendEntry.item, "guide-structure-legend-icon");
            } else {
                html.append(escapeHtml(legendEntry.abbreviation));
            }
            html.append("</span><span class=\"guide-structure-legend-name\">")
                .append(escapeHtml(legendEntry.item.displayName()))
                .append("</span><span class=\"guide-structure-legend-count\">x")
                .append(legendEntry.count)
                .append("</span></span>");
        }
        html.append("</div></div>");
        return html.toString();
    }

    private String renderItemImage(MdxJsxElementFields element, String defaultNamespace,
        @Nullable ResourceLocation currentPageId, GuideSiteTemplateRegistry templates, boolean inline) {
        String rawId = readOptional(element, "id");
        String rawOre = readOptional(element, "ore");
        GuideItemReferenceResolver.ResolvedItemReference item = GuideItemReferenceResolver
            .resolveItemReference(defaultNamespace, rawId, rawOre);
        String itemId = resolveItemLabelKey(defaultNamespace, rawId, rawOre, item);
        if (item == null || item.stack() == null) {
            return renderFallbackItemLabel(itemId, currentPageId, templates, inline, readFloat(element, "scale"));
        }

        MdxJsxAttribute noTooltipAttribute = element.getAttribute("noTooltip");
        MdxJsxAttribute showTooltipAttribute = element.getAttribute("showTooltip");
        boolean noTooltip = readBoolean(noTooltipAttribute, "noTooltip", false);
        boolean includeTooltip = showTooltipAttribute != null ? readBoolean(showTooltipAttribute, "showTooltip", true)
            : !noTooltip;

        MdxJsxAttribute showIconAttribute = element.getAttribute("showIcon");
        String showIconRaw = readOptional(showIconAttribute);
        boolean showIcon = showIconRaw == null || readBoolean(showIconAttribute, "showIcon", true);

        String labelRaw = readOptional(element, "label");
        String labelPosition = ItemImageCompiler.resolveLabelPosition(labelRaw);

        String formatRaw = readOptional(element, "format");

        return renderItemStack(
            item.registryId(),
            item.stack(),
            currentPageId,
            templates,
            inline,
            readFloat(element, "scale"),
            includeTooltip,
            showIcon,
            labelPosition,
            formatRaw);
    }

    private String renderItemStack(ResourceLocation registryId, ItemStack stack,
        @Nullable ResourceLocation currentPageId, GuideSiteTemplateRegistry templates, boolean inline,
        @Nullable Float scale, boolean includeTooltip) {
        return renderItemStack(
            registryId,
            stack,
            currentPageId,
            templates,
            inline,
            scale,
            includeTooltip,
            true,
            null,
            null);
    }

    private String renderItemStack(ResourceLocation registryId, ItemStack stack,
        @Nullable ResourceLocation currentPageId, GuideSiteTemplateRegistry templates, boolean inline,
        @Nullable Float scale, boolean includeTooltip, boolean showIcon, @Nullable String labelPosition,
        @Nullable String labelFormat) {
        if (registryId == null || stack == null) {
            return renderError("Missing item");
        }

        GuideSiteExportedItem exportedItem = GuideSiteItemSupport
            .export(registryId, stack, itemIconResolver, registryId.toString());
        String templateId = null;
        if (includeTooltip) {
            templateId = createTooltipTemplate(new ItemTooltip(stack), templates, currentPageId);
        }

        StringBuilder classes = new StringBuilder(inline ? "guide-inline-item" : "guide-block-item");
        if (templateId != null) {
            classes.append(" guide-tooltip");
        }

        // The wrapper still receives a font-size hint so descendants that derive sizing from `em`
        // continue to scale, but the actual icon size is now baked into the <img> width/height by
        // GuideSiteItemHtml.appendIcon (see scale parameter below) so the resulting image really
        // grows / shrinks with the MDX `scale` attribute.
        float effectiveScale = scale != null && scale > 0f ? scale : 1f;
        StringBuilder style = new StringBuilder();
        if (effectiveScale != 1.0f) {
            style.append("font-size:")
                .append(effectiveScale)
                .append("em;");
        }

        String labelText = labelPosition != null ? buildLabelHtml(stack, labelFormat) : null;

        StringBuilder html = new StringBuilder();
        if (!inline) {
            html.append("<div class=\"guide-block-item-row\">");
        }
        html.append("<span class=\"")
            .append(escapeAttribute(classes.toString()))
            .append("\" data-item-id=\"")
            .append(escapeAttribute(exportedItem.itemId()))
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
        html.append(">");
        if (labelText != null && "left".equals(labelPosition)) {
            html.append("<em class=\"guide-item-label\">")
                .append(labelText)
                .append("</em>");
        }
        if (showIcon) {
            GuideSiteItemHtml.appendIcon(
                html,
                exportedItem,
                inline ? "guide-inline-item-icon" : "guide-block-item-icon",
                effectiveScale);
        }
        if (labelText != null && !"left".equals(labelPosition)) {
            html.append("<em class=\"guide-item-label\">")
                .append(labelText)
                .append("</em>");
        }
        html.append("</span>");
        if (!inline) {
            html.append("</div>");
        }
        return html.toString();
    }

    /**
     * Builds the HTML label text for an item, applying the optional format pattern.
     * Markdown-style wrapping markers are converted to HTML inline elements.
     */
    private String buildLabelHtml(ItemStack stack, @Nullable String format) {
        if (format == null) {
            return "<em>" + escapeHtml(stack.getDisplayName()) + "</em>";
        }
        String template = stripFormatMarkersHtml(format);
        String text = template.contains("%s") ? String.format(template, stack.getDisplayName()) : template;
        return applyFormatMarkersHtml(format, escapeHtml(text));
    }

    private String stripFormatMarkersHtml(String s) {
        boolean changed = true;
        while (changed) {
            changed = false;
            if (isHtmlWrapped(s, "~~")) {
                s = s.substring(2, s.length() - 2);
                changed = true;
            } else if (isHtmlWrapped(s, "**")) {
                s = s.substring(2, s.length() - 2);
                changed = true;
            } else if (isHtmlWrapped(s, "__")) {
                s = s.substring(2, s.length() - 2);
                changed = true;
            } else if (isHtmlWrapped(s, "^^")) {
                s = s.substring(2, s.length() - 2);
                changed = true;
            } else if (isHtmlWrapped(s, "::")) {
                s = s.substring(2, s.length() - 2);
                changed = true;
            } else if (isHtmlWrapped(s, "++")) {
                s = s.substring(2, s.length() - 2);
                changed = true;
            } else if (isHtmlWrapped(s, "*")) {
                s = s.substring(1, s.length() - 1);
                changed = true;
            } else if (isHtmlWrapped(s, "_")) {
                s = s.substring(1, s.length() - 1);
                changed = true;
            }
        }
        return s;
    }

    private String applyFormatMarkersHtml(String format, String escapedText) {
        String open = "";
        String close = "";
        String s = format;
        boolean changed = true;
        while (changed) {
            changed = false;
            if (isHtmlWrapped(s, "~~")) {
                open = "<s>" + open;
                close = close + "</s>";
                s = s.substring(2, s.length() - 2);
                changed = true;
            } else if (isHtmlWrapped(s, "**")) {
                open = "<strong>" + open;
                close = close + "</strong>";
                s = s.substring(2, s.length() - 2);
                changed = true;
            } else if (isHtmlWrapped(s, "__") || isHtmlWrapped(s, "++")) {
                open = "<u>" + open;
                close = close + "</u>";
                s = s.substring(2, s.length() - 2);
                changed = true;
            } else if (isHtmlWrapped(s, "^^")) {
                open = "<span style=\"text-decoration:underline wavy\">" + open;
                close = close + "</span>";
                s = s.substring(2, s.length() - 2);
                changed = true;
            } else if (isHtmlWrapped(s, "::")) {
                open = "<span style=\"text-decoration:underline dotted\">" + open;
                close = close + "</span>";
                s = s.substring(2, s.length() - 2);
                changed = true;
            } else if (isHtmlWrapped(s, "*") || isHtmlWrapped(s, "_")) {
                open = "<em>" + open;
                close = close + "</em>";
                s = s.substring(1, s.length() - 1);
                changed = true;
            }
        }
        return open + escapedText + close;
    }

    private static boolean isHtmlWrapped(String s, String marker) {
        return s.length() > 2 * marker.length() && s.startsWith(marker) && s.endsWith(marker);
    }

    private String renderBlockImage(MdxJsxElementFields element, String defaultNamespace,
        @Nullable ResourceLocation currentPageId, GuideSiteTemplateRegistry templates,
        GuideSiteHtmlCompiler.SceneResolver sceneResolver) {
        String rawId = readOptional(element, "id");
        String rawOre = readOptional(element, "ore");
        GuideItemReferenceResolver.ResolvedBlockReference block = GuideItemReferenceResolver
            .resolveBlockReference(defaultNamespace, rawId, rawOre);
        if (block == null || block.stack() == null
            || block.stack()
                .getItem() == null) {
            return renderFallbackItemLabel(
                resolveItemLabelKey(defaultNamespace, rawId, rawOre, null),
                currentPageId,
                templates,
                false,
                readFloat(element, "scale"));
        }

        GuideSiteExportedScene exportedScene = sceneResolver.nextScene();
        if (exportedScene != null) {
            int logicalWidth = exportedScene.logicalWidth() > 0 ? exportedScene.logicalWidth() : 256;
            int logicalHeight = exportedScene.logicalHeight() > 0 ? exportedScene.logicalHeight() : 192;
            String sceneHtml = GuideSiteSceneTagRenderer
                .renderSceneHtml(logicalWidth, logicalHeight, false, defaultNamespace, null, exportedScene);
            return wrapBlockImageFloat(element, sceneHtml);
        }

        return renderItemStack(
            block.registryId(),
            block.stack(),
            currentPageId,
            templates,
            false,
            readFloat(element, "scale"),
            true);
    }

    private String wrapBlockImageFloat(MdxJsxElementFields element, String html) {
        String floatDirection = resolveBlockFloatDirection(element);
        if (floatDirection == null) {
            return html;
        }

        String style = "right".equals(floatDirection) ? "float:right;margin:0 0 5px 5px;"
            : "float:left;margin:0 5px 5px 0;";
        return "<span class=\"guide-floating-image-wrap\" style=\"" + escapeAttribute(style) + "\">" + html + "</span>";
    }

    @Nullable
    private String resolveBlockFloatDirection(MdxJsxElementFields element) {
        String wrap = readOptional(element, "wrap");
        if ("square".equalsIgnoreCase(wrap) || "tight".equalsIgnoreCase(wrap) || "through".equalsIgnoreCase(wrap)) {
            String align = readOptional(element, "align");
            return "right".equalsIgnoreCase(align) ? "right" : "left";
        }

        String legacyFloat = readOptional(element, "float");
        if ("left".equalsIgnoreCase(legacyFloat) || "right".equalsIgnoreCase(legacyFloat)) {
            return legacyFloat.toLowerCase(Locale.ROOT);
        }
        return null;
    }

    private String renderItemGrid(MdxJsxElementFields element, String defaultNamespace,
        @Nullable ResourceLocation currentPageId, GuideSiteTemplateRegistry templates) {
        StringBuilder html = new StringBuilder();
        html.append("<div class=\"guide-item-grid\">");
        for (var child : element.children()) {
            if (!(child instanceof MdxJsxElementFields itemChild) || !"ItemIcon".equals(itemChild.name())) {
                continue;
            }
            String rawId = readOptional(itemChild, "id");
            String rawOre = readOptional(itemChild, "ore");
            GuideItemReferenceResolver.ResolvedItemReference item = GuideItemReferenceResolver
                .resolveItemReference(defaultNamespace, rawId, rawOre);
            String itemId = resolveItemLabelKey(defaultNamespace, rawId, rawOre, item);
            GuideSiteExportedItem exportedItem = item != null && item.stack() != null
                ? GuideSiteItemSupport.export(item.registryId(), item.stack(), itemIconResolver, itemId)
                : GuideSiteItemSupport.unresolved(itemId);
            String templateId = item != null && item.stack() != null
                ? createTooltipTemplate(new ItemTooltip(item.stack()), templates, currentPageId)
                : createTextTooltipTemplate(itemId, templates, currentPageId);
            html.append("<div class=\"ingredient-box\">");
            html.append("<span class=\"guide-item-label");
            if (templateId != null) {
                html.append(" guide-tooltip");
            }
            html.append("\" data-item-id=\"")
                .append(escapeAttribute(exportedItem.itemId()))
                .append("\"");
            if (templateId != null) {
                html.append(" data-template=\"")
                    .append(escapeAttribute(templateId))
                    .append("\"");
            }
            html.append(">");
            GuideSiteItemHtml
                .appendSummaryContent(html, exportedItem, "guide-item-label-icon", "guide-item-label-text");
            html.append("</span></div>");
        }
        html.append("</div>");
        return html.toString();
    }

    private String renderItemLink(MdxJsxElementFields element, String defaultNamespace,
        @Nullable ResourceLocation currentPageId, GuideSiteTemplateRegistry templates) {
        String rawId = readOptional(element, "id");
        String rawOre = readOptional(element, "ore");
        GuideItemReferenceResolver.ResolvedItemReference item = GuideItemReferenceResolver
            .resolveItemReference(defaultNamespace, rawId, rawOre);
        String itemId = resolveItemLabelKey(defaultNamespace, rawId, rawOre, item);
        GuideSiteExportedItem exportedItem = item != null && item.stack() != null
            ? GuideSiteItemSupport.export(item.registryId(), item.stack(), itemIconResolver, itemId)
            : GuideSiteItemSupport.unresolved(itemId);

        // showTooltip — default true for ItemLink
        MdxJsxAttribute noTooltipAttribute = element.getAttribute("noTooltip");
        MdxJsxAttribute showTooltipAttribute = element.getAttribute("showTooltip");
        boolean noTooltip = readBoolean(noTooltipAttribute, "noTooltip", false);
        boolean showTooltip = showTooltipAttribute != null ? readBoolean(showTooltipAttribute, "showTooltip", true)
            : !noTooltip;

        // showIcon — null/falsy = no icon; "left", "right", or any truthy = icon at that side
        String iconPosition = ItemImageCompiler.resolveLabelPosition(readOptional(element.getAttribute("showIcon")));

        String templateId = null;
        if (showTooltip) {
            templateId = item != null && item.stack() != null
                ? createTooltipTemplate(new ItemTooltip(item.stack()), templates, currentPageId)
                : createTextTooltipTemplate(itemId, templates, currentPageId);
        }

        PageAnchor linksTo = null;
        try {
            if (item != null && item.stack() != null) {
                linksTo = guide.getIndex(ItemIndex.class)
                    .findByStack(item.stack());
            }
        } catch (Exception ignored) {}
        if (linksTo == null) {
            linksTo = findPageAnchorByItemId(itemId);
        }

        String innerHtml = buildItemLinkContent(exportedItem, iconPosition);
        boolean samePageLink = linksTo != null && linksTo.anchor() == null
            && currentPageId != null
            && currentPageId.equals(linksTo.pageId());
        if (linksTo == null || samePageLink) {
            return buildTaggedSpanHtml("guide-item-link guide-tooltip", templateId, innerHtml);
        }

        NavigationNode targetNode = navigationTree.getNodeById(linksTo.pageId());
        ResourceLocation targetGuideId = targetNode != null ? targetNode.guideId() : null;

        return buildTaggedAnchorHtml(
            "guide-item-link guide-tooltip",
            GuideSiteHrefResolver.resolvePageAnchor(currentPageId, targetGuideId, linksTo),
            templateId,
            innerHtml);
    }

    private String buildItemLinkContent(GuideSiteExportedItem item, @Nullable String iconPosition) {
        StringBuilder html = new StringBuilder();
        if ("left".equals(iconPosition)) {
            GuideSiteItemHtml.appendIcon(html, item, "guide-item-link-icon");
        }
        String name = item.displayName()
            .isEmpty() ? item.itemId() : item.displayName();
        html.append("<span class=\"guide-item-link-text\">")
            .append(escapeHtml(name))
            .append("</span>");
        if ("right".equals(iconPosition)) {
            GuideSiteItemHtml.appendIcon(html, item, "guide-item-link-icon");
        }
        return html.toString();
    }

    private String renderSubPages(MdxJsxElementFields element, String defaultNamespace,
        @Nullable ResourceLocation currentPageId) {
        String pageIdStr = readOptional(element, "id");
        boolean alphabetical = readBoolean(element, "alphabetical", false);

        List<NavigationNode> nodes;
        if ("".equals(pageIdStr)) {
            nodes = navigationTree.getRootNodes();
        } else {
            ResourceLocation pageId;
            try {
                pageId = pageIdStr == null || pageIdStr.isEmpty() ? currentPageId
                    : new ResourceLocation(pageIdStr.contains(":") ? pageIdStr : defaultNamespace + ":" + pageIdStr);
            } catch (Exception e) {
                return renderError("Invalid page id");
            }
            if (pageId == null) {
                return renderError("Missing current page");
            }
            NavigationNode node = navigationTree.getNodeById(pageId);
            if (node == null) {
                return renderError("Unknown navigation page");
            }
            nodes = node.children();
        }

        List<NavigationNode> sorted = new ArrayList<>(nodes);
        if (alphabetical) {
            sorted.sort(SubPagesCompiler.ALPHABETICAL_COMPARATOR);
        }
        return renderNavigationNodeList(sorted, currentPageId);
    }

    private String renderCategory(MdxJsxElementFields element, @Nullable ResourceLocation currentPageId) {
        String categoryName = readOptional(element, "name");
        if (categoryName == null || categoryName.trim()
            .isEmpty()) {
            return renderError(GuidebookText.MediaWikiMissingCategoryName.text());
        }

        if (mediaWikiListContext == null) {
            return renderError(GuidebookText.MediaWikiCategoryPageUnavailable.text());
        }

        List<MediaWikiListEntry> entries = MediaWikiPageListBuilder
            .buildCategoryMembers(mediaWikiListContext, categoryName.trim());
        return renderMediaWikiEntryList(
            entries,
            readInt(element, "rows", MediaWikiListPlanner.DEFAULT_ROWS),
            currentPageId,
            GuidebookText.MediaWikiNoPagesInCategory.text());
    }

    private String renderSpecial(MdxJsxElementFields element, @Nullable ResourceLocation currentPageId) {
        String rawSpecialName = readOptional(element, "name");
        if (rawSpecialName == null || rawSpecialName.trim()
            .isEmpty()) {
            return renderError(GuidebookText.MediaWikiMissingSpecialPageName.text());
        }
        String specialName = specialPageResolver.normalizeSupportedName(rawSpecialName);
        if (specialName == null) {
            return renderError(GuidebookText.MediaWikiUnsupportedSpecialPage.text(rawSpecialName));
        }

        if (mediaWikiListContext == null) {
            return renderError(GuidebookText.MediaWikiSpecialPageUnavailable.text());
        }

        MediaWikiSpecialPageQuery specialQuery = buildSpecialPageQuery(element, Integer.MAX_VALUE);
        MediaWikiSpecialPageResult result = specialPageResolver
            .resolve(mediaWikiListContext, specialName, specialQuery);
        return renderSpecialResult(
            result,
            readInt(element, "rows", MediaWikiListPlanner.DEFAULT_ROWS),
            currentPageId,
            specialName);
    }

    private String renderCommandLink(MdxJsxElementFields element, String defaultNamespace,
        @Nullable ResourceLocation currentPageId, GuideSiteTemplateRegistry templates,
        GuideSiteHtmlCompiler.SceneResolver sceneResolver, GuideSiteHtmlCompiler compiler) {
        String command = readOptional(element, "command");
        if (command == null || command.isEmpty()) {
            return renderError("Missing command");
        }

        String content = compiler
            .compileFragment(element.children(), templates, defaultNamespace, sceneResolver, currentPageId);
        if (content == null || content.isEmpty()) {
            content = escapeHtml(stripLegacyFormatting(command));
        }
        String templateId = templates.create(
            GuideSiteSceneAnnotationSerializer.renderTooltipHtml(
                CommandLinkCompiler.buildTooltip(readOptional(element, "title"), command),
                currentPageId,
                null,
                itemIconResolver,
                templates));
        return "<span class=\"guide-command-link guide-tooltip\" data-template=\"" + escapeAttribute(templateId)
            + "\">"
            + content
            + "</span>";
    }

    private String renderDetails(MdxJsxElementFields element, String defaultNamespace,
        @Nullable ResourceLocation currentPageId, GuideSiteTemplateRegistry templates,
        GuideSiteHtmlCompiler.SceneResolver sceneResolver, GuideSiteHtmlCompiler compiler) {
        boolean open = readOptional(element, "open") != null;
        String detailsStyle = buildDetailsStyle(element);
        String bodyStyle = buildDetailsBodyStyle(element);
        StringBuilder html = new StringBuilder();
        html.append("<details class=\"guide-details\"");
        if (!detailsStyle.isEmpty()) {
            html.append(" style=\"")
                .append(escapeAttribute(detailsStyle))
                .append("\"");
        }
        if (open) {
            html.append(" open");
        }
        html.append(">");
        List<? extends MdAstAnyContent> children = element.children();
        int bodyStart = 0;
        if (!children.isEmpty() && children.get(0) instanceof MdxJsxElementFields summaryEl
            && "summary".equals(summaryEl.name())) {
            String summaryBody = compiler
                .compileFragment(summaryEl.children(), templates, defaultNamespace, sceneResolver, currentPageId);
            html.append("<summary>")
                .append(summaryBody)
                .append("</summary>");
            bodyStart = 1;
        } else {
            html.append("<summary>")
                .append(escapeHtml(GuidebookText.GuideEditorDetails.text()))
                .append("</summary>");
        }
        html.append("<div class=\"guide-details-body\"");
        if (!bodyStyle.isEmpty()) {
            html.append(" style=\"")
                .append(escapeAttribute(bodyStyle))
                .append("\"");
        }
        html.append(">");
        html.append(
            compiler.compileFragment(
                children.subList(bodyStart, children.size()),
                templates,
                defaultNamespace,
                sceneResolver,
                currentPageId));
        html.append("</div>");
        html.append("</details>");
        return html.toString();
    }

    private String renderFileTree(MdxJsxElementFields element, String defaultNamespace,
        @Nullable ResourceLocation currentPageId, GuideSiteTemplateRegistry templates,
        GuideSiteHtmlCompiler.SceneResolver sceneResolver, GuideSiteHtmlCompiler compiler) {
        StringBuilder text = new StringBuilder();
        collectStructureText(text, element.children());
        FileTreeModel model = FileTreeParser.parse(
            text.toString()
                .trim());
        StringBuilder html = new StringBuilder();
        html.append("<div class=\"guide-file-tree\">");
        for (FileTreeEntry entry : model.entries()) {
            html.append("<div class=\"guide-file-tree-row\">");
            appendFileTreePrefix(html, entry.slots());
            appendFileTreeIcon(html, entry.icon(), defaultNamespace, currentPageId, templates, sceneResolver);
            html.append("<span class=\"guide-file-tree-name\">")
                .append(
                    renderFileTreePayload(
                        entry.payloadSource(),
                        defaultNamespace,
                        currentPageId,
                        templates,
                        sceneResolver,
                        compiler))
                .append("</span></div>");
        }
        html.append("</div>");
        return html.toString();
    }

    private String buildDetailsStyle(MdxJsxElementFields element) {
        StringBuilder style = new StringBuilder();
        Integer width = readPositiveInt(readOptional(element, "width"));
        if (width != null) {
            style.append("max-width:")
                .append(width)
                .append("px;width:100%;");
        }
        String wrap = readOptional(element, "wrap");
        String align = readOptional(element, "align");
        if (isFloatWrapMode(wrap)) {
            if ("right".equalsIgnoreCase(align)) {
                style.append("float:right;margin:0 0 12px 12px;");
            } else {
                style.append("float:left;margin:0 12px 12px 0;");
            }
        } else if ("center".equalsIgnoreCase(align)) {
            style.append("margin-left:auto;margin-right:auto;");
        } else if ("right".equalsIgnoreCase(align)) {
            style.append("margin-left:auto;");
        }
        return style.toString();
    }

    private String buildDetailsBodyStyle(MdxJsxElementFields element) {
        Integer height = readPositiveInt(readOptional(element, "height"));
        if (height == null) {
            return "";
        }
        return "max-height:" + height + "px;overflow:auto;";
    }

    private boolean isFloatWrapMode(@Nullable String wrap) {
        return "square".equalsIgnoreCase(wrap) || "tight".equalsIgnoreCase(wrap) || "through".equalsIgnoreCase(wrap);
    }

    private void appendFileTreePrefix(StringBuilder html, List<SlotKind> slots) {
        if (slots.isEmpty()) {
            return;
        }
        StringBuilder prefix = new StringBuilder();
        for (SlotKind slot : slots) {
            switch (slot) {
                case VERTICAL:
                    prefix.append("|   ");
                    break;
                case BRANCH:
                    prefix.append("|-- ");
                    break;
                case LAST_BRANCH:
                    prefix.append("`-- ");
                    break;
                case EMPTY:
                    prefix.append("    ");
                    break;
            }
        }
        html.append("<span class=\"guide-file-tree-prefix\">")
            .append(escapeHtml(prefix.toString()))
            .append("</span>");
    }

    private void appendFileTreeIcon(StringBuilder html, @Nullable FileTreeIcon icon, String defaultNamespace,
        @Nullable ResourceLocation currentPageId, GuideSiteTemplateRegistry templates,
        GuideSiteHtmlCompiler.SceneResolver sceneResolver) {
        if (icon == null || icon.value() == null
            || icon.value()
                .isEmpty()) {
            return;
        }
        String rendered = switch (icon.kind()) {
            case TEXT -> escapeHtml(icon.value());
            case PNG -> "<img src=\"" + escapeAttribute(resolveFileTreeImage(icon.value(), currentPageId))
                + "\" alt=\"\" decoding=\"async\">";
            case ITEM -> renderFileTreeItemIcon(
                icon.value(),
                defaultNamespace,
                currentPageId,
                templates,
                sceneResolver);
        };
        if (rendered.isEmpty()) {
            return;
        }
        html.append("<span class=\"guide-file-tree-icon\">")
            .append(rendered)
            .append("</span> ");
    }

    private String resolveFileTreeImage(String value, @Nullable ResourceLocation currentPageId) {
        String resolved = assetExporter != null ? assetExporter.resolveImageSrc(value, currentPageId) : null;
        if (resolved != null && !resolved.isEmpty()) {
            return resolved;
        }
        return value;
    }

    private String renderFileTreeItemIcon(String value, String defaultNamespace,
        @Nullable ResourceLocation currentPageId, GuideSiteTemplateRegistry templates,
        GuideSiteHtmlCompiler.SceneResolver sceneResolver) {
        List<MdxJsxAttributeNode> attrs = new ArrayList<>();
        attrs.add(new MdxJsxAttribute("id", value));
        attrs.add(new MdxJsxAttribute("scale", "0.85"));
        MdxJsxFlowElement synthetic = new MdxJsxFlowElement("ItemImage", attrs);
        String rendered = renderItemImage(synthetic, defaultNamespace, currentPageId, templates, true);
        return rendered != null ? rendered : escapeHtml(value);
    }

    private String renderFileTreePayload(@Nullable String source, String defaultNamespace,
        @Nullable ResourceLocation currentPageId, GuideSiteTemplateRegistry templates,
        GuideSiteHtmlCompiler.SceneResolver sceneResolver, GuideSiteHtmlCompiler compiler) {
        String payloadSource = source != null ? source : "";
        if (payloadSource.trim()
            .isEmpty()) {
            return "";
        }
        if (currentPageId == null) {
            return escapeHtml(payloadSource);
        }
        ParsedGuidePage parsed = PageCompiler
            .parse(resolveSourcePack(currentPageId), "en_us", currentPageId, payloadSource);
        return compiler.compileInlineFragment(
            parsed.getAstRoot()
                .children(),
            templates,
            defaultNamespace,
            sceneResolver,
            currentPageId);
    }

    private String renderMermaid(MdxJsxElementFields element, String defaultNamespace,
        @Nullable ResourceLocation currentPageId, GuideSiteTemplateRegistry templates,
        GuideSiteHtmlCompiler.SceneResolver sceneResolver, GuideSiteHtmlCompiler compiler) {
        String src = readOptional(element, "src");
        String source = null;
        if (src != null && !src.isEmpty() && currentPageId != null) {
            try {
                ResourceLocation assetId = IdUtils.resolveLink(src, currentPageId);
                byte[] data = guide.loadAsset(assetId);
                if (data != null) {
                    source = MermaidMindmapParser.normalize(new String(data, StandardCharsets.UTF_8));
                }
            } catch (Exception ignored) {}
        }
        if (source == null) {
            ParsedGuidePage parsedPage = currentPageId != null ? parsedPagesById.get(currentPageId) : null;
            source = MermaidMindmapNodeContentExtractor
                .extractDiagramSource(element, parsedPage != null ? parsedPage.getSource() : null);
            if (source == null) {
                source = MermaidMindmapNodeContentExtractor.extractDiagramSource(element.children());
            }
        }
        source = source != null ? source.trim() : "";
        if (source.isEmpty()) {
            return renderError("Empty Mermaid diagram");
        }
        try {
            MermaidMindmapDocument doc = MermaidMindmapParser.parse(source);
            return GuideSiteGraphRenderer.renderMermaidTree(
                doc,
                compileMermaidNodeHtml(
                    element,
                    currentPageId,
                    doc,
                    defaultNamespace,
                    templates,
                    sceneResolver,
                    compiler));
        } catch (Exception ex) {
            return "<pre><code class=\"language-mermaid\">" + escapeHtml(source) + "</code></pre>";
        }
    }

    private Map<String, String> compileMermaidNodeHtml(MdxJsxElementFields element,
        @Nullable ResourceLocation currentPageId, MermaidMindmapDocument doc, String defaultNamespace,
        GuideSiteTemplateRegistry templates, GuideSiteHtmlCompiler.SceneResolver sceneResolver,
        GuideSiteHtmlCompiler compiler) {
        Map<String, MermaidMindmapNode> nodesById = new LinkedHashMap<>();
        collectMermaidNodes(nodesById, doc.getRoot());
        Map<String, String> nodeHtml = new LinkedHashMap<>();
        for (MdxJsxFlowElement nodeContent : MermaidMindmapNodeContentExtractor
            .collectNodeContentElements(element.children())) {
            String id = MermaidMindmapNodeContentExtractor.readNodeContentId(nodeContent);
            if (id == null || !nodesById.containsKey(id) || nodeHtml.containsKey(id)) {
                continue;
            }
            nodeHtml.put(
                id,
                compiler.compileFragment(
                    nodeContent.children(),
                    templates,
                    defaultNamespace,
                    sceneResolver,
                    currentPageId));
        }
        for (MermaidMindmapNode node : nodesById.values()) {
            if (nodeHtml.containsKey(node.getId()) || !shouldCompileMermaidRichLabel(node)) {
                continue;
            }
            ParsedGuidePage parsed = PageCompiler
                .parse(resolveSourcePack(currentPageId), "en_us", currentPageId, node.getLabelSource());
            nodeHtml.put(
                node.getId(),
                compiler.compileInlineFragment(
                    parsed.getAstRoot()
                        .children(),
                    templates,
                    defaultNamespace,
                    sceneResolver,
                    currentPageId));
        }
        return nodeHtml;
    }

    private void collectMermaidNodes(Map<String, MermaidMindmapNode> nodesById, MermaidMindmapNode node) {
        nodesById.putIfAbsent(node.getId(), node);
        for (MermaidMindmapNode child : node.getChildren()) {
            collectMermaidNodes(nodesById, child);
        }
    }

    private boolean shouldCompileMermaidRichLabel(MermaidMindmapNode node) {
        String labelSource = node.getLabelSource();
        return labelSource != null && !labelSource.trim()
            .isEmpty() && !labelSource.equals(node.getText());
    }

    private String renderCsvTable(MdxJsxElementFields element, @Nullable ResourceLocation currentPageId) {
        boolean hasHeader = !"false".equalsIgnoreCase(readOptional(element, "header"));
        String src = readOptional(element, "src");
        String csvText = null;
        if (src != null && !src.isEmpty() && currentPageId != null) {
            try {
                ResourceLocation assetId = IdUtils.resolveLink(src, currentPageId);
                byte[] data = guide.loadAsset(assetId);
                if (data != null) {
                    csvText = new String(data, StandardCharsets.UTF_8);
                }
            } catch (Exception ignored) {}
        }
        if (csvText == null) {
            StringBuilder inline = new StringBuilder();
            collectStructureText(inline, element.children());
            csvText = inline.toString()
                .trim();
        }
        if (csvText.isEmpty()) {
            return "<div class=\"guide-csv-table-wrap\"><table class=\"guide-csv-table\"></table></div>";
        }
        return GuideSiteGraphRenderer.renderCsvTable(csvText, hasHeader);
    }

    private String renderColumnChart(MdxJsxElementFields element) {
        int w = readInt(element, "width", 1280);
        int h = readInt(element, "height", 800);
        int bgColor = parseArgbAttr(element, "background", 0xFF1B1F23);
        int borderColor = parseArgbAttr(element, "border", 0xFF3A4047);
        String title = readOptional(element, "title");
        String[] categories = ChartAttrParser.parseStringArray(readOptional(element, "categories"));
        boolean showLegend = readBoolean(element, "showLegend", true);
        String yAxisUnit = readOptional(element, "yAxisUnit");
        boolean labelAbove = "above".equals(readOptional(element, "labelPosition"));
        List<GuideSiteGraphRenderer.SeriesData> series = parseSeriesChildren(element);
        GuideSiteGraphRenderer.PieInsetData pieInset = parsePieInsetChildren(element);
        return GuideSiteGraphRenderer.renderColumnChart(
            w,
            h,
            bgColor,
            borderColor,
            title,
            categories,
            series,
            showLegend,
            pieInset,
            yAxisUnit,
            labelAbove);
    }

    private String renderBarChart(MdxJsxElementFields element) {
        int w = readInt(element, "width", 1280);
        int h = readInt(element, "height", 800);
        int bgColor = parseArgbAttr(element, "background", 0xFF1B1F23);
        int borderColor = parseArgbAttr(element, "border", 0xFF3A4047);
        String title = readOptional(element, "title");
        String[] categories = ChartAttrParser.parseStringArray(readOptional(element, "categories"));
        boolean showLegend = readBoolean(element, "showLegend", true);
        List<GuideSiteGraphRenderer.SeriesData> series = parseSeriesChildren(element);
        return GuideSiteGraphRenderer.renderBarChart(w, h, bgColor, borderColor, title, categories, series, showLegend);
    }

    private String renderLineChart(MdxJsxElementFields element) {
        int w = readInt(element, "width", 1280);
        int h = readInt(element, "height", 800);
        int bgColor = parseArgbAttr(element, "background", 0xFF1B1F23);
        int borderColor = parseArgbAttr(element, "border", 0xFF3A4047);
        String title = readOptional(element, "title");
        String[] categories = ChartAttrParser.parseStringArray(readOptional(element, "categories"));
        boolean numericX = readBoolean(element, "numericX", false);
        boolean showPoints = readBoolean(element, "showPoints", true);
        boolean showLegend = readBoolean(element, "showLegend", true);
        CornerLegendPosition cornerLegendPosition = ChartAttrParser
            .parseCornerLegendPosition(readOptional(element, "cornerLegend"), CornerLegendPosition.NONE);
        int cornerLegendWidth = readInt(element, "cornerLegendWidth", 120);
        int cornerLegendHeight = readInt(element, "cornerLegendHeight", 64);
        int cornerLegendBackground = parseArgbAttr(element, "cornerLegendBackground", 0xAA111922);
        List<GuideSiteGraphRenderer.SeriesData> series = parseSeriesChildren(element);
        return GuideSiteGraphRenderer.renderLineChart(
            w,
            h,
            bgColor,
            borderColor,
            title,
            categories,
            series,
            numericX,
            showPoints,
            showLegend,
            cornerLegendPosition,
            cornerLegendWidth,
            cornerLegendHeight,
            cornerLegendBackground);
    }

    private String renderPieChart(MdxJsxElementFields element) {
        int w = readInt(element, "width", 1280);
        int h = readInt(element, "height", 800);
        int bgColor = parseArgbAttr(element, "background", 0xFF1B1F23);
        int borderColor = parseArgbAttr(element, "border", 0xFF3A4047);
        String title = readOptional(element, "title");
        boolean showLegend = readBoolean(element, "showLegend", true);
        List<GuideSiteGraphRenderer.SliceData> slices = parseSliceChildren(element);
        return GuideSiteGraphRenderer.renderPieChart(w, h, bgColor, borderColor, title, slices, showLegend);
    }

    private String renderScatterChart(MdxJsxElementFields element) {
        int w = readInt(element, "width", 1280);
        int h = readInt(element, "height", 800);
        int bgColor = parseArgbAttr(element, "background", 0xFF1B1F23);
        int borderColor = parseArgbAttr(element, "border", 0xFF3A4047);
        String title = readOptional(element, "title");
        boolean showLegend = readBoolean(element, "showLegend", true);
        CornerLegendPosition cornerLegendPosition = ChartAttrParser
            .parseCornerLegendPosition(readOptional(element, "cornerLegend"), CornerLegendPosition.NONE);
        int cornerLegendWidth = readInt(element, "cornerLegendWidth", 120);
        int cornerLegendHeight = readInt(element, "cornerLegendHeight", 64);
        int cornerLegendBackground = parseArgbAttr(element, "cornerLegendBackground", 0xAA111922);
        List<GuideSiteGraphRenderer.SeriesData> series = parseScatterSeriesChildren(element);
        return GuideSiteGraphRenderer.renderScatterChart(
            w,
            h,
            bgColor,
            borderColor,
            title,
            series,
            showLegend,
            cornerLegendPosition,
            cornerLegendWidth,
            cornerLegendHeight,
            cornerLegendBackground);
    }

    private String renderFunctionGraphTag(MdxJsxElementFields element) {
        int w = readInt(element, "width", 1280);
        int h = readInt(element, "height", 880);
        int bgColor = parseArgbAttr(element, "background", 0xFF1B1F23);
        int borderColor = parseArgbAttr(element, "border", 0xFF3A4047);
        int axisColor = parseArgbAttr(element, "axisColor", 0xFFB8C2CF);
        int gridColor = parseArgbAttr(element, "gridColor", 0x33B8C2CF);
        boolean showGrid = readBoolean(element, "showGrid", true);
        boolean showAxes = readBoolean(element, "showAxes", true);
        String title = readOptional(element, "title");
        CornerLegendPosition cornerLegendPosition = ChartAttrParser
            .parseCornerLegendPosition(readOptional(element, "cornerLegend"), CornerLegendPosition.NONE);
        int cornerLegendWidth = readInt(element, "cornerLegendWidth", 120);
        int cornerLegendHeight = readInt(element, "cornerLegendHeight", 64);
        int cornerLegendBackground = parseArgbAttr(element, "cornerLegendBackground", 0xAA111922);
        double xMin = parseDoubleAttr(element, "xMin", -10);
        double xMax = parseDoubleAttr(element, "xMax", 10);
        double yMin = parseDoubleAttr(element, "yMin", Double.NaN);
        double yMax = parseDoubleAttr(element, "yMax", Double.NaN);
        String xRange = readOptional(element, "xRange");
        int xRangeSeparator = rangeSeparatorIndex(xRange);
        if (xRangeSeparator >= 0) {
            try {
                xMin = Double.parseDouble(
                    xRange.substring(0, xRangeSeparator)
                        .trim());
            } catch (NumberFormatException ignored) {}
            try {
                xMax = Double.parseDouble(
                    xRange.substring(xRangeSeparator + 2)
                        .trim());
            } catch (NumberFormatException ignored) {}
        }
        String yRange = readOptional(element, "yRange");
        int yRangeSeparator = rangeSeparatorIndex(yRange);
        if (yRangeSeparator >= 0) {
            try {
                yMin = Double.parseDouble(
                    yRange.substring(0, yRangeSeparator)
                        .trim());
            } catch (NumberFormatException ignored) {}
            try {
                yMax = Double.parseDouble(
                    yRange.substring(yRangeSeparator + 2)
                        .trim());
            } catch (NumberFormatException ignored) {}
        }
        List<FunctionPlot> plots = parsePlotChildren(element);
        // Support self-closing usage like <Function expr="x^2" xRange="-2..4" />: when no nested
        // <Plot>/<Function> children exist but the outer element itself carries an expression,
        // treat the outer element as a single plot so the curve renders.
        if (plots.isEmpty()) {
            String selfExpr = readOptional(element, "expr");
            if (selfExpr != null && !selfExpr.trim()
                .isEmpty()) {
                String trimmed = selfExpr.trim();
                boolean inverse = readBoolean(element, "inverse", false);
                int color = parseArgbAttr(element, "color", FunctionGraphPalette.color(0));
                String label = readOptional(element, "label");
                DomainPredicate domain = DomainPredicate.parse(readOptional(element, "domain"));
                AutoPointSpec autoPointSpec = FunctionGraphAttrs.parseAutoPointSpec(
                    readOptional(element, "pointEveryX"),
                    readOptional(element, "pointEveryY"),
                    readOptional(element, "autoPointLabel"),
                    readOptional(element, "autoPointColor"),
                    color);
                plots.add(
                    new FunctionPlot(
                        trimmed,
                        FunctionExprParser.parse(trimmed, inverse ? 1 : 0),
                        inverse,
                        domain,
                        color,
                        label != null ? label : trimmed,
                        autoPointSpec));
            }
        }
        // Auto Y range when not specified
        if (Double.isNaN(yMin) || Double.isNaN(yMax)) {
            double autoYMin = Double.MAX_VALUE;
            double autoYMax = -Double.MAX_VALUE;
            for (FunctionPlot plot : plots) {
                for (int i = 0; i <= 256; i++) {
                    double x = xMin + (xMax - xMin) * i / 256.0;
                    double y = plot.evaluate(x);
                    if (Double.isFinite(y)) {
                        if (y < autoYMin) autoYMin = y;
                        if (y > autoYMax) autoYMax = y;
                    }
                }
            }
            if (!Double.isFinite(autoYMin)) {
                autoYMin = xMin;
                autoYMax = xMax;
            }
            if (autoYMin == autoYMax) {
                autoYMin -= 1;
                autoYMax += 1;
            }
            double margin = (autoYMax - autoYMin) * 0.1;
            if (Double.isNaN(yMin)) yMin = autoYMin - margin;
            if (Double.isNaN(yMax)) yMax = autoYMax + margin;
        }
        List<MarkedPoint> points = parsePointChildren(element);
        return GuideSiteGraphRenderer.renderFunctionGraphSvg(
            plots,
            points,
            w,
            h,
            title,
            bgColor,
            borderColor,
            axisColor,
            gridColor,
            showGrid,
            showAxes,
            xMin,
            xMax,
            yMin,
            yMax,
            cornerLegendPosition,
            cornerLegendWidth,
            cornerLegendHeight,
            cornerLegendBackground);
    }

    private int rangeSeparatorIndex(@Nullable String range) {
        if (range == null) {
            return -1;
        }
        int separator = range.indexOf("..");
        return separator >= 0 && range.indexOf("..", separator + 2) < 0 ? separator : -1;
    }

    /** Parse {@code <Series name="..." data="1,2,3" color="...">} children for bar/column/line charts. */
    private List<GuideSiteGraphRenderer.SeriesData> parseSeriesChildren(MdxJsxElementFields element) {
        List<GuideSiteGraphRenderer.SeriesData> result = new ArrayList<>();
        int idx = 0;
        for (MdAstAnyContent child : element.children()) {
            if (!(child instanceof MdxJsxElementFields c)) {
                continue;
            }
            boolean isSeries = "Series".equals(c.name());
            boolean isLineSeries = "LineSeries".equals(c.name());
            if (!isSeries && !isLineSeries) {
                continue;
            }
            String name = readOptional(c, "name");
            if (name == null) name = (isLineSeries ? "Line " : "Series ") + (idx + 1);
            int color = parseArgbAttr(c, "color", ChartAttrParser.paletteColor(idx));
            String rawData = readOptional(c, "data");
            String rawPoints = readOptional(c, "points");
            double[] xs;
            double[] ys;
            if ((rawData == null || rawData.isEmpty()) && rawPoints != null && !rawPoints.isEmpty()) {
                // `points="x1:y1,x2:y2,..."` syntax - same as scatter but drawn as a line.
                // This allows numericX line charts where each series carries its own X positions.
                double[][] pts = ChartAttrParser.parsePointArray(rawPoints);
                xs = pts[0];
                ys = pts[1];
            } else {
                ys = ChartAttrParser.parseDoubleArray(rawData);
                xs = new double[ys.length];
                for (int i = 0; i < xs.length; i++) xs[i] = i;
            }
            String type = isLineSeries ? GuideSiteGraphRenderer.SeriesData.TYPE_LINE
                : GuideSiteGraphRenderer.SeriesData.TYPE_COLUMN;
            result.add(new GuideSiteGraphRenderer.SeriesData(name, color, xs, ys, type));
            idx++;
        }
        return result;
    }

    /**
     * Parse a single {@code <PieInset size="..." position="..." title="...">} child.
     * Returns {@code null} if no such child is present.
     */
    @Nullable
    private GuideSiteGraphRenderer.PieInsetData parsePieInsetChildren(MdxJsxElementFields element) {
        for (MdAstAnyContent child : element.children()) {
            if (!(child instanceof MdxJsxElementFields c)) {
                continue;
            }
            if (!"PieInset".equals(c.name())) {
                continue;
            }
            int size = readInt(c, "size", 80);
            String position = readOptional(c, "position");
            String insetTitle = readOptional(c, "title");
            List<GuideSiteGraphRenderer.SliceData> slices = parseSliceChildren(c);
            return new GuideSiteGraphRenderer.PieInsetData(slices, size, position, insetTitle);
        }
        return null;
    }

    /** Parse {@code <Series points="x1:y1,x2:y2,...">} children for scatter chart. */
    private List<GuideSiteGraphRenderer.SeriesData> parseScatterSeriesChildren(MdxJsxElementFields element) {
        List<GuideSiteGraphRenderer.SeriesData> result = new ArrayList<>();
        int idx = 0;
        for (MdAstAnyContent child : element.children()) {
            if (!(child instanceof MdxJsxElementFields c)) {
                continue;
            }
            if (!"Series".equals(c.name())) {
                continue;
            }
            String name = readOptional(c, "name");
            if (name == null) name = "Series " + (idx + 1);
            int color = parseArgbAttr(c, "color", ChartAttrParser.paletteColor(idx));
            double[][] pts = ChartAttrParser.parsePointArray(readOptional(c, "points"));
            result.add(new GuideSiteGraphRenderer.SeriesData(name, color, pts[0], pts[1]));
            idx++;
        }
        return result;
    }

    /** Parse {@code <Slice label="..." value="0.5" color="...">} children for pie chart. */
    private List<GuideSiteGraphRenderer.SliceData> parseSliceChildren(MdxJsxElementFields element) {
        List<GuideSiteGraphRenderer.SliceData> result = new ArrayList<>();
        int idx = 0;
        for (MdAstAnyContent child : element.children()) {
            if (!(child instanceof MdxJsxElementFields c)) {
                continue;
            }
            if (!"Slice".equals(c.name())) {
                continue;
            }
            String label = readOptional(c, "label");
            if (label == null) label = "";
            double value = parseDoubleAttr(c, "value", 1.0);
            int color = parseArgbAttr(c, "color", ChartAttrParser.paletteColor(idx));
            result.add(new GuideSiteGraphRenderer.SliceData(label, value, color));
            idx++;
        }
        return result;
    }

    /** Parse {@code <Plot>expr</Plot>} or {@code <Function>expr</Function>} children. */
    private List<FunctionPlot> parsePlotChildren(MdxJsxElementFields element) {
        List<FunctionPlot> result = new ArrayList<>();
        int idx = 0;
        for (MdAstAnyContent child : element.children()) {
            if (!(child instanceof MdxJsxElementFields c)) {
                continue;
            }
            String cn = c.name();
            if (!("Plot".equals(cn) || "Function".equals(cn))) {
                continue;
            }
            // Prefer the expr="..." attribute, fall back to inline child text.
            String exprText = readOptional(c, "expr");
            if (exprText == null || exprText.trim()
                .isEmpty()) {
                StringBuilder exprBuf = new StringBuilder();
                collectStructureText(exprBuf, c.children());
                exprText = exprBuf.toString();
            }
            exprText = exprText != null ? exprText.trim() : "";
            if (exprText.isEmpty()) {
                continue;
            }
            boolean inverse = readBoolean(c, "inverse", false);
            int color = parseArgbAttr(c, "color", FunctionGraphPalette.color(idx));
            String label = readOptional(c, "label");
            DomainPredicate domain = DomainPredicate.parse(readOptional(c, "domain"));
            AutoPointSpec autoPointSpec = FunctionGraphAttrs.parseAutoPointSpec(
                readOptional(c, "pointEveryX"),
                readOptional(c, "pointEveryY"),
                readOptional(c, "autoPointLabel"),
                readOptional(c, "autoPointColor"),
                color);
            result.add(
                new FunctionPlot(
                    exprText,
                    FunctionExprParser.parse(exprText, inverse ? 1 : 0),
                    inverse,
                    domain,
                    color,
                    label != null ? label : exprText,
                    autoPointSpec));
            idx++;
        }
        return result;
    }

    /** Parse {@code <Point x="" y=""/>} or {@code <Point plot="0" atX="..."/>} children. */
    private List<MarkedPoint> parsePointChildren(MdxJsxElementFields element) {
        List<MarkedPoint> result = new ArrayList<>();
        for (MdAstAnyContent child : element.children()) {
            if (!(child instanceof MdxJsxElementFields c)) {
                continue;
            }
            if (!"Point".equals(c.name())) {
                continue;
            }
            String label = readOptional(c, "label");
            if (label == null) {
                label = "";
            }
            String plotAttr = readOptional(c, "plot");
            int color = parseArgbAttr(c, "color", 0);
            boolean colorInherit = readOptional(c, "color") == null;
            if (plotAttr != null) {
                int plotIdx;
                try {
                    plotIdx = Integer.parseInt(plotAttr.trim());
                } catch (NumberFormatException ignored) {
                    continue;
                }
                String atX = readOptional(c, "atX");
                String atY = readOptional(c, "atY");
                if (atX != null) {
                    try {
                        result.add(
                            new MarkedPoint(
                                MarkedPoint.MODE_PLOT_AT_X,
                                plotIdx,
                                Double.parseDouble(atX.trim()),
                                0,
                                color,
                                colorInherit,
                                label));
                    } catch (NumberFormatException ignored) {}
                } else if (atY != null) {
                    try {
                        result.add(
                            new MarkedPoint(
                                MarkedPoint.MODE_PLOT_AT_Y,
                                plotIdx,
                                Double.parseDouble(atY.trim()),
                                0,
                                color,
                                colorInherit,
                                label));
                    } catch (NumberFormatException ignored) {}
                }
                continue;
            }
            String xs = readOptional(c, "x");
            String ys = readOptional(c, "y");
            if (xs == null || ys == null) {
                continue;
            }
            try {
                double xv = Double.parseDouble(xs.trim());
                double yv = Double.parseDouble(ys.trim());
                result.add(new MarkedPoint(MarkedPoint.MODE_EXPLICIT, -1, xv, yv, color, colorInherit, label));
            } catch (NumberFormatException ignored) {}
        }
        return result;
    }

    /** Parse an ARGB color attribute (hex #rrggbb or #aarrggbb). */
    private int parseArgbAttr(MdxJsxElementFields element, String attr, int def) {
        String val = readOptional(element, attr);
        if (val == null) {
            return def;
        }
        return ChartAttrParser.parseColor(val, def);
    }

    /** Parse a double attribute, returning defaultValue on missing or invalid. */
    private double parseDoubleAttr(MdxJsxElementFields element, String attr, double defaultValue) {
        String val = readOptional(element, attr);
        if (val == null || val.trim()
            .isEmpty()) {
            return defaultValue;
        }
        try {
            return Double.parseDouble(val.trim());
        } catch (NumberFormatException ignored) {
            return defaultValue;
        }
    }

    private String renderNavigationNodeList(List<NavigationNode> nodes, @Nullable ResourceLocation currentPageId) {
        StringBuilder html = new StringBuilder();
        html.append("<ul class=\"guide-generated-links\">");
        for (NavigationNode node : nodes) {
            if (!node.hasPage() || node.pageId() == null) {
                continue;
            }
            html.append("<li><a href=\"")
                .append(
                    escapeAttribute(
                        GuideSiteHrefResolver
                            .resolvePageAnchor(currentPageId, node.guideId(), PageAnchor.page(node.pageId()))))
                .append("\">")
                .append(buildGuideLinkContent(node.title(), node.icon(), node.guideId()))
                .append("</a></li>");
        }
        html.append("</ul>");
        return html.toString();
    }

    private String renderPageAnchorList(List<PageAnchor> anchors, @Nullable ResourceLocation currentPageId) {
        StringBuilder html = new StringBuilder();
        html.append("<ul class=\"guide-generated-links\">");
        LinkedHashSet<String> seen = new LinkedHashSet<>();
        for (PageAnchor anchor : anchors) {
            if (anchor == null || anchor.pageId() == null) {
                continue;
            }
            ParsedGuidePage page = parsedPagesById.get(anchor.pageId());
            FrontmatterNavigation navigation = page != null ? page.getFrontmatter()
                .navigationEntry() : null;
            String title = navigation != null ? navigation.title()
                : anchor.pageId()
                    .toString();
            NavigationNode node = navigationTree.getNodeById(anchor.pageId());
            ResourceLocation targetGuideId = node != null ? node.guideId() : null;
            String href = GuideSiteHrefResolver.resolvePageAnchor(currentPageId, targetGuideId, anchor);
            if (!seen.add(href)) {
                continue;
            }
            html.append("<li><a href=\"")
                .append(escapeAttribute(href))
                .append("\">")
                .append(buildGuideLinkContent(title, node != null ? node.icon() : null, targetGuideId))
                .append("</a></li>");
        }
        html.append("</ul>");
        return html.toString();
    }

    private String renderMediaWikiEntryList(List<MediaWikiListEntry> entries, int rows,
        @Nullable ResourceLocation currentPageId, String emptyText) {
        List<MediaWikiListEntry> sorted = MediaWikiListPlanner.sortEntries(entries);
        int columnCount = MediaWikiListPlanner.sanitizeRows(rows);
        StringBuilder html = new StringBuilder();
        html.append("<div class=\"guide-mediawiki-index\">");
        if (sorted.isEmpty()) {
            html.append("<div class=\"guide-mediawiki-empty\">")
                .append(escapeHtml(emptyText))
                .append("</div></div>");
            return html.toString();
        }

        html.append("<div class=\"guide-mediawiki-columns\" style=\"--guide-mediawiki-columns:")
            .append(columnCount)
            .append(";\">");
        LinkedHashSet<String> seen = new LinkedHashSet<>();
        for (MediaWikiListPlanner.MediaWikiListColumn column : MediaWikiListPlanner.planColumns(sorted, columnCount)) {
            html.append("<div class=\"guide-mediawiki-column\">");
            for (MediaWikiListPlanner.MediaWikiListSection section : column.sections()) {
                if (!section.entries()
                    .isEmpty()) {
                    html.append("<div class=\"guide-mediawiki-section\">");
                    html.append("<div class=\"guide-mediawiki-section-key\">")
                        .append(escapeHtml(section.key()))
                        .append("</div>");
                }
                for (MediaWikiListEntry entry : section.entries()) {
                    String href = GuideSiteHrefResolver
                        .resolvePageAnchor(currentPageId, PageAnchor.page(entry.pageId()));
                    if (!seen.add(href)) {
                        continue;
                    }
                    html.append("<a class=\"guide-mediawiki-row\" href=\"")
                        .append(escapeAttribute(href))
                        .append("\">")
                        .append(buildGuideLinkContent(entry.title(), entry.icon(), null))
                        .append("</a>");
                }
                if (!section.entries()
                    .isEmpty()) {
                    html.append("</div>");
                }
            }
            html.append("</div>");
        }
        html.append("</div></div>");
        return html.toString();
    }

    private String renderSpecialResult(MediaWikiSpecialPageResult result, int rows,
        @Nullable ResourceLocation currentPageId, String specialName) {
        if (result == null) {
            return renderError("Special page result is not available");
        }

        StringBuilder html = new StringBuilder();
        html.append("<div class=\"guide-mediawiki-special\" data-guide-special-page=\"")
            .append(escapeAttribute(specialName))
            .append("\" data-guide-special-mode=\"")
            .append(
                result.kind() == MediaWikiSpecialPageKind.GROUPED
                    || result.kind() == MediaWikiSpecialPageKind.GROUP_INDEX ? "grouped" : "flat")
            .append("\" data-guide-special-default-visible=\"")
            .append(resolveSpecialDefaultVisibleCount(result))
            .append("\">");
        if (result.searchEnabled()) {
            html.append("<div class=\"guide-mediawiki-special-search\">")
                .append("<input class=\"guide-search-input guide-mediawiki-special-input\"")
                .append(" data-guide-special-filter=\"true\"")
                .append(" type=\"text\" autocomplete=\"off\">")
                .append("</div>")
                .append("<div class=\"guide-mediawiki-special-divider\"></div>");
        }

        switch (result.kind()) {
            case GROUP_INDEX:
            case GROUPED:
                html.append(renderSpecialGrouped(result.groupedEntries(), rows, currentPageId, result.kind()));
                break;
            case GRID:
                html.append(renderSpecialFlat(result.flatEntries(), rows, currentPageId, true));
                break;
            case FLAT:
                html.append(renderSpecialFlat(result.flatEntries(), rows, currentPageId, false));
                break;
            case INFO:
            case ACTION:
                html.append(renderSpecialInfo(result.flatEntries()));
                break;
            default:
                html.append(renderSpecialInfo(result.flatEntries()));
                break;
        }
        if (shouldRenderSpecialShowMore(result)) {
            html.append(renderSpecialShowMoreButton());
        }
        html.append("</div>");
        return html.toString();
    }

    private String renderSpecialGrouped(List<MediaWikiSpecialGroupedEntry> groups, int rows,
        @Nullable ResourceLocation currentPageId, MediaWikiSpecialPageKind kind) {
        int effectiveRows = kind == MediaWikiSpecialPageKind.GROUP_INDEX ? SPECIAL_PAGES_GROUP_COLUMNS : rows;
        StringBuilder html = new StringBuilder();
        html.append("<div class=\"guide-mediawiki-special-groups")
            .append(kind == MediaWikiSpecialPageKind.GROUP_INDEX ? " guide-mediawiki-special-groups-index" : "")
            .append("\">");
        for (MediaWikiSpecialGroupedEntry group : groups) {
            html.append(
                "<section class=\"guide-mediawiki-special-group\" data-guide-special-group=\"true\" data-guide-special-search=\"")
                .append(escapeAttribute(group.searchBlob()))
                .append("\">")
                .append("<h2 class=\"guide-mediawiki-special-group-title\">")
                .append(escapeHtml(group.title()))
                .append("</h2>");
            if (group.summary() != null && !group.summary()
                .isEmpty()) {
                html.append("<div class=\"guide-mediawiki-special-group-summary\">")
                    .append(escapeHtml(group.summary()))
                    .append("</div>");
            }
            html.append(renderSpecialFlat(group.children(), effectiveRows, currentPageId, false))
                .append("</section>");
        }
        html.append("</div>");
        return html.toString();
    }

    private String renderSpecialFlat(List<MediaWikiSpecialListEntry> entries, int rows,
        @Nullable ResourceLocation currentPageId, boolean grid) {
        int columnCount = Math.max(1, rows);
        StringBuilder html = new StringBuilder();
        html.append("<div class=\"guide-mediawiki-special-list")
            .append(grid ? " guide-mediawiki-special-grid" : " guide-mediawiki-special-flat")
            .append(!grid && columnCount > 1 ? " guide-mediawiki-special-multi-column-flat" : "")
            .append("\" style=\"--guide-mediawiki-columns:")
            .append(columnCount)
            .append(";\">");
        for (MediaWikiSpecialListEntry entry : entries) {
            html.append(renderSpecialEntry(entry, currentPageId));
        }
        if (entries.isEmpty()) {
            html.append("<div class=\"guide-mediawiki-empty\">")
                .append(escapeHtml(GuidebookText.MediaWikiNoPages.text()))
                .append("</div>");
        }
        html.append("</div>");
        return html.toString();
    }

    private int resolveSpecialDefaultVisibleCount(MediaWikiSpecialPageResult result) {
        if (result == null || result.definition() == null
            || result.definition()
                .showsAllByDefault()) {
            return Integer.MAX_VALUE;
        }
        return Math.max(
            1,
            result.definition()
                .defaultVisibleCount());
    }

    private int resolveSpecialInitialVisibleCount(String specialName) {
        var definition = specialPageResolver.findDefinition(specialName);
        if (definition == null || definition.showsAllByDefault()) {
            return Integer.MAX_VALUE;
        }
        return Math.max(1, definition.defaultVisibleCount());
    }

    private String renderSpecialShowMoreButton() {
        return "<button type=\"button\" class=\"guide-mediawiki-special-show-more\" data-guide-special-show-more=\"true\">"
            + escapeHtml(GuidebookText.SpecialPageShowMore.text())
            + "</button>";
    }

    private MediaWikiSpecialPageQuery buildSpecialPageQuery(MdxJsxElementFields element, int visibleCount) {
        LinkedHashMap<String, String> parameters = new LinkedHashMap<>();
        appendSpecialParameter(parameters, MediaWikiSpecialPageQuery.PARAM_PAGE, readOptional(element, "page"));
        appendSpecialParameter(parameters, MediaWikiSpecialPageQuery.PARAM_PREFIX, readOptional(element, "prefix"));
        appendSpecialParameter(parameters, MediaWikiSpecialPageQuery.PARAM_LANGUAGE, readOptional(element, "language"));
        String searchText = readOptional(element, "query");
        return new MediaWikiSpecialPageQuery(searchText != null ? searchText.trim() : "", visibleCount, parameters);
    }

    private void appendSpecialParameter(Map<String, String> parameters, String key, @Nullable String value) {
        if (value == null) {
            return;
        }
        String trimmed = value.trim();
        if (!trimmed.isEmpty()) {
            parameters.put(key, trimmed);
        }
    }

    private boolean shouldRenderSpecialShowMore(MediaWikiSpecialPageResult result) {
        int defaultVisibleCount = resolveSpecialDefaultVisibleCount(result);
        if (defaultVisibleCount == Integer.MAX_VALUE) {
            return false;
        }
        int totalCount = result.kind() == MediaWikiSpecialPageKind.GROUPED
            || result.kind() == MediaWikiSpecialPageKind.GROUP_INDEX
                ? result.groupedEntries()
                    .size()
                : result.flatEntries()
                    .size();
        return totalCount > defaultVisibleCount;
    }

    private String renderSpecialEntry(MediaWikiSpecialListEntry entry, @Nullable ResourceLocation currentPageId) {
        StringBuilder html = new StringBuilder();
        html.append(
            "<div class=\"guide-mediawiki-special-entry\" data-guide-special-entry=\"true\" data-guide-special-search=\"")
            .append(escapeAttribute(entry.searchBlob()))
            .append("\">");
        String titleContent = buildSpecialEntryTitle(entry, currentPageId);
        if (MediaWikiExternalLinkSupport.isSupportedExternalUrl(entry.externalUrl())) {
            String href = GuideSiteHrefResolver
                .resolveExternalConfirmHref(currentPageId, entry.externalUrl(), entry.title());
            html.append("<a class=\"guide-mediawiki-special-link\" href=\"")
                .append(escapeAttribute(href))
                .append("\">")
                .append(titleContent)
                .append("</a>");
        } else if (entry.pageId() != null) {
            String rowAnchor = entry.lineNumber() != null ? "line-" + entry.lineNumber() : null;
            String href = GuideSiteHrefResolver
                .resolvePageAnchor(currentPageId, new PageAnchor(entry.pageId(), rowAnchor));
            html.append("<a class=\"guide-mediawiki-special-link\" href=\"")
                .append(escapeAttribute(href))
                .append("\">")
                .append(titleContent)
                .append("</a>");
        } else {
            html.append("<span class=\"guide-mediawiki-special-text\">")
                .append(titleContent)
                .append("</span>");
        }
        if (entry.subtitle() != null && !entry.subtitle()
            .isEmpty()) {
            html.append("<div class=\"guide-mediawiki-special-subtitle\">")
                .append(escapeHtml(entry.subtitle()))
                .append("</div>");
        }
        html.append("</div>");
        return html.toString();
    }

    private String buildSpecialEntryTitle(MediaWikiSpecialListEntry entry, @Nullable ResourceLocation currentPageId) {
        StringBuilder html = new StringBuilder();
        GuidePageIcon icon = entry.icon();
        if (icon != null) {
            appendGuidePageIcon(html, icon, currentPageId);
        }
        html.append("<span class=\"guide-generated-link-text\"");
        if (entry.title() != null && !entry.title()
            .isEmpty()) {
            html.append(" title=\"")
                .append(escapeAttribute(entry.title()))
                .append("\"");
        }
        html.append(">")
            .append(escapeHtml(entry.title()))
            .append("</span>");
        return html.toString();
    }

    private String renderSpecialInfo(List<MediaWikiSpecialListEntry> entries) {
        if (entries == null || entries.isEmpty()) {
            return "<div class=\"guide-mediawiki-empty\">" + escapeHtml(GuidebookText.MediaWikiNoDataAvailable.text())
                + "</div>";
        }
        StringBuilder html = new StringBuilder();
        html.append("<div class=\"guide-mediawiki-special-info\">");
        for (MediaWikiSpecialListEntry entry : entries) {
            html.append("<div class=\"guide-mediawiki-special-info-row\">")
                .append(renderSpecialInfoEntry(entry))
                .append("</div>");
        }
        html.append("</div>");
        return html.toString();
    }

    private String renderSpecialInfoEntry(MediaWikiSpecialListEntry entry) {
        if (entry == null) {
            return "";
        }
        if (MediaWikiExternalLinkSupport.isSupportedExternalUrl(entry.externalUrl())) {
            return "<a class=\"guide-mediawiki-special-link\" href=\""
                + escapeAttribute(
                    GuideSiteHrefResolver.resolveExternalConfirmHref(null, entry.externalUrl(), entry.title()))
                + "\">"
                + escapeHtml(entry.title())
                + "</a>";
        }
        String title = entry.title() != null ? entry.title() : "";
        if (MediaWikiExternalLinkSupport.isSupportedExternalUrl(title)) {
            return "<a class=\"guide-mediawiki-special-link\" href=\""
                + escapeAttribute(GuideSiteHrefResolver.resolveExternalConfirmHref(null, title, title))
                + "\">"
                + escapeHtml(title)
                + "</a>";
        }
        return escapeHtml(title);
    }

    private String buildGuideLinkContent(String title, @Nullable GuidePageIcon icon,
        @Nullable ResourceLocation guideId) {
        StringBuilder html = new StringBuilder();
        appendGuidePageIcon(html, icon, guideId);
        html.append("<span class=\"guide-generated-link-text\">")
            .append(escapeHtml(title))
            .append("</span>");
        return html.toString();
    }

    private void appendGuidePageIcon(StringBuilder html, @Nullable GuidePageIcon icon,
        @Nullable ResourceLocation guideId) {
        if (icon == null) {
            return;
        }
        if (icon.isItemIcon() && icon.resolveCurrentItemStack() != null) {
            GuideSiteItemHtml.appendIcon(
                html,
                GuideSiteItemSupport.export(icon.resolveCurrentItemStack(), itemIconResolver),
                "guide-nav-item-icon");
            return;
        }
        GuideSitePageAssetExporter resolvedAssetExporter = resolveAssetExporter(guideId);
        ResourceLocation textureId = resolveTextureIconId(icon);
        String src = resolvedAssetExporter != null && textureId != null
            ? resolvedAssetExporter.exportResource(textureId)
            : "";
        if (src.isEmpty()) {
            return;
        }
        html.append("<img class=\"item-icon guide-nav-item-icon\" src=\"")
            .append(escapeAttribute(src))
            .append("\" alt=\"\" width=\"32\" height=\"32\" decoding=\"async\">");
    }

    @Nullable
    private GuideSitePageAssetExporter resolveAssetExporter(@Nullable ResourceLocation guideId) {
        if (guideId != null && assetExportersByGuideId != null) {
            GuideSitePageAssetExporter mapped = assetExportersByGuideId.get(guideId);
            if (mapped != null) {
                return mapped;
            }
        }
        return assetExporter;
    }

    @Nullable
    private ResourceLocation resolveTextureIconId(GuidePageIcon icon) {
        ResourceLocation textureId = icon.resolveCurrentTextureId();
        if (textureId == null && icon.resolveCurrentTexture() != null) {
            textureId = icon.resolveCurrentTexture()
                .getSourceId();
        }
        return textureId;
    }

    private String buildTaggedSpan(String cssClass, @Nullable String templateId, String text) {
        StringBuilder html = new StringBuilder();
        html.append("<span class=\"")
            .append(escapeAttribute(cssClass))
            .append("\"");
        if (templateId != null && !templateId.isEmpty()) {
            html.append(" data-template=\"")
                .append(escapeAttribute(templateId))
                .append("\"");
        }
        html.append(">");
        html.append(escapeHtml(text));
        html.append("</span>");
        return html.toString();
    }

    private String buildTaggedSpanHtml(String cssClass, @Nullable String templateId, String innerHtml) {
        StringBuilder html = new StringBuilder();
        html.append("<span class=\"")
            .append(escapeAttribute(cssClass))
            .append("\"");
        if (templateId != null && !templateId.isEmpty()) {
            html.append(" data-template=\"")
                .append(escapeAttribute(templateId))
                .append("\"");
        }
        html.append(">")
            .append(innerHtml)
            .append("</span>");
        return html.toString();
    }

    private String buildTaggedAnchor(String cssClass, String href, @Nullable String templateId, String text) {
        StringBuilder html = new StringBuilder();
        html.append("<a class=\"")
            .append(escapeAttribute(cssClass))
            .append("\" href=\"")
            .append(escapeAttribute(href))
            .append("\"");
        if (templateId != null && !templateId.isEmpty()) {
            html.append(" data-template=\"")
                .append(escapeAttribute(templateId))
                .append("\"");
        }
        html.append(">");
        html.append(escapeHtml(text));
        html.append("</a>");
        return html.toString();
    }

    private String buildTaggedAnchorHtml(String cssClass, String href, @Nullable String templateId, String innerHtml) {
        StringBuilder html = new StringBuilder();
        html.append("<a class=\"")
            .append(escapeAttribute(cssClass))
            .append("\" href=\"")
            .append(escapeAttribute(href))
            .append("\"");
        if (templateId != null && !templateId.isEmpty()) {
            html.append(" data-template=\"")
                .append(escapeAttribute(templateId))
                .append("\"");
        }
        html.append(">")
            .append(innerHtml)
            .append("</a>");
        return html.toString();
    }

    @Nullable
    private String createTooltipTemplate(ItemTooltip tooltip, GuideSiteTemplateRegistry templates,
        @Nullable ResourceLocation currentPageId) {
        String html = GuideSiteSceneAnnotationSerializer
            .renderTooltipHtml(tooltip, currentPageId, null, itemIconResolver, templates);
        if (html == null || html.trim()
            .isEmpty()) {
            return null;
        }
        return templates.create(html);
    }

    @Nullable
    private String createTextTooltipTemplate(String text, GuideSiteTemplateRegistry templates,
        @Nullable ResourceLocation currentPageId) {
        if (text == null || text.isEmpty()) {
            return null;
        }
        String html = GuideSiteSceneAnnotationSerializer
            .renderTooltipHtml(new TextTooltip(text), currentPageId, null, itemIconResolver, templates);
        return html == null || html.trim()
            .isEmpty() ? null : templates.create(html);
    }

    private String resolvePlayerName() {
        try {
            Minecraft minecraft = Minecraft.getMinecraft();
            if (minecraft != null && minecraft.getSession() != null) {
                String username = minecraft.getSession()
                    .getUsername();
                if (username != null && !username.isEmpty()) {
                    return username;
                }
            }
        } catch (Throwable ignored) {}
        return "Player";
    }

    private String resolveKeyBindLabel(MdxJsxElementFields element) {
        String id = KeyBindTagCompiler.getKeyBindId(element);
        if (id == null || id.isEmpty()) {
            return "";
        }
        try {
            KeyBinding mapping = KeyBindTagCompiler.findMapping(id);
            if (mapping != null) {
                return KeyBindTagCompiler.describeMapping(mapping);
            }
        } catch (Throwable ignored) {}
        return id;
    }

    private String displayName(ItemStack stack) {
        return GuideSiteItemSupport.displayName(stack);
    }

    private String stripLegacyFormatting(String text) {
        return GuideSiteItemSupport.stripLegacyFormatting(text);
    }

    private String resolveItemLabelKey(String defaultNamespace, @Nullable String rawId, @Nullable String rawOre,
        @Nullable GuideItemReferenceResolver.ResolvedItemReference item) {
        if (item != null && item.registryId() != null) {
            return item.registryId()
                .toString();
        }
        if (rawId != null && !rawId.isEmpty()) {
            try {
                IdUtils.ParsedItemRef ref = IdUtils.parseItemRef(rawId, defaultNamespace);
                if (ref != null) {
                    return ref.id()
                        .toString();
                }
            } catch (IllegalArgumentException ignored) {}
            return rawId;
        }
        if (rawOre != null && !rawOre.isEmpty()) {
            return "ore:" + rawOre;
        }
        return "item";
    }

    @Nullable
    private String resolveCssColor(MdxJsxElementFields element, String defaultNamespace) {
        String symbolicId = readOptional(element, "id");
        if (symbolicId != null && !symbolicId.isEmpty()) {
            ColorValue color = resolveSymbolicColor(symbolicId, defaultNamespace);
            return color != null ? toCssColor(color) : null;
        }

        String rawColor = readOptional(element, "color");
        return parseLiteralColor(rawColor);
    }

    @Nullable
    private ColorValue resolveSymbolicColor(String id, String defaultNamespace) {
        try {
            return SymbolicColor.valueOf(id.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {}

        ResourceLocation colorId;
        try {
            colorId = IdUtils.resolveId(id, defaultNamespace);
        } catch (Exception e) {
            return null;
        }

        for (SymbolicColorResolver resolver : guide.getExtensions()
            .get(SymbolicColorResolver.EXTENSION_POINT)) {
            ColorValue color = resolver.resolve(colorId);
            if (color != null) {
                return color;
            }
        }
        return null;
    }

    @Nullable
    private String parseLiteralColor(@Nullable String rawColor) {
        if (rawColor == null || rawColor.isEmpty()) {
            return null;
        }

        String value = rawColor.trim();
        if ("transparent".equalsIgnoreCase(value)) {
            return toCssColor(ConstantColor.TRANSPARENT);
        }

        if (!value.startsWith("#")) {
            return null;
        }

        try {
            if (value.length() == 7) {
                int red = Integer.parseInt(value.substring(1, 3), 16);
                int green = Integer.parseInt(value.substring(3, 5), 16);
                int blue = Integer.parseInt(value.substring(5, 7), 16);
                return toCssColor(new ConstantColor((255 << 24) | (red << 16) | (green << 8) | blue));
            }
            if (value.length() == 9) {
                int alpha = Integer.parseInt(value.substring(1, 3), 16);
                int red = Integer.parseInt(value.substring(3, 5), 16);
                int green = Integer.parseInt(value.substring(5, 7), 16);
                int blue = Integer.parseInt(value.substring(7, 9), 16);
                return toCssColor(new ConstantColor((alpha << 24) | (red << 16) | (green << 8) | blue));
            }
        } catch (NumberFormatException ignored) {}

        return null;
    }

    private String toCssColor(ColorValue color) {
        int argb = color.resolve(LightDarkMode.LIGHT_MODE);
        int alpha = argb >>> 24 & 0xFF;
        int red = argb >>> 16 & 0xFF;
        int green = argb >>> 8 & 0xFF;
        int blue = argb & 0xFF;
        return "rgba(" + red + "," + green + "," + blue + "," + alpha / 255.0f + ")";
    }

    private String resolveFlexAlignment(@Nullable String value) {
        if ("center".equalsIgnoreCase(value)) {
            return "center";
        }
        if ("end".equalsIgnoreCase(value)) {
            return "flex-end";
        }
        return "flex-start";
    }

    @Nullable
    private List<StructureBlockView> parseStructureBlocks(MdxJsxElementFields element,
        @Nullable ResourceLocation currentPageId, GuideSiteTemplateRegistry templates) {
        StringBuilder rawText = new StringBuilder();
        collectStructureText(rawText, element.children());

        List<StructureBlockView> blocks = new ArrayList<>();
        boolean[] failed = new boolean[1];
        GuideStringLines.visitLines(rawText.toString(), (rawLine, lineIndex) -> {
            String line = rawLine.trim();
            if (line.isEmpty() || line.startsWith("#")) {
                return true;
            }

            StructureBlockView block = parseStructureBlock(line, currentPageId, templates);
            if (block == null) {
                failed[0] = true;
                return false;
            }
            blocks.add(block);
            return true;
        });
        return failed[0] ? null : blocks;
    }

    private void collectStructureText(StringBuilder text, List<? extends MdAstAnyContent> children) {
        for (MdAstAnyContent child : children) {
            if (child instanceof MdAstLiteral literal) {
                text.append(literal.value())
                    .append('\n');
                continue;
            }
            if (child instanceof MdAstBreak) {
                text.append('\n');
                continue;
            }
            if (child instanceof MdAstParent<?>parent) {
                collectStructureText(text, parent.children());
            }
        }
    }

    @Nullable
    private StructureBlockView parseStructureBlock(String line, @Nullable ResourceLocation currentPageId,
        GuideSiteTemplateRegistry templates) {
        List<String> parts = splitWhitespaceTokens(line, 4);
        if (parts.size() < 4) {
            return null;
        }

        int x;
        int y;
        int z;
        try {
            x = Integer.parseInt(parts.get(0));
            y = Integer.parseInt(parts.get(1));
            z = Integer.parseInt(parts.get(2));
        } catch (NumberFormatException e) {
            return null;
        }

        String idSpec = parts.get(3);
        int meta = 0;
        int firstColon = idSpec.indexOf(':');
        int lastColon = idSpec.lastIndexOf(':');
        String resourceId = idSpec;
        if (firstColon != lastColon) {
            try {
                meta = Integer.parseInt(idSpec.substring(lastColon + 1));
                resourceId = idSpec.substring(0, lastColon);
            } catch (NumberFormatException ignored) {}
        }

        ItemStack stack = StructureViewCompiler.resolveStack(resourceId, meta);
        GuideSiteExportedItem exportedItem;
        String displayName;
        @Nullable
        String templateId;
        String itemId = meta != 0 ? resourceId + ":" + meta : resourceId;
        if (stack != null) {
            exportedItem = GuideSiteItemSupport.export(null, stack, itemIconResolver, itemId);
            displayName = exportedItem.displayName();
            templateId = createTooltipTemplate(new ItemTooltip(stack), templates, currentPageId);
        } else {
            exportedItem = GuideSiteItemSupport.unresolved(itemId);
            displayName = exportedItem.displayName();
            templateId = createTextTooltipTemplate(itemId, templates, currentPageId);
        }

        return new StructureBlockView(
            x,
            y,
            z,
            LytStructureView.projectX(x, z),
            LytStructureView.projectY(x, y, z),
            exportedItem,
            displayName,
            abbreviateStructureLabel(displayName, exportedItem.itemId()),
            templateId);
    }

    private List<String> splitWhitespaceTokens(String text, int limit) {
        List<String> tokens = new ArrayList<>(Math.max(1, limit));
        int start = -1;
        for (int index = 0, length = text.length(); index <= length; index++) {
            char value = index < length ? text.charAt(index) : ' ';
            if (Character.isWhitespace(value)) {
                if (start >= 0) {
                    tokens.add(text.substring(start, index));
                    if (limit > 0 && tokens.size() >= limit) {
                        return tokens;
                    }
                    start = -1;
                }
            } else if (start < 0) {
                start = index;
            }
        }
        return tokens;
    }

    private String abbreviateStructureLabel(String displayName, String itemId) {
        String cleaned = displayName != null ? displayName.trim() : "";
        if (!cleaned.isEmpty()) {
            int lastSlash = cleaned.lastIndexOf('/');
            if (lastSlash >= 0 && lastSlash + 1 < cleaned.length()) {
                cleaned = cleaned.substring(lastSlash + 1);
            }
            int lastColon = cleaned.lastIndexOf(':');
            if (lastColon >= 0 && lastColon + 1 < cleaned.length()) {
                cleaned = cleaned.substring(lastColon + 1);
            }
            cleaned = cleaned.replace('_', ' ')
                .replace('-', ' ');

            StringBuilder initials = new StringBuilder();
            for (String word : splitWhitespaceTokens(cleaned, 3)) {
                char ch = word.charAt(0);
                if (Character.isLetterOrDigit(ch)) {
                    initials.append(Character.toUpperCase(ch));
                }
                if (initials.length() >= 3) {
                    break;
                }
            }
            if (initials.length() > 0) {
                return initials.toString();
            }

            String compact = keepAsciiLettersAndDigits(cleaned);
            if (!compact.isEmpty()) {
                return compact.substring(0, Math.min(3, compact.length()))
                    .toUpperCase(Locale.ROOT);
            }
        }

        String fallback = itemId != null ? itemId : "BLK";
        int slash = fallback.lastIndexOf('/');
        if (slash >= 0 && slash + 1 < fallback.length()) {
            fallback = fallback.substring(slash + 1);
        }
        int colon = fallback.lastIndexOf(':');
        if (colon >= 0 && colon + 1 < fallback.length()) {
            fallback = fallback.substring(colon + 1);
        }
        fallback = keepAsciiLettersAndDigits(fallback);
        if (fallback.isEmpty()) {
            fallback = "BLK";
        }
        return fallback.substring(0, Math.min(3, fallback.length()))
            .toUpperCase(Locale.ROOT);
    }

    private String keepAsciiLettersAndDigits(String value) {
        StringBuilder builder = new StringBuilder(value.length());
        for (int index = 0; index < value.length(); index++) {
            char ch = value.charAt(index);
            if ((ch >= 'A' && ch <= 'Z') || (ch >= 'a' && ch <= 'z') || (ch >= '0' && ch <= '9')) {
                builder.append(ch);
            }
        }
        return builder.toString();
    }

    private String resolveSourcePack(ResourceLocation pageId) {
        ParsedGuidePage parsedPage = parsedPagesById.get(pageId);
        if (parsedPage != null && parsedPage.getSourcePack() != null) {
            return parsedPage.getSourcePack();
        }
        return "site-export";
    }

    @Nullable
    private Integer readPositiveInt(@Nullable String raw) {
        if (raw == null || raw.trim()
            .isEmpty()) {
            return null;
        }
        try {
            int value = Integer.parseInt(raw.trim());
            return value > 0 ? value : null;
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private String renderFallbackItemLabel(String itemId, @Nullable ResourceLocation currentPageId,
        GuideSiteTemplateRegistry templates, boolean inline, @Nullable Float scale) {
        GuideSiteExportedItem exportedItem = GuideSiteItemSupport.unresolved(itemId);
        String templateId = createTextTooltipTemplate(itemId, templates, currentPageId);
        StringBuilder classes = new StringBuilder(inline ? "guide-inline-item" : "guide-block-item");
        if (templateId != null) {
            classes.append(" guide-tooltip");
        }
        StringBuilder style = new StringBuilder();
        if (scale != null && scale > 0f && scale != 1.0f) {
            style.append("font-size:")
                .append(scale)
                .append("em;");
        }
        StringBuilder html = new StringBuilder();
        if (!inline) {
            html.append("<div class=\"guide-block-item-row\">");
        }
        html.append("<span class=\"")
            .append(escapeAttribute(classes.toString()))
            .append("\" data-item-id=\"")
            .append(escapeAttribute(exportedItem.itemId()))
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
        html.append(">");
        GuideSiteItemHtml.appendSummaryContent(
            html,
            exportedItem,
            inline ? "guide-inline-item-icon" : "guide-block-item-icon",
            "guide-item-summary-text");
        html.append("</span>");
        if (!inline) {
            html.append("</div>");
        }
        return html.toString();
    }

    private String buildItemSummaryContent(GuideSiteExportedItem item, @Nullable String iconClass, String textClass) {
        StringBuilder html = new StringBuilder();
        GuideSiteItemHtml.appendSummaryContent(html, item, iconClass, textClass);
        return html.toString();
    }

    @Nullable
    private PageAnchor findPageAnchorByItemId(String itemId) {
        if (itemId == null || itemId.isEmpty()) {
            return null;
        }
        return indexItemAnchorsByItemId().get(itemId);
    }

    private Map<String, PageAnchor> indexItemAnchorsByItemId() {
        if (itemAnchorsByItemId != null) {
            return itemAnchorsByItemId;
        }

        var indexedAnchors = new LinkedHashMap<String, PageAnchor>();
        for (ParsedGuidePage page : parsedPagesById.values()) {
            Object rawItemIds = page.getFrontmatter()
                .additionalProperties()
                .get("item_ids");
            if (!(rawItemIds instanceof List<?>values)) {
                continue;
            }
            PageAnchor pageAnchor = PageAnchor.page(page.getId());
            for (Object value : values) {
                if (!(value instanceof String rawValue)) {
                    continue;
                }
                String normalized = resolveItemLabelKey(
                    page.getId()
                        .getResourceDomain(),
                    rawValue,
                    null,
                    null);
                if (!normalized.isEmpty()) {
                    indexedAnchors.putIfAbsent(normalized, pageAnchor);
                }
            }
        }
        itemAnchorsByItemId = indexedAnchors;
        return indexedAnchors;
    }

    @Nullable
    private String readOptional(MdxJsxElementFields element, String name) {
        return readOptional(element.getAttribute(name));
    }

    @Nullable
    private String readOptional(@Nullable MdxJsxAttribute attribute) {
        if (attribute == null) {
            return null;
        }
        if (attribute.hasStringValue()) {
            return attribute.getStringValue();
        }
        if (attribute.hasExpressionValue()) {
            return attribute.getExpressionValue();
        }
        return "";
    }

    private boolean readBoolean(MdxJsxElementFields element, String name, boolean fallback) {
        return readBoolean(element.getAttribute(name), name, fallback);
    }

    private boolean readBoolean(@Nullable MdxJsxAttribute attribute, String name, boolean fallback) {
        try {
            return MdxAttrs.getBoolean(MdxAttrs.parseOptionalBoolean(attribute, name), fallback);
        } catch (MdxAttrs.AttributeException ignored) {
            return fallback;
        }
    }

    private int readInt(MdxJsxElementFields element, String name, int fallback) {
        String raw = readOptional(element, name);
        if (raw == null || raw.isEmpty()) {
            return fallback;
        }
        try {
            return Integer.parseInt(raw.trim());
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    @Nullable
    private Float readFloat(MdxJsxElementFields element, String name) {
        String raw = readOptional(element, name);
        if (raw == null || raw.isEmpty()) {
            return null;
        }
        try {
            return Float.parseFloat(raw);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private String renderError(String message) {
        return "<span class=\"guide-export-error\">" + escapeHtml(message) + "</span>";
    }

    private String escapeHtml(String text) {
        return text.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;");
    }

    private String escapeAttribute(String text) {
        return escapeHtml(text);
    }

    private static class StructureBlockView {

        private final int x;
        private final int y;
        private final int z;
        private final int projectedX;
        private final int projectedY;
        private final GuideSiteExportedItem item;
        private final String displayName;
        private final String abbreviation;
        @Nullable
        private final String templateId;

        private StructureBlockView(int x, int y, int z, int projectedX, int projectedY, GuideSiteExportedItem item,
            String displayName, String abbreviation, @Nullable String templateId) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.projectedX = projectedX;
            this.projectedY = projectedY;
            this.item = item;
            this.displayName = displayName;
            this.abbreviation = abbreviation;
            this.templateId = templateId;
        }
    }

    private static class StructureLegendEntry {

        private final GuideSiteExportedItem item;
        private final String abbreviation;
        @Nullable
        private final String templateId;
        private int count = 1;

        private StructureLegendEntry(GuideSiteExportedItem item, String abbreviation, @Nullable String templateId) {
            this.item = item;
            this.abbreviation = abbreviation;
            this.templateId = templateId;
        }
    }
}
