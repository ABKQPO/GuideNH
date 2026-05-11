package com.hfstudio.guidenh.guide.scene.support;

import net.minecraft.block.Block;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;

import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.integration.gregtech.GregTechHelpers;

/**
 * Backwards-compatible facade. All GregTech integration now lives in
 * {@link GregTechHelpers}.
 */
public class GuideGregTechTileSupport {

    protected GuideGregTechTileSupport() {}

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

    @Nullable
    public static Integer getMetaTileBaseType(int metaTileId) {
        return GregTechHelpers.getMetaTileBaseType(metaTileId);
    }

    public static boolean initializeMetaTile(@Nullable TileEntity tileEntity, int metaTileId,
        @Nullable NBTTagCompound tileTag) {
        return GregTechHelpers.initializeMetaTile(tileEntity, metaTileId, tileTag);
    }

    public static void applyDefaultFacing(@Nullable TileEntity tileEntity, @Nullable NBTTagCompound tileTag) {
        GregTechHelpers.applyDefaultFacing(tileEntity, tileTag);
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
