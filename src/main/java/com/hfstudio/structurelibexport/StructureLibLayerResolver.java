package com.hfstudio.structurelibexport;

import java.util.LinkedHashSet;
import java.util.Set;

import net.minecraft.command.CommandException;

import com.hfstudio.guidenh.guide.scene.GuidebookSceneLayerSelection;
import com.hfstudio.guidenh.guide.scene.level.GuidebookLevel;

public class StructureLibLayerResolver {

    public GuidebookSceneLayerSelection resolve(GuidebookLevel level, StructureLibExportTaskSpec task)
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
        return visibleLayers.isEmpty() ? GuidebookSceneLayerSelection.filtered(new LinkedHashSet<>())
            : GuidebookSceneLayerSelection.filtered(visibleLayers);
    }

    public Set<Integer> resolveActualLayers(GuidebookLevel level) {
        LinkedHashSet<Integer> layers = new LinkedHashSet<>();
        if (level == null || level.isEmpty()) {
            return layers;
        }
        int[] bounds = level.getBounds();
        for (int y = bounds[1]; y <= bounds[4]; y++) {
            layers.add(y);
        }
        return layers;
    }
}
