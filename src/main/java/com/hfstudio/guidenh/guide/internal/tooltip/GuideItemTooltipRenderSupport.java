package com.hfstudio.guidenh.guide.internal.tooltip;

import net.minecraft.client.gui.FontRenderer;
import net.minecraft.item.ItemStack;

import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.guide.document.interaction.ItemTooltip;
import com.hfstudio.guidenh.guide.document.interaction.ItemTooltipAppender;

public final class GuideItemTooltipRenderSupport {

    private GuideItemTooltipRenderSupport() {}

    public static boolean shouldUseVanillaRenderer(ItemTooltip tooltip) {
        return !(tooltip instanceof ItemTooltipAppender);
    }

    @Nullable
    public static FontRenderer resolveFont(@Nullable ItemStack stack, @Nullable FontRenderer fallback) {
        if (stack == null) {
            return fallback;
        }
        try {
            FontRenderer itemFont = stack.getItem() != null ? stack.getItem()
                .getFontRenderer(stack) : null;
            return itemFont != null ? itemFont : fallback;
        } catch (Throwable ignored) {
            return fallback;
        }
    }
}
