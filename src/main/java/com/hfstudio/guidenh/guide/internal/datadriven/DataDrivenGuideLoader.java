package com.hfstudio.guidenh.guide.internal.datadriven;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
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

import com.hfstudio.guidenh.config.ModConfig;
import com.hfstudio.guidenh.guide.Guide;
import com.hfstudio.guidenh.guide.internal.GuideDevelopmentResourcePack;
import com.hfstudio.guidenh.guide.internal.GuideDevelopmentResourcePacks;
import com.hfstudio.guidenh.guide.internal.MutableGuide;
import com.hfstudio.guidenh.guide.internal.resource.GuideResourceAccess;
import com.hfstudio.guidenh.guide.internal.util.LangUtil;
import com.hfstudio.guidenh.mixins.early.fml.AccessorFMLClientHandler;
import com.hfstudio.guidenh.mixins.early.minecraft.AccessorAbstractResourcePack;

import cpw.mods.fml.client.FMLClientHandler;
import cpw.mods.fml.common.FMLLog;

public class DataDrivenGuideLoader {

    public static final String AUTO_GUIDE_FOLDER = "guidenh";
    public static final String LANGUAGE_FOLDER_PREFIX = "_";

    private DataDrivenGuideLoader() {}

    public static Map<ResourceLocation, MutableGuide> load() {
        long startedAt = System.nanoTime();
        long stageStartedAt = startedAt;
        var activeResourcePacks = getActiveResourcePacks();
        long resourcePackResolveNs = System.nanoTime() - stageStartedAt;

        var discoveredLanguages = new LinkedHashMap<ResourceLocation, LinkedHashSet<String>>();
        stageStartedAt = System.nanoTime();
        for (var resourcePack : activeResourcePacks) {
            scanResourcePack(resourcePack, discoveredLanguages);
        }
        long scanNs = System.nanoTime() - stageStartedAt;

        stageStartedAt = System.nanoTime();
        var guides = new LinkedHashMap<ResourceLocation, MutableGuide>();
        for (var entry : discoveredLanguages.entrySet()) {
            ResourceLocation guideId = entry.getKey();
            var builder = Guide.builder(guideId)
                .register(false)
                .folder(AUTO_GUIDE_FOLDER)
                .defaultLanguage(autoDiscoveredDefaultLanguage());
            guides.put(guideId, (MutableGuide) builder.build());
        }
        long buildNs = System.nanoTime() - stageStartedAt;
        int discoveredLanguageCount = countDiscoveredLanguages(discoveredLanguages);
        long totalNs = System.nanoTime() - startedAt;
        if (ModConfig.debug.enableDebugMode) {
            FMLLog.getLogger()
                .info(
                    "[GuideNH] [DataDrivenGuideLoader] Loaded {} guides across {} languages from {} resource packs in {} ns (resourcePackResolveNs={}, scanNs={}, buildNs={})",
                    guides.size(),
                    discoveredLanguageCount,
                    activeResourcePacks.size(),
                    totalNs,
                    resourcePackResolveNs,
                    scanNs,
                    buildNs);
        }
        return guides;
    }

    public static LinkedHashMap<String, LinkedHashSet<String>> discoverPagePaths(String folder) {
        long startedAt = System.nanoTime();
        var activeResourcePacks = getActiveResourcePacks();
        var pagePaths = new LinkedHashMap<String, LinkedHashSet<String>>();

        for (var resourcePack : activeResourcePacks) {
            scanPagePathsAllNamespaces(resourcePack, folder, pagePaths);
        }

        long totalNs = System.nanoTime() - startedAt;
        if (ModConfig.debug.enableDebugMode) {
            FMLLog.getLogger()
                .info(
                    "[GuideNH] [DataDrivenGuideLoader] Discovered {} page paths across {} namespaces for folder {} from {} resource packs in {} ns",
                    countDiscoveredPagePaths(pagePaths),
                    pagePaths.size(),
                    folder,
                    activeResourcePacks.size(),
                    totalNs);
        }
        return pagePaths;
    }

    private static int countDiscoveredLanguages(Map<ResourceLocation, LinkedHashSet<String>> discoveredLanguages) {
        int total = 0;
        for (var languages : discoveredLanguages.values()) {
            total += languages.size();
        }
        return total;
    }

    private static int countDiscoveredPagePaths(LinkedHashMap<String, LinkedHashSet<String>> pagePaths) {
        int total = 0;
        for (var namespacePaths : pagePaths.values()) {
            total += namespacePaths.size();
        }
        return total;
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
        scanPagePathsAllNamespaces(resourcePackFile, folder, pagePaths);
    }

    public static void scanPagePathsAllNamespaces(File resourcePackRoot, String folder,
        LinkedHashMap<String, LinkedHashSet<String>> pagePaths) {
        if (!resourcePackRoot.isDirectory()) {
            scanZipPagePathsAllNamespaces(resourcePackRoot, folder, pagePaths);
            return;
        }

        var assetsDir = new File(resourcePackRoot, "assets");
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
            scanFolderPagePaths(resourcePackRoot, prefix, paths);
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

    public static Set<String> discoverPagePaths(ResourceLocation guideId, String folder) {
        var result = new LinkedHashSet<String>();
        for (var resourcePack : getActiveResourcePacks()) {
            scanPagePathsForNamespace(resourcePack, guideId.getResourceDomain(), folder, result);
        }
        return result;
    }

    public static void scanPagePathsForNamespace(IResourcePack resourcePack, String namespace, String folder,
        Set<String> pagePaths) {
        var resourcePackFile = getResourcePackFile(resourcePack);
        if (resourcePackFile == null || !resourcePackFile.exists()) {
            return;
        }
        scanPagePathsForNamespace(resourcePackFile, namespace, folder, pagePaths);
    }

    public static void scanPagePathsForNamespace(File resourcePackRoot, String namespace, String folder,
        Set<String> pagePaths) {
        if (resourcePackRoot.isDirectory()) {
            scanFolderPagePaths(resourcePackRoot, toFolderPrefix(namespace, folder), pagePaths);
        } else {
            scanZipPagePaths(resourcePackRoot, toFolderPrefix(namespace, folder), pagePaths);
        }
    }

    public static List<IResourcePack> getActiveResourcePacks() {
        var resourcePacks = new LinkedHashSet<IResourcePack>(GuideDevelopmentResourcePacks.getConfiguredPacks());

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

        if (resourcePackFile.isDirectory()) {
            scanResourcePackFolder(resourcePackFile, discoveredLanguages);
        } else {
            scanResourcePackZip(resourcePackFile, discoveredLanguages);
        }
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
        if (resourcePack instanceof GuideDevelopmentResourcePack) {
            return ((GuideDevelopmentResourcePack) resourcePack).getRoot()
                .toFile();
        }

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

    public static byte[] readBytes(IResourcePack resourcePack, ResourceLocation resourceLocation) {
        if (!resourcePack.resourceExists(resourceLocation)) {
            return null;
        }
        try (var input = resourcePack.getInputStream(resourceLocation)) {
            return GuideResourceAccess.readFully(input);
        } catch (IOException e) {
            FMLLog.getLogger()
                .warn(
                    "[GuideNH] [DataDrivenGuideLoader] Failed to read resource {} from resource pack {}",
                    resourceLocation,
                    resourcePack.getPackName(),
                    e);
            return null;
        }
    }

    public static IResourcePack findResourcePack(ResourceLocation resourceLocation) {
        return findResourcePack(resourceLocation, getActiveResourcePacks());
    }

    public static IResourcePack findResourcePack(ResourceLocation resourceLocation,
        Iterable<? extends IResourcePack> resourcePacks) {
        GuidePageResourceSelector.SelectedPageResource selected = GuidePageResourceSelector
            .select(resourceLocation, resourcePacks);
        return selected != null ? selected.resourcePack() : null;
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

    public static void scanResourcePackZip(File resourcePackFile,
        Map<ResourceLocation, LinkedHashSet<String>> discoveredLanguages) {
        String assetsPrefix = "assets/";
        try (var zip = new ZipFile(resourcePackFile)) {
            var entries = zip.entries();
            while (entries.hasMoreElements()) {
                var entry = entries.nextElement();
                if (entry.isDirectory()) {
                    continue;
                }

                var path = entry.getName();
                if (!path.startsWith(assetsPrefix) || !path.endsWith(".md")) {
                    continue;
                }

                var afterAssets = path.substring(assetsPrefix.length());
                var namespaceEnd = afterAssets.indexOf('/');
                if (namespaceEnd <= 0) {
                    continue;
                }

                var namespace = afterAssets.substring(0, namespaceEnd);
                var afterNamespace = afterAssets.substring(namespaceEnd + 1);
                if (!afterNamespace.startsWith(AUTO_GUIDE_FOLDER + "/")) {
                    continue;
                }

                var afterGuideFolder = afterNamespace.substring(AUTO_GUIDE_FOLDER.length() + 1);
                var languageEnd = afterGuideFolder.indexOf('/');
                if (languageEnd <= 0) {
                    continue;
                }

                var languageFolder = afterGuideFolder.substring(0, languageEnd);
                if (!isLanguageFolder(languageFolder)) {
                    continue;
                }

                discoveredLanguages
                    .computeIfAbsent(
                        new ResourceLocation(namespace, AUTO_GUIDE_FOLDER),
                        ignored -> new LinkedHashSet<>())
                    .add(toLanguageCode(languageFolder));
            }
        } catch (IOException e) {
            FMLLog.getLogger()
                .warn(
                    "[GuideNH] [DataDrivenGuideLoader] Failed to scan guide languages from resource pack {}",
                    resourcePackFile.getAbsolutePath(),
                    e);
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
            String childPath = relativePath.isEmpty() ? child.getName() : relativePath + "/" + child.getName();
            if (child.isDirectory()) {
                collectMarkdownPaths(child, childPath, pagePaths);
            } else if (child.isFile() && child.getName()
                .endsWith(".md")) {
                    pagePaths.add(childPath);
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

    public static String autoDiscoveredDefaultLanguage() {
        return LangUtil.ENGLISH_LANGUAGE;
    }

    public static String toFolderPrefix(String namespace, String folder) {
        return "assets/" + namespace + "/" + folder + "/";
    }
}
