package com.hfstudio.guidenh.guide.internal.resource;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import net.minecraft.client.resources.IResource;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.util.ResourceLocation;

import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.guide.internal.GuideDevelopmentResourcePacks;
import com.hfstudio.guidenh.guide.internal.datadriven.DataDrivenGuideLoader;

public class GuideResourceAccess {

    private static final ConcurrentMap<ResourceLocation, Optional<byte[]>> FALLBACK_CACHE = new ConcurrentHashMap<>();

    private GuideResourceAccess() {}

    public static void clearCache() {
        FALLBACK_CACHE.clear();
    }

    public static @Nullable byte[] readBytes(IResourceManager resourceManager, ResourceLocation id) {
        byte[] developmentBytes = GuideDevelopmentResourcePacks.readBytes(id);
        if (developmentBytes != null) {
            return developmentBytes;
        }

        try {
            IResource resource = resourceManager.getResource(id);
            try (var input = resource.getInputStream()) {
                return readFully(input);
            }
        } catch (IOException ignored) {}

        return FALLBACK_CACHE.computeIfAbsent(id, GuideResourceAccess::readFallbackBytes)
            .orElse(null);
    }

    private static Optional<byte[]> readFallbackBytes(ResourceLocation id) {
        for (var resourcePack : DataDrivenGuideLoader.getLastActiveResourcePacks()) {
            byte[] bytes = DataDrivenGuideLoader.readLooseBytes(resourcePack, id);
            if (bytes != null) {
                return Optional.of(bytes);
            }
        }
        return Optional.empty();
    }

    public static @Nullable InputStream openStream(IResourceManager resourceManager, ResourceLocation id) {
        var bytes = readBytes(resourceManager, id);
        return bytes != null ? new ByteArrayInputStream(bytes) : null;
    }

    public static byte[] readFully(InputStream input) throws IOException {
        var out = new ByteArrayOutputStream();
        var buffer = new byte[8192];
        int read;
        while ((read = input.read(buffer)) >= 0) {
            out.write(buffer, 0, read);
        }
        return out.toByteArray();
    }
}
