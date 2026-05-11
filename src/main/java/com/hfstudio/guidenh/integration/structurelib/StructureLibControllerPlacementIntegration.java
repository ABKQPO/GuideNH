package com.hfstudio.guidenh.integration.structurelib;

import java.util.List;

import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.guide.scene.level.GuidebookLevel;

public interface StructureLibControllerPlacementIntegration {

    @Nullable
    TileEntity placeController(GuidebookLevel level, World world,
        StructureLibRuntimeFacade.ResolvedController controller, List<String> warnings);
}
