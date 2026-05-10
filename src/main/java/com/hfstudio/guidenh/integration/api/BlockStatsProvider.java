package com.hfstudio.guidenh.integration.api;

import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;

import com.hfstudio.guidenh.guide.scene.level.GuidebookLevel;
import com.hfstudio.guidenh.guide.scene.support.GuideBlockStatsStackResolver;

public interface BlockStatsProvider {

    void appendBlockStatsEntries(GuidebookLevel level, Block block, TileEntity tileEntity, int x, int y, int z,
        AxisAlignedBB fallbackBounds, List<GuideBlockStatsStackResolver.ResolvedStack> output);
}
