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
import java.util.Collections;
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
import net.minecraft.world.IBlockAccess;
import net.minecraftforge.client.ForgeHooksClient;

import org.apache.logging.log4j.LogManager;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
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
import com.hfstudio.guidenh.integration.api.client.GuideNhClientIntegrationRegistry;
import com.hfstudio.guidenh.mixins.early.forge.AccessorForgeHooksClient;

public class GuidebookLevelRenderer {

    public static final GuidebookLevelRenderer INSTANCE = new GuidebookLevelRenderer();
    public static final int FULL_BRIGHTNESS = 15728880;

    private final FloatBuffer matrixBuffer = BufferUtils.createFloatBuffer(16);
    private final GuideEntityRenderStateResolver.ResolvedEntityRenderState entityRenderState = new GuideEntityRenderStateResolver.ResolvedEntityRenderState();

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
            Collections.emptyList(),
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
            Collections.emptyList());
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
            particles);
    }

    public void render(GuidebookLevel level, CameraSettings camera, int panelX, int panelY, int panelWidth,
        int panelHeight, int scissorX, int scissorY, int scissorW, int scissorH, float partialTicks,
        List<InWorldAnnotation> annotations, LightDarkMode lightDarkMode, GuidebookSceneLayerSelection layerSelection,
        List<GuidebookSceneParticle> particles) {

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
                        if (!particles.isEmpty()) {
                            renderParticlesInContext(particles);
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

    private void renderParticlesInContext(List<GuidebookSceneParticle> particles) {
        matrixBuffer.clear();
        GL11.glGetFloat(GL11.GL_MODELVIEW_MATRIX, matrixBuffer);
        // For a billboard facing the camera we need the camera right/up vectors in scene space.
        // The modelview matrix M maps scene coords to eye coords (M = V * S, S = scene transform).
        // GL stores M in column-major order: element [col*4 + row].
        // Row 0 of M = [m[0], m[4], m[8]] → camera-right in scene space.
        // Row 1 of M = [m[1], m[5], m[9]] → camera-up in scene space.
        // Normalize because the scene transform S may include a scale factor.
        float rx = matrixBuffer.get(0), ry = matrixBuffer.get(4), rz = matrixBuffer.get(8);
        float ux = matrixBuffer.get(1), uy = matrixBuffer.get(5), uz = matrixBuffer.get(9);
        float rLen = (float) Math.sqrt(rx * rx + ry * ry + rz * rz);
        if (rLen > 1e-6f) {
            rx /= rLen;
            ry /= rLen;
            rz /= rLen;
        }
        float uLen = (float) Math.sqrt(ux * ux + uy * uy + uz * uz);
        if (uLen > 1e-6f) {
            ux /= uLen;
            uy /= uLen;
            uz /= uLen;
        }

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
            Minecraft.getMinecraft()
                .getTextureManager()
                .bindTexture(TextureMap.locationBlocksTexture);

            var tess = Tessellator.instance;
            tess.startDrawingQuads();
            for (GuidebookSceneParticle p : particles) {
                if (p.isDead()) continue;
                float alpha = p.getAlpha();
                tess.setColorRGBA_F(p.red, p.green, p.blue, alpha);
                float s = p.size;
                float cx = p.x, cy = p.y, cz = p.z;
                tess.addVertexWithUV(cx - rx * s - ux * s, cy - ry * s - uy * s, cz - rz * s - uz * s, p.u0, p.v1);
                tess.addVertexWithUV(cx + rx * s - ux * s, cy + ry * s - uy * s, cz + rz * s - uz * s, p.u1, p.v1);
                tess.addVertexWithUV(cx + rx * s + ux * s, cy + ry * s + uy * s, cz + rz * s + uz * s, p.u1, p.v0);
                tess.addVertexWithUV(cx - rx * s + ux * s, cy - ry * s + uy * s, cz - rz * s + uz * s, p.u0, p.v0);
            }
            tess.draw();
        } finally {
            GL11.glPopAttrib();
        }
    }

    public static void log(Throwable t) {
        try {
            GuideDebugLog.warn(LogManager.getLogger("GuideNH/SceneRenderer"), "Scene render warning", t);
        } catch (Throwable ignore) {}
    }
}
