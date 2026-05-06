package com.hfstudio.guidenh.guide.scene.snapshot;

/**
 * Applies structure sidecars into {@link com.hfstudio.guidenh.guide.scene.level.GuidebookLevel} after block placement.
 */
public interface StructureImportContributor {

    int priority();

    void apply(ImportBlockContext ctx);
}
