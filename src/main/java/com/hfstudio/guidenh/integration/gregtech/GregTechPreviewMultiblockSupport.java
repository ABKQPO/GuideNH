package com.hfstudio.guidenh.integration.gregtech;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import net.minecraft.tileentity.TileEntity;

import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.guide.scene.level.GuidebookLevel;
import com.hfstudio.guidenh.guide.scene.support.ScenePreviewFormedState;

public class GregTechPreviewMultiblockSupport {

    protected GregTechPreviewMultiblockSupport() {}

    public static List<TileEntity> collectGregTechMultiblockControllers(@Nullable GuidebookLevel level) {
        if (level == null) {
            return new ArrayList<>();
        }
        return collectGregTechMultiblockControllers(level.getTileEntities());
    }

    public static List<TileEntity> collectGregTechMultiblockControllers(@Nullable Collection<TileEntity> tileEntities) {
        List<TileEntity> controllers = new ArrayList<>();
        if (tileEntities == null || tileEntities.isEmpty()) {
            return controllers;
        }
        for (TileEntity tileEntity : tileEntities) {
            if (GregTechHelpers.isMultiblockController(tileEntity)) {
                controllers.add(tileEntity);
            }
        }
        return controllers;
    }

    public static void synchronizePreviewState(@Nullable GuidebookLevel level) {
        if (level == null) {
            return;
        }
        Collection<TileEntity> tileEntities = level.getTileEntities();
        if (tileEntities == null || tileEntities.isEmpty()) {
            return;
        }
        for (TileEntity controller : tileEntities) {
            if (!GregTechHelpers.isMultiblockController(controller)) {
                continue;
            }
            boolean activeController = shouldActivateControllerPreview(level, controller);
            if (!activeController) {
                continue;
            }
            GregTechHelpers.synchronizeMultiblockPreviewState(controller, null, true, null);
        }
    }

    public static boolean shouldActivateControllerPreview(@Nullable GuidebookLevel level,
        @Nullable TileEntity controller) {
        if (level == null || controller == null) {
            return false;
        }
        return !ScenePreviewFormedState
            .shouldSkipFormedSync(level, controller.xCoord, controller.yCoord, controller.zCoord);
    }
}
