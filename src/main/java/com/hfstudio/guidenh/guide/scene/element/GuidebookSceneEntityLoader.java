package com.hfstudio.guidenh.guide.scene.element;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;

import com.hfstudio.guidenh.guide.internal.structure.GuideTextNbtCodec;

final class GuidebookSceneEntityLoader {

    private static final Map<String, String> VANILLA_ENTITY_ID_ALIASES = createVanillaEntityIdAliases();

    private GuidebookSceneEntityLoader() {}

    @Nullable
    static Entity load(@Nullable World world, String entityId, @Nullable String rawData) {
        NBTTagCompound data = new NBTTagCompound();
        if (rawData != null && !rawData.trim()
            .isEmpty()) {
            try {
                data = GuideTextNbtCodec.readTextSafeCompound(rawData);
            } catch (Exception e) {
                throw new IllegalArgumentException("Bad entity data NBT: " + e.getMessage(), e);
            }
        }

        return load(world, entityId, data);
    }

    @Nullable
    static Entity load(@Nullable World world, String entityId, @Nullable NBTTagCompound data) {
        if (entityId == null) {
            return null;
        }

        String trimmedId = entityId.trim();
        if (trimmedId.isEmpty()) {
            return null;
        }

        NBTTagCompound baseTag = data != null ? (NBTTagCompound) data.copy() : new NBTTagCompound();
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

    private static Set<String> buildEntityIdCandidates(String entityId) {
        LinkedHashSet<String> candidates = new LinkedHashSet<>();
        candidates.add(entityId);

        String normalized = entityId.toLowerCase(Locale.ROOT);
        int namespaceSeparator = normalized.indexOf(':');
        if (namespaceSeparator >= 0) {
            candidates.add(entityId.substring(0, namespaceSeparator) + "." + entityId.substring(namespaceSeparator + 1));

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

        return candidates;
    }

    private static Map<String, String> createVanillaEntityIdAliases() {
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
}
