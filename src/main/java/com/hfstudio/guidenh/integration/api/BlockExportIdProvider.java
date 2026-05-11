package com.hfstudio.guidenh.integration.api;

import net.minecraft.block.Block;
import net.minecraft.tileentity.TileEntity;

import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.guide.scene.level.GuidebookLevel;

public interface BlockExportIdProvider {

    @Nullable
    String resolveExportId(GuidebookLevel level, Block block, @Nullable TileEntity tileEntity, int x, int y, int z,
        @Nullable String fallbackId);
}
