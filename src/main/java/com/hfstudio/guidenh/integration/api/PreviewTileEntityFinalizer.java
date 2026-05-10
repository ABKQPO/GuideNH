package com.hfstudio.guidenh.integration.api;

import net.minecraft.tileentity.TileEntity;

import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.guide.scene.level.GuidebookLevel;

public interface PreviewTileEntityFinalizer {

    @Nullable
    TileEntity finalizePreviewTile(GuidebookLevel level, int x, int y, int z, @Nullable TileEntity tileEntity);
}
