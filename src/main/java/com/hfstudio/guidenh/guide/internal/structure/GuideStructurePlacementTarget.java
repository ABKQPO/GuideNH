package com.hfstudio.guidenh.guide.internal.structure;

import org.jetbrains.annotations.Nullable;

import net.minecraft.block.Block;
import net.minecraft.tileentity.TileEntity;

public interface GuideStructurePlacementTarget {

    void placeBlock(int x, int y, int z, Block block, int meta, @Nullable TileEntity tileEntity);
}
