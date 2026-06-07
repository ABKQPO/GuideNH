package com.hfstudio.structurelibexport;

import java.util.ArrayList;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;

import com.hfstudio.guidenh.guide.scene.level.GuidebookLevel;
import com.hfstudio.guidenh.guide.scene.level.GuidebookPreviewBlockPlacer;
import com.hfstudio.guidenh.integration.gregtech.GregTechHelpers;
import com.hfstudio.guidenh.integration.structurelib.StructureLibImportRequest;
import com.hfstudio.guidenh.integration.structurelib.StructureLibImportResult;
import com.hfstudio.guidenh.integration.structurelib.StructureLibPreviewSelection;
import com.hfstudio.guidenh.integration.structurelib.StructureLibRuntimeFacade.BuildContext;
import com.hfstudio.guidenh.integration.structurelib.StructureLibSceneImportService;
import com.hfstudio.guidenh.integration.structurelib.StructureLibSceneOptions;

public class StructureLibSceneBuilder {

    public static final String GREGTECH_ACTIVE_CONTROLLER_OPTION = "gregtech.active_controller";
    public static final String GREGTECH_PLACE_HATCHES_OPTION = "gregtech.place_hatches";

    private final StructureLibSceneImportService importService;

    public StructureLibSceneBuilder() {
        this(new StructureLibSceneImportService());
    }

    public StructureLibSceneBuilder(StructureLibSceneImportService importService) {
        this.importService = importService != null ? importService : new StructureLibSceneImportService();
    }

    public StructureLibSceneBuildResult build(StructureLibExportTaskSpec task) {
        return build(task, null);
    }

    public StructureLibSceneBuildResult build(StructureLibExportTaskSpec task, BuildContext context) {
        return build(task, context, null);
    }

    public StructureLibSceneBuildResult build(StructureLibExportTaskSpec task, BuildContext context,
        GuidebookLevel targetLevel) {
        boolean useSurvivalConstruct = task.isGtPlaceHatches() || GregTechHelpers.getMachineControllerBaseMeta(
            task.getController()
                .getBlock(),
            task.getController()
                .getMeta())
            != null;
        StructureLibPreviewSelection selection = new StructureLibPreviewSelection(task.getTier(), task.getChannels())
            .withIntegrationOption(
                StructureLibSceneOptions.GREGTECH_ACTIVE_CONTROLLER_OPTION,
                task.isGtActiveController())
            .withIntegrationOption(StructureLibSceneOptions.GREGTECH_PLACE_HATCHES_OPTION, task.isGtPlaceHatches())
            .withIntegrationOption(StructureLibPreviewSelection.SURVIVAL_CONSTRUCT_OPTION, useSurvivalConstruct)
            .withIntegrationOption(
                StructureLibPreviewSelection.SURVIVAL_FILL_EMPTY_HATCHES_OPTION,
                useSurvivalConstruct && !task.isGtPlaceHatches());
        StructureLibImportRequest request = new StructureLibImportRequest(
            task.getController()
                .getControllerArgument(),
            null,
            task.getOrientation()
                .getFacing(),
            task.getOrientation()
                .getRotation(),
            task.getOrientation()
                .getFlip(),
            task.getTier(),
            selection);
        StructureLibImportResult result = context != null ? importService.importScene(request, context)
            : importService.importScene(request);
        ArrayList<String> warnings = new ArrayList<>(task.getWarnings());
        warnings.addAll(result.getWarnings());
        if (!result.isSuccess()) {
            return StructureLibSceneBuildResult.failure(warnings, result.getErrors());
        }

        GuidebookLevel level = targetLevel != null ? targetLevel : new GuidebookLevel();
        level.clear();
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
        }
        return StructureLibSceneBuildResult.success(level, warnings, result.getMetadata());
    }
}
