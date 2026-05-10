package com.hfstudio.guidenh.integration.forgemultipart;

import net.minecraft.tileentity.TileEntity;

import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.guide.scene.level.GuidebookLevel;
import com.hfstudio.guidenh.integration.api.PreviewTileEntityFinalizer;

public class ForgeMultipartPreviewTileEntityFinalizer implements PreviewTileEntityFinalizer {

    public ForgeMultipartPreviewTileEntityFinalizer() {}

    @Override
    @Nullable
    public TileEntity finalizePreviewTile(GuidebookLevel level, int x, int y, int z, @Nullable TileEntity tileEntity) {
        return ForgeMultipartHelpers.finalizePreviewTile(tileEntity);
    }
}
