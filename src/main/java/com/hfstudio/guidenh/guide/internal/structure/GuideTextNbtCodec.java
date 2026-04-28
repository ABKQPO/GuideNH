package com.hfstudio.guidenh.guide.internal.structure;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.zip.GZIPInputStream;

import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.JsonToNBT;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagByteArray;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagIntArray;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;

/**
 * 1.7.10's text-NBT reader/writer cannot round-trip compound keys containing {@code :} (AE's
 * {@code def:6}/{@code extra:2}/{@code facade:5} are the main example). This codec rewrites unsafe
 * compound keys into a list-backed representation before stringification, then restores the original
 * keys after parsing.
 */
public class GuideTextNbtCodec {

    public static final String ENCODED_KEYS_TAG = "__guidenh_encoded_keys_v1";
    public static final String ENTRY_KEY_TAG = "k";
    public static final String ENTRY_VALUE_TAG = "v";
    public static final String BYTE_ARRAY_WRAPPER_TAG = "__guidenh_byte_array_v1";

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

    public static void rewriteStructureTileTags(NBTTagCompound root, boolean encode) {
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

    public static NBTTagCompound encodeCompound(NBTTagCompound tag) {
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

    public static NBTBase encodeTag(NBTBase tag) {
        if (tag instanceof NBTTagByteArray byteArray) {
            NBTTagCompound encoded = new NBTTagCompound();
            encoded.setIntArray(BYTE_ARRAY_WRAPPER_TAG, toIntArray(byteArray.func_150292_c()));
            return encoded;
        }
        if (tag instanceof NBTTagCompound compound) {
            return encodeCompound(compound);
        }
        if (tag instanceof NBTTagList list) {
            return transformList(list, true);
        }
        return tag.copy();
    }

    public static NBTBase decodeTag(NBTBase tag) {
        if (tag instanceof NBTTagCompound compound) {
            byte[] decodedByteArray = decodeWrappedByteArray(compound);
            if (decodedByteArray != null) {
                return new NBTTagByteArray(decodedByteArray);
            }
            return decodeCompound(compound);
        }
        if (tag instanceof NBTTagList list) {
            return transformList(list, false);
        }
        return tag.copy();
    }

    public static boolean isEncodedByteArray(NBTTagCompound compound) {
        return compound.func_150296_c()
            .size() == 1 && compound.hasKey(BYTE_ARRAY_WRAPPER_TAG);
    }

    public static byte[] decodeWrappedByteArray(NBTTagCompound compound) {
        if (!isEncodedByteArray(compound)) {
            return null;
        }
        return tryDecodeByteArrayTag(compound.getTag(BYTE_ARRAY_WRAPPER_TAG));
    }

    public static int[] toIntArray(byte[] bytes) {
        int[] ints = new int[bytes.length];
        for (int index = 0; index < bytes.length; index++) {
            ints[index] = bytes[index];
        }
        return ints;
    }

    public static byte[] toByteArray(int[] ints) {
        byte[] bytes = new byte[ints.length];
        for (int index = 0; index < ints.length; index++) {
            bytes[index] = (byte) ints[index];
        }
        return bytes;
    }

    public static NBTTagList transformList(NBTTagList list, boolean encode) {
        NBTTagList transformed = new NBTTagList();
        NBTTagList remaining = (NBTTagList) list.copy();
        int count = remaining.tagCount();

        for (int index = 0; index < count; index++) {
            NBTBase entry = remaining.removeTag(0);
            transformed.appendTag(encode ? encodeTag(entry) : decodeTag(entry));
        }

        return transformed;
    }

    public static byte[] tryDecodeByteArrayTag(NBTBase tag) {
        if (tag instanceof NBTTagByteArray byteArray) {
            return byteArray.func_150292_c();
        }
        if (tag instanceof NBTTagIntArray intArray) {
            return toByteArray(intArray.func_150302_c());
        }
        if (tag instanceof NBTTagString stringTag) {
            return parseByteArrayLiteral(stringTag.func_150285_a_());
        }
        if (tag instanceof NBTTagList list) {
            if (list.tagCount() <= 0) {
                return new byte[0];
            }
            byte[] decoded = new byte[list.tagCount()];
            for (int index = 0; index < list.tagCount(); index++) {
                try {
                    decoded[index] = Byte.parseByte(trimNumericSuffix(list.getStringTagAt(index)));
                } catch (NumberFormatException ignored) {
                    return null;
                }
            }
            return decoded;
        }
        return null;
    }

    public static byte[] parseByteArrayLiteral(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        if (!trimmed.startsWith("[") || !trimmed.endsWith("]")) {
            return null;
        }
        String content = trimmed.substring(1, trimmed.length() - 1)
            .trim();
        if (content.isEmpty()) {
            return new byte[0];
        }

        String[] parts = content.split(",");
        ArrayList<Byte> decoded = new ArrayList<>(parts.length);
        for (String part : parts) {
            String numeric = trimNumericSuffix(part);
            if (numeric.isEmpty()) {
                continue;
            }
            try {
                decoded.add((byte) Integer.parseInt(numeric));
            } catch (NumberFormatException ignored) {
                return null;
            }
        }

        byte[] result = new byte[decoded.size()];
        for (int index = 0; index < decoded.size(); index++) {
            result[index] = decoded.get(index);
        }
        return result;
    }

    public static String trimNumericSuffix(String value) {
        String trimmed = value != null ? value.trim() : "";
        if (trimmed.endsWith("b") || trimmed.endsWith("B")) {
            return trimmed.substring(0, trimmed.length() - 1);
        }
        return trimmed;
    }

    public static boolean isDirectKeySafe(String key) {
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

    public static boolean looksLikeText(byte[] data) {
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
