package com.hfstudio.guidenh.guide.internal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.Set;

import net.minecraft.util.ResourceLocation;

import com.hfstudio.guidenh.config.ModConfig;

public class GuideBookmarkStore {

    public Set<ResourceLocation> load() {
        return parse(ModConfig.ui.guideBookmarks);
    }

    public void save(Set<ResourceLocation> pageIds) {
        ModConfig.ui.guideBookmarks = serialize(pageIds);
        ModConfig.save();
    }

    public Set<ResourceLocation> parse(String raw) {
        if (raw == null || raw.trim()
            .isEmpty()) {
            return Collections.emptySet();
        }
        Set<ResourceLocation> result = new LinkedHashSet<ResourceLocation>();
        String[] tokens = raw.split("\\|");
        for (String token : tokens) {
            String value = token.trim();
            if (value.isEmpty()) {
                continue;
            }
            try {
                result.add(new ResourceLocation(value));
            } catch (IllegalArgumentException ignored) {}
        }
        return result;
    }

    public String serialize(Set<ResourceLocation> pageIds) {
        if (pageIds == null || pageIds.isEmpty()) {
            return "";
        }
        var ordered = new ArrayList<ResourceLocation>(pageIds);
        Collections.sort(ordered, Comparator.comparing(ResourceLocation::toString));
        StringBuilder builder = new StringBuilder();
        for (ResourceLocation pageId : ordered) {
            if (pageId == null) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append('|');
            }
            builder.append(pageId);
        }
        return builder.toString();
    }
}
