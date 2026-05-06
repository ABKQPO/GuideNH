package com.hfstudio.guidenh.guide.scene.snapshot;

/**
 * Augments each {@code blocks[]} compound during structure export (sidecars, RPC-backed snapshots, etc.).
 */
public interface StructureExportContributor {

    /** Lower runs first among contributors. */
    int priority();

    default void beginExport(ExportSession session) {}

    void contributeBlock(ExportBlockContext ctx);

    default void endExport(ExportSession session) {}
}
