package com.hfstudio.guidenh.guide.scene.support;

import net.minecraft.block.Block;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;

import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.compat.gregtech.GregTechHelpers;

/**
 * Backwards-compatible facade. All GregTech integration now lives in
 * {@link com.hfstudio.guidenh.compat.gregtech.GregTechHelpers}.
 */
public final class GuideGregTechTileSupport {

    private GuideGregTechTileSupport() {}

    public static boolean isGregTechTileEntity(@Nullable TileEntity tileEntity) {
        return GregTechHelpers.isGregTechTileEntity(tileEntity);
    }

    public static int resolveMetaTileId(@Nullable TileEntity tileEntity, int fallback) {
        return GregTechHelpers.resolveMetaTileId(tileEntity, fallback);
    }

    public static boolean hasValidMetaTileBinding(@Nullable TileEntity tileEntity) {
        return GregTechHelpers.hasValidMetaTileBinding(tileEntity);
    }

    public static boolean repairMetaTileBinding(@Nullable TileEntity tileEntity) {
        return GregTechHelpers.repairMetaTileBinding(tileEntity);
    }

    public static void logInfoOnce(String key, String message, Object... args) {
        GregTechHelpers.logInfoOnce(key, message, args);
    }

    public static String describeBlock(@Nullable Block block) {
        return GregTechHelpers.describeBlock(block);
    }

    public static String describeTileTag(@Nullable NBTTagCompound tileTag) {
        return GregTechHelpers.describeTileTag(tileTag);
    }

    public static String describeTile(@Nullable TileEntity tileEntity) {
        return GregTechHelpers.describeTile(tileEntity);
    }
}
