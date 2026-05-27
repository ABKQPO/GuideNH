package com.hfstudio.guidenh.guide.scene.element;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityAgeable;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.monster.EntityZombie;

import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import com.hfstudio.guidenh.guide.compiler.tags.MdxAttrs;
import com.hfstudio.guidenh.guide.internal.scene.GuidebookPreviewPlayerPose;
import com.hfstudio.guidenh.guide.internal.scene.GuidebookScenePreviewPlayerEntity;

public class GuidebookSceneEntityStateSupport {

    private static final Map<String, Method> BOOLEAN_METHOD_CACHE = new ConcurrentHashMap<>();
    private static final Set<String> BOOLEAN_METHOD_MISS_CACHE = ConcurrentHashMap.newKeySet();

    private GuidebookSceneEntityStateSupport() {}

    @Nullable
    public static Vector3f parseOptionalVector3(@Nullable String raw) {
        String trimmed = GuidebookSceneEntityLoader.trimToNull(raw);
        if (trimmed == null) {
            return null;
        }

        float[] parts = MdxAttrs.parseVector3Parts(trimmed);
        if (parts == null) {
            return null;
        }
        return new Vector3f(parts[0], parts[1], parts[2]);
    }

    @Nullable
    public static GuidebookPreviewPlayerPose createPreviewPlayerPose(@Nullable Vector3f headRotation,
        @Nullable Vector3f leftArmRotation, @Nullable Vector3f rightArmRotation, @Nullable Vector3f leftLegRotation,
        @Nullable Vector3f rightLegRotation, @Nullable Vector3f capeRotation) {
        if (headRotation == null && leftArmRotation == null
            && rightArmRotation == null
            && leftLegRotation == null
            && rightLegRotation == null
            && capeRotation == null) {
            return null;
        }
        return new GuidebookPreviewPlayerPose(
            headRotation,
            leftArmRotation,
            rightArmRotation,
            leftLegRotation,
            rightLegRotation,
            capeRotation);
    }

    public static boolean applyOptionalPosition(Entity entity, @Nullable Double x, @Nullable Double y,
        @Nullable Double z) {
        if (entity == null || x == null && y == null && z == null) {
            return false;
        }

        double resolvedX = x != null ? x : entity.posX;
        double resolvedY = y != null ? y : entity.posY;
        double resolvedZ = z != null ? z : entity.posZ;
        boolean changed = Double.compare(entity.posX, resolvedX) != 0 || Double.compare(entity.posY, resolvedY) != 0
            || Double.compare(entity.posZ, resolvedZ) != 0;
        entity.setPosition(resolvedX, resolvedY, resolvedZ);
        GuidebookSceneEntityImportSupport.syncPreviousTransform(entity);
        entity.prevRotationYaw = entity.rotationYaw;
        entity.prevRotationPitch = entity.rotationPitch;
        return changed;
    }

    public static void applyOptionalRotation(Entity entity, @Nullable Float yaw, @Nullable Float pitch,
        @Nullable Float bodyYaw, @Nullable Float headYaw) {
        if (entity == null || yaw == null && pitch == null && bodyYaw == null && headYaw == null) {
            return;
        }

        float resolvedYaw = yaw != null ? yaw : entity.rotationYaw;
        float resolvedPitch = pitch != null ? pitch : entity.rotationPitch;
        boolean yawSpecified = yaw != null;
        float resolvedBodyYaw = resolvedYaw;
        float resolvedHeadYaw = resolvedYaw;
        if (entity instanceof EntityLivingBase living) {
            resolvedBodyYaw = bodyYaw != null ? bodyYaw : yawSpecified ? resolvedYaw : living.renderYawOffset;
            resolvedHeadYaw = headYaw != null ? headYaw : yawSpecified ? resolvedYaw : living.rotationYawHead;
        } else {
            resolvedBodyYaw = bodyYaw != null ? bodyYaw : resolvedYaw;
            resolvedHeadYaw = headYaw != null ? headYaw : resolvedYaw;
        }
        GuidebookSceneEntityImportSupport
            .applyRotation(entity, resolvedYaw, resolvedPitch, resolvedBodyYaw, resolvedHeadYaw);
    }

    public static boolean applyVisualState(Entity entity, @Nullable String entityId, @Nullable Boolean showName,
        @Nullable Boolean showCape, @Nullable Boolean baby, @Nullable GuidebookPreviewPlayerPose pose,
        boolean usePreviewDefaults) {
        if (entity == null) {
            return false;
        }

        boolean previewPlayer = entityId != null && GuidebookSceneEntityLoader.isPreviewPlayerId(entityId);
        if (entity instanceof GuidebookNameplateControllable nameplateControllable) {
            if (showName != null || usePreviewDefaults && previewPlayer) {
                nameplateControllable.setGuidebookNameplateVisible(showName != null ? showName : true);
            }
        } else if (showName != null && entity instanceof EntityLiving living) {
            living.setAlwaysRenderNameTag(showName);
        }

        if (entity instanceof GuidebookCapeControllable capeControllable
            && (showCape != null || usePreviewDefaults && previewPlayer)) {
            capeControllable.setGuidebookCapeVisible(showCape != null ? showCape : true);
        }

        if (pose != null && entity instanceof GuidebookPlayerPoseControllable poseControllable) {
            poseControllable.setGuidebookPreviewPlayerPose(pose);
        }

        return applyBabyState(entity, baby);
    }

    public static boolean applyBabyState(Entity entity, @Nullable Boolean baby) {
        if (entity == null || baby == null) {
            return false;
        }

        boolean child = baby;
        if (entity instanceof GuidebookScenePreviewPlayerEntity previewPlayer) {
            if (previewPlayer.isChild() == child) {
                return false;
            }
            previewPlayer.setGuidebookBaby(child);
            return true;
        }

        if (entity instanceof EntityAgeable ageable) {
            if (ageable.isChild() == child) {
                return false;
            }
            ageable.setGrowingAge(child ? -24000 : 0);
            realignEntityBounds(entity);
            return true;
        }

        if (entity instanceof EntityZombie zombie) {
            if (zombie.isChild() == child) {
                return false;
            }
            zombie.setChild(child);
            realignEntityBounds(entity);
            return true;
        }

        if (tryInvokeBooleanInstanceMethod(entity, "setChild", child)) {
            realignEntityBounds(entity);
            return true;
        }

        if (tryInvokeBooleanInstanceMethod(entity, "setBaby", child)) {
            realignEntityBounds(entity);
            return true;
        }
        return false;
    }

    public static void realignEntityBounds(Entity entity) {
        entity.setPosition(entity.posX, entity.posY, entity.posZ);
    }

    public static boolean tryInvokeBooleanInstanceMethod(Object target, String methodName, boolean argument) {
        if (target == null) {
            return false;
        }

        String cacheKey = target.getClass()
            .getName() + "#"
            + methodName;
        Method cachedMethod = BOOLEAN_METHOD_CACHE.get(cacheKey);
        if (cachedMethod != null) {
            try {
                cachedMethod.invoke(target, argument);
                return true;
            } catch (ReflectiveOperationException ignored) {
                BOOLEAN_METHOD_CACHE.remove(cacheKey);
            }
        }

        if (BOOLEAN_METHOD_MISS_CACHE.contains(cacheKey)) {
            return false;
        }

        try {
            Method method = target.getClass()
                .getMethod(methodName, boolean.class);
            BOOLEAN_METHOD_CACHE.put(cacheKey, method);
            method.invoke(target, argument);
            return true;
        } catch (ReflectiveOperationException ignored) {
            BOOLEAN_METHOD_MISS_CACHE.add(cacheKey);
            return false;
        }
    }
}
