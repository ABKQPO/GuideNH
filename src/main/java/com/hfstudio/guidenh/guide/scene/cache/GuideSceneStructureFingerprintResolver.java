package com.hfstudio.guidenh.guide.scene.cache;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.guide.compiler.IdUtils;
import com.hfstudio.guidenh.guide.compiler.PageCompiler;
import com.hfstudio.guidenh.guide.internal.editor.SceneEditorSession;
import com.hfstudio.guidenh.guide.internal.editor.io.SceneEditorStructureCache;
import com.hfstudio.guidenh.guide.internal.editor.model.SceneEditorSceneModel;
import com.hfstudio.guidenh.guide.internal.editor.model.SceneEditorSceneNodeModel;
import com.hfstudio.guidenh.guide.internal.editor.model.SceneEditorSceneNodeType;
import com.hfstudio.guidenh.guide.scene.SceneTagCompiler;
import com.hfstudio.guidenh.integration.structurelib.StructureLibPreviewSelection;
import com.hfstudio.guidenh.libs.mdast.mdx.model.MdxJsxAttribute;
import com.hfstudio.guidenh.libs.mdast.mdx.model.MdxJsxAttributeNode;
import com.hfstudio.guidenh.libs.mdast.mdx.model.MdxJsxElementFields;
import com.hfstudio.guidenh.libs.mdast.mdx.model.MdxJsxExpressionAttribute;
import com.hfstudio.guidenh.libs.mdast.model.MdAstAnyContent;
import com.hfstudio.guidenh.libs.unist.UnistNode;

public class GuideSceneStructureFingerprintResolver {

    public static final Comparator<String> NULL_SAFE_STRING_COMPARATOR = Comparator.nullsFirst(String::compareTo);

    @Nullable
    public GuideSceneStructureCacheKey buildForGameScene(PageCompiler compiler,
        List<? extends MdAstAnyContent> children,
        @Nullable Map<String, StructureLibPreviewSelection> structureLibSelections) {
        GuideSceneStructureFingerprintBuilder builder = new GuideSceneStructureFingerprintBuilder();
        Map<String, StructureLibPreviewSelection> selections = structureLibSelections != null ? structureLibSelections
            : Map.of();
        int structuralIndex = 0;
        int structureLibIndex = 0;
        for (MdAstAnyContent child : children) {
            MdxJsxElementFields element = unwrapSceneElement(child);
            if (element == null || !isStructuralSceneElement(element.name())) {
                continue;
            }
            int currentStructureLibIndex = "ImportStructureLib".equals(element.name()) ? structureLibIndex++ : -1;
            appendGameSceneElement(builder, compiler, element, selections, structuralIndex++, currentStructureLibIndex);
        }
        return builder.isEmpty() ? null : builder.build();
    }

    @Nullable
    public GuideSceneStructureCacheKey buildForPreview(SceneEditorSession session, Path workingRoot,
        @Nullable StructureLibPreviewSelection structureLibSelectionOverride) {
        SceneEditorSceneModel model = session.getSceneModel();
        GuideSceneStructureFingerprintBuilder builder = new GuideSceneStructureFingerprintBuilder();
        if (model.getSceneNodes()
            .isEmpty()) {
            appendLegacyPreviewStructure(builder, session, workingRoot, model);
            return builder.isEmpty() ? null : builder.build();
        }
        int structuralIndex = 0;
        for (SceneEditorSceneNodeModel node : model.getSceneNodes()) {
            if (!isStructuralPreviewNode(node.getType())) {
                continue;
            }
            appendPreviewNode(builder, session, workingRoot, node, structureLibSelectionOverride, structuralIndex++);
        }
        return builder.isEmpty() ? null : builder.build();
    }

    public boolean isStructuralSceneElement(@Nullable String name) {
        return "Block".equals(name) || "Entity".equals(name)
            || "ImportStructure".equals(name)
            || "ImportStructureLib".equals(name)
            || "PlaceBlock".equals(name)
            || "RemoveBlocks".equals(name)
            || "ReplaceBlock".equals(name);
    }

    public boolean isStructuralPreviewNode(SceneEditorSceneNodeType type) {
        return type == SceneEditorSceneNodeType.IMPORT_STRUCTURE
            || type == SceneEditorSceneNodeType.IMPORT_STRUCTURE_LIB
            || type == SceneEditorSceneNodeType.REMOVE_BLOCKS
            || type == SceneEditorSceneNodeType.OPAQUE;
    }

    @Nullable
    private static MdxJsxElementFields unwrapSceneElement(UnistNode node) {
        return SceneTagCompiler.unwrapSceneElement(node);
    }

    private void appendGameSceneElement(GuideSceneStructureFingerprintBuilder builder, PageCompiler compiler,
        MdxJsxElementFields element, Map<String, StructureLibPreviewSelection> structureLibSelections,
        int structuralIndex, int structureLibIndex) {
        String name = element.name();
        if (name == null) {
            return;
        }
        String prefix = structuralIndex + ":" + name;
        appendAttributes(
            builder,
            prefix,
            element.attributes(),
            attributeNode -> shouldIncludeSceneAttribute(name, attributeNode));
        if ("ImportStructure".equals(name)) {
            appendImportedStructureAsset(builder, prefix, compiler, element.getAttributeString("src", null));
            return;
        }
        if ("ImportStructureLib".equals(name)) {
            appendStructureLibSelection(
                builder,
                prefix,
                element.getAttributeString("name", null),
                structureLibSelections,
                structureLibIndex,
                element.getAttributeString("channel", null));
        }
    }

    private void appendLegacyPreviewStructure(GuideSceneStructureFingerprintBuilder builder, SceneEditorSession session,
        Path workingRoot, SceneEditorSceneModel model) {
        String structureText = resolvePreviewStructureText(session, workingRoot, model.getStructureSource());
        if (structureText != null) {
            builder.addHashedText("legacy:structure", structureText);
        }
    }

    private void appendPreviewNode(GuideSceneStructureFingerprintBuilder builder, SceneEditorSession session,
        Path workingRoot, SceneEditorSceneNodeModel node,
        @Nullable StructureLibPreviewSelection structureLibSelectionOverride, int structuralIndex) {
        String prefix = structuralIndex + ":"
            + node.getType()
                .name();
        if (node.getType() == SceneEditorSceneNodeType.OPAQUE) {
            String opaqueText = node.getOpaqueText();
            if (opaqueText != null && !opaqueText.isEmpty()) {
                builder.addHashedText(prefix + ":opaque", opaqueText);
            }
            return;
        }
        appendAttributes(builder, prefix, node.getType(), node.getAttributes());
        if (node.getType() == SceneEditorSceneNodeType.IMPORT_STRUCTURE) {
            appendPreviewStructureText(builder, prefix, session, workingRoot, node.getAttribute("src"));
            return;
        }
        if (node.getType() == SceneEditorSceneNodeType.IMPORT_STRUCTURE_LIB) {
            appendStructureLibSelection(builder, prefix, structureLibSelectionOverride, node.getAttribute("channel"));
        }
    }

    private void appendImportedStructureAsset(GuideSceneStructureFingerprintBuilder builder, String prefix,
        PageCompiler compiler, @Nullable String src) {
        if (src == null || src.trim()
            .isEmpty()) {
            builder.add(prefix + ":structure:missing", "missing-src");
            return;
        }
        builder.add(prefix + ":structure:src", src.trim());
        try {
            byte[] data = compiler.loadAsset(IdUtils.resolveLink(src, compiler.getPageId()));
            if (data != null) {
                builder.addHashedBytes(prefix + ":structure:bytes", data);
            } else {
                builder.add(prefix + ":structure:bytes", "missing-asset");
            }
        } catch (Exception e) {
            builder.add(
                prefix + ":structure:error",
                e.getClass()
                    .getName() + ":"
                    + e.getMessage());
        }
    }

    private void appendPreviewStructureText(GuideSceneStructureFingerprintBuilder builder, String prefix,
        SceneEditorSession session, Path workingRoot, @Nullable String structureSource) {
        String structureText = resolvePreviewStructureText(session, workingRoot, structureSource);
        if (structureText != null) {
            builder.addHashedText(prefix + ":structure:text", structureText);
        } else if (structureSource != null && !structureSource.trim()
            .isEmpty()) {
                builder.add(prefix + ":structure:src", structureSource.trim());
            }
    }

    private void appendStructureLibSelection(GuideSceneStructureFingerprintBuilder builder, String prefix,
        @Nullable String structureName, Map<String, StructureLibPreviewSelection> structureLibSelections,
        int structureLibIndex, @Nullable String requestedChannel) {
        String normalizedName = normalize(structureName);
        StructureLibPreviewSelection selection = normalizedName != null ? structureLibSelections.get(normalizedName)
            : structureLibSelections.get("structurelib#" + Math.max(0, structureLibIndex));
        appendStructureLibSelection(builder, prefix, selection, requestedChannel);
    }

    private void appendStructureLibSelection(GuideSceneStructureFingerprintBuilder builder, String prefix,
        @Nullable StructureLibPreviewSelection selection, @Nullable String requestedChannel) {
        StructureLibPreviewSelection effectiveSelection = resolveEffectiveSelection(selection, requestedChannel);
        if (effectiveSelection != null) {
            builder.add(prefix + ":selection:tier", effectiveSelection.getMasterTier());
            List<Map.Entry<String, Integer>> channelEntries = new ArrayList<>(
                effectiveSelection.getChannelOverrides()
                    .entrySet());
            channelEntries.sort(Map.Entry.comparingByKey());
            for (Map.Entry<String, Integer> entry : channelEntries) {
                builder.add(prefix + ":selection:channel:" + entry.getKey(), entry.getValue());
            }
            List<Map.Entry<String, Boolean>> optionEntries = new ArrayList<>(
                effectiveSelection.getIntegrationOptions()
                    .entrySet());
            optionEntries.sort(Map.Entry.comparingByKey());
            for (Map.Entry<String, Boolean> entry : optionEntries) {
                builder.add(prefix + ":selection:option:" + entry.getKey(), entry.getValue());
            }
            return;
        }
        if (requestedChannel != null && !requestedChannel.trim()
            .isEmpty()) {
            builder.add(prefix + ":selection:requestedChannel", requestedChannel.trim());
        }
    }

    @Nullable
    private StructureLibPreviewSelection resolveEffectiveSelection(@Nullable StructureLibPreviewSelection selection,
        @Nullable String requestedChannel) {
        if (selection != null) {
            return selection;
        }
        String normalizedRequestedChannel = normalize(requestedChannel);
        if (normalizedRequestedChannel == null) {
            return StructureLibPreviewSelection.defaultSelection();
        }
        try {
            return StructureLibPreviewSelection.ofMasterTier(Integer.parseInt(normalizedRequestedChannel));
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private void appendAttributes(GuideSceneStructureFingerprintBuilder builder, String prefix,
        List<MdxJsxAttributeNode> attributes, SceneAttributeInclusionPredicate inclusionPredicate) {
        List<String> rendered = new ArrayList<>();
        for (MdxJsxAttributeNode attributeNode : attributes) {
            if (!inclusionPredicate.shouldInclude(attributeNode)) {
                continue;
            }
            rendered.add(renderAttribute(attributeNode));
        }
        rendered.sort(NULL_SAFE_STRING_COMPARATOR);
        for (int i = 0; i < rendered.size(); i++) {
            builder.add(prefix + ":attr:" + i, rendered.get(i));
        }
    }

    private void appendAttributes(GuideSceneStructureFingerprintBuilder builder, String prefix,
        SceneEditorSceneNodeType nodeType, Map<String, String> attributes) {
        List<Map.Entry<String, String>> entries = new ArrayList<>(attributes.entrySet());
        entries.sort(Map.Entry.comparingByKey(NULL_SAFE_STRING_COMPARATOR));
        for (Map.Entry<String, String> entry : entries) {
            if (!shouldIncludePreviewAttribute(nodeType, entry.getKey())) {
                continue;
            }
            builder.add(prefix + ":attr:" + entry.getKey(), String.valueOf(entry.getValue()));
        }
    }

    private String renderAttribute(MdxJsxAttributeNode attributeNode) {
        if (attributeNode instanceof MdxJsxAttribute attribute) {
            if (attribute.hasStringValue()) {
                return attribute.name + "=\"" + attribute.getStringValue() + "\"";
            }
            if (attribute.hasExpressionValue()) {
                return attribute.name + "={" + attribute.getExpressionValue() + "}";
            }
            return attribute.name;
        }
        if (attributeNode instanceof MdxJsxExpressionAttribute expressionAttribute) {
            return "..." + expressionAttribute.value;
        }
        return attributeNode.type();
    }

    private boolean shouldIncludeSceneAttribute(String elementName, MdxJsxAttributeNode attributeNode) {
        if (!"ImportStructureLib".equals(elementName)) {
            return true;
        }
        if (!(attributeNode instanceof MdxJsxAttribute attribute)) {
            return true;
        }
        String attributeName = normalize(attribute.name);
        if (attributeName == null) {
            return true;
        }
        return !"name".equals(attributeName);
    }

    private boolean shouldIncludePreviewAttribute(SceneEditorSceneNodeType nodeType, @Nullable String attributeName) {
        if (nodeType != SceneEditorSceneNodeType.IMPORT_STRUCTURE_LIB) {
            return true;
        }
        String normalizedName = normalize(attributeName);
        return !"name".equals(normalizedName);
    }

    private interface SceneAttributeInclusionPredicate {

        boolean shouldInclude(MdxJsxAttributeNode attributeNode);
    }

    @Nullable
    private String resolvePreviewStructureText(SceneEditorSession session, Path workingRoot,
        @Nullable String structureSource) {
        String normalizedSource = normalize(structureSource);
        String importedStructureSnbt = session.getImportedStructureSnbt();
        if (importedStructureSnbt != null && !importedStructureSnbt.trim()
            .isEmpty()) {
            String modelStructureSource = normalize(
                session.getSceneModel()
                    .getStructureSource());
            if (modelStructureSource == null || modelStructureSource.equals(normalizedSource)) {
                return importedStructureSnbt;
            }
        }
        if (normalizedSource == null) {
            return null;
        }
        try {
            Path path = SceneEditorStructureCache.resolveSceneStructurePath(workingRoot, normalizedSource)
                .orElse(null);
            if (path == null || !Files.exists(path)) {
                return null;
            }
            return Files.readString(path);
        } catch (Exception ignored) {
            return null;
        }
    }

    @Nullable
    private static String normalize(@Nullable String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
