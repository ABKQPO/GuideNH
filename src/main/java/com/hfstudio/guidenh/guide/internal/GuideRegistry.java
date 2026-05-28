package com.hfstudio.guidenh.guide.internal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.util.ResourceLocation;

import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.config.ModConfig;
import com.hfstudio.guidenh.guide.compiler.ParsedGuidePage;
import com.hfstudio.guidenh.guide.navigation.NavigationTree;

import cpw.mods.fml.common.FMLLog;

/**
 * Internal registry for Guides.
 */
public class GuideRegistry {

    public static final ConcurrentHashMap<ResourceLocation, MutableGuide> guides = new ConcurrentHashMap<>();

    public static final Map<ResourceLocation, MutableGuide> dataDrivenGuides = new HashMap<>();

    // Merged between data-driven and in-code guides
    public static volatile Map<ResourceLocation, MutableGuide> mergedGuides = Map.of();
    private static volatile NavigationTree mergedNavigationTree = new NavigationTree();
    private static volatile long navigationRevision = 1L;
    private static volatile long cachedNavigationRevision = Long.MIN_VALUE;

    public static Collection<MutableGuide> getAll() {
        return mergedGuides.values();
    }

    /**
     * Return guides registered through code.
     */
    public static Collection<MutableGuide> getStaticGuides() {
        return List.copyOf(new ArrayList<>(guides.values()));
    }

    public static @Nullable MutableGuide getById(ResourceLocation id) {
        return mergedGuides.get(id);
    }

    public static NavigationTree getMergedNavigationTree() {
        if (cachedNavigationRevision == navigationRevision) {
            return mergedNavigationTree;
        }

        synchronized (GuideRegistry.class) {
            if (cachedNavigationRevision != navigationRevision) {
                var guides = new ArrayList<>(mergedGuides.values());
                guides.sort(
                    Comparator.comparing(
                        guide -> guide.getId()
                            .toString()));
                mergedNavigationTree = NavigationTree.buildMerged(guides);
                cachedNavigationRevision = navigationRevision;
            }
            return mergedNavigationTree;
        }
    }

    public static long getNavigationRevision() {
        return navigationRevision;
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
        int previousCount = dataDrivenGuides.size();
        for (MutableGuide existingGuide : new ArrayList<>(dataDrivenGuides.values())) {
            if (existingGuide != null) {
                existingGuide.close();
            }
        }
        dataDrivenGuides.clear();
        dataDrivenGuides.putAll(guides);
        if (ModConfig.debug.enableDebugMode) {
            FMLLog.getLogger()
                .info(
                    "[GuideNH] [GuideRegistry] Replaced {} data-driven guides with {} freshly loaded guides",
                    previousCount,
                    dataDrivenGuides.size());
        }

        rebuildGuides();
    }

    /**
     * Update parsed pages for a specific guide after a resource reload.
     */
    public static void updatePages(ResourceLocation guideId, Map<ResourceLocation, ParsedGuidePage> pages) {
        updatePages(guideId, pages, true);
    }

    public static void updatePages(ResourceLocation guideId, Map<ResourceLocation, ParsedGuidePage> pages,
        boolean invalidateMergedNavigationTree) {
        var guide = mergedGuides.get(guideId);
        if (guide != null) {
            guide.setPages(pages, invalidateMergedNavigationTree);
        }
    }

    public static void invalidateMergedNavigationTree() {
        synchronized (GuideRegistry.class) {
            navigationRevision++;
            cachedNavigationRevision = Long.MIN_VALUE;
            mergedNavigationTree = new NavigationTree();
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
            overridden.sort(Comparator.comparing(ResourceLocation::toString));
            if (ModConfig.debug.enableDebugMode) {
                FMLLog.getLogger()
                    .info(
                        "[GuideNH] [GuideRegistry] The following guides are overridden in resource packs: {}",
                        overridden);
            }
        }

        GuideRegistry.mergedGuides = Map.copyOf(merged);
        invalidateMergedNavigationTree();
    }
}
