package com.hfstudio.guidenh.guide.scene.snapshot;

/** Fans out structure export hooks to registered server-preview supplements. */
public final class ServerPreviewSupplementStructureExportContributor implements StructureExportContributor {

    @Override
    public int priority() {
        return 5;
    }

    @Override
    public void beginExport(ExportSession session) {
        for (RegisteredServerPreviewSupplement e : ServerPreviewSupplementRegistry.entriesInOrder()) {
            ServerPreviewSupplementFetchContributor fetch = e.fetchContributor();
            if (fetch != null) {
                fetch.beginExport(session);
            }
        }
    }

    @Override
    public void contributeBlock(ExportBlockContext ctx) {
        for (RegisteredServerPreviewSupplement e : ServerPreviewSupplementRegistry.entriesInOrder()) {
            e.snippetCodec()
                .encodeBlock(ctx, ctx.structureBlockTag());
        }
    }
}
