package com.hfstudio.guidenh.guide.scene.snapshot;

/** Fans out structure import hooks to registered server-preview supplements. */
public final class ServerPreviewSupplementStructureImportContributor implements StructureImportContributor {

    @Override
    public int priority() {
        return 5;
    }

    @Override
    public void apply(ImportBlockContext ctx) {
        for (RegisteredServerPreviewSupplement e : ServerPreviewSupplementRegistry.entriesInOrder()) {
            e.snippetCodec()
                .decodeBlock(ctx);
        }
    }
}
