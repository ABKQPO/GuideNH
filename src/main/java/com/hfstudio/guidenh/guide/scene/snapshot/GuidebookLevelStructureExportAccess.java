package com.hfstudio.guidenh.guide.scene.snapshot;

import net.minecraft.block.Block;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.guide.scene.level.GuidebookLevel;

public final class GuidebookLevelStructureExportAccess implements StructureExportAccess {

    private final GuidebookLevel level;

    public GuidebookLevelStructureExportAccess(GuidebookLevel level) {
        this.level = level;
    }

    @Override
    public Block getBlock(int x, int y, int z) {
        return level.getBlock(x, y, z);
    }

    @Override
    public int getBlockMetadata(int x, int y, int z) {
        return level.getBlockMetadata(x, y, z);
    }

    @Override
    public TileEntity getTileEntity(int x, int y, int z) {
        return level.getTileEntity(x, y, z);
    }

    @Override
    public String getBlockId(int x, int y, int z, Block block) {
        String explicitBlockId = level.getExplicitBlockId(x, y, z);
        return explicitBlockId != null && !explicitBlockId.isEmpty() ? explicitBlockId
            : Block.blockRegistry.getNameForObject(block);
    }

    @Override
    @Nullable
    public World getSourceWorld() {
        return null;
    }
}
