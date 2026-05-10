package com.hfstudio.guidenh.integration.buildcraft;

import net.minecraft.block.Block;
import net.minecraft.item.ItemStack;
import net.minecraft.util.MovingObjectPosition;

import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.guide.scene.level.GuidebookLevel;
import com.hfstudio.guidenh.integration.Mods;
import com.hfstudio.guidenh.integration.api.BlockDisplayProvider;

public class BuildCraftBlockDisplayProvider implements BlockDisplayProvider {

    @Override
    @Nullable
    public ItemStack resolveDisplayStack(GuidebookLevel level, Block block, int x, int y, int z,
        @Nullable MovingObjectPosition target) {
        return Mods.BuildCraftTransport.isModLoaded()
            ? BuildCraftHelpers.resolveDisplayStack(level, block, x, y, z, target)
            : null;
    }
}
