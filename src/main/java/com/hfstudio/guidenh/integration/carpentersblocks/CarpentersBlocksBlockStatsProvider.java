package com.hfstudio.guidenh.integration.carpentersblocks;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;

import com.hfstudio.guidenh.guide.scene.level.GuidebookLevel;
import com.hfstudio.guidenh.guide.scene.support.GuideBlockStatsStackResolver;
import com.hfstudio.guidenh.integration.api.BlockStatsProvider;

public class CarpentersBlocksBlockStatsProvider implements BlockStatsProvider {

    public CarpentersBlocksBlockStatsProvider() {}

    @Override
    public void appendBlockStatsEntries(GuidebookLevel level, Block block, TileEntity tileEntity, int x, int y, int z,
        AxisAlignedBB fallbackBounds, List<GuideBlockStatsStackResolver.ResolvedStack> output) {
        if (!CarpentersBlocksHelpers.isCarpentersBlock(block)) {
            return;
        }
        ArrayList<ItemStack> stacks = new ArrayList<>(4);
        CarpentersBlocksHelpers.appendComponentStatStacks(tileEntity, stacks);
        appendWithFallbackBounds(stacks, fallbackBounds, output);
    }

    public void appendWithFallbackBounds(List<ItemStack> stacks, AxisAlignedBB fallbackBounds,
        List<GuideBlockStatsStackResolver.ResolvedStack> output) {
        for (ItemStack stack : stacks) {
            output.add(new GuideBlockStatsStackResolver.ResolvedStack(stack, fallbackBounds));
        }
    }
}
