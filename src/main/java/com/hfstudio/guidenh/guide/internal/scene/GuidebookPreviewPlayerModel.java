package com.hfstudio.guidenh.guide.internal.scene;

import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.model.ModelRenderer;
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
    public final boolean smallArms;
    public ModelRenderer bipedLeftArmwear;
    public ModelRenderer bipedRightArmwear;
    public ModelRenderer bipedLeftLegwear;
    public ModelRenderer bipedRightLegwear;
    public ModelRenderer bipedBodyWear;

    GuidebookPreviewPlayerModel(float modelSize) {
        this(modelSize, true, false);
    }

    GuidebookPreviewPlayerModel(float modelSize, boolean modernSkinLayout) {
        this(modelSize, modernSkinLayout, false);
    }

    GuidebookPreviewPlayerModel(float modelSize, boolean modernSkinLayout, boolean smallArms) {
        super(modelSize, 0.0F, PLAYER_TEXTURE_WIDTH, resolveBaseTextureHeight(modernSkinLayout));
        this.smallArms = smallArms;
        if (modernSkinLayout) {
            if (GuidebookPreviewPlayerCompat.tryInitializeSimpleSkinBackport64xModel(this)) {
                return;
            }

            rebuildModernSkinModel(modelSize, smallArms);
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

    public static void applyRotationOverride(ModelRenderer renderer, Vector3f degrees) {
        if (degrees == null) {
            return;
        }
        renderer.rotateAngleX = degrees.x * DEGREES_TO_RADIANS;
        renderer.rotateAngleY = degrees.y * DEGREES_TO_RADIANS;
        renderer.rotateAngleZ = degrees.z * DEGREES_TO_RADIANS;
    }

    public void postRenderRightArm(float scale) {
        if (smallArms) {
            ++this.bipedRightArm.rotationPointX;
            this.bipedRightArm.postRender(scale);
            --this.bipedRightArm.rotationPointX;
            return;
        }

        this.bipedRightArm.postRender(scale);
    }

    private static int resolveBaseTextureHeight(boolean modernSkinLayout) {
        return modernSkinLayout && !GuidebookPreviewPlayerCompat.isSimpleSkinBackportAvailable() ? MODERN_TEXTURE_HEIGHT
            : LEGACY_TEXTURE_HEIGHT;
    }

    private void rebuildModernSkinModel(float modelSize, boolean smallArms) {
        this.bipedCloak = new ModelRenderer(this, 0, 0);
        this.bipedCloak.setTextureSize(64, 32);
        this.bipedCloak.addBox(-5.0F, 0.0F, -1.0F, 10, 16, 1, modelSize);

        if (smallArms) {
            this.bipedLeftArm = new ModelRenderer(this, 32, 48);
            this.bipedLeftArm.addBox(-1.0F, -2.0F, -2.0F, 3, 12, 4, modelSize);
            this.bipedLeftArm.setRotationPoint(5.0F, 2.5F, 0.0F);

            this.bipedRightArm = new ModelRenderer(this, 40, 16);
            this.bipedRightArm.addBox(-2.0F, -2.0F, -2.0F, 3, 12, 4, modelSize);
            this.bipedRightArm.setRotationPoint(-5.0F, 2.5F, 0.0F);

            this.bipedLeftArmwear = new ModelRenderer(this, 48, 48);
            this.bipedLeftArmwear.addBox(-1.0F, -2.0F, -2.0F, 3, 12, 4, modelSize + 0.25F);
            this.bipedLeftArm.addChild(this.bipedLeftArmwear);

            this.bipedRightArmwear = new ModelRenderer(this, 40, 32);
            this.bipedRightArmwear.addBox(-2.0F, -2.0F, -2.0F, 3, 12, 4, modelSize + 0.25F);
            this.bipedRightArm.addChild(this.bipedRightArmwear);
        } else {
            this.bipedLeftArm = new ModelRenderer(this, 32, 48);
            this.bipedLeftArm.addBox(-1.0F, -2.0F, -2.0F, 4, 12, 4, modelSize);
            this.bipedLeftArm.setRotationPoint(5.0F, 2.0F, 0.0F);

            this.bipedRightArm = new ModelRenderer(this, 40, 16);
            this.bipedRightArm.addBox(-3.0F, -2.0F, -2.0F, 4, 12, 4, modelSize);
            this.bipedRightArm.setRotationPoint(-5.0F, 2.0F, 0.0F);

            this.bipedLeftArmwear = new ModelRenderer(this, 48, 48);
            this.bipedLeftArmwear.addBox(-1.0F, -2.0F, -2.0F, 4, 12, 4, modelSize + 0.25F);
            this.bipedLeftArm.addChild(this.bipedLeftArmwear);

            this.bipedRightArmwear = new ModelRenderer(this, 40, 32);
            this.bipedRightArmwear.addBox(-3.0F, -2.0F, -2.0F, 4, 12, 4, modelSize + 0.25F);
            this.bipedRightArm.addChild(this.bipedRightArmwear);
        }

        this.bipedLeftLeg = new ModelRenderer(this, 16, 48);
        this.bipedLeftLeg.addBox(-2.0F, 0.0F, -2.0F, 4, 12, 4, modelSize);
        this.bipedLeftLeg.setRotationPoint(1.9F, 12.0F, 0.0F);

        this.bipedLeftLegwear = new ModelRenderer(this, 0, 48);
        this.bipedLeftLegwear.addBox(-2.0F, 0.0F, -2.0F, 4, 12, 4, modelSize + 0.25F);
        this.bipedLeftLeg.addChild(this.bipedLeftLegwear);

        this.bipedRightLegwear = new ModelRenderer(this, 0, 32);
        this.bipedRightLegwear.addBox(-2.0F, 0.0F, -2.0F, 4, 12, 4, modelSize + 0.25F);
        this.bipedRightLeg.addChild(this.bipedRightLegwear);

        this.bipedBodyWear = new ModelRenderer(this, 16, 32);
        this.bipedBodyWear.addBox(-4.0F, 0.0F, -2.0F, 8, 12, 4, modelSize + 0.25F);
        this.bipedBody.addChild(this.bipedBodyWear);
    }
}
