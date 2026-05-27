package com.hfstudio.guidenh.guide.compiler;

import java.util.List;
import java.util.Map;

import net.minecraft.util.ResourceLocation;

import org.jetbrains.annotations.Nullable;

/**
 * Inserts a page into the navigation tree. Null parent means top-level category.
 * <p>
 * When {@code requiredMods} is non-null and non-empty, the page is only included in the navigation
 * tree and page indices when every listed mod ID is currently loaded.
 */
public class FrontmatterNavigation {

    private final String title;
    @Nullable
    private final ResourceLocation parent;
    private final int position;
    private final int recommend;
    @Nullable
    private final String iconItemId;
    private final int iconItemMeta;
    @Nullable
    private final Map<?, ?> iconComponents;
    @Nullable
    private final ResourceLocation iconTextureId;
    @Nullable
    private final List<NavigationIconEntry> iconEntries;
    @Nullable
    private final List<ResourceLocation> iconTextureEntries;
    @Nullable
    private final List<String> requiredMods;
    private final int loadPriority;

    public FrontmatterNavigation(String title, @Nullable ResourceLocation parent, int position, int recommend,
        @Nullable String iconItemId, int iconItemMeta, @Nullable Map<?, ?> iconComponents,
        @Nullable ResourceLocation iconTextureId, @Nullable List<NavigationIconEntry> iconEntries,
        @Nullable List<ResourceLocation> iconTextureEntries, @Nullable List<String> requiredMods, int loadPriority) {
        this.title = title;
        this.parent = parent;
        this.position = position;
        this.recommend = recommend;
        this.iconItemId = iconItemId;
        this.iconItemMeta = iconItemMeta;
        this.iconComponents = iconComponents;
        this.iconTextureId = iconTextureId;
        this.iconEntries = iconEntries;
        this.iconTextureEntries = iconTextureEntries;
        this.requiredMods = requiredMods;
        this.loadPriority = loadPriority;
    }

    public String title() {
        return title;
    }

    @Nullable
    public ResourceLocation parent() {
        return parent;
    }

    public int position() {
        return position;
    }

    public int recommend() {
        return recommend;
    }

    @Nullable
    public String iconItemId() {
        return iconItemId;
    }

    public int iconItemMeta() {
        return iconItemMeta;
    }

    @Nullable
    public Map<?, ?> iconComponents() {
        return iconComponents;
    }

    @Nullable
    public ResourceLocation iconTextureId() {
        return iconTextureId;
    }

    @Nullable
    public List<NavigationIconEntry> iconEntries() {
        return iconEntries;
    }

    @Nullable
    public List<ResourceLocation> iconTextureEntries() {
        return iconTextureEntries;
    }

    @Nullable
    public List<String> requiredMods() {
        return requiredMods;
    }

    public int loadPriority() {
        return loadPriority;
    }
}
