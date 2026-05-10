package com.hfstudio.guidenh.integration.api;

import net.minecraft.block.Block;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

import org.jetbrains.annotations.Nullable;

public interface PreviewTileEntityProvider {

    @Nullable
    TileEntity loadPreviewTile(@Nullable World world, Block block, int meta, int x, int y, int z,
        @Nullable NBTTagCompound tag);
}
