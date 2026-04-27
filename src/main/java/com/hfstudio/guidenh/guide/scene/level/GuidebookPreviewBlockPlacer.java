package com.hfstudio.guidenh.guide.scene.level;

import javax.annotation.Nullable;

import net.minecraft.block.Block;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class GuidebookPreviewBlockPlacer {

    private static final Logger LOG = LogManager.getLogger("GuideNH/ScenePreview");

    private GuidebookPreviewBlockPlacer() {}

    public static void place(GuidebookLevel level, int x, int y, int z, Block block, int meta,
        @Nullable NBTTagCompound tileTag) {
        place(level, x, y, z, block, meta, tileTag, null);
    }

    public static void place(GuidebookLevel level, int x, int y, int z, Block block, int meta,
        @Nullable NBTTagCompound tileTag, @Nullable String explicitBlockId) {
        TileEntity tileEntity = null;
        if (tileTag != null || block.hasTileEntity(meta)) {
            try {
                tileEntity = GuidebookTileEntityLoader
                    .load(level.getOrCreateFakeWorld(), block, meta, x, y, z, tileTag);
            } catch (Throwable t) {
                LOG.warn("Preview tile entity load failed, falling back to block-only placement", t);
            }
        }
        level.setBlock(x, y, z, block, meta, tileEntity);
        level.setExplicitBlockId(x, y, z, explicitBlockId);
    }
}
