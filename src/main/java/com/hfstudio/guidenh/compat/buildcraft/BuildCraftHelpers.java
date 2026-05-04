package com.hfstudio.guidenh.compat.buildcraft;

import net.minecraft.block.Block;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;

import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.guide.scene.level.GuidebookLevel;
import com.hfstudio.guidenh.mixins.late.compat.buildcraft.AccessorTileGenericPipe;

import buildcraft.transport.BlockGenericPipe;
import buildcraft.transport.Pipe;
import buildcraft.transport.TileGenericPipe;
import cpw.mods.fml.common.Optional;

public final class BuildCraftHelpers {

    /**
     * Resolves the display {@link ItemStack} for a BuildCraft pipe at the given position.
     * Returns {@code null} when the block is not a {@code BlockGenericPipe}, the tile is
     * absent, or the pipe object has not been bound.
     */
    @Nullable
    @Optional.Method(modid = "BuildCraft|Transport")
    public static ItemStack resolveDisplayStack(GuidebookLevel level, Block block, int x, int y, int z) {
        if (!(block instanceof BlockGenericPipe)) {
            return null;
        }
        TileEntity tile = level.getTileEntity(x, y, z);
        if (!isTileGenericPipe(tile)) {
            return null;
        }
        AccessorTileGenericPipe accessor = (AccessorTileGenericPipe) tile;
        Pipe<?> pipe = accessor.getPipe();
        if (pipe == null || pipe.item == null) {
            return null;
        }
        return new ItemStack(pipe.item, 1, accessor.getItemMetadata());
    }

    /**
     * Calls the protected {@code refreshRenderState()} method on a {@code TileGenericPipe}
     * so that the {@code textureMatrix} is populated from the bound pipe's icon indices.
     * Silently ignored when the tile is not a pipe, the method cannot be reached, or the
     * pipe object has not been bound.
     */
    @Optional.Method(modid = "BuildCraft|Transport")
    public static void initializePipeRenderState(TileEntity tile) {
        if (!isTileGenericPipe(tile)) {
            return;
        }
        AccessorTileGenericPipe accessor = (AccessorTileGenericPipe) tile;
        if (accessor.getPipe() == null) {
            return;
        }
        accessor.refreshRenderState();
    }

    /**
     * Iterates all tile entities in the level and calls {@link #initializePipeRenderState}
     * on each so render textures reflect the correct pipe type.
     */
    @Optional.Method(modid = "BuildCraft|Transport")
    public static void prepare(GuidebookLevel level) {
        for (TileEntity tile : level.getTileEntities()) {
            initializePipeRenderState(tile);
        }
    }

    @Optional.Method(modid = "BuildCraft|Transport")
    public static boolean isTileGenericPipe(@Nullable TileEntity tile) {
        return tile instanceof TileGenericPipe;
    }

}
