package com.hfstudio.guidenh.guide.internal.editor.io;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Iterator;
import java.util.Objects;

import javax.annotation.Nullable;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriter;

import com.hfstudio.guidenh.guide.document.LytRect;
import com.hfstudio.guidenh.guide.scene.LytGuidebookScene;

public class SceneEditorScreenshotExportService {

    private static final int OPAQUE_BACKGROUND_RGB = 0x121216;
    private static final DateTimeFormatter FILE_NAME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH.mm.ss");

    private final Path rootDirectory;
    private final Renderer renderer;
    private final Encoder encoder;
    private final TimestampSupplier timestampSupplier;

    public SceneEditorScreenshotExportService(Path rootDirectory) {
        this(rootDirectory, new OffscreenRenderer(), new ImageIoEncoder(), LocalDateTime::now);
    }

    SceneEditorScreenshotExportService(Path rootDirectory, Renderer renderer, Encoder encoder,
        TimestampSupplier timestampSupplier) {
        this.rootDirectory = Objects.requireNonNull(rootDirectory, "rootDirectory");
        this.renderer = Objects.requireNonNull(renderer, "renderer");
        this.encoder = Objects.requireNonNull(encoder, "encoder");
        this.timestampSupplier = Objects.requireNonNull(timestampSupplier, "timestampSupplier");
    }

    public ExportResult export(LytGuidebookScene scene, SceneEditorScreenshotFormat format, int scale, int sceneWidth,
        int sceneHeight) {
        if (scene == null) {
            return ExportResult.failure(new IllegalArgumentException("scene cannot be null"));
        }

        SceneEditorScreenshotFormat normalizedFormat = format != null ? format : SceneEditorScreenshotFormat.PNG;
        int normalizedScale = Math.max(1, scale);
        int cropWidth = Math.max(16, sceneWidth);
        int cropHeight = Math.max(16, sceneHeight);
        int originalBackground = scene.getSceneBackgroundColor();
        int originalBorder = scene.getSceneBorderColor();
        int originalSceneWidth = scene.getSceneWidth();
        int originalSceneHeight = scene.getSceneHeight();
        boolean originalSceneButtonsVisible = scene.isSceneButtonsVisible();
        boolean originalBottomControlsVisible = scene.isBottomControlsVisible();
        boolean originalReserveBottomControlArea = scene.isReserveBottomControlArea();
        float originalZoom = scene.getCamera()
            .getZoom();
        LytRect currentViewport = scene.getScreenRect();
        int logicalViewportWidth = Math.max(
            16,
            currentViewport != null && !currentViewport.isEmpty() ? currentViewport.width()
                : originalSceneWidth > 0 ? originalSceneWidth : cropWidth);
        int logicalViewportHeight = Math.max(
            16,
            currentViewport != null && !currentViewport.isEmpty() ? currentViewport.height()
                : originalSceneHeight > 0 ? originalSceneHeight : cropHeight);
        int renderWidth = logicalViewportWidth * normalizedScale;
        int renderHeight = logicalViewportHeight * normalizedScale;

        try {
            if (!encoder.canWrite(normalizedFormat)) {
                return ExportResult.failure(
                    new IllegalStateException("No ImageIO writer available for ." + normalizedFormat.fileExtension()));
            }

            scene.setSceneBackgroundColor(0x00000000);
            scene.setSceneBorderColor(0x00000000);
            scene.setSceneButtonsVisible(false);
            scene.setBottomControlsVisible(false);
            scene.setReserveBottomControlArea(false);
            scene.setSceneSize(renderWidth, renderHeight);
            scene.setCameraViewportOverride(logicalViewportWidth, logicalViewportHeight);

            BufferedImage image = renderer.render(scene, renderWidth, renderHeight);
            image = cropCentered(image, cropWidth * normalizedScale, cropHeight * normalizedScale);
            if (!normalizedFormat.supportsAlpha()) {
                image = compositeOpaque(image);
            }

            Path target = resolveTargetPath(normalizedFormat);
            encoder.write(image, normalizedFormat, target);
            return ExportResult.success(target);
        } catch (Throwable throwable) {
            return ExportResult.failure(throwable);
        } finally {
            scene.setSceneBackgroundColor(originalBackground);
            scene.setSceneBorderColor(originalBorder);
            scene.setSceneButtonsVisible(originalSceneButtonsVisible);
            scene.setBottomControlsVisible(originalBottomControlsVisible);
            scene.setReserveBottomControlArea(originalReserveBottomControlArea);
            scene.setSceneSize(originalSceneWidth, originalSceneHeight);
            scene.clearCameraViewportOverride();
            scene.getCamera()
                .setZoom(originalZoom);
        }
    }

    private BufferedImage cropCentered(BufferedImage source, int width, int height) {
        int cropWidth = Math.max(1, Math.min(width, source.getWidth()));
        int cropHeight = Math.max(1, Math.min(height, source.getHeight()));
        if (cropWidth == source.getWidth() && cropHeight == source.getHeight()) {
            return source;
        }
        int cropX = Math.max(0, (source.getWidth() - cropWidth) / 2);
        int cropY = Math.max(0, (source.getHeight() - cropHeight) / 2);
        BufferedImage cropped = new BufferedImage(cropWidth, cropHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = cropped.createGraphics();
        try {
            graphics.drawImage(
                source,
                0,
                0,
                cropWidth,
                cropHeight,
                cropX,
                cropY,
                cropX + cropWidth,
                cropY + cropHeight,
                null);
        } finally {
            graphics.dispose();
        }
        return cropped;
    }

    private BufferedImage compositeOpaque(BufferedImage source) {
        BufferedImage image = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        try {
            graphics.setColor(new Color(OPAQUE_BACKGROUND_RGB));
            graphics.fillRect(0, 0, source.getWidth(), source.getHeight());
            graphics.drawImage(source, 0, 0, null);
        } finally {
            graphics.dispose();
        }
        return image;
    }

    private Path resolveTargetPath(SceneEditorScreenshotFormat format) throws Exception {
        Path screenshotsDir = rootDirectory.resolve("screenshots");
        Files.createDirectories(screenshotsDir);

        String baseName = FILE_NAME_FORMAT.format(timestampSupplier.get()) + "_guidenh_scene";
        Path candidate = screenshotsDir.resolve(baseName + "." + format.fileExtension());
        int collisionIndex = 2;
        while (Files.exists(candidate)) {
            candidate = screenshotsDir.resolve(baseName + "_" + collisionIndex + "." + format.fileExtension());
            collisionIndex++;
        }
        return candidate;
    }

    @FunctionalInterface
    interface Renderer {

        BufferedImage render(LytGuidebookScene scene, int width, int height) throws Exception;
    }

    public interface Encoder {

        boolean canWrite(SceneEditorScreenshotFormat format);

        void write(BufferedImage image, SceneEditorScreenshotFormat format, Path target) throws Exception;
    }

    @FunctionalInterface
    interface TimestampSupplier {

        LocalDateTime get();
    }

    private static final class OffscreenRenderer implements Renderer {

        @Override
        public BufferedImage render(LytGuidebookScene scene, int width, int height) throws Exception {
            try (SceneEditorOffscreenFramebuffer framebuffer = new SceneEditorOffscreenFramebuffer(width, height)) {
                return framebuffer.render(scene);
            }
        }
    }

    public static final class ImageIoEncoder implements Encoder {

        @Override
        public boolean canWrite(SceneEditorScreenshotFormat format) {
            Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName(format.fileExtension());
            return writers.hasNext();
        }

        @Override
        public void write(BufferedImage image, SceneEditorScreenshotFormat format, Path target) throws Exception {
            Files.createDirectories(target.getParent());
            if (!ImageIO.write(image, format.fileExtension(), target.toFile())) {
                throw new IllegalStateException("ImageIO could not encode ." + format.fileExtension());
            }
        }
    }

    public static final class ExportResult {

        private final boolean success;
        @Nullable
        private final Path savedPath;
        @Nullable
        private final Throwable error;

        private ExportResult(boolean success, @Nullable Path savedPath, @Nullable Throwable error) {
            this.success = success;
            this.savedPath = savedPath;
            this.error = error;
        }

        public static ExportResult success(Path savedPath) {
            return new ExportResult(true, savedPath, null);
        }

        public static ExportResult failure(Throwable error) {
            return new ExportResult(false, null, error);
        }

        public boolean isSuccess() {
            return success;
        }

        public Path getSavedPath() {
            return savedPath;
        }

        @Nullable
        public Throwable getError() {
            return error;
        }
    }
}
