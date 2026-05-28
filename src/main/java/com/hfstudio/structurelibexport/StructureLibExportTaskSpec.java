package com.hfstudio.structurelibexport;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

public class StructureLibExportTaskSpec {

    private final StructureLibControllerSpec controller;
    private final StructureLibOrientationSpec orientation;
    private final int tier;
    private final Map<String, Integer> channels;
    private final String layerExpression;
    private final boolean eachLayer;
    @Nullable
    private final Integer explicitLayer;
    private final StructureLibExportView view;
    private final StructureLibExportBackground background;
    private final int pixelsPerBlock;
    private final float scale;
    private final boolean gtActiveController;
    private final boolean gtPlaceHatches;
    private final List<String> warnings;
    @Nullable
    private Path outputPath;

    public StructureLibExportTaskSpec(StructureLibControllerSpec controller, StructureLibOrientationSpec orientation,
        int tier, Map<String, Integer> channels, String layerExpression, boolean eachLayer, StructureLibExportView view,
        StructureLibExportBackground background, int pixelsPerBlock, float scale, boolean gtActiveController,
        boolean gtPlaceHatches, List<String> warnings) {
        this(
            controller,
            orientation,
            tier,
            channels,
            layerExpression,
            eachLayer,
            null,
            view,
            background,
            pixelsPerBlock,
            scale,
            gtActiveController,
            gtPlaceHatches,
            warnings);
    }

    public StructureLibExportTaskSpec(StructureLibControllerSpec controller, StructureLibOrientationSpec orientation,
        int tier, Map<String, Integer> channels, String layerExpression, boolean eachLayer,
        @Nullable Integer explicitLayer, StructureLibExportView view, StructureLibExportBackground background,
        int pixelsPerBlock, float scale, boolean gtActiveController, boolean gtPlaceHatches, List<String> warnings) {
        this.controller = controller;
        this.orientation = orientation != null ? orientation : StructureLibOrientationSpec.DEFAULT;
        this.tier = Math.max(1, tier);
        this.channels = immutableChannels(channels);
        this.layerExpression = layerExpression != null ? layerExpression : "all";
        this.eachLayer = eachLayer;
        this.explicitLayer = explicitLayer;
        this.view = view != null ? view : StructureLibExportView.defaultView();
        this.background = background != null ? background : StructureLibExportBackground.transparent();
        this.pixelsPerBlock = Math.max(1, pixelsPerBlock);
        this.scale = scale > 0f ? scale : 1f;
        this.gtActiveController = gtActiveController;
        this.gtPlaceHatches = gtPlaceHatches;
        this.warnings = warnings != null ? List.copyOf(warnings) : List.of();
    }

    public StructureLibControllerSpec getController() {
        return controller;
    }

    public StructureLibOrientationSpec getOrientation() {
        return orientation;
    }

    public int getTier() {
        return tier;
    }

    public Map<String, Integer> getChannels() {
        return channels;
    }

    public String getLayerExpression() {
        return layerExpression;
    }

    public boolean isEachLayer() {
        return eachLayer;
    }

    @Nullable
    public Integer getExplicitLayer() {
        return explicitLayer;
    }

    public StructureLibExportTaskSpec forExplicitLayer(int layer) {
        return new StructureLibExportTaskSpec(
            controller,
            orientation,
            tier,
            channels,
            String.valueOf(layer),
            false,
            layer,
            view,
            background,
            pixelsPerBlock,
            scale,
            gtActiveController,
            gtPlaceHatches,
            warnings);
    }

    public StructureLibExportView getView() {
        return view;
    }

    public StructureLibExportBackground getBackground() {
        return background;
    }

    public int getPixelsPerBlock() {
        return pixelsPerBlock;
    }

    public float getScale() {
        return scale;
    }

    public boolean isGtActiveController() {
        return gtActiveController;
    }

    public boolean isGtPlaceHatches() {
        return gtPlaceHatches;
    }

    public List<String> getWarnings() {
        return warnings;
    }

    @Nullable
    public Path getOutputPath() {
        return outputPath;
    }

    public void setOutputPath(Path outputPath) {
        this.outputPath = outputPath;
    }

    private static Map<String, Integer> immutableChannels(Map<String, Integer> source) {
        if (source == null || source.isEmpty()) {
            return Map.of();
        }
        return Map.copyOf(new LinkedHashMap<>(source));
    }
}
