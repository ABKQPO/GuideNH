package com.hfstudio.guidenh.integration.etfuturum;

import net.minecraft.client.entity.AbstractClientPlayer;

import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.integration.api.client.PreviewPlayerSlimArmProvider;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class EtFuturumSlimArmProvider implements PreviewPlayerSlimArmProvider {

    public EtFuturumSlimArmProvider() {}

    @Override
    @Nullable
    public Boolean resolveSlimArms(@Nullable AbstractClientPlayer player) {
        return EtFuturumHelpers.resolveSlim(player);
    }
}
