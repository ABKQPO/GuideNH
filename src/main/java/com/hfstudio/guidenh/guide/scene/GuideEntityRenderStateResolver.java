package com.hfstudio.guidenh.guide.scene;

import net.minecraft.entity.Entity;

public class GuideEntityRenderStateResolver {

    private GuideEntityRenderStateResolver() {}

    static ResolvedEntityRenderState resolve(Entity entity, float partialTicks) {
        if (entity.ticksExisted == 0) {
            entity.lastTickPosX = entity.posX;
            entity.lastTickPosY = entity.posY;
            entity.lastTickPosZ = entity.posZ;
        }
        double x = entity.lastTickPosX + (entity.posX - entity.lastTickPosX) * partialTicks;
        double y = entity.lastTickPosY + (entity.posY - entity.lastTickPosY) * partialTicks;
        double z = entity.lastTickPosZ + (entity.posZ - entity.lastTickPosZ) * partialTicks;
        float yaw = entity.prevRotationYaw + (entity.rotationYaw - entity.prevRotationYaw) * partialTicks;
        return new ResolvedEntityRenderState(x, y, z, yaw);
    }

    static public class ResolvedEntityRenderState {

        final double x;
        final double y;
        final double z;
        final float yaw;

        private ResolvedEntityRenderState(double x, double y, double z, float yaw) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.yaw = yaw;
        }
    }
}
