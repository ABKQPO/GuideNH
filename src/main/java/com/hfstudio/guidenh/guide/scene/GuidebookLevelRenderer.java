package com.hfstudio.guidenh.guide.scene;

import static org.lwjgl.opengl.GL11.GL_ALPHA_TEST;
import static org.lwjgl.opengl.GL11.GL_BLEND;
import static org.lwjgl.opengl.GL11.GL_DEPTH_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.GL_DEPTH_TEST;
import static org.lwjgl.opengl.GL11.GL_LIGHTING;
import static org.lwjgl.opengl.GL11.GL_MODELVIEW;
import static org.lwjgl.opengl.GL11.GL_PROJECTION;
import static org.lwjgl.opengl.GL11.GL_SCISSOR_TEST;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_2D;

import java.lang.reflect.Field;
import java.nio.FloatBuffer;
import java.util.Collections;
import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.client.ForgeHooksClient;

import org.apache.logging.log4j.LogManager;
import org.joml.Matrix4f;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

import com.hfstudio.guidenh.guide.color.LightDarkMode;
import com.hfstudio.guidenh.guide.internal.util.DisplayScale;
import com.hfstudio.guidenh.guide.scene.annotation.InWorldAnnotation;
import com.hfstudio.guidenh.guide.scene.annotation.InWorldAnnotationRenderer;
import com.hfstudio.guidenh.guide.scene.level.GuidebookLevel;

public final class GuidebookLevelRenderer {

    private static final GuidebookLevelRenderer INSTANCE = new GuidebookLevelRenderer();

    private static final Field FORGE_RENDER_PASS_FIELD = resolveRenderPassField();

    private final FloatBuffer matrixBuffer = BufferUtils.createFloatBuffer(16);

    // Rebind RenderBlocks only when the level instance changes.
    private RenderBlocks cachedRenderBlocks;
    private GuidebookLevel cachedRenderBlocksLevel;

    public static GuidebookLevelRenderer getInstance() {
        return INSTANCE;
    }

    private GuidebookLevelRenderer() {}

    public void render(GuidebookLevel level, CameraSettings camera, int panelX, int panelY, int panelWidth,
        int panelHeight, float partialTicks) {
        render(
            level,
            camera,
            panelX,
            panelY,
            panelWidth,
            panelHeight,
            partialTicks,
            Collections.emptyList(),
            LightDarkMode.LIGHT_MODE);
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
            lightDarkMode);
    }

    public void render(GuidebookLevel level, CameraSettings camera, int panelX, int panelY, int panelWidth,
        int panelHeight, int scissorX, int scissorY, int scissorW, int scissorH, float partialTicks,
        List<InWorldAnnotation> annotations, LightDarkMode lightDarkMode) {

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
            GL11.glEnable(GL_ALPHA_TEST);
            GL11.glAlphaFunc(GL11.GL_GREATER, 0.1f);
            GL11.glEnable(GL_TEXTURE_2D);
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

                trySetRenderPass(0);
                GL11.glDisable(GL_BLEND);
                renderBlocksPass(level, filledBlocks, 0);

                trySetRenderPass(1);
                GL11.glEnable(GL_BLEND);
                GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
                GL11.glDepthMask(false);
                renderBlocksPass(level, filledBlocks, 1);
                GL11.glDepthMask(true);
                GL11.glDisable(GL_BLEND);

                trySetRenderPass(-1);

                renderBlockEntities(level, tileEntities, partialTicks);
                renderEntitiesStub(level, partialTicks);

                if (!annotations.isEmpty()) {
                    InWorldAnnotationRenderer.render(annotations, lightDarkMode);
                }
            } catch (Throwable t) {
                log(t);
            } finally {
                GL11.glMatrixMode(GL_PROJECTION);
                GL11.glPopMatrix();
                GL11.glMatrixMode(GL_MODELVIEW);
                GL11.glPopMatrix();
            }
        } finally {
            GL11.glPopAttrib();
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

    private void renderBlocksPass(GuidebookLevel level, Iterable<int[]> filledBlocks, int pass) {
        RenderBlocks rb = cachedRenderBlocks;
        if (rb == null || cachedRenderBlocksLevel != level) {
            rb = new RenderBlocks(level);
            cachedRenderBlocks = rb;
            cachedRenderBlocksLevel = level;
        }
        var tes = Tessellator.instance;
        tes.startDrawingQuads();
        for (int[] p : filledBlocks) {
            Block block = level.getBlock(p[0], p[1], p[2]);
            if (block == null) continue;
            if (!block.canRenderInPass(pass)) continue;
            try {
                rb.renderBlockByRenderType(block, p[0], p[1], p[2]);
            } catch (Throwable t) {
                log(t);
            }
        }
        tes.draw();
    }

    private void renderBlockEntities(GuidebookLevel level, Iterable<TileEntity> tileEntities, float partialTicks) {
        var dispatcher = TileEntityRendererDispatcher.instance;
        var fakeWorld = level.getOrCreateFakeWorld();
        var prevWorld = dispatcher.field_147550_f;
        dispatcher.field_147550_f = fakeWorld;
        try {
            for (TileEntity te : tileEntities) {
                try {
                    dispatcher.renderTileEntityAt(te, te.xCoord, te.yCoord, te.zCoord, partialTicks);
                } catch (Throwable t) {
                    log(t);
                }
            }
        } finally {
            dispatcher.field_147550_f = prevWorld;
        }
    }

    @SuppressWarnings("unused")
    private void renderEntitiesStub(GuidebookLevel level, float partialTicks) {}

    private void loadMatrix(Matrix4f m) {
        matrixBuffer.clear();
        m.get(matrixBuffer);
        matrixBuffer.rewind();
        GL11.glLoadMatrix(matrixBuffer);
    }

    private static void trySetRenderPass(int pass) {
        if (FORGE_RENDER_PASS_FIELD == null) return;
        try {
            FORGE_RENDER_PASS_FIELD.setInt(null, pass);
        } catch (Throwable ignore) {}
    }

    private static Field resolveRenderPassField() {
        try {
            var f = ForgeHooksClient.class.getDeclaredField("renderPass");
            f.setAccessible(true);
            return f;
        } catch (Throwable t) {
            return null;
        }
    }

    private static void log(Throwable t) {
        try {
            LogManager.getLogger("GuideNH/SceneRenderer")
                .warn("Scene render warning", t);
        } catch (Throwable ignore) {}
    }
}
