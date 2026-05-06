package com.hfstudio.guidenh.guide.scene.snapshot;

import org.jetbrains.annotations.Nullable;

public final class RegisteredServerPreviewSupplement {

    private final ServerPreviewSupplementSnippetCodec snippetCodec;
    @Nullable
    private final ServerPreviewSupplementFetchContributor fetchContributor;

    public RegisteredServerPreviewSupplement(ServerPreviewSupplementSnippetCodec snippetCodec,
        @Nullable ServerPreviewSupplementFetchContributor fetchContributor) {
        this.snippetCodec = snippetCodec;
        this.fetchContributor = fetchContributor;
    }

    public ServerPreviewSupplementSnippetCodec snippetCodec() {
        return snippetCodec;
    }

    @Nullable
    public ServerPreviewSupplementFetchContributor fetchContributor() {
        return fetchContributor;
    }
}
