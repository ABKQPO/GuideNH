package com.hfstudio.guidenh.guide.internal;

import java.util.LinkedHashSet;
import java.util.Set;

import net.minecraft.util.ResourceLocation;

import org.jetbrains.annotations.Nullable;

import com.github.bsideup.jabel.Desugar;
import com.hfstudio.guidenh.guide.PageAnchor;

import cpw.mods.fml.common.FMLLog;

public class GuideStartupOptions {

    public GuideStartupOptions() {}

    @Desugar
    public record ShowOnStartup(ResourceLocation guideId, @Nullable PageAnchor anchor) {}

    public static @Nullable ShowOnStartup parseShowOnStartup(@Nullable String value) {
        if (value == null) {
            return null;
        }

        String trimmedValue = value.trim();
        if (trimmedValue.isEmpty()) {
            return null;
        }

        try {
            int anchorSeparator = trimmedValue.indexOf('!');
            String guidePart = anchorSeparator >= 0 ? trimmedValue.substring(0, anchorSeparator) : trimmedValue;
            ResourceLocation guideId = new ResourceLocation(guidePart);
            if (anchorSeparator < 0 || anchorSeparator == trimmedValue.length() - 1) {
                return new ShowOnStartup(guideId, null);
            }

            return new ShowOnStartup(guideId, parseStartupAnchor(guideId, trimmedValue.substring(anchorSeparator + 1)));
        } catch (RuntimeException e) {
            FMLLog.getLogger()
                .error("[GuideNH] [GuideStartupOptions] Failed to parse guideme.showOnStartup='{}'", trimmedValue, e);
            return null;
        }
    }

    public static Set<ResourceLocation> parseValidateAtStartup(@Nullable String value) {
        Set<ResourceLocation> result = new LinkedHashSet<>();
        if (value == null) {
            return result;
        }

        String trimmedValue = value.trim();
        if (trimmedValue.isEmpty()) {
            return result;
        }

        int start = 0;
        int length = trimmedValue.length();
        while (start <= length) {
            int end = trimmedValue.indexOf(',', start);
            if (end < 0) {
                end = length;
            }
            String trimmedToken = trimmedValue.substring(start, end)
                .trim();
            if (!trimmedToken.isEmpty()) {
                try {
                    result.add(new ResourceLocation(trimmedToken));
                } catch (RuntimeException e) {
                    FMLLog.getLogger()
                        .error(
                            "[GuideNH] [GuideStartupOptions] Failed to parse validateAtStartup guide id '{}'",
                            trimmedToken,
                            e);
                }
            }
            if (end == length) {
                break;
            }
            start = end + 1;
        }

        return result;
    }

    public static PageAnchor parseStartupAnchor(ResourceLocation guideId, String rawAnchor) {
        return PageAnchor.parse(resolveRelativePageId(guideId, rawAnchor));
    }

    public static String resolveRelativePageId(ResourceLocation guideId, String rawAnchor) {
        int fragmentSeparator = rawAnchor.indexOf('#');
        String pagePart = fragmentSeparator >= 0 ? rawAnchor.substring(0, fragmentSeparator) : rawAnchor;
        String fragmentPart = fragmentSeparator >= 0 ? rawAnchor.substring(fragmentSeparator) : "";
        if (pagePart.contains(":")) {
            return rawAnchor;
        }
        return guideId.getResourceDomain() + ":" + pagePart + fragmentPart;
    }
}
