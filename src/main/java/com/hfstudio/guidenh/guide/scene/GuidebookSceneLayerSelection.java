package com.hfstudio.guidenh.guide.scene;

import java.util.LinkedHashSet;
import java.util.Set;

import org.jetbrains.annotations.Nullable;

public class GuidebookSceneLayerSelection {

    public enum Mode {
        ALL,
        FILTERED,
        EACH
    }

    private final Mode mode;
    private final Set<Integer> visibleLayers;
    @Nullable
    private final Integer singleExportLayer;

    public GuidebookSceneLayerSelection(Mode mode, Set<Integer> visibleLayers, @Nullable Integer singleExportLayer) {
        this.mode = mode != null ? mode : Mode.ALL;
        this.visibleLayers = immutableCopy(visibleLayers);
        this.singleExportLayer = singleExportLayer;
    }

    public static GuidebookSceneLayerSelection all() {
        return new GuidebookSceneLayerSelection(Mode.ALL, Set.of(), null);
    }

    public static GuidebookSceneLayerSelection filtered(Set<Integer> visibleLayers) {
        return new GuidebookSceneLayerSelection(Mode.FILTERED, visibleLayers, null);
    }

    public static GuidebookSceneLayerSelection eachLayer(int layer) {
        return new GuidebookSceneLayerSelection(Mode.EACH, Set.of(layer), layer);
    }

    public static GuidebookSceneLayerSelection fromVisibleLayer(@Nullable Integer visibleLayerY) {
        return visibleLayerY == null ? all() : eachLayer(visibleLayerY);
    }

    public boolean isLayerVisible(int y) {
        return mode == Mode.ALL || visibleLayers.contains(y);
    }

    public boolean shouldRenderAllFaces() {
        return mode != Mode.ALL;
    }

    public Mode getMode() {
        return mode;
    }

    public Set<Integer> getVisibleLayers() {
        return visibleLayers;
    }

    @Nullable
    public Integer getSingleExportLayer() {
        return singleExportLayer;
    }

    private static Set<Integer> immutableCopy(Set<Integer> source) {
        if (source == null || source.isEmpty()) {
            return Set.of();
        }
        return Set.copyOf(new LinkedHashSet<>(source));
    }
}
