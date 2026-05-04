package com.hfstudio.guidenh.guide.siteexport.site.layout;

import com.hfstudio.guidenh.compat.nei.NeiRecipeLookup;
import com.hfstudio.guidenh.guide.siteexport.site.GuideSiteExportedItem;
import com.hfstudio.guidenh.guide.siteexport.site.GuideSiteItemIconResolver;
import com.hfstudio.guidenh.guide.siteexport.site.GuideSiteRecipeExporter;

/**
 * Preserves the historical site layout: 3×3 flow grid + optional supporting column + result slot.
 */
public final class DefaultSiteRecipeLayoutStrategy implements SiteRecipeLayoutStrategy {

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
                java.util.List<java.util.List<GuideSiteExportedItem>> ingredients = exporter
                    .ingredientItemsFromNeiSlots(access.readIngredientSlots(handler, idx), resolver);
                java.util.List<java.util.List<GuideSiteExportedItem>> supporting = exporter
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
