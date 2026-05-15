package com.hfstudio.guidenh.integration.nei;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import org.jetbrains.annotations.Nullable;

public class GuideNeiItemReferenceFormatter {

    protected GuideNeiItemReferenceFormatter() {}

    @Nullable
    public static String formatItemReference(@Nullable ItemStack stack) {
        if (stack == null || stack.getItem() == null) {
            return null;
        }
        Object rawName = Item.itemRegistry.getNameForObject(stack.getItem());
        if (rawName == null) {
            return null;
        }
        StringBuilder builder = new StringBuilder(rawName.toString());
        int meta = stack.getItemDamage();
        NBTTagCompound tag = stack.stackTagCompound;
        if (meta != 0) {
            builder.append(':')
                .append(meta);
        }
        if (tag != null) {
            if (meta == 0) {
                builder.append(":0");
            }
            builder.append(':')
                .append(tag);
        }
        return builder.toString();
    }

    @Nullable
    public static String formatRecipeTag(@Nullable ItemStack stack, RecipeQueryKind queryKind) {
        String itemReference = formatItemReference(stack);
        if (itemReference == null) {
            return null;
        }
        String tagName = queryKind == RecipeQueryKind.USAGE ? "RecipeUsage" : "RecipeFor";
        return "<" + tagName + " id=\"" + escapeAttribute(itemReference) + "\" />";
    }

    public static String escapeAttribute(String value) {
        return value == null ? ""
            : value.replace("\\", "\\\\")
                .replace("\"", "\\\"");
    }

    public enum RecipeQueryKind {
        RECIPE,
        USAGE
    }
}
