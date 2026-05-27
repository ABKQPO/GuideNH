package com.hfstudio.guidenh.guide.compiler.tags;

import java.util.Locale;
import java.util.regex.Pattern;

import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;

import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import com.hfstudio.guidenh.guide.color.ARGB;
import com.hfstudio.guidenh.guide.color.ColorValue;
import com.hfstudio.guidenh.guide.color.ConstantColor;
import com.hfstudio.guidenh.guide.compiler.GuideItemReferenceResolver;
import com.hfstudio.guidenh.guide.compiler.IdUtils;
import com.hfstudio.guidenh.guide.compiler.PageCompiler;
import com.hfstudio.guidenh.guide.document.LytErrorSink;
import com.hfstudio.guidenh.libs.mdast.mdx.model.MdxJsxAttribute;
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
        var raw = getString(compiler, errorSink, el, attribute, null);
        if (raw == null) {
            errorSink.appendError(compiler, "Missing " + attribute + " attribute.", el);
            return null;
        }
        raw = raw.trim();
        IdUtils.ParsedItemRef ref;
        try {
            ref = IdUtils.parseItemRef(
                raw,
                compiler.getPageId()
                    .getResourceDomain());
        } catch (IllegalArgumentException e) {
            errorSink.appendError(compiler, "Malformed id " + raw + ": " + e.getMessage(), el);
            return null;
        }
        if (ref == null) {
            errorSink.appendError(compiler, "Missing " + attribute + " attribute.", el);
            return null;
        }
        Item resultItem = (Item) Item.itemRegistry.getObject(ref.rawKey());
        if (resultItem == null) {
            errorSink.appendError(compiler, "Missing item: " + ref.id(), el);
            return null;
        }
        return Pair.of(ref.id(), resultItem);
    }

    @Nullable
    public static Pair<ResourceLocation, Block> getRequiredBlockAndId(PageCompiler compiler, LytErrorSink errorSink,
        MdxJsxElementFields el, String attribute) {
        String oreName = GuideItemReferenceResolver.trimToNull(getString(compiler, errorSink, el, "ore", null));
        if (oreName != null) {
            ItemStack stack = GuideItemReferenceResolver.resolveOreDictionaryStack(oreName);
            if (stack == null || stack.getItem() == null) {
                errorSink.appendError(compiler, "Missing ore dictionary entry: " + oreName, el);
                return null;
            }

            Block block = Block.getBlockFromItem(stack.getItem());
            ResourceLocation blockId = GuideItemReferenceResolver.resolveBlockRegistryId(block);
            if (block == null || blockId == null) {
                errorSink.appendError(
                    compiler,
                    "Ore dictionary entry '" + oreName + "' does not resolve to a block item",
                    el);
                return null;
            }
            return Pair.of(blockId, block);
        }

        var blockId = getRequiredId(compiler, errorSink, el, attribute);
        if (blockId == null) {
            return null;
        }
        var rawAttr = getString(el, attribute, null);
        String blockLookupKey = rawAttr != null && !rawAttr.trim()
            .isEmpty() ? IdUtils.rawRegistryKey(
                rawAttr.trim(),
                compiler.getPageId()
                    .getResourceDomain())
                : blockId.toString();
        Block resultBlock = (Block) Block.blockRegistry.getObject(blockLookupKey);
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
        String oreName = GuideItemReferenceResolver.trimToNull(getString(compiler, errorSink, el, "ore", null));
        if (oreName != null) {
            ItemStack stack = GuideItemReferenceResolver.resolveOreDictionaryStack(oreName);
            if (stack == null || stack.getItem() == null) {
                errorSink.appendError(compiler, "Missing ore dictionary entry: " + oreName, el);
                return null;
            }

            ResourceLocation itemId = GuideItemReferenceResolver.resolveItemRegistryId(stack);
            if (itemId == null) {
                errorSink.appendError(compiler, "Unregistered item from ore dictionary entry: " + oreName, el);
                return null;
            }
            return Pair.of(itemId, stack);
        }

        var raw = getString(compiler, errorSink, el, "id", null);
        if (raw == null) {
            errorSink.appendError(compiler, "Missing id or ore attribute.", el);
            return null;
        }
        String idStr = raw.trim();
        IdUtils.ParsedItemRef ref;
        try {
            ref = IdUtils.parseItemRef(
                idStr,
                compiler.getPageId()
                    .getResourceDomain());
        } catch (IllegalArgumentException e) {
            errorSink.appendError(compiler, "Malformed id " + idStr + ": " + e.getMessage(), el);
            return null;
        }
        if (ref == null) {
            errorSink.appendError(compiler, "Missing id or ore attribute.", el);
            return null;
        }
        Item item = (Item) Item.itemRegistry.getObject(ref.rawKey());
        if (item == null) {
            errorSink.appendError(compiler, "Missing item: " + ref.id(), el);
            return null;
        }
        ItemStack stack = new ItemStack(item, 1, ref.concreteMeta());
        if (ref.nbt() != null) {
            stack.stackTagCompound = (NBTTagCompound) ref.nbt()
                .copy();
        }
        return Pair.of(ref.id(), stack);
    }

    @Nullable
    public static GuideItemReferenceResolver.ResolvedBlockReference getRequiredBlockReference(PageCompiler compiler,
        LytErrorSink errorSink, MdxJsxElementFields el, String attribute) {
        String oreName = GuideItemReferenceResolver.trimToNull(getString(compiler, errorSink, el, "ore", null));
        String raw = getString(compiler, errorSink, el, attribute, null);
        GuideItemReferenceResolver.ResolvedBlockReference resolved = GuideItemReferenceResolver.resolveBlockReference(
            compiler.getPageId()
                .getResourceDomain(),
            raw,
            oreName);
        if (resolved != null) {
            return resolved;
        }

        if (oreName != null) {
            ItemStack stack = GuideItemReferenceResolver.resolveOreDictionaryStack(oreName);
            if (stack == null || stack.getItem() == null) {
                errorSink.appendError(compiler, "Missing ore dictionary entry: " + oreName, el);
            } else {
                errorSink.appendError(
                    compiler,
                    "Ore dictionary entry '" + oreName + "' does not resolve to a block item",
                    el);
            }
            return null;
        }

        if (raw == null) {
            errorSink.appendError(compiler, "Missing " + attribute + " attribute.", el);
            return null;
        }
        errorSink.appendError(compiler, "Missing block: " + raw.trim(), el);
        return null;
    }

    @Nullable
    public static GuideItemReferenceResolver.ResolvedBlockReference getBlockReference(MdxJsxElementFields el,
        String attribute, String defaultNamespace) {
        String oreName = GuideItemReferenceResolver.trimToNull(getString(el, "ore", null));
        String raw = getString(el, attribute, null);
        return GuideItemReferenceResolver.resolveBlockReference(defaultNamespace, raw, oreName);
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
        Boolean parsed = getOptionalBoolean(el, name);
        return parsed != null ? parsed : defaultValue;
    }

    public static boolean getBoolean(@Nullable Boolean parsed, boolean defaultValue) {
        return parsed != null ? parsed : defaultValue;
    }

    @Nullable
    public static Boolean getOptionalBoolean(MdxJsxElementFields el, String name) {
        return parseOptionalBoolean(el.getAttribute(name), name);
    }

    @Nullable
    public static Boolean parseOptionalBoolean(@Nullable MdxJsxAttribute attribute, String attributeName) {
        if (attribute == null) {
            return null;
        }
        if (!attribute.hasExpressionValue() && !attribute.hasStringValue()) {
            return true;
        }
        String rawValue = attribute.hasExpressionValue() ? attribute.getExpressionValue() : attribute.getStringValue();
        String normalizedValue = rawValue == null ? ""
            : rawValue.trim()
                .toLowerCase(Locale.ROOT);
        return switch (normalizedValue) {
            case "true", "1", "yes", "on" -> true;
            case "false", "0", "no", "off" -> false;
            default -> throw new AttributeException(attributeName, attributeName + " should be true or false");
        };
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
        float[] parts = parseVector3Parts(raw);
        if (parts == null) {
            errorSink.appendError(compiler, name + " expects 3 space-separated floats, got: '" + raw + "'", el);
            return new Vector3f(defaultValue);
        }
        return new Vector3f(parts[0], parts[1], parts[2]);
    }

    @Nullable
    public static float[] parseVector3Parts(String raw) {
        float[] values = new float[3];
        int length = raw.length();
        int index = 0;
        int cursor = 0;
        while (cursor < length) {
            while (cursor < length && Character.isWhitespace(raw.charAt(cursor))) {
                cursor++;
            }
            if (cursor >= length) {
                break;
            }
            if (index >= values.length) {
                return null;
            }
            int start = cursor;
            while (cursor < length && !Character.isWhitespace(raw.charAt(cursor))) {
                cursor++;
            }
            try {
                values[index] = Float.parseFloat(raw.substring(start, cursor));
            } catch (NumberFormatException e) {
                return null;
            }
            index++;
        }
        return index == values.length ? values : null;
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
