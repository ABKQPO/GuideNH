package com.hfstudio.guidenh.guide.internal.structure;

import net.minecraft.block.Block;
import net.minecraft.tileentity.TileEntity;

import org.jetbrains.annotations.Nullable;

public interface GuideStructurePlacementTarget {

    void placeBlock(int x, int y, int z, Block block, int meta, @Nullable TileEntity tileEntity);
}
