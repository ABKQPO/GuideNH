package com.hfstudio.guidenh.guide.scene.ponder;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;

import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import com.hfstudio.guidenh.guide.internal.scene.GuidebookPreviewPlayerPose;
import com.hfstudio.guidenh.guide.scene.element.GuidebookPlayerPoseControllable;
import com.hfstudio.guidenh.guide.scene.element.GuidebookSceneEntityStateSupport;

public class PonderEntityAnimationRuntimeSupport {

    public static final int DEFAULT_LEFT_CLICK_TICKS = 6;
    public static final int DEFAULT_RIGHT_CLICK_TICKS = 6;
    public static final int DEFAULT_JUMP_TICKS = 12;
    public static final int DEFAULT_HURT_TICKS = 10;
    public static final int DEFAULT_SNEAK_TICKS = 1;
    public static final int DEFAULT_MOVE_TICKS = 20;
    public static final double DEFAULT_JUMP_HEIGHT = 1D;

    private PonderEntityAnimationRuntimeSupport() {}

    public static int resolveDurationTicks(PonderKeyframeEntityAnimation animation) {
        Integer explicitTicks = animation.getTicks();
        if (explicitTicks != null && explicitTicks > 0) {
            return explicitTicks;
        }

        String preset = normalizePreset(animation.getPreset());
        if ("leftclick".equals(preset)) {
            return DEFAULT_LEFT_CLICK_TICKS;
        }
        if ("rightclick".equals(preset)) {
            return DEFAULT_RIGHT_CLICK_TICKS;
        }
        if ("jump".equals(preset)) {
            return DEFAULT_JUMP_TICKS;
        }
        if ("hurt".equals(preset)) {
            return DEFAULT_HURT_TICKS;
        }
        if ("sneak".equals(preset) || "unsneak".equals(preset)) {
            return DEFAULT_SNEAK_TICKS;
        }
        return DEFAULT_MOVE_TICKS;
    }

    @Nullable
    public static String normalizePreset(@Nullable String preset) {
        return preset == null ? null
            : preset.trim()
                .toLowerCase();
    }

    public static boolean isPersistentPreset(@Nullable String preset) {
        String normalizedPreset = normalizePreset(preset);
        return "sneak".equals(normalizedPreset) || "unsneak".equals(normalizedPreset)
            || "walkto".equals(normalizedPreset)
            || "moveto".equals(normalizedPreset);
    }

    public static Baseline captureBaseline(Entity entity) {
        GuidebookPreviewPlayerPose previewPose = entity instanceof GuidebookPlayerPoseControllable poseControllable
            ? poseControllable.getGuidebookPreviewPlayerPose()
            : null;
        float bodyYaw = entity instanceof EntityLivingBase living ? living.renderYawOffset : entity.rotationYaw;
        float headYaw = entity instanceof EntityLivingBase living ? living.rotationYawHead : entity.rotationYaw;
        int hurtTime = entity instanceof EntityLivingBase living ? living.hurtTime : 0;
        int maxHurtTime = entity instanceof EntityLivingBase living ? living.maxHurtTime : 0;
        float attackedAtYaw = entity instanceof EntityLivingBase living ? living.attackedAtYaw : 0.0F;
        return new Baseline(
            entity.posX,
            entity.posY,
            entity.posZ,
            entity.rotationYaw,
            entity.rotationPitch,
            bodyYaw,
            headYaw,
            entity.motionX,
            entity.motionY,
            entity.motionZ,
            entity.isSneaking(),
            entity.onGround,
            hurtTime,
            maxHurtTime,
            attackedAtYaw,
            previewPose);
    }

    public static boolean restoreBaseline(Entity entity, Baseline baseline) {
        boolean changed = Double.compare(entity.posX, baseline.posX) != 0
            || Double.compare(entity.posY, baseline.posY) != 0
            || Double.compare(entity.posZ, baseline.posZ) != 0
            || entity.onGround != baseline.onGround;
        entity.setPosition(baseline.posX, baseline.posY, baseline.posZ);
        entity.rotationYaw = baseline.yaw;
        entity.rotationPitch = baseline.pitch;
        entity.motionX = baseline.motionX;
        entity.motionY = baseline.motionY;
        entity.motionZ = baseline.motionZ;
        if (entity instanceof EntityLivingBase living) {
            living.renderYawOffset = baseline.bodyYaw;
            living.rotationYawHead = baseline.headYaw;
            living.hurtTime = baseline.hurtTime;
            living.maxHurtTime = baseline.maxHurtTime;
            living.attackedAtYaw = baseline.attackedAtYaw;
        }
        entity.setSneaking(baseline.sneaking);
        entity.onGround = baseline.onGround;
        if (entity instanceof GuidebookPlayerPoseControllable poseControllable) {
            poseControllable.setGuidebookPreviewPlayerPose(baseline.previewPose);
        }
        return changed;
    }

    public static boolean apply(Entity entity, Baseline baseline, PonderKeyframeEntityAnimation animation,
        int elapsedTicks) {
        return apply(
            entity,
            baseline,
            animation,
            normalizePreset(animation.getPreset()),
            Math.max(1, resolveDurationTicks(animation)),
            elapsedTicks);
    }

    public static boolean apply(Entity entity, Baseline baseline, PonderKeyframeEntityAnimation animation,
        @Nullable String preset, int elapsedTicks) {
        return apply(entity, baseline, animation, preset, Math.max(1, resolveDurationTicks(animation)), elapsedTicks);
    }

    public static boolean apply(Entity entity, Baseline baseline, PonderKeyframeEntityAnimation animation,
        @Nullable String preset, int duration, int elapsedTicks) {
        if (preset == null || entity == null || baseline == null || elapsedTicks < 0) {
            return false;
        }

        int clampedElapsed = Math.min(elapsedTicks, duration - 1);
        float progress = duration <= 1 ? 1.0F : clampedElapsed / (float) (duration - 1);

        switch (preset) {
            case "leftclick" -> {
                applyLeftClick(entity, baseline, progress);
                return false;
            }
            case "rightclick" -> {
                applyRightClick(entity, baseline, progress);
                return false;
            }
            case "jump" -> {
                return applyJump(entity, baseline, animation, progress);
            }
            case "hurt" -> {
                applyHurt(entity, baseline, duration, elapsedTicks);
                return false;
            }
            case "sneak" -> {
                entity.setSneaking(true);
                return false;
            }
            case "unsneak" -> {
                entity.setSneaking(false);
                return false;
            }
            case "walkto" -> {
                return applyMove(entity, baseline, animation, progress, true);
            }
            case "moveto" -> {
                return applyMove(entity, baseline, animation, progress, false);
            }
            default -> {
                return false;
            }
        }
    }

    private static boolean applyMove(Entity entity, Baseline baseline, PonderKeyframeEntityAnimation animation,
        float progress, boolean rotateWithPath) {
        Double targetX = animation.getX();
        Double targetY = animation.getY();
        Double targetZ = animation.getZ();
        if (targetX == null && targetY == null && targetZ == null) {
            return false;
        }

        double endX = targetX != null ? targetX : baseline.posX;
        double endY = targetY != null ? targetY : baseline.posY;
        double endZ = targetZ != null ? targetZ : baseline.posZ;
        double currentX = lerp(baseline.posX, endX, progress);
        double currentY = lerp(baseline.posY, endY, progress);
        double currentZ = lerp(baseline.posZ, endZ, progress);
        boolean changed = Double.compare(entity.posX, currentX) != 0 || Double.compare(entity.posY, currentY) != 0
            || Double.compare(entity.posZ, currentZ) != 0
            || !entity.onGround;
        entity.setPosition(currentX, currentY, currentZ);
        entity.motionX = 0.0D;
        entity.motionY = 0.0D;
        entity.motionZ = 0.0D;
        entity.onGround = true;
        if (!rotateWithPath) {
            return changed;
        }

        applyWalkAnimation(entity, baseline, endX, endZ);
        double dx = endX - baseline.posX;
        double dz = endZ - baseline.posZ;
        if (Math.abs(dx) < 1.0E-5D && Math.abs(dz) < 1.0E-5D) {
            return changed;
        }
        float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
        GuidebookSceneEntityStateSupport.applyOptionalRotation(entity, yaw, null, yaw, yaw);
        return changed;
    }

    private static boolean applyJump(Entity entity, Baseline baseline, PonderKeyframeEntityAnimation animation,
        float progress) {
        double jumpHeight = animation.getHeight() != null ? animation.getHeight() : DEFAULT_JUMP_HEIGHT;
        double yOffset = 4.0D * jumpHeight * progress * (1.0D - progress);
        boolean onGround = progress >= 0.999F;
        boolean changed = Double.compare(entity.posX, baseline.posX) != 0
            || Double.compare(entity.posY, baseline.posY + yOffset) != 0
            || Double.compare(entity.posZ, baseline.posZ) != 0
            || entity.onGround != onGround;
        entity.setPosition(baseline.posX, baseline.posY + yOffset, baseline.posZ);
        entity.motionX = 0.0D;
        entity.motionY = 0.0D;
        entity.motionZ = 0.0D;
        entity.onGround = onGround;
        if (entity instanceof GuidebookPlayerPoseControllable poseControllable) {
            GuidebookPreviewPlayerPose pose = mergePose(
                baseline.previewPose,
                null,
                new Vector3f(-25.0F * progress, 0.0F, 0.0F),
                new Vector3f(-25.0F * progress, 0.0F, 0.0F),
                new Vector3f(35.0F * progress, 0.0F, 0.0F),
                new Vector3f(35.0F * progress, 0.0F, 0.0F),
                null);
            poseControllable.setGuidebookPreviewPlayerPose(pose);
        }
        return changed;
    }

    private static void applyHurt(Entity entity, Baseline baseline, int duration, int elapsedTicks) {
        if (!(entity instanceof EntityLivingBase living)) {
            return;
        }

        int remainingTicks = Math.max(0, duration - elapsedTicks);
        if (remainingTicks <= 0) {
            living.hurtTime = 0;
            living.maxHurtTime = duration;
            living.attackedAtYaw = baseline.attackedAtYaw;
            return;
        }

        // Guide preview ticks the fake world after the ponder state is applied, so keep one extra
        // tick buffered here to preserve the requested visible hurt duration on screen.
        living.hurtTime = remainingTicks + 1;
        living.maxHurtTime = duration;
        living.attackedAtYaw = 0.0F;
    }

    private static void applyLeftClick(Entity entity, Baseline baseline, float progress) {
        if (!(entity instanceof GuidebookPlayerPoseControllable poseControllable)) {
            return;
        }

        float swing = (float) Math.sin(progress * Math.PI);
        GuidebookPreviewPlayerPose pose = mergePose(
            baseline.previewPose,
            null,
            resolveRotation(baseline.previewPose, Limb.LEFT_ARM),
            offset(
                resolveRotation(baseline.previewPose, Limb.RIGHT_ARM),
                -78.0F * swing,
                -10.0F * swing,
                14.0F * swing),
            null,
            null,
            null);
        poseControllable.setGuidebookPreviewPlayerPose(pose);
    }

    private static void applyRightClick(Entity entity, Baseline baseline, float progress) {
        if (!(entity instanceof GuidebookPlayerPoseControllable poseControllable)) {
            return;
        }

        float lift = (float) Math.sin(progress * Math.PI);
        float hold = progress < 0.5F ? progress * 2.0F : (1.0F - progress) * 2.0F;
        GuidebookPreviewPlayerPose pose = mergePose(
            baseline.previewPose,
            null,
            null,
            offset(resolveRotation(baseline.previewPose, Limb.RIGHT_ARM), -42.0F * lift, -18.0F * hold, 8.0F * hold),
            null,
            null,
            null);
        poseControllable.setGuidebookPreviewPlayerPose(pose);
    }

    private static GuidebookPreviewPlayerPose mergePose(@Nullable GuidebookPreviewPlayerPose baseline,
        @Nullable Vector3f headRotation, @Nullable Vector3f leftArmRotation, @Nullable Vector3f rightArmRotation,
        @Nullable Vector3f leftLegRotation, @Nullable Vector3f rightLegRotation, @Nullable Vector3f capeRotation) {
        GuidebookPreviewPlayerPose source = baseline != null ? baseline : GuidebookPreviewPlayerPose.DEFAULT;
        return new GuidebookPreviewPlayerPose(
            headRotation != null ? headRotation : source.getHeadRotationDegrees(),
            leftArmRotation != null ? leftArmRotation : source.getLeftArmRotationDegrees(),
            rightArmRotation != null ? rightArmRotation : source.getRightArmRotationDegrees(),
            leftLegRotation != null ? leftLegRotation : source.getLeftLegRotationDegrees(),
            rightLegRotation != null ? rightLegRotation : source.getRightLegRotationDegrees(),
            capeRotation != null ? capeRotation : source.resolveCapeRotationDegrees());
    }

    @Nullable
    private static Vector3f resolveRotation(@Nullable GuidebookPreviewPlayerPose pose, Limb limb) {
        if (pose == null) {
            return null;
        }
        return switch (limb) {
            case HEAD -> pose.getHeadRotationDegrees();
            case LEFT_ARM -> pose.getLeftArmRotationDegrees();
            case RIGHT_ARM -> pose.getRightArmRotationDegrees();
            case LEFT_LEG -> pose.getLeftLegRotationDegrees();
            case RIGHT_LEG -> pose.getRightLegRotationDegrees();
            case CAPE -> pose.resolveCapeRotationDegrees();
        };
    }

    @Nullable
    private static Vector3f offset(@Nullable Vector3f base, float x, float y, float z) {
        Vector3f result = base != null ? new Vector3f(base) : new Vector3f();
        result.x += x;
        result.y += y;
        result.z += z;
        return result;
    }

    private static void applyWalkAnimation(Entity entity, Baseline baseline, double endX, double endZ) {
        if (!(entity instanceof EntityLivingBase living)) {
            return;
        }

        double dx = endX - baseline.posX;
        double dz = endZ - baseline.posZ;
        double distance = Math.sqrt(dx * dx + dz * dz);
        float swingAmount = (float) Math.min(1.0D, distance * 0.6D);
        living.prevLimbSwingAmount = living.limbSwingAmount;
        living.limbSwingAmount = swingAmount;
        living.limbSwing += Math.max(0.1F, swingAmount * 1.8F);
    }

    private static double lerp(double start, double end, float progress) {
        return start + (end - start) * progress;
    }

    public static class Baseline {

        private final double posX;
        private final double posY;
        private final double posZ;
        private final float yaw;
        private final float pitch;
        private final float bodyYaw;
        private final float headYaw;
        private final double motionX;
        private final double motionY;
        private final double motionZ;
        private final boolean sneaking;
        private final boolean onGround;
        private final int hurtTime;
        private final int maxHurtTime;
        private final float attackedAtYaw;
        private final GuidebookPreviewPlayerPose previewPose;

        public Baseline(double posX, double posY, double posZ, float yaw, float pitch, float bodyYaw, float headYaw,
            double motionX, double motionY, double motionZ, boolean sneaking, boolean onGround, int hurtTime,
            int maxHurtTime, float attackedAtYaw, @Nullable GuidebookPreviewPlayerPose previewPose) {
            this.posX = posX;
            this.posY = posY;
            this.posZ = posZ;
            this.yaw = yaw;
            this.pitch = pitch;
            this.bodyYaw = bodyYaw;
            this.headYaw = headYaw;
            this.motionX = motionX;
            this.motionY = motionY;
            this.motionZ = motionZ;
            this.sneaking = sneaking;
            this.onGround = onGround;
            this.hurtTime = hurtTime;
            this.maxHurtTime = maxHurtTime;
            this.attackedAtYaw = attackedAtYaw;
            this.previewPose = previewPose != null ? previewPose : GuidebookPreviewPlayerPose.DEFAULT;
        }
    }

    public static class TimedAnimation {

        private final int startTick;
        private final String ref;
        private final String preset;
        private final int durationTicks;
        private final boolean persistent;
        private final PonderKeyframeEntityAnimation animation;

        public TimedAnimation(int startTick, String ref, String preset, int durationTicks, boolean persistent,
            PonderKeyframeEntityAnimation animation) {
            this.startTick = startTick;
            this.ref = ref;
            this.preset = preset;
            this.durationTicks = durationTicks;
            this.persistent = persistent;
            this.animation = animation;
        }

        public int startTick() {
            return startTick;
        }

        public String ref() {
            return ref;
        }

        public String preset() {
            return preset;
        }

        public int durationTicks() {
            return durationTicks;
        }

        public boolean persistent() {
            return persistent;
        }

        public PonderKeyframeEntityAnimation animation() {
            return animation;
        }
    }

    private enum Limb {
        HEAD,
        LEFT_ARM,
        RIGHT_ARM,
        LEFT_LEG,
        RIGHT_LEG,
        CAPE
    }
}
