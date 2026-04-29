package com.hfstudio.guidenh.guide.siteexport.site;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.util.ResourceLocation;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.hfstudio.guidenh.libs.mdast.mdx.model.MdxJsxAttribute;
import com.hfstudio.guidenh.libs.mdast.mdx.model.MdxJsxElementFields;
import com.hfstudio.guidenh.libs.mdast.mdx.model.MdxJsxFlowElement;
import com.hfstudio.guidenh.libs.mdast.model.MdAstAnyContent;
import com.hfstudio.guidenh.libs.mdast.model.MdAstNode;

public class GuideSiteSceneTagRenderer implements GuideSiteHtmlCompiler.SceneTagRenderer {

    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping()
        .serializeNulls()
        .create();
    private static final String TRANSPARENT_PIXEL = "data:image/gif;base64,R0lGODlhAQABAIAAAAAAAP///ywAAAAAAQABAAACAUwAOw==";
    private static final String[] FORWARDED_ATTRIBUTES = { "zoom", "perspective", "rotateX", "rotateY", "rotateZ",
        "offsetX", "offsetY", "centerX", "centerY", "centerZ", "allowLayerSlider" };

    private final GuideSiteHtmlCompiler fragmentCompiler;

    public GuideSiteSceneTagRenderer() {
        this(new GuideSiteRecipeTagRenderer(), new GuideSiteHtmlCompiler.ImageResolver() {

            @Override
            public String resolve(String rawUrl, ResourceLocation currentPageId) {
                return rawUrl != null ? rawUrl : "";
            }
        }, noopMdxTagRenderer());
    }

    public GuideSiteSceneTagRenderer(GuideSiteHtmlCompiler.ImageResolver imageResolver) {
        this(new GuideSiteRecipeTagRenderer(), imageResolver, noopMdxTagRenderer());
    }

    public GuideSiteSceneTagRenderer(GuideSiteHtmlCompiler.RecipeTagRenderer recipeTagRenderer,
        GuideSiteHtmlCompiler.ImageResolver imageResolver, GuideSiteHtmlCompiler.MdxTagRenderer mdxTagRenderer) {
        this.fragmentCompiler = new GuideSiteHtmlCompiler(
            recipeTagRenderer,
            new GuideSiteHtmlCompiler.SceneTagRenderer() {

                @Override
                public String render(MdxJsxElementFields element, String defaultNamespace,
                    ResourceLocation currentPageId, GuideSiteTemplateRegistry templates,
                    GuideSiteExportedScene exportedScene) {
                    return "";
                }
            },
            imageResolver,
            mdxTagRenderer != null ? mdxTagRenderer : noopMdxTagRenderer());
    }

    @Override
    public String render(MdxJsxElementFields element, String defaultNamespace, ResourceLocation currentPageId,
        GuideSiteTemplateRegistry templates, GuideSiteExportedScene exportedScene) {
        String width = readDimension(element, "width", 256);
        String height = readDimension(element, "height", 192);
        String interactive = readBoolean(element, "interactive", true);
        String background = readOptional(element, "background");
        AnnotationPayload payload = resolveAnnotationPayload(
            element,
            defaultNamespace,
            currentPageId,
            templates,
            exportedScene);

        String src = exportedScene != null ? GuideSitePageAssetExporter.ROOT_PREFIX + exportedScene.placeholderPath()
            : TRANSPARENT_PIXEL;
        String sceneSrc = exportedScene != null ? GuideSitePageAssetExporter.ROOT_PREFIX + exportedScene.scenePath()
            : null;
        String cssClass = sceneSrc != null ? "game-scene guide-scene" : "guide-scene";

        StringBuilder html = new StringBuilder();
        html.append("<img class=\"")
            .append(cssClass)
            .append("\" src=\"")
            .append(escapeAttribute(src))
            .append("\" alt=\"3D scene preview\" loading=\"lazy\" decoding=\"async\" width=\"")
            .append(escapeAttribute(width))
            .append("\" height=\"")
            .append(escapeAttribute(height))
            .append("\" data-scene-width=\"")
            .append(escapeAttribute(width))
            .append("\" data-scene-height=\"")
            .append(escapeAttribute(height))
            .append("\" data-scene-interactive=\"")
            .append(escapeAttribute(interactive))
            .append("\" data-scene-default-namespace=\"")
            .append(escapeAttribute(defaultNamespace))
            .append("\"");

        if (sceneSrc != null) {
            html.append(" data-scene-src=\"")
                .append(escapeAttribute(sceneSrc))
                .append("\" data-scene-asset-prefix=\"")
                .append(escapeAttribute(GuideSitePageAssetExporter.ROOT_PREFIX))
                .append("\"");
        }
        if (background != null && !background.isEmpty()) {
            html.append(" data-scene-background=\"")
                .append(escapeAttribute(background))
                .append("\"");
        }
        if (!payload.inWorldJson.isEmpty()) {
            html.append(" data-scene-in-world-annotations=\"")
                .append(escapeAttribute(payload.inWorldJson))
                .append("\"");
        }
        if (!payload.overlayJson.isEmpty()) {
            html.append(" data-scene-overlay-annotations=\"")
                .append(escapeAttribute(payload.overlayJson))
                .append("\"");
        }
        if (exportedScene != null && exportedScene.hoverTargetsJson() != null
            && !exportedScene.hoverTargetsJson()
                .isEmpty()) {
            html.append(" data-scene-hover-targets=\"")
                .append(escapeAttribute(exportedScene.hoverTargetsJson()))
                .append("\"");
        }

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

        html.append(">");
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
        List<Map<String, Object>> inWorld = new ArrayList<Map<String, Object>>();
        List<Map<String, Object>> overlay = new ArrayList<Map<String, Object>>();

        for (MdAstAnyContent child : element.children()) {
            if (!(child instanceof MdxJsxFlowElement flowElement)) {
                if (child instanceof MdAstNode node && node.toText()
                    .trim()
                    .isEmpty()) {
                    continue;
                }
                continue;
            }

            String name = flowElement.name();
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
                        readFloat(flowElement, "thickness", 1.0f),
                        createTemplateId(flowElement, defaultNamespace, currentPageId, templates),
                        readBooleanValue(flowElement, "alwaysOnTop", false)));
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
                        readFloat(flowElement, "thickness", 1.0f),
                        createTemplateId(flowElement, defaultNamespace, currentPageId, templates),
                        readBooleanValue(flowElement, "alwaysOnTop", false)));
                continue;
            }

            if ("LineAnnotation".equals(name)) {
                float[] from = parseVector3(readOptional(flowElement, "from"), new float[] { 0f, 0f, 0f });
                float[] to = parseVector3(readOptional(flowElement, "to"), new float[] { 0f, 0f, 0f });
                inWorld.add(
                    buildInWorldAnnotation(
                        "line",
                        null,
                        null,
                        from,
                        to,
                        normalizeColor(readOptional(flowElement, "color"), "#ffffff"),
                        readFloat(flowElement, "thickness", 1.0f),
                        createTemplateId(flowElement, defaultNamespace, currentPageId, templates),
                        readBooleanValue(flowElement, "alwaysOnTop", false)));
                continue;
            }

            if ("DiamondAnnotation".equals(name)) {
                float[] pos = parseVector3(readOptional(flowElement, "pos"), new float[] { 0f, 0f, 0f });
                Map<String, Object> data = new LinkedHashMap<String, Object>();
                data.put("type", "overlay");
                data.put("position", pos);
                data.put("color", normalizeColor(readOptional(flowElement, "color"), "#00e000"));
                String templateId = createTemplateId(flowElement, defaultNamespace, currentPageId, templates);
                if (templateId != null) {
                    data.put("contentTemplateId", templateId);
                }
                overlay.add(data);
            }
        }

        return new AnnotationPayload(GSON.toJson(inWorld), GSON.toJson(overlay));
    }

    private Map<String, Object> buildInWorldAnnotation(String type, float[] min, float[] max, float[] from, float[] to,
        String color, float thickness, String templateId, boolean alwaysOnTop) {
        Map<String, Object> data = new LinkedHashMap<String, Object>();
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
        data.put("thickness", Float.valueOf(thickness));
        if (templateId != null) {
            data.put("contentTemplateId", templateId);
        }
        data.put("alwaysOnTop", Boolean.valueOf(alwaysOnTop));
        return data;
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

    private String readDimension(MdxJsxElementFields element, String name, int fallback) {
        String raw = readOptional(element, name);
        if (raw == null || raw.trim()
            .isEmpty()) {
            return Integer.toString(fallback);
        }
        try {
            int parsed = Integer.parseInt(raw.trim());
            return parsed > 0 ? Integer.toString(parsed) : Integer.toString(fallback);
        } catch (NumberFormatException ignored) {
            return Integer.toString(fallback);
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

    private String readBoolean(MdxJsxElementFields element, String name, boolean fallback) {
        return Boolean.toString(readBooleanValue(element, name, fallback));
    }

    private boolean readBooleanValue(MdxJsxElementFields element, String name, boolean fallback) {
        MdxJsxAttribute attribute = element.getAttribute(name);
        if (attribute == null) {
            return fallback;
        }
        if (attribute.hasStringValue()) {
            return normalizeBoolean(attribute.getStringValue(), fallback);
        }
        if (attribute.hasExpressionValue()) {
            return normalizeBoolean(attribute.getExpressionValue(), fallback);
        }
        return true;
    }

    private boolean normalizeBoolean(String value, boolean fallback) {
        if (value == null) {
            return fallback;
        }
        String trimmed = value.trim();
        if ("true".equalsIgnoreCase(trimmed)) {
            return true;
        }
        if ("false".equalsIgnoreCase(trimmed)) {
            return false;
        }
        return fallback;
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
        return text.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;");
    }

    private static GuideSiteHtmlCompiler.MdxTagRenderer noopMdxTagRenderer() {
        return new GuideSiteHtmlCompiler.MdxTagRenderer() {

            @Override
            public String render(MdxJsxElementFields element, String defaultNamespace, ResourceLocation currentPageId,
                GuideSiteTemplateRegistry templates, GuideSiteHtmlCompiler.SceneResolver sceneResolver,
                GuideSiteHtmlCompiler compiler) {
                return null;
            }
        };
    }

    private static final class AnnotationPayload {

        private final String inWorldJson;
        private final String overlayJson;

        private AnnotationPayload(String inWorldJson, String overlayJson) {
            this.inWorldJson = inWorldJson;
            this.overlayJson = overlayJson;
        }

        private String inWorldJson() {
            return inWorldJson;
        }

        private String overlayJson() {
            return overlayJson;
        }
    }
}
