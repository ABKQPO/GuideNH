package com.hfstudio.guidenh.guide.internal.scene;

import net.minecraft.client.model.ModelBiped;
import net.minecraft.entity.Entity;

import org.joml.Vector3f;

import com.hfstudio.guidenh.guide.scene.element.GuidebookPlayerPoseControllable;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class GuidebookPreviewPlayerModel extends ModelBiped {

    public static final float DEGREES_TO_RADIANS = (float) Math.PI / 180.0f;

    GuidebookPreviewPlayerModel(float modelSize) {
        super(modelSize);
    }

    @Override
    public void setRotationAngles(float p_78087_1_, float p_78087_2_, float p_78087_3_, float p_78087_4_,
        float p_78087_5_, float p_78087_6_, Entity p_78087_7_) {
        super.setRotationAngles(p_78087_1_, p_78087_2_, p_78087_3_, p_78087_4_, p_78087_5_, p_78087_6_, p_78087_7_);

        if (!(p_78087_7_ instanceof GuidebookPlayerPoseControllable poseControllable)) {
            return;
        }

        GuidebookPreviewPlayerPose pose = poseControllable.getGuidebookPreviewPlayerPose();
        if (pose == null) {
            return;
        }

        applyRotationOverride(this.bipedHead, pose.getHeadRotationDegrees());
        this.bipedHeadwear.rotateAngleX = this.bipedHead.rotateAngleX;
        this.bipedHeadwear.rotateAngleY = this.bipedHead.rotateAngleY;
        this.bipedHeadwear.rotateAngleZ = this.bipedHead.rotateAngleZ;
        applyRotationOverride(this.bipedLeftArm, pose.getLeftArmRotationDegrees());
        applyRotationOverride(this.bipedRightArm, pose.getRightArmRotationDegrees());
        applyRotationOverride(this.bipedLeftLeg, pose.getLeftLegRotationDegrees());
        applyRotationOverride(this.bipedRightLeg, pose.getRightLegRotationDegrees());
    }

    public static void applyRotationOverride(net.minecraft.client.model.ModelRenderer renderer, Vector3f degrees) {
        if (degrees == null) {
            return;
        }
        renderer.rotateAngleX = degrees.x * DEGREES_TO_RADIANS;
        renderer.rotateAngleY = degrees.y * DEGREES_TO_RADIANS;
        renderer.rotateAngleZ = degrees.z * DEGREES_TO_RADIANS;
    }
}
