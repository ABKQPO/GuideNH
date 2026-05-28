package com.hfstudio.structurelibexport;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

public class StructureLibExportManifest {

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
        private final String controller;
        private final int meta;
        private final String displayName;
        private final int tier;
        private final Map<String, Integer> channels;
        private final String layers;
        @Nullable
        private final Integer explicitLayer;
        private final String orientation;
        private final String view;
        @Nullable
        private final String path;
        private final int width;
        private final int height;
        private final List<String> warnings;
        private final List<String> errors;

        public Entry(boolean success, StructureLibExportTaskSpec task, @Nullable String path, int width, int height,
            List<String> warnings, List<String> errors) {
            this.success = success;
            this.controller = task.getController()
                .getBlockId();
            this.meta = task.getController()
                .getMeta();
            this.displayName = task.getController()
                .getDisplayName();
            this.tier = task.getTier();
            this.channels = task.getChannels();
            this.layers = task.getLayerExpression();
            this.explicitLayer = task.getExplicitLayer();
            this.orientation = task.getOrientation()
                .asSuffix();
            this.view = task.getView()
                .getName();
            this.path = path;
            this.width = width;
            this.height = height;
            this.warnings = warnings != null ? List.copyOf(warnings) : List.of();
            this.errors = errors != null ? List.copyOf(errors) : List.of();
        }
    }
}
