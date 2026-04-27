package com.hfstudio.guidenh.guide.internal.scene;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityClientPlayerMP;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.stats.StatFileWriter;
import net.minecraft.util.MovementInputFromOptions;

import com.hfstudio.guidenh.guide.scene.CameraSettings;
import com.hfstudio.guidenh.guide.scene.level.GuidebookLevel;

final class GuidebookPreviewPlayer extends EntityClientPlayerMP {

    private final float[] rayScratch = new float[6];

    GuidebookPreviewPlayer(Minecraft minecraft, WorldClient world, NetHandlerPlayClient netHandler) {
        super(minecraft, world, minecraft.getSession(), netHandler, new StatFileWriter());
        this.movementInput = new MovementInputFromOptions(minecraft.gameSettings);
        this.noClip = true;
        this.onGround = true;
    }

    void syncToPreviewWorld(WorldClient world, GuidebookLevel level, CameraSettings camera) {
        this.worldObj = world;
        this.dimension = world.provider.dimensionId;

        float[] ray = camera.screenToWorldRay(0f, 0f, rayScratch);
        int[] bounds = level.getBounds();
        float sizeX = bounds[3] - bounds[0] + 1f;
        float sizeY = bounds[4] - bounds[1] + 1f;
        float sizeZ = bounds[5] - bounds[2] + 1f;
        float radius = (float) Math.sqrt(sizeX * sizeX + sizeY * sizeY + sizeZ * sizeZ) * 0.5f;
        float distance = Math.max(16f, radius + 16f);

        double posX = ray[0] - ray[3] * distance;
        double posY = ray[1] - ray[4] * distance;
        double posZ = ray[2] - ray[5] * distance;
        float horizontal = (float) Math.sqrt(ray[3] * ray[3] + ray[5] * ray[5]);
        float yaw = (float) Math.toDegrees(Math.atan2(-ray[3], ray[5]));
        float pitch = (float) Math.toDegrees(Math.atan2(-ray[4], horizontal));

        this.prevPosX = this.lastTickPosX = this.posX = posX;
        this.prevPosY = this.lastTickPosY = this.posY = posY;
        this.prevPosZ = this.lastTickPosZ = this.posZ = posZ;
        this.prevRotationYaw = this.rotationYaw = yaw;
        this.prevRotationPitch = this.rotationPitch = pitch;
        this.setPositionAndRotation(posX, posY, posZ, yaw, pitch);
        this.setSneaking(false);
        this.setSprinting(false);
        this.ticksExisted = 0;
    }

    @Override
    public void onUpdate() {}

    @Override
    public void onLivingUpdate() {}

    @Override
    public void sendMotionUpdates() {}
}
