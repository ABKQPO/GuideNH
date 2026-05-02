package com.hfstudio.guidenh.compat.simpleskinbackport;

import java.lang.reflect.Method;

import net.minecraft.client.entity.AbstractClientPlayer;

import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.compat.Mods;

public final class SimpleSkinBackportHelpers {

    @Nullable
    private static final Method SLIM_GETTER = findSlimGetter();
    @Nullable
    private static final Method SET_64X = findSet64xMethod();

    private SimpleSkinBackportHelpers() {}

    public static boolean isAvailable() {
        return SET_64X != null;
    }

    @Nullable
    public static Boolean resolveSlim(AbstractClientPlayer player) {
        if (SLIM_GETTER == null || player == null) return null;
        try {
            Object result = SLIM_GETTER.invoke(player);
            return result instanceof Boolean ? (Boolean) result : null;
        } catch (Throwable ignored) {
            return null;
        }
    }

    public static boolean tryInitialize64xModel(Object model) {
        if (model == null || SET_64X == null) return false;
        Class<?> declaringType = SET_64X.getDeclaringClass();
        if (!declaringType.isInstance(model)) return false;
        try {
            SET_64X.invoke(model);
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    @Nullable
    private static Method findSlimGetter() {
        if (!Mods.SimpleSkinBackport.isModLoaded()) return null;
        try {
            return Class.forName("roadhog360.simpleskinbackport.ducks.IArmsState")
                .getMethod("ssb$isSlim");
        } catch (Throwable ignored) {
            return null;
        }
    }

    @Nullable
    private static Method findSet64xMethod() {
        if (!Mods.SimpleSkinBackport.isModLoaded()) return null;
        try {
            return Class.forName("roadhog360.simpleskinbackport.ducks.INewBipedModel")
                .getMethod("ssb$set64x");
        } catch (Throwable ignored) {
            return null;
        }
    }
}
