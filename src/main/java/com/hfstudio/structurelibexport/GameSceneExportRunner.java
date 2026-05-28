package com.hfstudio.structurelibexport;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.ChatComponentText;

import org.joml.Vector3fc;

import com.hfstudio.guidenh.guide.GuidePage;
import com.hfstudio.guidenh.guide.compiler.ParsedGuidePage;
import com.hfstudio.guidenh.guide.internal.GuideRegistry;
import com.hfstudio.guidenh.guide.internal.MutableGuide;
import com.hfstudio.guidenh.guide.scene.CameraSettings;
import com.hfstudio.guidenh.guide.scene.GuidebookSceneLayerSelection;
import com.hfstudio.guidenh.guide.scene.LytGuidebookScene;
import com.hfstudio.guidenh.guide.scene.annotation.InWorldAnnotation;
import com.hfstudio.guidenh.guide.scene.annotation.OverlayAnnotation;
import com.hfstudio.guidenh.guide.scene.level.GuidebookLevel;

public class GameSceneExportRunner {

    public static final int DEFAULT_GAME_SCENE_PIXELS_PER_BLOCK = 128;

    private final StructureLibLayerResolver layerResolver;
    private final StructureLibSceneCameraFitter cameraFitter;
    private final StructureLibSceneImageExporter imageExporter;
    private final StructureLibExportManifestWriter manifestWriter;

    public GameSceneExportRunner() {
        this(
            new StructureLibLayerResolver(),
            new StructureLibSceneCameraFitter(),
            new StructureLibSceneImageExporter(),
            new StructureLibExportManifestWriter());
    }

    public GameSceneExportRunner(StructureLibLayerResolver layerResolver, StructureLibSceneCameraFitter cameraFitter,
        StructureLibSceneImageExporter imageExporter, StructureLibExportManifestWriter manifestWriter) {
        this.layerResolver = layerResolver;
        this.cameraFitter = cameraFitter;
        this.imageExporter = imageExporter;
        this.manifestWriter = manifestWriter;
    }

    public void run(ICommandSender sender, GameSceneExportOptions options) {
        try {
            Path outputDirectory = resolveOutputDirectory(options);
            Files.createDirectories(outputDirectory);
            List<GameSceneExportTaskSpec> tasks = discoverTasks(options);
            GameSceneExportManifest manifest = new GameSceneExportManifest();
            GameSceneExportFileNamer fileNamer = new GameSceneExportFileNamer();
            int successCount = 0;
            int failureCount = 0;
            int renderedTaskCount = 0;
            sender.addChatMessage(
                new ChatComponentText("GameScene export started: " + tasks.size() + " scene(s) discovered."));
            for (GameSceneExportTaskSpec task : tasks) {
                List<GameSceneExportTaskSpec> renderTasks = expandRenderTasks(task);
                for (GameSceneExportTaskSpec renderTask : renderTasks) {
                    renderedTaskCount++;
                    if (renderedTaskCount > StructureLibExportPlanner.DEFAULT_MAX_TASKS && !options.isForce()) {
                        throw new IllegalStateException(
                            "GameScene export expanded beyond " + StructureLibExportPlanner.DEFAULT_MAX_TASKS
                                + " screenshots. Use --force to allow this.");
                    }
                    Path target = fileNamer.resolve(outputDirectory, renderTask);
                    renderTask.setOutputPath(target);
                    try {
                        ExportRenderPlan renderPlan = planRender(renderTask);
                        if (options.isDryRun()) {
                            successCount++;
                            manifest.add(
                                new GameSceneExportManifest.Entry(
                                    true,
                                    renderTask,
                                    target.toString(),
                                    renderPlan.width,
                                    renderPlan.height,
                                    List.of(),
                                    List.of()));
                            checkpointManifest(sender, outputDirectory, manifest, options);
                            continue;
                        }
                        LytGuidebookScene scene = renderTask.getScene();
                        StructureLibSceneImageExporter.ExportedImage image = imageExporter.export(
                            scene.getLevel(),
                            renderPlan.camera,
                            renderPlan.layers,
                            renderPlan.annotations,
                            renderPlan.overlays,
                            renderTask.getBackground(),
                            target,
                            renderPlan.width,
                            renderPlan.height,
                            options.getMaxPixels());
                        successCount++;
                        manifest.add(
                            new GameSceneExportManifest.Entry(
                                true,
                                renderTask,
                                image.getPath()
                                    .toString(),
                                image.getWidth(),
                                image.getHeight(),
                                List.of(),
                                List.of()));
                        checkpointManifest(sender, outputDirectory, manifest, options);
                    } catch (Throwable t) {
                        failureCount++;
                        ArrayList<String> errors = new ArrayList<>();
                        errors.add(resolveErrorMessage(t));
                        manifest.add(
                            new GameSceneExportManifest.Entry(
                                false,
                                renderTask,
                                target.toString(),
                                0,
                                0,
                                List.of(),
                                errors));
                        checkpointManifest(sender, outputDirectory, manifest, options);
                    }
                }
            }
            Path manifestPath = manifestWriter.write(outputDirectory, manifest);
            sender.addChatMessage(
                new ChatComponentText(
                    "GameScene export complete: " + successCount
                        + " succeeded, "
                        + failureCount
                        + " failed. Manifest: "
                        + manifestPath));
        } catch (Throwable t) {
            sender.addChatMessage(new ChatComponentText("GameScene export failed: " + resolveErrorMessage(t)));
        }
    }

    private List<GameSceneExportTaskSpec> discoverTasks(GameSceneExportOptions options) {
        ArrayList<GameSceneExportTaskSpec> tasks = new ArrayList<>();
        for (MutableGuide guide : GuideRegistry.getAll()) {
            for (ParsedGuidePage parsedPage : guide.getPages()) {
                GuidePage page = guide.getPage(parsedPage.getId());
                if (page == null) {
                    continue;
                }
                List<LytGuidebookScene> scenes = page.scenes();
                for (int index = 0; index < scenes.size(); index++) {
                    tasks.add(
                        new GameSceneExportTaskSpec(
                            guide.getId(),
                            page.id(),
                            page.sourcePack(),
                            index,
                            scenes.get(index),
                            options.getLayerExpression(),
                            options.isLayersEach(),
                            options.getView(),
                            options.getBackground(),
                            options.getPixelsPerBlock(),
                            options.getScale(),
                            options.isShowAnnotations(),
                            options.isShowGrid()));
                }
            }
        }
        return tasks;
    }

    private ExportRenderPlan planRender(GameSceneExportTaskSpec task) throws CommandException {
        LytGuidebookScene scene = task.getScene();
        GuidebookLevel level = scene.getLevel();
        if (level == null) {
            throw new CommandException("GameScene has no level to render.");
        }
        if (level.isEmpty()) {
            return planEmptyRender(scene, task);
        }
        GuidebookSceneLayerSelection layers = resolveLayers(level, task);
        StructureLibSceneCameraFitter.FittedCamera fittedCamera = task.getView()
            .isExplicit() ? cameraFitter.fit(level, task.getPixelsPerBlock(), task.getScale(), task.getView())
                : fitSceneCamera(scene, task);
        List<InWorldAnnotation> annotations = scene
            .collectInWorldAnnotationsForExport(task.isShowAnnotations(), task.isShowGrid(), layers);
        List<OverlayAnnotation> overlays = task.isShowAnnotations() ? scene.collectOverlayAnnotationsForExport(layers)
            : List.of();
        return new ExportRenderPlan(
            fittedCamera.getCamera(),
            fittedCamera.getWidth(),
            fittedCamera.getHeight(),
            layers,
            annotations,
            overlays);
    }

    private ExportRenderPlan planEmptyRender(LytGuidebookScene scene, GameSceneExportTaskSpec task)
        throws CommandException {
        GuidebookSceneLayerSelection layers = GuidebookSceneLayerSelection.all();
        List<InWorldAnnotation> annotations = scene
            .collectInWorldAnnotationsForExport(task.isShowAnnotations(), false, layers);
        List<OverlayAnnotation> overlays = task.isShowAnnotations() ? scene.collectOverlayAnnotationsForExport(layers)
            : List.of();
        if (annotations.isEmpty() && overlays.isEmpty()) {
            throw new CommandException("GameScene has no blocks or annotations to render.");
        }
        if (task.getView()
            .isExplicit()) {
            throw new CommandException("GameScene has no blocks, so explicit fitted views cannot be used.");
        }
        StructureLibSceneCameraFitter.FittedCamera fittedCamera = fitSceneCamera(scene, task);
        return new ExportRenderPlan(
            fittedCamera.getCamera(),
            fittedCamera.getWidth(),
            fittedCamera.getHeight(),
            layers,
            annotations,
            overlays);
    }

    private StructureLibSceneCameraFitter.FittedCamera fitSceneCamera(LytGuidebookScene scene,
        GameSceneExportTaskSpec task) {
        CameraSettings source = scene.getCamera();
        CameraSettings camera = copyCamera(source);
        int width = Math.max(16, Math.round(scene.getSceneWidth() * task.getScale()));
        int height = Math.max(16, Math.round(scene.getSceneHeight() * task.getScale()));
        float zoom = source.getZoom() * task.getPixelsPerBlock()
            / (float) DEFAULT_GAME_SCENE_PIXELS_PER_BLOCK
            * task.getScale();
        camera.setZoom(zoom);
        camera.setViewportSize(width, height);
        return new StructureLibSceneCameraFitter.FittedCamera(camera, width, height);
    }

    private CameraSettings copyCamera(CameraSettings source) {
        CameraSettings camera = new CameraSettings();
        Vector3fc center = source.getRotationCenter();
        camera.setRotationCenter(center.x(), center.y(), center.z());
        camera.setRotationX(source.getRotationX());
        camera.setRotationY(source.getRotationY());
        camera.setRotationZ(source.getRotationZ());
        camera.setOffsetX(source.getOffsetX());
        camera.setOffsetY(source.getOffsetY());
        camera.setZoom(source.getZoom());
        return camera;
    }

    private GuidebookSceneLayerSelection resolveLayers(GuidebookLevel level, GameSceneExportTaskSpec task)
        throws CommandException {
        if (task.getExplicitLayer() != null) {
            return GuidebookSceneLayerSelection.eachLayer(task.getExplicitLayer());
        }
        if (task.isEachLayer() || "all".equalsIgnoreCase(task.getLayerExpression())) {
            return GuidebookSceneLayerSelection.all();
        }
        int[] bounds = level.getBounds();
        Set<Integer> visibleLayers = StructureLibNumericFilter.parse(task.getLayerExpression())
            .resolveWithin(bounds[1], bounds[4]);
        return GuidebookSceneLayerSelection.filtered(visibleLayers);
    }

    private List<GameSceneExportTaskSpec> expandRenderTasks(GameSceneExportTaskSpec task) {
        ArrayList<GameSceneExportTaskSpec> tasks = new ArrayList<>();
        if (!task.isEachLayer()) {
            tasks.add(task);
            return tasks;
        }
        for (Integer layer : layerResolver.resolveActualLayers(
            task.getScene()
                .getLevel())) {
            tasks.add(task.forExplicitLayer(layer));
        }
        if (tasks.isEmpty()) {
            tasks.add(task);
        }
        return tasks;
    }

    private void checkpointManifest(ICommandSender sender, Path outputDirectory, GameSceneExportManifest manifest,
        GameSceneExportOptions options) throws Exception {
        int entries = manifest.getEntries()
            .size();
        if (entries > 0 && entries % options.getBatchSize() == 0) {
            Path manifestPath = manifestWriter.write(outputDirectory, manifest);
            sender.addChatMessage(
                new ChatComponentText(
                    "GameScene export checkpoint: " + entries + " result(s). Manifest: " + manifestPath));
            System.gc();
        }
    }

    private Path resolveOutputDirectory(GameSceneExportOptions options) {
        if (options.getOutDir() != null) {
            return options.getOutDir()
                .toAbsolutePath();
        }
        return Paths.get("screenshots", "gameScene", StructureExportTimestamp.OUTPUT_FORMAT.format(LocalDateTime.now()))
            .toAbsolutePath();
    }

    private static String resolveErrorMessage(Throwable throwable) {
        String message = throwable.getMessage();
        if (message == null || message.trim()
            .isEmpty()) {
            return throwable.getClass()
                .getSimpleName();
        }
        return message;
    }

    public static class ExportRenderPlan {

        private final CameraSettings camera;
        private final int width;
        private final int height;
        private final GuidebookSceneLayerSelection layers;
        private final List<InWorldAnnotation> annotations;
        private final List<OverlayAnnotation> overlays;

        public ExportRenderPlan(CameraSettings camera, int width, int height, GuidebookSceneLayerSelection layers,
            List<InWorldAnnotation> annotations, List<OverlayAnnotation> overlays) {
            this.camera = camera;
            this.width = width;
            this.height = height;
            this.layers = layers;
            this.annotations = annotations != null ? annotations : List.of();
            this.overlays = overlays != null ? overlays : List.of();
        }
    }
}
