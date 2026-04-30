package com.hfstudio.guidenh.guide.internal.scene;

import java.util.UUID;

import net.minecraft.client.entity.EntityOtherPlayerMP;
import net.minecraft.crash.CrashReport;
import net.minecraft.crash.CrashReportCategory;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.ReportedException;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;

import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.guide.scene.element.GuidebookCapeControllable;
import com.hfstudio.guidenh.guide.scene.element.GuidebookNameplateControllable;
import com.hfstudio.guidenh.guide.scene.element.GuidebookPlayerPoseControllable;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.minecraft.MinecraftProfileTexture.Type;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class GuidebookScenePreviewPlayerEntity extends EntityOtherPlayerMP
    implements GuidebookNameplateControllable, GuidebookCapeControllable, GuidebookPlayerPoseControllable {

    private static final float ADULT_PLAYER_WIDTH = 0.6F;
    private static final float ADULT_PLAYER_HEIGHT = 1.8F;
    private static final float BABY_SCALE = 0.5F;

    private boolean guidebookNameplateVisible = true;
    private boolean guidebookCapeVisible = true;
    private boolean guidebookBaby;
    private GuidebookPreviewPlayerPose guidebookPreviewPlayerPose = GuidebookPreviewPlayerPose.DEFAULT;
    private ResourceLocation guidebookPreferredSkinLocation;
    @Nullable
    private Boolean guidebookSlimArms;

    public GuidebookScenePreviewPlayerEntity(World world, GameProfile gameProfile) {
        super(world, gameProfile);
        GuidebookPreviewPlayerSkinResolver.queueSkinRefresh(this);
    }

    @Override
    public void readFromNBT(NBTTagCompound tagCompound) {
        try {
            NBTTagList position = tagCompound.getTagList("Pos", 6);
            NBTTagList motion = tagCompound.getTagList("Motion", 6);
            NBTTagList rotation = tagCompound.getTagList("Rotation", 5);
            this.motionX = motion.func_150309_d(0);
            this.motionY = motion.func_150309_d(1);
            this.motionZ = motion.func_150309_d(2);

            if (Math.abs(this.motionX) > 10.0D) {
                this.motionX = 0.0D;
            }
            if (Math.abs(this.motionY) > 10.0D) {
                this.motionY = 0.0D;
            }
            if (Math.abs(this.motionZ) > 10.0D) {
                this.motionZ = 0.0D;
            }

            this.prevPosX = this.lastTickPosX = this.posX = position.func_150309_d(0);
            this.prevPosY = this.lastTickPosY = this.posY = position.func_150309_d(1);
            this.prevPosZ = this.lastTickPosZ = this.posZ = position.func_150309_d(2);
            this.prevRotationYaw = this.rotationYaw = rotation.func_150308_e(0);
            this.prevRotationPitch = this.rotationPitch = rotation.func_150308_e(1);
            this.fallDistance = tagCompound.getFloat("FallDistance");
            short fireTicks = tagCompound.getShort("Fire");
            if (fireTicks > 0) {
                this.setFire(Math.max(1, (fireTicks + 19) / 20));
            } else {
                this.extinguish();
            }
            this.setAir(tagCompound.getShort("Air"));
            this.onGround = tagCompound.getBoolean("OnGround");
            this.dimension = tagCompound.getInteger("Dimension");
            this.timeUntilPortal = tagCompound.getInteger("PortalCooldown");

            if (tagCompound.hasKey("UUIDMost", 4) && tagCompound.hasKey("UUIDLeast", 4)) {
                this.entityUniqueID = new UUID(tagCompound.getLong("UUIDMost"), tagCompound.getLong("UUIDLeast"));
            }

            this.setPosition(this.posX, this.posY, this.posZ);
            this.setRotation(this.rotationYaw, this.rotationPitch);

            // Preview players are client-only stand-ins. Skip Forge extended properties here so
            // broken third-party player data loaders cannot crash guide page compilation.
            if (tagCompound.hasKey("PersistentIDMSB") && tagCompound.hasKey("PersistentIDLSB")) {
                this.entityUniqueID = new UUID(
                    tagCompound.getLong("PersistentIDMSB"),
                    tagCompound.getLong("PersistentIDLSB"));
            }

            this.readEntityFromNBT(tagCompound);

            if (this.shouldSetPosAfterLoading()) {
                this.setPosition(this.posX, this.posY, this.posZ);
            }
        } catch (Throwable throwable) {
            CrashReport crashReport = CrashReport.makeCrashReport(throwable, "Loading entity NBT");
            CrashReportCategory crashReportCategory = crashReport.makeCategory("Entity being loaded");
            this.addEntityCrashInfo(crashReportCategory);
            throw new ReportedException(crashReport);
        }
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

    public void setGuidebookBaby(boolean baby) {
        this.guidebookBaby = baby;
        updateGuidebookSize();
    }

    @Override
    public boolean isChild() {
        return guidebookBaby;
    }

    public void setGuidebookPreferredSkinLocation(ResourceLocation skinLocation) {
        this.guidebookPreferredSkinLocation = skinLocation;
        if (skinLocation != null) {
            super.func_152121_a(Type.SKIN, skinLocation);
        }
    }

    public void setGuidebookSlimArms(@Nullable Boolean slimArms) {
        this.guidebookSlimArms = slimArms;
    }

    @Nullable
    public Boolean getGuidebookSlimArms() {
        return guidebookSlimArms;
    }

    @Override
    public void func_152121_a(Type skinPart, ResourceLocation skinLoc) {
        if (skinPart == Type.SKIN && shouldIgnoreVanillaSkinLocation(skinLoc)) {
            return;
        }
        super.func_152121_a(skinPart, skinLoc);
    }

    private boolean shouldIgnoreVanillaSkinLocation(ResourceLocation skinLoc) {
        return guidebookPreferredSkinLocation != null && skinLoc != null
            && !guidebookPreferredSkinLocation.equals(skinLoc)
            && !GuidebookPreviewPlayerSkinResolver.isGuidebookManagedSkinLocation(skinLoc);
    }

    private void updateGuidebookSize() {
        float scale = guidebookBaby ? BABY_SCALE : 1.0F;
        this.setSize(ADULT_PLAYER_WIDTH * scale, ADULT_PLAYER_HEIGHT * scale);
        this.setPosition(this.posX, this.posY, this.posZ);
    }
}
