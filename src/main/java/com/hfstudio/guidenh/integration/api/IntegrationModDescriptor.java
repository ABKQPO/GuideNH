package com.hfstudio.guidenh.integration.api;

import java.util.Locale;
import java.util.function.Supplier;

import org.jetbrains.annotations.Nullable;

public class IntegrationModDescriptor {

    private final String id;
    @Nullable
    private final String resourceDomain;
    private final Supplier<Boolean> loadedSupplier;

    public IntegrationModDescriptor(String id, @Nullable String resourceDomain, Supplier<Boolean> loadedSupplier) {
        this.id = normalizeId(id);
        this.resourceDomain = normalizeResourceDomain(resourceDomain);
        if (loadedSupplier == null) {
            throw new IllegalArgumentException("loadedSupplier");
        }
        this.loadedSupplier = loadedSupplier;
    }

    public String id() {
        return id;
    }

    @Nullable
    public String resourceDomain() {
        return resourceDomain;
    }

    public boolean isLoaded() {
        return loadedSupplier.get();
    }

    public static String normalizeId(String id) {
        String normalized = normalizeIdOrNull(id);
        if (normalized == null) {
            throw new IllegalArgumentException("id");
        }
        return normalized;
    }

    @Nullable
    public static String normalizeIdOrNull(@Nullable String id) {
        if (id == null) {
            return null;
        }
        String trimmed = id.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    @Nullable
    public static String normalizeResourceDomain(@Nullable String resourceDomain) {
        String normalized = normalizeIdOrNull(resourceDomain);
        return normalized != null ? normalized.toLowerCase(Locale.ENGLISH) : null;
    }
}
