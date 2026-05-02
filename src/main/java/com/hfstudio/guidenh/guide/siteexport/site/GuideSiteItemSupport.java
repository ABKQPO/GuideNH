package com.hfstudio.guidenh.guide.siteexport.site;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;

import org.jetbrains.annotations.Nullable;

public class GuideSiteItemSupport {

    private GuideSiteItemSupport() {}

    public static GuideSiteExportedItem export(@Nullable ItemStack stack, GuideSiteItemIconResolver iconResolver) {
        return export(null, stack, iconResolver, "");
    }

    public static GuideSiteExportedItem export(@Nullable ResourceLocation registryId, @Nullable ItemStack stack,
        GuideSiteItemIconResolver iconResolver, String fallbackItemId) {
        String itemId = registryId != null ? registryId.toString() : itemId(stack);
        if (itemId.isEmpty()) {
            itemId = fallbackItemId != null ? fallbackItemId : "";
        }
        if (stack == null || stack.getItem() == null) {
            return unresolved(itemId);
        }

        String displayName = displayName(stack);
        if (displayName.isEmpty()) {
            displayName = itemId;
        }

        String iconSrc = iconResolver != null ? iconResolver.exportIcon(stack) : "";
        return new GuideSiteExportedItem(itemId, displayName, iconSrc);
    }

    public static GuideSiteExportedItem unresolved(String itemId) {
        String safeItemId = itemId != null ? itemId : "";
        return new GuideSiteExportedItem(safeItemId, safeItemId, "");
    }

    public static String itemId(@Nullable ItemStack stack) {
        if (stack == null || stack.getItem() == null) {
            return "";
        }
        Item item = stack.getItem();
        String registryName = Item.itemRegistry.getNameForObject(item);
        return registryName != null ? registryName : "";
    }

    public static String displayName(@Nullable ItemStack stack) {
        if (stack == null) {
            return "";
        }
        try {
            return stripLegacyFormatting(stack.getDisplayName());
        } catch (Throwable ignored) {
            String itemId = itemId(stack);
            return itemId != null ? itemId : "";
        }
    }

    public static String stripLegacyFormatting(@Nullable String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        StringBuilder cleaned = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            if (ch == '\u00A7' && i + 1 < text.length()) {
                i++;
                continue;
            }
            cleaned.append(ch);
        }
        return cleaned.toString();
    }
}
