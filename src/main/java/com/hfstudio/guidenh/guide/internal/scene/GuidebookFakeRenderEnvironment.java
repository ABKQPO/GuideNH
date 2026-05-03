package com.hfstudio.guidenh.guide.internal.scene;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityClientPlayerMP;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.world.World;

import com.hfstudio.guidenh.guide.scene.CameraSettings;
import com.hfstudio.guidenh.guide.scene.level.GuidebookLevel;

public class GuidebookFakeRenderEnvironment implements AutoCloseable {

    public static GuidebookPreviewPlayer cachedPreviewPlayer;
    public static NetHandlerPlayClient cachedNetHandler;

    private final Minecraft minecraft;
    private final EntityClientPlayerMP previousPlayer;
    private final EntityLivingBase previousRenderViewEntity;
    private final Entity previousPointedEntity;
    private final WorldClient previousWorld;
    private final RenderManagerState renderManagerState;
    private final TileEntityDispatcherState tileEntityDispatcherState;

    private GuidebookFakeRenderEnvironment(GuidebookLevel level, CameraSettings camera, float partialTicks) {
        this.minecraft = Minecraft.getMinecraft();
        this.previousWorld = minecraft.theWorld;
        this.previousPlayer = minecraft.thePlayer;
        this.previousRenderViewEntity = minecraft.renderViewEntity;
        this.previousPointedEntity = minecraft.pointedEntity;
        this.renderManagerState = new RenderManagerState(RenderManager.instance);
        this.tileEntityDispatcherState = new TileEntityDispatcherState(TileEntityRendererDispatcher.instance);

        WorldClient fakeWorld = level.getOrCreateFakeWorld();
        GuidebookPreviewPlayer previewPlayer = getOrCreatePreviewPlayer(minecraft, fakeWorld);
        previewPlayer.syncToPreviewWorld(fakeWorld, level, camera);

        minecraft.theWorld = fakeWorld;
        minecraft.thePlayer = previewPlayer;
        minecraft.renderViewEntity = previewPlayer;
        minecraft.pointedEntity = null;

        TileEntityRendererDispatcher dispatcher = TileEntityRendererDispatcher.instance;
        if (!level.getTileEntities()
            .isEmpty()) {
            dispatcher.cacheActiveRenderInfo(
                fakeWorld,
                minecraft.getTextureManager(),
                minecraft.fontRenderer,
                previewPlayer,
                partialTicks);
        }

        RenderManager renderManager = RenderManager.instance;
        renderManager.cacheActiveRenderInfo(
            fakeWorld,
            minecraft.getTextureManager(),
            minecraft.fontRenderer,
            previewPlayer,
            null,
            minecraft.gameSettings,
            partialTicks);

        double viewerX = previewPlayer.lastTickPosX + (previewPlayer.posX - previewPlayer.lastTickPosX) * partialTicks;
        double viewerY = previewPlayer.lastTickPosY + (previewPlayer.posY - previewPlayer.lastTickPosY) * partialTicks;
        double viewerZ = previewPlayer.lastTickPosZ + (previewPlayer.posZ - previewPlayer.lastTickPosZ) * partialTicks;
        TileEntityRendererDispatcher.staticPlayerX = viewerX;
        TileEntityRendererDispatcher.staticPlayerY = viewerY;
        TileEntityRendererDispatcher.staticPlayerZ = viewerZ;
        RenderManager.renderPosX = viewerX;
        RenderManager.renderPosY = viewerY;
        RenderManager.renderPosZ = viewerZ;
    }

    public static GuidebookFakeRenderEnvironment enter(GuidebookLevel level, CameraSettings camera,
        float partialTicks) {
        GuidebookPreviewPlayerRenderer.ensureRegistered();
        return new GuidebookFakeRenderEnvironment(level, camera, partialTicks);
    }

    @Override
    public void close() {
        minecraft.theWorld = previousWorld;
        minecraft.thePlayer = previousPlayer;
        minecraft.renderViewEntity = previousRenderViewEntity;
        minecraft.pointedEntity = previousPointedEntity;

        renderManagerState.restore(RenderManager.instance);
        tileEntityDispatcherState.restore(TileEntityRendererDispatcher.instance);
    }

    public static GuidebookPreviewPlayer getOrCreatePreviewPlayer(Minecraft minecraft, WorldClient world) {
        NetHandlerPlayClient netHandler = minecraft.getNetHandler();
        if (netHandler == null) {
            throw new IllegalStateException("Guidebook preview requires an active client world");
        }
        if (cachedPreviewPlayer == null || cachedNetHandler != netHandler) {
            cachedPreviewPlayer = new GuidebookPreviewPlayer(minecraft, world, netHandler);
            cachedNetHandler = netHandler;
        }
        return cachedPreviewPlayer;
    }

    public static class RenderManagerState {

        private final World world;
        private final EntityLivingBase livingPlayer;
        private final Entity field147941I;
        private final float playerViewY;
        private final float playerViewX;
        private final double viewerPosX;
        private final double viewerPosY;
        private final double viewerPosZ;
        private final double renderPosX;
        private final double renderPosY;
        private final double renderPosZ;

        private RenderManagerState(RenderManager renderManager) {
            this.world = renderManager.worldObj;
            this.livingPlayer = renderManager.livingPlayer;
            this.field147941I = renderManager.field_147941_i;
            this.playerViewY = renderManager.playerViewY;
            this.playerViewX = renderManager.playerViewX;
            this.viewerPosX = renderManager.viewerPosX;
            this.viewerPosY = renderManager.viewerPosY;
            this.viewerPosZ = renderManager.viewerPosZ;
            this.renderPosX = RenderManager.renderPosX;
            this.renderPosY = RenderManager.renderPosY;
            this.renderPosZ = RenderManager.renderPosZ;
        }

        private void restore(RenderManager renderManager) {
            renderManager.set(world);
            renderManager.livingPlayer = livingPlayer;
            renderManager.field_147941_i = field147941I;
            renderManager.playerViewY = playerViewY;
            renderManager.playerViewX = playerViewX;
            renderManager.viewerPosX = viewerPosX;
            renderManager.viewerPosY = viewerPosY;
            renderManager.viewerPosZ = viewerPosZ;
            RenderManager.renderPosX = renderPosX;
            RenderManager.renderPosY = renderPosY;
            RenderManager.renderPosZ = renderPosZ;
        }
    }

    public static class TileEntityDispatcherState {

        private final World world;
        private final EntityLivingBase livingPlayer;
        private final float rotationYaw;
        private final float rotationPitch;
        private final double viewerPosX;
        private final double viewerPosY;
        private final double viewerPosZ;
        private final double staticPlayerX;
        private final double staticPlayerY;
        private final double staticPlayerZ;

        private TileEntityDispatcherState(TileEntityRendererDispatcher dispatcher) {
            this.world = dispatcher.field_147550_f;
            this.livingPlayer = dispatcher.field_147551_g;
            this.rotationYaw = dispatcher.field_147562_h;
            this.rotationPitch = dispatcher.field_147563_i;
            this.viewerPosX = dispatcher.field_147560_j;
            this.viewerPosY = dispatcher.field_147561_k;
            this.viewerPosZ = dispatcher.field_147558_l;
            this.staticPlayerX = TileEntityRendererDispatcher.staticPlayerX;
            this.staticPlayerY = TileEntityRendererDispatcher.staticPlayerY;
            this.staticPlayerZ = TileEntityRendererDispatcher.staticPlayerZ;
        }

        private void restore(TileEntityRendererDispatcher dispatcher) {
            if (dispatcher.field_147550_f != world) {
                dispatcher.func_147543_a(world);
            }
            dispatcher.field_147551_g = livingPlayer;
            dispatcher.field_147562_h = rotationYaw;
            dispatcher.field_147563_i = rotationPitch;
            dispatcher.field_147560_j = viewerPosX;
            dispatcher.field_147561_k = viewerPosY;
            dispatcher.field_147558_l = viewerPosZ;
            TileEntityRendererDispatcher.staticPlayerX = staticPlayerX;
            TileEntityRendererDispatcher.staticPlayerY = staticPlayerY;
            TileEntityRendererDispatcher.staticPlayerZ = staticPlayerZ;
        }
    }
}
