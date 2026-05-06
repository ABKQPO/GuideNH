package com.hfstudio.guidenh.guide.scene.snapshot;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

import org.jetbrains.annotations.Nullable;

public final class ServerPreviewSupplementRegistry {

    private static final List<RegisteredServerPreviewSupplement> ENTRIES = new CopyOnWriteArrayList<>();

    private static final Comparator<RegisteredServerPreviewSupplement> ENTRY_ORDER =
        Comparator.comparingInt((RegisteredServerPreviewSupplement e) -> e.snippetCodec()
            .priority())
            .thenComparing(e -> e.snippetCodec()
                .supplementId());

    private ServerPreviewSupplementRegistry() {}

    public static void registerSnippet(ServerPreviewSupplementSnippetCodec snippet) {
        register(new RegisteredServerPreviewSupplement(snippet, null));
    }

    public static void registerSnippetAndFetch(ServerPreviewSupplementSnippetCodec snippet,
        ServerPreviewSupplementFetchContributor fetch) {
        if (!Objects.equals(snippet.supplementId(), fetch.supplementId())) {
            throw new IllegalArgumentException("snippet id != fetch id");
        }
        register(new RegisteredServerPreviewSupplement(snippet, fetch));
    }

    private static void register(RegisteredServerPreviewSupplement entry) {
        ENTRIES.add(entry);
    }

    public static List<RegisteredServerPreviewSupplement> entriesInOrder() {
        ArrayList<RegisteredServerPreviewSupplement> sorted = new ArrayList<>(ENTRIES);
        sorted.sort(ENTRY_ORDER);
        return sorted;
    }

    @Nullable
    public static RegisteredServerPreviewSupplement findBySupplementId(String id) {
        for (RegisteredServerPreviewSupplement e : ENTRIES) {
            if (id.equals(e.snippetCodec().supplementId())) {
                return e;
            }
        }
        return null;
    }
}
