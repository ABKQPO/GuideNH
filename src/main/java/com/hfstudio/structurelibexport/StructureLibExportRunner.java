package com.hfstudio.structurelibexport;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import net.minecraft.command.ICommandSender;
import net.minecraft.util.ChatComponentText;

import com.hfstudio.guidenh.guide.scene.GuidebookSceneLayerSelection;
import com.hfstudio.guidenh.guide.scene.level.GuidebookLevel;
import com.hfstudio.guidenh.integration.structurelib.StructureLibRuntimeFacade;
import com.hfstudio.guidenh.integration.structurelib.StructureLibRuntimeFacade.BuildContext;

public class StructureLibExportRunner {

    private final StructureLibControllerDiscovery controllerDiscovery;
    private final StructureLibExportPlanner planner;
    private final StructureLibSceneBuilder sceneBuilder;
    private final StructureLibLayerResolver layerResolver;
    private final StructureLibSceneCameraFitter cameraFitter;
    private final StructureLibSceneImageExporter imageExporter;
    private final StructureLibExportManifestWriter manifestWriter;

    public StructureLibExportRunner() {
        this(
            new StructureLibControllerDiscovery(),
            new StructureLibExportPlanner(),
            new StructureLibSceneBuilder(),
            new StructureLibLayerResolver(),
            new StructureLibSceneCameraFitter(),
            new StructureLibSceneImageExporter(),
            new StructureLibExportManifestWriter());
    }

    public StructureLibExportRunner(StructureLibControllerDiscovery controllerDiscovery,
        StructureLibExportPlanner planner, StructureLibSceneBuilder sceneBuilder,
        StructureLibLayerResolver layerResolver, StructureLibSceneCameraFitter cameraFitter,
        StructureLibSceneImageExporter imageExporter, StructureLibExportManifestWriter manifestWriter) {
        this.controllerDiscovery = controllerDiscovery;
        this.planner = planner;
        this.sceneBuilder = sceneBuilder;
        this.layerResolver = layerResolver;
        this.cameraFitter = cameraFitter;
        this.imageExporter = imageExporter;
        this.manifestWriter = manifestWriter;
    }

    public void run(ICommandSender sender, StructureLibExportOptions options) {
        try {
            Path outputDirectory = resolveOutputDirectory(options);
            Files.createDirectories(outputDirectory);
            List<StructureLibControllerSpec> controllers = controllerDiscovery.resolveControllers(options);
            StructureLibExportManifest manifest = new StructureLibExportManifest();
            StructureLibExportFileNamer fileNamer = new StructureLibExportFileNamer();
            BuildContext buildContext = new BuildContext();
            GuidebookLevel renderLevel = new GuidebookLevel();
            int successCount = 0;
            int failureCount = 0;
            int renderedTaskCount = 0;
            sender.addChatMessage(
                new ChatComponentText(
                    "StructureLib export started: " + controllers.size() + " controller(s) discovered."));
            try {
                for (StructureLibControllerSpec controller : controllers) {
                    List<StructureLibExportTaskSpec> tasks = planController(controller, options);
                    for (StructureLibExportTaskSpec task : tasks) {
                        StructureLibSceneBuildResult buildResult = sceneBuilder.build(task, buildContext, renderLevel);
                        if (!buildResult.isSuccess()) {
                            failureCount++;
                            manifest.add(
                                new StructureLibExportManifest.Entry(
                                    false,
                                    task,
                                    null,
                                    0,
                                    0,
                                    buildResult.getWarnings(),
                                    buildResult.getErrors()));
                            checkpointManifest(sender, outputDirectory, manifest, options, buildContext);
                            continue;
                        }
                        List<StructureLibExportTaskSpec> renderTasks = expandRenderTasks(task, buildResult);
                        for (StructureLibExportTaskSpec renderTask : renderTasks) {
                            renderedTaskCount++;
                            if (renderedTaskCount > StructureLibExportPlanner.DEFAULT_MAX_TASKS && !options.isForce()) {
                                throw new IllegalStateException(
                                    "StructureLib export expanded beyond " + StructureLibExportPlanner.DEFAULT_MAX_TASKS
                                        + " screenshots. Use --force to allow this.");
                            }
                            Path target = fileNamer.resolve(outputDirectory, renderTask);
                            renderTask.setOutputPath(target);
                            try {
                                GuidebookSceneLayerSelection layers = layerResolver
                                    .resolve(buildResult.getLevel(), renderTask);
                                StructureLibSceneCameraFitter.FittedCamera fittedCamera = cameraFitter
                                    .fit(buildResult.getLevel(), renderTask);
                                if (options.isDryRun()) {
                                    successCount++;
                                    manifest.add(
                                        new StructureLibExportManifest.Entry(
                                            true,
                                            renderTask,
                                            target.toString(),
                                            fittedCamera.width(),
                                            fittedCamera.height(),
                                            buildResult.getWarnings(),
                                            new ArrayList<>()));
                                    checkpointManifest(sender, outputDirectory, manifest, options, buildContext);
                                    continue;
                                }
                                StructureLibSceneImageExporter.ExportedImage image = imageExporter.export(
                                    buildResult.getLevel(),
                                    fittedCamera.camera(),
                                    layers,
                                    renderTask.getBackground(),
                                    target,
                                    fittedCamera.width(),
                                    fittedCamera.height(),
                                    options.getMaxPixels());
                                successCount++;
                                manifest.add(
                                    new StructureLibExportManifest.Entry(
                                        true,
                                        renderTask,
                                        image.path()
                                            .toString(),
                                        image.width(),
                                        image.height(),
                                        buildResult.getWarnings(),
                                        new ArrayList<>()));
                                checkpointManifest(sender, outputDirectory, manifest, options, buildContext);
                            } catch (Throwable t) {
                                failureCount++;
                                ArrayList<String> errors = new ArrayList<>();
                                errors.add(resolveErrorMessage(t));
                                manifest.add(
                                    new StructureLibExportManifest.Entry(
                                        false,
                                        renderTask,
                                        target.toString(),
                                        0,
                                        0,
                                        buildResult.getWarnings(),
                                        errors));
                                checkpointManifest(sender, outputDirectory, manifest, options, buildContext);
                            }
                        }
                        renderLevel.clear();
                    }
                    releaseControllerMemory(buildContext);
                }
            } finally {
                renderLevel.clear();
                buildContext.clear();
            }
            Path manifestPath = manifestWriter.write(outputDirectory, manifest);
            sender.addChatMessage(
                new ChatComponentText(
                    "StructureLib export complete: " + successCount
                        + " succeeded, "
                        + failureCount
                        + " failed. Manifest: "
                        + manifestPath));
        } catch (Throwable t) {
            sender.addChatMessage(new ChatComponentText("StructureLib export failed: " + resolveErrorMessage(t)));
        }
    }

    private List<StructureLibExportTaskSpec> planController(StructureLibControllerSpec controller,
        StructureLibExportOptions options) {
        try {
            return planner.plan(List.of(controller), options);
        } catch (Exception e) {
            if (options.getController() != null) {
                throw e;
            }
            return List.of();
        }
    }

    private void checkpointManifest(ICommandSender sender, Path outputDirectory, StructureLibExportManifest manifest,
        StructureLibExportOptions options, BuildContext buildContext) throws Exception {
        int entries = manifest.getEntries()
            .size();
        if (entries > 0 && entries % options.getBatchSize() == 0) {
            Path manifestPath = manifestWriter.write(outputDirectory, manifest);
            sender.addChatMessage(
                new ChatComponentText(
                    "StructureLib export checkpoint: " + entries + " result(s). Manifest: " + manifestPath));
            releaseControllerMemory(buildContext);
        }
    }

    private void releaseControllerMemory(BuildContext buildContext) {
        if (buildContext != null) {
            buildContext.clear();
        }
        StructureLibRuntimeFacade.CONTROL_ANALYSIS_CACHE.clear();
        System.gc();
    }

    private List<StructureLibExportTaskSpec> expandRenderTasks(StructureLibExportTaskSpec task,
        StructureLibSceneBuildResult buildResult) {
        ArrayList<StructureLibExportTaskSpec> tasks = new ArrayList<>();
        if (!task.isEachLayer()) {
            tasks.add(task);
            return tasks;
        }
        for (Integer layer : layerResolver.resolveActualLayers(buildResult.getLevel())) {
            tasks.add(task.forExplicitLayer(layer));
        }
        if (tasks.isEmpty()) {
            tasks.add(task);
        }
        return tasks;
    }

    private Path resolveOutputDirectory(StructureLibExportOptions options) {
        if (options.getOutDir() != null) {
            return options.getOutDir()
                .toAbsolutePath();
        }
        return Paths
            .get("screenshots", "structurelib", StructureExportTimestamp.OUTPUT_FORMAT.format(LocalDateTime.now()))
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
}
