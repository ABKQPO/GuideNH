package com.hfstudio.guidenh.guide.scene.snapshot;

import java.util.Base64;

import net.minecraft.nbt.NBTTagCompound;

import org.jetbrains.annotations.Nullable;

/**
 * Single structure-block root for server authoritative preview supplements. Text-SNBT payloads use Base64(raw bytes).
 */
public final class ServerPreviewSupplementNbt {

    public static final String TAG_ROOT = "guidenh_server_preview_supplement";

    public static final String KEY_WIRE = "v";

    public static final String KEY_B64 = "b64";

    private ServerPreviewSupplementNbt() {}

    /** Current on-disk wire for per-supplement sub-compounds. */
    public static final int STRUCTURE_WIRE_V1 = 1;

    public static void putSupplement(NBTTagCompound structureBlock, String supplementId, byte[] rawPayload) {
        if (rawPayload == null || rawPayload.length == 0) {
            removeSupplement(structureBlock, supplementId);
            return;
        }
        NBTTagCompound root = structureBlock.hasKey(TAG_ROOT, 10)
            ? (NBTTagCompound) structureBlock.getCompoundTag(TAG_ROOT).copy()
            : new NBTTagCompound();
        NBTTagCompound entry = new NBTTagCompound();
        entry.setInteger(KEY_WIRE, STRUCTURE_WIRE_V1);
        entry.setString(KEY_B64, Base64.getEncoder().encodeToString(rawPayload));
        root.setTag(supplementId, entry);
        structureBlock.setTag(TAG_ROOT, root);
    }

    public static void removeSupplement(NBTTagCompound structureBlock, String supplementId) {
        if (!structureBlock.hasKey(TAG_ROOT, 10)) {
            return;
        }
        NBTTagCompound root = (NBTTagCompound) structureBlock.getCompoundTag(TAG_ROOT).copy();
        root.removeTag(supplementId);
        if (root.func_150296_c().isEmpty()) {
            structureBlock.removeTag(TAG_ROOT);
        } else {
            structureBlock.setTag(TAG_ROOT, root);
        }
    }

    @Nullable
    public static byte[] readSupplement(@Nullable NBTTagCompound structureBlock, String supplementId) {
        if (structureBlock == null || !structureBlock.hasKey(TAG_ROOT, 10)) {
            return null;
        }
        NBTTagCompound root = structureBlock.getCompoundTag(TAG_ROOT);
        if (!root.hasKey(supplementId, 10)) {
            return null;
        }
        NBTTagCompound entry = root.getCompoundTag(supplementId);
        if (!entry.hasKey(KEY_B64, 8)) {
            return null;
        }
        try {
            return Base64.getDecoder()
                .decode(entry.getString(KEY_B64));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
