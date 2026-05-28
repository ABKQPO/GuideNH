package com.hfstudio.structurelibexport;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.Nullable;

public class GameSceneExportManifest {

    private final List<Entry> entries = new ArrayList<>();

    public void add(Entry entry) {
        if (entry != null) {
            entries.add(entry);
        }
    }

    public List<Entry> getEntries() {
        return List.copyOf(entries);
    }

    public static class Entry {

        private final boolean success;
        private final String guide;
        private final String page;
        private final String sourcePack;
        private final int sceneIndex;
        private final String layers;
        @Nullable
        private final Integer explicitLayer;
        private final String view;
        private final boolean sceneCamera;
        private final boolean showAnnotations;
        private final boolean showGrid;
        @Nullable
        private final String path;
        private final int width;
        private final int height;
        private final List<String> warnings;
        private final List<String> errors;

        public Entry(boolean success, GameSceneExportTaskSpec task, @Nullable String path, int width, int height,
            List<String> warnings, List<String> errors) {
            this.success = success;
            this.guide = task.getGuideId()
                .toString();
            this.page = task.getPageId()
                .toString();
            this.sourcePack = task.getSourcePack();
            this.sceneIndex = task.getSceneIndex();
            this.layers = task.getLayerExpression();
            this.explicitLayer = task.getExplicitLayer();
            this.view = task.getView()
                .getName();
            this.sceneCamera = !task.getView()
                .isExplicit();
            this.showAnnotations = task.isShowAnnotations();
            this.showGrid = task.isShowGrid();
            this.path = path;
            this.width = width;
            this.height = height;
            this.warnings = warnings != null ? List.copyOf(warnings) : List.of();
            this.errors = errors != null ? List.copyOf(errors) : List.of();
        }
    }
}
