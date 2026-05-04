package com.hfstudio.guidenh.guide.siteexport.site.layout;

/**
 * Provenance of recipe data for static site export layout strategies.
 */
public enum SiteRecipeSourceKind {
    /** {@link com.hfstudio.guidenh.guide.internal.recipe.RecipeLookup.Entry} crafting 3×3. */
    VANILLA,
    /** Snapshot from {@link com.hfstudio.guidenh.compat.nei.NeiRecipeLookup#readHandler}. */
    NEI_ENTRY,
    /** Live {@code IRecipeHandler} + recipe index (raw handler path). */
    RAW_HANDLER
}
