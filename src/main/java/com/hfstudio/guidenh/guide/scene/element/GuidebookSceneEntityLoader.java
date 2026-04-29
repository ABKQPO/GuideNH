package com.hfstudio.guidenh.guide.scene.element;

import java.lang.reflect.Constructor;
import java.net.Proxy;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.World;

import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.guide.internal.structure.GuideTextNbtCodec;
import com.mojang.authlib.Agent;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.GameProfileRepository;
import com.mojang.authlib.ProfileLookupCallback;
import com.mojang.authlib.yggdrasil.YggdrasilAuthenticationService;

public class GuidebookSceneEntityLoader {

    public static final Map<String, String> VANILLA_ENTITY_ID_ALIASES = createVanillaEntityIdAliases();
    public static final Set<String> PREVIEW_PLAYER_IDS = createPreviewPlayerIds();
    public static final Map<String, GameProfile> PREVIEW_PLAYER_PROFILE_CACHE = Collections
        .synchronizedMap(new LinkedHashMap<String, GameProfile>());
    public static volatile GameProfileRepository previewPlayerProfileRepository;

    private GuidebookSceneEntityLoader() {}

    @Nullable
    static Entity load(@Nullable World world, String entityId, @Nullable String rawData) {
        return load(world, entityId, rawData, null, null);
    }

    @Nullable
    static Entity load(@Nullable World world, String entityId, @Nullable String rawData, @Nullable String playerName,
        @Nullable String playerUuid) {
        NBTTagCompound data = new NBTTagCompound();
        if (rawData != null && !rawData.trim()
            .isEmpty()) {
            try {
                data = GuideTextNbtCodec.readTextSafeCompound(rawData);
            } catch (Exception e) {
                throw new IllegalArgumentException("Bad entity data NBT: " + e.getMessage(), e);
            }
        }

        return load(world, entityId, data, playerName, playerUuid);
    }

    @Nullable
    static Entity load(@Nullable World world, String entityId, @Nullable NBTTagCompound data) {
        return load(world, entityId, data, null, null);
    }

    @Nullable
    static Entity load(@Nullable World world, String entityId, @Nullable NBTTagCompound data,
        @Nullable String playerName, @Nullable String playerUuid) {
        if (entityId == null) {
            return null;
        }

        String trimmedId = entityId.trim();
        if (trimmedId.isEmpty()) {
            return null;
        }

        NBTTagCompound baseTag = data != null ? (NBTTagCompound) data.copy() : new NBTTagCompound();
        if (isPreviewPlayerId(trimmedId)) {
            return loadPreviewPlayer(world, baseTag, playerName, playerUuid);
        }

        for (String idCandidate : buildEntityIdCandidates(trimmedId)) {
            NBTTagCompound entityTag = (NBTTagCompound) baseTag.copy();
            entityTag.setString("id", idCandidate);
            Entity entity = EntityList.createEntityFromNBT(entityTag, world);
            if (entity != null) {
                if (world != null) {
                    entity.worldObj = world;
                    entity.dimension = world.provider.dimensionId;
                }
                return entity;
            }
        }

        return null;
    }

    @Nullable
    static GameProfileSpec resolvePreviewPlayerProfile(@Nullable String requestedName, @Nullable String requestedUuid) {
        String trimmedName = trimToNull(requestedName);
        String trimmedUuid = trimToNull(requestedUuid);
        boolean defaultSteveFallback = trimmedName == null && trimmedUuid == null;
        if (defaultSteveFallback) {
            trimmedName = "Steve";
        }

        UUID uuid = null;
        if (trimmedUuid != null) {
            try {
                uuid = UUID.fromString(trimmedUuid);
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Bad preview player uuid: " + trimmedUuid, e);
            }
        }

        String resolvedName = trimmedName;
        if (resolvedName == null && uuid != null) {
            resolvedName = "Player-" + uuid.toString()
                .substring(0, 8);
        }
        if (resolvedName == null) {
            return null;
        }
        if (uuid == null && defaultSteveFallback) {
            uuid = UUID.nameUUIDFromBytes(("OfflinePlayer:" + resolvedName).getBytes(StandardCharsets.UTF_8));
        }
        return new GameProfileSpec(uuid, resolvedName);
    }

    public static Set<String> buildEntityIdCandidates(String entityId) {
        LinkedHashSet<String> candidates = new LinkedHashSet<>();
        addCandidateForms(candidates, entityId);

        String normalized = entityId.toLowerCase(Locale.ROOT);
        int namespaceSeparator = normalized.indexOf(':');
        if (namespaceSeparator >= 0) {
            String namespace = normalized.substring(0, namespaceSeparator);
            String path = normalized.substring(namespaceSeparator + 1);
            if ("minecraft".equals(namespace)) {
                String alias = VANILLA_ENTITY_ID_ALIASES.get(path);
                if (alias != null) {
                    candidates.add(alias);
                }
            }
        } else {
            String alias = VANILLA_ENTITY_ID_ALIASES.get(normalized);
            if (alias != null) {
                candidates.add(alias);
            }
        }

        appendRegisteredEntityMatches(candidates, entityId);
        return candidates;
    }

    public static void appendRegisteredEntityMatches(Set<String> candidates, String entityId) {
        String normalizedInput = normalizeEntityId(entityId);
        if (normalizedInput == null) {
            return;
        }

        LinkedHashSet<String> uniqueSimpleMatches = new LinkedHashSet<>();
        boolean simpleLookup = shouldUseSimpleRegisteredLookup(entityId);

        for (String registeredId : EntityList.stringToClassMapping.keySet()) {
            if (registeredId == null) {
                continue;
            }

            String normalizedRegisteredId = normalizeEntityId(registeredId);
            if (normalizedInput.equals(normalizedRegisteredId)) {
                addCandidateForms(candidates, registeredId);
                continue;
            }

            if (simpleLookup && normalizedInput.equals(extractSimpleEntityToken(registeredId))) {
                uniqueSimpleMatches.add(registeredId);
            }
        }

        if (uniqueSimpleMatches.size() == 1) {
            addCandidateForms(
                candidates,
                uniqueSimpleMatches.iterator()
                    .next());
        }
    }

    public static void addCandidateForms(Set<String> candidates, @Nullable String candidate) {
        String trimmed = trimToNull(candidate);
        if (trimmed == null) {
            return;
        }

        candidates.add(trimmed);

        int namespaceSeparator = trimmed.indexOf(':');
        if (namespaceSeparator >= 0) {
            candidates.add(trimmed.substring(0, namespaceSeparator) + "." + trimmed.substring(namespaceSeparator + 1));
        }

        int dotSeparator = trimmed.indexOf('.');
        if (namespaceSeparator < 0 && dotSeparator > 0) {
            candidates.add(trimmed.substring(0, dotSeparator) + ":" + trimmed.substring(dotSeparator + 1));
        }
    }

    public static boolean shouldUseSimpleRegisteredLookup(String entityId) {
        return entityId.indexOf(':') < 0 && entityId.indexOf('.') < 0;
    }

    @Nullable
    public static String normalizeEntityId(@Nullable String entityId) {
        String trimmed = trimToNull(entityId);
        return trimmed == null ? null
            : trimmed.replace(':', '.')
                .toLowerCase(Locale.ROOT);
    }

    public static String extractSimpleEntityToken(String entityId) {
        int dot = entityId.lastIndexOf('.');
        int colon = entityId.lastIndexOf(':');
        int split = Math.max(dot, colon);
        String suffix = split >= 0 ? entityId.substring(split + 1) : entityId;
        return normalizeEntityId(suffix);
    }

    @Nullable
    public static Entity loadPreviewPlayer(@Nullable World world, NBTTagCompound data, @Nullable String playerName,
        @Nullable String playerUuid) {
        GameProfileSpec profileSpec = resolvePreviewPlayerProfile(playerName, playerUuid);
        if (profileSpec == null) {
            throw new IllegalArgumentException("Preview player entities require a name or uuid");
        }
        if (world == null) {
            throw new IllegalArgumentException("Preview player entities require an active client world");
        }

        GameProfile gameProfile = resolveInitialPreviewPlayerGameProfile(profileSpec);
        Entity entity = createPreviewPlayerEntity(world, gameProfile);
        if (entity == null) {
            throw new IllegalArgumentException("Failed to create preview player entity");
        }

        if (data != null) {
            entity.readFromNBT(data);
        }
        entity.worldObj = world;
        entity.dimension = world.provider.dimensionId;
        return entity;
    }

    @Nullable
    public static Entity createPreviewPlayerEntity(World world, GameProfile gameProfile) {
        try {
            Class<?> entityClass = Class
                .forName("com.hfstudio.guidenh.guide.internal.scene.GuidebookScenePreviewPlayerEntity");
            Constructor<?> constructor = entityClass.getConstructor(World.class, GameProfile.class);
            Object entity = constructor.newInstance(world, gameProfile);
            return entity instanceof Entity ? (Entity) entity : null;
        } catch (Throwable ignored) {
            try {
                Class<?> entityClass = Class.forName("net.minecraft.client.entity.EntityOtherPlayerMP");
                Constructor<?> constructor = entityClass.getConstructor(World.class, GameProfile.class);
                Object entity = constructor.newInstance(world, gameProfile);
                return entity instanceof Entity ? (Entity) entity : null;
            } catch (Throwable ignoredToo) {
                return null;
            }
        }
    }

    public static GameProfile resolveInitialPreviewPlayerGameProfile(GameProfileSpec profileSpec) {
        if (profileSpec.getUuid() != null) {
            return new GameProfile(profileSpec.getUuid(), profileSpec.getName());
        }

        GameProfile resolvedNamedProfile = resolveCachedPreviewPlayerProfile(profileSpec.getName());
        if (resolvedNamedProfile != null && resolvedNamedProfile.getId() != null) {
            return resolvedNamedProfile;
        }

        UUID offlineUuid = UUID
            .nameUUIDFromBytes(("OfflinePlayer:" + profileSpec.getName()).getBytes(StandardCharsets.UTF_8));
        return new GameProfile(offlineUuid, profileSpec.getName());
    }

    @Nullable
    public static GameProfile resolveCachedPreviewPlayerProfile(String playerName) {
        String cacheKey = normalizeProfileCacheKey(playerName);
        GameProfile cachedProfile = cacheKey == null ? null : PREVIEW_PLAYER_PROFILE_CACHE.get(cacheKey);
        if (cachedProfile != null) {
            return cachedProfile;
        }

        GameProfile profile = lookupProfileFromServerCache(playerName);
        if (isOfflineFallbackProfile(profile, playerName)) {
            profile = null;
        }
        if (profile == null) {
            return null;
        }

        cachePreviewPlayerProfile(playerName, profile);
        return profile;
    }

    @Nullable
    public static GameProfile lookupProfileFromServerCache(String playerName) {
        try {
            MinecraftServer minecraftServer = MinecraftServer.getServer();
            if (minecraftServer == null || minecraftServer.func_152358_ax() == null) {
                return null;
            }
            return minecraftServer.func_152358_ax()
                .func_152655_a(playerName);
        } catch (Throwable ignored) {
            return null;
        }
    }

    @Nullable
    public static GameProfile lookupProfileFromRepository(String playerName) {
        GameProfileRepository repository = getPreviewPlayerProfileRepository();
        if (repository == null) {
            return null;
        }

        final GameProfile[] resolvedProfile = new GameProfile[1];
        ProfileLookupCallback callback = new ProfileLookupCallback() {

            @Override
            public void onProfileLookupSucceeded(GameProfile profile) {
                resolvedProfile[0] = profile;
            }

            @Override
            public void onProfileLookupFailed(GameProfile profile, Exception exception) {
                resolvedProfile[0] = null;
            }
        };

        try {
            repository.findProfilesByNames(new String[] { playerName }, Agent.MINECRAFT, callback);
        } catch (Throwable ignored) {
            return null;
        }
        return resolvedProfile[0];
    }

    public static void cacheResolvedPreviewPlayerProfile(String playerName, GameProfile profile) {
        if (profile == null) {
            return;
        }
        cachePreviewPlayerProfile(playerName, profile);
    }

    @Nullable
    public static GameProfileRepository getPreviewPlayerProfileRepository() {
        GameProfileRepository repository = previewPlayerProfileRepository;
        if (repository != null) {
            return repository;
        }

        synchronized (GuidebookSceneEntityLoader.class) {
            repository = previewPlayerProfileRepository;
            if (repository != null) {
                return repository;
            }
            try {
                YggdrasilAuthenticationService authenticationService = new YggdrasilAuthenticationService(
                    resolveMinecraftProxy(),
                    UUID.randomUUID()
                        .toString());
                repository = authenticationService.createProfileRepository();
                previewPlayerProfileRepository = repository;
                return repository;
            } catch (Throwable ignored) {
                return null;
            }
        }
    }

    public static Proxy resolveMinecraftProxy() {
        try {
            Class<?> minecraftClass = Class.forName("net.minecraft.client.Minecraft");
            Object minecraft = minecraftClass.getMethod("getMinecraft")
                .invoke(null);
            Object proxy = minecraftClass.getMethod("getProxy")
                .invoke(minecraft);
            return proxy instanceof Proxy ? (Proxy) proxy : Proxy.NO_PROXY;
        } catch (Throwable ignored) {
            return Proxy.NO_PROXY;
        }
    }

    public static void cachePreviewPlayerProfile(String playerName, GameProfile profile) {
        String cacheKey = normalizeProfileCacheKey(playerName);
        if (cacheKey != null) {
            PREVIEW_PLAYER_PROFILE_CACHE.put(cacheKey, profile);
        }
        if (profile != null && profile.getId() != null) {
            PREVIEW_PLAYER_PROFILE_CACHE.put(
                profile.getId()
                    .toString(),
                profile);
        }
    }

    public static boolean isOfflineFallbackProfile(@Nullable GameProfile profile, String requestedName) {
        if (profile == null || profile.getId() == null) {
            return false;
        }
        UUID offlineUuid = UUID.nameUUIDFromBytes(("OfflinePlayer:" + requestedName).getBytes(StandardCharsets.UTF_8));
        return offlineUuid.equals(profile.getId()) && !hasTextures(profile);
    }

    @Nullable
    public static String normalizeProfileCacheKey(@Nullable String playerName) {
        String trimmedName = trimToNull(playerName);
        return trimmedName == null ? null : trimmedName.toLowerCase(Locale.ROOT);
    }

    public static boolean hasTextures(GameProfile profile) {
        try {
            return profile.getProperties() != null && profile.getProperties()
                .containsKey("textures");
        } catch (Throwable ignored) {
            return false;
        }
    }

    static boolean isPreviewPlayerId(String entityId) {
        String normalized = normalizeEntityId(entityId);
        return normalized != null && PREVIEW_PLAYER_IDS.contains(normalized);
    }

    @Nullable
    public static String trimToNull(@Nullable String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    public static Set<String> createPreviewPlayerIds() {
        LinkedHashSet<String> ids = new LinkedHashSet<>();
        ids.add("player");
        ids.add("minecraft.player");
        ids.add("fakeplayer");
        ids.add("minecraft.fakeplayer");
        return ids;
    }

    public static Map<String, String> createVanillaEntityIdAliases() {
        Map<String, String> aliases = new LinkedHashMap<>();
        aliases.put("item", "Item");
        aliases.put("experience_orb", "XPOrb");
        aliases.put("leash_knot", "LeashKnot");
        aliases.put("painting", "Painting");
        aliases.put("arrow", "Arrow");
        aliases.put("snowball", "Snowball");
        aliases.put("large_fireball", "Fireball");
        aliases.put("fireball", "Fireball");
        aliases.put("small_fireball", "SmallFireball");
        aliases.put("ender_pearl", "ThrownEnderpearl");
        aliases.put("eye_of_ender", "EyeOfEnderSignal");
        aliases.put("potion", "ThrownPotion");
        aliases.put("experience_bottle", "ThrownExpBottle");
        aliases.put("item_frame", "ItemFrame");
        aliases.put("wither_skull", "WitherSkull");
        aliases.put("tnt", "PrimedTnt");
        aliases.put("falling_block", "FallingSand");
        aliases.put("firework_rocket", "FireworksRocketEntity");
        aliases.put("boat", "Boat");
        aliases.put("minecart", "MinecartRideable");
        aliases.put("chest_minecart", "MinecartChest");
        aliases.put("furnace_minecart", "MinecartFurnace");
        aliases.put("tnt_minecart", "MinecartTNT");
        aliases.put("hopper_minecart", "MinecartHopper");
        aliases.put("spawner_minecart", "MinecartSpawner");
        aliases.put("command_block_minecart", "MinecartCommandBlock");
        aliases.put("creeper", "Creeper");
        aliases.put("skeleton", "Skeleton");
        aliases.put("spider", "Spider");
        aliases.put("giant", "Giant");
        aliases.put("zombie", "Zombie");
        aliases.put("slime", "Slime");
        aliases.put("ghast", "Ghast");
        aliases.put("zombie_pigman", "PigZombie");
        aliases.put("enderman", "Enderman");
        aliases.put("cave_spider", "CaveSpider");
        aliases.put("silverfish", "Silverfish");
        aliases.put("blaze", "Blaze");
        aliases.put("magma_cube", "LavaSlime");
        aliases.put("ender_dragon", "EnderDragon");
        aliases.put("wither", "WitherBoss");
        aliases.put("bat", "Bat");
        aliases.put("witch", "Witch");
        aliases.put("pig", "Pig");
        aliases.put("sheep", "Sheep");
        aliases.put("cow", "Cow");
        aliases.put("chicken", "Chicken");
        aliases.put("squid", "Squid");
        aliases.put("wolf", "Wolf");
        aliases.put("mooshroom", "MushroomCow");
        aliases.put("snow_golem", "SnowMan");
        aliases.put("ocelot", "Ozelot");
        aliases.put("iron_golem", "VillagerGolem");
        aliases.put("horse", "EntityHorse");
        aliases.put("villager", "Villager");
        aliases.put("ender_crystal", "EnderCrystal");
        return aliases;
    }

    static public class GameProfileSpec {

        private final UUID uuid;
        private final String name;

        private GameProfileSpec(UUID uuid, String name) {
            this.uuid = uuid;
            this.name = name;
        }

        public UUID getUuid() {
            return uuid;
        }

        public String getName() {
            return name;
        }
    }
}
