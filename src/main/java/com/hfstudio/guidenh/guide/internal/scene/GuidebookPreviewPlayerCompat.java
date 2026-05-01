package com.hfstudio.guidenh.guide.internal.scene;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Collection;

import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;

import org.jetbrains.annotations.Nullable;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.hfstudio.guidenh.compat.etfuturum.EtFuturumHelpers;
import com.hfstudio.guidenh.compat.simpleskinbackport.SimpleSkinBackportHelpers;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import com.mojang.authlib.properties.Property;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public final class GuidebookPreviewPlayerCompat {

    private static final String TEXTURES_PROPERTY_NAME = "textures";
    private static final String SLIM_MODEL_NAME = "slim";

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

        Boolean simpleSkinBackportSlim = SimpleSkinBackportHelpers.resolveSlim(player);
        if (simpleSkinBackportSlim != null) {
            return simpleSkinBackportSlim;
        }

        Boolean etFuturumSlim = EtFuturumHelpers.resolveSlim(player);
        if (etFuturumSlim != null) {
            return etFuturumSlim;
        }

        return false;
    }

    public static boolean isSimpleSkinBackportAvailable() {
        return SimpleSkinBackportHelpers.isAvailable();
    }

    public static boolean tryInitializeSimpleSkinBackport64xModel(Object model) {
        return SimpleSkinBackportHelpers.tryInitialize64xModel(model);
    }

    public static boolean isEtFuturumElytraStack(@Nullable ItemStack stack) {
        return EtFuturumHelpers.isElytraStack(stack);
    }

    public static boolean tryRenderEtFuturumElytraLayer(EntityLivingBase entity, float limbSwing, float limbSwingAmount,
        float partialTicks, float ageInTicks, float scale) {
        return EtFuturumHelpers
            .tryRenderElytraLayer(entity, limbSwing, limbSwingAmount, partialTicks, ageInTicks, scale);
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
}
