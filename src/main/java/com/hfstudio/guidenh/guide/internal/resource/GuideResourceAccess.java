package com.hfstudio.guidenh.guide.internal.resource;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.annotation.Nullable;

import net.minecraft.client.resources.IResource;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.util.ResourceLocation;

public class GuideResourceAccess {

    private GuideResourceAccess() {}

    public static @Nullable byte[] readBytes(IResourceManager resourceManager, ResourceLocation id) {
        try {
            IResource resource = resourceManager.getResource(id);
            try (var input = resource.getInputStream()) {
                return readFully(input);
            }
        } catch (IOException ignored) {}
        return null;
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
