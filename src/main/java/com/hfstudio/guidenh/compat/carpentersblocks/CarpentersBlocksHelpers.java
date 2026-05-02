package com.hfstudio.guidenh.compat.carpentersblocks;

import net.minecraft.block.Block;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.MovingObjectPosition;
import net.minecraftforge.common.util.ForgeDirection;

import org.jetbrains.annotations.Nullable;

import com.carpentersblocks.tileentity.TEBase;
import com.carpentersblocks.util.BlockProperties;
import com.hfstudio.guidenh.compat.Mods;

import cpw.mods.fml.common.Optional;

public final class CarpentersBlocksHelpers {

    public static final String CARPENTERS_BLOCK_PACKAGE = "com.carpentersblocks.block.";
    public static final String CARPENTERS_TILE_PACKAGE = "com.carpentersblocks.tileentity.";
    public static final int BASE_COVER_SIDE = 6;

    private CarpentersBlocksHelpers() {}

    public static boolean isCarpentersBlock(@Nullable Block block) {
        if (block == null || !Mods.CarpentersBlocks.isModLoaded()) {
            return false;
        }
        for (Class<?> type = block.getClass(); type != null; type = type.getSuperclass()) {
            String name = type.getName();
            if (name != null && name.startsWith(CARPENTERS_BLOCK_PACKAGE)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isCarpentersTile(@Nullable TileEntity tileEntity) {
        return tileEntity != null && Mods.CarpentersBlocks.isModLoaded() && isCarpentersTileImpl(tileEntity);
    }

    @Optional.Method(modid = "CarpentersBlocks")
    private static boolean isCarpentersTileImpl(TileEntity tileEntity) {
        return tileEntity instanceof TEBase;
    }

    public static int resolvePreferredSide(@Nullable MovingObjectPosition target) {
        return target != null && target.sideHit >= 0 && target.sideHit < BASE_COVER_SIDE ? target.sideHit
            : BASE_COVER_SIDE;
    }

    public static ForgeDirection resolveForgeDirection(@Nullable MovingObjectPosition target) {
        return target != null && target.sideHit >= 0 && target.sideHit < BASE_COVER_SIDE
            ? ForgeDirection.getOrientation(target.sideHit)
            : ForgeDirection.UNKNOWN;
    }

    @Nullable
    public static ItemStack resolveFeatureSensitiveStack(@Nullable TileEntity tileEntity,
        @Nullable MovingObjectPosition target) {
        if (!isCarpentersTile(tileEntity)) {
            return null;
        }
        try {
            return resolveFeatureSensitiveStackImpl(tileEntity, resolveForgeDirection(target));
        } catch (Throwable ignored) {
            return null;
        }
    }

    @Optional.Method(modid = "CarpentersBlocks")
    @Nullable
    private static ItemStack resolveFeatureSensitiveStackImpl(TileEntity tileEntity, ForgeDirection direction) {
        ItemStack value = BlockProperties.getFeatureSensitiveSideItemStack((TEBase) tileEntity, direction);
        return value != null ? value.copy() : null;
    }

    @Nullable
    public static ItemStack resolveCoverStack(@Nullable TileEntity tileEntity, int sideIndex) {
        if (!isCarpentersTile(tileEntity) || sideIndex < 0 || sideIndex > BASE_COVER_SIDE) {
            return null;
        }
        try {
            return resolveCoverStackImpl(tileEntity, sideIndex);
        } catch (Throwable ignored) {
            return null;
        }
    }

    @Optional.Method(modid = "CarpentersBlocks")
    @Nullable
    private static ItemStack resolveCoverStackImpl(TileEntity tileEntity, int sideIndex) {
        TEBase te = (TEBase) tileEntity;
        byte attribute = TEBase.ATTR_COVER[sideIndex];
        if (!te.hasAttribute(attribute)) {
            return null;
        }
        ItemStack value = te.getAttribute(attribute);
        return value != null ? value.copy() : null;
    }
}
