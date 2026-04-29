package com.hfstudio.guidenh.guide.siteexport.site;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;

import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.hfstudio.guidenh.guide.PageAnchor;
import com.hfstudio.guidenh.guide.color.ColorValue;
import com.hfstudio.guidenh.guide.color.LightDarkMode;
import com.hfstudio.guidenh.guide.document.block.LytBlock;
import com.hfstudio.guidenh.guide.document.block.LytDocument;
import com.hfstudio.guidenh.guide.document.block.LytHeading;
import com.hfstudio.guidenh.guide.document.block.LytImage;
import com.hfstudio.guidenh.guide.document.block.LytItemGrid;
import com.hfstudio.guidenh.guide.document.block.LytItemImage;
import com.hfstudio.guidenh.guide.document.block.LytList;
import com.hfstudio.guidenh.guide.document.block.LytListItem;
import com.hfstudio.guidenh.guide.document.block.LytNode;
import com.hfstudio.guidenh.guide.document.block.LytParagraph;
import com.hfstudio.guidenh.guide.document.block.LytSlot;
import com.hfstudio.guidenh.guide.document.block.LytSlotGrid;
import com.hfstudio.guidenh.guide.document.block.LytThematicBreak;
import com.hfstudio.guidenh.guide.document.block.table.LytTable;
import com.hfstudio.guidenh.guide.document.block.table.LytTableCell;
import com.hfstudio.guidenh.guide.document.block.table.LytTableRow;
import com.hfstudio.guidenh.guide.document.flow.LytFlowAnchor;
import com.hfstudio.guidenh.guide.document.flow.LytFlowBreak;
import com.hfstudio.guidenh.guide.document.flow.LytFlowContent;
import com.hfstudio.guidenh.guide.document.flow.LytFlowInlineBlock;
import com.hfstudio.guidenh.guide.document.flow.LytFlowLink;
import com.hfstudio.guidenh.guide.document.flow.LytFlowSpan;
import com.hfstudio.guidenh.guide.document.flow.LytFlowText;
import com.hfstudio.guidenh.guide.document.interaction.ContentTooltip;
import com.hfstudio.guidenh.guide.document.interaction.GuideTooltip;
import com.hfstudio.guidenh.guide.document.interaction.ItemTooltip;
import com.hfstudio.guidenh.guide.document.interaction.TextTooltip;
import com.hfstudio.guidenh.guide.internal.tooltip.GuideItemTooltipLines;
import com.hfstudio.guidenh.guide.scene.LytGuidebookScene;
import com.hfstudio.guidenh.guide.scene.annotation.DiamondAnnotation;
import com.hfstudio.guidenh.guide.scene.annotation.InWorldBlockFaceOverlayAnnotation;
import com.hfstudio.guidenh.guide.scene.annotation.InWorldBoxAnnotation;
import com.hfstudio.guidenh.guide.scene.annotation.InWorldLineAnnotation;
import com.hfstudio.guidenh.guide.scene.annotation.SceneAnnotation;
import com.hfstudio.guidenh.guide.style.ResolvedTextStyle;
import com.hfstudio.guidenh.guide.style.TextAlignment;

public final class GuideSiteSceneAnnotationSerializer {

    private static final float ANNOTATION_THICKNESS_SCALE = 32.0f;
    private static final float MIN_EXPORTED_WORLD_THICKNESS = 1.0f / 256.0f;
    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping()
        .serializeNulls()
        .create();

    private GuideSiteSceneAnnotationSerializer() {}

    public static AnnotationPayload serialize(LytGuidebookScene scene, GuideSiteTemplateRegistry templates) {
        return serialize(scene, templates, null, null, GuideSiteItemIconResolver.NONE);
    }

    public static AnnotationPayload serialize(LytGuidebookScene scene, GuideSiteTemplateRegistry templates,
        @Nullable ResourceLocation currentPageId) {
        return serialize(scene, templates, currentPageId, null, GuideSiteItemIconResolver.NONE);
    }

    public static AnnotationPayload serialize(LytGuidebookScene scene, GuideSiteTemplateRegistry templates,
        @Nullable ResourceLocation currentPageId, @Nullable GuideSitePageAssetExporter assetExporter) {
        return serialize(scene, templates, currentPageId, assetExporter, GuideSiteItemIconResolver.NONE);
    }

    public static AnnotationPayload serialize(LytGuidebookScene scene, GuideSiteTemplateRegistry templates,
        @Nullable ResourceLocation currentPageId, @Nullable GuideSitePageAssetExporter assetExporter,
        GuideSiteItemIconResolver itemIconResolver) {
        List<Map<String, Object>> inWorld = new ArrayList<>();
        List<Map<String, Object>> overlay = new ArrayList<>();

        if (scene != null) {
            for (SceneAnnotation annotation : scene.getAnnotations()) {
                if (annotation instanceof InWorldBoxAnnotation box) {
                    inWorld.add(serializeBox(box, templates, currentPageId, assetExporter, itemIconResolver));
                    continue;
                }
                if (annotation instanceof InWorldLineAnnotation line) {
                    inWorld.add(serializeLine(line, templates, currentPageId, assetExporter, itemIconResolver));
                    continue;
                }
                if (annotation instanceof InWorldBlockFaceOverlayAnnotation blockOverlay) {
                    inWorld.add(
                        serializeBlockOverlay(blockOverlay, templates, currentPageId, assetExporter, itemIconResolver));
                    continue;
                }
                if (annotation instanceof DiamondAnnotation diamond) {
                    overlay.add(serializeDiamond(diamond, templates, currentPageId, assetExporter, itemIconResolver));
                }
            }
        }

        return new AnnotationPayload(GSON.toJson(inWorld), GSON.toJson(overlay));
    }

    private static Map<String, Object> serializeBox(InWorldBoxAnnotation box, GuideSiteTemplateRegistry templates,
        @Nullable ResourceLocation currentPageId, @Nullable GuideSitePageAssetExporter assetExporter,
        GuideSiteItemIconResolver itemIconResolver) {
        Map<String, Object> data = createBaseInWorldAnnotation(
            "box",
            box.color(),
            box.thickness(),
            box.isAlwaysOnTop(),
            box.getTooltip(),
            templates,
            currentPageId,
            assetExporter,
            itemIconResolver);
        data.put("minCorner", toVector(box.min()));
        data.put("maxCorner", toVector(box.max()));
        return data;
    }

    private static Map<String, Object> serializeLine(InWorldLineAnnotation line, GuideSiteTemplateRegistry templates,
        @Nullable ResourceLocation currentPageId, @Nullable GuideSitePageAssetExporter assetExporter,
        GuideSiteItemIconResolver itemIconResolver) {
        Map<String, Object> data = createBaseInWorldAnnotation(
            "line",
            line.color(),
            line.thickness(),
            line.isAlwaysOnTop(),
            line.getTooltip(),
            templates,
            currentPageId,
            assetExporter,
            itemIconResolver);
        data.put("from", toVector(line.from()));
        data.put("to", toVector(line.to()));
        return data;
    }

    private static Map<String, Object> serializeBlockOverlay(InWorldBlockFaceOverlayAnnotation blockOverlay,
        GuideSiteTemplateRegistry templates, @Nullable ResourceLocation currentPageId,
        @Nullable GuideSitePageAssetExporter assetExporter, GuideSiteItemIconResolver itemIconResolver) {
        Map<String, Object> data = createBaseInWorldAnnotation(
            "box",
            blockOverlay.color(),
            InWorldBoxAnnotation.DEFAULT_THICKNESS,
            blockOverlay.isAlwaysOnTop(),
            blockOverlay.getTooltip(),
            templates,
            currentPageId,
            assetExporter,
            itemIconResolver);
        data.put(
            "minCorner",
            new float[] { blockOverlay.getBlockX(), blockOverlay.getBlockY(), blockOverlay.getBlockZ() });
        data.put(
            "maxCorner",
            new float[] { blockOverlay.getBlockX() + 1f, blockOverlay.getBlockY() + 1f,
                blockOverlay.getBlockZ() + 1f });
        return data;
    }

    private static Map<String, Object> serializeDiamond(DiamondAnnotation diamond, GuideSiteTemplateRegistry templates,
        @Nullable ResourceLocation currentPageId, @Nullable GuideSitePageAssetExporter assetExporter,
        GuideSiteItemIconResolver itemIconResolver) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("type", "overlay");
        data.put("position", toVector(diamond.getPos()));
        data.put("color", toCssColor(diamond.getColor()));
        String templateId = createTemplateId(
            diamond.getTooltip(),
            templates,
            currentPageId,
            assetExporter,
            itemIconResolver);
        if (templateId != null) {
            data.put("contentTemplateId", templateId);
        }
        return data;
    }

    private static Map<String, Object> createBaseInWorldAnnotation(String type, ColorValue color, float thickness,
        boolean alwaysOnTop, @Nullable GuideTooltip tooltip, GuideSiteTemplateRegistry templates,
        @Nullable ResourceLocation currentPageId, @Nullable GuideSitePageAssetExporter assetExporter,
        GuideSiteItemIconResolver itemIconResolver) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("type", type);
        data.put("color", toCssColor(color));
        data.put("thickness", exportAnnotationThickness(thickness));
        data.put("alwaysOnTop", alwaysOnTop);
        String templateId = createTemplateId(tooltip, templates, currentPageId, assetExporter, itemIconResolver);
        if (templateId != null) {
            data.put("contentTemplateId", templateId);
        }
        return data;
    }

    private static float exportAnnotationThickness(float thickness) {
        return Math.max(thickness / ANNOTATION_THICKNESS_SCALE, MIN_EXPORTED_WORLD_THICKNESS);
    }

    @Nullable
    private static String createTemplateId(@Nullable GuideTooltip tooltip, GuideSiteTemplateRegistry templates,
        @Nullable ResourceLocation currentPageId, @Nullable GuideSitePageAssetExporter assetExporter,
        GuideSiteItemIconResolver itemIconResolver) {
        String html = TooltipHtmlRenderer
            .render(tooltip, currentPageId, assetExporter, itemIconResolver, templates, true);
        if (html == null || html.trim()
            .isEmpty()) {
            return null;
        }
        return templates.create(html);
    }

    public static String renderTooltipHtml(@Nullable GuideTooltip tooltip, @Nullable ResourceLocation currentPageId) {
        return renderTooltipHtml(tooltip, currentPageId, null, GuideSiteItemIconResolver.NONE);
    }

    public static String renderTooltipHtml(@Nullable GuideTooltip tooltip, @Nullable ResourceLocation currentPageId,
        @Nullable GuideSitePageAssetExporter assetExporter) {
        return renderTooltipHtml(tooltip, currentPageId, assetExporter, GuideSiteItemIconResolver.NONE);
    }

    public static String renderTooltipHtml(@Nullable GuideTooltip tooltip, @Nullable ResourceLocation currentPageId,
        @Nullable GuideSitePageAssetExporter assetExporter, GuideSiteItemIconResolver itemIconResolver) {
        return renderTooltipHtml(tooltip, currentPageId, assetExporter, itemIconResolver, null);
    }

    public static String renderTooltipHtml(@Nullable GuideTooltip tooltip, @Nullable ResourceLocation currentPageId,
        @Nullable GuideSitePageAssetExporter assetExporter, GuideSiteItemIconResolver itemIconResolver,
        @Nullable GuideSiteTemplateRegistry templates) {
        return TooltipHtmlRenderer
            .render(tooltip, currentPageId, assetExporter, itemIconResolver, templates, templates != null);
    }

    private static float[] toVector(Vector3f vector) {
        return new float[] { vector.x, vector.y, vector.z };
    }

    private static String toCssColor(@Nullable ColorValue color) {
        int argb = color != null ? color.resolve(LightDarkMode.LIGHT_MODE) : 0xFFFFFFFF;
        int alpha = argb >>> 24 & 0xFF;
        int red = argb >>> 16 & 0xFF;
        int green = argb >>> 8 & 0xFF;
        int blue = argb & 0xFF;
        return "rgba(" + red + "," + green + "," + blue + "," + alpha / 255.0f + ")";
    }

    public static final class AnnotationPayload {

        private final String inWorldJson;
        private final String overlayJson;

        public AnnotationPayload(String inWorldJson, String overlayJson) {
            this.inWorldJson = inWorldJson;
            this.overlayJson = overlayJson;
        }

        public String inWorldJson() {
            return inWorldJson;
        }

        public String overlayJson() {
            return overlayJson;
        }
    }

    private static final class TooltipHtmlRenderer {

        private TooltipHtmlRenderer() {}

        private static String render(@Nullable GuideTooltip tooltip, @Nullable ResourceLocation currentPageId,
            @Nullable GuideSitePageAssetExporter assetExporter, GuideSiteItemIconResolver itemIconResolver,
            @Nullable GuideSiteTemplateRegistry templates, boolean allowNestedItemTooltips) {
            if (tooltip == null) {
                return "";
            }
            if (tooltip instanceof TextTooltip textTooltip) {
                return renderPlainTextTooltip(textTooltip.getText());
            }
            if (tooltip instanceof ItemTooltip itemTooltip) {
                return renderItemTooltip(itemTooltip, itemIconResolver);
            }
            if (tooltip instanceof ContentTooltip contentTooltip) {
                return renderBlock(
                    contentTooltip.getContent(),
                    currentPageId,
                    assetExporter,
                    itemIconResolver,
                    templates,
                    allowNestedItemTooltips);
            }
            return "";
        }

        private static String renderPlainTextTooltip(@Nullable String text) {
            if (text == null || text.isEmpty()) {
                return "";
            }
            String normalized = text.indexOf('\\') >= 0 ? text.replace("\\n", "\n") : text;
            String[] lines = normalized.split("\\n", -1);
            StringBuilder html = new StringBuilder();
            for (String line : lines) {
                html.append("<p>")
                    .append(renderLegacyFormattedText(line))
                    .append("</p>");
            }
            return html.toString();
        }

        private static String renderItemTooltip(ItemTooltip tooltip, GuideSiteItemIconResolver itemIconResolver) {
            ItemStack stack = tooltip.getStack();
            GuideSiteExportedItem item = GuideSiteItemSupport.export(stack, itemIconResolver);

            List<String> lines = new ArrayList<>();
            try {
                Minecraft minecraft = Minecraft.getMinecraft();
                if (minecraft != null) {
                    lines.addAll(GuideItemTooltipLines.build(tooltip, minecraft));
                }
            } catch (Throwable ignored) {}

            if (lines.isEmpty() && stack != null) {
                try {
                    lines.add(stack.getDisplayName());
                } catch (Throwable ignored) {}
            }

            StringBuilder html = new StringBuilder();
            if (!item.isEmpty()) {
                html.append("<div class=\"guide-tooltip-item-row\">");
                GuideSiteItemHtml.appendIcon(html, item, null);
                html.append("</div>");
            }
            for (String line : lines) {
                html.append("<p>")
                    .append(renderLegacyFormattedText(line))
                    .append("</p>");
            }
            return html.toString();
        }

        private static String renderBlock(@Nullable LytBlock block, @Nullable ResourceLocation currentPageId,
            @Nullable GuideSitePageAssetExporter assetExporter, GuideSiteItemIconResolver itemIconResolver,
            @Nullable GuideSiteTemplateRegistry templates, boolean allowNestedItemTooltips) {
            if (block == null) {
                return "";
            }
            StringBuilder html = new StringBuilder();
            appendBlock(
                html,
                block,
                currentPageId,
                assetExporter,
                itemIconResolver,
                templates,
                allowNestedItemTooltips);
            return html.toString();
        }

        private static void appendBlock(StringBuilder html, @Nullable LytNode node,
            @Nullable ResourceLocation currentPageId, @Nullable GuideSitePageAssetExporter assetExporter,
            GuideSiteItemIconResolver itemIconResolver, @Nullable GuideSiteTemplateRegistry templates,
            boolean allowNestedItemTooltips) {
            if (node == null) {
                return;
            }
            if (node instanceof LytDocument document) {
                for (LytBlock child : document.getBlocks()) {
                    appendBlock(
                        html,
                        child,
                        currentPageId,
                        assetExporter,
                        itemIconResolver,
                        templates,
                        allowNestedItemTooltips);
                }
                return;
            }
            if (node instanceof LytHeading heading) {
                appendParagraph(
                    html,
                    heading,
                    "h" + clampHeadingDepth(heading.getDepth()),
                    currentPageId,
                    assetExporter,
                    itemIconResolver,
                    templates,
                    allowNestedItemTooltips);
                return;
            }
            if (node instanceof LytParagraph paragraph) {
                appendParagraph(
                    html,
                    paragraph,
                    "p",
                    currentPageId,
                    assetExporter,
                    itemIconResolver,
                    templates,
                    allowNestedItemTooltips);
                return;
            }
            if (node instanceof LytList list) {
                appendList(
                    html,
                    list,
                    currentPageId,
                    assetExporter,
                    itemIconResolver,
                    templates,
                    allowNestedItemTooltips);
                return;
            }
            if (node instanceof LytListItem listItem) {
                appendListItem(
                    html,
                    listItem,
                    currentPageId,
                    assetExporter,
                    itemIconResolver,
                    templates,
                    allowNestedItemTooltips);
                return;
            }
            if (node instanceof LytThematicBreak) {
                html.append("<hr>");
                return;
            }
            if (node instanceof LytTable table) {
                appendTable(
                    html,
                    table,
                    currentPageId,
                    assetExporter,
                    itemIconResolver,
                    templates,
                    allowNestedItemTooltips);
                return;
            }
            if (node instanceof LytSlotGrid slotGrid) {
                appendSlotGrid(
                    html,
                    slotGrid,
                    itemIconResolver,
                    currentPageId,
                    assetExporter,
                    templates,
                    allowNestedItemTooltips);
                return;
            }
            if (node instanceof LytItemGrid itemGrid) {
                appendItemGrid(
                    html,
                    itemGrid,
                    itemIconResolver,
                    currentPageId,
                    assetExporter,
                    templates,
                    allowNestedItemTooltips);
                return;
            }
            if (node instanceof LytItemImage itemImage) {
                appendItemStacks(
                    html,
                    itemImage.getStacks(),
                    itemIconResolver,
                    currentPageId,
                    assetExporter,
                    templates,
                    allowNestedItemTooltips);
                return;
            }
            if (node instanceof LytImage image) {
                appendImage(html, image, assetExporter);
                return;
            }
            if (node instanceof LytSlot slot) {
                appendSlot(
                    html,
                    slot,
                    itemIconResolver,
                    currentPageId,
                    assetExporter,
                    templates,
                    allowNestedItemTooltips);
                return;
            }

            List<? extends LytNode> children = node.getChildren();
            if (children != null) {
                for (LytNode child : children) {
                    appendBlock(
                        html,
                        child,
                        currentPageId,
                        assetExporter,
                        itemIconResolver,
                        templates,
                        allowNestedItemTooltips);
                }
            }
        }

        private static void appendParagraph(StringBuilder html, LytParagraph paragraph, String tagName,
            @Nullable ResourceLocation currentPageId, @Nullable GuideSitePageAssetExporter assetExporter,
            GuideSiteItemIconResolver itemIconResolver, @Nullable GuideSiteTemplateRegistry templates,
            boolean allowNestedItemTooltips) {
            html.append("<")
                .append(tagName);
            appendParagraphStyleAttribute(html, paragraph.resolveStyle());
            if (paragraph instanceof LytHeading heading) {
                String anchor = GuideSiteHrefResolver.headingAnchor(heading.getTextContent());
                if (!anchor.isEmpty()) {
                    html.append(" id=\"")
                        .append(escapeAttribute(anchor))
                        .append("\"");
                }
            }
            html.append(">");
            for (LytFlowContent content : paragraph.getContent()) {
                appendFlowContent(
                    html,
                    content,
                    currentPageId,
                    assetExporter,
                    itemIconResolver,
                    templates,
                    allowNestedItemTooltips);
            }
            html.append("</")
                .append(tagName)
                .append(">");
        }

        private static void appendList(StringBuilder html, LytList list, @Nullable ResourceLocation currentPageId,
            @Nullable GuideSitePageAssetExporter assetExporter, GuideSiteItemIconResolver itemIconResolver,
            @Nullable GuideSiteTemplateRegistry templates, boolean allowNestedItemTooltips) {
            String tagName = list.isOrdered() ? "ol" : "ul";
            html.append("<")
                .append(tagName);
            if (list.isOrdered() && list.getStart() > 1) {
                html.append(" start=\"")
                    .append(list.getStart())
                    .append("\"");
            }
            html.append(">");
            for (LytNode child : list.getChildren()) {
                appendBlock(
                    html,
                    child,
                    currentPageId,
                    assetExporter,
                    itemIconResolver,
                    templates,
                    allowNestedItemTooltips);
            }
            html.append("</")
                .append(tagName)
                .append(">");
        }

        private static void appendListItem(StringBuilder html, LytListItem listItem,
            @Nullable ResourceLocation currentPageId, @Nullable GuideSitePageAssetExporter assetExporter,
            GuideSiteItemIconResolver itemIconResolver, @Nullable GuideSiteTemplateRegistry templates,
            boolean allowNestedItemTooltips) {
            html.append("<li>");
            for (LytNode child : listItem.getChildren()) {
                appendBlock(
                    html,
                    child,
                    currentPageId,
                    assetExporter,
                    itemIconResolver,
                    templates,
                    allowNestedItemTooltips);
            }
            html.append("</li>");
        }

        private static void appendTable(StringBuilder html, LytTable table, @Nullable ResourceLocation currentPageId,
            @Nullable GuideSitePageAssetExporter assetExporter, GuideSiteItemIconResolver itemIconResolver,
            @Nullable GuideSiteTemplateRegistry templates, boolean allowNestedItemTooltips) {
            html.append("<table>");
            List<LytTableRow> rows = table.getChildren();
            if (!rows.isEmpty()) {
                html.append("<thead>");
                appendTableRow(
                    html,
                    rows.get(0),
                    "th",
                    currentPageId,
                    assetExporter,
                    itemIconResolver,
                    templates,
                    allowNestedItemTooltips);
                html.append("</thead>");
            }
            if (rows.size() > 1) {
                html.append("<tbody>");
                for (int i = 1; i < rows.size(); i++) {
                    appendTableRow(
                        html,
                        rows.get(i),
                        "td",
                        currentPageId,
                        assetExporter,
                        itemIconResolver,
                        templates,
                        allowNestedItemTooltips);
                }
                html.append("</tbody>");
            }
            html.append("</table>");
        }

        private static void appendTableRow(StringBuilder html, LytTableRow row, String cellTagName,
            @Nullable ResourceLocation currentPageId, @Nullable GuideSitePageAssetExporter assetExporter,
            GuideSiteItemIconResolver itemIconResolver, @Nullable GuideSiteTemplateRegistry templates,
            boolean allowNestedItemTooltips) {
            html.append("<tr>");
            for (LytTableCell cell : row.getChildren()) {
                html.append("<")
                    .append(cellTagName)
                    .append(">");
                for (LytNode child : cell.getChildren()) {
                    appendBlock(
                        html,
                        child,
                        currentPageId,
                        assetExporter,
                        itemIconResolver,
                        templates,
                        allowNestedItemTooltips);
                }
                html.append("</")
                    .append(cellTagName)
                    .append(">");
            }
            html.append("</tr>");
        }

        private static void appendParagraphStyleAttribute(StringBuilder html, ResolvedTextStyle style) {
            StringBuilder css = new StringBuilder();
            if (style.alignment() == TextAlignment.CENTER) {
                css.append("text-align:center;");
            } else if (style.alignment() == TextAlignment.RIGHT) {
                css.append("text-align:right;");
            }
            if (css.length() > 0) {
                html.append(" style=\"")
                    .append(escapeAttribute(css.toString()))
                    .append("\"");
            }
        }

        private static void appendFlowContent(StringBuilder html, @Nullable LytFlowContent content,
            @Nullable ResourceLocation currentPageId, @Nullable GuideSitePageAssetExporter assetExporter,
            GuideSiteItemIconResolver itemIconResolver, @Nullable GuideSiteTemplateRegistry templates,
            boolean allowNestedItemTooltips) {
            if (content == null) {
                return;
            }
            if (content instanceof LytFlowText text) {
                html.append(escapeHtml(text.getText()));
                return;
            }
            if (content instanceof LytFlowBreak) {
                html.append("<br>");
                return;
            }
            if (content instanceof LytFlowAnchor flowAnchor) {
                html.append("<span id=\"")
                    .append(escapeAttribute(flowAnchor.getName()))
                    .append("\"></span>");
                return;
            }
            if (content instanceof LytFlowInlineBlock inlineBlock) {
                appendInlineBlock(
                    html,
                    inlineBlock,
                    currentPageId,
                    assetExporter,
                    itemIconResolver,
                    templates,
                    allowNestedItemTooltips);
                return;
            }
            if (content instanceof LytFlowLink link) {
                appendLink(html, link, currentPageId, itemIconResolver, templates, allowNestedItemTooltips);
                return;
            }
            if (content instanceof LytFlowSpan span) {
                appendStyledFlowContainer(
                    html,
                    span,
                    "span",
                    currentPageId,
                    assetExporter,
                    itemIconResolver,
                    templates,
                    allowNestedItemTooltips,
                    null);
            }
        }

        private static void appendInlineBlock(StringBuilder html, LytFlowInlineBlock inlineBlock,
            @Nullable ResourceLocation currentPageId, @Nullable GuideSitePageAssetExporter assetExporter,
            GuideSiteItemIconResolver itemIconResolver, @Nullable GuideSiteTemplateRegistry templates,
            boolean allowNestedItemTooltips) {
            LytBlock block = inlineBlock.getBlock();
            if (block instanceof LytItemImage itemImage) {
                appendItemStacks(
                    html,
                    itemImage.getStacks(),
                    itemIconResolver,
                    currentPageId,
                    assetExporter,
                    templates,
                    allowNestedItemTooltips);
                return;
            }
            if (block instanceof LytImage image) {
                appendImage(html, image, assetExporter);
                return;
            }
            appendBlock(
                html,
                block,
                currentPageId,
                assetExporter,
                itemIconResolver,
                templates,
                allowNestedItemTooltips);
        }

        private static void appendLink(StringBuilder html, LytFlowLink link, @Nullable ResourceLocation currentPageId,
            GuideSiteItemIconResolver itemIconResolver, @Nullable GuideSiteTemplateRegistry templates,
            boolean allowNestedItemTooltips) {
            String href = null;
            PageAnchor pageAnchor = link.getPageAnchor();
            if (pageAnchor != null) {
                href = GuideSiteHrefResolver.resolvePageAnchor(currentPageId, pageAnchor);
            } else if (link.getExternalUrl() != null) {
                href = link.getExternalUrl()
                    .toString();
            }

            appendStyledFlowContainer(
                html,
                link,
                href != null && !href.isEmpty() ? "a" : "span",
                currentPageId,
                null,
                itemIconResolver,
                templates,
                allowNestedItemTooltips,
                href);
        }

        private static void appendStyledFlowContainer(StringBuilder html, LytFlowSpan span, String tagName,
            @Nullable ResourceLocation currentPageId, @Nullable GuideSitePageAssetExporter assetExporter,
            GuideSiteItemIconResolver itemIconResolver, @Nullable GuideSiteTemplateRegistry templates,
            boolean allowNestedItemTooltips, @Nullable String href) {
            html.append("<")
                .append(tagName);
            if ("a".equals(tagName) && href != null && !href.isEmpty()) {
                html.append(" href=\"")
                    .append(escapeAttribute(href))
                    .append("\"");
            }
            appendInlineStyleAttribute(html, span.resolveStyle());
            html.append(">");
            for (LytFlowContent child : span.getChildren()) {
                appendFlowContent(
                    html,
                    child,
                    currentPageId,
                    assetExporter,
                    itemIconResolver,
                    templates,
                    allowNestedItemTooltips);
            }
            html.append("</")
                .append(tagName)
                .append(">");
        }

        private static void appendImage(StringBuilder html, LytImage image,
            @Nullable GuideSitePageAssetExporter assetExporter) {
            String src = "";
            if (image.getImageId() != null && assetExporter != null) {
                src = assetExporter.exportResource(image.getImageId());
            }
            if (src.isEmpty() && image.getImageId() != null) {
                src = image.getImageId()
                    .toString();
            }
            if (src.isEmpty()) {
                return;
            }

            html.append("<img class=\"guide-image\" src=\"")
                .append(escapeAttribute(src))
                .append("\" alt=\"")
                .append(escapeAttribute(image.getAlt() != null ? image.getAlt() : ""))
                .append("\"");
            if (image.getTitle() != null && !image.getTitle()
                .isEmpty()) {
                html.append(" title=\"")
                    .append(escapeAttribute(image.getTitle()))
                    .append("\"");
            }
            html.append(" loading=\"lazy\" decoding=\"async\">");
        }

        private static void appendInlineStyleAttribute(StringBuilder html, ResolvedTextStyle style) {
            StringBuilder css = new StringBuilder();
            if (style.bold()) {
                css.append("font-weight:700;");
            }
            if (style.italic()) {
                css.append("font-style:italic;");
            }
            if (style.underlined() || style.strikethrough()) {
                css.append("text-decoration:");
                if (style.underlined()) {
                    css.append(" underline");
                }
                if (style.strikethrough()) {
                    css.append(" line-through");
                }
                css.append(";");
            }
            if (style.color() != null) {
                css.append("color:")
                    .append(toCssColor(style.color()))
                    .append(";");
            }
            if (style.fontScale() != 1.0f) {
                css.append("font-size:")
                    .append(style.fontScale())
                    .append("em;");
            }
            if (css.length() > 0) {
                html.append(" style=\"")
                    .append(escapeAttribute(css.toString()))
                    .append("\"");
            }
        }

        private static void appendSlotGrid(StringBuilder html, LytSlotGrid grid,
            GuideSiteItemIconResolver itemIconResolver, @Nullable ResourceLocation currentPageId,
            @Nullable GuideSitePageAssetExporter assetExporter, @Nullable GuideSiteTemplateRegistry templates,
            boolean allowNestedItemTooltips) {
            html.append("<div class=\"guide-tooltip-item-grid\">");
            for (int row = 0; row < grid.getHeight(); row++) {
                for (int col = 0; col < grid.getWidth(); col++) {
                    LytSlot slot = grid.getSlot(col, row);
                    if (slot == null) {
                        continue;
                    }
                    html.append("<div class=\"ingredient-box\">");
                    appendSlot(
                        html,
                        slot,
                        itemIconResolver,
                        currentPageId,
                        assetExporter,
                        templates,
                        allowNestedItemTooltips);
                    html.append("</div>");
                }
            }
            html.append("</div>");
        }

        private static void appendItemGrid(StringBuilder html, LytItemGrid grid,
            GuideSiteItemIconResolver itemIconResolver, @Nullable ResourceLocation currentPageId,
            @Nullable GuideSitePageAssetExporter assetExporter, @Nullable GuideSiteTemplateRegistry templates,
            boolean allowNestedItemTooltips) {
            html.append("<div class=\"guide-tooltip-item-grid\">");
            for (LytNode child : grid.getChildren()) {
                if (!(child instanceof LytSlot slot)) {
                    continue;
                }
                html.append("<div class=\"ingredient-box\">");
                appendSlot(
                    html,
                    slot,
                    itemIconResolver,
                    currentPageId,
                    assetExporter,
                    templates,
                    allowNestedItemTooltips);
                html.append("</div>");
            }
            html.append("</div>");
        }

        private static void appendSlot(StringBuilder html, LytSlot slot, GuideSiteItemIconResolver itemIconResolver,
            @Nullable ResourceLocation currentPageId, @Nullable GuideSitePageAssetExporter assetExporter,
            @Nullable GuideSiteTemplateRegistry templates, boolean allowNestedItemTooltips) {
            Optional<GuideTooltip> tooltip = slot.getTooltip(0, 0);
            if (tooltip.isPresent() && tooltip.get() instanceof ItemTooltip itemTooltip) {
                ItemStack stack = itemTooltip.getStack();
                if (stack != null) {
                    List<ItemStack> stacks = new ArrayList<>(1);
                    stacks.add(stack);
                    appendItemStacks(
                        html,
                        stacks,
                        itemIconResolver,
                        currentPageId,
                        assetExporter,
                        templates,
                        allowNestedItemTooltips);
                }
            }
        }

        private static void appendItemStacks(StringBuilder html, List<ItemStack> stacks,
            GuideSiteItemIconResolver itemIconResolver, @Nullable ResourceLocation currentPageId,
            @Nullable GuideSitePageAssetExporter assetExporter, @Nullable GuideSiteTemplateRegistry templates,
            boolean allowNestedItemTooltips) {
            if (stacks == null || stacks.isEmpty()) {
                return;
            }
            for (ItemStack stack : stacks) {
                GuideSiteExportedItem item = GuideSiteItemSupport.export(stack, itemIconResolver);
                if (item.isEmpty()) {
                    continue;
                }
                appendTooltipCapableItemIcon(
                    html,
                    stack,
                    item,
                    currentPageId,
                    assetExporter,
                    itemIconResolver,
                    templates,
                    allowNestedItemTooltips);
            }
        }

        private static void appendTooltipCapableItemIcon(StringBuilder html, ItemStack stack,
            GuideSiteExportedItem item, @Nullable ResourceLocation currentPageId,
            @Nullable GuideSitePageAssetExporter assetExporter, GuideSiteItemIconResolver itemIconResolver,
            @Nullable GuideSiteTemplateRegistry templates, boolean allowNestedItemTooltips) {
            if (!allowNestedItemTooltips || templates == null || stack == null || stack.stackSize <= 0) {
                GuideSiteItemHtml.appendIcon(html, item, null);
                return;
            }

            String templateId = createNestedItemTemplateId(
                stack,
                currentPageId,
                assetExporter,
                itemIconResolver,
                templates);
            if (templateId == null || templateId.isEmpty()) {
                GuideSiteItemHtml.appendIcon(html, item, null);
                return;
            }

            html.append("<span class=\"guide-tooltip\" data-template=\"")
                .append(escapeAttribute(templateId))
                .append("\">");
            GuideSiteItemHtml.appendIcon(html, item, null);
            html.append("</span>");
        }

        @Nullable
        private static String createNestedItemTemplateId(ItemStack stack, @Nullable ResourceLocation currentPageId,
            @Nullable GuideSitePageAssetExporter assetExporter, GuideSiteItemIconResolver itemIconResolver,
            GuideSiteTemplateRegistry templates) {
            String html = render(
                new ItemTooltip(stack.copy()),
                currentPageId,
                assetExporter,
                itemIconResolver,
                templates,
                false);
            if (html == null || html.trim()
                .isEmpty()) {
                return null;
            }
            return templates.create(html);
        }

        private static int clampHeadingDepth(int depth) {
            if (depth < 1) {
                return 1;
            }
            return Math.min(depth, 6);
        }

        private static String renderLegacyFormattedText(@Nullable String text) {
            if (text == null || text.isEmpty()) {
                return "";
            }

            LegacyStyle style = new LegacyStyle();
            StringBuilder html = new StringBuilder();
            StringBuilder segment = new StringBuilder();

            for (int i = 0; i < text.length(); i++) {
                char ch = text.charAt(i);
                if (ch == '\u00A7' && i + 1 < text.length()) {
                    appendLegacySegment(html, segment, style);
                    style.apply(text.charAt(++i));
                    continue;
                }
                segment.append(ch);
            }
            appendLegacySegment(html, segment, style);
            return html.toString();
        }

        private static void appendLegacySegment(StringBuilder html, StringBuilder segment, LegacyStyle style) {
            if (segment.length() == 0) {
                return;
            }
            String text = escapeHtml(segment.toString());
            segment.setLength(0);

            StringBuilder css = new StringBuilder();
            if (style.color != null) {
                css.append("color:")
                    .append(style.color)
                    .append(";");
            }
            if (style.bold) {
                css.append("font-weight:700;");
            }
            if (style.italic) {
                css.append("font-style:italic;");
            }
            if (style.underlined || style.strikethrough) {
                css.append("text-decoration:");
                if (style.underlined) {
                    css.append(" underline");
                }
                if (style.strikethrough) {
                    css.append(" line-through");
                }
                css.append(";");
            }
            if (style.obfuscated) {
                css.append("filter:blur(0.6px);");
            }

            if (css.length() == 0) {
                html.append(text);
                return;
            }

            html.append("<span style=\"")
                .append(escapeAttribute(css.toString()))
                .append("\">")
                .append(text)
                .append("</span>");
        }

        private static String escapeHtml(String text) {
            return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
        }

        private static String escapeAttribute(String text) {
            return escapeHtml(text);
        }

        private static final class LegacyStyle {

            @Nullable
            private String color;
            private boolean bold;
            private boolean italic;
            private boolean underlined;
            private boolean strikethrough;
            private boolean obfuscated;

            private void apply(char rawCode) {
                char code = Character.toLowerCase(rawCode);
                String nextColor = switch (code) {
                    case '0' -> "#000000";
                    case '1' -> "#0000AA";
                    case '2' -> "#00AA00";
                    case '3' -> "#00AAAA";
                    case '4' -> "#AA0000";
                    case '5' -> "#AA00AA";
                    case '6' -> "#FFAA00";
                    case '7' -> "#AAAAAA";
                    case '8' -> "#555555";
                    case '9' -> "#5555FF";
                    case 'a' -> "#55FF55";
                    case 'b' -> "#55FFFF";
                    case 'c' -> "#FF5555";
                    case 'd' -> "#FF55FF";
                    case 'e' -> "#FFFF55";
                    case 'f' -> "#FFFFFF";
                    default -> null;
                };

                if (nextColor != null) {
                    reset();
                    color = nextColor;
                    return;
                }

                switch (code) {
                    case 'k':
                        obfuscated = true;
                        break;
                    case 'l':
                        bold = true;
                        break;
                    case 'm':
                        strikethrough = true;
                        break;
                    case 'n':
                        underlined = true;
                        break;
                    case 'o':
                        italic = true;
                        break;
                    case 'r':
                        reset();
                        break;
                    default:
                        break;
                }
            }

            private void reset() {
                color = null;
                bold = false;
                italic = false;
                underlined = false;
                strikethrough = false;
                obfuscated = false;
            }
        }
    }
}
