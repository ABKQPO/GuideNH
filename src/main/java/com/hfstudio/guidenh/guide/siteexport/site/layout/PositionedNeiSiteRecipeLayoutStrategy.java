package com.hfstudio.guidenh.guide.siteexport.site.layout;

import java.util.ArrayList;
import java.util.List;

import com.hfstudio.guidenh.compat.nei.NeiRecipeLookup;

/**
 * Lays out NEI-style recipes using each {@link NeiRecipeLookup.Slot}'s {@code relx}/{@code rely}
 * inside a percentage-based canvas so handlers with irregular grids (e.g. loot tables) match NEI
 * spacing more closely than the legacy 3×3 + vertical {@code others} column.
 */
public final class PositionedNeiSiteRecipeLayoutStrategy implements SiteRecipeLayoutStrategy {

    @Override
    public boolean supports(SiteRecipeLayoutContext ctx) {
        return ctx.kind() == SiteRecipeSourceKind.NEI_ENTRY || ctx.kind() == SiteRecipeSourceKind.RAW_HANDLER;
    }

    @Override
    public String render(SiteRecipeLayoutContext ctx) {
        List<NeiRecipeLookup.Slot> merged = collectSlots(ctx);
        if (merged.isEmpty()) {
            return "";
        }
        return ctx.exporter()
            .renderNeiPositionedSlots(
                merged,
                ctx.itemIconResolver(),
                ctx.neiPhase1BackgroundUrl(),
                ctx.neiPhase1CanvasWidthPx(),
                ctx.neiPhase1CanvasHeightPx(),
                ctx.neiPhase1BodyYShiftPx());
    }

    private static List<NeiRecipeLookup.Slot> collectSlots(SiteRecipeLayoutContext ctx) {
        List<NeiRecipeLookup.Slot> out = new ArrayList<>();
        if (ctx.kind() == SiteRecipeSourceKind.NEI_ENTRY) {
            NeiRecipeLookup.Entry e = ctx.neiEntry();
            if (e == null) {
                return out;
            }
            addAll(out, e.ingredients);
            addAll(out, e.others);
            if (e.result != null) {
                out.add(e.result);
            }
            return out;
        }
        if (ctx.kind() == SiteRecipeSourceKind.RAW_HANDLER) {
            Object handler = ctx.rawHandler();
            SiteRecipeRawHandlerAccess access = ctx.rawHandlerAccess();
            if (handler == null || access == null) {
                return out;
            }
            int idx = ctx.rawRecipeIndex();
            addAll(out, access.readIngredientSlots(handler, idx));
            addAll(out, access.readOtherSlots(handler, idx));
            NeiRecipeLookup.Slot res = access.readResultSlot(handler, idx);
            if (res != null) {
                out.add(res);
            }
            return out;
        }
        return out;
    }

    private static void addAll(List<NeiRecipeLookup.Slot> dest, List<NeiRecipeLookup.Slot> src) {
        if (src == null) {
            return;
        }
        for (NeiRecipeLookup.Slot s : src) {
            if (s != null) {
                dest.add(s);
            }
        }
    }
}
