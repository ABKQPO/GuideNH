package com.hfstudio.guidenh.integration.api.client;

import net.minecraft.block.Block;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.IBlockAccess;

import org.jetbrains.annotations.Nullable;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public interface PreviewBlockRenderProvider {

    @Nullable
    TileEntity promoteTileEntity(Block block, TileEntity tileEntity);

    boolean tryRenderWorldBlock(RenderBlocks renderBlocks, IBlockAccess blockAccess, Block block, int x, int y, int z);
}
