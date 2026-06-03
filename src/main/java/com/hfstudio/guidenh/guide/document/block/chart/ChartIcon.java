package com.hfstudio.guidenh.guide.document.block.chart;

import net.minecraft.client.Minecraft;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;

import com.hfstudio.guidenh.guide.internal.resource.GuideResourceAccess;
import com.hfstudio.guidenh.guide.render.GuidePageTexture;

/**
 * Icon associated with a chart series/slice: either an {@link ItemStack} or a PNG resource.
 * Supports deferred resolution so compilers can produce icons without accessing registries or I/O.
 */
public class ChartIcon {

    private ItemStack stack;
    private GuidePageTexture texture;
    private ResourceLocation imageId;

    private String deferredRawKey;
    private int deferredMeta;
    private ResourceLocation deferredImageId;
    private boolean resolved;

    private ChartIcon(ItemStack stack, GuidePageTexture texture, ResourceLocation imageId) {
        this.stack = stack;
        this.texture = texture;
        this.imageId = imageId;
        this.resolved = true;
    }

    public static ChartIcon ofItemStack(ItemStack stack) {
        return new ChartIcon(stack, null, null);
    }

    public static ChartIcon ofImage(ResourceLocation id, GuidePageTexture texture) {
        return new ChartIcon(null, texture, id);
    }

    public static ChartIcon ofDeferredItem(String rawKey, int meta) {
        ChartIcon icon = new ChartIcon(null, null, null);
        icon.resolved = false;
        icon.deferredRawKey = rawKey;
        icon.deferredMeta = meta;
        return icon;
    }

    public static ChartIcon ofDeferredImage(ResourceLocation id) {
        ChartIcon icon = new ChartIcon(null, null, null);
        icon.resolved = false;
        icon.deferredImageId = id;
        return icon;
    }

    @SuppressWarnings("deprecation")
    private void resolve() {
        if (resolved) return;
        resolved = true;
        if (deferredRawKey != null) {
            Item item = (Item) Item.itemRegistry.getObject(deferredRawKey);
            if (item != null) {
                stack = new ItemStack(item, 1, deferredMeta);
            }
            deferredRawKey = null;
        }
        if (deferredImageId != null) {
            byte[] data = GuideResourceAccess.readBytes(
                Minecraft.getMinecraft()
                    .getResourceManager(),
                deferredImageId);
            if (data != null) {
                imageId = deferredImageId;
                texture = GuidePageTexture.load(imageId, data);
            }
            deferredImageId = null;
        }
    }

    public ItemStack getStack() {
        resolve();
        return stack;
    }

    public GuidePageTexture getTexture() {
        resolve();
        return texture;
    }

    public ResourceLocation getImageId() {
        resolve();
        return imageId;
    }

    public boolean hasItemStack() {
        resolve();
        return stack != null && stack.getItem() != null;
    }

    public boolean hasImage() {
        resolve();
        return texture != null && !texture.isMissing();
    }
}
