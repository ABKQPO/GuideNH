package com.hfstudio.guidenh.guide.scene.snapshot;

import net.minecraft.block.Block;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

import org.jetbrains.annotations.Nullable;

/**
 * Read-only access to blocks/tiles during structure SNBT export (world or guidebook level).
 */
public interface StructureExportAccess {

    Block getBlock(int x, int y, int z);

    int getBlockMetadata(int x, int y, int z);

    @Nullable
    TileEntity getTileEntity(int x, int y, int z);

    @Nullable
    String getBlockId(int x, int y, int z, Block block);

    /**
     * Source {@link World} when exporting from a live dimension (region wand); {@code null} for guidebook-level
     * fake exports. Used by contributors that resolve logical-server tiles or RPC.
     */
    @Nullable
    default World getSourceWorld() {
        return null;
    }
}
