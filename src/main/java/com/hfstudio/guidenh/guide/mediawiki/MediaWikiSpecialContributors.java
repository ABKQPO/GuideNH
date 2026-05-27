package com.hfstudio.guidenh.guide.mediawiki;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.IResource;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.StatCollector;

import com.github.bsideup.jabel.Desugar;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.hfstudio.guidenh.guide.Guide;

public class MediaWikiSpecialContributors {

    private static final Gson GSON = new Gson();
    private static final ResourceLocation CONTRIBUTORS_ID = new ResourceLocation(
        "guidenh",
        "mediawiki/contributors.json");
    private static volatile List<ContributorEntry> cachedContributors;

    public List<ContributorEntry> load(Guide guide) {
        List<ContributorEntry> cached = cachedContributors;
        if (cached != null) {
            return cached;
        }
        byte[] data = loadContributorsBytes();
        if (data == null || data.length == 0) {
            return Collections.emptyList();
        }

        List<ContributorEntry> contributors = GSON
            .fromJson(new String(data, StandardCharsets.UTF_8), new TypeToken<List<ContributorEntry>>() {}.getType());
        if (contributors == null || contributors.isEmpty()) {
            return Collections.emptyList();
        }

        ArrayList<ContributorEntry> sanitized = new ArrayList<>(contributors.size());
        for (ContributorEntry contributor : contributors) {
            if (contributor == null || contributor.nameKey() == null
                || contributor.nameKey()
                    .trim()
                    .isEmpty()) {
                continue;
            }
            sanitized.add(contributor);
        }
        List<ContributorEntry> immutable = Collections.unmodifiableList(sanitized);
        cachedContributors = immutable;
        return immutable;
    }

    public String resolveName(ContributorEntry contributor) {
        return resolveKey(contributor != null ? contributor.nameKey() : null);
    }

    public String resolveRole(ContributorEntry contributor) {
        return resolveKey(contributor != null ? contributor.roleKey() : null);
    }

    private String resolveKey(String key) {
        if (key == null || key.trim()
            .isEmpty()) {
            return "";
        }
        String translated = StatCollector.translateToLocal(key);
        return translated != null && !translated.equals(key) ? translated : key;
    }

    private byte @org.jetbrains.annotations.Nullable [] loadContributorsBytes() {
        try {
            IResource resource = Minecraft.getMinecraft()
                .getResourceManager()
                .getResource(CONTRIBUTORS_ID);
            try (InputStream stream = resource.getInputStream()) {
                return org.apache.commons.io.IOUtils.toByteArray(stream);
            }
        } catch (IOException ignored) {
            return null;
        }
    }

    @Desugar
    public record ContributorEntry(String nameKey, String roleKey, String link) {}
}
