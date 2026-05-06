package com.hfstudio.guidenh.guide.scene.snapshot;

import net.minecraft.block.Block;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

import org.jetbrains.annotations.Nullable;

public final class WorldStructureExportAccess implements StructureExportAccess {

    private final World world;

    public WorldStructureExportAccess(World world) {
        this.world = world;
    }

    @Override
    public Block getBlock(int x, int y, int z) {
        return world.getBlock(x, y, z);
    }

    @Override
    public int getBlockMetadata(int x, int y, int z) {
        return world.getBlockMetadata(x, y, z);
    }

    @Override
    public TileEntity getTileEntity(int x, int y, int z) {
        return world.getTileEntity(x, y, z);
    }

    @Override
    public String getBlockId(int x, int y, int z, Block block) {
        return Block.blockRegistry.getNameForObject(block);
    }

    @Override
    public World getSourceWorld() {
        return world;
    }
}
