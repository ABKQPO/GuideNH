package com.hfstudio.guidenh.integration.structurelib;

import java.util.List;

import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;

public interface StructureLibPreviewStateSynchronizer {

    void synchronizePreviewState(TileEntity controllerTile, ItemStack triggerStack,
        StructureLibPreviewSelection selection, List<String> warnings);
}
