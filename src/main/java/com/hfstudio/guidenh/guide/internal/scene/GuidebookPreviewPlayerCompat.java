package com.hfstudio.guidenh.guide.internal.scene;

import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Collection;

import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import org.jetbrains.annotations.Nullable;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import com.mojang.authlib.properties.Property;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public final class GuidebookPreviewPlayerCompat {

    private static final String TEXTURES_PROPERTY_NAME = "textures";
    private static final String SLIM_MODEL_NAME = "slim";
    @Nullable
    private static final Method SIMPLE_SKIN_BACKPORT_SLIM_GETTER = findSimpleSkinBackportSlimGetter();
    @Nullable
    private static final Method SIMPLE_SKIN_BACKPORT_SET_64X = findSimpleSkinBackportSet64xMethod();
    @Nullable
    private static final Method ET_FUTURUM_ALEX_CHECKER = findEtFuturumAlexChecker();
    @Nullable
    private static final Class<?> ET_FUTURUM_ELYTRA_ITEM_CLASS = findEtFuturumElytraItemClass();
    @Nullable
    private static final Method ET_FUTURUM_ELYTRA_RENDER_LAYER = findEtFuturumElytraRenderLayer();

    private GuidebookPreviewPlayerCompat() {}

    @Nullable
    public static Boolean resolveSlimSkinModel(@Nullable GameProfile profile,
        @Nullable MinecraftProfileTexture skinTexture) {
        Boolean slimFromTexture = resolveSlimFromTextureMetadata(skinTexture);
        if (slimFromTexture != null) {
            return slimFromTexture;
        }
        return resolveSlimFromGameProfile(profile);
    }

    public static boolean shouldUseSlimArms(AbstractClientPlayer player) {
        if (player instanceof GuidebookScenePreviewPlayerEntity previewPlayer) {
            Boolean slimArms = previewPlayer.getGuidebookSlimArms();
            if (slimArms != null) {
                return slimArms;
            }

            Boolean profileSlim = resolveSlimSkinModel(previewPlayer.getGameProfile(), null);
            if (profileSlim != null) {
                return profileSlim;
            }
        }

        Boolean simpleSkinBackportSlim = resolveSimpleSkinBackportSlim(player);
        if (simpleSkinBackportSlim != null) {
            return simpleSkinBackportSlim;
        }

        Boolean etFuturumSlim = resolveEtFuturumSlim(player);
        if (etFuturumSlim != null) {
            return etFuturumSlim;
        }

        return false;
    }

    public static boolean isSimpleSkinBackportAvailable() {
        return SIMPLE_SKIN_BACKPORT_SET_64X != null;
    }

    public static boolean tryInitializeSimpleSkinBackport64xModel(Object model) {
        if (model == null || SIMPLE_SKIN_BACKPORT_SET_64X == null) {
            return false;
        }

        Class<?> declaringType = SIMPLE_SKIN_BACKPORT_SET_64X.getDeclaringClass();
        if (!declaringType.isInstance(model)) {
            return false;
        }

        try {
            SIMPLE_SKIN_BACKPORT_SET_64X.invoke(model);
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    public static boolean isEtFuturumElytraStack(@Nullable ItemStack stack) {
        if (stack == null || ET_FUTURUM_ELYTRA_ITEM_CLASS == null) {
            return false;
        }

        Item item = stack.getItem();
        return ET_FUTURUM_ELYTRA_ITEM_CLASS.isInstance(item);
    }

    public static boolean tryRenderEtFuturumElytraLayer(EntityLivingBase entity, float limbSwing, float limbSwingAmount,
        float partialTicks, float ageInTicks, float scale) {
        if (entity == null || ET_FUTURUM_ELYTRA_RENDER_LAYER == null) {
            return false;
        }

        try {
            ET_FUTURUM_ELYTRA_RENDER_LAYER
                .invoke(null, entity, limbSwing, limbSwingAmount, partialTicks, ageInTicks, scale);
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    @Nullable
    private static Boolean resolveSlimFromTextureMetadata(@Nullable MinecraftProfileTexture skinTexture) {
        if (skinTexture == null) {
            return null;
        }

        try {
            return SLIM_MODEL_NAME.equals(skinTexture.getMetadata("model"));
        } catch (Throwable ignored) {
            return null;
        }
    }

    @Nullable
    private static Boolean resolveSlimFromGameProfile(@Nullable GameProfile profile) {
        if (profile == null || profile.getProperties() == null
            || !profile.getProperties()
                .containsKey(TEXTURES_PROPERTY_NAME)) {
            return null;
        }

        try {
            Collection<Property> properties = profile.getProperties()
                .get(TEXTURES_PROPERTY_NAME);
            if (properties == null) {
                return null;
            }

            for (Property property : properties) {
                if (property == null || !TEXTURES_PROPERTY_NAME.equals(property.getName())) {
                    continue;
                }

                Boolean slim = decodeSlimFromTexturePayload(property.getValue());
                if (slim != null) {
                    return slim;
                }
            }
        } catch (Throwable ignored) {}

        return null;
    }

    @Nullable
    private static Boolean decodeSlimFromTexturePayload(@Nullable String encodedTexturePayload) {
        String payload = GuidebookPreviewPlayerSkinResolver.trimToNull(encodedTexturePayload);
        if (payload == null) {
            return null;
        }

        try {
            String decodedPayload = new String(
                Base64.getDecoder()
                    .decode(payload),
                StandardCharsets.UTF_8);
            JsonElement rootElement = new JsonParser().parse(decodedPayload);
            if (!rootElement.isJsonObject()) {
                return null;
            }

            JsonObject rootObject = rootElement.getAsJsonObject();
            JsonObject texturesObject = getObject(rootObject, "textures");
            JsonObject skinObject = getObject(texturesObject, "SKIN");
            if (skinObject == null) {
                return null;
            }

            JsonObject metadataObject = getObject(skinObject, "metadata");
            if (metadataObject == null) {
                return Boolean.FALSE;
            }

            JsonElement modelElement = metadataObject.get("model");
            if (modelElement == null || modelElement.isJsonNull()) {
                return Boolean.FALSE;
            }

            return SLIM_MODEL_NAME.equals(modelElement.getAsString());
        } catch (Throwable ignored) {
            return null;
        }
    }

    @Nullable
    private static JsonObject getObject(@Nullable JsonObject parent, String fieldName) {
        if (parent == null) {
            return null;
        }

        JsonElement child = parent.get(fieldName);
        return child != null && child.isJsonObject() ? child.getAsJsonObject() : null;
    }

    @Nullable
    private static Boolean resolveSimpleSkinBackportSlim(AbstractClientPlayer player) {
        if (SIMPLE_SKIN_BACKPORT_SLIM_GETTER == null) {
            return null;
        }

        try {
            Object result = SIMPLE_SKIN_BACKPORT_SLIM_GETTER.invoke(player);
            return result instanceof Boolean ? (Boolean) result : null;
        } catch (Throwable ignored) {
            return null;
        }
    }

    @Nullable
    private static Boolean resolveEtFuturumSlim(AbstractClientPlayer player) {
        if (ET_FUTURUM_ALEX_CHECKER == null || player == null) {
            return null;
        }

        try {
            Object result = ET_FUTURUM_ALEX_CHECKER.invoke(null, player);
            return result instanceof Boolean ? (Boolean) result : null;
        } catch (Throwable ignored) {
            return null;
        }
    }

    @Nullable
    private static Method findSimpleSkinBackportSlimGetter() {
        try {
            return Class.forName("roadhog360.simpleskinbackport.ducks.IArmsState")
                .getMethod("ssb$isSlim");
        } catch (Throwable ignored) {
            return null;
        }
    }

    @Nullable
    private static Method findSimpleSkinBackportSet64xMethod() {
        try {
            return Class.forName("roadhog360.simpleskinbackport.ducks.INewBipedModel")
                .getMethod("ssb$set64x");
        } catch (Throwable ignored) {
            return null;
        }
    }

    @Nullable
    private static Method findEtFuturumAlexChecker() {
        try {
            return Class.forName("ganymedes01.etfuturum.client.skins.PlayerModelManager")
                .getMethod("isPlayerModelAlex", EntityPlayer.class);
        } catch (Throwable ignored) {
            return null;
        }
    }

    @Nullable
    private static Class<?> findEtFuturumElytraItemClass() {
        try {
            return Class.forName("ganymedes01.etfuturum.items.equipment.ItemArmorElytra");
        } catch (Throwable ignored) {
            return null;
        }
    }

    @Nullable
    private static Method findEtFuturumElytraRenderLayer() {
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
