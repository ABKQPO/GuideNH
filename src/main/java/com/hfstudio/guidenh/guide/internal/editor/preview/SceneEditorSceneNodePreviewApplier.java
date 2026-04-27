package com.hfstudio.guidenh.guide.internal.editor.preview;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.annotation.Nullable;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.joml.Vector3f;

import com.hfstudio.guidenh.guide.color.ConstantColor;
import com.hfstudio.guidenh.guide.internal.editor.SceneEditorSession;
import com.hfstudio.guidenh.guide.internal.editor.model.SceneEditorElementModel;
import com.hfstudio.guidenh.guide.internal.editor.model.SceneEditorSceneModel;
import com.hfstudio.guidenh.guide.internal.editor.model.SceneEditorSceneNodeModel;
import com.hfstudio.guidenh.guide.internal.structure.GuideTextNbtCodec;
import com.hfstudio.guidenh.guide.scene.LytGuidebookScene;
import com.hfstudio.guidenh.guide.scene.annotation.DiamondAnnotation;
import com.hfstudio.guidenh.guide.scene.annotation.InWorldBoxAnnotation;
import com.hfstudio.guidenh.guide.scene.annotation.InWorldLineAnnotation;
import com.hfstudio.guidenh.guide.scene.annotation.SceneAnnotation;
import com.hfstudio.guidenh.guide.scene.level.GuidebookLevel;
import com.hfstudio.guidenh.guide.scene.level.GuidebookPreviewBlockPlacer;
import com.hfstudio.guidenh.guide.scene.structurelib.StructureLibImportRequest;
import com.hfstudio.guidenh.guide.scene.structurelib.StructureLibImportResult;
import com.hfstudio.guidenh.guide.scene.structurelib.StructureLibSceneImportService;
import com.hfstudio.guidenh.guide.scene.support.BlockAnnotationTemplateExpander;
import com.hfstudio.guidenh.guide.scene.support.GuideBlockMatcher;
import com.hfstudio.guidenh.guide.scene.support.RemoveBlocksExecutor;

final class SceneEditorSceneNodePreviewApplier {

    private static final Logger LOG = LogManager.getLogger("GuideNH/ScenePreview");

    private final Path workingRoot;
    private final StructureLibSceneImportService structureLibImportService;

    SceneEditorSceneNodePreviewApplier(Path workingRoot, StructureLibSceneImportService structureLibImportService) {
        this.workingRoot = workingRoot;
        this.structureLibImportService = structureLibImportService;
    }

    void apply(SceneEditorSession session, LytGuidebookScene scene) {
        apply(session, scene, null);
    }

    void apply(SceneEditorSession session, LytGuidebookScene scene, @Nullable Integer structureLibChannelOverride) {
        SceneEditorSceneModel model = session.getSceneModel();
        if (model.getSceneNodes()
            .isEmpty()) {
            applyLegacyPreview(session, scene);
            return;
        }

        for (SceneEditorSceneNodeModel node : model.getSceneNodes()) {
            applyNode(session, scene, node, structureLibChannelOverride);
        }
    }

    private void applyLegacyPreview(SceneEditorSession session, LytGuidebookScene scene) {
        String structureText = resolveStructureText(
            session,
            session.getSceneModel()
                .getStructureSource());
        if (structureText != null) {
            loadStructureIntoLevel(scene.getLevel(), structureText);
        }

        for (SceneEditorElementModel element : session.getSceneModel()
            .getElements()) {
            appendAnnotation(scene, element);
        }
    }

    private void applyNode(SceneEditorSession session, LytGuidebookScene scene, SceneEditorSceneNodeModel node,
        @Nullable Integer structureLibChannelOverride) {
        switch (node.getType()) {
            case IMPORT_STRUCTURE:
                applyImportStructure(session, scene.getLevel(), node);
                return;
            case IMPORT_STRUCTURE_LIB:
                applyImportStructureLib(scene, node, structureLibChannelOverride);
                return;
            case REMOVE_BLOCKS:
                applyRemoveBlocks(scene.getLevel(), node);
                return;
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

    private void applyImportStructure(SceneEditorSession session, GuidebookLevel level,
        SceneEditorSceneNodeModel node) {
        String src = normalizeAttribute(node.getAttribute("src"));
        if (src == null) {
            return;
        }

        String structureText = resolveStructureText(session, src);
        if (structureText == null) {
            return;
        }

        loadStructureIntoLevel(level, structureText);
    }

    private void applyImportStructureLib(LytGuidebookScene scene, SceneEditorSceneNodeModel node,
        @Nullable Integer structureLibChannelOverride) {
        String controller = normalizeAttribute(node.getAttribute("controller"));
        if (controller == null) {
            return;
        }
        GuidebookLevel level = scene.getLevel();

        StructureLibImportRequest request = new StructureLibImportRequest(
            controller,
            node.getAttribute("piece"),
            node.getAttribute("facing"),
            node.getAttribute("rotation"),
            node.getAttribute("flip"),
            structureLibChannelOverride != null ? structureLibChannelOverride
                : parseIntegerAttribute(node.getAttribute("channel")));
        StructureLibImportResult result = structureLibImportService.importScene(request);
        attachStructureLibMetadata(scene, request, result);
        if (!result.isSuccess()) {
            return;
        }

        for (StructureLibImportResult.PlacedBlock placedBlock : result.getBlocks()) {
            Block block = placedBlock.getBlock();
            if (block == null || block == Blocks.air) {
                continue;
            }

            GuidebookPreviewBlockPlacer.place(
                level,
                placedBlock.getX(),
                placedBlock.getY(),
                placedBlock.getZ(),
                block,
                placedBlock.getMeta(),
                placedBlock.getTileTag(),
                placedBlock.getBlockId());
            level.setExplicitBlockId(
                placedBlock.getX(),
                placedBlock.getY(),
                placedBlock.getZ(),
                placedBlock.getBlockId());
        }
    }

    private void attachStructureLibMetadata(LytGuidebookScene scene, StructureLibImportRequest request,
        StructureLibImportResult result) {
        if (result.getMetadata() != null) {
            scene.setStructureLibSceneMetadata(result.getMetadata());
            if (request.getChannel() != null && !scene.hasStructureLibChannelData()) {
                scene.setStructureLibCurrentChannelSilently(
                    request.getChannel()
                        .intValue());
            }
            return;
        }
        if (!result.isSuccess()) {
            return;
        }

        scene.setStructureLibSceneMetadata(
            new com.hfstudio.guidenh.guide.scene.structurelib.StructureLibSceneMetadata(
                request.getController(),
                request.getPiece(),
                request.getFacing(),
                request.getRotation(),
                request.getFlip()));
        if (request.getChannel() != null) {
            scene.setStructureLibCurrentChannelSilently(
                request.getChannel()
                    .intValue());
        }
    }

    private void applyRemoveBlocks(GuidebookLevel level, SceneEditorSceneNodeModel node) {
        String blockId = normalizeAttribute(node.getAttribute("id"));
        if (blockId == null) {
            return;
        }

        try {
            RemoveBlocksExecutor.execute(level, GuideBlockMatcher.parse(blockId));
        } catch (IllegalArgumentException e) {
            LOG.warn("Ignoring invalid RemoveBlocks matcher in preview: {}", blockId, e);
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
            templateAnnotations.add(toRuntimeAnnotation(templateElement));
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
            LOG.warn("Ignoring invalid BlockAnnotationTemplate matcher in preview: {}", blockId, e);
        }
    }

    private void appendAnnotation(LytGuidebookScene scene, @Nullable SceneEditorElementModel element) {
        if (element == null || !element.isVisible()) {
            return;
        }
        scene.addAnnotation(toRuntimeAnnotation(element));
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

        Path path = workingRoot.resolve(normalizedSource)
            .normalize();
        if (!Files.exists(path)) {
            return null;
        }

        try {
            return new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
        } catch (Exception e) {
            LOG.warn("Failed to read scene editor preview structure {}", normalizedSource, e);
            return null;
        }
    }

    private void loadStructureIntoLevel(GuidebookLevel level, String structureText) {
        try {
            NBTTagCompound root = GuideTextNbtCodec.readStructureNbt(structureText.getBytes(StandardCharsets.UTF_8));
            loadStructureIntoLevel(level, root);
        } catch (Exception e) {
            LOG.warn("Failed to parse scene editor preview structure text", e);
        }
    }

    private void loadStructureIntoLevel(GuidebookLevel level, NBTTagCompound root) {
        if (!root.hasKey("palette") || !root.hasKey("blocks")) {
            return;
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
            int meta = blockTag.hasKey("meta") ? blockTag.getInteger("meta") : 0;
            NBTTagCompound tileTag = blockTag.hasKey("nbt", 10) ? blockTag.getCompoundTag("nbt") : null;
            GuidebookPreviewBlockPlacer.place(level, pos[0], pos[1], pos[2], block, meta, tileTag, palette[state]);
            level.setExplicitBlockId(pos[0], pos[1], pos[2], palette[state]);
        }
    }

    private SceneAnnotation toRuntimeAnnotation(SceneEditorElementModel element) {
        ConstantColor color = parseColor(element.getColorLiteral());
        switch (element.getType()) {
            case BLOCK: {
                Vector3f min = new Vector3f(element.getPrimaryX(), element.getPrimaryY(), element.getPrimaryZ());
                Vector3f max = new Vector3f(
                    element.getPrimaryX() + 1f,
                    element.getPrimaryY() + 1f,
                    element.getPrimaryZ() + 1f);
                InWorldBoxAnnotation annotation = new InWorldBoxAnnotation(min, max, color, element.getThickness());
                annotation.setAlwaysOnTop(element.isAlwaysOnTop());
                if (!element.getTooltipMarkdown()
                    .isEmpty()) {
                    annotation.setTooltipText(element.getTooltipMarkdown());
                }
                return annotation;
            }
            case BOX: {
                Vector3f min = new Vector3f(element.getPrimaryX(), element.getPrimaryY(), element.getPrimaryZ());
                Vector3f max = new Vector3f(element.getSecondaryX(), element.getSecondaryY(), element.getSecondaryZ());
                normalizeBounds(min, max);
                InWorldBoxAnnotation annotation = new InWorldBoxAnnotation(min, max, color, element.getThickness());
                annotation.setAlwaysOnTop(element.isAlwaysOnTop());
                if (!element.getTooltipMarkdown()
                    .isEmpty()) {
                    annotation.setTooltipText(element.getTooltipMarkdown());
                }
                return annotation;
            }
            case LINE: {
                InWorldLineAnnotation annotation = new InWorldLineAnnotation(
                    new Vector3f(element.getPrimaryX(), element.getPrimaryY(), element.getPrimaryZ()),
                    new Vector3f(element.getSecondaryX(), element.getSecondaryY(), element.getSecondaryZ()),
                    color,
                    element.getThickness());
                annotation.setAlwaysOnTop(element.isAlwaysOnTop());
                if (!element.getTooltipMarkdown()
                    .isEmpty()) {
                    annotation.setTooltipText(element.getTooltipMarkdown());
                }
                return annotation;
            }
            case DIAMOND:
            default: {
                DiamondAnnotation annotation = new DiamondAnnotation(
                    new Vector3f(element.getPrimaryX(), element.getPrimaryY(), element.getPrimaryZ()),
                    color);
                annotation.setAlwaysOnTop(element.isAlwaysOnTop());
                if (!element.getTooltipMarkdown()
                    .isEmpty()) {
                    annotation.setTooltipText(element.getTooltipMarkdown());
                }
                return annotation;
            }
        }
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

    @Nullable
    private static String normalizeAttribute(@Nullable String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    @Nullable
    private static Integer parseIntegerAttribute(@Nullable String value) {
        String normalized = normalizeAttribute(value);
        if (normalized == null) {
            return null;
        }
        try {
            return Integer.valueOf(Integer.parseInt(normalized));
        } catch (NumberFormatException ignored) {
            return null;
        }
    }
}
