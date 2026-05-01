package com.hfstudio.guidenh.compat.etfuturum;

import java.lang.reflect.Method;

import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.compat.Mods;

public final class EtFuturumHelpers {

    @Nullable
    private static final Method ALEX_CHECKER = findAlexChecker();
    @Nullable
    private static final Class<?> ELYTRA_ITEM_CLASS = findElytraItemClass();
    @Nullable
    private static final Method ELYTRA_RENDER_LAYER = findElytraRenderLayer();

    private EtFuturumHelpers() {}

    @Nullable
    public static Boolean resolveSlim(AbstractClientPlayer player) {
        if (ALEX_CHECKER == null || player == null) return null;
        try {
            Object result = ALEX_CHECKER.invoke(null, player);
            return result instanceof Boolean ? (Boolean) result : null;
        } catch (Throwable ignored) {
            return null;
        }
    }

    public static boolean isElytraStack(@Nullable ItemStack stack) {
        if (stack == null || ELYTRA_ITEM_CLASS == null) return false;
        Item item = stack.getItem();
        return ELYTRA_ITEM_CLASS.isInstance(item);
    }

    public static boolean tryRenderElytraLayer(EntityLivingBase entity, float limbSwing, float limbSwingAmount,
        float partialTicks, float ageInTicks, float scale) {
        if (entity == null || ELYTRA_RENDER_LAYER == null) return false;
        try {
            ELYTRA_RENDER_LAYER.invoke(null, entity, limbSwing, limbSwingAmount, partialTicks, ageInTicks, scale);
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    @Nullable
    private static Method findAlexChecker() {
        if (!Mods.EtFuturum.isModLoaded()) return null;
        try {
            return Class.forName("ganymedes01.etfuturum.client.skins.PlayerModelManager")
                .getMethod("isPlayerModelAlex", EntityPlayer.class);
        } catch (Throwable ignored) {
            return null;
        }
    }

    @Nullable
    private static Class<?> findElytraItemClass() {
        if (!Mods.EtFuturum.isModLoaded()) return null;
        try {
            return Class.forName("ganymedes01.etfuturum.items.equipment.ItemArmorElytra");
        } catch (Throwable ignored) {
            return null;
        }
    }

    @Nullable
    private static Method findElytraRenderLayer() {
        if (!Mods.EtFuturum.isModLoaded()) return null;
        try {
            return Class.forName("ganymedes01.etfuturum.client.renderer.entity.elytra.LayerBetterElytra")
                .getMethod(
                    "doRenderLayer",
                    EntityLivingBase.class,
                    Float.TYPE,
                    Float.TYPE,
                    Float.TYPE,
                    Float.TYPE,
                    Float.TYPE);
        } catch (Throwable ignored) {
            return null;
        }
    }
}
