package com.hfstudio.structurelibexport;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

public class StructureLibExportOptions {

    @Nullable
    private final String controller;
    @Nullable
    private final Path outDir;
    private final int pixelsPerBlock;
    private final float scale;
    private final List<Integer> tiers;
    private final Map<String, List<Integer>> channels;
    private final boolean tierExplicit;
    private final boolean channelsExplicit;
    private final String layerExpression;
    private final boolean layersEach;
    private final List<StructureLibOrientationSpec> orientations;
    private final StructureLibExportView view;
    private final StructureLibExportBackground background;
    private final long maxPixels;
    private final int batchSize;
    private final boolean gtActiveController;
    private final boolean gtPlaceHatches;
    private final boolean force;
    private final boolean dryRun;

    public StructureLibExportOptions(@Nullable String controller, @Nullable Path outDir, int pixelsPerBlock,
        float scale, List<Integer> tiers, Map<String, List<Integer>> channels, String layerExpression,
        boolean layersEach, List<StructureLibOrientationSpec> orientations, StructureLibExportView view,
        StructureLibExportBackground background, long maxPixels, int batchSize, boolean force, boolean dryRun) {
        this(
            controller,
            outDir,
            pixelsPerBlock,
            scale,
            tiers,
            channels,
            false,
            channels != null && !channels.isEmpty(),
            layerExpression,
            layersEach,
            orientations,
            view,
            background,
            maxPixels,
            batchSize,
            false,
            false,
            force,
            dryRun);
    }

    public StructureLibExportOptions(@Nullable String controller, @Nullable Path outDir, int pixelsPerBlock,
        float scale, List<Integer> tiers, Map<String, List<Integer>> channels, boolean tierExplicit,
        boolean channelsExplicit, String layerExpression, boolean layersEach,
        List<StructureLibOrientationSpec> orientations, StructureLibExportView view,
        StructureLibExportBackground background, long maxPixels, int batchSize, boolean gtActiveController,
        boolean gtPlaceHatches, boolean force, boolean dryRun) {
        this.controller = normalize(controller);
        this.outDir = outDir;
        this.pixelsPerBlock = Math.max(1, pixelsPerBlock);
        this.scale = scale > 0f ? scale : 1f;
        this.tiers = immutableValues(tiers, 1);
        this.channels = immutableChannels(channels);
        this.tierExplicit = tierExplicit;
        this.channelsExplicit = channelsExplicit && !this.channels.isEmpty();
        this.layerExpression = normalize(layerExpression) != null ? normalize(layerExpression) : "all";
        this.layersEach = layersEach;
        this.orientations = immutableOrientations(orientations);
        this.view = view != null ? view : StructureLibExportView.defaultView();
        this.background = background != null ? background : StructureLibExportBackground.transparent();
        this.maxPixels = maxPixels == -1L ? -1L : Math.max(1L, maxPixels);
        this.batchSize = Math.max(1, batchSize);
        this.gtActiveController = gtActiveController;
        this.gtPlaceHatches = gtPlaceHatches;
        this.force = force;
        this.dryRun = dryRun;
    }

    @Nullable
    public String getController() {
        return controller;
    }

    @Nullable
    public Path getOutDir() {
        return outDir;
    }

    public int getPixelsPerBlock() {
        return pixelsPerBlock;
    }

    public float getScale() {
        return scale;
    }

    public List<Integer> getTiers() {
        return tiers;
    }

    public Map<String, List<Integer>> getChannels() {
        return channels;
    }

    public boolean isTierExplicit() {
        return tierExplicit;
    }

    public boolean isChannelsExplicit() {
        return channelsExplicit;
    }

    public String getLayerExpression() {
        return layerExpression;
    }

    public boolean isLayersEach() {
        return layersEach;
    }

    public List<StructureLibOrientationSpec> getOrientations() {
        return orientations;
    }

    public StructureLibExportView getView() {
        return view;
    }

    public StructureLibExportBackground getBackground() {
        return background;
    }

    public long getMaxPixels() {
        return maxPixels;
    }

    public int getBatchSize() {
        return batchSize;
    }

    public boolean isGtActiveController() {
        return gtActiveController;
    }

    public boolean isGtPlaceHatches() {
        return gtPlaceHatches;
    }

    public boolean isForce() {
        return force;
    }

    public boolean isDryRun() {
        return dryRun;
    }

    @Nullable
    private static String normalize(@Nullable String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static List<Integer> immutableValues(List<Integer> values, int fallback) {
        var copy = new ArrayList<Integer>();
        if (values != null) {
            for (Integer value : values) {
                if (value != null) {
                    copy.add(value);
                }
            }
        }
        if (copy.isEmpty()) {
            copy.add(fallback);
        }
        return List.copyOf(copy);
    }

    private static Map<String, List<Integer>> immutableChannels(Map<String, List<Integer>> channels) {
        var copy = new LinkedHashMap<String, List<Integer>>();
        if (channels != null) {
            for (Map.Entry<String, List<Integer>> entry : channels.entrySet()) {
                String key = normalize(entry.getKey());
                if (key != null) {
                    copy.put(key, immutableValues(entry.getValue(), 1));
                }
            }
        }
        return copy.isEmpty() ? Map.of() : Map.copyOf(copy);
    }

    private static List<StructureLibOrientationSpec> immutableOrientations(List<StructureLibOrientationSpec> values) {
        var copy = new ArrayList<StructureLibOrientationSpec>();
        if (values != null) {
            for (StructureLibOrientationSpec value : values) {
                if (value != null) {
                    copy.add(value);
                }
            }
        }
        if (copy.isEmpty()) {
            copy.add(StructureLibOrientationSpec.DEFAULT);
        }
        return List.copyOf(copy);
    }
}
