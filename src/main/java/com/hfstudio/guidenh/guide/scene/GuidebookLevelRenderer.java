package com.hfstudio.guidenh.guide.scene;

import static org.lwjgl.opengl.GL11.GL_ALPHA_TEST;
import static org.lwjgl.opengl.GL11.GL_BLEND;
import static org.lwjgl.opengl.GL11.GL_CULL_FACE;
import static org.lwjgl.opengl.GL11.GL_DEPTH_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.GL_DEPTH_TEST;
import static org.lwjgl.opengl.GL11.GL_LIGHTING;
import static org.lwjgl.opengl.GL11.GL_MODELVIEW;
import static org.lwjgl.opengl.GL11.GL_PROJECTION;
import static org.lwjgl.opengl.GL11.GL_SCISSOR_TEST;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_2D;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.entity.Entity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.IBlockAccess;
import net.minecraftforge.client.ForgeHooksClient;

import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Vector3fc;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

import com.hfstudio.guidenh.guide.color.LightDarkMode;
import com.hfstudio.guidenh.guide.internal.scene.GuidebookFakeRenderEnvironment;
import com.hfstudio.guidenh.guide.internal.util.DisplayScale;
import com.hfstudio.guidenh.guide.scene.annotation.InWorldAnnotation;
import com.hfstudio.guidenh.guide.scene.annotation.InWorldAnnotationRenderer;
import com.hfstudio.guidenh.guide.scene.level.GuidebookLevel;
import com.hfstudio.guidenh.guide.scene.support.GuideDebugLog;
import com.hfstudio.guidenh.guide.scene.support.GuideGregTechTileSupport;
import com.hfstudio.guidenh.guide.siteexport.site.GuideSiteSceneTessellatorCapture;
import com.hfstudio.guidenh.integration.api.client.GuideNhClientIntegrationRegistry;
import com.hfstudio.guidenh.mixins.early.forge.AccessorForgeHooksClient;

public class GuidebookLevelRenderer {

    public static final GuidebookLevelRenderer INSTANCE = new GuidebookLevelRenderer();
    public static final int FULL_BRIGHTNESS = 15728880;
    public static final ResourceLocation RAIN_TEXTURE = new ResourceLocation("textures/environment/rain.png");
    public static final ResourceLocation SNOW_TEXTURE = new ResourceLocation("textures/environment/snow.png");
    public static final int WEATHER_RENDER_RADIUS = 10;
    public static final int WEATHER_COVERAGE_RESOLUTION = 1024;

    private final FloatBuffer matrixBuffer = BufferUtils.createFloatBuffer(16);
    private final GuideEntityRenderStateResolver.ResolvedEntityRenderState entityRenderState = new GuideEntityRenderStateResolver.ResolvedEntityRenderState();
    private final ArrayList<GuidebookSceneWeatherRenderColumn> weatherColumnsScratch = new ArrayList<>();
    private final ArrayList<GuidebookSceneWeatherRenderColumn> weatherColumnPool = new ArrayList<>();

    // Rebind RenderBlocks only when the level instance changes.
    private RenderBlocks cachedRenderBlocks;
    private GuidebookLevel cachedRenderBlocksLevel;

    public static GuidebookLevelRenderer getInstance() {
        return INSTANCE;
    }

    protected GuidebookLevelRenderer() {}

    public void render(GuidebookLevel level, CameraSettings camera, int panelX, int panelY, int panelWidth,
        int panelHeight, float partialTicks) {
        render(
            level,
            camera,
            panelX,
            panelY,
            panelWidth,
            panelHeight,
            panelX,
            panelY,
            panelWidth,
            panelHeight,
            partialTicks,
            List.of(),
            LightDarkMode.LIGHT_MODE,
            null);
    }

    public void render(GuidebookLevel level, CameraSettings camera, int panelX, int panelY, int panelWidth,
        int panelHeight, float partialTicks, List<InWorldAnnotation> annotations, LightDarkMode lightDarkMode) {
        render(
            level,
            camera,
            panelX,
            panelY,
            panelWidth,
            panelHeight,
            panelX,
            panelY,
            panelWidth,
            panelHeight,
            partialTicks,
            annotations,
            lightDarkMode,
            null);
    }

    public void render(GuidebookLevel level, CameraSettings camera, int panelX, int panelY, int panelWidth,
        int panelHeight, int scissorX, int scissorY, int scissorW, int scissorH, float partialTicks,
        List<InWorldAnnotation> annotations, LightDarkMode lightDarkMode) {
        render(
            level,
            camera,
            panelX,
            panelY,
            panelWidth,
            panelHeight,
            scissorX,
            scissorY,
            scissorW,
            scissorH,
            partialTicks,
            annotations,
            lightDarkMode,
            null);
    }

    public void render(GuidebookLevel level, CameraSettings camera, int panelX, int panelY, int panelWidth,
        int panelHeight, int scissorX, int scissorY, int scissorW, int scissorH, float partialTicks,
        List<InWorldAnnotation> annotations, LightDarkMode lightDarkMode, @Nullable Integer visibleLayerY) {
        render(
            level,
            camera,
            panelX,
            panelY,
            panelWidth,
            panelHeight,
            scissorX,
            scissorY,
            scissorW,
            scissorH,
            partialTicks,
            annotations,
            lightDarkMode,
            visibleLayerY,
            List.of());
    }

    public void render(GuidebookLevel level, CameraSettings camera, int panelX, int panelY, int panelWidth,
        int panelHeight, int scissorX, int scissorY, int scissorW, int scissorH, float partialTicks,
        List<InWorldAnnotation> annotations, LightDarkMode lightDarkMode, @Nullable Integer visibleLayerY,
        List<GuidebookSceneParticle> particles) {
        render(
            level,
            camera,
            panelX,
            panelY,
            panelWidth,
            panelHeight,
            scissorX,
            scissorY,
            scissorW,
            scissorH,
            partialTicks,
            annotations,
            lightDarkMode,
            GuidebookSceneLayerSelection.fromVisibleLayer(visibleLayerY),
            particles,
            List.of(),
            0.0f);
    }

    public void render(GuidebookLevel level, CameraSettings camera, int panelX, int panelY, int panelWidth,
        int panelHeight, int scissorX, int scissorY, int scissorW, int scissorH, float partialTicks,
        List<InWorldAnnotation> annotations, LightDarkMode lightDarkMode, GuidebookSceneLayerSelection layerSelection,
        List<GuidebookSceneParticle> particles) {
        render(
            level,
            camera,
            panelX,
            panelY,
            panelWidth,
            panelHeight,
            scissorX,
            scissorY,
            scissorW,
            scissorH,
            partialTicks,
            annotations,
            lightDarkMode,
            layerSelection,
            particles,
            List.of(),
            0.0f);
    }

    public void render(GuidebookLevel level, CameraSettings camera, int panelX, int panelY, int panelWidth,
        int panelHeight, int scissorX, int scissorY, int scissorW, int scissorH, float partialTicks,
        List<InWorldAnnotation> annotations, LightDarkMode lightDarkMode, GuidebookSceneLayerSelection layerSelection,
        List<GuidebookSceneParticle> particles, List<GuidebookSceneWeatherEffect> weatherEffects,
        float weatherAnimationTick) {

        int cx0 = Math.max(panelX, scissorX);
        int cy0 = Math.max(panelY, scissorY);
        int cx1 = Math.min(panelX + panelWidth, scissorX + scissorW);
        int cy1 = Math.min(panelY + panelHeight, scissorY + scissorH);
        if (cx1 <= cx0 || cy1 <= cy0) return;

        var mc = Minecraft.getMinecraft();
        int displayHeight = mc.displayHeight;
        int sf = DisplayScale.scaleFactor();
        int glScissorX = cx0 * sf;
        int glScissorY = displayHeight - cy1 * sf;
        int glScissorW = (cx1 - cx0) * sf;
        int glScissorH = (cy1 - cy0) * sf;

        try (var env = GuidebookFakeRenderEnvironment.enter(level, camera, partialTicks)) {
            level.prepareForPreview();

            GL11.glPushAttrib(
                GL11.GL_ENABLE_BIT | GL11.GL_COLOR_BUFFER_BIT
                    | GL11.GL_DEPTH_BUFFER_BIT
                    | GL11.GL_SCISSOR_BIT
                    | GL11.GL_LIGHTING_BIT
                    | GL11.GL_TEXTURE_BIT
                    | GL11.GL_CURRENT_BIT
                    | GL11.GL_VIEWPORT_BIT
                    | GL11.GL_TRANSFORM_BIT
                    | GL11.GL_POLYGON_BIT);
            try {
                GL11.glEnable(GL_SCISSOR_TEST);
                GL11.glScissor(glScissorX, glScissorY, glScissorW, glScissorH);
                GL11.glClear(GL_DEPTH_BUFFER_BIT);

                int glVpX = panelX * sf;
                int glVpY = displayHeight - (panelY + panelHeight) * sf;
                int glVpW = panelWidth * sf;
                int glVpH = panelHeight * sf;
                GL11.glViewport(glVpX, glVpY, glVpW, glVpH);

                GL11.glEnable(GL_DEPTH_TEST);
                GL11.glDepthFunc(GL11.GL_LEQUAL);
                GL11.glDisable(GL_LIGHTING);
                GL11.glDisable(GL_BLEND);
                GL11.glEnable(GL_CULL_FACE);
                GL11.glEnable(GL_ALPHA_TEST);
                GL11.glAlphaFunc(GL11.GL_GREATER, 0.1f);
                GL11.glEnable(GL_TEXTURE_2D);
                GL11.glDisable(GL12.GL_RESCALE_NORMAL);
                GL11.glEnable(GL11.GL_NORMALIZE);
                GL11.glColor4f(1f, 1f, 1f, 1f);
                GL11.glNormal3f(0f, 1f, 0f);

                OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240f, 240f);

                GL11.glMatrixMode(GL_PROJECTION);
                GL11.glPushMatrix();
                loadMatrix(camera.getProjectionMatrix());

                GL11.glMatrixMode(GL_MODELVIEW);
                GL11.glPushMatrix();
                loadMatrix(camera.getViewMatrix());

                try {
                    mc.getTextureManager()
                        .bindTexture(TextureMap.locationBlocksTexture);
                    var filledBlocks = level.getFilledBlocks();
                    var tileEntities = level.getTileEntities();

                    mc.entityRenderer.enableLightmap(partialTicks);
                    try {
                        setRenderPass(0);
                        GL11.glDisable(GL_BLEND);
                        renderBlocksPass(level, filledBlocks, 0, layerSelection);

                        setRenderPass(1);
                        GL11.glEnable(GL_BLEND);
                        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
                        GL11.glDepthMask(false);
                        renderBlocksPass(level, filledBlocks, 1, layerSelection);
                        GL11.glDepthMask(true);
                        GL11.glDisable(GL_BLEND);

                        setRenderPass(-1);

                        renderBlockEntities(tileEntities, partialTicks, layerSelection);
                        renderEntities(level.getEntities(), partialTicks, layerSelection);

                        if (!annotations.isEmpty()) {
                            InWorldAnnotationRenderer.render(annotations, lightDarkMode);
                        }
                        if (weatherEffects != null && !weatherEffects.isEmpty()) {
                            renderWeatherInContext(level, camera, layerSelection, weatherEffects, weatherAnimationTick);
                        }
                        if (!particles.isEmpty()) {
                            renderParticlesInContext(particles, partialTicks);
                        }
                    } finally {
                        mc.entityRenderer.disableLightmap(partialTicks);
                    }
                } catch (Throwable t) {
                    log(t);
                } finally {
                    GL11.glMatrixMode(GL_PROJECTION);
                    GL11.glPopMatrix();
                    GL11.glMatrixMode(GL_MODELVIEW);
                    GL11.glPopMatrix();
                }

                // Wipe depth values within the scene's scissor so subsequent GUI rendering (e.g.
                // ItemStack icons drawn on top of a tooltip that contains this scene) is not occluded
                // by the 3D blocks we just drew. glPopAttrib restores GL state but not pixel data.
                GL11.glClear(GL_DEPTH_BUFFER_BIT);
            } finally {
                GL11.glPopAttrib();
            }
            OpenGlHelper.setActiveTexture(OpenGlHelper.defaultTexUnit);
            GL11.glEnable(GL_TEXTURE_2D);
            GL11.glDisable(GL_LIGHTING);
            GL11.glDisable(GL_DEPTH_TEST);
            GL11.glDepthMask(true);
            GL11.glEnable(GL_BLEND);
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            GL11.glEnable(GL_ALPHA_TEST);
            GL11.glAlphaFunc(GL11.GL_GREATER, 0.1f);
            GL11.glColor4f(1f, 1f, 1f, 1f);
            OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240f, 240f);
            RenderHelper.disableStandardItemLighting();
        }
    }

    private void renderBlocksPass(GuidebookLevel level, Iterable<int[]> filledBlocks, int pass,
        GuidebookSceneLayerSelection layerSelection) {
        RenderBlocks rb = cachedRenderBlocks;
        if (rb == null || cachedRenderBlocksLevel != level) {
            rb = new RenderBlocks(level.getOrCreateFakeWorld());
            cachedRenderBlocks = rb;
            cachedRenderBlocksLevel = level;
        }
        Minecraft mc = Minecraft.getMinecraft();
        int savedAmbientOcclusion = mc.gameSettings.ambientOcclusion;
        mc.gameSettings.ambientOcclusion = 0;
        IBlockAccess fakeWorld = level.getOrCreateFakeWorld();
        var tes = Tessellator.instance;
        tes.startDrawingQuads();
        try {
            tes.setBrightness((15 << 20) | (15 << 4));
            GuidebookSceneLayerSelection effectiveSelection = layerSelection != null ? layerSelection
                : GuidebookSceneLayerSelection.all();
            boolean filteredLayerMode = effectiveSelection.shouldRenderAllFaces();
            for (int[] p : filledBlocks) {
                if (!effectiveSelection.isLayerVisible(p[1])) {
                    continue;
                }
                Block block = level.getBlock(p[0], p[1], p[2]);
                if (block == null) continue;
                if (!block.canRenderInPass(pass)) continue;
                try {
                    TileEntity tileEntity = level.getTileEntity(p[0], p[1], p[2]);
                    if (GuideGregTechTileSupport.isGregTechTileEntity(tileEntity)
                        && !GuideGregTechTileSupport.hasValidMetaTileBinding(tileEntity)) {
                        GuideGregTechTileSupport.logInfoOnce(
                            "render-invalid-block-pass:" + pass
                                + ":"
                                + GuideGregTechTileSupport.describeTile(tileEntity),
                            "Render pass {} found invalid GregTech block tile before block render: {}",
                            pass,
                            GuideGregTechTileSupport.describeTile(tileEntity));
                        GuideGregTechTileSupport.repairMetaTileBinding(tileEntity);
                    }
                    TileEntity promoted = GuideNhClientIntegrationRegistry.global()
                        .promotePreviewBlockTileEntity(block, tileEntity);
                    if (promoted != null && promoted != tileEntity) {
                        level.setTileEntity(p[0], p[1], p[2], promoted);
                        tileEntity = promoted;
                    }
                    resetRenderBlocksState(rb, fakeWorld, filteredLayerMode);
                    boolean rendered = rb.renderBlockByRenderType(block, p[0], p[1], p[2]);
                    if (!rendered) {
                        GuideNhClientIntegrationRegistry.global()
                            .tryRenderPreviewWorldBlock(rb, fakeWorld, block, p[0], p[1], p[2]);
                    }
                } catch (Throwable t) {
                    log(t);
                }
            }
        } finally {
            tes.draw();
            tes.setTranslation(0.0D, 0.0D, 0.0D);
            mc.gameSettings.ambientOcclusion = savedAmbientOcclusion;
        }
    }

    public static void resetRenderBlocksState(RenderBlocks renderBlocks, IBlockAccess blockAccess,
        boolean renderAllFaces) {
        renderBlocks.blockAccess = blockAccess;
        renderBlocks.clearOverrideBlockTexture();
        renderBlocks.renderAllFaces = renderAllFaces;
        renderBlocks.useInventoryTint = false;
        renderBlocks.enableAO = false;
        renderBlocks.partialRenderBounds = false;
        renderBlocks.renderFromInside = false;
        renderBlocks.flipTexture = false;
        renderBlocks.lockBlockBounds = false;
        renderBlocks.uvRotateEast = 0;
        renderBlocks.uvRotateWest = 0;
        renderBlocks.uvRotateSouth = 0;
        renderBlocks.uvRotateNorth = 0;
        renderBlocks.uvRotateTop = 0;
        renderBlocks.uvRotateBottom = 0;
        renderBlocks.setRenderBounds(0.0D, 0.0D, 0.0D, 1.0D, 1.0D, 1.0D);
    }

    private void renderBlockEntities(Iterable<TileEntity> tileEntities, float partialTicks,
        GuidebookSceneLayerSelection layerSelection) {
        GuidebookSceneLayerSelection effectiveSelection = layerSelection != null ? layerSelection
            : GuidebookSceneLayerSelection.all();
        TileEntityRendererDispatcher dispatcher = TileEntityRendererDispatcher.instance;
        for (int pass = 0; pass < 2; pass++) {
            setRenderPass(pass);
            setTileEntityRenderPassState(pass);
            preparePreviewModelLighting();
            for (TileEntity te : tileEntities) {
                if (te == null) {
                    continue;
                }
                if (GuideGregTechTileSupport.isGregTechTileEntity(te)
                    && !GuideGregTechTileSupport.hasValidMetaTileBinding(te)) {
                    GuideGregTechTileSupport.logInfoOnce(
                        "render-invalid-tesr-pass:" + pass + ":" + GuideGregTechTileSupport.describeTile(te),
                        "Render pass {} found invalid GregTech tile before TESR render: {}",
                        pass,
                        GuideGregTechTileSupport.describeTile(te));
                    GuideGregTechTileSupport.repairMetaTileBinding(te);
                }
                if (!effectiveSelection.isLayerVisible(te.yCoord)) {
                    continue;
                }
                if (!dispatcher.hasSpecialRenderer(te) || !te.shouldRenderInPass(pass)) {
                    continue;
                }
                try {
                    preparePreviewModelLighting();
                    dispatcher.renderTileEntityAt(te, te.xCoord, te.yCoord, te.zCoord, partialTicks);
                } catch (Throwable t) {
                    log(t);
                }
            }
        }
        setRenderPass(-1);
        RenderHelper.disableStandardItemLighting();
        GL11.glDisable(GL_LIGHTING);
        GL11.glEnable(GL_DEPTH_TEST);
        GL11.glDisable(GL_BLEND);
        GL11.glDepthMask(true);
    }

    private void renderEntities(Iterable<Entity> entities, float partialTicks,
        GuidebookSceneLayerSelection layerSelection) {
        GuidebookSceneLayerSelection effectiveSelection = layerSelection != null ? layerSelection
            : GuidebookSceneLayerSelection.all();
        RenderManager renderManager = RenderManager.instance;
        preparePreviewModelLighting();
        for (Entity entity : entities) {
            if (entity == null || entity.isDead) {
                continue;
            }
            if (entity.boundingBox != null && !intersectsVisibleLayerSelection(entity, effectiveSelection)) {
                continue;
            }
            try {
                preparePreviewModelLighting();
                GuideEntityRenderStateResolver.ResolvedEntityRenderState renderState = GuideEntityRenderStateResolver
                    .resolve(entity, partialTicks, entityRenderState);
                int brightness = resolveEntityBrightnessForPreview(entity, partialTicks);
                int lowerBits = brightness % 65536;
                int upperBits = brightness / 65536;
                OpenGlHelper
                    .setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, (float) lowerBits, (float) upperBits);
                GL11.glColor4f(1f, 1f, 1f, 1f);
                // Our guidebook camera already supplies world-space transforms, so pass raw
                // interpolated coordinates here instead of letting RenderManager subtract the
                // preview player position a second time via renderEntityStatic().
                renderManager.renderEntityWithPosYaw(
                    entity,
                    renderState.x(),
                    renderState.y(),
                    renderState.z(),
                    renderState.yaw(),
                    partialTicks);
            } catch (Throwable t) {
                log(t);
            }
        }
        RenderHelper.disableStandardItemLighting();
        GL11.glDisable(GL_LIGHTING);
    }

    private boolean intersectsVisibleLayerSelection(Entity entity, GuidebookSceneLayerSelection layerSelection) {
        if (layerSelection == null || layerSelection.getMode() == GuidebookSceneLayerSelection.Mode.ALL
            || entity.boundingBox == null) {
            return true;
        }
        int minLayer = (int) Math.floor(entity.boundingBox.minY);
        int maxLayer = Math.max(minLayer, (int) Math.ceil(entity.boundingBox.maxY) - 1);
        for (int layer = minLayer; layer <= maxLayer; layer++) {
            if (layerSelection.isLayerVisible(layer)) {
                return true;
            }
        }
        return false;
    }

    public static int resolveEntityBrightnessForPreview(Entity entity, float partialTicks) {
        return FULL_BRIGHTNESS;
    }

    public static void preparePreviewModelLighting() {
        RenderHelper.enableStandardItemLighting();
        GL11.glEnable(GL_LIGHTING);
        GL11.glDisable(GL12.GL_RESCALE_NORMAL);
        GL11.glEnable(GL11.GL_NORMALIZE);
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240f, 240f);
        GL11.glColor4f(1f, 1f, 1f, 1f);
    }

    private void loadMatrix(Matrix4f m) {
        matrixBuffer.clear();
        m.get(matrixBuffer);
        matrixBuffer.rewind();
        GL11.glLoadMatrix(matrixBuffer);
    }

    public static void setRenderPass(int pass) {
        try {
            ForgeHooksClient.setRenderPass(pass);
        } catch (Throwable ignore) {}
        try {
            AccessorForgeHooksClient.setWorldRenderPass(pass);
        } catch (Throwable ignore) {}
    }

    public static void setTileEntityRenderPassState(int pass) {
        GL11.glColor4f(1f, 1f, 1f, 1f);
        if (pass == 0) {
            GL11.glEnable(GL_DEPTH_TEST);
            GL11.glDisable(GL_BLEND);
            GL11.glDepthMask(true);
            return;
        }
        GL11.glEnable(GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glDepthMask(false);
    }

    private void renderParticlesInContext(List<GuidebookSceneParticle> particles, float partialTicks) {
        if (particles == null || particles.isEmpty()) {
            return;
        }
        BillboardAxes billboardAxes = resolveBillboardAxes();
        float rx = billboardAxes.rightX();
        float ry = billboardAxes.rightY();
        float rz = billboardAxes.rightZ();
        float ux = billboardAxes.upX();
        float uy = billboardAxes.upY();
        float uz = billboardAxes.upZ();

        GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
        try {
            GL11.glEnable(GL_BLEND);
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            GL11.glEnable(GL_TEXTURE_2D);
            GL11.glDepthMask(false);
            GL11.glDisable(GL_CULL_FACE);
            GL11.glDisable(GL_LIGHTING);
            GL11.glDisable(GL_ALPHA_TEST);

            OpenGlHelper.setActiveTexture(OpenGlHelper.defaultTexUnit);
            var tess = Tessellator.instance;
            ResourceLocation activeTexture = null;
            boolean drawing = false;
            for (GuidebookSceneParticle p : particles) {
                if (p.isDead() || !p.isReadyToRender()) continue;
                ResourceLocation nextTexture = p.texture != null ? p.texture : TextureMap.locationBlocksTexture;
                if (!drawing || !nextTexture.equals(activeTexture)) {
                    if (drawing) {
                        tess.draw();
                    }
                    Minecraft.getMinecraft()
                        .getTextureManager()
                        .bindTexture(nextTexture);
                    tess.startDrawingQuads();
                    activeTexture = nextTexture;
                    drawing = true;
                }
                float alpha = p.getAlpha(partialTicks);
                int brightness = p.getBrightness(partialTicks);
                tess.setBrightness(
                    brightness != GuidebookSceneParticle.NO_BRIGHTNESS_OVERRIDE ? brightness : FULL_BRIGHTNESS);
                tess.setColorRGBA_F(p.red, p.green, p.blue, alpha);
                float s = p.getSize(partialTicks);
                float cx = p.getRenderX(partialTicks), cy = p.getRenderY(partialTicks), cz = p.getRenderZ(partialTicks);
                tess.addVertexWithUV(cx - rx * s - ux * s, cy - ry * s - uy * s, cz - rz * s - uz * s, p.u0, p.v1);
                tess.addVertexWithUV(cx + rx * s - ux * s, cy + ry * s - uy * s, cz + rz * s - uz * s, p.u1, p.v1);
                tess.addVertexWithUV(cx + rx * s + ux * s, cy + ry * s + uy * s, cz + rz * s + uz * s, p.u1, p.v0);
                tess.addVertexWithUV(cx - rx * s + ux * s, cy - ry * s + uy * s, cz - rz * s + uz * s, p.u0, p.v0);
            }
            if (drawing) {
                tess.draw();
            }
        } finally {
            GL11.glPopAttrib();
        }
    }

    private void renderWeatherInContext(GuidebookLevel level, CameraSettings camera,
        GuidebookSceneLayerSelection layerSelection, List<GuidebookSceneWeatherEffect> weatherEffects,
        float animationTick) {
        if (level == null || weatherEffects == null || weatherEffects.isEmpty()) {
            return;
        }

        int[] bounds = level.getBounds();
        if (bounds == null || bounds.length < 6) {
            return;
        }
        Vector3fc rotationCenter = camera != null ? camera.getRotationCenter() : null;
        float[] levelCenter = level.getCenter();
        float centerX = rotationCenter != null ? rotationCenter.x() : levelCenter[0];
        float centerY = rotationCenter != null ? rotationCenter.y() : levelCenter[1];
        float centerZ = rotationCenter != null ? rotationCenter.z() : levelCenter[2];
        BillboardAxes billboardAxes = resolveBillboardAxes();

        GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
        try {
            GL11.glEnable(GL_BLEND);
            OpenGlHelper.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, 1, 0);
            GL11.glEnable(GL_TEXTURE_2D);
            GL11.glDisable(GL_LIGHTING);
            GL11.glDisable(GL_CULL_FACE);
            GL11.glDepthMask(false);
            GL11.glEnable(GL_ALPHA_TEST);
            GL11.glAlphaFunc(GL11.GL_GREATER, 0.1f);
            GL11.glNormal3f(0.0f, 1.0f, 0.0f);

            boolean drewRain = appendWeatherEffectQuads(
                level,
                layerSelection,
                weatherEffects,
                bounds,
                animationTick,
                centerX,
                centerY,
                centerZ,
                billboardAxes,
                GuidebookSceneWeatherType.RAIN);
            boolean drewSnow = appendWeatherEffectQuads(
                level,
                layerSelection,
                weatherEffects,
                bounds,
                animationTick,
                centerX,
                centerY,
                centerZ,
                billboardAxes,
                GuidebookSceneWeatherType.SNOW);
            if (!drewRain && !drewSnow) {
                return;
            }
        } finally {
            GL11.glPopAttrib();
        }
    }

    private boolean appendWeatherEffectQuads(GuidebookLevel level, GuidebookSceneLayerSelection layerSelection,
        List<GuidebookSceneWeatherEffect> weatherEffects, int[] bounds, float animationTick, float centerX,
        float centerY, float centerZ, BillboardAxes billboardAxes, GuidebookSceneWeatherType weatherType) {
        weatherColumnsScratch.clear();
        collectWeatherColumns(
            weatherColumnsScratch,
            level,
            layerSelection,
            weatherEffects,
            bounds,
            animationTick,
            centerX,
            centerY,
            centerZ,
            weatherType);
        if (weatherColumnsScratch.isEmpty()) {
            return false;
        }

        ResourceLocation weatherTexture = weatherType == GuidebookSceneWeatherType.SNOW ? SNOW_TEXTURE : RAIN_TEXTURE;
        Minecraft.getMinecraft()
            .getTextureManager()
            .bindTexture(weatherTexture);
        GuideSiteSceneTessellatorCapture activeCapture = GuideSiteSceneTessellatorCapture.getActive();
        if (activeCapture != null) {
            activeCapture.setCurrentSourceTextureId(weatherTexture.toString());
        }

        Tessellator tessellator = Tessellator.instance;
        try {
            tessellator.startDrawingQuads();
            for (GuidebookSceneWeatherRenderColumn column : weatherColumnsScratch) {
                appendWeatherColumnQuad(level, tessellator, column, animationTick, centerX, centerZ, billboardAxes);
            }
            tessellator.draw();
        } finally {
            if (activeCapture != null) {
                activeCapture.setCurrentSourceTextureId(null);
            }
        }
        weatherColumnsScratch.clear();
        return true;
    }

    private void collectWeatherColumns(List<GuidebookSceneWeatherRenderColumn> out, GuidebookLevel level,
        GuidebookSceneLayerSelection layerSelection, List<GuidebookSceneWeatherEffect> weatherEffects, int[] bounds,
        float animationTick, float centerX, float centerY, float centerZ, GuidebookSceneWeatherType weatherType) {
        if (out == null || weatherEffects == null || weatherEffects.isEmpty()) {
            return;
        }
        int pooledColumnCount = 0;
        for (GuidebookSceneWeatherEffect effect : weatherEffects) {
            if (effect == null || effect.getWeatherType() != weatherType
                || !effect.isActiveAt((int) Math.floor(animationTick))) {
                continue;
            }
            float intensity = effect.resolveIntensity(animationTick);
            if (intensity <= 0.001f) {
                continue;
            }
            float alpha = GuidebookSceneWeatherSupport.resolveAlpha(effect.getWeatherType()) * intensity;
            if (alpha <= 0.001f) {
                continue;
            }
            float coverage = effect.resolveCoverage();
            for (GuidebookSceneWeatherArea area : effect.resolveAreas(bounds)) {
                if (area == null) {
                    continue;
                }
                int minZ = Math.max(area.getMinZ(), resolveWeatherMinCoordinate(centerZ));
                int maxZ = Math.min(area.getMaxZ(), resolveWeatherMaxCoordinate(centerZ));
                int minX = Math.max(area.getMinX(), resolveWeatherMinCoordinate(centerX));
                int maxX = Math.min(area.getMaxX(), resolveWeatherMaxCoordinate(centerX));
                if (minX > maxX || minZ > maxZ) {
                    continue;
                }
                for (int z = minZ; z <= maxZ; z++) {
                    for (int x = minX; x <= maxX; x++) {
                        if (!shouldRenderWeatherColumn(x, z, coverage)) {
                            continue;
                        }
                        GuidebookSceneWeatherRenderColumn column = createWeatherRenderColumn(
                            level,
                            layerSelection,
                            bounds,
                            effect.getWeatherType(),
                            x,
                            z,
                            alpha,
                            centerY,
                            pooledColumnCount);
                        if (column != null) {
                            out.add(column);
                            pooledColumnCount++;
                        }
                    }
                }
            }
        }
    }

    private GuidebookSceneWeatherRenderColumn createWeatherRenderColumn(GuidebookLevel level,
        GuidebookSceneLayerSelection layerSelection, int[] bounds, GuidebookSceneWeatherType weatherType, int x, int z,
        float alpha, float centerY, int pooledColumnIndex) {
        int precipitationBottom = Math.max(bounds[1], level.getPrecipitationHeight(x, z, bounds[1], bounds[4]));
        int precipitationTop = bounds[4] + 1;
        if (precipitationBottom > precipitationTop) {
            return null;
        }
        if (!isWeatherColumnVisible(layerSelection, precipitationBottom, precipitationTop)) {
            return null;
        }
        int renderBottom = precipitationBottom;
        int renderTop = precipitationTop;
        if (layerSelection != null && layerSelection.getMode() == GuidebookSceneLayerSelection.Mode.EACH
            && layerSelection.getSingleExportLayer() != null) {
            int visibleLayer = layerSelection.getSingleExportLayer();
            renderBottom = Math.max(renderBottom, visibleLayer);
            renderTop = Math.min(renderTop, visibleLayer + 1);
            if (renderBottom >= renderTop) {
                return null;
            }
        }
        int lightSampleY = Math.clamp((int) Math.floor(centerY), precipitationBottom, precipitationTop);
        return acquireWeatherRenderColumn(pooledColumnIndex)
            .set(weatherType, x, z, renderBottom, renderTop, lightSampleY, alpha);
    }

    private GuidebookSceneWeatherRenderColumn acquireWeatherRenderColumn(int index) {
        while (weatherColumnPool.size() <= index) {
            weatherColumnPool.add(new GuidebookSceneWeatherRenderColumn());
        }
        return weatherColumnPool.get(index);
    }

    private boolean isWeatherColumnVisible(GuidebookSceneLayerSelection layerSelection, int precipitationBottom,
        int precipitationTop) {
        if (layerSelection == null || layerSelection.getMode() == GuidebookSceneLayerSelection.Mode.ALL) {
            return true;
        }
        Integer singleExportLayer = layerSelection.getSingleExportLayer();
        if (singleExportLayer != null) {
            return singleExportLayer >= precipitationBottom && singleExportLayer < precipitationTop;
        }
        for (int y = precipitationBottom; y < precipitationTop; y++) {
            if (layerSelection.isLayerVisible(y)) {
                return true;
            }
        }
        return false;
    }

    private void appendWeatherColumnQuad(GuidebookLevel level, Tessellator tessellator,
        GuidebookSceneWeatherRenderColumn column, float animationTick, float centerX, float centerZ,
        BillboardAxes billboardAxes) {
        float halfWidth = GuidebookSceneWeatherSupport.resolveHalfWidth(column.weatherType());
        float offsetX = billboardAxes.horizontalRightX() * halfWidth;
        float offsetZ = billboardAxes.horizontalRightZ() * halfWidth;
        float x0 = column.x() + 0.5f - offsetX;
        float x1 = column.x() + 0.5f + offsetX;
        float z0 = column.z() + 0.5f - offsetZ;
        float z1 = column.z() + 0.5f + offsetZ;
        float alpha = resolveWeatherColumnAlpha(column, centerX, centerZ);
        if (alpha <= 0.001f) {
            return;
        }
        int brightness = resolveWeatherColumnBrightness(level, column);
        tessellator.setBrightness(brightness);
        tessellator.setColorRGBA_F(1.0f, 1.0f, 1.0f, alpha);

        float uvOffsetU = 0.0f;
        float uvOffsetV = resolveWeatherUvOffsetV(column, animationTick);
        if (column.weatherType() == GuidebookSceneWeatherType.SNOW) {
            uvOffsetU = resolveSnowUvOffsetU(column, animationTick);
        }

        tessellator.addVertexWithUV(
            x0,
            column.renderBottom(),
            z0,
            0.0f + uvOffsetU,
            column.renderBottom() * 0.25f + uvOffsetV);
        tessellator.addVertexWithUV(
            x1,
            column.renderBottom(),
            z1,
            1.0f + uvOffsetU,
            column.renderBottom() * 0.25f + uvOffsetV);
        tessellator
            .addVertexWithUV(x1, column.renderTop(), z1, 1.0f + uvOffsetU, column.renderTop() * 0.25f + uvOffsetV);
        tessellator
            .addVertexWithUV(x0, column.renderTop(), z0, 0.0f + uvOffsetU, column.renderTop() * 0.25f + uvOffsetV);
    }

    private BillboardAxes resolveBillboardAxes() {
        matrixBuffer.clear();
        GL11.glGetFloat(GL11.GL_MODELVIEW_MATRIX, matrixBuffer);
        float rightX = matrixBuffer.get(0);
        float rightY = matrixBuffer.get(4);
        float rightZ = matrixBuffer.get(8);
        float upX = matrixBuffer.get(1);
        float upY = matrixBuffer.get(5);
        float upZ = matrixBuffer.get(9);
        float rightLength = normalize3(rightX, rightY, rightZ);
        if (rightLength > 1.0e-6f) {
            rightX /= rightLength;
            rightY /= rightLength;
            rightZ /= rightLength;
        } else {
            rightX = 1.0f;
            rightY = 0.0f;
            rightZ = 0.0f;
        }
        float upLength = normalize3(upX, upY, upZ);
        if (upLength > 1.0e-6f) {
            upX /= upLength;
            upY /= upLength;
            upZ /= upLength;
        } else {
            upX = 0.0f;
            upY = 1.0f;
            upZ = 0.0f;
        }

        float horizontalRightX = rightX;
        float horizontalRightZ = rightZ;
        float horizontalLength = normalize2(horizontalRightX, horizontalRightZ);
        if (horizontalLength > 1.0e-6f) {
            horizontalRightX /= horizontalLength;
            horizontalRightZ /= horizontalLength;
        } else {
            horizontalRightX = 1.0f;
            horizontalRightZ = 0.0f;
        }
        return new BillboardAxes(rightX, rightY, rightZ, upX, upY, upZ, horizontalRightX, horizontalRightZ);
    }

    private float normalize3(float x, float y, float z) {
        return (float) Math.sqrt(x * x + y * y + z * z);
    }

    private float normalize2(float x, float z) {
        return (float) Math.sqrt(x * x + z * z);
    }

    private int resolveWeatherMinCoordinate(float center) {
        return (int) Math.floor(center) - WEATHER_RENDER_RADIUS;
    }

    private int resolveWeatherMaxCoordinate(float center) {
        return (int) Math.floor(center) + WEATHER_RENDER_RADIUS;
    }

    private boolean shouldRenderWeatherColumn(int x, int z, float coverage) {
        if (coverage >= 0.999f) {
            return true;
        }
        int threshold = Math.clamp(Math.round(coverage * WEATHER_COVERAGE_RESOLUTION), 1, WEATHER_COVERAGE_RESOLUTION);
        return Math.floorMod(resolveWeatherCoverageSeed(x, z), WEATHER_COVERAGE_RESOLUTION) < threshold;
    }

    private int resolveWeatherCoverageSeed(int x, int z) {
        return x * 73428767 ^ z * 912931;
    }

    private float resolveWeatherColumnAlpha(GuidebookSceneWeatherRenderColumn column, float centerX, float centerZ) {
        float alphaRadiusScale = GuidebookSceneWeatherSupport.resolveAlphaRadiusScale(column.weatherType());
        float dx = column.centerX() - centerX;
        float dz = column.centerZ() - centerZ;
        float radiusSquared = (dx * dx + dz * dz) / (WEATHER_RENDER_RADIUS * WEATHER_RENDER_RADIUS);
        float radialAlpha = (1.0f - radiusSquared) * alphaRadiusScale + 0.5f;
        radialAlpha = Math.clamp(radialAlpha, 0.0f, 1.0f);
        return column.baseAlpha() * radialAlpha;
    }

    private int resolveWeatherColumnBrightness(GuidebookLevel level, GuidebookSceneWeatherRenderColumn column) {
        int brightness = level.getLightBrightnessForSkyBlocks(column.x(), column.lightSampleY(), column.z(), 0);
        if (column.weatherType() == GuidebookSceneWeatherType.SNOW) {
            return (brightness * 3 + FULL_BRIGHTNESS) / 4;
        }
        return brightness;
    }

    private float resolveWeatherUvOffsetV(GuidebookSceneWeatherRenderColumn column, float animationTick) {
        if (column.weatherType() == GuidebookSceneWeatherType.SNOW) {
            return ((animationTick % 512.0f) / 512.0f);
        }
        int seed = column.seed();
        return ((seed & 31) + animationTick) / 32.0f;
    }

    private float resolveSnowUvOffsetU(GuidebookSceneWeatherRenderColumn column, float animationTick) {
        int seed = column.seed();
        return ((seed & 31) / 32.0f) + animationTick * 0.01f * (((seed >> 5) & 1) == 0 ? 1.0f : -1.0f);
    }

    private static class GuidebookSceneWeatherRenderColumn {

        private GuidebookSceneWeatherType weatherType;
        private int x;
        private int z;
        private int renderBottom;
        private int renderTop;
        private int lightSampleY;
        private float baseAlpha;

        private GuidebookSceneWeatherRenderColumn set(GuidebookSceneWeatherType weatherType, int x, int z,
            int renderBottom, int renderTop, int lightSampleY, float baseAlpha) {
            this.weatherType = weatherType;
            this.x = x;
            this.z = z;
            this.renderBottom = renderBottom;
            this.renderTop = renderTop;
            this.lightSampleY = lightSampleY;
            this.baseAlpha = baseAlpha;
            return this;
        }

        public GuidebookSceneWeatherType weatherType() {
            return weatherType;
        }

        public int x() {
            return x;
        }

        public int z() {
            return z;
        }

        public int renderBottom() {
            return renderBottom;
        }

        public int renderTop() {
            return renderTop;
        }

        public int lightSampleY() {
            return lightSampleY;
        }

        public float baseAlpha() {
            return baseAlpha;
        }

        public float centerX() {
            return x + 0.5f;
        }

        public float centerZ() {
            return z + 0.5f;
        }

        public int seed() {
            return x * x * 3121 + x * 45238971 + z * z * 418711 + z * 13761;
        }
    }

    private static class BillboardAxes {

        private final float rightX;
        private final float rightY;
        private final float rightZ;
        private final float upX;
        private final float upY;
        private final float upZ;
        private final float horizontalRightX;
        private final float horizontalRightZ;

        private BillboardAxes(float rightX, float rightY, float rightZ, float upX, float upY, float upZ,
            float horizontalRightX, float horizontalRightZ) {
            this.rightX = rightX;
            this.rightY = rightY;
            this.rightZ = rightZ;
            this.upX = upX;
            this.upY = upY;
            this.upZ = upZ;
            this.horizontalRightX = horizontalRightX;
            this.horizontalRightZ = horizontalRightZ;
        }

        public float rightX() {
            return rightX;
        }

        public float rightY() {
            return rightY;
        }

        public float rightZ() {
            return rightZ;
        }

        public float upX() {
            return upX;
        }

        public float upY() {
            return upY;
        }

        public float upZ() {
            return upZ;
        }

        public float horizontalRightX() {
            return horizontalRightX;
        }

        public float horizontalRightZ() {
            return horizontalRightZ;
        }
    }

    public static void log(Throwable t) {
        try {
            GuideDebugLog.warn("Scene render warning", t);
        } catch (Throwable ignore) {}
    }
}
