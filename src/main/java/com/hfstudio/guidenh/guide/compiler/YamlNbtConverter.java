package com.hfstudio.guidenh.guide.compiler;

import java.util.List;
import java.util.Map;

import net.minecraft.nbt.NBTTagByte;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagDouble;
import net.minecraft.nbt.NBTTagFloat;
import net.minecraft.nbt.NBTTagInt;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagLong;
import net.minecraft.nbt.NBTTagShort;
import net.minecraft.nbt.NBTTagString;

public final class YamlNbtConverter {

    private YamlNbtConverter() {}

    public static NBTTagCompound toNbt(Map<?, ?> map) {
        NBTTagCompound tag = new NBTTagCompound();
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            String key = String.valueOf(entry.getKey());
            Object value = entry.getValue();
            if (value == null) continue;
            putValue(tag, key, value);
        }
        return tag;
    }

    private static void putValue(NBTTagCompound tag, String key, Object value) {
        if (value instanceof Map<?, ?> map) {
            tag.setTag(key, toNbt(map));
        } else if (value instanceof List<?> list) {
            tag.setTag(key, toNbtList(list));
        } else if (value instanceof Boolean bool) {
            tag.setByte(key, (byte) (bool ? 1 : 0));
        } else if (value instanceof Byte b) {
            tag.setByte(key, b);
        } else if (value instanceof Short s) {
            tag.setShort(key, s);
        } else if (value instanceof Integer i) {
            tag.setInteger(key, i);
        } else if (value instanceof Long l) {
            tag.setLong(key, l);
        } else if (value instanceof Float f) {
            tag.setFloat(key, f);
        } else if (value instanceof Double d) {
            tag.setDouble(key, d);
        } else if (value instanceof String str) {
            tag.setString(key, str);
        }
    }

    private static NBTTagList toNbtList(List<?> list) {
        NBTTagList nbtList = new NBTTagList();

        for (Object item : list) {
            if (item == null) continue;

            if (item instanceof Map<?, ?> map) {
                nbtList.appendTag(toNbt(map));
            } else if (item instanceof Boolean bool) {
                nbtList.appendTag(new NBTTagByte((byte) (bool ? 1 : 0)));
            } else if (item instanceof Byte b) {
                nbtList.appendTag(new NBTTagByte(b));
            } else if (item instanceof Short s) {
                nbtList.appendTag(new NBTTagShort(s));
            } else if (item instanceof Integer i) {
                nbtList.appendTag(new NBTTagInt(i));
            } else if (item instanceof Long l) {
                nbtList.appendTag(new NBTTagLong(l));
            } else if (item instanceof Float f) {
                nbtList.appendTag(new NBTTagFloat(f));
            } else if (item instanceof Double d) {
                nbtList.appendTag(new NBTTagDouble(d));
            } else if (item instanceof String str) {
                nbtList.appendTag(new NBTTagString(str));
            }
        }

        return nbtList;
    }
}
