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
        ImportedSceneEntity importedEntity = loadImportedEntityRecord(
            world,
            entry,
            offsetX,
            offsetY,
            offsetZ,
            0f,
            maxY);
        return importedEntity != null ? importedEntity.entity() : null;
    }

    @Nullable
    public static Entity loadImportedEntityUnclamped(@Nullable World world, NBTTagCompound entry, float offsetX,
        float offsetY, float offsetZ) {
        ImportedSceneEntity importedEntity = loadImportedEntityRecord(
            world,
            entry,
            offsetX,
            offsetY,
            offsetZ,
            Float.NEGATIVE_INFINITY,
            Float.POSITIVE_INFINITY);
        return importedEntity != null ? importedEntity.entity() : null;
    }

    @Nullable
    public static Entity loadImportedEntity(@Nullable World world, NBTTagCompound entry, float offsetX, float offsetY,
        float offsetZ, float minY, float maxY) {
        ImportedSceneEntity importedEntity = loadImportedEntityRecord(
            world,
            entry,
            offsetX,
            offsetY,
            offsetZ,
            minY,
            maxY);
        return importedEntity != null ? importedEntity.entity() : null;
    }

    @Nullable
    public static ImportedSceneEntity loadImportedEntityRecord(@Nullable World world, NBTTagCompound entry,
        float offsetX, float offsetY, float offsetZ, float minY, float maxY) {
        Entity entity = loadImportedEntityInstance(world, entry, offsetX, offsetY, offsetZ, minY, maxY);
        if (entity == null) {
            return null;
        }
        return new ImportedSceneEntity(
            entity,
            GuidebookSceneEntityLoader.trimToNull(entry.getString("sceneEntityId")),
            GuidebookSceneEntityLoader.trimToNull(entry.getString("mount")),
            resolveUnmount(entry));
    }

    @Nullable
    public static Entity loadImportedEntityInstance(@Nullable World world, NBTTagCompound entry, float offsetX,
        float offsetY, float offsetZ, float minY, float maxY) {
        if (entry == null) {
            return null;
        }

        String entityId = entry.getString("id");
        if (entityId == null || entityId.trim()
            .isEmpty()) {
            return null;
        }

        float px = entry.getFloat("px") + offsetX;
        float py = Math.clamp(entry.getFloat("py") + offsetY, minY, maxY);
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

    @Nullable
    public static Boolean resolveUnmount(@Nullable NBTTagCompound entry) {
        if (entry == null || !entry.hasKey("unmount")) {
            return null;
        }
        return entry.getBoolean("unmount");
    }

    public static class ImportedSceneEntity {

        private final Entity entity;
        @Nullable
        private final String sceneEntityId;
        @Nullable
        private final String mountTargetSceneEntityId;
        @Nullable
        private final Boolean unmount;

        public ImportedSceneEntity(Entity entity, @Nullable String sceneEntityId,
            @Nullable String mountTargetSceneEntityId, @Nullable Boolean unmount) {
            this.entity = entity;
            this.sceneEntityId = sceneEntityId;
            this.mountTargetSceneEntityId = mountTargetSceneEntityId;
            this.unmount = unmount;
        }

        public Entity entity() {
            return entity;
        }

        @Nullable
        public String sceneEntityId() {
            return sceneEntityId;
        }

        @Nullable
        public String mountTargetSceneEntityId() {
            return mountTargetSceneEntityId;
        }

        @Nullable
        public Boolean unmount() {
            return unmount;
        }
    }
}
