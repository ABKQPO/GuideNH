package com.hfstudio.guidenh.guide.internal.editor.md;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import javax.annotation.Nullable;

import com.hfstudio.guidenh.guide.internal.editor.model.SceneEditorElementModel;
import com.hfstudio.guidenh.guide.internal.editor.model.SceneEditorElementType;
import com.hfstudio.guidenh.guide.internal.editor.model.SceneEditorSceneModel;
import com.hfstudio.guidenh.libs.mdast.MdAst;
import com.hfstudio.guidenh.libs.mdast.MdastOptions;
import com.hfstudio.guidenh.libs.mdast.YamlFrontmatterExtension;
import com.hfstudio.guidenh.libs.mdast.gfm.GfmTableMdastExtension;
import com.hfstudio.guidenh.libs.mdast.gfmstrikethrough.GfmStrikethroughMdastExtension;
import com.hfstudio.guidenh.libs.mdast.mdx.MdxMdastExtension;
import com.hfstudio.guidenh.libs.mdast.mdx.model.MdxJsxAttribute;
import com.hfstudio.guidenh.libs.mdast.mdx.model.MdxJsxAttributeNode;
import com.hfstudio.guidenh.libs.mdast.mdx.model.MdxJsxElementFields;
import com.hfstudio.guidenh.libs.mdast.model.MdAstRoot;
import com.hfstudio.guidenh.libs.mdx.MdxSyntax;
import com.hfstudio.guidenh.libs.micromark.ParseException;
import com.hfstudio.guidenh.libs.micromark.extensions.YamlFrontmatterSyntax;
import com.hfstudio.guidenh.libs.micromark.extensions.gfm.GfmTableSyntax;
import com.hfstudio.guidenh.libs.micromark.extensions.gfmstrikethrough.GfmStrikethroughSyntax;
import com.hfstudio.guidenh.libs.unist.UnistNode;
import com.hfstudio.guidenh.libs.unist.UnistPosition;

public final class SceneEditorMarkdownCodec {

    private static final MdastOptions PARSE_OPTIONS = new MdastOptions().withSyntaxExtension(MdxSyntax.INSTANCE)
        .withSyntaxExtension(YamlFrontmatterSyntax.INSTANCE)
        .withSyntaxExtension(GfmTableSyntax.INSTANCE)
        .withSyntaxExtension(GfmStrikethroughSyntax.INSTANCE)
        .withMdastExtension(MdxMdastExtension.INSTANCE)
        .withMdastExtension(YamlFrontmatterExtension.INSTANCE)
        .withMdastExtension(GfmTableMdastExtension.INSTANCE)
        .withMdastExtension(GfmStrikethroughMdastExtension.INSTANCE);

    private static final Set<String> ROOT_TAG_NAMES = Collections
        .unmodifiableSet(new HashSet<>(Arrays.asList("GameScene", "Scene")));
    private static final Set<String> ROOT_ATTRIBUTES = Collections.unmodifiableSet(
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
                "interactive")));
    private static final Set<String> IMPORT_STRUCTURE_ATTRIBUTES = Collections
        .unmodifiableSet(new HashSet<>(Collections.singletonList("src")));
    private static final Set<String> BLOCK_ATTRIBUTES = Collections
        .unmodifiableSet(new HashSet<>(Arrays.asList("pos", "color", "thickness", "alwaysOnTop", "visible")));
    private static final Set<String> BOX_ATTRIBUTES = Collections
        .unmodifiableSet(new HashSet<>(Arrays.asList("min", "max", "color", "thickness", "alwaysOnTop", "visible")));
    private static final Set<String> LINE_ATTRIBUTES = Collections
        .unmodifiableSet(new HashSet<>(Arrays.asList("from", "to", "color", "thickness", "alwaysOnTop", "visible")));
    private static final Set<String> DIAMOND_ATTRIBUTES = Collections
        .unmodifiableSet(new HashSet<>(Arrays.asList("pos", "color", "alwaysOnTop", "visible")));

    public SceneEditorMarkdownParseResult parse(String markdown) {
        String normalized = normalizeLineEndings(markdown);

        MdAstRoot root;
        try {
            root = MdAst.fromMarkdown(normalized, PARSE_OPTIONS);
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

        boolean hasImportStructure = model.getStructureSource() != null && !model.getStructureSource()
            .isEmpty();
        boolean hasElements = !model.getElements()
            .isEmpty();
        if (!hasImportStructure && !hasElements) {
            builder.append(" />");
            return builder.toString();
        }

        builder.append(">\n");
        if (hasImportStructure) {
            builder.append("    <ImportStructure src=\"")
                .append(escapeAttribute(model.getStructureSource()))
                .append("\" />\n");
        }
        for (SceneEditorElementModel element : model.getElements()) {
            appendElement(builder, element);
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
        model.setZoom(parseFloatAttribute(sceneElement, "zoom", model.getZoom()));
        model.setRotationX(parseFloatAttribute(sceneElement, "rotateX", model.getRotationX()));
        model.setRotationY(parseFloatAttribute(sceneElement, "rotateY", model.getRotationY()));
        model.setRotationZ(parseFloatAttribute(sceneElement, "rotateZ", model.getRotationZ()));
        model.setOffsetX(parseFloatAttribute(sceneElement, "offsetX", model.getOffsetX()));
        model.setOffsetY(parseFloatAttribute(sceneElement, "offsetY", model.getOffsetY()));
        model.setCenterX(parseFloatAttribute(sceneElement, "centerX", model.getCenterX()));
        model.setCenterY(parseFloatAttribute(sceneElement, "centerY", model.getCenterY()));
        model.setCenterZ(parseFloatAttribute(sceneElement, "centerZ", model.getCenterZ()));
        model.setInteractive(parseBooleanAttribute(sceneElement, "interactive", model.isInteractive()));

        boolean importStructureSeen = false;
        for (Object child : sceneElement.children()) {
            if (!(child instanceof UnistNode node)) {
                continue;
            }
            if (isIgnorableNode(node, source)) {
                continue;
            }
            if (!(child instanceof MdxJsxElementFields element)) {
                throw new UnsupportedSubsetException("Unsupported content inside <GameScene>");
            }
            String tagName = element.name();
            if ("ImportStructure".equals(tagName)) {
                if (importStructureSeen) {
                    throw new UnsupportedSubsetException("Only one <ImportStructure> is supported");
                }
                ensureAllowedAttributes(element, IMPORT_STRUCTURE_ATTRIBUTES, tagName);
                String src = parseRequiredStringAttribute(element, "src");
                if (src.isEmpty()) {
                    throw new InvalidSceneSyntaxException("ImportStructure src cannot be empty");
                }
                model.setStructureSource(src);
                importStructureSeen = true;
                continue;
            }

            model.addElement(parseElement(element, source));
        }

        return model;
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
            model.setTooltipMarkdown(extractTooltipMarkdown(element, source));
            return model;
        }
        if ("LineAnnotation".equals(tagName)) {
            ensureAllowedAttributes(element, LINE_ATTRIBUTES, tagName);
            SceneEditorElementModel model = new SceneEditorElementModel(SceneEditorElementType.LINE);
            applyPrimary(model, parseVectorAttribute(element, "from", new float[] { 0f, 0f, 0f }));
            applySecondary(model, parseVectorAttribute(element, "to", new float[] { 0f, 0f, 0f }));
            model.setColorLiteral(parseColorAttribute(element, model.getColorLiteral()));
            model.setThickness(parseFloatAttribute(element, "thickness", model.getThickness()));
            model.setAlwaysOnTop(parseBooleanAttribute(element, "alwaysOnTop", model.isAlwaysOnTop()));
            model.setVisible(parseBooleanAttribute(element, "visible", model.isVisible()));
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
            model.setTooltipMarkdown(extractTooltipMarkdown(element, source));
            return model;
        }
        throw new UnsupportedSubsetException("Unsupported scene element <" + tagName + ">");
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
        appendFloatAttribute(builder, "zoom", model.getZoom(), 1f);
        appendFloatAttribute(builder, "rotateX", model.getRotationX(), 35f);
        appendFloatAttribute(builder, "rotateY", model.getRotationY(), 45f);
        appendFloatAttribute(builder, "rotateZ", model.getRotationZ(), 0f);
        appendFloatAttribute(builder, "offsetX", model.getOffsetX(), 0f);
        appendFloatAttribute(builder, "offsetY", model.getOffsetY(), 0f);
        appendFloatAttribute(builder, "centerX", model.getCenterX(), 0f);
        appendFloatAttribute(builder, "centerY", model.getCenterY(), 0f);
        appendFloatAttribute(builder, "centerZ", model.getCenterZ(), 0f);
        if (!model.isInteractive()) {
            builder.append(" interactive={false}");
        }
    }

    private void appendElement(StringBuilder builder, SceneEditorElementModel element) {
        switch (element.getType()) {
            case BLOCK -> appendBlockElement(builder, element);
            case BOX -> appendBoxElement(builder, element);
            case LINE -> appendLineElement(builder, element);
            case DIAMOND -> appendDiamondElement(builder, element);
        }
    }

    private void appendBlockElement(StringBuilder builder, SceneEditorElementModel element) {
        StringBuilder tagBuilder = new StringBuilder();
        tagBuilder.append("<BlockAnnotation pos=\"")
            .append(formatVector(element.getPrimaryX(), element.getPrimaryY(), element.getPrimaryZ()))
            .append('"');
        appendElementStyleAttributes(tagBuilder, element, "#FFFFFFFF", true);
        appendElementTooltip(builder, "BlockAnnotation", tagBuilder, element.getTooltipMarkdown());
    }

    private void appendBoxElement(StringBuilder builder, SceneEditorElementModel element) {
        StringBuilder tagBuilder = new StringBuilder();
        tagBuilder.append("<BoxAnnotation min=\"")
            .append(formatVector(element.getPrimaryX(), element.getPrimaryY(), element.getPrimaryZ()))
            .append("\" max=\"")
            .append(formatVector(element.getSecondaryX(), element.getSecondaryY(), element.getSecondaryZ()))
            .append('"');
        appendElementStyleAttributes(tagBuilder, element, "#FFFFFFFF", true);
        appendElementTooltip(builder, "BoxAnnotation", tagBuilder, element.getTooltipMarkdown());
    }

    private void appendLineElement(StringBuilder builder, SceneEditorElementModel element) {
        StringBuilder tagBuilder = new StringBuilder();
        tagBuilder.append("<LineAnnotation from=\"")
            .append(formatVector(element.getPrimaryX(), element.getPrimaryY(), element.getPrimaryZ()))
            .append("\" to=\"")
            .append(formatVector(element.getSecondaryX(), element.getSecondaryY(), element.getSecondaryZ()))
            .append('"');
        appendElementStyleAttributes(tagBuilder, element, "#FFFFFFFF", true);
        appendElementTooltip(builder, "LineAnnotation", tagBuilder, element.getTooltipMarkdown());
    }

    private void appendDiamondElement(StringBuilder builder, SceneEditorElementModel element) {
        StringBuilder tagBuilder = new StringBuilder();
        tagBuilder.append("<DiamondAnnotation pos=\"")
            .append(formatVector(element.getPrimaryX(), element.getPrimaryY(), element.getPrimaryZ()))
            .append('"');
        appendElementStyleAttributes(tagBuilder, element, "#FF00E000", false);
        appendElementTooltip(builder, "DiamondAnnotation", tagBuilder, element.getTooltipMarkdown());
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

    private void appendElementTooltip(StringBuilder builder, String tagName, StringBuilder openingTag,
        String tooltipMarkdown) {
        if (tooltipMarkdown == null || tooltipMarkdown.isEmpty()) {
            builder.append("    ")
                .append(openingTag)
                .append(" />\n");
            return;
        }

        builder.append("    ")
            .append(openingTag)
            .append(">\n");
        appendIndentedTooltip(builder, tooltipMarkdown);
        if (!tooltipMarkdown.endsWith("\n")) {
            builder.append('\n');
        }
        builder.append("    </")
            .append(tagName)
            .append(">\n");
    }

    private void appendIndentedTooltip(StringBuilder builder, String tooltipMarkdown) {
        String normalizedTooltip = normalizeLineEndings(tooltipMarkdown);
        if (normalizedTooltip.isEmpty()) {
            return;
        }
        String[] lines = normalizedTooltip.split("\n", -1);
        for (int i = 0; i < lines.length; i++) {
            builder.append("        ")
                .append(lines[i]);
            if (i < lines.length - 1) {
                builder.append('\n');
            }
        }
    }

    private void ensureAllowedAttributes(MdxJsxElementFields element, Set<String> allowedAttributes, String tagName) {
        for (MdxJsxAttributeNode attributeNode : element.attributes()) {
            if (!(attributeNode instanceof MdxJsxAttribute attribute)) {
                throw new UnsupportedSubsetException("Spread attributes are not supported on <" + tagName + ">");
            }
            if (!allowedAttributes.contains(attribute.name)) {
                throw new UnsupportedSubsetException(
                    "Unsupported attribute '" + attribute.name + "' on <" + tagName + ">");
            }
        }
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
            return Arrays.copyOf(defaultValue, defaultValue.length);
        }

        String[] pieces = rawValue.trim()
            .split("\\s+");
        if (pieces.length != 3) {
            throw new InvalidSceneSyntaxException("Attribute '" + name + "' must contain exactly 3 numbers");
        }
        try {
            return new float[] { Float.parseFloat(pieces[0]), Float.parseFloat(pieces[1]),
                Float.parseFloat(pieces[2]) };
        } catch (NumberFormatException e) {
            throw new InvalidSceneSyntaxException("Attribute '" + name + "' must contain valid numbers");
        }
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

    private boolean isIgnorableNode(UnistNode node, String source) {
        if (node instanceof MdxJsxElementFields) {
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

    private void appendFloatAttribute(StringBuilder builder, String name, float value, float defaultValue) {
        if (!isNear(value, defaultValue)) {
            builder.append(' ')
                .append(name)
                .append("={")
                .append(formatNumber(value))
                .append('}');
        }
    }

    private String formatVector(float x, float y, float z) {
        return formatNumber(x) + ' ' + formatNumber(y) + ' ' + formatNumber(z);
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
        return markdown.replace("\r\n", "\n")
            .replace('\r', '\n');
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

    private static final class UnsupportedSubsetException extends RuntimeException {

        private UnsupportedSubsetException(String message) {
            super(message);
        }
    }

    private static final class InvalidSceneSyntaxException extends RuntimeException {

        private InvalidSceneSyntaxException(String message) {
            super(message);
        }
    }
}
