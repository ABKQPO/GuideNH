package com.hfstudio.guidenh.guide.scene.snapshot;

import com.hfstudio.guidenh.guide.scene.level.GuidebookLevel;

/**
 * Applies structure sidecars into {@link GuidebookLevel} after block placement.
 */
public interface StructureImportContributor {

    int priority();

    void apply(ImportBlockContext ctx);
}
