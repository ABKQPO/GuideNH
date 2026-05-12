package com.hfstudio.guidenh.guide.internal.datadriven;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipFile;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.AbstractResourcePack;
import net.minecraft.client.resources.IResourcePack;
import net.minecraft.util.ResourceLocation;

import com.hfstudio.guidenh.guide.Guide;
import com.hfstudio.guidenh.guide.internal.MutableGuide;
import com.hfstudio.guidenh.guide.internal.util.LangUtil;
import com.hfstudio.guidenh.mixins.early.fml.AccessorFMLClientHandler;
import com.hfstudio.guidenh.mixins.early.minecraft.AccessorAbstractResourcePack;

import cpw.mods.fml.client.FMLClientHandler;
import cpw.mods.fml.common.FMLLog;

public class DataDrivenGuideLoader {

    public static final String AUTO_GUIDE_FOLDER = "guidenh";
    public static final String LANGUAGE_FOLDER_PREFIX = "_";
    public static final ResourceLocation MERGED_GUIDE_ID = new ResourceLocation("guidenh", AUTO_GUIDE_FOLDER);

    private DataDrivenGuideLoader() {}

    public static Map<ResourceLocation, MutableGuide> load() {
        var discoveredLanguages = new LinkedHashMap<ResourceLocation, LinkedHashSet<String>>();

        for (var resourcePack : getActiveResourcePacks()) {
            scanResourcePack(resourcePack, discoveredLanguages);
        }

        var mergedLanguages = mergeLanguages(discoveredLanguages);
        var builder = Guide.builder(MERGED_GUIDE_ID)
            .register(false)
            .folder(AUTO_GUIDE_FOLDER)
            .defaultLanguage(selectDefaultLanguage(mergedLanguages));
        var guide = (MutableGuide) builder.build();

        var guides = new LinkedHashMap<ResourceLocation, MutableGuide>();
        guides.put(MERGED_GUIDE_ID, guide);
        return guides;
    }

    public static LinkedHashMap<String, LinkedHashSet<String>> discoverPagePaths(String folder) {
        var pagePaths = new LinkedHashMap<String, LinkedHashSet<String>>();

        for (var resourcePack : getActiveResourcePacks()) {
            scanPagePathsAllNamespaces(resourcePack, folder, pagePaths);
        }

        return pagePaths;
    }

    private static void scanPagePathsAllNamespaces(IResourcePack resourcePack, String folder,
        LinkedHashMap<String, LinkedHashSet<String>> pagePaths) {
        var resourcePackFile = getResourcePackFile(resourcePack);
        if (resourcePackFile == null || !resourcePackFile.exists()) {
            return;
        }

        if (!resourcePackFile.isDirectory()) {
            scanZipPagePathsAllNamespaces(resourcePackFile, folder, pagePaths);
            return;
        }

        var assetsDir = new File(resourcePackFile, "assets");
        var namespaceDirs = assetsDir.listFiles(File::isDirectory);
        if (namespaceDirs == null) {
            return;
        }

        for (var namespaceDir : namespaceDirs) {
            var guideRootDir = new File(namespaceDir, folder);
            if (!guideRootDir.isDirectory()) {
                continue;
            }

            var namespace = namespaceDir.getName();
            var prefix = toFolderPrefix(namespace, folder);
            var paths = pagePaths.computeIfAbsent(namespace, k -> new LinkedHashSet<>());
            scanFolderPagePaths(resourcePackFile, prefix, paths);
        }
    }

    private static void scanZipPagePathsAllNamespaces(File resourcePackFile, String folder,
        LinkedHashMap<String, LinkedHashSet<String>> pagePaths) {
        var prefix = "assets/";
        try (var zip = new ZipFile(resourcePackFile)) {
            var entries = zip.entries();
            while (entries.hasMoreElements()) {
                var entry = entries.nextElement();
                if (entry.isDirectory()) {
                    continue;
                }

                var path = entry.getName();
                if (!path.startsWith(prefix) || !path.endsWith(".md")) {
                    continue;
                }

                // path format: assets/<namespace>/<folder>/_<lang>/<pagePath>.md
                var afterAssets = path.substring(prefix.length());
                var firstSlash = afterAssets.indexOf('/');
                if (firstSlash <= 0) {
                    continue;
                }

                var namespace = afterAssets.substring(0, firstSlash);
                var afterNamespace = afterAssets.substring(firstSlash + 1);

                // Check that afterNamespace starts with folder/
                if (!afterNamespace.startsWith(folder + "/")) {
                    continue;
                }

                var afterFolder = afterNamespace.substring(folder.length() + 1);
                var slashIndex = afterFolder.indexOf('/');
                if (slashIndex <= 0) {
                    continue;
                }

                var language = afterFolder.substring(0, slashIndex);
                if (!isLanguageFolder(language)) {
                    continue;
                }

                var pagePath = afterFolder.substring(slashIndex + 1);
                if (!pagePath.isEmpty()) {
                    pagePaths.computeIfAbsent(namespace, k -> new LinkedHashSet<>())
                        .add(pagePath);
                }
            }
        } catch (IOException e) {
            FMLLog.getLogger()
                .warn(
                    "[GuideNH] [DataDrivenGuideLoader] Failed to scan guide pages from resource pack {}",
                    resourcePackFile.getAbsolutePath(),
                    e);
        }
    }

    private static LinkedHashSet<String> mergeLanguages(
        LinkedHashMap<ResourceLocation, LinkedHashSet<String>> discoveredLanguages) {
        var merged = new LinkedHashSet<String>();
        for (var languages : discoveredLanguages.values()) {
            merged.addAll(languages);
        }
        return merged;
    }

    /** @deprecated Use {@link #discoverPagePaths(String)} instead. */
    @Deprecated
    public static Set<String> discoverPagePaths(ResourceLocation guideId, String folder) {
        var result = new LinkedHashSet<String>();
        var allPaths = discoverPagePaths(folder);
        for (var paths : allPaths.values()) {
            result.addAll(paths);
        }
        return result;
    }

    public static List<IResourcePack> getActiveResourcePacks() {
        var resourcePacks = new LinkedHashSet<IResourcePack>();

        try {
            var accessor = (AccessorFMLClientHandler) FMLClientHandler.instance();
            var basePacks = accessor.guidenh$getResourcePackList();
            if (basePacks != null) {
                resourcePacks.addAll(basePacks);
            }
        } catch (RuntimeException e) {
            FMLLog.getLogger()
                .warn(
                    "[GuideNH] [DataDrivenGuideLoader] Failed to inspect the currently loaded base resource packs",
                    e);
        }

        var repository = Minecraft.getMinecraft()
            .getResourcePackRepository();
        for (var entry : repository.getRepositoryEntries()) {
            var resourcePack = entry.getResourcePack();
            if (resourcePack != null) {
                resourcePacks.add(resourcePack);
            }
        }

        var serverPack = repository.func_148530_e();
        if (serverPack != null) {
            resourcePacks.add(serverPack);
        }

        return new ArrayList<>(resourcePacks);
    }

    public static void scanResourcePack(IResourcePack resourcePack,
        Map<ResourceLocation, LinkedHashSet<String>> discoveredLanguages) {
        var resourcePackFile = getResourcePackFile(resourcePack);
        if (resourcePackFile == null || !resourcePackFile.exists()) {
            return;
        }

        if (!resourcePackFile.isDirectory()) return;
        scanResourcePackFolder(resourcePackFile, discoveredLanguages);
    }

    public static void scanPagePaths(IResourcePack resourcePack, String prefix, Set<String> pagePaths) {
        var resourcePackFile = getResourcePackFile(resourcePack);
        if (resourcePackFile == null || !resourcePackFile.exists()) {
            return;
        }

        if (resourcePackFile.isDirectory()) {
            scanFolderPagePaths(resourcePackFile, prefix, pagePaths);
        } else {
            scanZipPagePaths(resourcePackFile, prefix, pagePaths);
        }
    }

    public static File getResourcePackFile(IResourcePack resourcePack) {
        if (!(resourcePack instanceof AbstractResourcePack)) {
            return null;
        }

        try {
            return ((AccessorAbstractResourcePack) resourcePack).guidenh$getResourcePackFile();
        } catch (RuntimeException e) {
            FMLLog.getLogger()
                .warn(
                    "[GuideNH] [DataDrivenGuideLoader] Failed to resolve the backing file for resource pack {}",
                    resourcePack.getPackName(),
                    e);
            return null;
        }
    }

    public static void scanResourcePackFolder(File resourcePackRoot,
        Map<ResourceLocation, LinkedHashSet<String>> discoveredLanguages) {
        var assetsDir = new File(resourcePackRoot, "assets");
        var namespaceDirs = assetsDir.listFiles(File::isDirectory);
        if (namespaceDirs == null) {
            return;
        }

        for (var namespaceDir : namespaceDirs) {
            var guideRootDir = new File(namespaceDir, AUTO_GUIDE_FOLDER);
            if (!guideRootDir.isDirectory()) {
                continue;
            }

            var languageDirs = guideRootDir.listFiles(File::isDirectory);
            if (languageDirs == null) {
                continue;
            }

            for (var languageDir : languageDirs) {
                var languageFolder = languageDir.getName();
                if (!isLanguageFolder(languageFolder)) {
                    continue;
                }

                if (!containsMarkdownFiles(languageDir)) {
                    continue;
                }

                var guideId = new ResourceLocation(namespaceDir.getName(), AUTO_GUIDE_FOLDER);
                discoveredLanguages.computeIfAbsent(guideId, ignored -> new LinkedHashSet<>())
                    .add(toLanguageCode(languageFolder));
            }
        }
    }

    public static void scanFolderPagePaths(File resourcePackRoot, String prefix, Set<String> pagePaths) {
        var resourceRoot = new File(resourcePackRoot, prefix.replace('/', File.separatorChar));
        var languageDirs = resourceRoot.listFiles(File::isDirectory);
        if (languageDirs == null) {
            return;
        }

        for (var languageDir : languageDirs) {
            if (!isLanguageFolder(languageDir.getName())) {
                continue;
            }
            collectMarkdownPaths(languageDir, "", pagePaths);
        }
    }

    public static void scanZipPagePaths(File resourcePackFile, String prefix, Set<String> pagePaths) {
        try (var zip = new ZipFile(resourcePackFile)) {
            var entries = zip.entries();
            while (entries.hasMoreElements()) {
                var entry = entries.nextElement();
                if (entry.isDirectory()) {
                    continue;
                }

                var path = entry.getName();
                if (!path.startsWith(prefix) || !path.endsWith(".md")) {
                    continue;
                }

                var relative = path.substring(prefix.length());
                var slashIndex = relative.indexOf('/');
                if (slashIndex <= 0) {
                    continue;
                }

                var language = relative.substring(0, slashIndex);
                if (!isLanguageFolder(language)) {
                    continue;
                }

                var pagePath = relative.substring(slashIndex + 1);
                if (!pagePath.isEmpty()) {
                    pagePaths.add(pagePath);
                }
            }
        } catch (IOException e) {
            FMLLog.getLogger()
                .warn(
                    "[GuideNH] [DataDrivenGuideLoader] Failed to scan guide pages from resource pack {}",
                    resourcePackFile.getAbsolutePath(),
                    e);
        }
    }

    public static void collectMarkdownPaths(File directory, String relativePath, Set<String> pagePaths) {
        var children = directory.listFiles();
        if (children == null) {
            return;
        }

        for (var child : children) {
            if (child.isDirectory()) {
                var childRelative = relativePath.isEmpty() ? child.getName() : relativePath + "/" + child.getName();
                collectMarkdownPaths(child, childRelative, pagePaths);
            } else if (child.isFile() && child.getName()
                .endsWith(".md")) {
                    var pagePath = relativePath.isEmpty() ? child.getName() : relativePath + "/" + child.getName();
                    pagePaths.add(pagePath);
                }
        }
    }

    public static boolean containsMarkdownFiles(File directory) {
        var children = directory.listFiles();
        if (children == null) {
            return false;
        }

        for (var child : children) {
            if (child.isFile() && child.getName()
                .endsWith(".md")) {
                return true;
            }
            if (child.isDirectory() && containsMarkdownFiles(child)) {
                return true;
            }
        }

        return false;
    }

    public static boolean isLanguageFolder(String name) {
        return name.startsWith(LANGUAGE_FOLDER_PREFIX) && LangUtil.isLanguageCode(name.substring(1));
    }

    public static String toLanguageCode(String folderName) {
        return LangUtil.normalizeLanguage(folderName.substring(LANGUAGE_FOLDER_PREFIX.length()));
    }

    public static String selectDefaultLanguage(Set<String> languages) {
        if (languages.contains("en_us")) {
            return "en_us";
        }

        return languages.stream()
            .min(Comparator.naturalOrder())
            .orElse("en_us");
    }

    public static String toFolderPrefix(String namespace, String folder) {
        return "assets/" + namespace + "/" + folder + "/";
    }
}
