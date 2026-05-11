package com.hfstudio.structurelibexport;

import java.nio.file.Path;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public class GameSceneExportFileNamer {

    private final Set<String> usedNames = new HashSet<>();

    public Path resolve(Path outputDirectory, GameSceneExportTaskSpec task) {
        String baseName = StructureLibExportFileNamer.constrainBaseName(buildBaseName(task));
        String candidate = baseName + ".png";
        int collision = 2;
        while (!usedNames.add(candidate.toLowerCase(Locale.ROOT))) {
            candidate = baseName + "_" + collision + ".png";
            collision++;
        }
        return outputDirectory.resolve(candidate);
    }

    private String buildBaseName(GameSceneExportTaskSpec task) {
        StringBuilder builder = new StringBuilder();
        builder.append(task.getGuideId())
            .append("_")
            .append(task.getPageId())
            .append("_scene-")
            .append(task.getSceneIndex() + 1);
        if (task.getExplicitLayer() != null) {
            builder.append("_layer-")
                .append(task.getExplicitLayer());
        } else if (!"all".equalsIgnoreCase(task.getLayerExpression())) {
            builder.append("_layers-")
                .append(task.getLayerExpression());
        }
        if (task.getView()
            .isExplicit()) {
            builder.append("_")
                .append(
                    task.getView()
                        .getName());
        } else {
            builder.append("_scene-camera");
        }
        if (task.isShowAnnotations()) {
            builder.append("_annotations");
        }
        if (task.isShowGrid()) {
            builder.append("_grid");
        }
        return builder.toString();
    }
}
