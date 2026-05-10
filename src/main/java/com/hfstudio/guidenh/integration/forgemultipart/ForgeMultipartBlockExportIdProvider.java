package com.hfstudio.guidenh.integration.forgemultipart;

import net.minecraft.block.Block;
import net.minecraft.tileentity.TileEntity;

import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.guide.scene.level.GuidebookLevel;
import com.hfstudio.guidenh.integration.Mods;
import com.hfstudio.guidenh.integration.api.BlockExportIdProvider;

public class ForgeMultipartBlockExportIdProvider implements BlockExportIdProvider {

    public ForgeMultipartBlockExportIdProvider() {}

    @Override
    @Nullable
    public String resolveExportId(GuidebookLevel level, Block block, @Nullable TileEntity tileEntity, int x, int y,
        int z, @Nullable String fallbackId) {
        if (tileEntity == null || !Mods.ForgeMultipart.isModLoaded()
            || !ForgeMultipartHelpers.isForgeMultipartBlock(block)) {
            return null;
        }
        return ForgeMultipartHelpers.resolvePrimaryMicroblockId(tileEntity);
    }
}
