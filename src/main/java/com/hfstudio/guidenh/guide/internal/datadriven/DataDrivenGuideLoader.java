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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hfstudio.guidenh.guide.Guide;
import com.hfstudio.guidenh.guide.internal.MutableGuide;
import com.hfstudio.guidenh.guide.internal.util.LangUtil;

import cpw.mods.fml.client.FMLClientHandler;
import cpw.mods.fml.common.ObfuscationReflectionHelper;

public class DataDrivenGuideLoader {

    private static final Logger LOG = LoggerFactory.getLogger(DataDrivenGuideLoader.class);
    private static final String AUTO_GUIDE_FOLDER = "guidenh";
    private static final String LANGUAGE_FOLDER_PREFIX = "_";

    private DataDrivenGuideLoader() {}

    public static Map<ResourceLocation, MutableGuide> load() {
        var discoveredLanguages = new LinkedHashMap<ResourceLocation, LinkedHashSet<String>>();

        for (var resourcePack : getActiveResourcePacks()) {
            scanResourcePack(resourcePack, discoveredLanguages);
        }

        var guides = new LinkedHashMap<ResourceLocation, MutableGuide>();
        for (var entry : discoveredLanguages.entrySet()) {
            var guideId = entry.getKey();
            var builder = Guide.builder(guideId)
                .register(false)
                .folder(AUTO_GUIDE_FOLDER)
                .defaultLanguage(selectDefaultLanguage(entry.getValue()));
            guides.put(guideId, (MutableGuide) builder.build());
        }

        return guides;
    }

    public static Set<String> discoverPagePaths(ResourceLocation guideId, String folder) {
        var pagePaths = new LinkedHashSet<String>();
        var prefix = toFolderPrefix(guideId.getResourceDomain(), folder);

        for (var resourcePack : getActiveResourcePacks()) {
            scanPagePaths(resourcePack, prefix, pagePaths);
        }

        return pagePaths;
    }

    private static List<IResourcePack> getActiveResourcePacks() {
        var resourcePacks = new LinkedHashSet<IResourcePack>();

        try {
            @SuppressWarnings("unchecked")
            var basePacks = (List<IResourcePack>) ObfuscationReflectionHelper
                .getPrivateValue(FMLClientHandler.class, FMLClientHandler.instance(), "resourcePackList");
            if (basePacks != null) {
                resourcePacks.addAll(basePacks);
            }
        } catch (RuntimeException e) {
            LOG.warn("Failed to inspect the currently loaded base resource packs", e);
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

    private static void scanResourcePack(IResourcePack resourcePack,
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

    private static void scanPagePaths(IResourcePack resourcePack, String prefix, Set<String> pagePaths) {
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

    private static File getResourcePackFile(IResourcePack resourcePack) {
        if (!(resourcePack instanceof AbstractResourcePack)) {
            return null;
        }

        try {
            return ObfuscationReflectionHelper.getPrivateValue(
                AbstractResourcePack.class,
                (AbstractResourcePack) resourcePack,
                "resourcePackFile",
                "field_110597_b");
        } catch (RuntimeException e) {
            LOG.warn("Failed to resolve the backing file for resource pack {}", resourcePack.getPackName(), e);
            return null;
        }
    }

    private static void scanResourcePackFolder(File resourcePackRoot,
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

    private static void scanResourcePackZip(File resourcePackFile,
        Map<ResourceLocation, LinkedHashSet<String>> discoveredLanguages) {
        try (var zip = new ZipFile(resourcePackFile)) {
            var entries = zip.entries();
            while (entries.hasMoreElements()) {
                var entry = entries.nextElement();
                if (entry.isDirectory()) {
                    continue;
                }

                var path = entry.getName();
                if (!path.endsWith(".md") || !path.startsWith("assets/")) {
                    continue;
                }

                var parts = path.split("/", 5);
                if (parts.length < 5 || !AUTO_GUIDE_FOLDER.equals(parts[2]) || !isLanguageFolder(parts[3])) {
                    continue;
                }

                var guideId = new ResourceLocation(parts[1], AUTO_GUIDE_FOLDER);
                discoveredLanguages.computeIfAbsent(guideId, ignored -> new LinkedHashSet<>())
                    .add(toLanguageCode(parts[3]));
            }
        } catch (IOException e) {
            LOG.warn("Failed to scan guide pages from resource pack {}", resourcePackFile.getAbsolutePath(), e);
        }
    }

    private static void scanFolderPagePaths(File resourcePackRoot, String prefix, Set<String> pagePaths) {
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

    private static void scanZipPagePaths(File resourcePackFile, String prefix, Set<String> pagePaths) {
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
            LOG.warn("Failed to scan guide pages from resource pack {}", resourcePackFile.getAbsolutePath(), e);
        }
    }

    private static void collectMarkdownPaths(File directory, String relativePath, Set<String> pagePaths) {
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

    private static boolean containsMarkdownFiles(File directory) {
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

    private static boolean isLanguageFolder(String name) {
        return name.startsWith(LANGUAGE_FOLDER_PREFIX) && LangUtil.isLanguageCode(name.substring(1));
    }

    private static String toLanguageCode(String folderName) {
        return LangUtil.normalizeLanguage(folderName.substring(LANGUAGE_FOLDER_PREFIX.length()));
    }

    private static String selectDefaultLanguage(Set<String> languages) {
        if (languages.contains("en_us")) {
            return "en_us";
        }

        return languages.stream()
            .min(Comparator.naturalOrder())
            .orElse("en_us");
    }

    private static String toFolderPrefix(String namespace, String folder) {
        return "assets/" + namespace + "/" + folder + "/";
    }
}
