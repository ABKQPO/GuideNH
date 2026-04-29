package com.hfstudio.guidenh.guide.internal;

import java.util.LinkedHashSet;
import java.util.Set;

import net.minecraft.util.ResourceLocation;

import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.bsideup.jabel.Desugar;
import com.hfstudio.guidenh.guide.PageAnchor;

public class GuideStartupOptions {

    public static final Logger LOG = LoggerFactory.getLogger(GuideStartupOptions.class);

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
            String[] parts = trimmedValue.split("!", 2);
            ResourceLocation guideId = new ResourceLocation(parts[0]);
            if (parts.length == 1 || parts[1].isEmpty()) {
                return new ShowOnStartup(guideId, null);
            }

            return new ShowOnStartup(guideId, parseStartupAnchor(guideId, parts[1]));
        } catch (RuntimeException e) {
            LOG.error("Failed to parse guideme.showOnStartup='{}'", trimmedValue, e);
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

        for (String token : trimmedValue.split(",")) {
            String trimmedToken = token.trim();
            if (trimmedToken.isEmpty()) {
                continue;
            }

            try {
                result.add(new ResourceLocation(trimmedToken));
            } catch (RuntimeException e) {
                LOG.error("Failed to parse validateAtStartup guide id '{}'", trimmedToken, e);
            }
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
