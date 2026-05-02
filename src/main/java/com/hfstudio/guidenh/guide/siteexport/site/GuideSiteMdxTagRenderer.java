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
import com.hfstudio.guidenh.guide.compiler.ParsedGuidePage;
import com.hfstudio.guidenh.guide.compiler.tags.CommandLinkCompiler;
import com.hfstudio.guidenh.guide.compiler.tags.ItemImageCompiler;
import com.hfstudio.guidenh.guide.compiler.tags.KeyBindTagCompiler;
import com.hfstudio.guidenh.guide.compiler.tags.StructureViewCompiler;
import com.hfstudio.guidenh.guide.compiler.tags.SubPagesCompiler;
import com.hfstudio.guidenh.guide.compiler.tags.chart.ChartAttrParser;
import com.hfstudio.guidenh.guide.document.block.LytStructureView;
import com.hfstudio.guidenh.guide.document.block.functiongraph.DomainPredicate;
import com.hfstudio.guidenh.guide.document.block.functiongraph.FunctionExprParser;
import com.hfstudio.guidenh.guide.document.block.functiongraph.FunctionGraphPalette;
import com.hfstudio.guidenh.guide.document.block.functiongraph.FunctionPlot;
import com.hfstudio.guidenh.guide.document.interaction.ItemTooltip;
import com.hfstudio.guidenh.guide.document.interaction.TextTooltip;
import com.hfstudio.guidenh.guide.indices.CategoryIndex;
import com.hfstudio.guidenh.guide.indices.ItemIndex;
import com.hfstudio.guidenh.guide.internal.MutableGuide;
import com.hfstudio.guidenh.guide.internal.mermaid.MermaidMindmapDocument;
import com.hfstudio.guidenh.guide.internal.mermaid.MermaidMindmapParser;
import com.hfstudio.guidenh.guide.navigation.NavigationNode;
import com.hfstudio.guidenh.guide.navigation.NavigationTree;
import com.hfstudio.guidenh.libs.mdast.mdx.model.MdxJsxAttribute;
import com.hfstudio.guidenh.libs.mdast.mdx.model.MdxJsxElementFields;
import com.hfstudio.guidenh.libs.mdast.model.MdAstAnyContent;
import com.hfstudio.guidenh.libs.mdast.model.MdAstBreak;
import com.hfstudio.guidenh.libs.mdast.model.MdAstLiteral;
import com.hfstudio.guidenh.libs.mdast.model.MdAstParent;

public class GuideSiteMdxTagRenderer implements GuideSiteHtmlCompiler.MdxTagRenderer {

    private static final Comparator<StructureBlockView> STRUCTURE_DRAW_ORDER = Comparator
        .comparingInt((StructureBlockView block) -> block.y)
        .thenComparing(
            Comparator.comparingInt((StructureBlockView block) -> block.z)
                .reversed())
        .thenComparingInt(block -> block.x);

    private final MutableGuide guide;
    private final Map<ResourceLocation, ParsedGuidePage> parsedPagesById;
    private final NavigationTree navigationTree;
    @Nullable
    private final GuideSitePageAssetExporter assetExporter;
    private final GuideSiteItemIconResolver itemIconResolver;

    public GuideSiteMdxTagRenderer(MutableGuide guide, Map<ResourceLocation, ParsedGuidePage> parsedPagesById,
        NavigationTree navigationTree) {
        this(guide, parsedPagesById, navigationTree, null, GuideSiteItemIconResolver.NONE);
    }

    public GuideSiteMdxTagRenderer(MutableGuide guide, Map<ResourceLocation, ParsedGuidePage> parsedPagesById,
        NavigationTree navigationTree, GuideSiteItemIconResolver itemIconResolver) {
        this(guide, parsedPagesById, navigationTree, null, itemIconResolver);
    }

    public GuideSiteMdxTagRenderer(MutableGuide guide, Map<ResourceLocation, ParsedGuidePage> parsedPagesById,
        NavigationTree navigationTree, @Nullable GuideSitePageAssetExporter assetExporter,
        GuideSiteItemIconResolver itemIconResolver) {
        this.guide = guide;
        this.parsedPagesById = parsedPagesById;
        this.navigationTree = navigationTree;
        this.assetExporter = assetExporter;
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
            return renderBlockImage(element, defaultNamespace, currentPageId, templates);
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
        if ("CategoryIndex".equals(name)) {
            return renderCategoryIndex(element, currentPageId);
        }
        if ("Color".equals(name)) {
            return renderColor(element, defaultNamespace, currentPageId, templates, sceneResolver, compiler);
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
        if ("CommandLink".equals(name)) {
            return renderCommandLink(element, defaultNamespace, currentPageId, templates, sceneResolver, compiler);
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
            return renderFileTree(element);
        }
        if ("FootnoteList".equals(name)) {
            return "<div class=\"guide-footnote-list\">"
                + compiler
                    .compileFragment(element.children(), templates, defaultNamespace, sceneResolver, currentPageId)
                + "</div>";
        }
        if ("Mermaid".equals(name)) {
            return renderMermaid(element);
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
        return null;
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

        return renderItemStack(
            item.registryId(),
            item.stack(),
            currentPageId,
            templates,
            inline,
            readFloat(element, "scale"),
            !ItemImageCompiler.parseBool(readOptional(element, "noTooltip")));
    }

    private String renderItemStack(ResourceLocation registryId, ItemStack stack,
        @Nullable ResourceLocation currentPageId, GuideSiteTemplateRegistry templates, boolean inline,
        @Nullable Float scale, boolean includeTooltip) {
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

    private String renderBlockImage(MdxJsxElementFields element, String defaultNamespace,
        @Nullable ResourceLocation currentPageId, GuideSiteTemplateRegistry templates) {
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
        return renderItemStack(
            block.registryId(),
            block.stack(),
            currentPageId,
            templates,
            false,
            readFloat(element, "scale"),
            true);
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
        String templateId = item != null && item.stack() != null
            ? createTooltipTemplate(new ItemTooltip(item.stack()), templates, currentPageId)
            : createTextTooltipTemplate(itemId, templates, currentPageId);
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

        boolean samePageLink = linksTo != null && linksTo.anchor() == null
            && currentPageId != null
            && currentPageId.equals(linksTo.pageId());
        if (linksTo == null || samePageLink) {
            return buildTaggedSpanHtml(
                "guide-item-link guide-tooltip",
                templateId,
                buildItemSummaryContent(exportedItem, "guide-item-link-icon", "guide-item-link-text"));
        }

        return buildTaggedAnchorHtml(
            "guide-item-link guide-tooltip",
            GuideSiteHrefResolver.resolvePageAnchor(currentPageId, linksTo),
            templateId,
            buildItemSummaryContent(exportedItem, "guide-item-link-icon", "guide-item-link-text"));
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

    private String renderCategoryIndex(MdxJsxElementFields element, @Nullable ResourceLocation currentPageId) {
        String category = readOptional(element, "category");
        if (category == null || category.isEmpty()) {
            return renderError("Missing category");
        }

        List<PageAnchor> anchors;
        try {
            anchors = guide.getIndex(CategoryIndex.class)
                .get(category);
        } catch (Exception ignored) {
            anchors = new ArrayList<>();
        }
        return renderPageAnchorList(anchors, currentPageId);
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
        StringBuilder html = new StringBuilder();
        html.append("<details");
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
            html.append("<summary>Details</summary>");
        }
        html.append(
            compiler.compileFragment(
                children.subList(bodyStart, children.size()),
                templates,
                defaultNamespace,
                sceneResolver,
                currentPageId));
        html.append("</details>");
        return html.toString();
    }

    private String renderFileTree(MdxJsxElementFields element) {
        StringBuilder text = new StringBuilder();
        collectStructureText(text, element.children());
        return GuideSiteGraphRenderer.renderFileTree(
            text.toString()
                .trim());
    }

    private String renderMermaid(MdxJsxElementFields element) {
        StringBuilder text = new StringBuilder();
        collectStructureText(text, element.children());
        String source = text.toString()
            .trim();
        if (source.isEmpty()) {
            return renderError("Empty Mermaid diagram");
        }
        try {
            MermaidMindmapDocument doc = MermaidMindmapParser.parse(source);
            return GuideSiteGraphRenderer.renderMermaidTree(doc);
        } catch (Exception ex) {
            return "<pre><code class=\"language-mermaid\">" + escapeHtml(source) + "</code></pre>";
        }
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
            return "<table class=\"guide-csv-table\"></table>";
        }
        return GuideSiteGraphRenderer.renderCsvTable(csvText, hasHeader);
    }

    private String renderColumnChart(MdxJsxElementFields element) {
        int w = readInt(element, "width", 320);
        int h = readInt(element, "height", 200);
        int bgColor = parseArgbAttr(element, "background", 0xFF1B1F23);
        int borderColor = parseArgbAttr(element, "border", 0xFF3A4047);
        String title = readOptional(element, "title");
        String[] categories = ChartAttrParser.parseStringArray(readOptional(element, "categories"));
        boolean showLegend = readBoolean(element, "showLegend", true);
        List<GuideSiteGraphRenderer.SeriesData> series = parseSeriesChildren(element);
        return GuideSiteGraphRenderer
            .renderColumnChart(w, h, bgColor, borderColor, title, categories, series, showLegend);
    }

    private String renderBarChart(MdxJsxElementFields element) {
        int w = readInt(element, "width", 320);
        int h = readInt(element, "height", 200);
        int bgColor = parseArgbAttr(element, "background", 0xFF1B1F23);
        int borderColor = parseArgbAttr(element, "border", 0xFF3A4047);
        String title = readOptional(element, "title");
        String[] categories = ChartAttrParser.parseStringArray(readOptional(element, "categories"));
        boolean showLegend = readBoolean(element, "showLegend", true);
        List<GuideSiteGraphRenderer.SeriesData> series = parseSeriesChildren(element);
        return GuideSiteGraphRenderer.renderBarChart(w, h, bgColor, borderColor, title, categories, series, showLegend);
    }

    private String renderLineChart(MdxJsxElementFields element) {
        int w = readInt(element, "width", 320);
        int h = readInt(element, "height", 200);
        int bgColor = parseArgbAttr(element, "background", 0xFF1B1F23);
        int borderColor = parseArgbAttr(element, "border", 0xFF3A4047);
        String title = readOptional(element, "title");
        String[] categories = ChartAttrParser.parseStringArray(readOptional(element, "categories"));
        boolean numericX = readBoolean(element, "numericX", false);
        boolean showPoints = readBoolean(element, "showPoints", true);
        boolean showLegend = readBoolean(element, "showLegend", true);
        List<GuideSiteGraphRenderer.SeriesData> series = parseSeriesChildren(element);
        return GuideSiteGraphRenderer
            .renderLineChart(w, h, bgColor, borderColor, title, categories, series, numericX, showPoints, showLegend);
    }

    private String renderPieChart(MdxJsxElementFields element) {
        int w = readInt(element, "width", 320);
        int h = readInt(element, "height", 200);
        int bgColor = parseArgbAttr(element, "background", 0xFF1B1F23);
        int borderColor = parseArgbAttr(element, "border", 0xFF3A4047);
        String title = readOptional(element, "title");
        boolean showLegend = readBoolean(element, "showLegend", true);
        List<GuideSiteGraphRenderer.SliceData> slices = parseSliceChildren(element);
        return GuideSiteGraphRenderer.renderPieChart(w, h, bgColor, borderColor, title, slices, showLegend);
    }

    private String renderScatterChart(MdxJsxElementFields element) {
        int w = readInt(element, "width", 320);
        int h = readInt(element, "height", 200);
        int bgColor = parseArgbAttr(element, "background", 0xFF1B1F23);
        int borderColor = parseArgbAttr(element, "border", 0xFF3A4047);
        String title = readOptional(element, "title");
        boolean showLegend = readBoolean(element, "showLegend", true);
        List<GuideSiteGraphRenderer.SeriesData> series = parseScatterSeriesChildren(element);
        return GuideSiteGraphRenderer.renderScatterChart(w, h, bgColor, borderColor, title, series, showLegend);
    }

    private String renderFunctionGraphTag(MdxJsxElementFields element) {
        int w = readInt(element, "width", 320);
        int h = readInt(element, "height", 220);
        int bgColor = parseArgbAttr(element, "background", 0xFF1B1F23);
        int borderColor = parseArgbAttr(element, "border", 0xFF3A4047);
        int axisColor = parseArgbAttr(element, "axisColor", 0xFFB8C2CF);
        int gridColor = parseArgbAttr(element, "gridColor", 0x33B8C2CF);
        boolean showGrid = readBoolean(element, "showGrid", true);
        boolean showAxes = readBoolean(element, "showAxes", true);
        String title = readOptional(element, "title");
        double xMin = parseDoubleAttr(element, "xMin", -10);
        double xMax = parseDoubleAttr(element, "xMax", 10);
        double yMin = parseDoubleAttr(element, "yMin", Double.NaN);
        double yMax = parseDoubleAttr(element, "yMax", Double.NaN);
        String xRange = readOptional(element, "xRange");
        if (xRange != null && xRange.contains("..")) {
            String[] parts = xRange.split("\\.\\.");
            if (parts.length == 2) {
                try {
                    xMin = Double.parseDouble(parts[0].trim());
                } catch (NumberFormatException ignored) {}
                try {
                    xMax = Double.parseDouble(parts[1].trim());
                } catch (NumberFormatException ignored) {}
            }
        }
        String yRange = readOptional(element, "yRange");
        if (yRange != null && yRange.contains("..")) {
            String[] parts = yRange.split("\\.\\.");
            if (parts.length == 2) {
                try {
                    yMin = Double.parseDouble(parts[0].trim());
                } catch (NumberFormatException ignored) {}
                try {
                    yMax = Double.parseDouble(parts[1].trim());
                } catch (NumberFormatException ignored) {}
            }
        }
        List<FunctionPlot> plots = parsePlotChildren(element);
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
        return GuideSiteGraphRenderer.renderFunctionGraphSvg(
            plots,
            new ArrayList<>(),
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
            yMax);
    }

    /** Parse {@code <Series name="..." data="1,2,3" color="...">} children for bar/column/line charts. */
    private List<GuideSiteGraphRenderer.SeriesData> parseSeriesChildren(MdxJsxElementFields element) {
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
            double[] ys = ChartAttrParser.parseDoubleArray(readOptional(c, "data"));
            double[] xs = new double[ys.length];
            for (int i = 0; i < xs.length; i++) xs[i] = i;
            result.add(new GuideSiteGraphRenderer.SeriesData(name, color, xs, ys));
            idx++;
        }
        return result;
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
            StringBuilder exprBuf = new StringBuilder();
            collectStructureText(exprBuf, c.children());
            String exprText = exprBuf.toString()
                .trim();
            boolean inverse = readBoolean(c, "inverse", false);
            int color = parseArgbAttr(c, "color", FunctionGraphPalette.color(idx));
            String label = readOptional(c, "label");
            DomainPredicate domain = DomainPredicate.parse(readOptional(c, "domain"));
            result.add(
                new FunctionPlot(
                    exprText,
                    FunctionExprParser.parse(exprText, inverse ? 1 : 0),
                    inverse,
                    domain,
                    color,
                    label != null ? label : exprText));
            idx++;
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
                        GuideSiteHrefResolver.resolvePageAnchor(currentPageId, PageAnchor.page(node.pageId()))))
                .append("\">")
                .append(buildGuideLinkContent(node.title(), node.icon()))
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
            String href = GuideSiteHrefResolver.resolvePageAnchor(currentPageId, anchor);
            if (!seen.add(href)) {
                continue;
            }
            NavigationNode node = navigationTree.getNodeById(anchor.pageId());
            html.append("<li><a href=\"")
                .append(escapeAttribute(href))
                .append("\">")
                .append(buildGuideLinkContent(title, node != null ? node.icon() : null))
                .append("</a></li>");
        }
        html.append("</ul>");
        return html.toString();
    }

    private String buildGuideLinkContent(String title, @Nullable GuidePageIcon icon) {
        StringBuilder html = new StringBuilder();
        if (icon != null && icon.isItemIcon() && icon.itemStack() != null) {
            GuideSiteItemHtml.appendIcon(
                html,
                GuideSiteItemSupport.export(icon.itemStack(), itemIconResolver),
                "guide-nav-item-icon");
        } else if (icon != null && icon.textureId() != null && assetExporter != null) {
            String src = assetExporter.exportResource(icon.textureId());
            if (!src.isEmpty()) {
                html.append("<img class=\"item-icon guide-nav-item-icon\" src=\"")
                    .append(escapeAttribute(src))
                    .append("\" alt=\"\" width=\"32\" height=\"32\" decoding=\"async\">");
            }
        }
        html.append("<span class=\"guide-generated-link-text\">")
            .append(escapeHtml(title))
            .append("</span>");
        return html.toString();
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
        String id = readOptional(element, "id");
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
        for (String rawLine : rawText.toString()
            .split("\\R")) {
            String line = rawLine.trim();
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }

            StructureBlockView block = parseStructureBlock(line, currentPageId, templates);
            if (block == null) {
                return null;
            }
            blocks.add(block);
        }
        return blocks;
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
        String[] parts = line.split("\\s+");
        if (parts.length < 4) {
            return null;
        }

        int x;
        int y;
        int z;
        try {
            x = Integer.parseInt(parts[0]);
            y = Integer.parseInt(parts[1]);
            z = Integer.parseInt(parts[2]);
        } catch (NumberFormatException e) {
            return null;
        }

        String idSpec = parts[3];
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

            String[] words = cleaned.split("\\s+");
            StringBuilder initials = new StringBuilder();
            for (String word : words) {
                if (word.isEmpty()) {
                    continue;
                }
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

            String compact = cleaned.replaceAll("[^A-Za-z0-9]", "");
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
        fallback = fallback.replaceAll("[^A-Za-z0-9]", "");
        if (fallback.isEmpty()) {
            fallback = "BLK";
        }
        return fallback.substring(0, Math.min(3, fallback.length()))
            .toUpperCase(Locale.ROOT);
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
        for (ParsedGuidePage page : parsedPagesById.values()) {
            Object rawItemIds = page.getFrontmatter()
                .additionalProperties()
                .get("item_ids");
            if (!(rawItemIds instanceof List<?>values)) {
                continue;
            }
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
                if (itemId.equals(normalized)) {
                    return PageAnchor.page(page.getId());
                }
            }
        }
        return null;
    }

    @Nullable
    private String readOptional(MdxJsxElementFields element, String name) {
        MdxJsxAttribute attribute = element.getAttribute(name);
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
        MdxJsxAttribute attribute = element.getAttribute(name);
        if (attribute == null) {
            return fallback;
        }
        String value = attribute.hasExpressionValue() ? attribute.getExpressionValue()
            : attribute.hasStringValue() ? attribute.getStringValue() : "";
        if ("true".equalsIgnoreCase(value)) {
            return true;
        }
        if ("false".equalsIgnoreCase(value)) {
            return false;
        }
        return fallback;
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

    private static final class StructureBlockView {

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

    private static final class StructureLegendEntry {

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
