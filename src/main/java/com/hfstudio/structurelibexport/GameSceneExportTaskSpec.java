package com.hfstudio.structurelibexport;

import java.nio.file.Path;

import net.minecraft.util.ResourceLocation;

import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.guide.scene.LytGuidebookScene;

import lombok.Getter;
import lombok.Setter;

@Getter
public class GameSceneExportTaskSpec {

    private final ResourceLocation guideId;
    private final ResourceLocation pageId;
    private final String sourcePack;
    private final int sceneIndex;
    private final LytGuidebookScene scene;
    private final String layerExpression;
    private final boolean eachLayer;
    @Nullable
    private final Integer explicitLayer;
    private final StructureLibExportView view;
    private final StructureLibExportBackground background;
    private final int pixelsPerBlock;
    private final float scale;
    private final boolean showAnnotations;
    private final boolean showGrid;
    @Nullable
    @Setter
    private Path outputPath;

    public GameSceneExportTaskSpec(ResourceLocation guideId, ResourceLocation pageId, String sourcePack, int sceneIndex,
        LytGuidebookScene scene, String layerExpression, boolean eachLayer, StructureLibExportView view,
        StructureLibExportBackground background, int pixelsPerBlock, float scale, boolean showAnnotations,
        boolean showGrid) {
        this(
            guideId,
            pageId,
            sourcePack,
            sceneIndex,
            scene,
            layerExpression,
            eachLayer,
            null,
            view,
            background,
            pixelsPerBlock,
            scale,
            showAnnotations,
            showGrid);
    }

    public GameSceneExportTaskSpec(ResourceLocation guideId, ResourceLocation pageId, String sourcePack, int sceneIndex,
        LytGuidebookScene scene, String layerExpression, boolean eachLayer, @Nullable Integer explicitLayer,
        StructureLibExportView view, StructureLibExportBackground background, int pixelsPerBlock, float scale,
        boolean showAnnotations, boolean showGrid) {
        this.guideId = guideId;
        this.pageId = pageId;
        this.sourcePack = sourcePack != null ? sourcePack : "";
        this.sceneIndex = Math.max(0, sceneIndex);
        this.scene = scene;
        this.layerExpression = layerExpression != null ? layerExpression : "all";
        this.eachLayer = eachLayer;
        this.explicitLayer = explicitLayer;
        this.view = view != null ? view : StructureLibExportView.defaultView();
        this.background = background != null ? background : StructureLibExportBackground.transparent();
        this.pixelsPerBlock = Math.max(1, pixelsPerBlock);
        this.scale = scale > 0f ? scale : 1f;
        this.showAnnotations = showAnnotations;
        this.showGrid = showGrid;
    }

    public GameSceneExportTaskSpec forExplicitLayer(int layer) {
        return new GameSceneExportTaskSpec(
            guideId,
            pageId,
            sourcePack,
            sceneIndex,
            scene,
            String.valueOf(layer),
            false,
            layer,
            view,
            background,
            pixelsPerBlock,
            scale,
            showAnnotations,
            showGrid);
    }

}
