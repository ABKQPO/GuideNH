package com.hfstudio.guidenh.integration.forgemultipart;

import net.minecraft.block.Block;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.IBlockAccess;

import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.integration.Mods;
import com.hfstudio.guidenh.integration.api.client.PreviewBlockRenderProvider;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class ForgeMultipartPreviewBlockRenderProvider implements PreviewBlockRenderProvider {

    public ForgeMultipartPreviewBlockRenderProvider() {}

    @Override
    @Nullable
    public TileEntity promoteTileEntity(Block block, TileEntity tileEntity) {
        if (!Mods.ForgeMultipart.isModLoaded() || !ForgeMultipartHelpers.isForgeMultipartBlock(block)
            || !ForgeMultipartHelpers.isMultipartTileEntity(tileEntity)
            || ForgeMultipartHelpers.isClientMultipartTileEntity(tileEntity)) {
            return null;
        }
        return ForgeMultipartHelpers.ensureClientMultipartTile(tileEntity);
    }

    @Override
    public boolean tryRenderWorldBlock(RenderBlocks renderBlocks, IBlockAccess blockAccess, Block block, int x, int y,
        int z) {
        if (!Mods.ForgeMultipart.isModLoaded() || !ForgeMultipartHelpers.isForgeMultipartBlock(block)) {
            return false;
        }
        return ForgeMultipartHelpers.renderWorldBlock(renderBlocks, blockAccess, block, x, y, z);
    }
}
