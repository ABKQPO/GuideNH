package com.hfstudio.guidenh.guide.internal.localization;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import net.minecraft.client.resources.IResourcePack;
import net.minecraft.util.StringTranslate;

import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.config.ModConfig;
import com.hfstudio.guidenh.guide.internal.datadriven.DataDrivenGuideLoader;
import com.hfstudio.guidenh.guide.internal.util.LangUtil;

import cpw.mods.fml.common.FMLLog;

public class GuideResourceLanguageIndex {

    private static final Map<String, Map<String, String>> VALUES_BY_LANGUAGE = new ConcurrentHashMap<>();

    private GuideResourceLanguageIndex() {}

    public static void clear() {
        VALUES_BY_LANGUAGE.clear();
    }

    public static @Nullable String getValue(String language, String key) {
        if (key == null || key.isEmpty()) {
            return null;
        }
        return VALUES_BY_LANGUAGE
            .computeIfAbsent(LangUtil.normalizeLanguage(language), GuideResourceLanguageIndex::load)
            .get(key);
    }

    private static Map<String, String> load(String normalizedLanguage) {
        long startedAt = System.nanoTime();
        Map<String, String> merged = new LinkedHashMap<>();
        var activeResourcePacks = DataDrivenGuideLoader.getActiveResourcePacks();
        for (IResourcePack resourcePack : activeResourcePacks) {
            loadResourcePackLanguage(resourcePack, normalizedLanguage, merged);
        }
        long totalNs = System.nanoTime() - startedAt;
        if (ModConfig.debug.enableDebugMode) {
            FMLLog.getLogger()
                .info(
                    "[GuideNH] [GuideResourceLanguageIndex] Loaded {} lang entries for language {} from {} resource packs in {} ns",
                    merged.size(),
                    normalizedLanguage,
                    activeResourcePacks.size(),
                    totalNs);
        }
        return merged.isEmpty() ? Map.of() : Map.copyOf(merged);
    }

    private static void loadResourcePackLanguage(IResourcePack resourcePack, String normalizedLanguage,
        Map<String, String> target) {
        File resourcePackFile = DataDrivenGuideLoader.getResourcePackFile(resourcePack);
        if (resourcePackFile == null || !resourcePackFile.exists()) {
            return;
        }
        if (resourcePackFile.isDirectory()) {
            loadDirectoryLanguage(resourcePackFile, normalizedLanguage, target);
            return;
        }
        loadZipLanguage(resourcePackFile, normalizedLanguage, target);
    }

    private static void loadDirectoryLanguage(File resourcePackRoot, String normalizedLanguage,
        Map<String, String> target) {
        File assetsDir = new File(resourcePackRoot, "assets");
        File[] namespaceDirs = assetsDir.listFiles(File::isDirectory);
        if (namespaceDirs == null) {
            return;
        }

        for (File namespaceDir : namespaceDirs) {
            File langDir = new File(namespaceDir, "lang");
            if (!langDir.isDirectory()) {
                continue;
            }
            loadDirectoryLanguageEntries(langDir, normalizedLanguage, target);
        }
    }

    private static void loadDirectoryLanguageEntries(File directory, String normalizedLanguage,
        Map<String, String> target) {
        File[] children = directory.listFiles();
        if (children == null) {
            return;
        }

        for (File child : children) {
            if (child.isDirectory()) {
                loadDirectoryLanguageEntries(child, normalizedLanguage, target);
                continue;
            }
            if (!isMatchingLangFile(child.getName(), normalizedLanguage)) {
                continue;
            }
            try (InputStream input = new FileInputStream(child)) {
                target.putAll(StringTranslate.parseLangFile(input));
            } catch (IOException e) {
                FMLLog.getLogger()
                    .warn(
                        "[GuideNH] [GuideResourceLanguageIndex] Failed to read lang file {}",
                        child.getAbsolutePath(),
                        e);
            }
        }
    }

    private static void loadZipLanguage(File resourcePackFile, String normalizedLanguage, Map<String, String> target) {
        try (ZipFile zip = new ZipFile(resourcePackFile)) {
            Enumeration<? extends ZipEntry> entries = zip.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                if (entry.isDirectory()) {
                    continue;
                }
                String path = entry.getName();
                if (!path.startsWith("assets/") || !path.contains("/lang/")) {
                    continue;
                }
                int fileNameStart = path.lastIndexOf('/') + 1;
                if (fileNameStart <= 0 || fileNameStart >= path.length()) {
                    continue;
                }
                if (!isMatchingLangFile(path.substring(fileNameStart), normalizedLanguage)) {
                    continue;
                }
                try (InputStream input = zip.getInputStream(entry)) {
                    target.putAll(StringTranslate.parseLangFile(input));
                }
            }
        } catch (IOException e) {
            FMLLog.getLogger()
                .warn(
                    "[GuideNH] [GuideResourceLanguageIndex] Failed to scan lang entries from resource pack {}",
                    resourcePackFile.getAbsolutePath(),
                    e);
        }
    }

    private static boolean isMatchingLangFile(String fileName, String normalizedLanguage) {
        if (!fileName.endsWith(".lang")) {
            return false;
        }
        String baseName = fileName.substring(0, fileName.length() - 5);
        return LangUtil.normalizeLanguage(baseName)
            .equals(normalizedLanguage);
    }
}
