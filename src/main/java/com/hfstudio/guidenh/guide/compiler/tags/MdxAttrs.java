package com.hfstudio.guidenh.guide.compiler.tags;

import java.util.regex.Pattern;

import javax.annotation.Nullable;

import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;

import org.apache.commons.lang3.tuple.Pair;
import org.joml.Vector3f;

import com.hfstudio.guidenh.guide.color.ARGB;
import com.hfstudio.guidenh.guide.color.ColorValue;
import com.hfstudio.guidenh.guide.color.ConstantColor;
import com.hfstudio.guidenh.guide.compiler.PageCompiler;
import com.hfstudio.guidenh.guide.document.LytErrorSink;
import com.hfstudio.guidenh.libs.mdast.mdx.model.MdxJsxElementFields;

public class MdxAttrs {

    public static final Pattern COLOR_PATTERN = Pattern.compile("^#([0-9a-fA-F]{2}){3,4}$");

    private MdxAttrs() {}

    @Nullable
    public static String getString(PageCompiler compiler, LytErrorSink errorSink, MdxJsxElementFields el,
        String attribute, String defaultValue) {
        try {
            return getString(el, attribute, defaultValue);
        } catch (AttributeException e) {
            errorSink.appendError(compiler, e.getMessage(), el);
            return defaultValue;
        }
    }

    @Nullable
    public static String getString(MdxJsxElementFields el, String attribute, String defaultValue) {
        var id = el.getAttribute(attribute);
        if (id == null) {
            return defaultValue;
        }
        if (id.hasStringValue()) {
            return id.getStringValue();
        } else if (id.hasExpressionValue()) {
            throw new AttributeException(attribute, "Expected string for '" + attribute + "' but got an expression.");
        } else {
            return defaultValue;
        }
    }

    @Nullable
    public static ResourceLocation getRequiredId(PageCompiler compiler, LytErrorSink errorSink, MdxJsxElementFields el,
        String attribute) {
        var id = getString(compiler, errorSink, el, attribute, null);
        if (id == null) {
            errorSink.appendError(compiler, "Missing " + attribute + " attribute.", el);
            return null;
        }
        id = id.trim();
        try {
            return compiler.resolveId(id);
        } catch (IllegalArgumentException e) {
            errorSink.appendError(compiler, "Malformed id " + id + ": " + e.getMessage(), el);
            return null;
        }
    }

    @Nullable
    public static Pair<ResourceLocation, Item> getRequiredItemAndId(PageCompiler compiler, LytErrorSink errorSink,
        MdxJsxElementFields el, String attribute) {
        var itemId = getRequiredId(compiler, errorSink, el, attribute);
        if (itemId == null) {
            return null;
        }
        Item resultItem = (Item) Item.itemRegistry.getObject(itemId.toString());
        if (resultItem == null) {
            errorSink.appendError(compiler, "Missing item: " + itemId, el);
            return null;
        }
        return Pair.of(itemId, resultItem);
    }

    @Nullable
    public static Pair<ResourceLocation, Block> getRequiredBlockAndId(PageCompiler compiler, LytErrorSink errorSink,
        MdxJsxElementFields el, String attribute) {
        var blockId = getRequiredId(compiler, errorSink, el, attribute);
        if (blockId == null) {
            return null;
        }
        Block resultBlock = (Block) Block.blockRegistry.getObject(blockId.toString());
        if (resultBlock == null) {
            errorSink.appendError(compiler, "Missing block: " + blockId, el);
            return null;
        }
        return Pair.of(blockId, resultBlock);
    }

    @Nullable
    public static Item getRequiredItem(PageCompiler compiler, LytErrorSink errorSink, MdxJsxElementFields el,
        String attribute) {
        var result = getRequiredItemAndId(compiler, errorSink, el, attribute);
        return result != null ? result.getRight() : null;
    }

    @Nullable
    public static Pair<ResourceLocation, ItemStack> getRequiredItemStackAndId(PageCompiler compiler,
        LytErrorSink errorSink, MdxJsxElementFields el) {
        var raw = getString(compiler, errorSink, el, "id", null);
        if (raw == null) {
            errorSink.appendError(compiler, "Missing id attribute.", el);
            return null;
        }
        String idStr = raw.trim();
        com.hfstudio.guidenh.guide.compiler.IdUtils.ParsedItemRef ref;
        try {
            ref = com.hfstudio.guidenh.guide.compiler.IdUtils.parseItemRef(
                idStr,
                compiler.getPageId()
                    .getResourceDomain());
        } catch (IllegalArgumentException e) {
            errorSink.appendError(compiler, "Malformed id " + idStr + ": " + e.getMessage(), el);
            return null;
        }
        if (ref == null) {
            errorSink.appendError(compiler, "Missing id attribute.", el);
            return null;
        }
        Item item = (Item) Item.itemRegistry.getObject(
            ref.id()
                .toString());
        if (item == null) {
            errorSink.appendError(compiler, "Missing item: " + ref.id(), el);
            return null;
        }
        ItemStack stack = new ItemStack(item, 1, ref.concreteMeta());
        if (ref.nbt() != null) {
            stack.stackTagCompound = (net.minecraft.nbt.NBTTagCompound) ref.nbt()
                .copy();
        }
        return Pair.of(ref.id(), stack);
    }

    @Nullable
    public static ItemStack getRequiredItemStack(PageCompiler compiler, LytErrorSink errorSink,
        MdxJsxElementFields el) {
        var result = getRequiredItemStackAndId(compiler, errorSink, el);
        return result != null ? result.getValue() : null;
    }

    public static float getFloat(PageCompiler compiler, LytErrorSink errorSink, MdxJsxElementFields el, String name,
        float defaultValue) {
        try {
            return getFloat(el, name, defaultValue);
        } catch (AttributeException e) {
            errorSink.appendError(compiler, e.getMessage(), el);
            return defaultValue;
        }
    }

    public static float getFloat(MdxJsxElementFields el, String name, float defaultValue) {
        var attr = el.getAttribute(name);
        if (attr == null) {
            return defaultValue;
        }
        String attrValue;
        if (attr.hasExpressionValue()) {
            attrValue = attr.getExpressionValue();
        } else if (attr.hasStringValue()) {
            attrValue = attr.getStringValue();
        } else {
            return defaultValue;
        }
        try {
            return Float.parseFloat(attrValue);
        } catch (NumberFormatException e) {
            throw new AttributeException(name, "Malformed floating point value: '" + attrValue + "'");
        }
    }

    public static int getInt(PageCompiler compiler, LytErrorSink errorSink, MdxJsxElementFields el, String name,
        int defaultValue) {
        var attrValue = getString(compiler, errorSink, el, name, null);
        if (attrValue == null) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(attrValue);
        } catch (NumberFormatException e) {
            errorSink.appendError(compiler, "Malformed integer value: '" + attrValue + "'", el);
            return defaultValue;
        }
    }

    public static boolean getBoolean(PageCompiler compiler, LytErrorSink errorSink, MdxJsxElementFields el, String name,
        boolean defaultValue) {
        try {
            return getBoolean(el, name, defaultValue);
        } catch (AttributeException e) {
            errorSink.appendError(compiler, e.getMessage(), el);
            return defaultValue;
        }
    }

    public static boolean getBoolean(MdxJsxElementFields el, String name, boolean defaultValue) {
        var attribute = el.getAttribute(name);
        if (attribute == null) {
            return defaultValue;
        }
        if (attribute.hasExpressionValue()) {
            var expressionValue = attribute.getExpressionValue();
            if (expressionValue.equals("true")) {
                return true;
            } else if (expressionValue.equals("false")) {
                return false;
            }
        }
        throw new AttributeException(name, name + " should be {true} or {false}");
    }

    public static ColorValue getColor(PageCompiler compiler, LytErrorSink errorSink, MdxJsxElementFields el,
        String name, ColorValue defaultColor) {
        var colorStr = getString(compiler, errorSink, el, name, null);
        if (colorStr != null) {
            if ("transparent".equals(colorStr)) {
                return new ConstantColor(0);
            }
            var m = COLOR_PATTERN.matcher(colorStr);
            if (!m.matches()) {
                errorSink.appendError(compiler, "Color must have format #AARRGGBB", el);
                return defaultColor;
            }
            int r, g, b;
            int a = 255;
            if (colorStr.length() == 7) {
                r = Integer.valueOf(colorStr.substring(1, 3), 16);
                g = Integer.valueOf(colorStr.substring(3, 5), 16);
                b = Integer.valueOf(colorStr.substring(5, 7), 16);
            } else {
                a = Integer.valueOf(colorStr.substring(1, 3), 16);
                r = Integer.valueOf(colorStr.substring(3, 5), 16);
                g = Integer.valueOf(colorStr.substring(5, 7), 16);
                b = Integer.valueOf(colorStr.substring(7, 9), 16);
            }
            return new ConstantColor(ARGB.color(a, r, g, b));
        }
        return defaultColor;
    }

    public static Vector3f getVector3(PageCompiler compiler, LytErrorSink errorSink, MdxJsxElementFields el,
        String name, Vector3f defaultValue) {
        var raw = getString(compiler, errorSink, el, name, null);
        if (raw == null || raw.isEmpty()) {
            return new Vector3f(defaultValue);
        }
        String[] parts = raw.trim()
            .split("\\s+");
        if (parts.length != 3) {
            errorSink.appendError(compiler, name + " expects 3 space-separated floats, got: '" + raw + "'", el);
            return new Vector3f(defaultValue);
        }
        try {
            return new Vector3f(Float.parseFloat(parts[0]), Float.parseFloat(parts[1]), Float.parseFloat(parts[2]));
        } catch (NumberFormatException e) {
            errorSink.appendError(compiler, "Malformed vector3 for " + name + ": '" + raw + "'", el);
            return new Vector3f(defaultValue);
        }
    }

    @Nullable
    public static <T extends Enum<T> & SerializedEnum> T getEnum(PageCompiler compiler, LytErrorSink errorSink,
        MdxJsxElementFields el, String name, T defaultValue) {
        try {
            return getEnum(el, name, defaultValue);
        } catch (AttributeException e) {
            errorSink.appendError(compiler, e.getMessage(), el);
            return defaultValue;
        }
    }

    @SuppressWarnings("unchecked")
    public static <T extends Enum<T> & SerializedEnum> T getEnum(MdxJsxElementFields el, String name, T defaultValue) {
        var stringValue = getString(el, name, defaultValue.getSerializedName());
        var clazz = (Class<T>) defaultValue.getClass();
        for (var constant : clazz.getEnumConstants()) {
            if (constant.getSerializedName()
                .equals(stringValue)) {
                return constant;
            }
        }
        throw new AttributeException(name, "Unrecognized option for attribute " + name + ": " + stringValue);
    }

    public static class AttributeException extends RuntimeException {

        private final String attribute;

        public AttributeException(String attribute, String message) {
            super(message);
            this.attribute = attribute;
        }

        public String getAttribute() {
            return attribute;
        }
    }
}
