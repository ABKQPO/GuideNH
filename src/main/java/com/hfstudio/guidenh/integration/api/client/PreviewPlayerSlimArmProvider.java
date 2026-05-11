package com.hfstudio.guidenh.integration.api.client;

import net.minecraft.client.entity.AbstractClientPlayer;

import org.jetbrains.annotations.Nullable;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public interface PreviewPlayerSlimArmProvider {

    @Nullable
    Boolean resolveSlimArms(@Nullable AbstractClientPlayer player);
}
