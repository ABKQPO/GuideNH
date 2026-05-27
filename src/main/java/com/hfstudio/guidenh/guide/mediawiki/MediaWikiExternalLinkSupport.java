package com.hfstudio.guidenh.guide.mediawiki;

import java.net.URI;
import java.util.Locale;

import org.jetbrains.annotations.Nullable;

public class MediaWikiExternalLinkSupport {

    private MediaWikiExternalLinkSupport() {}

    public static @Nullable URI resolveExternalUri(@Nullable String rawUrl) {
        if (rawUrl == null) {
            return null;
        }
        String trimmed = rawUrl.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        try {
            URI uri = URI.create(trimmed);
            if (!uri.isAbsolute()) {
                return null;
            }
            String scheme = uri.getScheme();
            if (scheme == null) {
                return null;
            }
            String normalizedScheme = scheme.toLowerCase(Locale.ROOT);
            return "http".equals(normalizedScheme) || "https".equals(normalizedScheme) ? uri : null;
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    public static boolean isSupportedExternalUrl(@Nullable String rawUrl) {
        return resolveExternalUri(rawUrl) != null;
    }
}
