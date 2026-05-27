package com.hfstudio.guidenh.guide.siteexport.site;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import net.minecraft.util.ResourceLocation;

import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.guide.compiler.IdUtils;

import cpw.mods.fml.common.FMLLog;

public class GuideSitePageAssetExporter {

    public static final String ROOT_PREFIX = "{{root}}/";

    public interface AssetLoader {

        byte @Nullable [] load(ResourceLocation assetId) throws Exception;
    }

    private final GuideSiteAssetRegistry assets;
    private final AssetLoader assetLoader;
    private final Map<ResourceLocation, String> exportedResourcePaths = new HashMap<>();
    private final Map<ResourceLocation, String> exportedSoundPaths = new HashMap<>();

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
        } catch (Exception e) {
            FMLLog.getLogger()
                .debug(
                    "[GuideNH] [GuideSitePageAssetExporter] Failed to parse image source {} from page {}",
                    rawUrl,
                    currentPageId,
                    e);
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
        } catch (Exception e) {
            FMLLog.getLogger()
                .debug(
                    "[GuideNH] [GuideSitePageAssetExporter] Failed to resolve image source {} from page {}",
                    rawUrl,
                    currentPageId,
                    e);
            return rawUrl;
        }
    }

    public String exportResource(ResourceLocation assetId) {
        return exportCached(assetId, "images", exportedResourcePaths);
    }

    public String exportSound(ResourceLocation assetId) {
        return exportCached(assetId, "sounds", exportedSoundPaths);
    }

    private String exportCached(ResourceLocation assetId, String bucket, Map<ResourceLocation, String> cache) {
        if (assetId == null) {
            return "";
        }
        String cached = cache.get(assetId);
        if (cached != null) {
            return cached;
        }

        String exportedPath = exportUncached(assetId, bucket);
        cache.put(assetId, exportedPath);
        return exportedPath;
    }

    private String exportUncached(ResourceLocation assetId, String bucket) {
        if (assetId == null) {
            return "";
        }
        try {
            byte[] content = assetLoader.load(assetId);
            if (content == null || content.length == 0) {
                return "";
            }
            String exportedPath = assets.writeShared(bucket, extensionOf(assetId), content);
            return ROOT_PREFIX + exportedPath;
        } catch (Exception e) {
            FMLLog.getLogger()
                .debug("[GuideNH] [GuideSitePageAssetExporter] Failed to export {} resource {}", bucket, assetId, e);
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
