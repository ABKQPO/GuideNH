package com.hfstudio.guidenh.guide.internal.scene;

import net.minecraft.client.entity.EntityOtherPlayerMP;
import net.minecraft.world.World;

import com.mojang.authlib.GameProfile;
import com.hfstudio.guidenh.guide.scene.element.GuidebookCapeControllable;
import com.hfstudio.guidenh.guide.scene.element.GuidebookNameplateControllable;
import com.hfstudio.guidenh.guide.scene.element.GuidebookPlayerPoseControllable;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class GuidebookScenePreviewPlayerEntity
    extends EntityOtherPlayerMP
    implements GuidebookNameplateControllable, GuidebookCapeControllable, GuidebookPlayerPoseControllable {

    private boolean guidebookNameplateVisible = true;
    private boolean guidebookCapeVisible = true;
    private GuidebookPreviewPlayerPose guidebookPreviewPlayerPose = GuidebookPreviewPlayerPose.DEFAULT;

    public GuidebookScenePreviewPlayerEntity(World world, GameProfile gameProfile) {
        super(world, gameProfile);
    }

    @Override
    public void setGuidebookNameplateVisible(boolean visible) {
        this.guidebookNameplateVisible = visible;
    }

    @Override
    public boolean isGuidebookNameplateVisible() {
        return guidebookNameplateVisible;
    }

    @Override
    public void setGuidebookCapeVisible(boolean visible) {
        this.guidebookCapeVisible = visible;
        this.setHideCape(1, !visible);
    }

    @Override
    public boolean isGuidebookCapeVisible() {
        return guidebookCapeVisible;
    }

    @Override
    public void setGuidebookPreviewPlayerPose(GuidebookPreviewPlayerPose pose) {
        this.guidebookPreviewPlayerPose = pose != null ? pose : GuidebookPreviewPlayerPose.DEFAULT;
    }

    @Override
    public GuidebookPreviewPlayerPose getGuidebookPreviewPlayerPose() {
        return guidebookPreviewPlayerPose;
    }
}
