package com.hfstudio.guidenh.guide.scene.support;

import net.minecraft.block.Block;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.compat.Mods;
import com.hfstudio.guidenh.compat.forgemultipart.ForgeMultipartHelpers;

/**
 * Backwards-compatible facade. Implementation lives in
 * {@link com.hfstudio.guidenh.compat.forgemultipart.ForgeMultipartHelpers}.
 */
public final class GuideForgeMultipartSupport {

    private GuideForgeMultipartSupport() {}

    public static boolean isForgeMultipartBlock(@Nullable Block block) {
        return Mods.ForgeMultipart.isModLoaded() && ForgeMultipartHelpers.isForgeMultipartBlock(block);
    }

    public static boolean isMultipartTileEntity(@Nullable TileEntity tileEntity) {
        return Mods.ForgeMultipart.isModLoaded() && ForgeMultipartHelpers.isMultipartTileEntity(tileEntity);
    }

    public static boolean isClientMultipartTileEntity(@Nullable TileEntity tileEntity) {
        return Mods.ForgeMultipart.isModLoaded() && ForgeMultipartHelpers.isClientMultipartTileEntity(tileEntity);
    }

    @Nullable
    public static TileEntity ensureClientMultipartTile(@Nullable TileEntity tileEntity) {
        return ForgeMultipartHelpers.ensureClientMultipartTile(tileEntity);
    }

    public static boolean isSavedMultipartTag(@Nullable NBTTagCompound tag) {
        return ForgeMultipartHelpers.isSavedMultipartTag(tag);
    }

    @Nullable
    public static TileEntity loadPreviewTile(World world, Block block, int meta, int x, int y, int z,
        @Nullable NBTTagCompound tag) {
        return ForgeMultipartHelpers.loadPreviewTile(world, block, meta, x, y, z, tag);
    }

    @Nullable
    public static TileEntity finalizePreviewTile(@Nullable TileEntity tileEntity) {
        return ForgeMultipartHelpers.finalizePreviewTile(tileEntity);
    }

    public static boolean renderWorldBlock(@Nullable RenderBlocks renderBlocks, @Nullable IBlockAccess blockAccess,
        @Nullable Block block, int x, int y, int z) {
        return ForgeMultipartHelpers.renderWorldBlock(renderBlocks, blockAccess, block, x, y, z);
    }

    /**
     * Resolves the primary microblock material from a ForgeMultipart tile entity and returns a
     * {@code modid:blockname} or {@code modid:blockname:meta} string for scene export.
     * Returns {@code null} when ForgeMultipart is not loaded or no suitable material was found.
     */
    @Nullable
    public static String resolvePrimaryMicroblockId(@Nullable TileEntity tileEntity) {
        return ForgeMultipartHelpers.resolvePrimaryMicroblockId(tileEntity);
    }
}
