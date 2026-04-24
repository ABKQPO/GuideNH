package com.hfstudio.guidenh.guide.internal.util;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import javax.annotation.Nullable;

import net.minecraft.client.Minecraft;
import net.minecraft.util.ResourceLocation;

public final class LangUtil {

    public static final Set<String> SUPPORTED_LANGUAGES = Collections
        .unmodifiableSet(new HashSet<>(Arrays.asList("en_us", "zh_cn")));

    private LangUtil() {}

    public static Set<String> getValidLanguages() {
        return SUPPORTED_LANGUAGES;
    }

    public static String getCurrentLanguage() {
        var client = Minecraft.getMinecraft();
        if (client != null && client.gameSettings != null) {
            return client.gameSettings.language.toLowerCase(Locale.ROOT);
        }
        return "en_us";
    }

    public static ResourceLocation getTranslatedAsset(ResourceLocation assetId, String language) {
        return new ResourceLocation(assetId.getResourceDomain(), "_" + language + "/" + assetId.getResourcePath());
    }

    public static ResourceLocation stripLangFromPageId(ResourceLocation pageId, Set<String> supportedLanguages) {
        String path = pageId.getResourcePath();

        int firstSep = path.indexOf("/");
        if (firstSep == -1) {
            return pageId; // No directory, bare filename
        }

        if (path.charAt(0) != '_') {
            return pageId; // First folder doesn't start with "_"
        }

        // There has to be content after the slash since empty paths are not allowed
        if (firstSep + 1 >= path.length()) {
            return pageId;
        }

        var potentialLanguage = path.substring(1, firstSep);
        if (supportedLanguages.contains(potentialLanguage)) {
            return new ResourceLocation(pageId.getResourceDomain(), path.substring(firstSep + 1));
        }

        return pageId;
    }

    @Nullable
    public static String getLangFromPageId(ResourceLocation pageId, Set<String> supportedLanguages) {
        String path = pageId.getResourcePath();

        int firstSep = path.indexOf("/");
        if (firstSep == -1) {
            return null; // No directory, bare filename
        }

        if (path.charAt(0) != '_') {
            return null; // First folder doesn't start with "_"
        }

        // There has to be content after the slash since empty paths are not allowed
        if (firstSep + 1 >= path.length()) {
            return null;
        }

        var potentialLanguage = path.substring(1, firstSep);
        if (supportedLanguages.contains(potentialLanguage)) {
            return potentialLanguage;
        }

        return null;
    }

}
