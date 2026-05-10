package com.hfstudio.guidenh.integration.api.client;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;

import org.jetbrains.annotations.Nullable;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public interface PreviewPlayerElytraProvider {

    boolean isElytraStack(@Nullable ItemStack stack);

    boolean tryRenderElytraLayer(@Nullable EntityLivingBase entity, float limbSwing, float limbSwingAmount,
        float partialTicks, float ageInTicks, float scale);
}
