package com.hfstudio.guidenh.guide.scene.element;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;

import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.guide.scene.support.GuideEntityDisplayResolver;

public class GuidebookSceneEntityImportSupport {

    private GuidebookSceneEntityImportSupport() {}

    @Nullable
    public static Entity loadImportedEntity(@Nullable World world, NBTTagCompound entry, float offsetX, float offsetY,
        float offsetZ, float maxY) {
        return loadImportedEntity(world, entry, offsetX, offsetY, offsetZ, 0f, maxY);
    }

    @Nullable
    public static Entity loadImportedEntityUnclamped(@Nullable World world, NBTTagCompound entry, float offsetX,
        float offsetY, float offsetZ) {
        return loadImportedEntity(
            world,
            entry,
            offsetX,
            offsetY,
            offsetZ,
            Float.NEGATIVE_INFINITY,
            Float.POSITIVE_INFINITY);
    }

    @Nullable
    public static Entity loadImportedEntity(@Nullable World world, NBTTagCompound entry, float offsetX, float offsetY,
        float offsetZ, float minY, float maxY) {
        if (entry == null) {
            return null;
        }

        String entityId = entry.getString("id");
        if (entityId == null || entityId.trim()
            .isEmpty()) {
            return null;
        }

        float px = entry.getFloat("px") + offsetX;
        float py = Math.max(minY, Math.min(entry.getFloat("py") + offsetY, maxY));
        float pz = entry.getFloat("pz") + offsetZ;
        String playerName = entry.hasKey("name", 8) ? entry.getString("name") : null;
        NBTTagCompound entityNbt = entry.hasKey("nbt", 10) ? (NBTTagCompound) entry.getCompoundTag("nbt")
            .copy() : new NBTTagCompound();
        sanitizeCustomName(entityNbt);

        Entity entity;
        try {
            entity = GuidebookSceneEntityLoader.loadFromNbt(world, entityId, entityNbt, playerName, null);
        } catch (Throwable ignored) {
            return null;
        }
        if (entity == null) {
            return null;
        }

        entity.setPosition(px, py, pz);
        if (entry.hasKey("yaw") || entry.hasKey("pitch")) {
            applyRotation(
                entity,
                entry.getFloat("yaw"),
                entry.getFloat("pitch"),
                entry.hasKey("bodyYaw") ? entry.getFloat("bodyYaw") : entry.getFloat("yaw"),
                entry.hasKey("headYaw") ? entry.getFloat("headYaw") : entry.getFloat("yaw"));
        } else {
            syncPreviousTransform(entity);
        }
        return entity;
    }

    public static void applyRotation(Entity entity, float yaw, float pitch, float bodyYaw, float headYaw) {
        if (entity == null) {
            return;
        }

        entity.rotationYaw = yaw;
        entity.rotationPitch = pitch;
        entity.prevRotationYaw = yaw;
        entity.prevRotationPitch = pitch;
        if (entity instanceof EntityLivingBase living) {
            living.renderYawOffset = bodyYaw;
            living.prevRenderYawOffset = bodyYaw;
            living.rotationYawHead = headYaw;
            living.prevRotationYawHead = headYaw;
        }
        syncPreviousTransform(entity);
    }

    public static void syncPreviousTransform(Entity entity) {
        if (entity == null) {
            return;
        }

        entity.lastTickPosX = entity.posX;
        entity.lastTickPosY = entity.posY;
        entity.lastTickPosZ = entity.posZ;
        entity.prevPosX = entity.posX;
        entity.prevPosY = entity.posY;
        entity.prevPosZ = entity.posZ;
    }

    public static void sanitizeCustomName(NBTTagCompound entityNbt) {
        if (entityNbt == null || !entityNbt.hasKey("CustomName", 8)) {
            return;
        }

        String customName = entityNbt.getString("CustomName");
        if (!GuideEntityDisplayResolver.isUsefulDisplayName(customName)) {
            entityNbt.removeTag("CustomName");
            entityNbt.removeTag("CustomNameVisible");
        }
    }
}
