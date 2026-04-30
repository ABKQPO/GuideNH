package com.hfstudio.guidenh.guide.internal.scene;

import net.minecraft.block.Block;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.entity.RenderPlayer;
import net.minecraft.client.renderer.tileentity.TileEntitySkullRenderer;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.init.Items;
import net.minecraft.item.EnumAction;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTUtil;
import net.minecraft.util.StringUtils;

import org.joml.Vector3f;
import org.lwjgl.opengl.GL11;

import com.hfstudio.guidenh.guide.scene.element.GuidebookNameplateControllable;
import com.hfstudio.guidenh.guide.scene.element.GuidebookPlayerPoseControllable;
import com.mojang.authlib.GameProfile;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class GuidebookPreviewPlayerRenderer extends RenderPlayer {

    public static final GuidebookPreviewPlayerRenderer INSTANCE = new GuidebookPreviewPlayerRenderer();
    private static final int CHEST_ARMOR_SLOT = 2;
    private static final int CHEST_RENDER_PASS = 1;
    private static final float MODEL_RENDER_SCALE = 0.0625F;
    private static final float CHILD_BODY_SCALE = 0.5F;
    private static final float CHILD_BODY_TRANSLATE_Y = 24.0F * MODEL_RENDER_SCALE;
    private final GuidebookPreviewPlayerModel wideMainModel;
    private final GuidebookPreviewPlayerModel slimMainModel;
    private AbstractClientPlayer suppressedEtFuturumElytraPlayer;
    private ItemStack suppressedEtFuturumElytraChestItem;

    private GuidebookPreviewPlayerRenderer() {
        this.wideMainModel = new GuidebookPreviewPlayerModel(0.0F, true, false);
        this.slimMainModel = new GuidebookPreviewPlayerModel(0.0F, true, true);
        this.mainModel = this.wideMainModel;
        this.modelBipedMain = this.wideMainModel;
        this.modelArmorChestplate = new GuidebookPreviewPlayerModel(1.0F, false, false);
        this.modelArmor = new GuidebookPreviewPlayerModel(0.5F, false, false);
    }

    static void ensureRegistered() {
        RenderManager renderManager = RenderManager.instance;
        INSTANCE.setRenderManager(renderManager);
        if (renderManager.entityRenderMap.get(GuidebookScenePreviewPlayerEntity.class) != INSTANCE) {
            renderManager.entityRenderMap.put(GuidebookScenePreviewPlayerEntity.class, INSTANCE);
        }
    }

    @Override
    protected boolean func_110813_b(EntityLivingBase targetEntity) {
        if (targetEntity instanceof GuidebookNameplateControllable
            && !((GuidebookNameplateControllable) targetEntity).isGuidebookNameplateVisible()) {
            return false;
        }
        return super.func_110813_b(targetEntity);
    }

    @Override
    public void doRender(AbstractClientPlayer player, double x, double y, double z, float yaw, float partialTicks) {
        useResolvedMainModel(player);
        ItemStack chestItem = player.inventory.armorItemInSlot(CHEST_ARMOR_SLOT);
        if (!GuidebookPreviewPlayerCompat.isEtFuturumElytraStack(chestItem)) {
            try {
                super.doRender(player, x, y, z, yaw, partialTicks);
            } finally {
                setActiveMainModel(this.wideMainModel);
            }
            return;
        }

        AbstractClientPlayer previousSuppressedPlayer = this.suppressedEtFuturumElytraPlayer;
        ItemStack previousSuppressedChestItem = this.suppressedEtFuturumElytraChestItem;
        this.suppressedEtFuturumElytraPlayer = player;
        this.suppressedEtFuturumElytraChestItem = chestItem;
        player.inventory.armorInventory[CHEST_ARMOR_SLOT] = null;
        try {
            super.doRender(player, x, y, z, yaw, partialTicks);
        } finally {
            player.inventory.armorInventory[CHEST_ARMOR_SLOT] = chestItem;
            this.suppressedEtFuturumElytraPlayer = previousSuppressedPlayer;
            this.suppressedEtFuturumElytraChestItem = previousSuppressedChestItem;
            setActiveMainModel(this.wideMainModel);
        }
    }

    @Override
    protected void preRenderCallback(AbstractClientPlayer player, float partialTicks) {
        useResolvedMainModel(player);
        super.preRenderCallback(player, partialTicks);
    }

    @Override
    protected int shouldRenderPass(AbstractClientPlayer player, int pass, float partialTicks) {
        useResolvedMainModel(player);
        if (!shouldSuppressEtFuturumElytraAutoRender(player, pass)) {
            return super.shouldRenderPass(player, pass, partialTicks);
        }

        ItemStack chestItem = player.inventory.armorItemInSlot(CHEST_ARMOR_SLOT);
        player.inventory.armorInventory[CHEST_ARMOR_SLOT] = null;
        try {
            return super.shouldRenderPass(player, pass, partialTicks);
        } finally {
            player.inventory.armorInventory[CHEST_ARMOR_SLOT] = chestItem;
        }
    }

    @Override
    protected void renderEquippedItems(AbstractClientPlayer p_77029_1_, float p_77029_2_) {
        useResolvedMainModel(p_77029_1_);
        net.minecraftforge.client.event.RenderPlayerEvent.Specials.Pre event = new net.minecraftforge.client.event.RenderPlayerEvent.Specials.Pre(
            p_77029_1_,
            this,
            p_77029_2_);
        if (net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(event)) return;
        GL11.glColor3f(1.0F, 1.0F, 1.0F);
        super.renderArrowsStuckInEntity(p_77029_1_, p_77029_2_);
        ItemStack itemstack = p_77029_1_.inventory.armorItemInSlot(3);
        ItemStack chestItem = getRenderableChestItem(p_77029_1_);
        boolean hasEtFuturumElytra = GuidebookPreviewPlayerCompat.isEtFuturumElytraStack(chestItem);

        if (itemstack != null && event.renderHelmet) {
            GL11.glPushMatrix();
            this.modelBipedMain.bipedHead.postRender(0.0625F);
            float f1;

            if (itemstack.getItem() instanceof ItemBlock) {
                net.minecraftforge.client.IItemRenderer customRenderer = net.minecraftforge.client.MinecraftForgeClient
                    .getItemRenderer(itemstack, net.minecraftforge.client.IItemRenderer.ItemRenderType.EQUIPPED);
                boolean is3D = customRenderer != null && customRenderer.shouldUseRenderHelper(
                    net.minecraftforge.client.IItemRenderer.ItemRenderType.EQUIPPED,
                    itemstack,
                    net.minecraftforge.client.IItemRenderer.ItemRendererHelper.BLOCK_3D);

                if (is3D || RenderBlocks.renderItemIn3d(
                    Block.getBlockFromItem(itemstack.getItem())
                        .getRenderType())) {
                    f1 = 0.625F;
                    GL11.glTranslatef(0.0F, -0.25F, 0.0F);
                    GL11.glRotatef(90.0F, 0.0F, 1.0F, 0.0F);
                    GL11.glScalef(f1, -f1, -f1);
                }

                this.renderManager.itemRenderer.renderItem(p_77029_1_, itemstack, 0);
            } else if (itemstack.getItem() == Items.skull) {
                f1 = 1.0625F;
                GL11.glScalef(f1, -f1, -f1);
                GameProfile gameprofile = null;

                if (itemstack.hasTagCompound()) {
                    NBTTagCompound nbttagcompound = itemstack.getTagCompound();

                    if (nbttagcompound.hasKey("SkullOwner", 10)) {
                        gameprofile = NBTUtil.func_152459_a(nbttagcompound.getCompoundTag("SkullOwner"));
                    } else if (nbttagcompound.hasKey("SkullOwner", 8)
                        && !StringUtils.isNullOrEmpty(nbttagcompound.getString("SkullOwner"))) {
                            gameprofile = new GameProfile(null, nbttagcompound.getString("SkullOwner"));
                        }
                }

                TileEntitySkullRenderer.field_147536_b
                    .func_152674_a(-0.5F, 0.0F, -0.5F, 1, 180.0F, itemstack.getItemDamage(), gameprofile);
            }

            GL11.glPopMatrix();
        }

        float f2;

        if (p_77029_1_.getCommandSenderName()
            .equals("deadmau5") && p_77029_1_.func_152123_o()) {
            this.bindTexture(p_77029_1_.getLocationSkin());

            for (int j = 0; j < 2; ++j) {
                float f9 = p_77029_1_.prevRotationYaw
                    + (p_77029_1_.rotationYaw - p_77029_1_.prevRotationYaw) * p_77029_2_
                    - (p_77029_1_.prevRenderYawOffset
                        + (p_77029_1_.renderYawOffset - p_77029_1_.prevRenderYawOffset) * p_77029_2_);
                float f10 = p_77029_1_.prevRotationPitch
                    + (p_77029_1_.rotationPitch - p_77029_1_.prevRotationPitch) * p_77029_2_;
                GL11.glPushMatrix();
                GL11.glRotatef(f9, 0.0F, 1.0F, 0.0F);
                GL11.glRotatef(f10, 1.0F, 0.0F, 0.0F);
                GL11.glTranslatef(0.375F * (float) (j * 2 - 1), 0.0F, 0.0F);
                GL11.glTranslatef(0.0F, -0.375F, 0.0F);
                GL11.glRotatef(-f10, 1.0F, 0.0F, 0.0F);
                GL11.glRotatef(-f9, 0.0F, 1.0F, 0.0F);
                f2 = 1.3333334F;
                GL11.glScalef(f2, f2, f2);
                this.modelBipedMain.renderEars(0.0625F);
                GL11.glPopMatrix();
            }
        }

        boolean flag = p_77029_1_.func_152122_n();
        flag = event.renderCape && flag;

        if (flag && !p_77029_1_.isInvisible() && !p_77029_1_.getHideCape() && !hasEtFuturumElytra) {
            renderPreviewCape(p_77029_1_);
        }

        if (hasEtFuturumElytra) {
            renderPreviewEtFuturumElytra(p_77029_1_, chestItem, p_77029_2_);
        }

        ItemStack itemstack1 = p_77029_1_.inventory.getCurrentItem();

        if (itemstack1 != null && event.renderItem) {
            GL11.glPushMatrix();
            getActivePreviewMainModel().postRenderRightArm(0.0625F);
            GL11.glTranslatef(-0.0625F, 0.4375F, 0.0625F);

            if (p_77029_1_.fishEntity != null) {
                itemstack1 = new ItemStack(Items.stick);
            }

            EnumAction enumaction = null;

            if (p_77029_1_.getItemInUseCount() > 0) {
                enumaction = itemstack1.getItemUseAction();
            }

            net.minecraftforge.client.IItemRenderer customRenderer = net.minecraftforge.client.MinecraftForgeClient
                .getItemRenderer(itemstack1, net.minecraftforge.client.IItemRenderer.ItemRenderType.EQUIPPED);
            boolean is3D = customRenderer != null && customRenderer.shouldUseRenderHelper(
                net.minecraftforge.client.IItemRenderer.ItemRenderType.EQUIPPED,
                itemstack1,
                net.minecraftforge.client.IItemRenderer.ItemRendererHelper.BLOCK_3D);

            if (is3D || itemstack1.getItem() instanceof ItemBlock && RenderBlocks.renderItemIn3d(
                Block.getBlockFromItem(itemstack1.getItem())
                    .getRenderType())) {
                f2 = 0.5F;
                GL11.glTranslatef(0.0F, 0.1875F, -0.3125F);
                f2 *= 0.75F;
                GL11.glRotatef(20.0F, 1.0F, 0.0F, 0.0F);
                GL11.glRotatef(45.0F, 0.0F, 1.0F, 0.0F);
                GL11.glScalef(-f2, -f2, f2);
            } else if (itemstack1.getItem() == Items.bow) {
                f2 = 0.625F;
                GL11.glTranslatef(0.0F, 0.125F, 0.3125F);
                GL11.glRotatef(-20.0F, 0.0F, 1.0F, 0.0F);
                GL11.glScalef(f2, -f2, f2);
                GL11.glRotatef(-100.0F, 1.0F, 0.0F, 0.0F);
                GL11.glRotatef(45.0F, 0.0F, 1.0F, 0.0F);
            } else if (itemstack1.getItem()
                .isFull3D()) {
                    f2 = 0.625F;

                    if (itemstack1.getItem()
                        .shouldRotateAroundWhenRendering()) {
                        GL11.glRotatef(180.0F, 0.0F, 0.0F, 1.0F);
                        GL11.glTranslatef(0.0F, -0.125F, 0.0F);
                    }

                    if (p_77029_1_.getItemInUseCount() > 0 && enumaction == EnumAction.block) {
                        GL11.glTranslatef(0.05F, 0.0F, -0.1F);
                        GL11.glRotatef(-50.0F, 0.0F, 1.0F, 0.0F);
                        GL11.glRotatef(-10.0F, 1.0F, 0.0F, 0.0F);
                        GL11.glRotatef(-60.0F, 0.0F, 0.0F, 1.0F);
                    }

                    GL11.glTranslatef(0.0F, 0.1875F, 0.0F);
                    GL11.glScalef(f2, -f2, f2);
                    GL11.glRotatef(-100.0F, 1.0F, 0.0F, 0.0F);
                    GL11.glRotatef(45.0F, 0.0F, 1.0F, 0.0F);
                } else {
                    f2 = 0.375F;
                    GL11.glTranslatef(0.25F, 0.1875F, -0.1875F);
                    GL11.glScalef(f2, f2, f2);
                    GL11.glRotatef(60.0F, 0.0F, 0.0F, 1.0F);
                    GL11.glRotatef(-90.0F, 1.0F, 0.0F, 0.0F);
                    GL11.glRotatef(20.0F, 0.0F, 0.0F, 1.0F);
                }

            float f3;
            int k;
            float f12;
            float f4;

            if (itemstack1.getItem()
                .requiresMultipleRenderPasses()) {
                for (k = 0; k < itemstack1.getItem()
                    .getRenderPasses(itemstack1.getItemDamage()); ++k) {
                    int i = itemstack1.getItem()
                        .getColorFromItemStack(itemstack1, k);
                    f12 = (float) (i >> 16 & 255) / 255.0F;
                    f3 = (float) (i >> 8 & 255) / 255.0F;
                    f4 = (float) (i & 255) / 255.0F;
                    GL11.glColor4f(f12, f3, f4, 1.0F);
                    this.renderManager.itemRenderer.renderItem(p_77029_1_, itemstack1, k);
                }
            } else {
                k = itemstack1.getItem()
                    .getColorFromItemStack(itemstack1, 0);
                float f11 = (float) (k >> 16 & 255) / 255.0F;
                f12 = (float) (k >> 8 & 255) / 255.0F;
                f3 = (float) (k & 255) / 255.0F;
                GL11.glColor4f(f11, f12, f3, 1.0F);
                this.renderManager.itemRenderer.renderItem(p_77029_1_, itemstack1, 0);
            }

            GL11.glPopMatrix();
        }
        net.minecraftforge.common.MinecraftForge.EVENT_BUS
            .post(new net.minecraftforge.client.event.RenderPlayerEvent.Specials.Post(p_77029_1_, this, p_77029_2_));
    }

    private void renderPreviewCape(AbstractClientPlayer player) {
        this.bindTexture(player.getLocationCape());
        GL11.glPushMatrix();
        if (player.isChild()) {
            GL11.glScalef(CHILD_BODY_SCALE, CHILD_BODY_SCALE, CHILD_BODY_SCALE);
            GL11.glTranslatef(0.0F, CHILD_BODY_TRANSLATE_Y, 0.0F);
        }
        GL11.glTranslatef(0.0F, 0.0F, 0.125F);
        Vector3f capeRotation = resolvePose(player).resolveCapeRotationDegrees();
        GL11.glRotatef(capeRotation.x, 1.0F, 0.0F, 0.0F);
        GL11.glRotatef(capeRotation.y, 0.0F, 1.0F, 0.0F);
        GL11.glRotatef(capeRotation.z, 0.0F, 0.0F, 1.0F);
        GL11.glRotatef(180.0F, 0.0F, 1.0F, 0.0F);
        this.modelBipedMain.renderCloak(MODEL_RENDER_SCALE);
        GL11.glPopMatrix();
    }

    public static GuidebookPreviewPlayerPose resolvePose(AbstractClientPlayer player) {
        if (player instanceof GuidebookPlayerPoseControllable poseControllable) {
            GuidebookPreviewPlayerPose pose = poseControllable.getGuidebookPreviewPlayerPose();
            if (pose != null) {
                return pose;
            }
        }
        return GuidebookPreviewPlayerPose.DEFAULT;
    }

    private boolean shouldSuppressEtFuturumElytraAutoRender(AbstractClientPlayer player, int pass) {
        return pass == CHEST_RENDER_PASS
            && GuidebookPreviewPlayerCompat.isEtFuturumElytraStack(player.inventory.armorItemInSlot(CHEST_ARMOR_SLOT));
    }

    private ItemStack getRenderableChestItem(AbstractClientPlayer player) {
        if (player == this.suppressedEtFuturumElytraPlayer && this.suppressedEtFuturumElytraChestItem != null) {
            return this.suppressedEtFuturumElytraChestItem;
        }
        return player.inventory.armorItemInSlot(CHEST_ARMOR_SLOT);
    }

    private void renderPreviewEtFuturumElytra(AbstractClientPlayer player, ItemStack chestItem, float partialTicks) {
        GL11.glPushMatrix();
        if (player.isChild()) {
            GL11.glScalef(CHILD_BODY_SCALE, CHILD_BODY_SCALE, CHILD_BODY_SCALE);
            GL11.glTranslatef(0.0F, CHILD_BODY_TRANSLATE_Y, 0.0F);
        }
        ItemStack originalChestItem = player.inventory.armorItemInSlot(CHEST_ARMOR_SLOT);
        boolean restoreChestItem = chestItem != null && originalChestItem != chestItem;
        if (restoreChestItem) {
            player.inventory.armorInventory[CHEST_ARMOR_SLOT] = chestItem;
        }
        try {
            GuidebookPreviewPlayerCompat.tryRenderEtFuturumElytraLayer(
                player,
                player.limbSwing,
                player.limbSwingAmount,
                partialTicks,
                player.getAge(),
                MODEL_RENDER_SCALE);
        } finally {
            if (restoreChestItem) {
                player.inventory.armorInventory[CHEST_ARMOR_SLOT] = originalChestItem;
            }
        }
        GL11.glPopMatrix();
    }

    private void useResolvedMainModel(AbstractClientPlayer player) {
        GuidebookPreviewPlayerModel selectedModel = GuidebookPreviewPlayerCompat.shouldUseSlimArms(player)
            ? this.slimMainModel
            : this.wideMainModel;
        boolean child = player.isChild();
        selectedModel.isChild = child;
        this.modelArmorChestplate.isChild = child;
        this.modelArmor.isChild = child;
        setActiveMainModel(selectedModel);
    }

    private void setActiveMainModel(GuidebookPreviewPlayerModel model) {
        if (this.mainModel != model) {
            this.mainModel = model;
            this.modelBipedMain = model;
        }
    }

    private GuidebookPreviewPlayerModel getActivePreviewMainModel() {
        return (GuidebookPreviewPlayerModel) this.modelBipedMain;
    }
}
