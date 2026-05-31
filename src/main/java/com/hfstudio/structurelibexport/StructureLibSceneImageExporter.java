package com.hfstudio.structurelibexport;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import javax.imageio.ImageIO;

import net.minecraft.client.Minecraft;
import net.minecraft.client.shader.Framebuffer;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

import com.github.bsideup.jabel.Desugar;
import com.hfstudio.guidenh.guide.scene.CameraSettings;
import com.hfstudio.guidenh.guide.scene.GuidebookSceneLayerSelection;
import com.hfstudio.guidenh.guide.scene.annotation.InWorldAnnotation;
import com.hfstudio.guidenh.guide.scene.annotation.OverlayAnnotation;
import com.hfstudio.guidenh.guide.scene.level.GuidebookLevel;

public class StructureLibSceneImageExporter {

    private final StructureLibExportLevelRenderer renderer;
    private final StructureLibExportOverlayRenderer overlayRenderer;
    private final StructureLibExportImageLimits imageLimits;

    public StructureLibSceneImageExporter() {
        this(
            new StructureLibExportLevelRenderer(),
            new StructureLibExportOverlayRenderer(),
            new StructureLibExportImageLimits());
    }

    public StructureLibSceneImageExporter(StructureLibExportLevelRenderer renderer) {
        this(renderer, new StructureLibExportOverlayRenderer(), new StructureLibExportImageLimits());
    }

    public StructureLibSceneImageExporter(StructureLibExportLevelRenderer renderer,
        StructureLibExportImageLimits imageLimits) {
        this(renderer, new StructureLibExportOverlayRenderer(), imageLimits);
    }

    public StructureLibSceneImageExporter(StructureLibExportLevelRenderer renderer,
        StructureLibExportOverlayRenderer overlayRenderer, StructureLibExportImageLimits imageLimits) {
        this.renderer = renderer != null ? renderer : new StructureLibExportLevelRenderer();
        this.overlayRenderer = overlayRenderer != null ? overlayRenderer : new StructureLibExportOverlayRenderer();
        this.imageLimits = imageLimits != null ? imageLimits : new StructureLibExportImageLimits();
    }

    public ExportedImage export(GuidebookLevel level, CameraSettings camera, GuidebookSceneLayerSelection layers,
        StructureLibExportBackground background, Path target, int width, int height, long maxPixels) throws Exception {
        return export(level, camera, layers, List.of(), List.of(), background, target, width, height, maxPixels);
    }

    public ExportedImage export(GuidebookLevel level, CameraSettings camera, GuidebookSceneLayerSelection layers,
        List<InWorldAnnotation> annotations, StructureLibExportBackground background, Path target, int width,
        int height, long maxPixels) throws Exception {
        return export(level, camera, layers, annotations, List.of(), background, target, width, height, maxPixels);
    }

    public ExportedImage export(GuidebookLevel level, CameraSettings camera, GuidebookSceneLayerSelection layers,
        List<InWorldAnnotation> annotations, List<OverlayAnnotation> overlays, StructureLibExportBackground background,
        Path target, int width, int height, long maxPixels) throws Exception {
        imageLimits.validateSize(width, height, maxPixels);
        BufferedImage image = render(level, camera, layers, annotations, overlays, background, width, height);
        try {
            Files.createDirectories(target.getParent());
            if (!ImageIO.write(image, "png", target.toFile())) {
                throw new IllegalStateException("ImageIO could not encode PNG.");
            }
            return new ExportedImage(target, image.getWidth(), image.getHeight());
        } finally {
            image.flush();
        }
    }

    private BufferedImage render(GuidebookLevel level, CameraSettings camera, GuidebookSceneLayerSelection layers,
        List<InWorldAnnotation> annotations, List<OverlayAnnotation> overlays, StructureLibExportBackground background,
        int width, int height) {
        int maxFboSize = GL11.glGetInteger(GL11.GL_MAX_TEXTURE_SIZE);
        if (maxFboSize <= 0) {
            maxFboSize = 8192;
        }
        maxFboSize = Math.min(maxFboSize, imageLimits.resolveMaxTileEdge(maxFboSize));
        if (width <= maxFboSize && height <= maxFboSize) {
            return renderTile(
                level,
                camera,
                layers,
                annotations,
                overlays,
                background,
                width,
                height,
                0,
                0,
                width,
                height);
        }

        BufferedImage output = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = output.createGraphics();
        try {
            for (int y = 0; y < height; y += maxFboSize) {
                int tileHeight = Math.min(maxFboSize, height - y);
                for (int x = 0; x < width; x += maxFboSize) {
                    int tileWidth = Math.min(maxFboSize, width - x);
                    BufferedImage tile = renderTile(
                        level,
                        camera,
                        layers,
                        annotations,
                        overlays,
                        background,
                        width,
                        height,
                        x,
                        y,
                        tileWidth,
                        tileHeight);
                    try {
                        graphics.drawImage(tile, x, y, null);
                    } finally {
                        tile.flush();
                    }
                }
            }
        } finally {
            graphics.dispose();
        }
        return output;
    }

    private BufferedImage renderTile(GuidebookLevel level, CameraSettings camera, GuidebookSceneLayerSelection layers,
        List<InWorldAnnotation> annotations, List<OverlayAnnotation> overlays, StructureLibExportBackground background,
        int fullWidth, int fullHeight, int offsetX, int offsetY, int tileWidth, int tileHeight) {
        Minecraft minecraft = Minecraft.getMinecraft();
        int previousDisplayWidth = minecraft.displayWidth;
        int previousDisplayHeight = minecraft.displayHeight;
        int previousGuiScale = minecraft.gameSettings.guiScale;
        Framebuffer framebuffer = new Framebuffer(tileWidth, tileHeight, true);
        try {
            minecraft.displayWidth = tileWidth;
            minecraft.displayHeight = tileHeight;
            minecraft.gameSettings.guiScale = 1;

            framebuffer.bindFramebuffer(true);
            int argb = background.getArgb();
            float alpha = ((argb >>> 24) & 0xFF) / 255f;
            float red = ((argb >>> 16) & 0xFF) / 255f;
            float green = ((argb >>> 8) & 0xFF) / 255f;
            float blue = (argb & 0xFF) / 255f;
            GL11.glClearColor(red, green, blue, alpha);
            GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);

            renderer.renderExportTile(
                level,
                camera,
                layers,
                annotations,
                -offsetX,
                -offsetY,
                fullWidth,
                fullHeight,
                tileWidth,
                tileHeight);
            overlayRenderer.render(camera, overlays, -offsetX, -offsetY, fullWidth, fullHeight, tileWidth, tileHeight);
            BufferedImage image = readPixels(tileWidth, tileHeight);
            if (!background.isTransparent()) {
                return image;
            }
            return image;
        } finally {
            framebuffer.unbindFramebuffer();
            framebuffer.deleteFramebuffer();
            minecraft.displayWidth = previousDisplayWidth;
            minecraft.displayHeight = previousDisplayHeight;
            minecraft.gameSettings.guiScale = previousGuiScale;
            GL11.glViewport(0, 0, previousDisplayWidth, previousDisplayHeight);
        }
    }

    private BufferedImage readPixels(int width, int height) {
        ByteBuffer buffer = BufferUtils.createByteBuffer(width * height * 4);
        GL11.glReadPixels(0, 0, width, height, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, buffer);
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < height; y++) {
            int flippedY = height - 1 - y;
            for (int x = 0; x < width; x++) {
                int index = (x + y * width) * 4;
                int red = buffer.get(index) & 0xFF;
                int green = buffer.get(index + 1) & 0xFF;
                int blue = buffer.get(index + 2) & 0xFF;
                int alpha = buffer.get(index + 3) & 0xFF;
                image.setRGB(x, flippedY, alpha << 24 | red << 16 | green << 8 | blue);
            }
        }
        return image;
    }

    @Desugar
    public record ExportedImage(Path path, int width, int height) {}
}
