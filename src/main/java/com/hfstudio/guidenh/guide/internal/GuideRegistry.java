package com.hfstudio.guidenh.guide.internal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.util.ResourceLocation;

import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hfstudio.guidenh.guide.compiler.ParsedGuidePage;

/**
 * Internal registry for Guides.
 */
public class GuideRegistry {

    public static final Logger LOG = LoggerFactory.getLogger(GuideRegistry.class);

    public static final ConcurrentHashMap<ResourceLocation, MutableGuide> guides = new ConcurrentHashMap<>();

    public static final Map<ResourceLocation, MutableGuide> dataDrivenGuides = new HashMap<>();

    // Merged between data-driven and in-code guides
    public static volatile Map<ResourceLocation, MutableGuide> mergedGuides = Collections.emptyMap();

    public static Collection<MutableGuide> getAll() {
        return mergedGuides.values();
    }

    /**
     * Return guides registered through code.
     */
    public static Collection<MutableGuide> getStaticGuides() {
        return Collections.unmodifiableList(new ArrayList<>(guides.values()));
    }

    public static @Nullable MutableGuide getById(ResourceLocation id) {
        return mergedGuides.get(id);
    }

    /**
     * Register a static guide (implemented in code).
     */
    public static void registerStatic(MutableGuide guide) {
        if (guides.putIfAbsent(guide.getId(), guide) != null) {
            throw new IllegalStateException("There is already a Guide registered with id " + guide.getId());
        }

        rebuildGuides();
    }

    /**
     * Remove a static guide (implemented in code). This is primarily for testing purposes.
     */
    public static void unregisterStatic(MutableGuide guide) {
        if (guides.remove(guide.getId(), guide)) {
            rebuildGuides();
        }
    }

    /**
     * Register all dynamic guides (defined in resource packs), which replaces all previously available dynamic guides.
     */
    public static void setDataDriven(Map<ResourceLocation, MutableGuide> guides) {
        dataDrivenGuides.clear();
        dataDrivenGuides.putAll(guides);

        rebuildGuides();
    }

    /**
     * Update parsed pages for a specific guide after a resource reload.
     */
    public static void updatePages(ResourceLocation guideId, Map<ResourceLocation, ParsedGuidePage> pages) {
        var guide = mergedGuides.get(guideId);
        if (guide != null) {
            guide.setPages(pages);
        }
    }

    public static void rebuildGuides() {
        var merged = new HashMap<>(guides);
        var overridden = new ArrayList<ResourceLocation>();
        for (var entry : dataDrivenGuides.entrySet()) {
            if (merged.put(entry.getKey(), entry.getValue()) != null) {
                overridden.add(entry.getKey());
            }
        }

        if (!overridden.isEmpty()) {
            Collections.sort(overridden, Comparator.comparing(ResourceLocation::toString));
            LOG.info("The following guides are overridden in resource packs: {}", overridden);
        }

        GuideRegistry.mergedGuides = Collections.unmodifiableMap(new HashMap<>(merged));
    }
}
