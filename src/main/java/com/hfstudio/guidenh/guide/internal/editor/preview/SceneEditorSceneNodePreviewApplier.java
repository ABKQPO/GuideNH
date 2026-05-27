package com.hfstudio.guidenh.guide.internal.editor.preview;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.world.World;

import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import com.hfstudio.guidenh.guide.color.ConstantColor;
import com.hfstudio.guidenh.guide.compiler.PageCompiler;
import com.hfstudio.guidenh.guide.document.block.LytParagraph;
import com.hfstudio.guidenh.guide.internal.editor.SceneEditorSession;
import com.hfstudio.guidenh.guide.internal.editor.io.SceneEditorStructureCache;
import com.hfstudio.guidenh.guide.internal.editor.model.SceneEditorElementModel;
import com.hfstudio.guidenh.guide.internal.editor.model.SceneEditorElementType;
import com.hfstudio.guidenh.guide.internal.editor.model.SceneEditorSceneModel;
import com.hfstudio.guidenh.guide.internal.editor.model.SceneEditorSceneNodeModel;
import com.hfstudio.guidenh.guide.internal.localization.GuideResourceLanguageIndex;
import com.hfstudio.guidenh.guide.internal.structure.GuideTextNbtCodec;
import com.hfstudio.guidenh.guide.internal.util.LangUtil;
import com.hfstudio.guidenh.guide.scene.LytGuidebookScene;
import com.hfstudio.guidenh.guide.scene.StructureLibSceneBinding;
import com.hfstudio.guidenh.guide.scene.StructureLibSceneCondition;
import com.hfstudio.guidenh.guide.scene.annotation.DiamondAnnotation;
import com.hfstudio.guidenh.guide.scene.annotation.InWorldBoxAnnotation;
import com.hfstudio.guidenh.guide.scene.annotation.InWorldLineAnnotation;
import com.hfstudio.guidenh.guide.scene.annotation.SceneAnnotation;
import com.hfstudio.guidenh.guide.scene.annotation.TextAnnotation;
import com.hfstudio.guidenh.guide.scene.annotation.compiler.BlockAnnotationTemplateElementCompiler;
import com.hfstudio.guidenh.guide.scene.cache.GuideSceneStructureCache;
import com.hfstudio.guidenh.guide.scene.cache.GuideSceneStructureCacheEntry;
import com.hfstudio.guidenh.guide.scene.cache.GuideSceneStructureCacheKey;
import com.hfstudio.guidenh.guide.scene.cache.GuideSceneStructureFingerprintResolver;
import com.hfstudio.guidenh.guide.scene.element.GuidebookSceneEntityImportSupport;
import com.hfstudio.guidenh.guide.scene.level.GuidebookLevel;
import com.hfstudio.guidenh.guide.scene.level.GuidebookPreviewBlockPlacer;
import com.hfstudio.guidenh.guide.scene.support.BlockAnnotationTemplateExpander;
import com.hfstudio.guidenh.guide.scene.support.GuideBlockMatcher;
import com.hfstudio.guidenh.guide.scene.support.GuideDebugLog;
import com.hfstudio.guidenh.guide.scene.support.RemoveBlocksExecutor;
import com.hfstudio.guidenh.guide.scene.support.ScenePreviewFormedState;
import com.hfstudio.guidenh.integration.structurelib.StructureLibImportRequest;
import com.hfstudio.guidenh.integration.structurelib.StructureLibImportResult;
import com.hfstudio.guidenh.integration.structurelib.StructureLibPreviewSelection;
import com.hfstudio.guidenh.integration.structurelib.StructureLibSceneImportService;
import com.hfstudio.guidenh.integration.structurelib.StructureLibSceneMetadata;

public class SceneEditorSceneNodePreviewApplier {

    private final Path workingRoot;
    private final StructureLibSceneImportService structureLibImportService;
    private final SceneEditorTooltipCompiler tooltipCompiler;
    private final GuideSceneStructureFingerprintResolver structureFingerprintResolver;

    SceneEditorSceneNodePreviewApplier(Path workingRoot, StructureLibSceneImportService structureLibImportService) {
        this.workingRoot = workingRoot;
        this.structureLibImportService = structureLibImportService;
        this.tooltipCompiler = new SceneEditorTooltipCompiler();
        this.structureFingerprintResolver = new GuideSceneStructureFingerprintResolver();
    }

    void apply(SceneEditorSession session, LytGuidebookScene scene) {
        apply(session, scene, null);
    }

    void apply(SceneEditorSession session, LytGuidebookScene scene,
        @Nullable StructureLibPreviewSelection structureLibSelectionOverride) {
        GuideSceneStructureCacheKey cacheKey = structureFingerprintResolver
            .buildForPreview(session, workingRoot, structureLibSelectionOverride);
        if (cacheKey == null) {
            applySceneContent(session, scene, structureLibSelectionOverride, true);
            return;
        }

        GuideSceneStructureCacheEntry cacheEntry = GuideSceneStructureCache.global()
            .restore(cacheKey);
        if (cacheEntry != null) {
            cacheEntry.restoreInto(scene);
            applySceneContent(session, scene, structureLibSelectionOverride, false);
            return;
        }

        PreviewApplyResult result = applySceneContent(session, scene, structureLibSelectionOverride, true);
        if (result.isStructureCacheable()) {
            GuideSceneStructureCache.global()
                .put(cacheKey, GuideSceneStructureCacheEntry.capture(scene));
        }
    }

    private PreviewApplyResult applySceneContent(SceneEditorSession session, LytGuidebookScene scene,
        @Nullable StructureLibPreviewSelection structureLibSelectionOverride, boolean structureMutationEnabled) {
        SceneEditorSceneModel model = session.getSceneModel();
        if (model.getSceneNodes()
            .isEmpty()) {
            return new PreviewApplyResult(applyLegacyPreview(session, scene, structureMutationEnabled));
        }

        boolean structureCacheable = true;
        for (SceneEditorSceneNodeModel node : model.getSceneNodes()) {
            structureCacheable &= applyNode(
                session,
                scene,
                node,
                structureLibSelectionOverride,
                structureMutationEnabled);
        }
        return new PreviewApplyResult(structureCacheable);
    }

    void applyAnnotations(SceneEditorSession session, LytGuidebookScene scene) {
        SceneEditorSceneModel model = session.getSceneModel();
        if (model.getSceneNodes()
            .isEmpty()) {
            for (SceneEditorElementModel element : model.getElements()) {
                appendAnnotation(scene, element);
            }
            return;
        }

        for (SceneEditorSceneNodeModel node : model.getSceneNodes()) {
            applyAnnotationNode(scene, node);
        }
    }

    private boolean applyLegacyPreview(SceneEditorSession session, LytGuidebookScene scene,
        boolean structureMutationEnabled) {
        String structureSource = session.getSceneModel()
            .getStructureSource();
        String structureText = resolveStructureText(session, structureSource);
        boolean structureCacheable = true;
        if (structureMutationEnabled && structureText != null) {
            structureCacheable = loadStructureIntoLevel(scene.getLevel(), structureText);
        } else if (structureMutationEnabled && normalizeAttribute(structureSource) != null) {
            structureCacheable = false;
        }

        for (SceneEditorElementModel element : session.getSceneModel()
            .getElements()) {
            appendAnnotation(scene, element);
        }
        return structureCacheable;
    }

    private boolean applyNode(SceneEditorSession session, LytGuidebookScene scene, SceneEditorSceneNodeModel node,
        @Nullable StructureLibPreviewSelection structureLibSelectionOverride, boolean structureMutationEnabled) {
        switch (node.getType()) {
            case IMPORT_STRUCTURE:
                return applyImportStructure(session, scene.getLevel(), node, structureMutationEnabled);
            case IMPORT_STRUCTURE_LIB:
                return applyImportStructureLib(scene, node, structureLibSelectionOverride, structureMutationEnabled);
            case REMOVE_BLOCKS:
                return applyRemoveBlocks(scene.getLevel(), node, structureMutationEnabled);
            case BLOCK_ANNOTATION_TEMPLATE:
                applyBlockAnnotationTemplate(scene, node);
                return true;
            case ANNOTATION:
                appendAnnotation(scene, node.getAnnotationElement());
                return true;
            case OPAQUE:
            default:
                return true;
        }
    }

    private void applyAnnotationNode(LytGuidebookScene scene, SceneEditorSceneNodeModel node) {
        switch (node.getType()) {
            case BLOCK_ANNOTATION_TEMPLATE:
                applyBlockAnnotationTemplate(scene, node);
                return;
            case ANNOTATION:
                appendAnnotation(scene, node.getAnnotationElement());
                return;
            default:
                return;
        }
    }

    private boolean applyImportStructure(SceneEditorSession session, GuidebookLevel level,
        SceneEditorSceneNodeModel node, boolean structureMutationEnabled) {
        if (!structureMutationEnabled) {
            return true;
        }
        String src = normalizeAttribute(node.getAttribute("src"));
        if (src == null) {
            return false;
        }

        String structureText = resolveStructureText(session, src);
        if (structureText == null) {
            return false;
        }

        int offsetX = parseIntegerAttributeOrDefault(
            node.getAttribute("offsetX") != null ? node.getAttribute("offsetX") : node.getAttribute("x"),
            0);
        int offsetY = parseIntegerAttributeOrDefault(
            node.getAttribute("offsetY") != null ? node.getAttribute("offsetY") : node.getAttribute("y"),
            0);
        int offsetZ = parseIntegerAttributeOrDefault(
            node.getAttribute("offsetZ") != null ? node.getAttribute("offsetZ") : node.getAttribute("z"),
            0);
        boolean formed = parseBooleanAttribute(node.getAttribute("formed"));
        return loadStructureIntoLevel(level, structureText, offsetX, offsetY, offsetZ, formed);
    }

    private boolean applyImportStructureLib(LytGuidebookScene scene, SceneEditorSceneNodeModel node,
        @Nullable StructureLibPreviewSelection structureLibSelectionOverride, boolean structureMutationEnabled) {
        if (!structureMutationEnabled) {
            return true;
        }
        String controller = normalizeAttribute(node.getAttribute("controller"));
        if (controller == null) {
            return false;
        }
        GuidebookLevel level = scene.getLevel();
        int offsetX = parseIntegerAttributeOrDefault(node.getAttribute("offsetX"), 0);
        int offsetY = parseIntegerAttributeOrDefault(node.getAttribute("offsetY"), 0);
        int offsetZ = parseIntegerAttributeOrDefault(node.getAttribute("offsetZ"), 0);
        boolean formed = parseBooleanAttribute(node.getAttribute("formed"));
        Integer requestedChannel = parseIntegerAttribute(node.getAttribute("channel"));
        String structureName = normalizeAttribute(node.getAttribute("name"));
        StructureLibSceneBinding binding = scene.registerStructureLibBinding(structureName);
        StructureLibPreviewSelection selection = structureLibSelectionOverride != null ? structureLibSelectionOverride
            : binding.getPendingSelection() != null ? binding.getPendingSelection()
                : scene.getPendingStructureLibPreviewSelection(structureName) != null
                    ? scene.getPendingStructureLibPreviewSelection(structureName)
                    : requestedChannel != null ? StructureLibPreviewSelection.ofMasterTier(requestedChannel)
                        : StructureLibPreviewSelection.defaultSelection();

        StructureLibImportRequest request = new StructureLibImportRequest(
            controller,
            node.getAttribute("piece"),
            node.getAttribute("facing"),
            node.getAttribute("rotation"),
            node.getAttribute("flip"),
            structureLibSelectionOverride != null ? Integer.valueOf(structureLibSelectionOverride.getMasterTier())
                : requestedChannel,
            selection);
        StructureLibImportResult result = structureLibImportService.importScene(request);
        attachStructureLibMetadata(scene, structureName, request, result);
        if (!result.isSuccess()) {
            return false;
        }

        for (StructureLibImportResult.PlacedBlock placedBlock : result.getBlocks()) {
            Block block = placedBlock.getBlock();
            if (block == null || block == Blocks.air) {
                continue;
            }
            int clampedY = Math.max(0, Math.min(placedBlock.getY() + offsetY, level.getHeight() - 1));

            GuidebookPreviewBlockPlacer.place(
                level,
                placedBlock.getX() + offsetX,
                clampedY,
                placedBlock.getZ() + offsetZ,
                block,
                placedBlock.getMeta(),
                placedBlock.getTileTag(),
                placedBlock.getBlockId());
            ScenePreviewFormedState.updateAfterPlacement(
                level,
                placedBlock.getX() + offsetX,
                clampedY,
                placedBlock.getZ() + offsetZ,
                formed);
        }
        return true;
    }

    private void attachStructureLibMetadata(LytGuidebookScene scene, @Nullable String structureName,
        StructureLibImportRequest request, StructureLibImportResult result) {
        if (result.getMetadata() != null) {
            scene.setStructureLibSceneMetadata(structureName, result.getMetadata());
            return;
        }
        if (!result.isSuccess()) {
            return;
        }

        scene.setStructureLibSceneMetadata(
            structureName,
            new StructureLibSceneMetadata(
                request.getController(),
                request.getPiece(),
                request.getFacing(),
                request.getRotation(),
                request.getFlip()));
    }

    private boolean applyRemoveBlocks(GuidebookLevel level, SceneEditorSceneNodeModel node,
        boolean structureMutationEnabled) {
        if (!structureMutationEnabled) {
            return true;
        }
        String blockId = normalizeAttribute(node.getAttribute("id"));
        if (blockId == null) {
            return false;
        }

        try {
            RemoveBlocksExecutor.execute(level, GuideBlockMatcher.parse(blockId));
            return true;
        } catch (IllegalArgumentException e) {
            GuideDebugLog.warn("Ignoring invalid RemoveBlocks matcher in preview: {}", blockId, e);
            return false;
        }
    }

    private void applyBlockAnnotationTemplate(LytGuidebookScene scene, SceneEditorSceneNodeModel node) {
        String blockId = normalizeAttribute(node.getAttribute("id"));
        if (blockId == null) {
            return;
        }

        List<SceneAnnotation> templateAnnotations = new ArrayList<>();
        for (SceneEditorElementModel templateElement : node.getTemplateElements()) {
            if (!templateElement.isVisible()) {
                continue;
            }
            if (!isSupportedBlockTemplateElement(templateElement)) {
                GuideDebugLog.warn(
                    "Ignoring unsupported BlockAnnotationTemplate preview element type: {}",
                    templateElement.getType()
                        .getTagName());
                continue;
            }
            templateAnnotations.addAll(toRuntimeAnnotations(templateElement));
        }

        if (templateAnnotations.isEmpty()) {
            return;
        }

        try {
            List<SceneAnnotation> expanded = BlockAnnotationTemplateExpander
                .expand(scene.getLevel(), GuideBlockMatcher.parse(blockId), templateAnnotations);
            for (SceneAnnotation annotation : expanded) {
                scene.addAnnotation(annotation);
            }
        } catch (IllegalArgumentException e) {
            GuideDebugLog.warn("Ignoring invalid BlockAnnotationTemplate matcher in preview: {}", blockId, e);
        }
    }

    private boolean isSupportedBlockTemplateElement(SceneEditorElementModel templateElement) {
        return BlockAnnotationTemplateElementCompiler.TEMPLATE_ANNOTATION_COMPILERS.containsKey(
            templateElement.getType()
                .getTagName());
    }

    private void appendAnnotation(LytGuidebookScene scene, @Nullable SceneEditorElementModel element) {
        if (element == null || !element.isVisible()) {
            return;
        }
        for (SceneAnnotation annotation : toRuntimeAnnotations(element)) {
            if (annotation != null) {
                scene.addAnnotation(annotation);
            }
        }
    }

    @Nullable
    private String resolveStructureText(SceneEditorSession session, @Nullable String structureSource) {
        String normalizedSource = normalizeAttribute(structureSource);
        String importedStructureSnbt = session.getImportedStructureSnbt();
        if (importedStructureSnbt != null && !importedStructureSnbt.trim()
            .isEmpty()) {
            String modelStructureSource = normalizeAttribute(
                session.getSceneModel()
                    .getStructureSource());
            if (modelStructureSource == null || modelStructureSource.equals(normalizedSource)) {
                return importedStructureSnbt;
            }
        }

        if (normalizedSource == null) {
            return null;
        }

        Path path = SceneEditorStructureCache.resolveSceneStructurePath(workingRoot, normalizedSource)
            .orElse(null);
        if (path == null || !Files.exists(path)) {
            return null;
        }

        try {
            return new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
        } catch (Exception e) {
            GuideDebugLog.warn("Failed to read scene editor preview structure {}", normalizedSource, e);
            return null;
        }
    }

    private boolean loadStructureIntoLevel(GuidebookLevel level, String structureText, int offsetX, int offsetY,
        int offsetZ, boolean formed) {
        try {
            NBTTagCompound root = GuideTextNbtCodec.readStructureNbt(structureText.getBytes(StandardCharsets.UTF_8));
            return loadStructureIntoLevel(level, root, offsetX, offsetY, offsetZ, formed);
        } catch (Exception e) {
            GuideDebugLog.warn("Failed to parse scene editor preview structure text", e);
            return false;
        }
    }

    private boolean loadStructureIntoLevel(GuidebookLevel level, String structureText) {
        return loadStructureIntoLevel(level, structureText, 0, 0, 0, false);
    }

    private boolean loadStructureIntoLevel(GuidebookLevel level, NBTTagCompound root, int offsetX, int offsetY,
        int offsetZ, boolean formed) {
        if (!root.hasKey("palette") || !root.hasKey("blocks")) {
            return false;
        }
        NBTTagList paletteTag = root.getTagList("palette", 10);
        String[] palette = new String[paletteTag.tagCount()];
        for (int i = 0; i < paletteTag.tagCount(); i++) {
            palette[i] = paletteTag.getCompoundTagAt(i)
                .getString("Name");
        }

        NBTTagList blocksTag = root.getTagList("blocks", 10);
        for (int i = 0; i < blocksTag.tagCount(); i++) {
            NBTTagCompound blockTag = blocksTag.getCompoundTagAt(i);
            int state = blockTag.getInteger("state");
            if (state < 0 || state >= palette.length) {
                continue;
            }
            Block block = (Block) Block.blockRegistry.getObject(palette[state]);
            if (block == null || block == Blocks.air) {
                continue;
            }
            int[] pos = blockTag.getIntArray("pos");
            if (pos.length < 3) {
                continue;
            }
            int px = pos[0] + offsetX;
            int py = Math.max(0, Math.min(pos[1] + offsetY, level.getHeight() - 1));
            int pz = pos[2] + offsetZ;
            int meta = blockTag.hasKey("meta") ? blockTag.getInteger("meta") : 0;
            NBTTagCompound tileTag = blockTag.hasKey("nbt", 10) ? blockTag.getCompoundTag("nbt") : null;
            GuidebookPreviewBlockPlacer.place(level, px, py, pz, block, meta, tileTag, palette[state], blockTag);
            level.setExplicitBlockId(px, py, pz, palette[state]);
            ScenePreviewFormedState.updateAfterPlacement(level, px, py, pz, formed);
        }

        // Spawn entities stored in the "entities" list (produced by snbt+entities export mode).
        if (root.hasKey("entities", 9)) {
            World fakeWorld = null;
            try {
                fakeWorld = level.getOrCreateFakeWorld();
            } catch (IllegalStateException ignored) {}
            NBTTagList entitiesTag = root.getTagList("entities", 10);
            for (int i = 0; i < entitiesTag.tagCount(); i++) {
                NBTTagCompound et = entitiesTag.getCompoundTagAt(i);
                GuidebookSceneEntityImportSupport.ImportedSceneEntity importedEntity = GuidebookSceneEntityImportSupport
                    .loadImportedEntityRecord(fakeWorld, et, offsetX, offsetY, offsetZ, 0f, level.getHeight() - 1f);
                if (importedEntity != null) {
                    level.addEntity(importedEntity.entity(), importedEntity.sceneEntityId());
                    if (Boolean.TRUE.equals(importedEntity.unmount())) {
                        level.clearSceneEntityMount(importedEntity.sceneEntityId());
                    } else if (importedEntity.mountTargetSceneEntityId() != null) {
                        level.setSceneEntityMount(
                            importedEntity.sceneEntityId(),
                            importedEntity.mountTargetSceneEntityId());
                    }
                }
            }
        }
        return true;
    }

    private static class PreviewApplyResult {

        private final boolean structureCacheable;

        private PreviewApplyResult(boolean structureCacheable) {
            this.structureCacheable = structureCacheable;
        }

        public boolean isStructureCacheable() {
            return structureCacheable;
        }
    }

    private List<SceneAnnotation> toRuntimeAnnotations(SceneEditorElementModel element) {
        ConstantColor color = parseColor(element.getColorLiteral());
        if (element.getType() == SceneEditorElementType.BLOCK) {
            Vector3f min = new Vector3f(element.getPrimaryX(), element.getPrimaryY(), element.getPrimaryZ());
            Vector3f max = new Vector3f(
                element.getPrimaryX() + 1f,
                element.getPrimaryY() + 1f,
                element.getPrimaryZ() + 1f);
            InWorldBoxAnnotation annotation = new InWorldBoxAnnotation(min, max, color, element.getThickness());
            annotation.setAlwaysOnTop(element.isAlwaysOnTop());
            applyTooltip(annotation, element.getTooltipMarkdown());
            applyStructureLibCondition(annotation, element);
            return Collections.<SceneAnnotation>singletonList(annotation);
        }
        if (element.getType() == SceneEditorElementType.BOX) {
            Vector3f min = new Vector3f(element.getPrimaryX(), element.getPrimaryY(), element.getPrimaryZ());
            Vector3f max = new Vector3f(element.getSecondaryX(), element.getSecondaryY(), element.getSecondaryZ());
            normalizeBounds(min, max);
            InWorldBoxAnnotation annotation = new InWorldBoxAnnotation(min, max, color, element.getThickness());
            annotation.setAlwaysOnTop(element.isAlwaysOnTop());
            applyTooltip(annotation, element.getTooltipMarkdown());
            applyStructureLibCondition(annotation, element);
            return Collections.<SceneAnnotation>singletonList(annotation);
        }
        if (element.getType() == SceneEditorElementType.LINE) {
            InWorldLineAnnotation annotation = new InWorldLineAnnotation(
                resolveLinePoints(element),
                color,
                element.getThickness());
            annotation.setAlwaysOnTop(element.isAlwaysOnTop());
            applyTooltip(annotation, element.getTooltipMarkdown());
            applyStructureLibCondition(annotation, element);
            return Collections.<SceneAnnotation>singletonList(annotation);
        }
        if (element.getType() == SceneEditorElementType.TEXT) {
            String text = resolveAnnotationText(element);
            TextAnnotation annotation = createTextAnnotation(element, text, color);
            annotation.setBackgroundAlpha(element.getBackgroundAlpha());
            annotation.setConnector(
                parseConnectorSideAttribute(element),
                parseIntegerAttributeOrDefault(element.getExtraAttribute("connectorOffset"), 0),
                parseIntegerAttributeOrDefault(
                    element.getExtraAttribute("connectorLength"),
                    TextAnnotation.CONNECTOR_HEIGHT));
            LytParagraph paragraph = new LytParagraph();
            PageCompiler compiler = SceneEditorTooltipCompiler.createPreviewCompiler(text);
            compiler.compileInlineMarkdown(text, paragraph);
            annotation.setRichContent(paragraph);
            applyStructureLibCondition(annotation, element);
            ArrayList<SceneAnnotation> annotations = new ArrayList<>();
            annotations.add(annotation);
            SceneAnnotation highlight = createTextHighlightAnnotation(element);
            if (highlight != null) {
                annotations.add(highlight);
            }
            return annotations;
        }
        DiamondAnnotation annotation = new DiamondAnnotation(
            new Vector3f(element.getPrimaryX(), element.getPrimaryY(), element.getPrimaryZ()),
            color);
        annotation.setAlwaysOnTop(element.isAlwaysOnTop());
        applyTooltip(annotation, element.getTooltipMarkdown());
        applyStructureLibCondition(annotation, element);
        return Collections.<SceneAnnotation>singletonList(annotation);
    }

    private List<Vector3f> resolveLinePoints(SceneEditorElementModel element) {
        List<Vector3f> points = element.getLinePoints();
        if (points.size() >= 2) {
            return points;
        }
        List<Vector3f> fallback = new ArrayList<>(2);
        fallback.add(new Vector3f(element.getPrimaryX(), element.getPrimaryY(), element.getPrimaryZ()));
        fallback.add(new Vector3f(element.getSecondaryX(), element.getSecondaryY(), element.getSecondaryZ()));
        return fallback;
    }

    private TextAnnotation createTextAnnotation(SceneEditorElementModel element, String text, ConstantColor color) {
        if (parseBooleanAttribute(element.getExtraAttribute("independent"))) {
            return new TextAnnotation(
                text,
                color,
                parseIntegerAttributeOrDefault(element.getExtraAttribute("yOffset"), 0),
                element.getMaxWidth());
        }
        return new TextAnnotation(
            new Vector3f(element.getPrimaryX(), element.getPrimaryY(), element.getPrimaryZ()),
            text,
            color,
            element.getMaxWidth());
    }

    @Nullable
    private SceneAnnotation createTextHighlightAnnotation(SceneEditorElementModel element) {
        String hlMinX = normalizeAttribute(element.getExtraAttribute("hlMinX"));
        String hlMinY = normalizeAttribute(element.getExtraAttribute("hlMinY"));
        String hlMinZ = normalizeAttribute(element.getExtraAttribute("hlMinZ"));
        String hlMaxX = normalizeAttribute(element.getExtraAttribute("hlMaxX"));
        String hlMaxY = normalizeAttribute(element.getExtraAttribute("hlMaxY"));
        String hlMaxZ = normalizeAttribute(element.getExtraAttribute("hlMaxZ"));
        if (hlMinX == null && hlMinY == null && hlMinZ == null && hlMaxX == null && hlMaxY == null && hlMaxZ == null) {
            return null;
        }

        Vector3f min = new Vector3f(
            parseFloatAttributeOrDefault(hlMinX, 0f),
            parseFloatAttributeOrDefault(hlMinY, 0f),
            parseFloatAttributeOrDefault(hlMinZ, 0f));
        Vector3f max = new Vector3f(
            parseFloatAttributeOrDefault(hlMaxX, 1f),
            parseFloatAttributeOrDefault(hlMaxY, 1f),
            parseFloatAttributeOrDefault(hlMaxZ, 1f));
        normalizeBounds(min, max);
        ConstantColor highlightColor = parseColorOrDefault(element.getExtraAttribute("highlightColor"), 0x8000FFAA);
        InWorldBoxAnnotation annotation = new InWorldBoxAnnotation(
            min,
            max,
            highlightColor,
            InWorldBoxAnnotation.DEFAULT_THICKNESS);
        applyStructureLibCondition(annotation, element);
        return annotation;
    }

    private void applyTooltip(SceneAnnotation annotation, @Nullable String tooltipMarkdown) {
        annotation.setTooltip(tooltipCompiler.compile(tooltipMarkdown));
    }

    private void applyStructureLibCondition(SceneAnnotation annotation, SceneEditorElementModel element) {
        annotation.setStructureLibCondition(
            StructureLibSceneCondition
                .parse(element.getShowWhenStructure(), element.getShowWhenTier(), element.getShowWhenChannels()));
    }

    private void normalizeBounds(Vector3f min, Vector3f max) {
        if (min.x > max.x) {
            float swap = min.x;
            min.x = max.x;
            max.x = swap;
        }
        if (min.y > max.y) {
            float swap = min.y;
            min.y = max.y;
            max.y = swap;
        }
        if (min.z > max.z) {
            float swap = min.z;
            min.z = max.z;
            max.z = swap;
        }
    }

    private ConstantColor parseColor(@Nullable String colorLiteral) {
        if (colorLiteral == null || colorLiteral.isEmpty()) {
            return ConstantColor.WHITE;
        }
        if ("transparent".equalsIgnoreCase(colorLiteral)) {
            return ConstantColor.TRANSPARENT;
        }
        String normalized = colorLiteral.startsWith("#") ? colorLiteral.substring(1) : colorLiteral;
        if (normalized.length() == 6) {
            normalized = "FF" + normalized;
        }
        int color = (int) Long.parseLong(normalized.toUpperCase(Locale.ROOT), 16);
        return new ConstantColor(color);
    }

    private String resolveAnnotationText(SceneEditorElementModel element) {
        if (element == null) {
            return "";
        }
        String textKey = normalizeAttribute(element.getTextKey());
        if (textKey != null) {
            String localized = GuideResourceLanguageIndex.getValue(LangUtil.getCurrentLanguage(), textKey);
            if (localized != null && !localized.isEmpty()) {
                return localized;
            }
        }
        return element.getTextMarkdown();
    }

    private boolean parseBooleanAttribute(@Nullable String value) {
        String normalized = normalizeAttribute(value);
        return normalized != null && Boolean.parseBoolean(normalized);
    }

    private TextAnnotation.ConnectorSide parseConnectorSideAttribute(SceneEditorElementModel element) {
        String rawValue = element != null ? normalizeAttribute(element.getExtraAttribute("connectorSide")) : null;
        if (rawValue == null) {
            return TextAnnotation.ConnectorSide.BOTTOM;
        }
        try {
            return TextAnnotation.ConnectorSide.fromSerializedName(rawValue);
        } catch (IllegalArgumentException ignored) {
            return TextAnnotation.ConnectorSide.BOTTOM;
        }
    }

    private float parseFloatAttributeOrDefault(@Nullable String value, float defaultValue) {
        String normalized = normalizeAttribute(value);
        if (normalized == null) {
            return defaultValue;
        }
        try {
            return Float.parseFloat(normalized);
        } catch (NumberFormatException ignored) {
            return defaultValue;
        }
    }

    private ConstantColor parseColorOrDefault(@Nullable String colorLiteral, int defaultColor) {
        String normalized = normalizeAttribute(colorLiteral);
        return normalized != null ? parseColor(normalized) : new ConstantColor(defaultColor);
    }

    @Nullable
    public static String normalizeAttribute(@Nullable String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    @Nullable
    public static Integer parseIntegerAttribute(@Nullable String value) {
        String normalized = normalizeAttribute(value);
        if (normalized == null) {
            return null;
        }
        try {
            return Integer.parseInt(normalized);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private static int parseIntegerAttributeOrDefault(@Nullable String value, int defaultValue) {
        Integer parsed = parseIntegerAttribute(value);
        return parsed != null ? parsed.intValue() : defaultValue;
    }
}
