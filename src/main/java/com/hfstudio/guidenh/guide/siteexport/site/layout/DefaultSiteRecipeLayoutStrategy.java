package com.hfstudio.guidenh.guide.siteexport.site.layout;

import java.util.List;

import com.hfstudio.guidenh.guide.siteexport.site.GuideSiteExportedItem;
import com.hfstudio.guidenh.guide.siteexport.site.GuideSiteItemIconResolver;
import com.hfstudio.guidenh.guide.siteexport.site.GuideSiteRecipeExporter;
import com.hfstudio.guidenh.integration.api.RecipeEntry;
import com.hfstudio.guidenh.integration.nei.NeiRecipeLookup;

/**
 * Preserves the historical site layout: 3脳3 flow grid + optional supporting column + result slot.
 */
public class DefaultSiteRecipeLayoutStrategy implements SiteRecipeLayoutStrategy {

    @Override
    public boolean supports(SiteRecipeLayoutContext ctx) {
        return true;
    }

    @Override
    public String render(SiteRecipeLayoutContext ctx) {
        GuideSiteRecipeExporter exporter = ctx.exporter();
        GuideSiteItemIconResolver resolver = ctx.itemIconResolver();
        switch (ctx.kind()) {
            case VANILLA: {
                if (ctx.vanillaEntry() == null || ctx.vanillaEntry().result == null) {
                    return "";
                }
                return exporter.renderHtmlGridItems(
                    exporter.ingredientItemsFromVanillaEntry(ctx.vanillaEntry(), resolver),
                    exporter.itemInfo(ctx.vanillaEntry().result, resolver));
            }
            case RECIPE_ENTRY: {
                RecipeEntry entry = ctx.recipeEntry();
                if (entry == null || entry.result() == null) {
                    return "";
                }
                return exporter.renderNeiOverlayGridItems(
                    exporter.ingredientItemsFromRecipeEntry(entry, resolver),
                    exporter.resultItem(entry.result(), ctx.targetStack(), resolver),
                    exporter.supportingSlotItemsFromRecipeEntry(entry, resolver));
            }
            case NEI_ENTRY: {
                NeiRecipeLookup.Entry entry = ctx.neiEntry();
                if (entry == null || entry.result == null) {
                    return "";
                }
                return exporter.renderNeiOverlayGridItems(
                    exporter.ingredientItemsFromNeiEntry(entry, resolver),
                    exporter.resultItem(entry.result, ctx.targetStack(), resolver),
                    exporter.supportingSlotItemsFromNeiEntry(entry, resolver));
            }
            case RAW_HANDLER: {
                Object handler = ctx.rawHandler();
                SiteRecipeRawHandlerAccess access = ctx.rawHandlerAccess();
                if (handler == null || access == null) {
                    return "";
                }
                int idx = ctx.rawRecipeIndex();
                List<List<GuideSiteExportedItem>> ingredients = exporter
                    .ingredientItemsFromNeiSlots(access.readIngredientSlots(handler, idx), resolver);
                List<List<GuideSiteExportedItem>> supporting = exporter
                    .supportingSlotItemsFromNeiSlots(access.readOtherSlots(handler, idx), resolver);
                GuideSiteExportedItem resultItem = exporter
                    .resultItem(access.readResultSlot(handler, idx), ctx.targetStack(), resolver);
                if (ingredients.isEmpty() && supporting.isEmpty() && resultItem.isEmpty()) {
                    return "";
                }
                return exporter.renderNeiOverlayGridItems(ingredients, resultItem, supporting);
            }
            default:
                return "";
        }
    }
}
