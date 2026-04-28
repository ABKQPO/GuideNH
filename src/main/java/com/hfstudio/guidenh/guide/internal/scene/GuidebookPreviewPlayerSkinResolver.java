package com.hfstudio.guidenh.guide.internal.scene;

import java.lang.ref.WeakReference;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.SkinManager;

import com.hfstudio.guidenh.guide.scene.element.GuidebookSceneEntityLoader;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.minecraft.InsecureTextureException;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import com.mojang.authlib.minecraft.MinecraftProfileTexture.Type;
import com.mojang.authlib.minecraft.MinecraftSessionService;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
final class GuidebookPreviewPlayerSkinResolver {

    private static final ExecutorService LOOKUP_EXECUTOR = Executors
        .newSingleThreadExecutor(new GuidebookPreviewPlayerSkinThreadFactory());
    private static final Map<String, ResolvedPreviewPlayerSkin> RESOLVED_SKINS = new ConcurrentHashMap<>();
    private static final Map<String, List<WeakReference<GuidebookScenePreviewPlayerEntity>>> PENDING_ENTITIES = new ConcurrentHashMap<>();
    private static final Set<String> INFLIGHT_LOOKUPS = Collections
        .newSetFromMap(new ConcurrentHashMap<String, Boolean>());

    private GuidebookPreviewPlayerSkinResolver() {}

    static void queueSkinRefresh(GuidebookScenePreviewPlayerEntity entity) {
        GameProfile currentProfile = entity.getGameProfile();
        String playerName = trimToNull(currentProfile.getName());
        if (playerName == null) {
            return;
        }

        String cacheKey = normalizeCacheKey(playerName);
        if (cacheKey == null) {
            return;
        }

        ResolvedPreviewPlayerSkin cachedSkin = RESOLVED_SKINS.get(cacheKey);
        if (cachedSkin != null) {
            applyResolvedSkin(entity, cachedSkin);
            return;
        }

        GameProfile cachedProfile = GuidebookSceneEntityLoader.resolveCachedPreviewPlayerProfile(playerName);
        GameProfile lookupProfile = selectLookupProfile(currentProfile, cachedProfile, playerName);
        if (hasUsableTextures(lookupProfile, playerName)) {
            Minecraft.getMinecraft()
                .func_152342_ad()
                .func_152790_a(lookupProfile, entity, true);
            return;
        }

        if (!needsBackgroundLookup(lookupProfile, playerName)) {
            return;
        }

        registerPendingEntity(cacheKey, entity);
        if (!INFLIGHT_LOOKUPS.add(cacheKey)) {
            return;
        }

        LOOKUP_EXECUTOR.submit(() -> resolveSkinInBackground(cacheKey, playerName, lookupProfile));
    }

    private static void resolveSkinInBackground(String cacheKey, String playerName, GameProfile lookupProfile) {
        ResolvedPreviewPlayerSkin resolvedSkin = resolvePreviewPlayerSkin(playerName, lookupProfile);
        Minecraft minecraft = Minecraft.getMinecraft();
        minecraft.func_152344_a(() -> applyResolvedSkinOnMainThread(cacheKey, playerName, resolvedSkin));
    }

    private static void applyResolvedSkinOnMainThread(String cacheKey, String playerName,
        ResolvedPreviewPlayerSkin resolvedSkin) {
        INFLIGHT_LOOKUPS.remove(cacheKey);
        if (resolvedSkin != null) {
            GuidebookSceneEntityLoader.cacheResolvedPreviewPlayerProfile(playerName, resolvedSkin.profile);
            if (resolvedSkin.hasAnyTexture()) {
                RESOLVED_SKINS.put(cacheKey, resolvedSkin);
            }
        }

        List<WeakReference<GuidebookScenePreviewPlayerEntity>> pendingEntities = PENDING_ENTITIES.remove(cacheKey);
        if (pendingEntities == null) {
            return;
        }

        synchronized (pendingEntities) {
            for (WeakReference<GuidebookScenePreviewPlayerEntity> entityRef : pendingEntities) {
                GuidebookScenePreviewPlayerEntity entity = entityRef.get();
                if (entity == null) {
                    continue;
                }
                if (resolvedSkin != null) {
                    applyResolvedSkin(entity, resolvedSkin);
                }
            }
        }
    }

    private static ResolvedPreviewPlayerSkin resolvePreviewPlayerSkin(String playerName, GameProfile lookupProfile) {
        ResolvedPreviewPlayerSkin resolvedFromProfile = resolveTexturesForProfile(lookupProfile);
        if (resolvedFromProfile != null && resolvedFromProfile.hasAnyTexture()) {
            return resolvedFromProfile;
        }

        GameProfile resolvedProfile = GuidebookSceneEntityLoader.lookupProfileFromRepository(playerName);
        return resolveTexturesForProfile(resolvedProfile);
    }

    private static ResolvedPreviewPlayerSkin resolveTexturesForProfile(GameProfile profile) {
        if (profile == null || profile.getId() == null) {
            return null;
        }

        GameProfile resolvedProfile = profile;
        MinecraftSessionService sessionService = Minecraft.getMinecraft()
            .func_152347_ac();
        Map<Type, MinecraftProfileTexture> textures = new EnumMap<>(Type.class);

        try {
            textures.putAll(sessionService.getTextures(resolvedProfile, true));
        } catch (InsecureTextureException ignored) {
            // Secure texture validation can reject unsigned cache entries. Fallback below.
        } catch (Throwable ignored) {}

        if (textures.isEmpty() || !hasTextures(resolvedProfile)) {
            try {
                GameProfile filledProfile = sessionService.fillProfileProperties(resolvedProfile, false);
                if (filledProfile != null) {
                    resolvedProfile = filledProfile;
                }
                textures.putAll(sessionService.getTextures(resolvedProfile, false));
            } catch (Throwable ignored) {}
        }

        return new ResolvedPreviewPlayerSkin(resolvedProfile, textures.get(Type.SKIN), textures.get(Type.CAPE));
    }

    private static void applyResolvedSkin(GuidebookScenePreviewPlayerEntity entity,
        ResolvedPreviewPlayerSkin resolvedSkin) {
        SkinManager skinManager = Minecraft.getMinecraft()
            .func_152342_ad();
        if (resolvedSkin.skinTexture != null) {
            skinManager.func_152789_a(resolvedSkin.skinTexture, Type.SKIN, entity);
        }
        if (resolvedSkin.capeTexture != null) {
            skinManager.func_152789_a(resolvedSkin.capeTexture, Type.CAPE, entity);
        }
        if (!resolvedSkin.hasAnyTexture()) {
            skinManager.func_152790_a(resolvedSkin.profile, entity, true);
        }
    }

    private static void registerPendingEntity(String cacheKey, GuidebookScenePreviewPlayerEntity entity) {
        PENDING_ENTITIES.compute(cacheKey, (ignored, existing) -> {
            List<WeakReference<GuidebookScenePreviewPlayerEntity>> pending = existing;
            if (pending == null) {
                pending = Collections
                    .synchronizedList(new ArrayList<WeakReference<GuidebookScenePreviewPlayerEntity>>());
            }
            pending.add(new WeakReference<>(entity));
            return pending;
        });
    }

    private static GameProfile selectLookupProfile(GameProfile currentProfile, GameProfile cachedProfile,
        String playerName) {
        if (hasUsableTextures(currentProfile, playerName)) {
            return currentProfile;
        }
        if (hasUsableTextures(cachedProfile, playerName)) {
            return cachedProfile;
        }
        if (isUsableResolvedProfile(currentProfile, playerName)) {
            return currentProfile;
        }
        if (isUsableResolvedProfile(cachedProfile, playerName)) {
            return cachedProfile;
        }
        return cachedProfile != null ? cachedProfile : currentProfile;
    }

    private static boolean needsBackgroundLookup(GameProfile profile, String playerName) {
        UUID profileId = profile == null ? null : profile.getId();
        return profileId == null || isOfflineFallbackProfile(profileId, playerName) || !hasTextures(profile);
    }

    private static boolean hasUsableTextures(GameProfile profile, String playerName) {
        return isUsableResolvedProfile(profile, playerName) && hasTextures(profile);
    }

    private static boolean isUsableResolvedProfile(GameProfile profile, String playerName) {
        return profile != null && profile.getId() != null && !isOfflineFallbackProfile(profile.getId(), playerName);
    }

    private static boolean isOfflineFallbackProfile(UUID profileId, String playerName) {
        UUID offlineUuid = UUID.nameUUIDFromBytes(("OfflinePlayer:" + playerName).getBytes(StandardCharsets.UTF_8));
        return offlineUuid.equals(profileId);
    }

    private static String normalizeCacheKey(String playerName) {
        String trimmedName = trimToNull(playerName);
        return trimmedName == null ? null : trimmedName.toLowerCase(java.util.Locale.ROOT);
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static boolean hasTextures(GameProfile profile) {
        try {
            return profile != null && profile.getProperties() != null
                && profile.getProperties()
                    .containsKey("textures");
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static final class ResolvedPreviewPlayerSkin {

        private final GameProfile profile;
        private final MinecraftProfileTexture skinTexture;
        private final MinecraftProfileTexture capeTexture;

        private ResolvedPreviewPlayerSkin(GameProfile profile, MinecraftProfileTexture skinTexture,
            MinecraftProfileTexture capeTexture) {
            this.profile = profile;
            this.skinTexture = skinTexture;
            this.capeTexture = capeTexture;
        }

        private boolean hasAnyTexture() {
            return skinTexture != null || capeTexture != null;
        }
    }

    private static final class GuidebookPreviewPlayerSkinThreadFactory implements ThreadFactory {

        @Override
        public Thread newThread(Runnable runnable) {
            Thread thread = new Thread(runnable, "GuideNH-PreviewPlayerSkin");
            thread.setDaemon(true);
            return thread;
        }
    }
}
