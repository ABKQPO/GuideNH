package com.hfstudio.guidenh.integration.etfuturum;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;

import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.integration.api.client.PreviewPlayerElytraProvider;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class EtFuturumPreviewPlayerElytraProvider implements PreviewPlayerElytraProvider {

    public EtFuturumPreviewPlayerElytraProvider() {}

    @Override
    public boolean isElytraStack(@Nullable ItemStack stack) {
        return EtFuturumHelpers.isElytraStack(stack);
    }

    @Override
    public boolean tryRenderElytraLayer(@Nullable EntityLivingBase entity, float limbSwing, float limbSwingAmount,
        float partialTicks, float ageInTicks, float scale) {
        return EtFuturumHelpers
            .tryRenderElytraLayer(entity, limbSwing, limbSwingAmount, partialTicks, ageInTicks, scale);
    }
}
