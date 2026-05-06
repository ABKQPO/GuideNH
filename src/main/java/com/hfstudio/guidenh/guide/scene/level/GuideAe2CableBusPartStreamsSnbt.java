package com.hfstudio.guidenh.guide.scene.level;

import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagByte;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagInt;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagShort;
import net.minecraft.nbt.NBTTagString;

import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.guide.internal.structure.GuideTextNbtCodec;

/**
 * AE2 cable-bus side part stream sidecar inside structure SNBT {@code blocks[]} compounds (parallel to {@code nbt}).
 */
public final class GuideAe2CableBusPartStreamsSnbt {

    public static final String TAG_ROOT = "guidenh_ae2CableBusPartStreams_v1";

    private static final String KEY_PREFIX = "d";

    private GuideAe2CableBusPartStreamsSnbt() {}

    public static void applySidecar(GuidebookLevel level, int x, int y, int z, @Nullable NBTTagCompound structureBlock) {
        ExportedAe2CableBusPartStreams decoded = readFromStructureBlock(structureBlock);
        if (decoded == null || decoded.isEmpty()) {
            level.removeExportedAe2CableBusPartStreams(x, y, z);
        } else {
            level.putExportedAe2CableBusPartStreams(x, y, z, decoded);
        }
    }

    public static void writeToStructureBlock(NBTTagCompound structureBlockTag,
        @Nullable ExportedAe2CableBusPartStreams streams) {
        if (streams == null || streams.isEmpty()) {
            structureBlockTag.removeTag(TAG_ROOT);
            return;
        }
        NBTTagCompound ext = new NBTTagCompound();
        for (int o = 0; o < 6; o++) {
            byte[] chunk = streams.getSlot(o);
            if (chunk != null && chunk.length > 0) {
                // Text SNBT round-trip turns byte[] into TAG_List; int[] survives as [I;…] in 1.7.10 JsonToNBT.
                ext.setIntArray(KEY_PREFIX + o, GuideTextNbtCodec.toIntArray(chunk));
            }
        }
        structureBlockTag.setTag(TAG_ROOT, ext);
    }

    @Nullable
    static ExportedAe2CableBusPartStreams readFromStructureBlock(@Nullable NBTTagCompound structureBlock) {
        if (structureBlock == null || !structureBlock.hasKey(TAG_ROOT, 10)) {
            return null;
        }
        NBTTagCompound ext = structureBlock.getCompoundTag(TAG_ROOT);
        byte[][] slots = new byte[6][];
        boolean any = false;
        for (int o = 0; o < 6; o++) {
            String key = KEY_PREFIX + o;
            byte[] chunk = readSlotPayload(ext, key);
            if (chunk != null && chunk.length > 0) {
                slots[o] = chunk;
                any = true;
            }
        }
        return any ? new ExportedAe2CableBusPartStreams(slots) : null;
    }

    /**
     * SNBT / text codecs often drop raw {@code byte[]}; slot payloads may appear as int[], list of numeric tags, or
     * {@link GuidebookPreviewBlockPlacer#BYTE_ARRAY_WRAPPER_TAG} compounds.
     */
    @Nullable
    private static byte[] readSlotPayload(NBTTagCompound ext, String key) {
        if (!ext.hasKey(key)) {
            return null;
        }
        if (ext.hasKey(key, 7)) {
            byte[] direct = ext.getByteArray(key);
            return direct != null && direct.length > 0 ? direct : null;
        }
        if (ext.hasKey(key, 11)) {
            int[] ia = ext.getIntArray(key);
            if (ia != null && ia.length > 0) {
                return GuidebookPreviewBlockPlacer.toByteArray(ia);
            }
        }
        NBTBase raw = ext.getTag(key);
        if (raw instanceof NBTTagCompound compound) {
            byte[] wrapped = GuidebookPreviewBlockPlacer.decodeWrappedByteArray(compound);
            if (wrapped != null && wrapped.length > 0) {
                return wrapped;
            }
        }
        // Prefer homogeneous typed lists (TAG_Byte / TAG_Int / …): JsonToNBT may also emit TAG_String lists that
        // tryDecodeLegacyByteArray mis-decodes as numeric strings — wrong bytes leave unread tail on readFromStream.
        if (raw instanceof NBTTagList listFirst) {
            byte[] fromTypedList = decodeAe2PartStreamList(listFirst);
            if (fromTypedList != null && fromTypedList.length > 0) {
                return fromTypedList;
            }
        }
        byte[] legacy = GuidebookPreviewBlockPlacer.tryDecodeLegacyByteArray(raw, false);
        if (legacy != null && legacy.length > 0) {
            return legacy;
        }
        return null;
    }

    @Nullable
    private static byte[] decodeAe2PartStreamList(NBTTagList list) {
        int n = list.tagCount();
        if (n <= 0) {
            return null;
        }
        NBTTagList remaining = (NBTTagList) list.copy();
        byte[] out = new byte[n];
        for (int i = 0; i < n; i++) {
            NBTBase el = remaining.removeTag(0);
            if (el instanceof NBTTagByte tb) {
                out[i] = (byte) tb.func_150287_d();
            } else if (el instanceof NBTTagShort sh) {
                out[i] = (byte) sh.func_150289_e();
            } else if (el instanceof NBTTagInt it) {
                out[i] = (byte) it.func_150287_d();
            } else if (el instanceof NBTTagString st) {
                try {
                    out[i] = Byte.parseByte(GuidebookPreviewBlockPlacer.trimNumericSuffix(st.func_150285_a_()));
                } catch (NumberFormatException ignored) {
                    return null;
                }
            } else {
                return null;
            }
        }
        return out;
    }
}
