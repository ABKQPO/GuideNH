package com.hfstudio.guidenh.guide.internal;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.Set;

import net.minecraft.client.resources.IResourcePack;
import net.minecraft.client.resources.data.IMetadataSection;
import net.minecraft.client.resources.data.IMetadataSerializer;
import net.minecraft.util.ResourceLocation;

import org.jetbrains.annotations.Nullable;

public final class GuideDevelopmentResourcePack implements IResourcePack {

    private final Path root;
    private final Set<String> resourceDomains;

    GuideDevelopmentResourcePack(Path root) {
        this.root = root.toAbsolutePath()
            .normalize();
        this.resourceDomains = discoverResourceDomains(this.root);
    }

    public Path getRoot() {
        return root;
    }

    @Override
    public InputStream getInputStream(ResourceLocation resourceLocation) throws IOException {
        Path resourcePath = resolveResourcePath(resourceLocation);
        if (!Files.isRegularFile(resourcePath)) {
            throw new IOException("Resource does not exist: " + resourceLocation);
        }
        return Files.newInputStream(resourcePath);
    }

    @Override
    public boolean resourceExists(ResourceLocation resourceLocation) {
        return Files.isRegularFile(resolveResourcePath(resourceLocation));
    }

    @Override
    public Set<String> getResourceDomains() {
        return resourceDomains;
    }

    @Override
    public BufferedImage getPackImage() throws IOException {
        return null;
    }

    @Override
    public IMetadataSection getPackMetadata(IMetadataSerializer metadataSerializer, String metadataSectionName)
        throws IOException {
        return null;
    }

    @Override
    public String getPackName() {
        return "GuideNH Development Resources (" + root + ")";
    }

    @Nullable
    byte[] readBytes(ResourceLocation resourceLocation) {
        Path resourcePath = resolveResourcePath(resourceLocation);
        if (!Files.isRegularFile(resourcePath)) {
            return null;
        }

        try {
            return Files.readAllBytes(resourcePath);
        } catch (IOException e) {
            return null;
        }
    }

    private Path resolveResourcePath(ResourceLocation resourceLocation) {
        return root.resolve("assets")
            .resolve(resourceLocation.getResourceDomain())
            .resolve(resourceLocation.getResourcePath());
    }

    private static Set<String> discoverResourceDomains(Path root) {
        var domains = new LinkedHashSet<String>();
        Path assetsDir = root.resolve("assets");
        if (!Files.isDirectory(assetsDir)) {
            return domains;
        }

        try (var children = Files.list(assetsDir)) {
            children.filter(Files::isDirectory)
                .forEach(
                    child -> domains.add(
                        child.getFileName()
                            .toString()));
        } catch (IOException ignored) {}

        return domains;
    }
}
