package com.hfstudio.guidenh.guide.internal.structure;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.zip.GZIPInputStream;

import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.JsonToNBT;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;

/**
 * 1.7.10's text-NBT reader/writer cannot round-trip compound keys containing {@code :} (AE's
 * {@code def:6}/{@code extra:2}/{@code facade:5} are the main example). This codec rewrites unsafe
 * compound keys into a list-backed representation before stringification, then restores the original
 * keys after parsing.
 */
public class GuideTextNbtCodec {

    private static final String ENCODED_KEYS_TAG = "__guidenh_encoded_keys_v1";
    private static final String ENTRY_KEY_TAG = "k";
    private static final String ENTRY_VALUE_TAG = "v";

    private GuideTextNbtCodec() {}

    public static String writeTextSafeCompound(NBTTagCompound tag) {
        return encodeCompound(tag).toString();
    }

    public static NBTTagCompound readTextSafeCompound(String text) throws Exception {
        String normalized = text;
        if (!normalized.isEmpty() && normalized.charAt(0) == '\uFEFF') {
            normalized = normalized.substring(1);
        }

        NBTBase parsed = JsonToNBT.func_150315_a(normalized);
        if (parsed instanceof NBTTagCompound compound) {
            return decodeCompound(compound);
        }

        throw new IllegalStateException("SNBT root must be a Compound");
    }

    public static String writeStructureSnbt(NBTTagCompound root) {
        NBTTagCompound copy = (NBTTagCompound) root.copy();
        rewriteStructureTileTags(copy, true);
        return copy.toString();
    }

    public static NBTTagCompound readStructureNbt(byte[] data) throws Exception {
        NBTTagCompound root;
        if (looksLikeText(data)) {
            String text = new String(data, StandardCharsets.UTF_8);
            root = readTextSafeCompound(text);
        } else {
            try (GZIPInputStream gzip = new GZIPInputStream(new ByteArrayInputStream(data));
                DataInputStream input = new DataInputStream(gzip)) {
                root = CompressedStreamTools.read(input);
            } catch (Exception ignored) {
                try (DataInputStream input = new DataInputStream(new ByteArrayInputStream(data))) {
                    root = CompressedStreamTools.read(input);
                }
            }
        }

        rewriteStructureTileTags(root, false);
        return root;
    }

    public static NBTTagCompound decodeCompound(NBTTagCompound tag) {
        NBTTagCompound decoded = new NBTTagCompound();
        ArrayList<String> keys = new ArrayList<>(tag.func_150296_c());

        for (String key : keys) {
            if (ENCODED_KEYS_TAG.equals(key)) {
                continue;
            }
            decoded.setTag(key, decodeTag(tag.getTag(key)));
        }

        if (tag.hasKey(ENCODED_KEYS_TAG, 9)) {
            NBTTagList encodedEntries = tag.getTagList(ENCODED_KEYS_TAG, 10);
            for (int index = 0; index < encodedEntries.tagCount(); index++) {
                NBTTagCompound entry = encodedEntries.getCompoundTagAt(index);
                if (!entry.hasKey(ENTRY_KEY_TAG, 8) || !entry.hasKey(ENTRY_VALUE_TAG)) {
                    continue;
                }

                decoded.setTag(entry.getString(ENTRY_KEY_TAG), decodeTag(entry.getTag(ENTRY_VALUE_TAG)));
            }
        }

        return decoded;
    }

    private static void rewriteStructureTileTags(NBTTagCompound root, boolean encode) {
        if (!root.hasKey("blocks", 9)) {
            return;
        }

        NBTTagList blocks = root.getTagList("blocks", 10);
        for (int index = 0; index < blocks.tagCount(); index++) {
            NBTTagCompound blockTag = blocks.getCompoundTagAt(index);
            if (!blockTag.hasKey("nbt", 10)) {
                continue;
            }

            NBTTagCompound tileTag = blockTag.getCompoundTag("nbt");
            blockTag.setTag("nbt", encode ? encodeCompound(tileTag) : decodeCompound(tileTag));
        }
    }

    private static NBTTagCompound encodeCompound(NBTTagCompound tag) {
        NBTTagCompound encoded = new NBTTagCompound();
        NBTTagList encodedEntries = null;
        ArrayList<String> keys = new ArrayList<>(tag.func_150296_c());

        for (String key : keys) {
            NBTBase value = encodeTag(tag.getTag(key));
            if (isDirectKeySafe(key)) {
                encoded.setTag(key, value);
            } else {
                if (encodedEntries == null) {
                    encodedEntries = new NBTTagList();
                }

                NBTTagCompound entry = new NBTTagCompound();
                entry.setString(ENTRY_KEY_TAG, key);
                entry.setTag(ENTRY_VALUE_TAG, value);
                encodedEntries.appendTag(entry);
            }
        }

        if (encodedEntries != null && encodedEntries.tagCount() > 0) {
            encoded.setTag(ENCODED_KEYS_TAG, encodedEntries);
        }

        return encoded;
    }

    private static NBTBase encodeTag(NBTBase tag) {
        if (tag instanceof NBTTagCompound compound) {
            return encodeCompound(compound);
        }
        if (tag instanceof NBTTagList list) {
            return transformList(list, true);
        }
        return tag.copy();
    }

    private static NBTBase decodeTag(NBTBase tag) {
        if (tag instanceof NBTTagCompound compound) {
            return decodeCompound(compound);
        }
        if (tag instanceof NBTTagList list) {
            return transformList(list, false);
        }
        return tag.copy();
    }

    private static NBTTagList transformList(NBTTagList list, boolean encode) {
        NBTTagList transformed = new NBTTagList();
        NBTTagList remaining = (NBTTagList) list.copy();
        int count = remaining.tagCount();

        for (int index = 0; index < count; index++) {
            NBTBase entry = remaining.removeTag(0);
            transformed.appendTag(encode ? encodeTag(entry) : decodeTag(entry));
        }

        return transformed;
    }

    private static boolean isDirectKeySafe(String key) {
        if (key == null || key.isEmpty() || ENCODED_KEYS_TAG.equals(key)) {
            return false;
        }

        for (int index = 0; index < key.length(); index++) {
            char c = key.charAt(index);
            if (Character.isLetterOrDigit(c) || c == '_' || c == '-' || c == '.' || c == '+') {
                continue;
            }
            return false;
        }

        return true;
    }

    private static boolean looksLikeText(byte[] data) {
        int index = 0;
        if (data.length >= 3 && (data[0] & 0xFF) == 0xEF && (data[1] & 0xFF) == 0xBB && (data[2] & 0xFF) == 0xBF) {
            index = 3;
        }
        while (index < data.length) {
            byte next = data[index];
            if (next == ' ' || next == '\t' || next == '\r' || next == '\n') {
                index++;
                continue;
            }
            return next == '{';
        }
        return false;
    }
}
