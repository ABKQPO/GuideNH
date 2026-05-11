package com.hfstudio.guidenh.integration.ae2;

import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;

import com.hfstudio.guidenh.guide.scene.level.GuidebookLevel;
import com.hfstudio.guidenh.guide.scene.support.GuideBlockStatsStackResolver;
import com.hfstudio.guidenh.integration.Mods;
import com.hfstudio.guidenh.integration.api.BlockStatsProvider;

public class Ae2BlockStatsProvider implements BlockStatsProvider {

    public Ae2BlockStatsProvider() {}

    @Override
    public void appendBlockStatsEntries(GuidebookLevel level, Block block, TileEntity tileEntity, int x, int y, int z,
        AxisAlignedBB fallbackBounds, List<GuideBlockStatsStackResolver.ResolvedStack> output) {
        if (Mods.AE2.isModLoaded()) {
            Ae2Helpers.appendCableBusStatEntries(tileEntity, output, x, y, z);
        }
    }
}
