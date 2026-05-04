package com.hfstudio.guidenh.guide.internal.editor.io;

import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.shader.Framebuffer;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

import com.hfstudio.guidenh.guide.color.LightDarkMode;
import com.hfstudio.guidenh.guide.document.LytRect;
import com.hfstudio.guidenh.guide.layout.LayoutContext;
import com.hfstudio.guidenh.guide.layout.MinecraftFontMetrics;
import com.hfstudio.guidenh.guide.render.VanillaRenderContext;
import com.hfstudio.guidenh.guide.scene.LytGuidebookScene;

public class SceneEditorOffscreenFramebuffer implements AutoCloseable {

    private final Framebuffer framebuffer;
    private final int width;
    private final int height;

    public SceneEditorOffscreenFramebuffer(int width, int height) {
        this.width = Math.max(16, width);
        this.height = Math.max(16, height);
        this.framebuffer = new Framebuffer(this.width, this.height, true);
        this.framebuffer.setFramebufferColor(0f, 0f, 0f, 0f);
    }

    public BufferedImage render(LytGuidebookScene scene) {
        if (scene == null) {
            throw new IllegalArgumentException("scene cannot be null");
        }
        Minecraft minecraft = Minecraft.getMinecraft();
        if (minecraft == null || minecraft.gameSettings == null) {
            throw new IllegalStateException("Minecraft client is not ready for screenshot export.");
        }

        int previousDisplayWidth = minecraft.displayWidth;
        int previousDisplayHeight = minecraft.displayHeight;
        int previousGuiScale = minecraft.gameSettings.guiScale;

        try {
            minecraft.displayWidth = width;
            minecraft.displayHeight = height;
            minecraft.gameSettings.guiScale = 1;

            framebuffer.bindFramebuffer(true);
            GL11.glClearColor(0f, 0f, 0f, 0f);
            GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);

            scene.setSceneSize(width, height);
            scene.layout(new LayoutContext(new MinecraftFontMetrics()), 0, 0, width);

            VanillaRenderContext renderContext = new VanillaRenderContext(
                LightDarkMode.LIGHT_MODE,
                new LytRect(0, 0, width, height),
                height);
            renderContext.setDocumentOrigin(0, 0);
            renderContext.setScrollOffsetY(0);
            scene.render(renderContext);

            return readPixels();
        } finally {
            framebuffer.unbindFramebuffer();
            minecraft.displayWidth = previousDisplayWidth;
            minecraft.displayHeight = previousDisplayHeight;
            minecraft.gameSettings.guiScale = previousGuiScale;
            GL11.glViewport(0, 0, previousDisplayWidth, previousDisplayHeight);
        }
    }

    /**
     * Renders a tile of the scene at the given pixel offset within the full render.
     *
     * <p>
     * Unlike {@link #render(LytGuidebookScene)}, this method does <em>not</em> call
     * {@code scene.setSceneSize()} or {@code scene.layout()}: the caller is responsible
     * for laying out the scene at the full render resolution before calling this method.
     * The document origin is shifted by {@code (-offsetX, -offsetY)} so that only the
     * portion of the scene starting at {@code (offsetX, offsetY)} is written into this
     * framebuffer.
     * </p>
     */
    public BufferedImage renderTile(LytGuidebookScene scene, int offsetX, int offsetY) {
        if (scene == null) {
            throw new IllegalArgumentException("scene cannot be null");
        }
        Minecraft minecraft = Minecraft.getMinecraft();
        if (minecraft == null || minecraft.gameSettings == null) {
            throw new IllegalStateException("Minecraft client is not ready for screenshot export.");
        }

        int previousDisplayWidth = minecraft.displayWidth;
        int previousDisplayHeight = minecraft.displayHeight;
        int previousGuiScale = minecraft.gameSettings.guiScale;

        try {
            minecraft.displayWidth = width;
            minecraft.displayHeight = height;
            minecraft.gameSettings.guiScale = 1;

            framebuffer.bindFramebuffer(true);
            GL11.glClearColor(0f, 0f, 0f, 0f);
            GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);

            VanillaRenderContext renderContext = new VanillaRenderContext(
                LightDarkMode.LIGHT_MODE,
                new LytRect(0, 0, width, height),
                height);
            renderContext.setDocumentOrigin(-offsetX, -offsetY);
            renderContext.setScrollOffsetY(0);
            scene.render(renderContext);

            return readPixels();
        } finally {
            framebuffer.unbindFramebuffer();
            minecraft.displayWidth = previousDisplayWidth;
            minecraft.displayHeight = previousDisplayHeight;
            minecraft.gameSettings.guiScale = previousGuiScale;
            GL11.glViewport(0, 0, previousDisplayWidth, previousDisplayHeight);
        }
    }

    private BufferedImage readPixels() {
        ByteBuffer buffer = BufferUtils.createByteBuffer(width * height * 4);
        GL11.glReadPixels(0, 0, width, height, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, buffer);

        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < height; y++) {
            int flippedY = height - 1 - y;
            for (int x = 0; x < width; x++) {
                int index = (x + y * width) * 4;
                int r = buffer.get(index) & 0xFF;
                int g = buffer.get(index + 1) & 0xFF;
                int b = buffer.get(index + 2) & 0xFF;
                int a = buffer.get(index + 3) & 0xFF;
                image.setRGB(x, flippedY, (a << 24) | (r << 16) | (g << 8) | b);
            }
        }
        return image;
    }

    @Override
    public void close() {
        framebuffer.deleteFramebuffer();
    }
}
