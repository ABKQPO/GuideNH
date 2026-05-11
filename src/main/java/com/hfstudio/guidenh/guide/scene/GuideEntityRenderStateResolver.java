package com.hfstudio.guidenh.guide.scene;

import net.minecraft.entity.Entity;

public class GuideEntityRenderStateResolver {

    private GuideEntityRenderStateResolver() {}

    public static ResolvedEntityRenderState resolve(Entity entity, float partialTicks) {
        return resolve(entity, partialTicks, new ResolvedEntityRenderState());
    }

    public static ResolvedEntityRenderState resolve(Entity entity, float partialTicks,
        ResolvedEntityRenderState result) {
        if (entity.ticksExisted == 0) {
            entity.lastTickPosX = entity.posX;
            entity.lastTickPosY = entity.posY;
            entity.lastTickPosZ = entity.posZ;
        }
        double x = entity.lastTickPosX + (entity.posX - entity.lastTickPosX) * partialTicks;
        double y = entity.lastTickPosY + (entity.posY - entity.lastTickPosY) * partialTicks;
        double z = entity.lastTickPosZ + (entity.posZ - entity.lastTickPosZ) * partialTicks;
        float yaw = entity.prevRotationYaw + (entity.rotationYaw - entity.prevRotationYaw) * partialTicks;
        return result.set(x, y, z, yaw);
    }

    public static class ResolvedEntityRenderState {

        private double x;
        private double y;
        private double z;
        private float yaw;

        public ResolvedEntityRenderState() {}

        private ResolvedEntityRenderState set(double x, double y, double z, float yaw) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.yaw = yaw;
            return this;
        }

        public double x() {
            return x;
        }

        public double y() {
            return y;
        }

        public double z() {
            return z;
        }

        public float yaw() {
            return yaw;
        }
    }
}
