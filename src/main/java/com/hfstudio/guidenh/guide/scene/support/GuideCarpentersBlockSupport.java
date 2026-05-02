package com.hfstudio.guidenh.guide.scene.support;

import net.minecraft.block.Block;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.MovingObjectPosition;

import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.compat.carpentersblocks.CarpentersBlocksHelpers;
import com.hfstudio.guidenh.guide.scene.level.GuidebookLevel;

/**
 * Backwards-compatible facade. Implementation lives in
 * {@link com.hfstudio.guidenh.compat.carpentersblocks.CarpentersBlocksHelpers}.
 */
public final class GuideCarpentersBlockSupport {

    private GuideCarpentersBlockSupport() {}

    public static boolean isCarpentersBlock(@Nullable Block block) {
        return CarpentersBlocksHelpers.isCarpentersBlock(block);
    }

    @Nullable
    public static ItemStack resolveDisplayStack(GuidebookLevel level, Block block, int x, int y, int z,
        @Nullable MovingObjectPosition target) {
        if (!CarpentersBlocksHelpers.isCarpentersBlock(block)) {
            return null;
        }

        TileEntity tileEntity = level.getTileEntity(x, y, z);
        if (!CarpentersBlocksHelpers.isCarpentersTile(tileEntity)) {
            return null;
        }

        ItemStack featureSensitiveStack = CarpentersBlocksHelpers.resolveFeatureSensitiveStack(tileEntity, target);
        if (featureSensitiveStack != null) {
            return featureSensitiveStack;
        }

        int preferredSide = CarpentersBlocksHelpers.resolvePreferredSide(target);
        ItemStack preferredStack = CarpentersBlocksHelpers.resolveCoverStack(tileEntity, preferredSide);
        if (preferredStack != null) {
            return preferredStack;
        }

        return preferredSide != CarpentersBlocksHelpers.BASE_COVER_SIDE
            ? CarpentersBlocksHelpers.resolveCoverStack(tileEntity, CarpentersBlocksHelpers.BASE_COVER_SIDE)
            : null;
    }
}
