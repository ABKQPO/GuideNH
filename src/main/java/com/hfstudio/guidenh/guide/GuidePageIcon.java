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
