package com.hfstudio.guidenh.guide.internal.editor.md;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import com.hfstudio.guidenh.guide.compiler.GuideMarkdownOptions;
import com.hfstudio.guidenh.guide.compiler.tags.MdxAttrs;
import com.hfstudio.guidenh.guide.internal.editor.model.SceneEditorElementModel;
import com.hfstudio.guidenh.guide.internal.editor.model.SceneEditorElementType;
import com.hfstudio.guidenh.guide.internal.editor.model.SceneEditorSceneModel;
import com.hfstudio.guidenh.guide.internal.editor.model.SceneEditorSceneNodeModel;
import com.hfstudio.guidenh.guide.internal.editor.model.SceneEditorSceneNodeType;
import com.hfstudio.guidenh.guide.internal.util.GuideStringLines;
import com.hfstudio.guidenh.guide.scene.annotation.compiler.BlockAnnotationTemplateElementCompiler;
import com.hfstudio.guidenh.libs.mdast.MdAst;
import com.hfstudio.guidenh.libs.mdast.MdastOptions;
import com.hfstudio.guidenh.libs.mdast.mdx.model.MdxJsxAttribute;
import com.hfstudio.guidenh.libs.mdast.mdx.model.MdxJsxAttributeNode;
import com.hfstudio.guidenh.libs.mdast.mdx.model.MdxJsxElementFields;
import com.hfstudio.guidenh.libs.mdast.model.MdAstHTML;
import com.hfstudio.guidenh.libs.mdast.model.MdAstRoot;
import com.hfstudio.guidenh.libs.mdx.MdxCommentMasker;
import com.hfstudio.guidenh.libs.micromark.ParseException;
import com.hfstudio.guidenh.libs.unist.UnistNode;
import com.hfstudio.guidenh.libs.unist.UnistParent;
import com.hfstudio.guidenh.libs.unist.UnistPosition;

public class SceneEditorMarkdownCodec {

    public static final MdastOptions PARSE_OPTIONS = GuideMarkdownOptions.sceneEditor();

    public static final Set<String> ROOT_TAG_NAMES = Collections
        .unmodifiableSet(new HashSet<>(Arrays.asList("GameScene", "Scene")));
    public static final Set<String> ROOT_ATTRIBUTES = Collections.unmodifiableSet(
        new HashSet<>(
            Arrays.asList(
                "width",
                "height",
                "perspective",
                "zoom",
                "rotateX",
                "rotateY",
                "rotateZ",
                "offsetX",
                "offsetY",
                "centerX",
                "centerY",
                "centerZ",
                "interactive",
                "showBackground",
                "allowLayerSlider")));
    public static final Set<String> IMPORT_STRUCTURE_ATTRIBUTES = Collections
        .unmodifiableSet(new HashSet<>(Arrays.asList("src", "x", "y", "z", "offsetX", "offsetY", "offsetZ", "formed")));
    public static final Set<String> IMPORT_STRUCTURE_LIB_ATTRIBUTES = Collections.unmodifiableSet(
        new HashSet<>(
            Arrays.asList(
                "controller",
                "name",
                "piece",
                "facing",
                "rotation",
                "flip",
                "channel",
                "offsetX",
                "offsetY",
                "offsetZ",
                "formed")));
    public static final Set<String> REMOVE_BLOCKS_ATTRIBUTES = Collections
        .unmodifiableSet(new HashSet<>(Collections.singletonList("id")));
    public static final Set<String> BLOCK_ANNOTATION_TEMPLATE_ATTRIBUTES = Collections
        .unmodifiableSet(new HashSet<>(Arrays.asList("id", "showWhenStructure", "showWhenTier", "showWhenChannels")));
    public static final Set<String> BLOCK_ATTRIBUTES = Collections.unmodifiableSet(
        new HashSet<>(
            Arrays.asList(
                "pos",
                "color",
                "thickness",
                "alwaysOnTop",
                "visible",
                "showWhenStructure",
                "showWhenTier",
                "showWhenChannels")));
    public static final Set<String> BOX_ATTRIBUTES = Collections.unmodifiableSet(
        new HashSet<>(
            Arrays.asList(
                "min",
                "max",
                "color",
                "thickness",
                "alwaysOnTop",
                "visible",
                "showWhenStructure",
                "showWhenTier",
                "showWhenChannels")));
    public static final Set<String> LINE_ATTRIBUTES = Collections.unmodifiableSet(
        new HashSet<>(
            Arrays.asList(
                "from",
                "to",
                "points",
                "arrow",
                "color",
                "thickness",
                "alwaysOnTop",
                "visible",
                "showPoints",
                "pointColor",
                "pointSize",
                "showWhenStructure",
                "showWhenTier",
                "showWhenChannels")));
    public static final Set<String> DIAMOND_ATTRIBUTES = Collections.unmodifiableSet(
        new HashSet<>(
            Arrays.asList(
                "pos",
                "color",
                "alwaysOnTop",
                "visible",
                "showWhenStructure",
                "showWhenTier",
                "showWhenChannels")));
    public static final Set<String> TEXT_ATTRIBUTES = Collections.unmodifiableSet(
        new HashSet<>(
            Arrays.asList(
                "pos",
                "x",
                "y",
                "z",
                "color",
                "maxWidth",
                "backgroundAlpha",
                "textKey",
                "independent",
                "yOffset",
                "connectorSide",
                "connectorOffset",
                "connectorLength",
                "hlMinX",
                "hlMinY",
                "hlMinZ",
                "hlMaxX",
                "hlMaxY",
                "hlMaxZ",
                "highlightColor",
                "visible",
                "showWhenStructure",
                "showWhenTier",
                "showWhenChannels")));

    public SceneEditorMarkdownParseResult parse(String markdown) {
        String normalized = normalizeLineEndings(markdown);
        String parseSource = MdxCommentMasker.mask(normalized);

        MdAstRoot root;
        try {
            root = MdAst.fromMarkdown(parseSource, PARSE_OPTIONS);
        } catch (ParseException e) {
            return new SceneEditorMarkdownParseResult.SyntaxError(formatParseException(e));
        }

        try {
            MdxJsxElementFields sceneElement = requireSingleRootScene(root, normalized);
            SceneEditorSceneModel model = parseScene(sceneElement, normalized);
            return new SceneEditorMarkdownParseResult.Success(model);
        } catch (UnsupportedSubsetException e) {
            return new SceneEditorMarkdownParseResult.Unsupported(e.getMessage());
        } catch (InvalidSceneSyntaxException e) {
            return new SceneEditorMarkdownParseResult.SyntaxError(e.getMessage());
        }
    }

    public String serialize(SceneEditorSceneModel model) {
        StringBuilder builder = new StringBuilder();
        builder.append("<GameScene");
        appendRootAttributes(builder, model);

        List<SceneEditorSceneNodeModel> sceneNodes = collectSerializableSceneNodes(model);
        if (sceneNodes.isEmpty()) {
            builder.append(" />");
            return builder.toString();
        }

        builder.append(">\n");
        for (SceneEditorSceneNodeModel sceneNode : sceneNodes) {
            appendSceneNode(builder, sceneNode);
        }
        builder.append("</GameScene>");
        return builder.toString();
    }

    private MdxJsxElementFields requireSingleRootScene(MdAstRoot root, String source) {
        List<?> children = root.children();
        MdxJsxElementFields sceneElement = null;
        int sceneCount = 0;
        for (Object child : children) {
            if (!(child instanceof UnistNode node)) {
                continue;
            }
            if (isIgnorableNode(node, source)) {
                continue;
            }
            if (!(child instanceof MdxJsxElementFields element)) {
                throw new UnsupportedSubsetException("Only a single <GameScene> root is supported");
            }
            if (!ROOT_TAG_NAMES.contains(element.name())) {
                throw new UnsupportedSubsetException("Only <GameScene> is supported as the root tag");
            }
            sceneElement = element;
            sceneCount++;
        }
        if (sceneCount != 1 || sceneElement == null) {
            throw new UnsupportedSubsetException("Only a single <GameScene> root is supported");
        }
        return sceneElement;
    }

    private SceneEditorSceneModel parseScene(MdxJsxElementFields sceneElement, String source) {
        ensureAllowedAttributes(sceneElement, ROOT_ATTRIBUTES, "GameScene");

        SceneEditorSceneModel model = SceneEditorSceneModel.blank();
        model.setPreviewWidth(parseIntAttribute(sceneElement, "width", model.getPreviewWidth()));
        model.setPreviewHeight(parseIntAttribute(sceneElement, "height", model.getPreviewHeight()));
        model.setPerspectivePreset(parseOptionalStringAttribute(sceneElement, "perspective"));
        model.setZoom(parseOptionalFloatAttribute(sceneElement, "zoom"));
        model.setRotationX(parseOptionalFloatAttribute(sceneElement, "rotateX"));
        model.setRotationY(parseOptionalFloatAttribute(sceneElement, "rotateY"));
        model.setRotationZ(parseOptionalFloatAttribute(sceneElement, "rotateZ"));
        model.setOffsetX(parseOptionalFloatAttribute(sceneElement, "offsetX"));
        model.setOffsetY(parseOptionalFloatAttribute(sceneElement, "offsetY"));
        model.setCenterX(parseOptionalFloatAttribute(sceneElement, "centerX"));
        model.setCenterY(parseOptionalFloatAttribute(sceneElement, "centerY"));
        model.setCenterZ(parseOptionalFloatAttribute(sceneElement, "centerZ"));
        model.setInteractive(parseBooleanAttribute(sceneElement, "interactive", model.isInteractive()));
        model.setShowBackground(parseBooleanAttribute(sceneElement, "showBackground", model.isShowBackground()));
        model.setAllowLayerSlider(parseBooleanAttribute(sceneElement, "allowLayerSlider", model.isAllowLayerSlider()));

        for (Object child : sceneElement.children()) {
            if (!(child instanceof UnistNode node)) {
                continue;
            }
            if (isIgnorableNode(node, source)) {
                continue;
            }
            MdxJsxElementFields element = unwrapJsxElement(node, source);
            if (element != null) {
                appendParsedSceneChild(model, node, element, source);
                continue;
            }
            if (!appendParsedSceneChildFromRawText(model, node, source)) {
                String rawText = extractRawNodeText(node, source);
                if (rawText != null && !rawText.trim()
                    .isEmpty()) {
                    SceneEditorSceneNodeModel opaqueNode = new SceneEditorSceneNodeModel(
                        SceneEditorSceneNodeType.OPAQUE);
                    opaqueNode.setOpaqueText(rawText);
                    model.addSceneNode(opaqueNode);
                }
            }
        }

        return model;
    }

    private void appendParsedSceneChild(SceneEditorSceneModel model, UnistNode node, MdxJsxElementFields element,
        String source) {
        String tagName = element.name();
        if ("ImportStructure".equals(tagName)) {
            model.addSceneNode(parseImportStructureNode(element));
            return;
        }
        if ("ImportStructureLib".equals(tagName)) {
            model.addSceneNode(parseImportStructureLibNode(element));
            return;
        }
        if ("RemoveBlocks".equals(tagName)) {
            model.addSceneNode(parseRemoveBlocksNode(element));
            return;
        }
        if ("BlockAnnotationTemplate".equals(tagName)) {
            model.addSceneNode(parseBlockAnnotationTemplateNode(element, source));
            return;
        }

        try {
            model.addElement(parseElement(element, source));
        } catch (UnsupportedSubsetException ignored) {
            if (isKnownSceneTag(tagName)) {
                throw ignored;
            }
            String rawText = extractRawNodeText(node, source);
            if (rawText != null && !rawText.trim()
                .isEmpty()) {
                SceneEditorSceneNodeModel opaqueNode = new SceneEditorSceneNodeModel(SceneEditorSceneNodeType.OPAQUE);
                opaqueNode.setOpaqueText(rawText);
                model.addSceneNode(opaqueNode);
            }
        }
    }

    private boolean appendParsedSceneChildFromRawText(SceneEditorSceneModel model, UnistNode node, String source) {
        String rawText = extractRawNodeText(node, source);
        if (rawText == null || rawText.trim()
            .isEmpty() || rawText.indexOf('<') < 0 || rawText.indexOf('>') < 0) {
            return false;
        }
        SceneEditorMarkdownParseResult nestedResult = parseSceneChildFragment(rawText);
        String rawTagName = extractLeadingTagName(rawText);
        if (nestedResult instanceof SceneEditorMarkdownParseResult.Unsupported unsupported
            && isKnownSceneTag(rawTagName)) {
            throw new UnsupportedSubsetException(unsupported.message());
        }
        if (nestedResult instanceof SceneEditorMarkdownParseResult.SyntaxError syntaxError
            && isKnownSceneTag(rawTagName)) {
            throw new InvalidSceneSyntaxException(syntaxError.message());
        }
        if (!(nestedResult instanceof SceneEditorMarkdownParseResult.Success success)) {
            return false;
        }
        mergeParsedSceneChild(model, success.model());
        return true;
    }

    private SceneEditorMarkdownParseResult parseSceneChildFragment(String rawText) {
        String trimmed = trimSceneChildRawText(rawText);
        if (trimmed.isEmpty()) {
            return new SceneEditorMarkdownParseResult.Unsupported("Empty scene child");
        }
        String wrapped = "<GameScene>\n" + trimmed + "\n</GameScene>";
        return parse(wrapped);
    }

    private String trimSceneChildRawText(String rawText) {
        String normalized = normalizeLineEndings(rawText);
        if (normalized.indexOf('\n') < 0) {
            return normalized.trim();
        }
        return trimCommonIndent(normalized);
    }

    private void mergeParsedSceneChild(SceneEditorSceneModel target, SceneEditorSceneModel parsed) {
        for (SceneEditorSceneNodeModel sceneNode : parsed.getSceneNodes()) {
            target.addSceneNode(sceneNode.duplicate());
        }
    }

    private SceneEditorElementModel parseElement(MdxJsxElementFields element, String source) {
        String tagName = element.name();
        if ("BlockAnnotation".equals(tagName)) {
            ensureAllowedAttributes(element, BLOCK_ATTRIBUTES, tagName);
            SceneEditorElementModel model = new SceneEditorElementModel(SceneEditorElementType.BLOCK);
            float[] pos = parseVectorAttribute(element, "pos", new float[] { 0f, 0f, 0f });
            model.setPrimaryX(pos[0]);
            model.setPrimaryY(pos[1]);
            model.setPrimaryZ(pos[2]);
            model.setColorLiteral(parseColorAttribute(element, model.getColorLiteral()));
            model.setThickness(parseFloatAttribute(element, "thickness", model.getThickness()));
            model.setAlwaysOnTop(parseBooleanAttribute(element, "alwaysOnTop", model.isAlwaysOnTop()));
            model.setVisible(parseBooleanAttribute(element, "visible", model.isVisible()));
            applyStructureLibConditionAttributes(model, element);
            model.setTooltipMarkdown(extractTooltipMarkdown(element, source));
            return model;
        }
        if ("BoxAnnotation".equals(tagName)) {
            ensureAllowedAttributes(element, BOX_ATTRIBUTES, tagName);
            SceneEditorElementModel model = new SceneEditorElementModel(SceneEditorElementType.BOX);
            float[] min = parseVectorAttribute(element, "min", new float[] { 0f, 0f, 0f });
            float[] max = parseVectorAttribute(element, "max", new float[] { 0f, 0f, 0f });
            normalizeBounds(min, max);
            applyPrimary(model, min);
            applySecondary(model, max);
            model.setColorLiteral(parseColorAttribute(element, model.getColorLiteral()));
            model.setThickness(parseFloatAttribute(element, "thickness", model.getThickness()));
            model.setAlwaysOnTop(parseBooleanAttribute(element, "alwaysOnTop", model.isAlwaysOnTop()));
            model.setVisible(parseBooleanAttribute(element, "visible", model.isVisible()));
            applyStructureLibConditionAttributes(model, element);
            model.setTooltipMarkdown(extractTooltipMarkdown(element, source));
            return model;
        }
        if ("LineAnnotation".equals(tagName)) {
            ensureAllowedAttributes(element, LINE_ATTRIBUTES, tagName);
            SceneEditorElementModel model = new SceneEditorElementModel(SceneEditorElementType.LINE);
            float[][] points = parseLinePointsAttribute(element);
            if (points != null) {
                applyPrimary(model, points[0]);
                applySecondary(model, points[points.length - 1]);
                model.setLinePoints(toLinePoints(points));
            } else {
                applyPrimary(model, parseVectorAttribute(element, "from", new float[] { 0f, 0f, 0f }));
                applySecondary(model, parseVectorAttribute(element, "to", new float[] { 0f, 0f, 0f }));
                model.setLinePoints(createEndpointLinePoints(model));
            }
            model.setColorLiteral(parseColorAttribute(element, model.getColorLiteral()));
            model.setThickness(parseFloatAttribute(element, "thickness", model.getThickness()));
            model.setAlwaysOnTop(parseBooleanAttribute(element, "alwaysOnTop", model.isAlwaysOnTop()));
            model.setVisible(parseBooleanAttribute(element, "visible", model.isVisible()));
            applyStructureLibConditionAttributes(model, element);
            model.setTooltipMarkdown(extractTooltipMarkdown(element, source));
            return model;
        }
        if ("DiamondAnnotation".equals(tagName)) {
            ensureAllowedAttributes(element, DIAMOND_ATTRIBUTES, tagName);
            SceneEditorElementModel model = new SceneEditorElementModel(SceneEditorElementType.DIAMOND);
            float[] pos = parseVectorAttribute(element, "pos", new float[] { 0f, 0f, 0f });
            model.setPrimaryX(pos[0]);
            model.setPrimaryY(pos[1]);
            model.setPrimaryZ(pos[2]);
            model.setColorLiteral(parseColorAttribute(element, model.getColorLiteral()));
            model.setAlwaysOnTop(parseBooleanAttribute(element, "alwaysOnTop", model.isAlwaysOnTop()));
            model.setVisible(parseBooleanAttribute(element, "visible", model.isVisible()));
            applyStructureLibConditionAttributes(model, element);
            model.setTooltipMarkdown(extractTooltipMarkdown(element, source));
            return model;
        }
        if ("TextAnnotation".equals(tagName)) {
            ensureAllowedAttributes(element, TEXT_ATTRIBUTES, tagName);
            SceneEditorElementModel model = new SceneEditorElementModel(SceneEditorElementType.TEXT);
            float[] pos = parseVectorAttribute(element, "pos", null);
            if (pos == null) {
                pos = new float[] { parseFloatAttribute(element, "x", 0f), parseFloatAttribute(element, "y", 0f),
                    parseFloatAttribute(element, "z", 0f) };
            }
            model.setPrimaryX(pos[0]);
            model.setPrimaryY(pos[1]);
            model.setPrimaryZ(pos[2]);
            model.setColorLiteral(parseColorAttribute(element, model.getColorLiteral()));
            model.setMaxWidth(parseIntAttribute(element, "maxWidth", model.getMaxWidth()));
            model.setBackgroundAlpha(parseAlphaAttribute(element, "backgroundAlpha", model.getBackgroundAlpha()));
            model.setVisible(parseBooleanAttribute(element, "visible", model.isVisible()));
            applyStructureLibConditionAttributes(model, element);
            model.setTextKey(parseOptionalStringAttribute(element, "textKey"));
            model.setTextMarkdown(parseTextAnnotationText(element, source, model.getTextMarkdown()));
            applyExtraAttributes(
                model,
                element,
                "independent",
                "yOffset",
                "connectorSide",
                "connectorOffset",
                "connectorLength",
                "hlMinX",
                "hlMinY",
                "hlMinZ",
                "hlMaxX",
                "hlMaxY",
                "hlMaxZ",
                "highlightColor");
            return model;
        }
        SceneEditorElementType registeredType = SceneEditorElementType.getByTagName(tagName);
        if (registeredType != null) {
            return parseGenericElement(element, source, registeredType);
        }
        throw new UnsupportedSubsetException("Unsupported scene element <" + tagName + ">");
    }

    private SceneEditorElementModel parseGenericElement(MdxJsxElementFields element, String source,
        SceneEditorElementType type) {
        SceneEditorElementModel model = new SceneEditorElementModel(type);
        float[] pos = parseVectorAttribute(element, "pos", new float[] { 0f, 0f, 0f });
        applyPrimary(model, pos);
        model.setColorLiteral(parseColorAttribute(element, model.getColorLiteral()));
        model.setVisible(parseBooleanAttribute(element, "visible", model.isVisible()));
        if (type.supportsAlwaysOnTop()) {
            model.setAlwaysOnTop(parseBooleanAttribute(element, "alwaysOnTop", model.isAlwaysOnTop()));
        }
        if (type.supportsThickness()) {
            model.setThickness(parseFloatAttribute(element, "thickness", model.getThickness()));
        }
        if (type.supportsMaxWidth()) {
            model.setMaxWidth(parseIntAttribute(element, "maxWidth", model.getMaxWidth()));
        }
        if (type.supportsBackgroundAlpha()) {
            model.setBackgroundAlpha(parseAlphaAttribute(element, "backgroundAlpha", model.getBackgroundAlpha()));
        }
        applyStructureLibConditionAttributes(model, element);
        if (type.supportsText()) {
            model.setTextKey(parseOptionalStringAttribute(element, "textKey"));
            model.setTextMarkdown(parseTextAnnotationText(element, source, model.getTextMarkdown()));
        } else if (type.supportsTooltip()) {
            model.setTooltipMarkdown(extractTooltipMarkdown(element, source));
        }
        return model;
    }

    private SceneEditorSceneNodeModel parseImportStructureNode(MdxJsxElementFields element) {
        ensureAllowedAttributes(element, IMPORT_STRUCTURE_ATTRIBUTES, "ImportStructure");
        String src = parseRequiredStringAttribute(element, "src");
        if (src.isEmpty()) {
            throw new InvalidSceneSyntaxException("ImportStructure src cannot be empty");
        }

        SceneEditorSceneNodeModel node = new SceneEditorSceneNodeModel(SceneEditorSceneNodeType.IMPORT_STRUCTURE);
        node.setAttribute("src", src);
        copyOptionalIntegerAttribute(element, node, "x");
        copyOptionalIntegerAttribute(element, node, "y");
        copyOptionalIntegerAttribute(element, node, "z");
        copyOptionalIntegerAttribute(element, node, "offsetX");
        copyOptionalIntegerAttribute(element, node, "offsetY");
        copyOptionalIntegerAttribute(element, node, "offsetZ");
        copyOptionalAttribute(element, node, "formed");
        return node;
    }

    private SceneEditorSceneNodeModel parseImportStructureLibNode(MdxJsxElementFields element) {
        ensureAllowedAttributes(element, IMPORT_STRUCTURE_LIB_ATTRIBUTES, "ImportStructureLib");
        String controller = parseRequiredStringAttribute(element, "controller");
        if (controller.isEmpty()) {
            throw new InvalidSceneSyntaxException("ImportStructureLib controller cannot be empty");
        }

        SceneEditorSceneNodeModel node = new SceneEditorSceneNodeModel(SceneEditorSceneNodeType.IMPORT_STRUCTURE_LIB);
        node.setAttribute("controller", controller);
        copyOptionalAttribute(element, node, "name");
        copyOptionalAttribute(element, node, "piece");
        copyOptionalAttribute(element, node, "facing");
        copyOptionalAttribute(element, node, "rotation");
        copyOptionalAttribute(element, node, "flip");
        copyOptionalIntegerAttribute(element, node, "channel");
        copyOptionalIntegerAttribute(element, node, "offsetX");
        copyOptionalIntegerAttribute(element, node, "offsetY");
        copyOptionalIntegerAttribute(element, node, "offsetZ");
        copyOptionalAttribute(element, node, "formed");
        return node;
    }

    private SceneEditorSceneNodeModel parseRemoveBlocksNode(MdxJsxElementFields element) {
        ensureAllowedAttributes(element, REMOVE_BLOCKS_ATTRIBUTES, "RemoveBlocks");
        String blockId = parseRequiredStringAttribute(element, "id");
        if (blockId.isEmpty()) {
            throw new InvalidSceneSyntaxException("RemoveBlocks id cannot be empty");
        }

        SceneEditorSceneNodeModel node = new SceneEditorSceneNodeModel(SceneEditorSceneNodeType.REMOVE_BLOCKS);
        node.setAttribute("id", blockId);
        return node;
    }

    private SceneEditorSceneNodeModel parseBlockAnnotationTemplateNode(MdxJsxElementFields element, String source) {
        ensureAllowedAttributes(element, BLOCK_ANNOTATION_TEMPLATE_ATTRIBUTES, "BlockAnnotationTemplate");
        String blockId = parseRequiredStringAttribute(element, "id");
        if (blockId.isEmpty()) {
            throw new InvalidSceneSyntaxException("BlockAnnotationTemplate id cannot be empty");
        }

        SceneEditorSceneNodeModel node = new SceneEditorSceneNodeModel(
            SceneEditorSceneNodeType.BLOCK_ANNOTATION_TEMPLATE);
        node.setAttribute("id", blockId);
        copyOptionalAttribute(element, node, "showWhenStructure");
        copyOptionalAttribute(element, node, "showWhenTier");
        copyOptionalAttribute(element, node, "showWhenChannels");

        for (Object child : element.children()) {
            if (!(child instanceof UnistNode childNode)) {
                continue;
            }
            if (isIgnorableNode(childNode, source)) {
                continue;
            }
            MdxJsxElementFields childElement = unwrapJsxElement(childNode, source);
            if (childElement == null) {
                continue;
            }
            ensureSupportedBlockAnnotationTemplateChild(childElement);
            node.addTemplateElement(parseElement(childElement, source));
        }

        return node;
    }

    private void ensureSupportedBlockAnnotationTemplateChild(MdxJsxElementFields childElement) {
        if (BlockAnnotationTemplateElementCompiler.TEMPLATE_ANNOTATION_COMPILERS.containsKey(childElement.name())) {
            return;
        }
        throw new InvalidSceneSyntaxException(
            "Unsupported BlockAnnotationTemplate child <" + childElement.name() + ">");
    }

    private void copyOptionalAttribute(MdxJsxElementFields element, SceneEditorSceneNodeModel node, String attribute) {
        String value = parseOptionalStringAttribute(element, attribute);
        if (value != null && !value.isEmpty()) {
            node.setAttribute(attribute, value);
        }
    }

    private void applyExtraAttributes(SceneEditorElementModel model, MdxJsxElementFields element,
        String... attributes) {
        if (model == null || element == null || attributes == null || attributes.length == 0) {
            return;
        }
        Map<String, String> extraAttributes = new LinkedHashMap<>();
        for (String attribute : attributes) {
            String value = parseOptionalStringAttribute(element, attribute);
            if (value != null && !value.isEmpty()) {
                extraAttributes.put(attribute, value);
            }
        }
        model.setExtraAttributes(extraAttributes);
    }

    private void applyStructureLibConditionAttributes(SceneEditorElementModel model, MdxJsxElementFields element) {
        model.setShowWhenStructure(parseOptionalStringAttribute(element, "showWhenStructure"));
        model.setShowWhenTier(parseOptionalStringAttribute(element, "showWhenTier"));
        model.setShowWhenChannels(parseOptionalStringAttribute(element, "showWhenChannels"));
    }

    private void appendRootAttributes(StringBuilder builder, SceneEditorSceneModel model) {
        if (model.getPreviewWidth() != 256) {
            builder.append(" width=\"")
                .append(model.getPreviewWidth())
                .append('"');
        }
        if (model.getPreviewHeight() != 192) {
            builder.append(" height=\"")
                .append(model.getPreviewHeight())
                .append('"');
        }
        if (model.getPerspectivePreset() != null && !model.getPerspectivePreset()
            .isEmpty()) {
            builder.append(" perspective=\"")
                .append(escapeAttribute(model.getPerspectivePreset()))
                .append('"');
        }
        appendOptionalFloatAttribute(builder, "zoom", model.getZoom(), 1f);
        appendOptionalFloatAttribute(builder, "rotateX", model.getRotationX(), 35f);
        appendOptionalFloatAttribute(builder, "rotateY", model.getRotationY(), 45f);
        appendOptionalFloatAttribute(builder, "rotateZ", model.getRotationZ(), 0f);
        appendOptionalFloatAttribute(builder, "offsetX", model.getOffsetX(), 0f);
        appendOptionalFloatAttribute(builder, "offsetY", model.getOffsetY(), 0f);
        appendOptionalFloatAttribute(builder, "centerX", model.getCenterX(), 0f);
        appendOptionalFloatAttribute(builder, "centerY", model.getCenterY(), 0f);
        appendOptionalFloatAttribute(builder, "centerZ", model.getCenterZ(), 0f);
        if (!model.isInteractive()) {
            builder.append(" interactive={false}");
        }
        if (!model.isShowBackground()) {
            builder.append(" showBackground={false}");
        }
        if (model.isAllowLayerSlider()) {
            builder.append(" allowLayerSlider={true}");
        }
    }

    private List<SceneEditorSceneNodeModel> collectSerializableSceneNodes(SceneEditorSceneModel model) {
        ArrayList<SceneEditorSceneNodeModel> sceneNodes = new ArrayList<>(model.getSceneNodes());
        if (!hasImportStructureNode(sceneNodes) && model.getStructureSource() != null
            && !model.getStructureSource()
                .isEmpty()) {
            SceneEditorSceneNodeModel importStructure = new SceneEditorSceneNodeModel(
                SceneEditorSceneNodeType.IMPORT_STRUCTURE);
            importStructure.setAttribute("src", model.getStructureSource());
            sceneNodes.add(0, importStructure);
        }

        Set<UUID> annotationIds = new HashSet<>();
        for (SceneEditorSceneNodeModel sceneNode : sceneNodes) {
            if (sceneNode.getType() == SceneEditorSceneNodeType.ANNOTATION
                && sceneNode.getAnnotationElement() != null) {
                annotationIds.add(
                    sceneNode.getAnnotationElement()
                        .getId());
            }
        }

        for (SceneEditorElementModel element : model.getElements()) {
            if (annotationIds.contains(element.getId())) {
                continue;
            }
            SceneEditorSceneNodeModel annotationNode = new SceneEditorSceneNodeModel(
                SceneEditorSceneNodeType.ANNOTATION);
            annotationNode.setAnnotationElement(element);
            sceneNodes.add(annotationNode);
        }

        return sceneNodes;
    }

    private boolean hasImportStructureNode(List<SceneEditorSceneNodeModel> sceneNodes) {
        for (SceneEditorSceneNodeModel sceneNode : sceneNodes) {
            if (sceneNode.getType() == SceneEditorSceneNodeType.IMPORT_STRUCTURE) {
                return true;
            }
        }
        return false;
    }

    private void appendSceneNode(StringBuilder builder, SceneEditorSceneNodeModel sceneNode) {
        switch (sceneNode.getType()) {
            case IMPORT_STRUCTURE -> appendImportStructureNode(builder, sceneNode);
            case IMPORT_STRUCTURE_LIB -> appendImportStructureLibNode(builder, sceneNode);
            case REMOVE_BLOCKS -> appendRemoveBlocksNode(builder, sceneNode);
            case BLOCK_ANNOTATION_TEMPLATE -> appendBlockAnnotationTemplateNode(builder, sceneNode);
            case ANNOTATION -> {
                if (sceneNode.getAnnotationElement() != null) {
                    appendElement(builder, sceneNode.getAnnotationElement(), "    ");
                }
            }
            case OPAQUE -> {
                String rawText = sceneNode.getOpaqueText();
                if (rawText != null && !rawText.isEmpty()) {
                    // Preserve indentation: emit as-is (already includes leading whitespace from source).
                    builder.append(rawText);
                    if (!rawText.endsWith("\n")) {
                        builder.append('\n');
                    }
                }
            }
        }
    }

    private void appendImportStructureNode(StringBuilder builder, SceneEditorSceneNodeModel sceneNode) {
        String src = sceneNode.getAttribute("src");
        if (src == null || src.isEmpty()) {
            return;
        }
        builder.append("    <ImportStructure src=\"")
            .append(escapeAttribute(src))
            .append('"');
        appendSceneNodeAttribute(builder, sceneNode, "x");
        appendSceneNodeAttribute(builder, sceneNode, "y");
        appendSceneNodeAttribute(builder, sceneNode, "z");
        appendSceneNodeAttribute(builder, sceneNode, "offsetX");
        appendSceneNodeAttribute(builder, sceneNode, "offsetY");
        appendSceneNodeAttribute(builder, sceneNode, "offsetZ");
        appendSceneNodeBooleanAttribute(builder, sceneNode, "formed");
        builder.append(" />\n");
    }

    private void appendImportStructureLibNode(StringBuilder builder, SceneEditorSceneNodeModel sceneNode) {
        String controller = sceneNode.getAttribute("controller");
        if (controller == null || controller.isEmpty()) {
            return;
        }
        builder.append("    <ImportStructureLib controller=\"")
            .append(escapeAttribute(controller))
            .append('"');
        appendSceneNodeAttribute(builder, sceneNode, "name");
        appendSceneNodeAttribute(builder, sceneNode, "piece");
        appendSceneNodeAttribute(builder, sceneNode, "facing");
        appendSceneNodeAttribute(builder, sceneNode, "rotation");
        appendSceneNodeAttribute(builder, sceneNode, "flip");
        appendSceneNodeAttribute(builder, sceneNode, "channel");
        appendSceneNodeAttribute(builder, sceneNode, "offsetX");
        appendSceneNodeAttribute(builder, sceneNode, "offsetY");
        appendSceneNodeAttribute(builder, sceneNode, "offsetZ");
        appendSceneNodeBooleanAttribute(builder, sceneNode, "formed");
        builder.append(" />\n");
    }

    private void appendRemoveBlocksNode(StringBuilder builder, SceneEditorSceneNodeModel sceneNode) {
        String blockId = sceneNode.getAttribute("id");
        if (blockId == null || blockId.isEmpty()) {
            return;
        }
        builder.append("    <RemoveBlocks id=\"")
            .append(escapeAttribute(blockId))
            .append("\" />\n");
    }

    private void appendBlockAnnotationTemplateNode(StringBuilder builder, SceneEditorSceneNodeModel sceneNode) {
        String blockId = sceneNode.getAttribute("id");
        if (blockId == null || blockId.isEmpty()) {
            return;
        }

        if (sceneNode.getTemplateElements()
            .isEmpty()) {
            builder.append("    <BlockAnnotationTemplate id=\"")
                .append(escapeAttribute(blockId))
                .append('"');
            appendSceneNodeAttribute(builder, sceneNode, "showWhenStructure");
            appendSceneNodeAttribute(builder, sceneNode, "showWhenTier");
            appendSceneNodeAttribute(builder, sceneNode, "showWhenChannels");
            builder.append(" />\n");
            return;
        }

        builder.append("    <BlockAnnotationTemplate id=\"")
            .append(escapeAttribute(blockId))
            .append('"');
        appendSceneNodeAttribute(builder, sceneNode, "showWhenStructure");
        appendSceneNodeAttribute(builder, sceneNode, "showWhenTier");
        appendSceneNodeAttribute(builder, sceneNode, "showWhenChannels");
        builder.append(">\n");
        for (SceneEditorElementModel templateElement : sceneNode.getTemplateElements()) {
            appendElement(builder, templateElement, "        ");
        }
        builder.append("    </BlockAnnotationTemplate>\n");
    }

    private void appendSceneNodeAttribute(StringBuilder builder, SceneEditorSceneNodeModel sceneNode,
        String attribute) {
        String value = sceneNode.getAttribute(attribute);
        if (value == null || value.isEmpty()) {
            return;
        }
        builder.append(' ')
            .append(attribute)
            .append("=\"")
            .append(escapeAttribute(value))
            .append('"');
    }

    private void appendSceneNodeBooleanAttribute(StringBuilder builder, SceneEditorSceneNodeModel sceneNode,
        String attribute) {
        String value = sceneNode.getAttribute(attribute);
        if (value == null || value.isEmpty()) {
            return;
        }
        builder.append(' ')
            .append(attribute)
            .append("={")
            .append(value)
            .append('}');
    }

    private void appendElement(StringBuilder builder, SceneEditorElementModel element) {
        appendElement(builder, element, "    ");
    }

    private void appendElement(StringBuilder builder, SceneEditorElementModel element, String indent) {
        if (element.getType() == SceneEditorElementType.BLOCK) {
            appendBlockElement(builder, element, indent);
            return;
        }
        if (element.getType() == SceneEditorElementType.BOX) {
            appendBoxElement(builder, element, indent);
            return;
        }
        if (element.getType() == SceneEditorElementType.LINE) {
            appendLineElement(builder, element, indent);
            return;
        }
        if (element.getType() == SceneEditorElementType.DIAMOND) {
            appendDiamondElement(builder, element, indent);
            return;
        }
        if (element.getType() == SceneEditorElementType.TEXT) {
            appendTextElement(builder, element, indent);
            return;
        }
        appendGenericElement(builder, element, indent);
    }

    private void appendBlockElement(StringBuilder builder, SceneEditorElementModel element, String indent) {
        StringBuilder tagBuilder = new StringBuilder();
        tagBuilder.append("<BlockAnnotation pos=\"")
            .append(formatVector(element.getPrimaryX(), element.getPrimaryY(), element.getPrimaryZ()))
            .append('"');
        appendElementStyleAttributes(tagBuilder, element, "#FFFFFFFF", true);
        appendElementTooltip(builder, indent, "BlockAnnotation", tagBuilder, element.getTooltipMarkdown());
    }

    private void appendBoxElement(StringBuilder builder, SceneEditorElementModel element, String indent) {
        StringBuilder tagBuilder = new StringBuilder();
        tagBuilder.append("<BoxAnnotation min=\"")
            .append(formatVector(element.getPrimaryX(), element.getPrimaryY(), element.getPrimaryZ()))
            .append("\" max=\"")
            .append(formatVector(element.getSecondaryX(), element.getSecondaryY(), element.getSecondaryZ()))
            .append('"');
        appendElementStyleAttributes(tagBuilder, element, "#FFFFFFFF", true);
        appendElementTooltip(builder, indent, "BoxAnnotation", tagBuilder, element.getTooltipMarkdown());
    }

    private void appendLineElement(StringBuilder builder, SceneEditorElementModel element, String indent) {
        StringBuilder tagBuilder = new StringBuilder();
        List<Vector3f> linePoints = normalizeLinePoints(element);
        if (linePoints.size() > 2) {
            tagBuilder.append("<LineAnnotation points=\"")
                .append(formatLinePoints(linePoints))
                .append('"');
        } else {
            tagBuilder.append("<LineAnnotation from=\"")
                .append(formatVector(element.getPrimaryX(), element.getPrimaryY(), element.getPrimaryZ()))
                .append("\" to=\"")
                .append(formatVector(element.getSecondaryX(), element.getSecondaryY(), element.getSecondaryZ()))
                .append('"');
        }
        appendElementStyleAttributes(tagBuilder, element, "#FFFFFFFF", true);
        appendElementTooltip(builder, indent, "LineAnnotation", tagBuilder, element.getTooltipMarkdown());
    }

    private void appendDiamondElement(StringBuilder builder, SceneEditorElementModel element, String indent) {
        StringBuilder tagBuilder = new StringBuilder();
        tagBuilder.append("<DiamondAnnotation pos=\"")
            .append(formatVector(element.getPrimaryX(), element.getPrimaryY(), element.getPrimaryZ()))
            .append('"');
        appendElementStyleAttributes(tagBuilder, element, "#FF00E000", false);
        appendElementTooltip(builder, indent, "DiamondAnnotation", tagBuilder, element.getTooltipMarkdown());
    }

    private void appendTextElement(StringBuilder builder, SceneEditorElementModel element, String indent) {
        StringBuilder tagBuilder = new StringBuilder();
        tagBuilder.append("<TextAnnotation pos=\"")
            .append(formatVector(element.getPrimaryX(), element.getPrimaryY(), element.getPrimaryZ()))
            .append('"');
        if (!"#FFFFFFFF".equalsIgnoreCase(element.getColorLiteral())) {
            tagBuilder.append(" color=\"")
                .append(escapeAttribute(element.getColorLiteral()))
                .append('"');
        }
        if (element.getMaxWidth() > 0) {
            tagBuilder.append(" maxWidth=\"")
                .append(element.getMaxWidth())
                .append('"');
        }
        if (element.getBackgroundAlpha() != element.getType()
            .getDefaultBackgroundAlpha()) {
            tagBuilder.append(" backgroundAlpha=\"")
                .append(element.getBackgroundAlpha())
                .append('"');
        }
        if (!element.getTextKey()
            .isEmpty()) {
            tagBuilder.append(" textKey=\"")
                .append(escapeAttribute(element.getTextKey()))
                .append('"');
        }
        if (!element.isVisible()) {
            tagBuilder.append(" visible={false}");
        }
        appendExtraAttributes(
            tagBuilder,
            element,
            "independent",
            "yOffset",
            "connectorSide",
            "connectorOffset",
            "connectorLength",
            "hlMinX",
            "hlMinY",
            "hlMinZ",
            "hlMaxX",
            "hlMaxY",
            "hlMaxZ",
            "highlightColor");
        appendStructureLibConditionAttributes(tagBuilder, element);
        appendTextElementBody(builder, indent, tagBuilder, element.getTextMarkdown());
    }

    private void appendGenericElement(StringBuilder builder, SceneEditorElementModel element, String indent) {
        StringBuilder tagBuilder = new StringBuilder();
        tagBuilder.append('<')
            .append(
                element.getType()
                    .getTagName());
        if (element.getType()
            .supportsPrimaryVector()) {
            tagBuilder.append(" pos=\"")
                .append(formatVector(element.getPrimaryX(), element.getPrimaryY(), element.getPrimaryZ()))
                .append('"');
        }
        appendElementStyleAttributes(
            tagBuilder,
            element,
            element.getType()
                .getDefaultColorLiteral(),
            element.getType()
                .supportsThickness());
        if (element.getType()
            .supportsMaxWidth() && element.getMaxWidth() > 0) {
            tagBuilder.append(" maxWidth=\"")
                .append(element.getMaxWidth())
                .append('"');
        }
        if (element.getType()
            .supportsBackgroundAlpha()
            && element.getBackgroundAlpha() != element.getType()
                .getDefaultBackgroundAlpha()) {
            tagBuilder.append(" backgroundAlpha=\"")
                .append(element.getBackgroundAlpha())
                .append('"');
        }
        if (element.getType()
            .supportsText()
            && !element.getTextKey()
                .isEmpty()) {
            tagBuilder.append(" textKey=\"")
                .append(escapeAttribute(element.getTextKey()))
                .append('"');
        }
        appendStructureLibConditionAttributes(tagBuilder, element);
        if (element.getType()
            .supportsText()) {
            appendTextElementBody(
                builder,
                indent,
                tagBuilder,
                element.getTextMarkdown(),
                element.getType()
                    .getTagName());
            return;
        }
        appendElementTooltip(
            builder,
            indent,
            element.getType()
                .getTagName(),
            tagBuilder,
            element.getTooltipMarkdown());
    }

    private void appendElementStyleAttributes(StringBuilder builder, SceneEditorElementModel element,
        String defaultColor, boolean includeThickness) {
        if (!defaultColor.equalsIgnoreCase(element.getColorLiteral())) {
            builder.append(" color=\"")
                .append(escapeAttribute(element.getColorLiteral()))
                .append('"');
        }
        if (includeThickness && !isNear(element.getThickness(), 1f)) {
            builder.append(" thickness={")
                .append(formatNumber(element.getThickness()))
                .append('}');
        }
        if (element.isAlwaysOnTop()) {
            builder.append(" alwaysOnTop={true}");
        }
        if (!element.isVisible()) {
            builder.append(" visible={false}");
        }
    }

    private void appendStructureLibConditionAttributes(StringBuilder builder, SceneEditorElementModel element) {
        appendOptionalStringAttribute(builder, "showWhenStructure", element.getShowWhenStructure());
        appendOptionalStringAttribute(builder, "showWhenTier", element.getShowWhenTier());
        appendOptionalStringAttribute(builder, "showWhenChannels", element.getShowWhenChannels());
    }

    private void appendOptionalStringAttribute(StringBuilder builder, String attribute, String value) {
        if (value == null || value.isEmpty()) {
            return;
        }
        builder.append(' ')
            .append(attribute)
            .append("=\"")
            .append(escapeAttribute(value))
            .append('"');
    }

    private void appendExtraAttributes(StringBuilder builder, SceneEditorElementModel element, String... attributes) {
        if (builder == null || element == null || attributes == null || attributes.length == 0) {
            return;
        }
        for (String attribute : attributes) {
            appendOptionalStringAttribute(builder, attribute, element.getExtraAttribute(attribute));
        }
    }

    private void appendElementTooltip(StringBuilder builder, String indent, String tagName, StringBuilder openingTag,
        String tooltipMarkdown) {
        if (tooltipMarkdown == null || tooltipMarkdown.isEmpty()) {
            builder.append(indent)
                .append(openingTag)
                .append(" />\n");
            return;
        }

        builder.append(indent)
            .append(openingTag)
            .append(">\n");
        appendIndentedTooltip(builder, indent + "    ", tooltipMarkdown);
        if (!tooltipMarkdown.endsWith("\n")) {
            builder.append('\n');
        }
        builder.append(indent)
            .append("</")
            .append(tagName)
            .append(">\n");
    }

    private void appendTextElementBody(StringBuilder builder, String indent, StringBuilder openingTag,
        String textMarkdown) {
        appendTextElementBody(builder, indent, openingTag, textMarkdown, "TextAnnotation");
    }

    private void appendTextElementBody(StringBuilder builder, String indent, StringBuilder openingTag,
        String textMarkdown, String tagName) {
        if (textMarkdown == null || textMarkdown.isEmpty()) {
            builder.append(indent)
                .append(openingTag)
                .append(" text=\"\" />\n");
            return;
        }

        builder.append(indent)
            .append(openingTag)
            .append(">\n");
        appendIndentedTooltip(builder, indent + "    ", textMarkdown);
        if (!textMarkdown.endsWith("\n")) {
            builder.append('\n');
        }
        builder.append(indent)
            .append("</")
            .append(tagName)
            .append(">\n");
    }

    private void appendIndentedTooltip(StringBuilder builder, String indent, String tooltipMarkdown) {
        String normalizedTooltip = normalizeLineEndings(tooltipMarkdown);
        if (normalizedTooltip.isEmpty()) {
            return;
        }
        List<String> lines = GuideStringLines.splitLines(normalizedTooltip);
        for (int i = 0; i < lines.size(); i++) {
            builder.append(indent)
                .append(lines.get(i));
            if (i < lines.size() - 1) {
                builder.append('\n');
            }
        }
    }

    private void ensureAllowedAttributes(MdxJsxElementFields element, Set<String> allowedAttributes, String tagName) {
        for (MdxJsxAttributeNode attributeNode : element.attributes()) {
            if (!(attributeNode instanceof MdxJsxAttribute attribute)) {
                throw new UnsupportedSubsetException("Spread attributes are not supported on <" + tagName + ">");
            }
            String attributeName = attribute.name;
            if (attributeName == null || !allowedAttributes.contains(attributeName)) {
                throw new UnsupportedSubsetException(
                    "Attribute '" + attributeName + "' is not supported on <" + tagName + ">");
            }
        }
    }

    private boolean isKnownSceneTag(@Nullable String tagName) {
        if (tagName == null) {
            return false;
        }
        return "ImportStructure".equals(tagName) || "ImportStructureLib".equals(tagName)
            || "RemoveBlocks".equals(tagName)
            || "BlockAnnotationTemplate".equals(tagName)
            || "BlockAnnotation".equals(tagName)
            || "BoxAnnotation".equals(tagName)
            || "LineAnnotation".equals(tagName)
            || "DiamondAnnotation".equals(tagName)
            || "TextAnnotation".equals(tagName);
    }

    @Nullable
    private String extractLeadingTagName(String rawText) {
        String trimmed = trimSceneChildRawText(rawText);
        if (trimmed.isEmpty() || trimmed.charAt(0) != '<') {
            return null;
        }
        int start = 1;
        while (start < trimmed.length() && Character.isWhitespace(trimmed.charAt(start))) {
            start++;
        }
        if (start >= trimmed.length() || trimmed.charAt(start) == '/'
            || trimmed.charAt(start) == '!'
            || trimmed.charAt(start) == '?') {
            return null;
        }
        int end = start;
        while (end < trimmed.length()) {
            char ch = trimmed.charAt(end);
            if (Character.isWhitespace(ch) || ch == '>' || ch == '/') {
                break;
            }
            end++;
        }
        return end > start ? trimmed.substring(start, end) : null;
    }

    @Nullable
    private String parseOptionalStringAttribute(MdxJsxElementFields element, String name) {
        MdxJsxAttribute attribute = element.getAttribute(name);
        if (attribute == null) {
            return null;
        }
        String value = getAttributeValue(attribute, name);
        return value.isEmpty() ? null : value;
    }

    private String parseRequiredStringAttribute(MdxJsxElementFields element, String name) {
        MdxJsxAttribute attribute = element.getAttribute(name);
        if (attribute == null) {
            throw new InvalidSceneSyntaxException("Missing required attribute '" + name + "'");
        }
        return getAttributeValue(attribute, name);
    }

    private int parseIntAttribute(MdxJsxElementFields element, String name, int defaultValue) {
        String rawValue = getOptionalAttributeValue(element, name);
        if (rawValue == null) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(rawValue.trim());
        } catch (NumberFormatException e) {
            throw new InvalidSceneSyntaxException("Attribute '" + name + "' must be an integer");
        }
    }

    private int parseAlphaAttribute(MdxJsxElementFields element, String name, int defaultValue) {
        return Math.max(0, Math.min(255, parseIntAttribute(element, name, defaultValue)));
    }

    private void copyOptionalIntegerAttribute(MdxJsxElementFields element, SceneEditorSceneNodeModel node,
        String name) {
        String rawValue = getOptionalAttributeValue(element, name);
        if (rawValue == null) {
            return;
        }
        int parsed = parseIntAttribute(element, name, Integer.MIN_VALUE);
        if (parsed != Integer.MIN_VALUE) {
            node.setAttribute(name, Integer.toString(parsed));
        }
    }

    private float parseFloatAttribute(MdxJsxElementFields element, String name, float defaultValue) {
        String rawValue = getOptionalAttributeValue(element, name);
        if (rawValue == null) {
            return defaultValue;
        }
        try {
            return Float.parseFloat(rawValue.trim());
        } catch (NumberFormatException e) {
            throw new InvalidSceneSyntaxException("Attribute '" + name + "' must be a number");
        }
    }

    private float parseOptionalFloatAttribute(MdxJsxElementFields element, String name) {
        String rawValue = getOptionalAttributeValue(element, name);
        if (rawValue == null) {
            return Float.NaN;
        }
        try {
            return Float.parseFloat(rawValue.trim());
        } catch (NumberFormatException e) {
            throw new InvalidSceneSyntaxException("Attribute '" + name + "' must be a number");
        }
    }

    private boolean parseBooleanAttribute(MdxJsxElementFields element, String name, boolean defaultValue) {
        String rawValue = getOptionalAttributeValue(element, name);
        if (rawValue == null) {
            return defaultValue;
        }
        if ("true".equalsIgnoreCase(rawValue.trim())) {
            return true;
        }
        if ("false".equalsIgnoreCase(rawValue.trim())) {
            return false;
        }
        throw new InvalidSceneSyntaxException("Attribute '" + name + "' must be true or false");
    }

    private String parseColorAttribute(MdxJsxElementFields element, String defaultValue) {
        String rawValue = getOptionalAttributeValue(element, "color");
        if (rawValue == null) {
            return defaultValue;
        }

        String normalized = rawValue.trim();
        if ("transparent".equalsIgnoreCase(normalized)) {
            return "transparent";
        }
        if (normalized.matches("#(?i:[0-9a-f]{6}|[0-9a-f]{8})")) {
            return normalized.toUpperCase(Locale.ROOT);
        }
        throw new InvalidSceneSyntaxException("Attribute 'color' must be #RRGGBB, #AARRGGBB, or transparent");
    }

    private float[] parseVectorAttribute(MdxJsxElementFields element, String name, float[] defaultValue) {
        String rawValue = getOptionalAttributeValue(element, name);
        if (rawValue == null) {
            if (defaultValue == null) {
                return null;
            }
            return Arrays.copyOf(defaultValue, defaultValue.length);
        }

        float[] pieces = MdxAttrs.parseVector3Parts(rawValue);
        if (pieces == null) {
            throw new InvalidSceneSyntaxException("Attribute '" + name + "' must contain exactly 3 numbers");
        }
        return pieces;
    }

    private float[][] parseLinePointsAttribute(MdxJsxElementFields element) {
        String rawValue = getOptionalAttributeValue(element, "points");
        if (rawValue == null || rawValue.trim()
            .isEmpty()) {
            return null;
        }
        String[] tokens = rawValue.split(";");
        List<float[]> points = new ArrayList<>();
        for (String token : tokens) {
            String trimmed = token.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            float[] point = MdxAttrs.parseVector3Parts(trimmed);
            if (point == null) {
                throw new InvalidSceneSyntaxException("Attribute 'points' must contain semicolon-separated 3D vectors");
            }
            points.add(point);
        }
        if (points.size() < 2) {
            throw new InvalidSceneSyntaxException("Attribute 'points' must contain at least two points");
        }
        return points.toArray(new float[0][]);
    }

    @Nullable
    private String getOptionalAttributeValue(MdxJsxElementFields element, String name) {
        MdxJsxAttribute attribute = element.getAttribute(name);
        if (attribute == null) {
            return null;
        }
        return getAttributeValue(attribute, name);
    }

    private String getAttributeValue(MdxJsxAttribute attribute, String name) {
        if (attribute.hasStringValue()) {
            return attribute.getStringValue();
        }
        if (attribute.hasExpressionValue()) {
            return attribute.getExpressionValue();
        }
        throw new InvalidSceneSyntaxException("Attribute '" + name + "' is missing a value");
    }

    private String extractTooltipMarkdown(MdxJsxElementFields element, String source) {
        if (element.children() == null || element.children()
            .isEmpty()) {
            return "";
        }
        UnistPosition position = element.position();
        if (position == null || position.start() == null || position.end() == null) {
            return "";
        }

        int startOffset = position.start()
            .offset();
        int endOffset = position.end()
            .offset();
        if (startOffset < 0 || endOffset < startOffset || endOffset > source.length()) {
            return "";
        }

        String rawElement = source.substring(startOffset, endOffset);
        if (rawElement.endsWith("/>")) {
            return "";
        }

        int openingEnd = findOpeningTagEnd(rawElement);
        int closingStart = rawElement.lastIndexOf("</");
        if (openingEnd == -1 || closingStart == -1 || closingStart < openingEnd + 1) {
            return "";
        }
        return rawElement.substring(openingEnd + 1, closingStart);
    }

    private String parseTextAnnotationText(MdxJsxElementFields element, String source, String defaultValue) {
        String textAttribute = parseOptionalStringAttribute(element, "text");
        if (textAttribute != null) {
            return textAttribute;
        }
        String body = extractTooltipMarkdown(element, source);
        if (body == null || body.trim()
            .isEmpty()) {
            return defaultValue;
        }
        return trimCommonIndent(body);
    }

    private String trimCommonIndent(String text) {
        String normalized = normalizeLineEndings(text);
        List<String> lines = GuideStringLines.splitLines(normalized);
        int start = 0;
        int end = lines.size();
        while (start < end && lines.get(start)
            .trim()
            .isEmpty()) {
            start++;
        }
        while (end > start && lines.get(end - 1)
            .trim()
            .isEmpty()) {
            end--;
        }
        int indent = Integer.MAX_VALUE;
        for (int i = start; i < end; i++) {
            if (lines.get(i)
                .trim()
                .isEmpty()) {
                continue;
            }
            indent = Math.min(indent, countLeadingSpaces(lines.get(i)));
        }
        if (indent == Integer.MAX_VALUE) {
            indent = 0;
        }
        StringBuilder builder = new StringBuilder();
        for (int i = start; i < end; i++) {
            if (i > start) {
                builder.append('\n');
            }
            String line = lines.get(i);
            builder.append(line.substring(Math.min(indent, line.length())));
        }
        return builder.toString();
    }

    private int countLeadingSpaces(String line) {
        int count = 0;
        while (count < line.length() && line.charAt(count) == ' ') {
            count++;
        }
        return count;
    }

    private int findOpeningTagEnd(String rawElement) {
        boolean inSingleQuote = false;
        boolean inDoubleQuote = false;
        int braceDepth = 0;
        for (int i = 0; i < rawElement.length(); i++) {
            char ch = rawElement.charAt(i);
            if (ch == '\'' && !inDoubleQuote) {
                inSingleQuote = !inSingleQuote;
                continue;
            }
            if (ch == '"' && !inSingleQuote) {
                inDoubleQuote = !inDoubleQuote;
                continue;
            }
            if (inSingleQuote || inDoubleQuote) {
                continue;
            }
            if (ch == '{') {
                braceDepth++;
                continue;
            }
            if (ch == '}') {
                braceDepth = Math.max(0, braceDepth - 1);
                continue;
            }
            if (ch == '>' && braceDepth == 0) {
                return i;
            }
        }
        return -1;
    }

    @Nullable
    private String extractRawNodeText(UnistNode node, String source) {
        UnistPosition position = node.position();
        if (position == null || position.start() == null || position.end() == null) {
            return null;
        }
        int startOffset = position.start()
            .offset();
        int endOffset = position.end()
            .offset();
        if (startOffset < 0 || endOffset < startOffset || endOffset > source.length()) {
            return null;
        }
        return source.substring(startOffset, endOffset);
    }

    private boolean isIgnorableNode(UnistNode node, String source) {
        if (node instanceof MdxJsxElementFields || node instanceof MdAstHTML) {
            return false;
        }
        UnistPosition position = node.position();
        if (position == null || position.start() == null || position.end() == null) {
            return false;
        }

        int startOffset = position.start()
            .offset();
        int endOffset = position.end()
            .offset();
        if (startOffset < 0 || endOffset < startOffset || endOffset > source.length()) {
            return false;
        }
        return source.substring(startOffset, endOffset)
            .trim()
            .isEmpty();
    }

    @Nullable
    private MdxJsxElementFields unwrapJsxElement(UnistNode node, String source) {
        if (node instanceof MdxJsxElementFields element) {
            return element;
        }
        if (!(node instanceof UnistParent parent)) {
            return null;
        }

        MdxJsxElementFields found = null;
        for (UnistNode child : parent.children()) {
            if (isIgnorableNode(child, source)) {
                continue;
            }
            MdxJsxElementFields nested = unwrapJsxElement(child, source);
            if (nested == null) {
                return null;
            }
            if (found != null) {
                return null;
            }
            found = nested;
        }
        return found;
    }

    private void appendFloatAttribute(StringBuilder builder, String name, float value, float defaultValue) {
        if (!isNear(value, defaultValue)) {
            builder.append(' ')
                .append(name)
                .append("={")
                .append(formatNumber(value))
                .append('}');
        }
    }

    private void appendOptionalFloatAttribute(StringBuilder builder, String name, float value, float defaultValue) {
        if (Float.isNaN(value)) {
            return;
        }
        appendFloatAttribute(builder, name, value, defaultValue);
    }

    private String formatVector(float x, float y, float z) {
        return formatNumber(x) + ' ' + formatNumber(y) + ' ' + formatNumber(z);
    }

    private String formatLinePoints(List<Vector3f> points) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < points.size(); i++) {
            if (i > 0) {
                builder.append("; ");
            }
            Vector3f point = points.get(i);
            builder.append(formatVector(point.x, point.y, point.z));
        }
        return builder.toString();
    }

    private String formatNumber(float value) {
        if (Float.isNaN(value) || Float.isInfinite(value)) {
            return "0";
        }
        int rounded = Math.round(value);
        if (isNear(value, rounded)) {
            return Integer.toString(rounded);
        }
        return Float.toString(value);
    }

    private boolean isNear(float left, float right) {
        return Math.abs(left - right) < 0.0001f;
    }

    private void normalizeBounds(float[] min, float[] max) {
        for (int i = 0; i < 3; i++) {
            if (min[i] > max[i]) {
                float swap = min[i];
                min[i] = max[i];
                max[i] = swap;
            }
        }
    }

    private List<Vector3f> toLinePoints(float[][] points) {
        List<Vector3f> result = new ArrayList<>(points.length);
        for (float[] point : points) {
            result.add(new Vector3f(point[0], point[1], point[2]));
        }
        return result;
    }

    private List<Vector3f> createEndpointLinePoints(SceneEditorElementModel model) {
        List<Vector3f> result = new ArrayList<>(2);
        result.add(new Vector3f(model.getPrimaryX(), model.getPrimaryY(), model.getPrimaryZ()));
        result.add(new Vector3f(model.getSecondaryX(), model.getSecondaryY(), model.getSecondaryZ()));
        return result;
    }

    private List<Vector3f> normalizeLinePoints(SceneEditorElementModel element) {
        List<Vector3f> linePoints = element.getLinePoints();
        if (linePoints.size() >= 2) {
            return linePoints;
        }
        return createEndpointLinePoints(element);
    }

    private void applyPrimary(SceneEditorElementModel model, float[] values) {
        model.setPrimaryX(values[0]);
        model.setPrimaryY(values[1]);
        model.setPrimaryZ(values[2]);
    }

    private void applySecondary(SceneEditorElementModel model, float[] values) {
        model.setSecondaryX(values[0]);
        model.setSecondaryY(values[1]);
        model.setSecondaryZ(values[2]);
    }

    private String escapeAttribute(String value) {
        return value.replace("&", "&amp;")
            .replace("\"", "&quot;");
    }

    private String normalizeLineEndings(String markdown) {
        return GuideStringLines.normalizeLineEndings(markdown);
    }

    private String formatParseException(ParseException exception) {
        if (exception.getFrom() == null) {
            return exception.getMessage();
        }
        return exception.getMessage() + " (line "
            + exception.getFrom()
                .line()
            + ", column "
            + exception.getFrom()
                .column()
            + ")";
    }

    public static class UnsupportedSubsetException extends RuntimeException {

        private UnsupportedSubsetException(String message) {
            super(message);
        }
    }

    public static class InvalidSceneSyntaxException extends RuntimeException {

        private InvalidSceneSyntaxException(String message) {
            super(message);
        }
    }
}
