package com.hfstudio.structurelibexport;

import java.nio.file.Path;

import org.jetbrains.annotations.Nullable;

import lombok.Getter;

@Getter
public class GameSceneExportOptions {

    @Nullable
    private final Path outDir;
    private final int pixelsPerBlock;
    private final float scale;
    private final String layerExpression;
    private final boolean layersEach;
    private final StructureLibExportView view;
    private final StructureLibExportBackground background;
    private final long maxPixels;
    private final int batchSize;
    private final boolean showAnnotations;
    private final boolean showGrid;
    private final boolean force;
    private final boolean dryRun;

    public GameSceneExportOptions(@Nullable Path outDir, int pixelsPerBlock, float scale, String layerExpression,
        boolean layersEach, StructureLibExportView view, StructureLibExportBackground background, long maxPixels,
        int batchSize, boolean showAnnotations, boolean showGrid, boolean force, boolean dryRun) {
        this.outDir = outDir;
        this.pixelsPerBlock = Math.max(1, pixelsPerBlock);
        this.scale = scale > 0f ? scale : 1f;
        this.layerExpression = normalize(layerExpression) != null ? normalize(layerExpression) : "all";
        this.layersEach = layersEach;
        this.view = view != null ? view : StructureLibExportView.defaultView();
        this.background = background != null ? background : StructureLibExportBackground.transparent();
        this.maxPixels = maxPixels == -1L ? -1L : Math.max(1L, maxPixels);
        this.batchSize = Math.max(1, batchSize);
        this.showAnnotations = showAnnotations;
        this.showGrid = showGrid;
        this.force = force;
        this.dryRun = dryRun;
    }

    @Nullable
    private static String normalize(@Nullable String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
