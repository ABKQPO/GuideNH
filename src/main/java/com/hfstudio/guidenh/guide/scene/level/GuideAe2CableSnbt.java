package com.hfstudio.guidenh.guide.scene.level;

import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagByte;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagInt;
import net.minecraft.nbt.NBTTagLong;
import net.minecraft.nbt.NBTTagShort;

import org.jetbrains.annotations.Nullable;

/**
 * AE2 cable preview sidecar inside structure SNBT {@code blocks[]} compounds (parallel to {@code nbt}).
 */
public final class GuideAe2CableSnbt {

    public static final String TAG_ROOT = "guidenh_ae2CableStream_v1";

    public static final String KEY_CS = "cs";

    public static final String KEY_SIDE_OUT = "sideOut";

    private GuideAe2CableSnbt() {}

    /**
     * Structure SNBT round-trip often turns bytes into ints; {@link NBTTagCompound#getByte(String)} yields 0 on int tags.
     */
    private static int readCsUnsigned(NBTTagCompound ext) {
        if (!ext.hasKey(KEY_CS)) {
            return 0;
        }
        NBTBase raw = ext.getTag(KEY_CS);
        if (raw instanceof NBTTagByte) {
            return ext.getByte(KEY_CS) & 0xFF;
        }
        if (raw instanceof NBTTagShort) {
            return ext.getShort(KEY_CS) & 0xFF;
        }
        if (raw instanceof NBTTagInt) {
            return ext.getInteger(KEY_CS) & 0xFF;
        }
        return 0;
    }

    private static int readSideOut(NBTTagCompound ext) {
        if (!ext.hasKey(KEY_SIDE_OUT)) {
            return 0;
        }
        NBTBase raw = ext.getTag(KEY_SIDE_OUT);
        if (raw instanceof NBTTagInt) {
            return ext.getInteger(KEY_SIDE_OUT);
        }
        if (raw instanceof NBTTagLong) {
            return (int) ext.getLong(KEY_SIDE_OUT);
        }
        if (raw instanceof NBTTagByte) {
            return ext.getByte(KEY_SIDE_OUT) & 0xFF;
        }
        if (raw instanceof NBTTagShort) {
            return ext.getShort(KEY_SIDE_OUT) & 0xFFFF;
        }
        return 0;
    }

    public static void applySidecar(GuidebookLevel level, int x, int y, int z, @Nullable NBTTagCompound structureBlock) {
        if (structureBlock != null && structureBlock.hasKey(TAG_ROOT, 10)) {
            NBTTagCompound ext = structureBlock.getCompoundTag(TAG_ROOT);
            int csUnsigned = readCsUnsigned(ext);
            int sideOut = readSideOut(ext);
            level.putExportedAe2CableStream(x, y, z, (byte) csUnsigned, sideOut);
        } else {
            level.removeExportedAe2CableStream(x, y, z);
        }
    }
}
