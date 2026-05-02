package com.hfstudio.guidenh.guide.document.block.chart;

import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;

import com.hfstudio.guidenh.guide.render.GuidePageTexture;

/**
 * Icon associated with a chart series/slice: either an {@link ItemStack} or a PNG resource.
 */
public final class ChartIcon {

    private final ItemStack stack;
    private final GuidePageTexture texture;
    private final ResourceLocation imageId;

    private ChartIcon(ItemStack stack, GuidePageTexture texture, ResourceLocation imageId) {
        this.stack = stack;
        this.texture = texture;
        this.imageId = imageId;
    }

    public static ChartIcon ofItemStack(ItemStack stack) {
        return new ChartIcon(stack, null, null);
    }

    public static ChartIcon ofImage(ResourceLocation id, GuidePageTexture texture) {
        return new ChartIcon(null, texture, id);
    }

    public ItemStack getStack() {
        return stack;
    }

    public GuidePageTexture getTexture() {
        return texture;
    }

    public ResourceLocation getImageId() {
        return imageId;
    }

    public boolean hasItemStack() {
        return stack != null && stack.getItem() != null;
    }

    public boolean hasImage() {
        return texture != null && !texture.isMissing();
    }
}
