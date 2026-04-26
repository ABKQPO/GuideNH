package com.hfstudio.guidenh.guide.internal.structure;

import javax.annotation.Nullable;

import net.minecraft.block.Block;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

public final class GuideStructureWorldPlacementTarget implements GuideStructurePlacementTarget {

    private final World world;

    public GuideStructureWorldPlacementTarget(World world) {
        this.world = world;
    }

    @Override
    public void placeBlock(int x, int y, int z, Block block, int meta, @Nullable TileEntity tileEntity) {
        world.setBlock(x, y, z, block, meta, 3);
        if (tileEntity != null) {
            world.setTileEntity(x, y, z, tileEntity);
        }
    }
}
