package com.hfstudio.guidenh.guide.siteexport.site.layout;

import com.hfstudio.guidenh.guide.internal.recipe.RecipeLookup;
import com.hfstudio.guidenh.integration.nei.NeiRecipeLookup;

/**
 * Provenance of recipe data for static site export layout strategies.
 */
public enum SiteRecipeSourceKind {
    /** {@link RecipeLookup.Entry} crafting 3x3. */
    VANILLA,
    /** Generic integration recipe snapshot. */
    RECIPE_ENTRY,
    /** Snapshot from {@link NeiRecipeLookup#readHandler}. */
    NEI_ENTRY,
    /** Live {@code IRecipeHandler} + recipe index (raw handler path). */
    RAW_HANDLER
}
