package com.hfstudio.guidenh.guide.siteexport.site.layout;

import net.minecraft.item.ItemStack;

import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.compat.nei.NeiRecipeLookup;
import com.hfstudio.guidenh.guide.internal.recipe.RecipeLookup;
import com.hfstudio.guidenh.guide.siteexport.site.GuideSiteItemIconResolver;
import com.hfstudio.guidenh.guide.siteexport.site.GuideSiteRecipeExporter;

/**
 * Immutable inputs for {@link SiteRecipeLayoutStrategy}. Exactly one of
 * {@link #vanillaEntry}, {@link #neiEntry}, or {@link #rawHandler} is meaningful
 * for a given {@link #kind}.
 */
public final class SiteRecipeLayoutContext {

    private final SiteRecipeSourceKind kind;
    private final ItemStack targetStack;
    private final GuideSiteRecipeExporter exporter;
    private final GuideSiteItemIconResolver itemIconResolver;
    private final @Nullable RecipeLookup.Entry vanillaEntry;
    private final @Nullable NeiRecipeLookup.Entry neiEntry;
    private final @Nullable Object rawHandler;
    private final int rawRecipeIndex;
    private final @Nullable SiteRecipeRawHandlerAccess rawHandlerAccess;

    private SiteRecipeLayoutContext(SiteRecipeSourceKind kind, ItemStack targetStack, GuideSiteRecipeExporter exporter,
        GuideSiteItemIconResolver itemIconResolver, @Nullable RecipeLookup.Entry vanillaEntry,
        @Nullable NeiRecipeLookup.Entry neiEntry, @Nullable Object rawHandler, int rawRecipeIndex,
        @Nullable SiteRecipeRawHandlerAccess rawHandlerAccess) {
        this.kind = kind;
        this.targetStack = targetStack;
        this.exporter = exporter;
        this.itemIconResolver = itemIconResolver;
        this.vanillaEntry = vanillaEntry;
        this.neiEntry = neiEntry;
        this.rawHandler = rawHandler;
        this.rawRecipeIndex = rawRecipeIndex;
        this.rawHandlerAccess = rawHandlerAccess;
    }

    public static SiteRecipeLayoutContext vanilla(RecipeLookup.Entry entry, ItemStack targetStack,
        GuideSiteRecipeExporter exporter, GuideSiteItemIconResolver itemIconResolver) {
        return new SiteRecipeLayoutContext(
            SiteRecipeSourceKind.VANILLA,
            targetStack,
            exporter,
            itemIconResolver,
            entry,
            null,
            null,
            -1,
            null);
    }

    public static SiteRecipeLayoutContext neiEntry(NeiRecipeLookup.Entry entry, ItemStack targetStack,
        GuideSiteRecipeExporter exporter, GuideSiteItemIconResolver itemIconResolver) {
        return new SiteRecipeLayoutContext(
            SiteRecipeSourceKind.NEI_ENTRY,
            targetStack,
            exporter,
            itemIconResolver,
            null,
            entry,
            null,
            -1,
            null);
    }

    public static SiteRecipeLayoutContext rawHandler(Object handler, int recipeIndex, ItemStack targetStack,
        GuideSiteRecipeExporter exporter, GuideSiteItemIconResolver itemIconResolver,
        SiteRecipeRawHandlerAccess rawHandlerAccess) {
        return new SiteRecipeLayoutContext(
            SiteRecipeSourceKind.RAW_HANDLER,
            targetStack,
            exporter,
            itemIconResolver,
            null,
            null,
            handler,
            recipeIndex,
            rawHandlerAccess);
    }

    public SiteRecipeSourceKind kind() {
        return kind;
    }

    public ItemStack targetStack() {
        return targetStack;
    }

    public GuideSiteRecipeExporter exporter() {
        return exporter;
    }

    public GuideSiteItemIconResolver itemIconResolver() {
        return itemIconResolver;
    }

    public @Nullable RecipeLookup.Entry vanillaEntry() {
        return vanillaEntry;
    }

    public @Nullable NeiRecipeLookup.Entry neiEntry() {
        return neiEntry;
    }

    public @Nullable Object rawHandler() {
        return rawHandler;
    }

    public int rawRecipeIndex() {
        return rawRecipeIndex;
    }

    public @Nullable SiteRecipeRawHandlerAccess rawHandlerAccess() {
        return rawHandlerAccess;
    }
}
