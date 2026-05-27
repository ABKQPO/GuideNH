package com.hfstudio.guidenh.guide.internal.scene;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import javax.imageio.ImageIO;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.renderer.ThreadDownloadImageData;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.resources.SkinManager;
import net.minecraft.util.ResourceLocation;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.guide.scene.element.GuidebookSceneEntityLoader;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.minecraft.InsecureTextureException;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import com.mojang.authlib.minecraft.MinecraftProfileTexture.Type;
import com.mojang.authlib.minecraft.MinecraftSessionService;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class GuidebookPreviewPlayerSkinResolver {

    private static final String PREVIEW_SKIN_RESOURCE_DOMAIN = "guidenh";
    private static final String PREVIEW_SKIN_RESOURCE_PATH_PREFIX = "preview-skins/";
    private static final ResourceLocation DEFAULT_PREVIEW_SKIN_LOCATION = new ResourceLocation(
        PREVIEW_SKIN_RESOURCE_DOMAIN,
        PREVIEW_SKIN_RESOURCE_PATH_PREFIX + "default-steve");
    private static final int MAX_RESOLVED_SKINS = 256;
    public static final ExecutorService LOOKUP_EXECUTOR = Executors
        .newSingleThreadExecutor(new GuidebookPreviewPlayerSkinThreadFactory());
    public static final Map<String, ResolvedPreviewPlayerSkin> RESOLVED_SKINS = createResolvedSkinCache();
    public static final Map<String, ResourceLocation> PREVIEW_SKIN_TEXTURE_LOCATIONS = new ConcurrentHashMap<>();
    public static final Map<String, List<WeakReference<GuidebookScenePreviewPlayerEntity>>> PENDING_ENTITIES = new ConcurrentHashMap<>();
    public static final Set<String> INFLIGHT_LOOKUPS = Collections.newSetFromMap(new ConcurrentHashMap<>());

    protected GuidebookPreviewPlayerSkinResolver() {}

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

        GameProfile cachedProfile = GuidebookSceneEntityLoader.findCachedPreviewPlayerProfile(playerName);
        GameProfile lookupProfile = selectLookupProfile(currentProfile, cachedProfile, playerName);
        if (hasUsableTextures(lookupProfile, playerName)) {
            ResolvedPreviewPlayerSkin resolvedSkin = resolveTexturesForProfile(lookupProfile);
            if (resolvedSkin != null && resolvedSkin.hasAnyTexture()) {
                RESOLVED_SKINS.put(cacheKey, resolvedSkin);
                applyResolvedSkin(entity, resolvedSkin);
                return;
            }
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

    public static void resolveSkinInBackground(String cacheKey, String playerName, GameProfile lookupProfile) {
        ResolvedPreviewPlayerSkin resolvedSkin = resolvePreviewPlayerSkinSafely(playerName, lookupProfile);
        Minecraft minecraft = Minecraft.getMinecraft();
        minecraft.func_152344_a(() -> applyResolvedSkinOnMainThread(cacheKey, playerName, resolvedSkin));
    }

    public static void applyResolvedSkinOnMainThread(String cacheKey, String playerName,
        ResolvedPreviewPlayerSkin resolvedSkin) {
        INFLIGHT_LOOKUPS.remove(cacheKey);
        if (resolvedSkin != null) {
            prepareResolvedSkinTextures(resolvedSkin);
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

    public static ResolvedPreviewPlayerSkin resolvePreviewPlayerSkin(String playerName, GameProfile lookupProfile) {
        ResolvedPreviewPlayerSkin resolvedFromProfile = resolveTexturesForProfile(lookupProfile);
        if (resolvedFromProfile != null && resolvedFromProfile.hasAnyTexture()) {
            return resolvedFromProfile;
        }

        GameProfile resolvedProfile = GuidebookSceneEntityLoader.lookupProfileFromRepository(playerName);
        return resolveTexturesForProfile(resolvedProfile);
    }

    @Nullable
    private static ResolvedPreviewPlayerSkin resolvePreviewPlayerSkinSafely(String playerName,
        GameProfile lookupProfile) {
        try {
            return resolvePreviewPlayerSkin(playerName, lookupProfile);
        } catch (Throwable ignored) {
            return null;
        }
    }

    public static ResolvedPreviewPlayerSkin resolveTexturesForProfile(GameProfile profile) {
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

        MinecraftProfileTexture skinTexture = textures.get(Type.SKIN);
        return new ResolvedPreviewPlayerSkin(
            resolvedProfile,
            skinTexture,
            textures.get(Type.CAPE),
            GuidebookPreviewPlayerCompat.resolveSlimSkinModel(resolvedProfile, skinTexture));
    }

    public static void applyResolvedSkin(GuidebookScenePreviewPlayerEntity entity,
        ResolvedPreviewPlayerSkin resolvedSkin) {
        SkinManager skinManager = Minecraft.getMinecraft()
            .func_152342_ad();
        ResourceLocation skinLocation = resolvedSkin.skinLocation != null ? resolvedSkin.skinLocation
            : resolvedSkin.skinTexture != null ? loadPreviewSkinTexture(resolvedSkin.skinTexture)
                : loadDefaultPreviewSkinTexture();
        entity.setGuidebookPreferredSkinLocation(skinLocation);
        entity.setGuidebookSlimArms(resolvedSkin.slimArms);
        if (resolvedSkin.capeTexture != null) {
            ResourceLocation capeLocation = skinManager.func_152789_a(resolvedSkin.capeTexture, Type.CAPE, entity);
            entity.func_152121_a(Type.CAPE, capeLocation);
        }
        if (!resolvedSkin.hasAnyTexture()) {
            skinManager.func_152790_a(resolvedSkin.profile, entity, true);
        }
    }

    public static ResourceLocation getDefaultPreviewSkinLocation() {
        return loadDefaultPreviewSkinTexture();
    }

    static boolean isGuidebookManagedSkinLocation(ResourceLocation skinLoc) {
        return skinLoc != null && PREVIEW_SKIN_RESOURCE_DOMAIN.equals(skinLoc.getResourceDomain())
            && skinLoc.getResourcePath()
                .startsWith(PREVIEW_SKIN_RESOURCE_PATH_PREFIX);
    }

    private static void prepareResolvedSkinTextures(ResolvedPreviewPlayerSkin resolvedSkin) {
        if (resolvedSkin.skinTexture != null && resolvedSkin.skinLocation == null) {
            resolvedSkin.skinLocation = loadPreviewSkinTexture(resolvedSkin.skinTexture);
        }
    }

    private static ResourceLocation loadPreviewSkinTexture(MinecraftProfileTexture skinTexture) {
        String textureHash = trimToNull(skinTexture.getHash());
        if (textureHash == null) {
            return loadDefaultPreviewSkinTexture();
        }

        ResourceLocation resourceLocation = PREVIEW_SKIN_TEXTURE_LOCATIONS.computeIfAbsent(
            textureHash,
            ignored -> new ResourceLocation(
                PREVIEW_SKIN_RESOURCE_DOMAIN,
                PREVIEW_SKIN_RESOURCE_PATH_PREFIX + textureHash));
        TextureManager textureManager = Minecraft.getMinecraft()
            .getTextureManager();
        if (textureManager.getTexture(resourceLocation) == null) {
            textureManager.loadTexture(
                resourceLocation,
                new ThreadDownloadImageData(
                    null,
                    skinTexture.getUrl(),
                    AbstractClientPlayer.locationStevePng,
                    new GuidebookPreviewPlayerSkinImageBuffer()));
        }
        return resourceLocation;
    }

    private static ResourceLocation loadDefaultPreviewSkinTexture() {
        TextureManager textureManager = Minecraft.getMinecraft()
            .getTextureManager();
        if (textureManager.getTexture(DEFAULT_PREVIEW_SKIN_LOCATION) != null) {
            return DEFAULT_PREVIEW_SKIN_LOCATION;
        }

        try (InputStream inputStream = Minecraft.getMinecraft()
            .getResourceManager()
            .getResource(AbstractClientPlayer.locationStevePng)
            .getInputStream()) {
            BufferedImage sourceImage = ImageIO.read(inputStream);
            BufferedImage convertedImage = GuidebookPreviewPlayerSkinImageBuffer.processSkinFormat(sourceImage);
            if (convertedImage == null) {
                return AbstractClientPlayer.locationStevePng;
            }
            textureManager.loadTexture(DEFAULT_PREVIEW_SKIN_LOCATION, new DynamicTexture(convertedImage));
            return DEFAULT_PREVIEW_SKIN_LOCATION;
        } catch (IOException ignored) {
            return AbstractClientPlayer.locationStevePng;
        }
    }

    public static void registerPendingEntity(String cacheKey, GuidebookScenePreviewPlayerEntity entity) {
        PENDING_ENTITIES.compute(cacheKey, (ignored, existing) -> {
            List<WeakReference<GuidebookScenePreviewPlayerEntity>> pending = existing;
            if (pending == null) {
                pending = Collections.synchronizedList(new ArrayList<>());
            }
            pending.add(new WeakReference<>(entity));
            return pending;
        });
    }

    public static GameProfile selectLookupProfile(GameProfile currentProfile, GameProfile cachedProfile,
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

    public static boolean needsBackgroundLookup(GameProfile profile, String playerName) {
        UUID profileId = profile == null ? null : profile.getId();
        return profileId == null || isOfflineFallbackProfile(profileId, playerName) || !hasTextures(profile);
    }

    public static boolean hasUsableTextures(GameProfile profile, String playerName) {
        return isUsableResolvedProfile(profile, playerName) && hasTextures(profile);
    }

    public static boolean isUsableResolvedProfile(GameProfile profile, String playerName) {
        return profile != null && profile.getId() != null && !isOfflineFallbackProfile(profile.getId(), playerName);
    }

    public static boolean isOfflineFallbackProfile(UUID profileId, String playerName) {
        UUID offlineUuid = UUID.nameUUIDFromBytes(("OfflinePlayer:" + playerName).getBytes(StandardCharsets.UTF_8));
        return offlineUuid.equals(profileId);
    }

    public static String normalizeCacheKey(String playerName) {
        String trimmedName = trimToNull(playerName);
        return trimmedName == null ? null : trimmedName.toLowerCase(Locale.ROOT);
    }

    public static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    public static boolean hasTextures(GameProfile profile) {
        try {
            return profile != null && profile.getProperties() != null
                && profile.getProperties()
                    .containsKey("textures");
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static Map<String, ResolvedPreviewPlayerSkin> createResolvedSkinCache() {
        return Collections
            .synchronizedMap(new LinkedHashMap<String, ResolvedPreviewPlayerSkin>(MAX_RESOLVED_SKINS + 1, 0.75f, true) {

                @Override
                protected boolean removeEldestEntry(Entry<String, ResolvedPreviewPlayerSkin> eldest) {
                    return size() > MAX_RESOLVED_SKINS;
                }
            });
    }

    public static class ResolvedPreviewPlayerSkin {

        private final GameProfile profile;
        private final MinecraftProfileTexture skinTexture;
        private final MinecraftProfileTexture capeTexture;
        @Nullable
        private final Boolean slimArms;
        @Nullable
        private ResourceLocation skinLocation;

        private ResolvedPreviewPlayerSkin(GameProfile profile, MinecraftProfileTexture skinTexture,
            MinecraftProfileTexture capeTexture, @Nullable Boolean slimArms) {
            this.profile = profile;
            this.skinTexture = skinTexture;
            this.capeTexture = capeTexture;
            this.slimArms = slimArms;
        }

        private boolean hasAnyTexture() {
            return skinTexture != null || capeTexture != null;
        }
    }

    public static class GuidebookPreviewPlayerSkinThreadFactory implements ThreadFactory {

        @Override
        public Thread newThread(@NotNull Runnable runnable) {
            Thread thread = new Thread(runnable, "GuideNH-PreviewPlayerSkin");
            thread.setDaemon(true);
            return thread;
        }
    }
}
