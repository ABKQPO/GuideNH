package com.hfstudio.guidenh.integration.carpentersblocks;

import net.minecraft.block.Block;
import net.minecraft.util.MovingObjectPosition;

import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.guide.scene.level.GuidebookLevel;
import com.hfstudio.guidenh.guide.scene.support.GuideBlockDisplayResolver;
import com.hfstudio.guidenh.integration.api.BlockDisplayNameProvider;

public class CarpentersBlocksBlockDisplayNameProvider implements BlockDisplayNameProvider {

    public CarpentersBlocksBlockDisplayNameProvider() {}

    @Override
    @Nullable
    public String resolveDisplayName(GuidebookLevel level, Block block, int x, int y, int z,
        @Nullable MovingObjectPosition target) {
        return CarpentersBlocksHelpers.isCarpentersBlock(block)
            ? GuideBlockDisplayResolver.resolveIntrinsicBlockDisplayName(level, block, x, y, z)
            : null;
    }
}
