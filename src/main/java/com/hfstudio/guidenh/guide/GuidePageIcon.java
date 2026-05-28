package com.hfstudio.guidenh.guide;

import java.util.List;

import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;

import org.jetbrains.annotations.Nullable;

import com.github.bsideup.jabel.Desugar;
import com.hfstudio.guidenh.guide.render.GuidePageTexture;

@Desugar
public record GuidePageIcon(@Nullable ItemStack itemStack, @Nullable ResourceLocation textureId,
    @Nullable GuidePageTexture texture, @Nullable List<ItemStack> cycleItemStacks,
    @Nullable List<GuidePageTexture> cycleTextures, @Nullable List<ResourceLocation> cycleTextureIds) {

    public static GuidePageIcon item(ItemStack itemStack) {
        return new GuidePageIcon(itemStack, null, null, null, null, null);
    }

    public static GuidePageIcon textureId(ResourceLocation textureId) {
        return new GuidePageIcon(null, textureId, null, null, null, null);
    }

    public static GuidePageIcon texture(ResourceLocation textureId, GuidePageTexture texture) {
        return new GuidePageIcon(null, textureId, texture, null, null, null);
    }

    public static GuidePageIcon cycleItems(List<ItemStack> itemStacks) {
        if (itemStacks.isEmpty()) {
            throw new IllegalArgumentException("cycle item icon list must not be empty");
        }
        var copiedStacks = copy(itemStacks);
        return new GuidePageIcon(copiedStacks.get(0), null, null, copiedStacks, null, null);
    }

    public static GuidePageIcon cycleTextureIds(List<ResourceLocation> textureIds) {
        if (textureIds.isEmpty()) {
            throw new IllegalArgumentException("cycle texture icon list must not be empty");
        }
        var copiedTextureIds = copy(textureIds);
        return new GuidePageIcon(null, copiedTextureIds.get(0), null, null, null, copiedTextureIds);
    }

    public static GuidePageIcon cycleTextures(List<ResourceLocation> textureIds, List<GuidePageTexture> textures) {
        if (textureIds.isEmpty()) {
            throw new IllegalArgumentException("cycle texture icon id list must not be empty");
        }
        var copiedTextureIds = copy(textureIds);
        var copiedTextures = textures.isEmpty() ? null : copy(textures);
        return new GuidePageIcon(
            null,
            copiedTextureIds.get(0),
            copiedTextures == null ? null : copiedTextures.get(0),
            null,
            copiedTextures,
            copiedTextureIds);
    }

    private static <T> List<T> copy(List<T> values) {
        return List.copyOf(values);
    }

    public boolean isItemIcon() {
        return itemStack != null || (cycleItemStacks != null && !cycleItemStacks.isEmpty());
    }

    public boolean isTextureIcon() {
        return texture != null || (cycleTextures != null && !cycleTextures.isEmpty());
    }

    @Nullable
    public ItemStack resolveCurrentItemStack() {
        if (cycleItemStacks != null && !cycleItemStacks.isEmpty()) {
            if (cycleItemStacks.size() == 1) return cycleItemStacks.get(0);
            int idx = (int) ((System.currentTimeMillis() / 1000L) % cycleItemStacks.size());
            return cycleItemStacks.get(idx);
        }
        return itemStack;
    }

    @Nullable
    public GuidePageTexture resolveCurrentTexture() {
        if (cycleTextures != null && !cycleTextures.isEmpty()) {
            if (cycleTextures.size() == 1) return cycleTextures.get(0);
            int idx = (int) ((System.currentTimeMillis() / 1000L) % cycleTextures.size());
            return cycleTextures.get(idx);
        }
        return texture;
    }

    @Nullable
    public ResourceLocation resolveCurrentTextureId() {
        if (cycleTextureIds != null && !cycleTextureIds.isEmpty()) {
            if (cycleTextureIds.size() == 1) return cycleTextureIds.get(0);
            int idx = (int) ((System.currentTimeMillis() / 1000L) % cycleTextureIds.size());
            return cycleTextureIds.get(idx);
        }
        return textureId;
    }
}
