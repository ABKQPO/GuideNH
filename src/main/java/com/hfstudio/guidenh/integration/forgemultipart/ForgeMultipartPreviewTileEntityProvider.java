package com.hfstudio.guidenh.integration.forgemultipart;

import net.minecraft.block.Block;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.integration.api.PreviewTileEntityProvider;

public class ForgeMultipartPreviewTileEntityProvider implements PreviewTileEntityProvider {

    public ForgeMultipartPreviewTileEntityProvider() {}

    @Override
    @Nullable
    public TileEntity loadPreviewTile(@Nullable World world, Block block, int meta, int x, int y, int z,
        @Nullable NBTTagCompound tag) {
        return ForgeMultipartHelpers.loadPreviewTile(world, block, meta, x, y, z, tag);
    }
}
