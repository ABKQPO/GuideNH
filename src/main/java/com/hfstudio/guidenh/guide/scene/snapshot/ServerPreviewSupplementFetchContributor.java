package com.hfstudio.guidenh.guide.scene.snapshot;

/**
 * Optional batch server fetch before per-block structure export (e.g. multiplayer RPC).
 */
public interface ServerPreviewSupplementFetchContributor {

    /** Same ordering key as snippet codecs sharing this supplement. */
    String supplementId();

    default int priority() {
        return 10;
    }

    void beginExport(ExportSession session);
}
