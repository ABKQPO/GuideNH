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
    private static final int PLAYER_TEXTURE_WIDTH = 64;
    private static final int LEGACY_TEXTURE_HEIGHT = 32;
    private static final int MODERN_TEXTURE_HEIGHT = 64;

    GuidebookPreviewPlayerModel(float modelSize) {
        this(modelSize, true);
    }

    GuidebookPreviewPlayerModel(float modelSize, boolean modernSkinLayout) {
        super(modelSize, 0.0F, PLAYER_TEXTURE_WIDTH, modernSkinLayout ? MODERN_TEXTURE_HEIGHT : LEGACY_TEXTURE_HEIGHT);
        if (modernSkinLayout) {
            rebuildModernSkinLimbs(modelSize);
        }
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

    private void rebuildModernSkinLimbs(float modelSize) {
        this.bipedLeftArm = new net.minecraft.client.model.ModelRenderer(this, 32, 48);
        this.bipedLeftArm.mirror = true;
        this.bipedLeftArm.addBox(-1.0F, -2.0F, -2.0F, 4, 12, 4, modelSize);
        this.bipedLeftArm.setRotationPoint(5.0F, 2.0F, 0.0F);

        this.bipedLeftLeg = new net.minecraft.client.model.ModelRenderer(this, 16, 48);
        this.bipedLeftLeg.mirror = true;
        this.bipedLeftLeg.addBox(-2.0F, 0.0F, -2.0F, 4, 12, 4, modelSize);
        this.bipedLeftLeg.setRotationPoint(1.9F, 12.0F, 0.0F);
    }
}
