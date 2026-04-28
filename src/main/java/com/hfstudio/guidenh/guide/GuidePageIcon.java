package com.hfstudio.guidenh.guide;

import org.jetbrains.annotations.Nullable;

import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;

import com.github.bsideup.jabel.Desugar;
import com.hfstudio.guidenh.guide.render.GuidePageTexture;

@Desugar
public record GuidePageIcon(@Nullable ItemStack itemStack, @Nullable ResourceLocation textureId,
    @Nullable GuidePageTexture texture) {

    public boolean isItemIcon() {
        return itemStack != null;
    }

    public boolean isTextureIcon() {
        return texture != null;
    }
}
