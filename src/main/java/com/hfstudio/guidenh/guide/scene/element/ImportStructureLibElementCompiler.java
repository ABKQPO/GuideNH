package com.hfstudio.guidenh.guide.scene.element;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;

import com.hfstudio.guidenh.guide.compiler.PageCompiler;
import com.hfstudio.guidenh.guide.compiler.tags.MdxAttrs;
import com.hfstudio.guidenh.guide.document.LytErrorSink;
import com.hfstudio.guidenh.guide.scene.CameraSettings;
import com.hfstudio.guidenh.guide.scene.LytGuidebookScene;
import com.hfstudio.guidenh.guide.scene.annotation.compiler.AnnotationTagCompiler;
import com.hfstudio.guidenh.guide.scene.level.GuidebookLevel;
import com.hfstudio.guidenh.guide.scene.level.GuidebookPreviewBlockPlacer;
import com.hfstudio.guidenh.guide.scene.structurelib.StructureLibImportRequest;
import com.hfstudio.guidenh.guide.scene.structurelib.StructureLibImportResult;
import com.hfstudio.guidenh.guide.scene.structurelib.StructureLibSceneImportService;
import com.hfstudio.guidenh.guide.scene.structurelib.StructureLibSceneMetadata;
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
        StructureLibImportRequest request = new StructureLibImportRequest(
            controller,
            MdxAttrs.getString(compiler, errorSink, el, "piece", null),
            MdxAttrs.getString(compiler, errorSink, el, "facing", null),
            MdxAttrs.getString(compiler, errorSink, el, "rotation", null),
            MdxAttrs.getString(compiler, errorSink, el, "flip", null),
            requestedChannel == Integer.MIN_VALUE ? null : Integer.valueOf(requestedChannel));

        StructureLibImportResult result = importService.importScene(request);
        attachMetadata(scene, request, result);

        if (!result.isSuccess()) {
            errorSink.appendError(compiler, resolveFailureMessage(result.getErrors(), request.getController()), el);
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

    private static void attachMetadata(LytGuidebookScene scene, StructureLibImportRequest request,
        StructureLibImportResult result) {
        StructureLibSceneMetadata metadata = result.getMetadata();
        if (metadata != null) {
            scene.setStructureLibSceneMetadata(metadata);
            if (request.getChannel() != null && !scene.hasStructureLibChannelData()) {
                scene.setStructureLibCurrentChannelSilently(
                    request.getChannel()
                        .intValue());
            }
            return;
        }

        if (result.isSuccess()) {
            scene.setStructureLibSceneMetadata(
                new StructureLibSceneMetadata(
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
    }

    private static String resolveFailureMessage(List<String> errors, String controller) {
        if (errors != null && !errors.isEmpty()) {
            String firstError = errors.get(0);
            if (firstError != null && !firstError.trim()
                .isEmpty()) {
                return firstError;
            }
        }
        return "StructureLib import failed for controller: " + controller;
    }
}
