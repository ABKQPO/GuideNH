package com.hfstudio.guidenh.guide.scene.element;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;

import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.guide.compiler.PageCompiler;
import com.hfstudio.guidenh.guide.compiler.tags.MdxAttrs;
import com.hfstudio.guidenh.guide.document.LytErrorSink;
import com.hfstudio.guidenh.guide.scene.CameraSettings;
import com.hfstudio.guidenh.guide.scene.LytGuidebookScene;
import com.hfstudio.guidenh.guide.scene.StructureLibSceneBinding;
import com.hfstudio.guidenh.guide.scene.annotation.compiler.AnnotationTagCompiler;
import com.hfstudio.guidenh.guide.scene.cache.GuideSceneStructureCompileScope;
import com.hfstudio.guidenh.guide.scene.level.GuidebookLevel;
import com.hfstudio.guidenh.guide.scene.level.GuidebookPreviewBlockPlacer;
import com.hfstudio.guidenh.guide.scene.support.ScenePreviewFormedState;
import com.hfstudio.guidenh.guide.scene.support.SceneStructureOptions;
import com.hfstudio.guidenh.integration.gregtech.GregTechHelpers;
import com.hfstudio.guidenh.integration.structurelib.StructureLibImportRequest;
import com.hfstudio.guidenh.integration.structurelib.StructureLibImportResult;
import com.hfstudio.guidenh.integration.structurelib.StructureLibPreviewSelection;
import com.hfstudio.guidenh.integration.structurelib.StructureLibRuntimeFacade;
import com.hfstudio.guidenh.integration.structurelib.StructureLibSceneImportService;
import com.hfstudio.guidenh.integration.structurelib.StructureLibSceneMetadata;
import com.hfstudio.guidenh.integration.structurelib.StructureLibSceneOptions;
import com.hfstudio.guidenh.libs.mdast.mdx.model.MdxJsxElementFields;

public class ImportStructureLibElementCompiler implements SceneElementTagCompiler {

    private final StructureLibSceneImportService importService;

    public ImportStructureLibElementCompiler() {
        this(new StructureLibSceneImportService());
    }

    public ImportStructureLibElementCompiler(StructureLibSceneImportService importService) {
        this.importService = importService != null ? importService : new StructureLibSceneImportService();
    }

    @Override
    public Set<String> getTagNames() {
        return Collections.singleton("ImportStructureLib");
    }

    @Override
    public void compile(GuidebookLevel level, CameraSettings camera, PageCompiler compiler, LytErrorSink errorSink,
        MdxJsxElementFields el) {
        if (!GuideSceneStructureCompileScope.isStructureMutationEnabled()) {
            return;
        }
        LytGuidebookScene scene = AnnotationTagCompiler.CURRENT_SCENE.get();
        if (scene == null) {
            errorSink.appendError(compiler, "ImportStructureLib used outside <GameScene>", el);
            return;
        }

        String controller = MdxAttrs.getString(compiler, errorSink, el, "controller", null);
        if (controller == null || controller.trim()
            .isEmpty()) {
            errorSink.appendError(compiler, "Missing controller attribute.", el);
            return;
        }

        int requestedChannel = MdxAttrs.getInt(compiler, errorSink, el, "channel", Integer.MIN_VALUE);
        int offsetX = MdxAttrs.getInt(compiler, errorSink, el, "offsetX", 0);
        int offsetY = MdxAttrs.getInt(compiler, errorSink, el, "offsetY", 0);
        int offsetZ = MdxAttrs.getInt(compiler, errorSink, el, "offsetZ", 0);
        StructureLibSceneOptions childOptions = StructureLibSceneOptionParser.parseChildren(compiler, errorSink, el);
        StructureLibSceneOptions legacyOptions = StructureLibSceneOptionParser.parseAttributes(compiler, errorSink, el);
        StructureLibSceneOptions sceneOptions = legacyOptions.merge(childOptions);
        boolean formed = SceneStructureOptions.isFormed(compiler, errorSink, el);
        String structureName = MdxAttrs.getString(compiler, errorSink, el, "name", null);
        StructureLibSceneBinding binding = scene.registerStructureLibBinding(structureName);
        StructureLibPreviewSelection selectionOverride = binding.getPendingSelection() != null
            ? binding.getPendingSelection()
            : scene.getPendingStructureLibPreviewSelection(structureName) != null
                ? scene.getPendingStructureLibPreviewSelection(structureName)
                : scene.getPendingStructureLibPreviewSelection();
        StructureLibPreviewSelection defaultSelection = sceneOptions
            .createSelection(requestedChannel == Integer.MIN_VALUE ? null : Integer.valueOf(requestedChannel));
        StructureLibPreviewSelection selection = selectionOverride != null
            ? mergePersistentOptions(selectionOverride, defaultSelection, sceneOptions)
            : defaultSelection;
        StructureLibImportRequest request = new StructureLibImportRequest(
            controller,
            MdxAttrs.getString(compiler, errorSink, el, "piece", null),
            StructureLibSceneOptions
                .resolveFacing(MdxAttrs.getString(compiler, errorSink, el, "facing", null), sceneOptions),
            StructureLibSceneOptions
                .resolveRotation(MdxAttrs.getString(compiler, errorSink, el, "rotation", null), sceneOptions),
            StructureLibSceneOptions
                .resolveFlip(MdxAttrs.getString(compiler, errorSink, el, "flip", null), sceneOptions),
            Integer.valueOf(selection.getMasterTier()),
            applyControllerDefaults(controller, selection, sceneOptions),
            sceneOptions);
        scene.setPendingStructureLibPreviewSelection(structureName, request.getPreviewSelection());

        StructureLibImportResult result = importService.importScene(request);
        attachMetadata(scene, structureName, request, result);

        if (!result.isSuccess()) {
            errorSink.appendError(compiler, resolveFailureMessage(result.getErrors(), request.getController()), el);
            return;
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
    }

    public static void attachMetadata(LytGuidebookScene scene, @Nullable String structureName,
        StructureLibImportRequest request, StructureLibImportResult result) {
        StructureLibSceneMetadata metadata = result.getMetadata();
        if (metadata != null) {
            scene.setStructureLibSceneMetadata(structureName, metadata);
            return;
        }

        if (result.isSuccess()) {
            scene.setStructureLibSceneMetadata(
                structureName,
                new StructureLibSceneMetadata(
                    request.getController(),
                    request.getPiece(),
                    request.getFacing(),
                    request.getRotation(),
                    request.getFlip()));
        }
    }

    public static String resolveFailureMessage(List<String> errors, String controller) {
        if (errors != null && !errors.isEmpty()) {
            String firstError = errors.get(0);
            if (firstError != null && !firstError.trim()
                .isEmpty()) {
                return firstError;
            }
        }
        return "StructureLib import failed for controller: " + controller;
    }

    public static StructureLibPreviewSelection mergePersistentOptions(StructureLibPreviewSelection selection,
        StructureLibPreviewSelection defaults, StructureLibSceneOptions options) {
        StructureLibPreviewSelection merged = new StructureLibPreviewSelection(
            selection.getMasterTier(),
            selection.getChannelOverrides(),
            defaults.getIntegrationOptions());
        if (options != null && options.isGregTechActiveController()) {
            merged = merged.withIntegrationOption(StructureLibSceneOptions.GREGTECH_ACTIVE_CONTROLLER_OPTION, true);
        }
        if (options != null && options.isGregTechPlaceHatches()) {
            merged = merged.withIntegrationOption(StructureLibSceneOptions.GREGTECH_PLACE_HATCHES_OPTION, true);
        }
        return merged;
    }

    public static StructureLibPreviewSelection applyControllerDefaults(String controller,
        StructureLibPreviewSelection selection, StructureLibSceneOptions options) {
        StructureLibPreviewSelection result = selection != null ? selection
            : StructureLibPreviewSelection.defaultSelection();
        if (options != null && options.isGregTechPlaceHatches()) {
            result = result.withIntegrationOption(StructureLibPreviewSelection.SURVIVAL_CONSTRUCT_OPTION, true);
            result = result
                .withIntegrationOption(StructureLibPreviewSelection.SURVIVAL_FILL_EMPTY_HATCHES_OPTION, false);
            return result;
        }
        try {
            StructureLibRuntimeFacade.ResolvedController resolvedController = StructureLibRuntimeFacade
                .resolveController(
                    new StructureLibImportRequest(
                        controller,
                        null,
                        null,
                        null,
                        null,
                        Integer.valueOf(result.getMasterTier()),
                        result));
            if (GregTechHelpers
                .getMachineControllerBaseMeta(resolvedController.getBlock(), resolvedController.getMeta()) != null) {
                result = result.withIntegrationOption(StructureLibPreviewSelection.SURVIVAL_CONSTRUCT_OPTION, true);
                result = result
                    .withIntegrationOption(StructureLibPreviewSelection.SURVIVAL_FILL_EMPTY_HATCHES_OPTION, true);
            }
        } catch (Throwable ignored) {}
        return result;
    }
}
