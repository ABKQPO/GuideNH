package com.hfstudio.guidenh.integration.tinkerconstruct;

import net.minecraft.tileentity.TileEntity;

import com.hfstudio.guidenh.guide.scene.level.GuidebookLevel;
import com.hfstudio.guidenh.integration.Mods;

import cpw.mods.fml.common.Optional;
import tconstruct.smeltery.logic.SmelteryLogic;

public class TinkersConstructHelpers {

    public static void prepareSmelteryPreview(GuidebookLevel level) {
        if (!Mods.TinkersConstruct.isModLoaded()) {
            return;
        }
        try {
            prepareSmelteryPreviewImpl(level);
        } catch (Throwable ignored) {}
    }

    @Optional.Method(modid = "TConstruct")
    private static void prepareSmelteryPreviewImpl(GuidebookLevel level) {
        for (TileEntity te : level.getTileEntities()) {
            if (!(te instanceof SmelteryLogic logic)) {
                continue;
            }
            if (logic.validStructure) {
                continue;
            }
            logic.checkValidPlacement();
        }
    }
}
