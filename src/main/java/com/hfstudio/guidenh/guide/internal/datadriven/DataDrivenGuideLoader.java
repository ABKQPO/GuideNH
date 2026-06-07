package com.hfstudio.guidenh.guide.internal.datadriven;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipFile;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.AbstractResourcePack;
import net.minecraft.client.resources.FallbackResourceManager;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.client.resources.IResourcePack;
import net.minecraft.client.resources.SimpleReloadableResourceManager;
import net.minecraft.util.ResourceLocation;

import com.hfstudio.guidenh.config.ModConfig;
import com.hfstudio.guidenh.guide.Guide;
import com.hfstudio.guidenh.guide.internal.GuideDevelopmentResourcePack;
import com.hfstudio.guidenh.guide.internal.GuideDevelopmentResourcePacks;
import com.hfstudio.guidenh.guide.internal.MutableGuide;
import com.hfstudio.guidenh.guide.internal.resource.GuideResourceAccess;
import com.hfstudio.guidenh.guide.internal.util.LangUtil;
import com.hfstudio.guidenh.guide.scene.support.GuideDebugLog;
import com.hfstudio.guidenh.mixins.early.fml.AccessorFMLClientHandler;
import com.hfstudio.guidenh.mixins.early.minecraft.AccessorAbstractResourcePack;
import com.hfstudio.guidenh.mixins.early.minecraft.AccessorFallbackResourceManager;
import com.hfstudio.guidenh.mixins.early.minecraft.AccessorSimpleReloadableResourceManager;

import cpw.mods.fml.client.FMLClientHandler;

public class DataDrivenGuideLoader {

    public static final String AUTO_GUIDE_FOLDER = "guidenh";
    public static final String LANGUAGE_FOLDER_PREFIX = "_";
    private static final Map<Class<?>, Field> LOOSE_ROOT_FIELDS = new IdentityHashMap<>();
    private static volatile List<IResourcePack> lastActiveResourcePacks = List.of();
    private static volatile List<IResourcePack> lastResourceManagerResourcePacks = List.of();
    private static volatile Map<IResourcePack, Set<String>> lastResourceManagerDomainsByPack = Map.of();
    private static volatile GuideLanguageDiscoverySnapshot lastGuideLanguageDiscovery = GuideLanguageDiscoverySnapshot
        .empty();

    private DataDrivenGuideLoader() {}

    public static Map<ResourceLocation, MutableGuide> load() {
        return load(getActiveResourcePacks());
    }

    public static Map<ResourceLocation, MutableGuide> load(IResourceManager resourceManager) {
        return load(getActiveResourcePacks(resourceManager));
    }

    public static Map<ResourceLocation, MutableGuide> load(Iterable<? extends IResourcePack> activeResourcePacks) {
        long startedAt = System.nanoTime();
        long stageStartedAt = startedAt;
        var resolvedResourcePacks = toList(activeResourcePacks);
        long resourcePackResolveNs = System.nanoTime() - stageStartedAt;

        stageStartedAt = System.nanoTime();
        var discoveredLanguages = discoverGuideLanguages(resolvedResourcePacks);
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
            GuideDebugLog.infoAlways(
                "[GuideNH] [DataDrivenGuideLoader] Loaded {} guides across {} languages from {} resource packs in {} ns (resourcePackResolveNs={}, scanNs={}, buildNs={})",
                guides.size(),
                discoveredLanguageCount,
                resolvedResourcePacks.size(),
                totalNs,
                resourcePackResolveNs,
                scanNs,
                buildNs);
        }
        return guides;
    }

    public static Map<ResourceLocation, Set<String>> discoverGuideLanguages() {
        return discoverGuideLanguages(getActiveResourcePacks());
    }

    public static Map<ResourceLocation, Set<String>> discoverGuideLanguages(
        Iterable<? extends IResourcePack> activeResourcePacks) {
        var resolvedResourcePacks = toList(activeResourcePacks);
        GuideLanguageDiscoverySnapshot cached = lastGuideLanguageDiscovery;
        if (cached.matches(resolvedResourcePacks)) {
            return cached.discoveredLanguages();
        }

        var discoveredLanguages = new LinkedHashMap<ResourceLocation, LinkedHashSet<String>>();
        for (var resourcePack : resolvedResourcePacks) {
            scanResourcePack(resourcePack, discoveredLanguages);
        }

        var frozen = freezeDiscoveredLanguages(discoveredLanguages);
        lastGuideLanguageDiscovery = new GuideLanguageDiscoverySnapshot(List.copyOf(resolvedResourcePacks), frozen);
        return frozen;
    }

    public static LinkedHashMap<String, LinkedHashSet<String>> discoverPagePaths(String folder) {
        return discoverPagePaths(folder, getActiveResourcePacks());
    }

    public static LinkedHashMap<String, LinkedHashSet<String>> discoverPagePaths(String folder,
        Iterable<? extends IResourcePack> activeResourcePacks) {
        long startedAt = System.nanoTime();
        var resolvedResourcePacks = toList(activeResourcePacks);
        var pagePaths = new LinkedHashMap<String, LinkedHashSet<String>>();

        for (var resourcePack : resolvedResourcePacks) {
            scanPagePathsAllNamespaces(resourcePack, folder, pagePaths);
        }

        long totalNs = System.nanoTime() - startedAt;
        if (ModConfig.debug.enableDebugMode) {
            GuideDebugLog.infoAlways(
                "[GuideNH] [DataDrivenGuideLoader] Discovered {} page paths across {} namespaces for folder {} from {} resource packs in {} ns",
                countDiscoveredPagePaths(pagePaths),
                pagePaths.size(),
                folder,
                resolvedResourcePacks.size(),
                totalNs);
        }
        return pagePaths;
    }

    private static int countDiscoveredPagePaths(LinkedHashMap<String, LinkedHashSet<String>> pagePaths) {
        int total = 0;
        for (var namespacePaths : pagePaths.values()) {
            total += namespacePaths.size();
        }
        return total;
    }

    private static int countDiscoveredLanguages(Map<ResourceLocation, ? extends Set<String>> discoveredLanguages) {
        int total = 0;
        for (var languages : discoveredLanguages.values()) {
            total += languages.size();
        }
        return total;
    }

    private static void scanPagePathsAllNamespaces(IResourcePack resourcePack, String folder,
        LinkedHashMap<String, LinkedHashSet<String>> pagePaths) {
        var resourcePackRoot = getLooseResourcePackRoot(resourcePack);
        if (resourcePackRoot == null || !resourcePackRoot.exists()) {
            return;
        }

        if (!resourcePackRoot.isDirectory()) {
            scanZipPagePathsAllNamespaces(resourcePackRoot, folder, pagePaths);
            return;
        }
        scanPagePathsAllNamespaces(resourcePack, resourcePackRoot, folder, pagePaths);
    }

    public static void scanPagePathsAllNamespaces(File resourcePackRoot, String folder,
        LinkedHashMap<String, LinkedHashSet<String>> pagePaths) {
        if (!resourcePackRoot.isDirectory()) {
            scanZipPagePathsAllNamespaces(resourcePackRoot, folder, pagePaths);
            return;
        }

        var assetsDir = new File(resourcePackRoot, "assets");
        var namespaceDirs = assetsDir.listFiles(File::isDirectory);
        if (namespaceDirs != null) {
            for (var namespaceDir : namespaceDirs) {
                scanPagePathsForNamespaceRoot(resourcePackRoot, namespaceDir.getName(), folder, pagePaths);
            }
        }
        scanLoosePagePathsAllNamespaces(resourcePackRoot, folder, pagePaths);
    }

    private static void scanPagePathsAllNamespaces(IResourcePack resourcePack, File resourcePackRoot, String folder,
        LinkedHashMap<String, LinkedHashSet<String>> pagePaths) {
        for (String namespace : getResourceDomains(resourcePack)) {
            scanPagePathsForNamespaceRoot(resourcePackRoot, namespace, folder, pagePaths);
        }
    }

    private static void scanPagePathsForNamespaceRoot(File resourcePackRoot, String namespace, String folder,
        LinkedHashMap<String, LinkedHashSet<String>> pagePaths) {
        if (!isValidNamespace(namespace)) {
            return;
        }

        var discovered = new LinkedHashSet<String>();
        for (String prefix : pagePathPrefixes(namespace, folder)) {
            scanFolderPagePaths(resourcePackRoot, prefix, discovered);
        }
        if (!discovered.isEmpty()) {
            pagePaths.computeIfAbsent(namespace, k -> new LinkedHashSet<>())
                .addAll(discovered);
        }
    }

    private static void scanLoosePagePathsAllNamespaces(File resourcePackRoot, String folder,
        LinkedHashMap<String, LinkedHashSet<String>> pagePaths) {
        var namespaceDirs = resourcePackRoot.listFiles(File::isDirectory);
        if (namespaceDirs == null) {
            return;
        }
        for (var namespaceDir : namespaceDirs) {
            if ("assets".equals(namespaceDir.getName())) {
                continue;
            }
            scanPagePathsForNamespaceRoot(resourcePackRoot, namespaceDir.getName(), folder, pagePaths);
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
            GuideDebugLog.warnAlways(
                "[GuideNH] [DataDrivenGuideLoader] Failed to scan guide pages from resource pack {}",
                resourcePackFile.getAbsolutePath(),
                e);
        }
    }

    public static Set<String> discoverPagePaths(ResourceLocation guideId, String folder) {
        return discoverPagePaths(guideId, folder, getActiveResourcePacks());
    }

    public static Set<String> discoverPagePaths(ResourceLocation guideId, String folder,
        Iterable<? extends IResourcePack> activeResourcePacks) {
        var result = new LinkedHashSet<String>();
        for (var resourcePack : activeResourcePacks) {
            scanPagePathsForNamespace(resourcePack, guideId.getResourceDomain(), folder, result);
        }
        return result;
    }

    public static void scanPagePathsForNamespace(IResourcePack resourcePack, String namespace, String folder,
        Set<String> pagePaths) {
        var resourcePackRoot = getLooseResourcePackRoot(resourcePack);
        if (resourcePackRoot == null || !resourcePackRoot.exists()) {
            return;
        }
        scanPagePathsForNamespace(resourcePackRoot, namespace, folder, pagePaths);
    }

    public static void scanPagePathsForNamespace(File resourcePackRoot, String namespace, String folder,
        Set<String> pagePaths) {
        if (resourcePackRoot.isDirectory()) {
            for (String prefix : pagePathPrefixes(namespace, folder)) {
                scanFolderPagePaths(resourcePackRoot, prefix, pagePaths);
            }
        } else {
            scanZipPagePaths(resourcePackRoot, toFolderPrefix(namespace, folder), pagePaths);
        }
    }

    public static List<IResourcePack> getActiveResourcePacks() {
        var resourcePacks = new LinkedHashSet<IResourcePack>(GuideDevelopmentResourcePacks.getConfiguredPacks());
        resourcePacks.addAll(lastResourceManagerResourcePacks);
        addConfiguredResourcePacks(resourcePacks);
        var resolved = new ArrayList<>(resourcePacks);
        lastActiveResourcePacks = List.copyOf(resolved);
        return resolved;
    }

    public static List<IResourcePack> getActiveResourcePacks(IResourceManager resourceManager) {
        var resourcePacks = new LinkedHashSet<IResourcePack>(GuideDevelopmentResourcePacks.getConfiguredPacks());
        var resourceManagerResourcePacks = new LinkedHashSet<IResourcePack>();
        var domainsByPack = new IdentityHashMap<IResourcePack, LinkedHashSet<String>>();
        addResourceManagerResourcePacks(resourceManager, resourceManagerResourcePacks, domainsByPack);
        lastResourceManagerResourcePacks = List.copyOf(resourceManagerResourcePacks);
        lastResourceManagerDomainsByPack = freezeDomainsByPack(domainsByPack);
        resourcePacks.addAll(resourceManagerResourcePacks);
        addConfiguredResourcePacks(resourcePacks);
        var resolved = new ArrayList<>(resourcePacks);
        lastActiveResourcePacks = List.copyOf(resolved);
        return resolved;
    }

    public static List<IResourcePack> getLastActiveResourcePacks() {
        List<IResourcePack> snapshot = lastActiveResourcePacks;
        return snapshot.isEmpty() ? getActiveResourcePacks() : snapshot;
    }

    private static void addConfiguredResourcePacks(LinkedHashSet<IResourcePack> resourcePacks) {
        try {
            var accessor = (AccessorFMLClientHandler) FMLClientHandler.instance();
            var basePacks = accessor.guidenh$getResourcePackList();
            if (basePacks != null) {
                resourcePacks.addAll(basePacks);
            }
        } catch (RuntimeException e) {
            GuideDebugLog.warnAlways(
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
    }

    private static void addResourceManagerResourcePacks(IResourceManager resourceManager,
        LinkedHashSet<IResourcePack> resourcePacks,
        IdentityHashMap<IResourcePack, LinkedHashSet<String>> domainsByPack) {
        if (!(resourceManager instanceof SimpleReloadableResourceManager)) {
            return;
        }

        try {
            var accessor = (AccessorSimpleReloadableResourceManager) resourceManager;
            Map<String, FallbackResourceManager> domainManagers = accessor.guidenh$getDomainResourceManagers();
            if (domainManagers == null || domainManagers.isEmpty()) {
                return;
            }

            for (String domain : resourceManager.getResourceDomains()) {
                FallbackResourceManager fallbackResourceManager = domainManagers.get(domain);
                if (fallbackResourceManager == null) {
                    continue;
                }
                List<IResourcePack> packs = ((AccessorFallbackResourceManager) fallbackResourceManager)
                    .guidenh$getResourcePacks();
                if (packs != null) {
                    for (IResourcePack pack : packs) {
                        resourcePacks.add(pack);
                        domainsByPack.computeIfAbsent(pack, ignored -> new LinkedHashSet<>())
                            .add(domain);
                    }
                }
            }
        } catch (RuntimeException e) {
            GuideDebugLog.warnAlways(
                "[GuideNH] [DataDrivenGuideLoader] Failed to inspect the currently loaded resource manager packs",
                e);
        }
    }

    private static Map<IResourcePack, Set<String>> freezeDomainsByPack(
        IdentityHashMap<IResourcePack, LinkedHashSet<String>> domainsByPack) {
        if (domainsByPack.isEmpty()) {
            return Map.of();
        }

        var result = new IdentityHashMap<IResourcePack, Set<String>>();
        for (var entry : domainsByPack.entrySet()) {
            result.put(entry.getKey(), Set.copyOf(entry.getValue()));
        }
        return Collections.unmodifiableMap(result);
    }

    private static Set<String> getResourceDomains(IResourcePack resourcePack) {
        Set<String> cachedDomains = lastResourceManagerDomainsByPack.get(resourcePack);
        return cachedDomains != null ? cachedDomains : resourcePack.getResourceDomains();
    }

    public static void scanResourcePack(IResourcePack resourcePack,
        Map<ResourceLocation, LinkedHashSet<String>> discoveredLanguages) {
        var resourcePackRoot = getLooseResourcePackRoot(resourcePack);
        if (resourcePackRoot == null || !resourcePackRoot.exists()) {
            return;
        }

        if (resourcePackRoot.isDirectory()) {
            scanResourcePackFolder(resourcePack, resourcePackRoot, discoveredLanguages);
        } else {
            scanResourcePackZip(resourcePackRoot, discoveredLanguages);
        }
    }

    public static void scanPagePaths(IResourcePack resourcePack, String prefix, Set<String> pagePaths) {
        var resourcePackRoot = getLooseResourcePackRoot(resourcePack);
        if (resourcePackRoot == null || !resourcePackRoot.exists()) {
            return;
        }

        if (resourcePackRoot.isDirectory()) {
            scanFolderPagePaths(resourcePackRoot, prefix, pagePaths);
        } else {
            scanZipPagePaths(resourcePackRoot, prefix, pagePaths);
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
            GuideDebugLog.warnAlways(
                "[GuideNH] [DataDrivenGuideLoader] Failed to resolve the backing file for resource pack {}",
                resourcePack.getPackName(),
                e);
            return null;
        }
    }

    public static File getLooseResourcePackRoot(IResourcePack resourcePack) {
        File resourcePackFile = getResourcePackFile(resourcePack);
        if (resourcePackFile != null) {
            return resourcePackFile;
        }

        Field field = findLooseRootField(resourcePack.getClass());
        if (field == null) {
            return null;
        }

        try {
            Object value = field.get(resourcePack);
            if (value instanceof Path path) {
                return path.toFile();
            }
            if (value instanceof File file) {
                return file;
            }
        } catch (IllegalAccessException e) {
            GuideDebugLog.warnAlways(
                "[GuideNH] [DataDrivenGuideLoader] Failed to resolve the directory root for resource pack {}",
                resourcePack.getPackName(),
                e);
        }
        return null;
    }

    private static Field findLooseRootField(Class<?> resourcePackClass) {
        synchronized (LOOSE_ROOT_FIELDS) {
            if (LOOSE_ROOT_FIELDS.containsKey(resourcePackClass)) {
                return LOOSE_ROOT_FIELDS.get(resourcePackClass);
            }

            Field field = discoverLooseRootField(resourcePackClass);
            LOOSE_ROOT_FIELDS.put(resourcePackClass, field);
            return field;
        }
    }

    private static Field discoverLooseRootField(Class<?> resourcePackClass) {
        Class<?> current = resourcePackClass;
        while (current != null && current != Object.class) {
            for (Field field : current.getDeclaredFields()) {
                Class<?> type = field.getType();
                if (type == Path.class || type == File.class) {
                    field.setAccessible(true);
                    return field;
                }
            }
            current = current.getSuperclass();
        }
        return null;
    }

    public static byte[] readBytes(IResourcePack resourcePack, ResourceLocation resourceLocation) {
        if (!resourcePack.resourceExists(resourceLocation)) {
            return readLooseBytes(resourcePack, resourceLocation);
        }
        try (var input = resourcePack.getInputStream(resourceLocation)) {
            return GuideResourceAccess.readFully(input);
        } catch (IOException e) {
            GuideDebugLog.warnAlways(
                "[GuideNH] [DataDrivenGuideLoader] Failed to read resource {} from resource pack {}",
                resourceLocation,
                resourcePack.getPackName(),
                e);
            return null;
        }
    }

    public static byte[] readLooseBytes(IResourcePack resourcePack, ResourceLocation resourceLocation) {
        File looseRoot = getLooseResourcePackRoot(resourcePack);
        if (looseRoot == null || !looseRoot.isDirectory()) {
            return null;
        }

        Path root = looseRoot.toPath()
            .toAbsolutePath()
            .normalize();
        for (String candidate : looseResourceCandidates(resourceLocation)) {
            Path path = root.resolve(candidate.replace('/', File.separatorChar))
                .normalize();
            if (!path.startsWith(root) || !Files.isRegularFile(path)) {
                continue;
            }
            try {
                return Files.readAllBytes(path);
            } catch (IOException e) {
                GuideDebugLog.warnAlways(
                    "[GuideNH] [DataDrivenGuideLoader] Failed to read loose resource {} from resource pack {}",
                    path,
                    resourcePack.getPackName(),
                    e);
                return null;
            }
        }
        return null;
    }

    private static List<String> looseResourceCandidates(ResourceLocation resourceLocation) {
        String namespace = resourceLocation.getResourceDomain();
        String path = resourceLocation.getResourcePath();
        var candidates = new ArrayList<String>();
        candidates.add("assets/" + namespace + "/" + path);
        candidates.add(namespace + "/" + path);
        if (path.startsWith(AUTO_GUIDE_FOLDER + "/")) {
            candidates.add(path);
        }
        return candidates;
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
        if (namespaceDirs != null) {
            for (var namespaceDir : namespaceDirs) {
                scanResourcePackFolderNamespace(resourcePackRoot, namespaceDir.getName(), discoveredLanguages);
            }
        }

        var looseNamespaceDirs = resourcePackRoot.listFiles(File::isDirectory);
        if (looseNamespaceDirs == null) {
            return;
        }
        for (var namespaceDir : looseNamespaceDirs) {
            if ("assets".equals(namespaceDir.getName())) {
                continue;
            }
            scanResourcePackFolderNamespace(resourcePackRoot, namespaceDir.getName(), discoveredLanguages);
        }
    }

    private static void scanResourcePackFolder(IResourcePack resourcePack, File resourcePackRoot,
        Map<ResourceLocation, LinkedHashSet<String>> discoveredLanguages) {
        for (String namespace : getResourceDomains(resourcePack)) {
            scanResourcePackFolderNamespace(resourcePackRoot, namespace, discoveredLanguages);
        }
    }

    private static void scanResourcePackFolderNamespace(File resourcePackRoot, String namespace,
        Map<ResourceLocation, LinkedHashSet<String>> discoveredLanguages) {
        if (!isValidNamespace(namespace)) {
            return;
        }
        scanResourcePackFolderNamespaceRoot(
            namespace,
            new File(resourcePackRoot, toFolderPrefix(namespace, AUTO_GUIDE_FOLDER).replace('/', File.separatorChar)),
            discoveredLanguages);
        scanResourcePackFolderNamespaceRoot(
            namespace,
            new File(
                resourcePackRoot,
                toLooseFolderPrefix(namespace, AUTO_GUIDE_FOLDER).replace('/', File.separatorChar)),
            discoveredLanguages);
        if (AUTO_GUIDE_FOLDER.equals(namespace)) {
            scanResourcePackFolderNamespaceRoot(
                namespace,
                new File(resourcePackRoot, AUTO_GUIDE_FOLDER.replace('/', File.separatorChar)),
                discoveredLanguages);
        }
    }

    private static void scanResourcePackFolderNamespaceRoot(String namespace, File guideRootDir,
        Map<ResourceLocation, LinkedHashSet<String>> discoveredLanguages) {
        if (!guideRootDir.isDirectory()) {
            return;
        }
        var languageDirs = guideRootDir.listFiles(File::isDirectory);
        if (languageDirs == null) {
            return;
        }
        for (var languageDir : languageDirs) {
            var languageFolder = languageDir.getName();
            if (!isLanguageFolder(languageFolder)) {
                continue;
            }

            if (!containsMarkdownFiles(languageDir)) {
                continue;
            }

            var guideId = new ResourceLocation(namespace, AUTO_GUIDE_FOLDER);
            discoveredLanguages.computeIfAbsent(guideId, ignored -> new LinkedHashSet<>())
                .add(toLanguageCode(languageFolder));
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
            GuideDebugLog.warnAlways(
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
            GuideDebugLog.warnAlways(
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

    public static String toLooseFolderPrefix(String namespace, String folder) {
        return namespace + "/" + folder + "/";
    }

    private static List<String> pagePathPrefixes(String namespace, String folder) {
        var prefixes = new ArrayList<String>(3);
        prefixes.add(toFolderPrefix(namespace, folder));
        prefixes.add(toLooseFolderPrefix(namespace, folder));
        if (folder.equals(namespace)) {
            prefixes.add(folder + "/");
        }
        return prefixes;
    }

    private static boolean isValidNamespace(String namespace) {
        return namespace != null && !namespace.isEmpty() && namespace.indexOf('/') < 0 && namespace.indexOf('\\') < 0;
    }

    private static List<IResourcePack> toList(Iterable<? extends IResourcePack> resourcePacks) {
        var result = new ArrayList<IResourcePack>();
        for (IResourcePack resourcePack : resourcePacks) {
            result.add(resourcePack);
        }
        return result;
    }

    public static void clearCaches() {
        lastGuideLanguageDiscovery = GuideLanguageDiscoverySnapshot.empty();
    }

    private static Map<ResourceLocation, Set<String>> freezeDiscoveredLanguages(
        Map<ResourceLocation, LinkedHashSet<String>> discoveredLanguages) {
        if (discoveredLanguages.isEmpty()) {
            return Map.of();
        }

        var frozen = new LinkedHashMap<ResourceLocation, Set<String>>(discoveredLanguages.size());
        for (var entry : discoveredLanguages.entrySet()) {
            frozen.put(entry.getKey(), Set.copyOf(entry.getValue()));
        }
        return Collections.unmodifiableMap(frozen);
    }

    private record GuideLanguageDiscoverySnapshot(List<IResourcePack> resourcePacks,
        Map<ResourceLocation, Set<String>> discoveredLanguages) {

        private static GuideLanguageDiscoverySnapshot empty() {
            return new GuideLanguageDiscoverySnapshot(List.of(), Map.of());
        }

        private boolean matches(List<IResourcePack> otherResourcePacks) {
            return !resourcePacks.isEmpty() && resourcePacks.equals(otherResourcePacks);
        }
    }
}
