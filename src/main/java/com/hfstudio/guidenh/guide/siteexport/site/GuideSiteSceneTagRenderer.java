package com.hfstudio.guidenh.guide.siteexport.site;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;

import org.jetbrains.annotations.Nullable;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.hfstudio.guidenh.guide.compiler.tags.MdxAttrs;
import com.hfstudio.guidenh.guide.internal.localization.GuideResourceLanguageIndex;
import com.hfstudio.guidenh.guide.internal.util.LangUtil;
import com.hfstudio.guidenh.guide.scene.StructureLibSceneCondition;
import com.hfstudio.guidenh.guide.scene.annotation.InWorldBoxAnnotation;
import com.hfstudio.guidenh.guide.scene.annotation.InWorldLineAnnotation;
import com.hfstudio.guidenh.guide.scene.annotation.PonderInputAnnotation;
import com.hfstudio.guidenh.guide.scene.annotation.TextAnnotation;
import com.hfstudio.guidenh.guide.sound.GuideSoundSpec;
import com.hfstudio.guidenh.guide.sound.GuideSoundTrigger;
import com.hfstudio.guidenh.libs.mdast.mdx.model.MdxJsxAttribute;
import com.hfstudio.guidenh.libs.mdast.mdx.model.MdxJsxElementFields;
import com.hfstudio.guidenh.libs.mdast.mdx.model.MdxJsxFlowElement;
import com.hfstudio.guidenh.libs.mdast.model.MdAstAnyContent;
import com.hfstudio.guidenh.libs.mdast.model.MdAstNode;

public class GuideSiteSceneTagRenderer implements GuideSiteHtmlCompiler.SceneTagRenderer {

    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping()
        .serializeNulls()
        .create();
    public static final int DEFAULT_WEB_SCENE_SCALE = 4;
    public static final int TOOLTIP_WEB_SCENE_SCALE = 3;
    private static final String TRANSPARENT_PIXEL = "data:image/gif;base64,R0lGODlhAQABAIAAAAAAAP///ywAAAAAAQABAAACAUwAOw==";
    private static final String[] FORWARDED_ATTRIBUTES = { "zoom", "perspective", "rotateX", "rotateY", "rotateZ",
        "offsetX", "offsetY", "centerX", "centerY", "centerZ", "allowLayerSlider" };

    private final GuideSiteHtmlCompiler fragmentCompiler;
    @Nullable
    private final GuideSitePageAssetExporter assetExporter;
    private final GuideSiteItemIconResolver itemIconResolver;

    public GuideSiteSceneTagRenderer() {
        this(
            new GuideSiteRecipeTagRenderer(),
            (rawUrl, currentPageId) -> rawUrl != null ? rawUrl : "",
            noopMdxTagRenderer());
    }

    public GuideSiteSceneTagRenderer(GuideSiteHtmlCompiler.ImageResolver imageResolver) {
        this(new GuideSiteRecipeTagRenderer(), imageResolver, noopMdxTagRenderer());
    }

    public GuideSiteSceneTagRenderer(GuideSiteHtmlCompiler.RecipeTagRenderer recipeTagRenderer,
        GuideSiteHtmlCompiler.ImageResolver imageResolver, GuideSiteHtmlCompiler.MdxTagRenderer mdxTagRenderer) {
        this(recipeTagRenderer, imageResolver, mdxTagRenderer, null);
    }

    public GuideSiteSceneTagRenderer(GuideSiteHtmlCompiler.RecipeTagRenderer recipeTagRenderer,
        GuideSiteHtmlCompiler.ImageResolver imageResolver, GuideSiteHtmlCompiler.MdxTagRenderer mdxTagRenderer,
        GuideSiteLatexExporter latexExporter) {
        this(recipeTagRenderer, imageResolver, mdxTagRenderer, latexExporter, null, GuideSiteItemIconResolver.NONE);
    }

    public GuideSiteSceneTagRenderer(GuideSiteHtmlCompiler.RecipeTagRenderer recipeTagRenderer,
        GuideSiteHtmlCompiler.ImageResolver imageResolver, GuideSiteHtmlCompiler.MdxTagRenderer mdxTagRenderer,
        GuideSiteLatexExporter latexExporter, @Nullable GuideSitePageAssetExporter assetExporter) {
        this(
            recipeTagRenderer,
            imageResolver,
            mdxTagRenderer,
            latexExporter,
            assetExporter,
            GuideSiteItemIconResolver.NONE);
    }

    public GuideSiteSceneTagRenderer(GuideSiteHtmlCompiler.RecipeTagRenderer recipeTagRenderer,
        GuideSiteHtmlCompiler.ImageResolver imageResolver, GuideSiteHtmlCompiler.MdxTagRenderer mdxTagRenderer,
        GuideSiteLatexExporter latexExporter, @Nullable GuideSitePageAssetExporter assetExporter,
        GuideSiteItemIconResolver itemIconResolver) {
        this.fragmentCompiler = new GuideSiteHtmlCompiler(
            recipeTagRenderer,
            (element, defaultNamespace, currentPageId, templates, exportedScene) -> "",
            imageResolver,
            mdxTagRenderer != null ? mdxTagRenderer : noopMdxTagRenderer(),
            latexExporter,
            assetExporter);
        this.assetExporter = assetExporter;
        this.itemIconResolver = itemIconResolver != null ? itemIconResolver : GuideSiteItemIconResolver.NONE;
    }

    @Override
    public String render(MdxJsxElementFields element, String defaultNamespace, ResourceLocation currentPageId,
        GuideSiteTemplateRegistry templates, GuideSiteExportedScene exportedScene) {
        int width = readDimension(element, "width", 256);
        int height = readDimension(element, "height", 192);
        boolean interactive = readBooleanValue(element, "interactive", true);
        String background = readBooleanValue(element, "showBackground", true) ? null : "transparent";
        AnnotationPayload payload = resolveAnnotationPayload(
            element,
            defaultNamespace,
            currentPageId,
            templates,
            exportedScene);

        StringBuilder html = new StringBuilder();
        boolean hasBlockStats = exportedScene != null && exportedScene.blockStatsHtml() != null
            && !exportedScene.blockStatsHtml()
                .isEmpty();
        if (hasBlockStats) {
            html.append("<div class=\"guide-scene-export-frame");
            if (exportedScene.blockStatsLayoutClass() != null && !exportedScene.blockStatsLayoutClass()
                .isEmpty()) {
                html.append(" ")
                    .append(escapeAttribute(exportedScene.blockStatsLayoutClass()));
            }
            html.append("\"");
            if (exportedScene.blockStatsLayoutStyle() != null && !exportedScene.blockStatsLayoutStyle()
                .isEmpty()) {
                html.append(" style=\"")
                    .append(escapeAttribute(exportedScene.blockStatsLayoutStyle()))
                    .append("\"");
            }
            html.append(">");
        }
        html.append(
            renderSceneHtml(
                width,
                height,
                interactive,
                defaultNamespace,
                background,
                exportedScene,
                payload.inWorldJson(),
                payload.overlayJson()));

        for (String attributeName : FORWARDED_ATTRIBUTES) {
            String attributeValue = readOptional(element, attributeName);
            if (attributeValue != null && !attributeValue.isEmpty()) {
                html.append(" data-scene-")
                    .append(attributeName.toLowerCase())
                    .append("=\"")
                    .append(escapeAttribute(attributeValue))
                    .append("\"");
            }
        }
        appendSceneActionAttributes(html, exportedScene);
        appendSceneSoundAttributes(html, element, defaultNamespace, currentPageId, exportedScene);

        html.append(">");
        if (hasBlockStats) {
            html.append(exportedScene.blockStatsHtml())
                .append("</div>");
        }
        return html.toString();
    }

    private void appendSceneSoundAttributes(StringBuilder html, MdxJsxElementFields element, String defaultNamespace,
        ResourceLocation currentPageId, @Nullable GuideSiteExportedScene exportedScene) {
        String soundsJson = exportedScene != null ? exportedScene.sceneSoundsJson() : null;
        if ((soundsJson == null || soundsJson.isEmpty()) && element != null) {
            List<Map<String, Object>> sounds = collectSounds(element, defaultNamespace, currentPageId);
            if (sounds.isEmpty()) {
                return;
            }
            soundsJson = GSON.toJson(sounds);
        }
        if (soundsJson == null || soundsJson.isEmpty() || "[]".equals(soundsJson)) {
            return;
        }
        html.append(" data-guide-scene-sounds=\"")
            .append(escapeAttribute(soundsJson))
            .append("\"");
    }

    private List<Map<String, Object>> collectSounds(MdxJsxElementFields element, String defaultNamespace,
        ResourceLocation currentPageId) {
        List<Map<String, Object>> sounds = new ArrayList<>();
        for (MdAstAnyContent child : element.children()) {
            if (!(child instanceof MdxJsxFlowElement flowElement) || !"PlaySound".equals(flowElement.name())) {
                continue;
            }
            GuideSoundSpec sound = GuideSiteSoundExport
                .parse(name -> readOptional(flowElement, name), defaultNamespace, currentPageId);
            if (sound == null) {
                continue;
            }
            Map<String, Object> data = new LinkedHashMap<>();
            data.put(
                "sound",
                sound.soundId()
                    .toString());
            data.put(
                "src",
                GuideSiteSoundExport
                    .exportSource(sound, name -> readOptional(flowElement, name), currentPageId, assetExporter));
            data.put(
                "trigger",
                GuideSoundTrigger.parse(readOptional(flowElement, "trigger"), GuideSoundTrigger.CLICK)
                    .name()
                    .toLowerCase());
            data.put("volume", sound.volume());
            data.put("pitch", sound.pitch());
            data.put("cooldown", sound.cooldownMillis());
            data.put("radius", sound.radius());
            data.put("minVolume", sound.minVolume());
            if (sound.hasPosition()) {
                data.put("x", sound.x());
                data.put("y", sound.y());
                data.put("z", sound.z());
            }
            StructureLibSceneCondition condition = StructureLibSceneCondition.parse(
                readOptional(flowElement, "showWhenStructure"),
                readOptional(flowElement, "showWhenTier"),
                readOptional(flowElement, "showWhenChannels"));
            if (condition != null && condition.hasAnyConstraint()) {
                data.put("structureLibCondition", condition.toSiteExportData());
            }
            sounds.add(data);
        }
        return sounds;
    }

    private void appendSceneActionAttributes(StringBuilder html, GuideSiteExportedScene exportedScene) {
        if (exportedScene == null) {
            return;
        }
        if (exportedScene.gridButtonEnabled()) {
            html.append(" data-scene-grid-toggle=\"true\" data-scene-grid-visible=\"")
                .append(exportedScene.gridVisible())
                .append("\"");
            if (exportedScene.gridAnnotationJson() != null && !exportedScene.gridAnnotationJson()
                .isEmpty()) {
                html.append(" data-scene-grid-annotations=\"")
                    .append(escapeAttribute(exportedScene.gridAnnotationJson()))
                    .append("\"");
            }
        }
        if (exportedScene.blockStatsButtonEnabled()) {
            html.append(" data-scene-block-stats-toggle=\"true\" data-scene-block-stats-visible=\"")
                .append(exportedScene.blockStatsVisible())
                .append("\"");
        }
    }

    public static String renderSceneHtml(int logicalWidth, int logicalHeight, boolean interactive,
        String defaultNamespace, String background, GuideSiteExportedScene exportedScene) {
        return renderSceneHtml(
            logicalWidth,
            logicalHeight,
            interactive,
            defaultNamespace,
            background,
            exportedScene,
            DEFAULT_WEB_SCENE_SCALE);
    }

    public static String renderSceneHtml(int logicalWidth, int logicalHeight, boolean interactive,
        String defaultNamespace, String background, GuideSiteExportedScene exportedScene, int displayScale) {
        String inWorldJson = exportedScene != null && exportedScene.inWorldJson() != null ? exportedScene.inWorldJson()
            : "[]";
        String overlayJson = exportedScene != null && exportedScene.overlayJson() != null ? exportedScene.overlayJson()
            : "[]";
        return renderSceneHtml(
            logicalWidth,
            logicalHeight,
            interactive,
            defaultNamespace,
            background,
            exportedScene,
            inWorldJson,
            overlayJson,
            displayScale);
    }

    public static String renderSceneHtml(int logicalWidth, int logicalHeight, boolean interactive,
        String defaultNamespace, String background, GuideSiteExportedScene exportedScene, String inWorldJson,
        String overlayJson) {
        return renderSceneHtml(
            logicalWidth,
            logicalHeight,
            interactive,
            defaultNamespace,
            background,
            exportedScene,
            inWorldJson,
            overlayJson,
            DEFAULT_WEB_SCENE_SCALE);
    }

    public static String renderSceneHtml(int logicalWidth, int logicalHeight, boolean interactive,
        String defaultNamespace, String background, GuideSiteExportedScene exportedScene, String inWorldJson,
        String overlayJson, int displayScale) {
        int normalizedWidth = Math.max(16, logicalWidth);
        int normalizedHeight = Math.max(16, logicalHeight);
        int normalizedDisplayScale = Math.max(1, displayScale);
        int displayWidth = normalizedWidth * normalizedDisplayScale;
        int displayHeight = normalizedHeight * normalizedDisplayScale;

        String src = exportedScene != null ? GuideSitePageAssetExporter.ROOT_PREFIX + exportedScene.placeholderPath()
            : TRANSPARENT_PIXEL;
        String sceneSrc = exportedScene != null ? GuideSitePageAssetExporter.ROOT_PREFIX + exportedScene.scenePath()
            : null;
        String cssClass = sceneSrc != null ? "game-scene guide-scene" : "guide-scene";

        StringBuilder html = new StringBuilder();
        html.append("<img class=\"")
            .append(cssClass)
            .append("\" src=\"")
            .append(escapeAttributeStatic(src))
            .append("\" alt=\"3D scene preview\" loading=\"lazy\" decoding=\"async\" width=\"")
            .append(displayWidth)
            .append("\" height=\"")
            .append(displayHeight)
            .append("\" data-scene-width=\"")
            .append(normalizedWidth)
            .append("\" data-scene-height=\"")
            .append(normalizedHeight)
            .append("\" data-scene-interactive=\"")
            .append(interactive)
            .append("\" data-scene-default-namespace=\"")
            .append(escapeAttributeStatic(defaultNamespace != null ? defaultNamespace : "guidenh"))
            .append("\" data-scene-display-scale=\"")
            .append(normalizedDisplayScale)
            .append("\"");

        if (sceneSrc != null) {
            html.append(" data-scene-src=\"")
                .append(escapeAttributeStatic(sceneSrc))
                .append("\" data-scene-asset-prefix=\"")
                .append(escapeAttributeStatic(GuideSitePageAssetExporter.ROOT_PREFIX))
                .append("\"");
        }
        if (background != null && !background.isEmpty()) {
            html.append(" data-scene-background=\"")
                .append(escapeAttributeStatic(background))
                .append("\"");
        }
        if (inWorldJson != null && !inWorldJson.isEmpty()) {
            html.append(" data-scene-in-world-annotations=\"")
                .append(escapeAttributeStatic(inWorldJson))
                .append("\"");
        }
        if (overlayJson != null && !overlayJson.isEmpty()) {
            html.append(" data-scene-overlay-annotations=\"")
                .append(escapeAttributeStatic(overlayJson))
                .append("\"");
        }
        if (exportedScene != null && exportedScene.hoverTargetsJson() != null
            && !exportedScene.hoverTargetsJson()
                .isEmpty()) {
            html.append(" data-scene-hover-targets=\"")
                .append(escapeAttributeStatic(exportedScene.hoverTargetsJson()))
                .append("\"");
        }
        if (exportedScene != null && exportedScene.stateManifestPath() != null
            && !exportedScene.stateManifestPath()
                .isEmpty()) {
            html.append(" data-scene-state-manifest-src=\"")
                .append(
                    escapeAttributeStatic(GuideSitePageAssetExporter.ROOT_PREFIX + exportedScene.stateManifestPath()))
                .append("\" data-scene-state-controls=\"true\"");
        }
        return html.toString();
    }

    public static String renderStaticScenePlaceholder(int logicalWidth, int logicalHeight,
        GuideSiteExportedScene exportedScene) {
        int normalizedWidth = Math.max(16, logicalWidth);
        int normalizedHeight = Math.max(16, logicalHeight);
        String src = exportedScene != null && exportedScene.placeholderPath() != null
            && !exportedScene.placeholderPath()
                .isEmpty() ? GuideSitePageAssetExporter.ROOT_PREFIX + exportedScene.placeholderPath()
                    : TRANSPARENT_PIXEL;

        StringBuilder html = new StringBuilder();
        html.append("<div class=\"guide-tooltip-scene-placeholder\" style=\"width:")
            .append(normalizedWidth)
            .append("px;height:")
            .append(normalizedHeight)
            .append("px;\">")
            .append("<img src=\"")
            .append(escapeAttributeStatic(src))
            .append("\" alt=\"3D scene preview\" loading=\"lazy\" decoding=\"async\" width=\"")
            .append(normalizedWidth)
            .append("\" height=\"")
            .append(normalizedHeight)
            .append("\"></div>");
        return html.toString();
    }

    private AnnotationPayload resolveAnnotationPayload(MdxJsxElementFields element, String defaultNamespace,
        ResourceLocation currentPageId, GuideSiteTemplateRegistry templates, GuideSiteExportedScene exportedScene) {
        if (exportedScene != null && (exportedScene.inWorldJson() != null || exportedScene.overlayJson() != null)) {
            return new AnnotationPayload(
                exportedScene.inWorldJson() != null ? exportedScene.inWorldJson() : "[]",
                exportedScene.overlayJson() != null ? exportedScene.overlayJson() : "[]");
        }
        return collectAnnotations(element, defaultNamespace, currentPageId, templates);
    }

    private AnnotationPayload collectAnnotations(MdxJsxElementFields element, String defaultNamespace,
        ResourceLocation currentPageId, GuideSiteTemplateRegistry templates) {
        List<Map<String, Object>> inWorld = new ArrayList<>();
        List<Map<String, Object>> overlay = new ArrayList<>();

        for (MdAstAnyContent child : element.children()) {
            if (!(child instanceof MdxJsxFlowElement flowElement)) {
                if (child instanceof MdAstNode node) {
                    node.toText();
                }
                continue;
            }

            String name = flowElement.name();
            if ("PlaySound".equals(name)) {
                continue;
            }
            if ("BoxAnnotation".equals(name)) {
                float[] min = parseVector3(readOptional(flowElement, "min"), new float[] { 0f, 0f, 0f });
                float[] max = parseVector3(readOptional(flowElement, "max"), new float[] { 0f, 0f, 0f });
                ensureMinMax(min, max);
                inWorld.add(
                    buildInWorldAnnotation(
                        "box",
                        min,
                        max,
                        null,
                        null,
                        normalizeColor(readOptional(flowElement, "color"), "#ffffff"),
                        readFloat(flowElement, "thickness", InWorldBoxAnnotation.DEFAULT_THICKNESS),
                        createTemplateId(flowElement, defaultNamespace, currentPageId, templates),
                        readBooleanValue(flowElement, "alwaysOnTop", false),
                        parseStructureLibCondition(flowElement)));
                continue;
            }

            if ("BlockAnnotation".equals(name)) {
                float[] pos = parseVector3(readOptional(flowElement, "pos"), new float[] { 0f, 0f, 0f });
                float[] min = new float[] { pos[0], pos[1], pos[2] };
                float[] max = new float[] { pos[0] + 1f, pos[1] + 1f, pos[2] + 1f };
                inWorld.add(
                    buildInWorldAnnotation(
                        "box",
                        min,
                        max,
                        null,
                        null,
                        normalizeColor(readOptional(flowElement, "color"), "#ffffff"),
                        readFloat(flowElement, "thickness", InWorldBoxAnnotation.DEFAULT_THICKNESS),
                        createTemplateId(flowElement, defaultNamespace, currentPageId, templates),
                        readBooleanValue(flowElement, "alwaysOnTop", false),
                        parseStructureLibCondition(flowElement)));
                continue;
            }

            if ("LineAnnotation".equals(name)) {
                List<float[]> points = parseLinePoints(flowElement);
                float[] from = points.getFirst();
                float[] to = points.getLast();
                inWorld.add(
                    buildInWorldAnnotation(
                        "line",
                        null,
                        null,
                        from,
                        to,
                        normalizeColor(readOptional(flowElement, "color"), "#ffffff"),
                        readFloat(flowElement, "thickness", InWorldLineAnnotation.DEFAULT_THICKNESS),
                        createTemplateId(flowElement, defaultNamespace, currentPageId, templates),
                        readBooleanValue(flowElement, "alwaysOnTop", false),
                        parseStructureLibCondition(flowElement)));
                Map<String, Object> data = inWorld.getLast();
                data.put("points", points);
                String arrow = readOptional(flowElement, "arrow");
                if (arrow != null && !arrow.isEmpty()) {
                    data.put("arrow", arrow);
                }
                if (readBooleanValue(flowElement, "showPoints", false)) {
                    data.put("showPoints", true);
                }
                data.put(
                    "pointColor",
                    normalizeColor(
                        readOptional(flowElement, "pointColor"),
                        data.get("color")
                            .toString()));
                data.put(
                    "pointSize",
                    GuideSiteSceneAnnotationSerializer.exportWorldAnnotationSize(
                        readFloat(
                            flowElement,
                            "pointSize",
                            readFloat(flowElement, "thickness", InWorldLineAnnotation.DEFAULT_THICKNESS)
                                * InWorldLineAnnotation.DEFAULT_POINT_SIZE_SCALE)));
                List<Map<String, Object>> pointStyles = collectLinePointStyles(flowElement);
                if (!pointStyles.isEmpty()) {
                    data.put("pointStyles", pointStyles);
                }
                continue;
            }

            if ("DiamondAnnotation".equals(name)) {
                float[] pos = parseVector3(readOptional(flowElement, "pos"), new float[] { 0f, 0f, 0f });
                Map<String, Object> data = new LinkedHashMap<>();
                data.put("type", "diamond");
                data.put("position", pos);
                data.put("color", normalizeColor(readOptional(flowElement, "color"), "#00e000"));
                appendStructureLibCondition(data, parseStructureLibCondition(flowElement));
                String templateId = createTemplateId(flowElement, defaultNamespace, currentPageId, templates);
                if (templateId != null) {
                    data.put("contentTemplateId", templateId);
                }
                overlay.add(data);
                continue;
            }

            if ("TextAnnotation".equals(name)) {
                Map<String, Object> data = buildTextAnnotationData(
                    flowElement,
                    defaultNamespace,
                    currentPageId,
                    templates);
                overlay.add(data);
                continue;
            }

            if ("InputAnnotation".equals(name)) {
                Map<String, Object> data = buildInputAnnotationData(flowElement);
                overlay.add(data);
            }
        }

        return new AnnotationPayload(GSON.toJson(inWorld), GSON.toJson(overlay));
    }

    private Map<String, Object> buildTextAnnotationData(MdxJsxElementFields flowElement, String defaultNamespace,
        ResourceLocation currentPageId, GuideSiteTemplateRegistry templates) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("type", "text");
        data.put("position", parseTextAnnotationPosition(flowElement));
        data.put("color", normalizeColor(readOptional(flowElement, "color"), "#ffffffff"));
        data.put("text", compileTextAnnotationHtml(flowElement, defaultNamespace, currentPageId, templates));
        data.put("plainText", resolveTextAnnotationPlainText(flowElement));
        data.put("maxWidth", Math.max(0, readDimension(flowElement, "maxWidth", 0)));
        data.put(
            "backgroundAlpha",
            clampInt(readDimension(flowElement, "backgroundAlpha", TextAnnotation.DEFAULT_BACKGROUND_ALPHA), 0, 255));
        data.put("independent", readBooleanValue(flowElement, "independent", false));
        data.put("screenYOffset", readFloat(flowElement, "yOffset", 0f));
        data.put("connectorSide", normalizeConnectorSide(readOptional(flowElement, "connectorSide")));
        data.put("connectorOffset", readDimension(flowElement, "connectorOffset", 0));
        data.put(
            "connectorLength",
            Math.max(0, readDimension(flowElement, "connectorLength", TextAnnotation.CONNECTOR_HEIGHT)));
        appendStructureLibCondition(data, parseStructureLibCondition(flowElement));
        return data;
    }

    private Map<String, Object> buildInputAnnotationData(MdxJsxElementFields flowElement) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("type", "input");
        data.put("position", parseTextAnnotationPosition(flowElement));
        data.put("inputType", resolveInputType(readOptional(flowElement, "inputType")));
        String modifier = normalizeOptionalText(readOptional(flowElement, "modifier"));
        if (modifier != null) {
            data.put("modifier", modifier);
        }
        GuideSiteExportedItem item = resolveExportedInputItem(readOptional(flowElement, "item"));
        if (item != null && !item.isEmpty()) {
            data.put("item", serializeExportedItem(item));
        }
        appendStructureLibCondition(data, parseStructureLibCondition(flowElement));
        return data;
    }

    private float[] parseTextAnnotationPosition(MdxJsxElementFields flowElement) {
        String pos = readOptional(flowElement, "pos");
        if (pos != null && !pos.trim()
            .isEmpty()) {
            return parseVector3(pos, new float[] { 0f, 0f, 0f });
        }
        return new float[] { readFloat(flowElement, "x", 0f), readFloat(flowElement, "y", 0f),
            readFloat(flowElement, "z", 0f) };
    }

    private String compileTextAnnotationHtml(MdxJsxElementFields flowElement, String defaultNamespace,
        ResourceLocation currentPageId, GuideSiteTemplateRegistry templates) {
        String source = resolveTextAnnotationPlainText(flowElement);
        if (source == null || source.trim()
            .isEmpty()) {
            return "";
        }
        if (flowElement.children() != null && !flowElement.children()
            .isEmpty()) {
            String html = fragmentCompiler
                .compileFragment(flowElement.children(), templates, defaultNamespace, currentPageId);
            if (html != null && !html.trim()
                .isEmpty()) {
                return html;
            }
        }
        return GuideSiteSceneAnnotationSerializer.renderPlainTextFragment(source);
    }

    private String resolveTextAnnotationPlainText(MdxJsxElementFields flowElement) {
        String textKey = readOptional(flowElement, "textKey");
        if (textKey != null && !textKey.trim()
            .isEmpty()) {
            String localized = GuideResourceLanguageIndex.getValue(LangUtil.getCurrentLanguage(), textKey.trim());
            if (localized != null && !localized.isEmpty()) {
                return localized;
            }
        }
        String text = readOptional(flowElement, "text");
        if (text != null && !text.trim()
            .isEmpty()) {
            return text;
        }
        StringBuilder builder = new StringBuilder();
        for (MdAstAnyContent child : flowElement.children()) {
            if (child instanceof MdAstNode node) {
                if (!builder.isEmpty()) {
                    builder.append('\n');
                }
                builder.append(node.toText());
            }
        }
        return builder.toString();
    }

    private String normalizeConnectorSide(@Nullable String rawConnectorSide) {
        if (rawConnectorSide == null || rawConnectorSide.trim()
            .isEmpty()) {
            return TextAnnotation.ConnectorSide.BOTTOM.serializedName();
        }
        try {
            return TextAnnotation.ConnectorSide.fromSerializedName(rawConnectorSide)
                .serializedName();
        } catch (IllegalArgumentException ignored) {
            return TextAnnotation.ConnectorSide.BOTTOM.serializedName();
        }
    }

    private String resolveInputType(@Nullable String rawInputType) {
        if (rawInputType == null) {
            return PonderInputAnnotation.InputType.LMB.name()
                .toLowerCase();
        }
        return switch (rawInputType.trim()
            .toLowerCase()) {
            case "rmb", "rightclick", "right_click", "right-click" -> PonderInputAnnotation.InputType.RMB.name()
                .toLowerCase();
            case "scroll", "scrollwheel", "scroll_wheel", "scroll-wheel" -> PonderInputAnnotation.InputType.SCROLL
                .name()
                .toLowerCase();
            default -> PonderInputAnnotation.InputType.LMB.name()
                .toLowerCase();
        };
    }

    @Nullable
    private GuideSiteExportedItem resolveExportedInputItem(@Nullable String rawItemId) {
        String itemId = normalizeOptionalText(rawItemId);
        if (itemId == null) {
            return null;
        }
        ItemStack stack = resolveItemStack(itemId);
        if (stack == null || stack.getItem() == null) {
            return GuideSiteItemSupport.unresolved(itemId);
        }
        return GuideSiteItemSupport.export(null, stack, itemIconResolver, itemId);
    }

    @Nullable
    private ItemStack resolveItemStack(@Nullable String itemId) {
        String normalized = normalizeOptionalText(itemId);
        if (normalized == null) {
            return null;
        }
        String registryId = normalized;
        int meta = 0;
        int firstColon = normalized.indexOf(':');
        int lastColon = normalized.lastIndexOf(':');
        if (firstColon >= 0 && firstColon != lastColon) {
            try {
                meta = Integer.parseInt(normalized.substring(lastColon + 1));
                registryId = normalized.substring(0, lastColon);
            } catch (NumberFormatException ignored) {
                registryId = normalized;
            }
        }
        Item item = (Item) Item.itemRegistry.getObject(registryId);
        if (item == null) {
            return null;
        }
        return new ItemStack(item, 1, meta);
    }

    @Nullable
    private String normalizeOptionalText(@Nullable String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private Map<String, Object> serializeExportedItem(GuideSiteExportedItem item) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("itemId", item.itemId());
        data.put("displayName", item.displayName());
        data.put("iconSrc", item.iconSrc());
        return data;
    }

    private Map<String, Object> buildInWorldAnnotation(String type, float[] min, float[] max, float[] from, float[] to,
        String color, float thickness, String templateId, boolean alwaysOnTop,
        @Nullable StructureLibSceneCondition structureLibCondition) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("type", type);
        if (min != null) {
            data.put("minCorner", min);
        }
        if (max != null) {
            data.put("maxCorner", max);
        }
        if (from != null) {
            data.put("from", from);
        }
        if (to != null) {
            data.put("to", to);
        }
        data.put("color", color);
        data.put("thickness", GuideSiteSceneAnnotationSerializer.exportWorldAnnotationSize(thickness));
        if (templateId != null) {
            data.put("contentTemplateId", templateId);
        }
        data.put("alwaysOnTop", alwaysOnTop);
        appendStructureLibCondition(data, structureLibCondition);
        return data;
    }

    private List<float[]> parseLinePoints(MdxJsxElementFields flowElement) {
        String rawPoints = readOptional(flowElement, "points");
        if (rawPoints != null && !rawPoints.trim()
            .isEmpty()) {
            List<float[]> points = new ArrayList<>();
            for (String token : rawPoints.split(";")) {
                String trimmed = token.trim();
                if (trimmed.isEmpty()) {
                    continue;
                }
                points.add(parseVector3(trimmed, new float[] { 0f, 0f, 0f }));
            }
            if (points.size() >= 2) {
                return points;
            }
        }
        float[] from = parseVector3(readOptional(flowElement, "from"), new float[] { 0f, 0f, 0f });
        float[] to = parseVector3(readOptional(flowElement, "to"), new float[] { 0f, 0f, 0f });
        List<float[]> points = new ArrayList<>(2);
        points.add(from);
        points.add(to);
        return points;
    }

    private List<Map<String, Object>> collectLinePointStyles(MdxJsxElementFields flowElement) {
        List<Map<String, Object>> styles = new ArrayList<>();
        for (MdAstAnyContent child : flowElement.children()) {
            if (!(child instanceof MdxJsxFlowElement pointElement) || !"LinePoint".equals(pointElement.name())) {
                continue;
            }
            Map<String, Object> style = new LinkedHashMap<>();
            style.put("index", Math.max(0, Math.round(readFloat(pointElement, "index", 0f))));
            if (pointElement.hasAttribute("show")) {
                style.put("show", readBooleanValue(pointElement, "show", true));
            }
            String color = readOptional(pointElement, "color");
            if (color != null) {
                style.put("color", normalizeColor(color, "#ffffff"));
            }
            if (pointElement.hasAttribute("size")) {
                style.put(
                    "size",
                    GuideSiteSceneAnnotationSerializer.exportWorldAnnotationSize(
                        readFloat(pointElement, "size", InWorldLineAnnotation.DEFAULT_POINT_SIZE_SCALE)));
            }
            styles.add(style);
        }
        return styles;
    }

    @Nullable
    private StructureLibSceneCondition parseStructureLibCondition(MdxJsxElementFields element) {
        return StructureLibSceneCondition.parse(
            readOptional(element, "showWhenStructure"),
            readOptional(element, "showWhenTier"),
            readOptional(element, "showWhenChannels"));
    }

    private void appendStructureLibCondition(Map<String, Object> data,
        @Nullable StructureLibSceneCondition structureLibCondition) {
        if (data == null || structureLibCondition == null || !structureLibCondition.hasAnyConstraint()) {
            return;
        }
        data.put("structureLibCondition", structureLibCondition.toSiteExportData());
    }

    private String createTemplateId(MdxJsxElementFields element, String defaultNamespace,
        ResourceLocation currentPageId, GuideSiteTemplateRegistry templates) {
        String html = fragmentCompiler.compileFragment(element.children(), templates, defaultNamespace, currentPageId);
        if (html == null || html.trim()
            .isEmpty()) {
            return null;
        }
        return templates.create(html);
    }

    private void ensureMinMax(float[] min, float[] max) {
        if (min[0] > max[0]) {
            float swap = min[0];
            min[0] = max[0];
            max[0] = swap;
        }
        if (min[1] > max[1]) {
            float swap = min[1];
            min[1] = max[1];
            max[1] = swap;
        }
        if (min[2] > max[2]) {
            float swap = min[2];
            min[2] = max[2];
            max[2] = swap;
        }
    }

    private float[] parseVector3(String raw, float[] fallback) {
        if (raw == null || raw.trim()
            .isEmpty()) {
            return fallback;
        }
        String[] parts = raw.trim()
            .split("\\s+");
        if (parts.length < 3) {
            return fallback;
        }
        try {
            return new float[] { Float.parseFloat(parts[0]), Float.parseFloat(parts[1]), Float.parseFloat(parts[2]) };
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private String normalizeColor(String raw, String fallback) {
        if (raw == null || raw.trim()
            .isEmpty()) {
            return fallback;
        }
        String trimmed = raw.trim();
        if (trimmed.startsWith("#") && trimmed.length() == 9) {
            int alpha = Integer.parseInt(trimmed.substring(1, 3), 16);
            int red = Integer.parseInt(trimmed.substring(3, 5), 16);
            int green = Integer.parseInt(trimmed.substring(5, 7), 16);
            int blue = Integer.parseInt(trimmed.substring(7, 9), 16);
            return "rgba(" + red + "," + green + "," + blue + "," + alpha / 255.0f + ")";
        }
        return trimmed;
    }

    private int readDimension(MdxJsxElementFields element, String name, int fallback) {
        String raw = readOptional(element, name);
        if (raw == null || raw.trim()
            .isEmpty()) {
            return fallback;
        }
        try {
            int parsed = Integer.parseInt(raw.trim());
            return parsed > 0 ? parsed : fallback;
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private float readFloat(MdxJsxElementFields element, String name, float fallback) {
        String raw = readOptional(element, name);
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

    private int clampInt(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private boolean readBooleanValue(MdxJsxElementFields element, String name, boolean fallback) {
        try {
            return MdxAttrs.getBoolean(element, name, fallback);
        } catch (MdxAttrs.AttributeException ignored) {
            return fallback;
        }
    }

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

    private String escapeAttribute(String text) {
        return escapeAttributeStatic(text);
    }

    private static String escapeAttributeStatic(String text) {
        return text.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;");
    }

    private static GuideSiteHtmlCompiler.MdxTagRenderer noopMdxTagRenderer() {
        return (element, defaultNamespace, currentPageId, templates, sceneResolver, compiler) -> null;
    }

    public static class AnnotationPayload {

        public final String inWorldJson;
        public final String overlayJson;

        private AnnotationPayload(String inWorldJson, String overlayJson) {
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
}
