package com.hfstudio.guidenh.guide.siteexport.site;

import java.net.URI;

import net.minecraft.util.ResourceLocation;

import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.guide.compiler.IdUtils;

public class GuideSitePageAssetExporter {

    public static final String ROOT_PREFIX = "{{root}}/";

    public interface AssetLoader {

        @Nullable
        byte[] load(ResourceLocation assetId) throws Exception;
    }

    private final GuideSiteAssetRegistry assets;
    private final AssetLoader assetLoader;

    public GuideSitePageAssetExporter(GuideSiteAssetRegistry assets, AssetLoader assetLoader) {
        this.assets = assets;
        this.assetLoader = assetLoader;
    }

    public String resolveImageSrc(String rawUrl, @Nullable ResourceLocation currentPageId) {
        if (rawUrl == null || rawUrl.isEmpty()) {
            return "";
        }

        URI uri;
        try {
            uri = URI.create(rawUrl);
        } catch (Exception ignored) {
            uri = null;
        }
        if (uri != null && uri.isAbsolute()) {
            return rawUrl;
        }
        if (currentPageId == null) {
            return rawUrl;
        }

        try {
            ResourceLocation imageId = IdUtils.resolveLink(rawUrl, currentPageId);
            String exportedPath = exportResource(imageId);
            return exportedPath.isEmpty() ? rawUrl : exportedPath;
        } catch (Exception ignored) {
            return rawUrl;
        }
    }

    public String exportResource(ResourceLocation assetId) {
        if (assetId == null) {
            return "";
        }
        try {
            byte[] content = assetLoader.load(assetId);
            if (content == null || content.length == 0) {
                return "";
            }
            String exportedPath = assets.writeShared("images", extensionOf(assetId), content);
            return ROOT_PREFIX + exportedPath;
        } catch (Exception ignored) {
            return "";
        }
    }

    public static String toRootRelativePath(@Nullable String exportedPath) {
        if (exportedPath == null || exportedPath.isEmpty()) {
            return "";
        }
        return exportedPath.startsWith(ROOT_PREFIX) ? exportedPath.substring(ROOT_PREFIX.length()) : exportedPath;
    }

    private String extensionOf(ResourceLocation assetId) {
        String path = assetId.getResourcePath();
        int dot = path.lastIndexOf('.');
        if (dot < 0 || dot == path.length() - 1) {
            return ".bin";
        }
        return path.substring(dot);
    }
}
