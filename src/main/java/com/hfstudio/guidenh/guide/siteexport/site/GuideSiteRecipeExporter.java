package com.hfstudio.guidenh.guide.siteexport.site;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import net.minecraft.item.ItemStack;

import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.compat.nei.NeiRecipeLookup;
import com.hfstudio.guidenh.guide.internal.recipe.RecipeLookup;

public class GuideSiteRecipeExporter {

    /** NEI slot chrome is commonly {@code 18×18} pixels; vanilla/GT handlers draw {@code 16×16} items inset by 1px. */
    public static final int NEI_SLOT_GUI_PIXELS = 18;

    public String renderHtmlGrid(List<List<String>> ingredients, String resultItemId) {
        return renderGrid(
            unresolvedItems(ingredients),
            GuideSiteItemSupport.unresolved(resultItemId),
            "html-grid",
            Collections.emptyList());
    }

    public String renderNeiOverlayGrid(List<List<String>> ingredients, String resultItemId) {
        return renderNeiOverlayGrid(ingredients, resultItemId, Collections.emptyList());
    }

    public String renderNeiOverlayGrid(List<List<String>> ingredients, String resultItemId,
        List<List<String>> supportingSlots) {
        return renderGrid(
            unresolvedItems(ingredients),
            GuideSiteItemSupport.unresolved(resultItemId),
            "nei-overlay",
            unresolvedItems(supportingSlots));
    }

    public String renderHtmlGridItems(List<List<GuideSiteExportedItem>> ingredients, GuideSiteExportedItem resultItem) {
        return renderGrid(ingredients, resultItem, "html-grid", Collections.emptyList());
    }

    public String renderNeiOverlayGridItems(List<List<GuideSiteExportedItem>> ingredients,
        GuideSiteExportedItem resultItem) {
        return renderNeiOverlayGridItems(ingredients, resultItem, Collections.emptyList());
    }

    public String renderNeiOverlayGridItems(List<List<GuideSiteExportedItem>> ingredients,
        GuideSiteExportedItem resultItem, List<List<GuideSiteExportedItem>> supportingSlots) {
        return renderGrid(ingredients, resultItem, "nei-overlay", supportingSlots);
    }

    public String renderRecipeCollection(List<String> renderedRecipes, boolean multi) {
        if (renderedRecipes == null || renderedRecipes.isEmpty()) {
            return "";
        }
        if (!multi || renderedRecipes.size() == 1) {
            StringBuilder html = new StringBuilder();
            for (String renderedRecipe : renderedRecipes) {
                html.append(renderedRecipe);
            }
            return html.toString();
        }

        StringBuilder html = new StringBuilder();
        html.append("<div class=\"guide-recipe-gallery\">");
        for (String renderedRecipe : renderedRecipes) {
            html.append(renderedRecipe);
        }
        html.append("</div>");
        return html.toString();
    }

    private String renderGrid(List<List<GuideSiteExportedItem>> ingredients, GuideSiteExportedItem resultItem,
        String renderMode, List<List<GuideSiteExportedItem>> supportingSlots) {
        StringBuilder html = new StringBuilder();
        html.append("<section class=\"recipe-grid\" data-render-mode=\"")
            .append(escapeHtml(renderMode))
            .append("\">");
        html.append("<div class=\"recipe-main\">");
        html.append("<div class=\"recipe-ingredients\">");
        appendSlotBoxes(html, ingredients);
        html.append("</div>");
        if (supportingSlots != null && !supportingSlots.isEmpty()) {
            html.append("<div class=\"recipe-supporting-slots\">");
            appendSlotBoxes(html, supportingSlots);
            html.append("</div>");
        }
        html.append("</div>");
        // Wrap the result icon in an `ingredient-box` so the output slot also receives the
        // slot.png border-image background (previously the result was rendered as a bare
        // `recipe-result` div with no slot frame, which made the output look unboxed).
        html.append("<div class=\"recipe-result ingredient-box\" data-result-item-id=\"")
            .append(escapeHtml(resultItem.itemId()))
            .append("\">");
        // Emit the native `title=` tooltip so hovering the result slot reports the
        // ItemStack display name even though we don't register a full template.
        GuideSiteItemHtml.appendIcon(html, resultItem, null, 1f, true);
        html.append("</div>");
        html.append("</section>");
        return html.toString();
    }

    /**
     * Renders ingredient / other / result stacks in a single coordinate system using each slot's
     * {@link NeiRecipeLookup.Slot#relx}/{@code rely}, scaled with {@code --gui-scale} like other
     * recipe chrome.
     */
    public String renderNeiPositionedSlots(List<NeiRecipeLookup.Slot> slots,
        GuideSiteItemIconResolver itemIconResolver) {
        return renderNeiPositionedSlots(slots, itemIconResolver, null, null, null, null);
    }

    /**
     * @param phase1BackgroundUrl optional Phase1 PNG URL; valid canvas width/height must be set together for Phase1 alignment.
     * @param phase1BodyYShiftPx matches Phase1 framebuffer {@code glTranslate Y} ({@link NeiRecipeLookup#lookupHandlerYShift}); {@code null} when Phase1 is unused.
     */
    public String renderNeiPositionedSlots(List<NeiRecipeLookup.Slot> slots,
        GuideSiteItemIconResolver itemIconResolver, @Nullable String phase1BackgroundUrl,
        @Nullable Integer phase1CanvasWidthPx, @Nullable Integer phase1CanvasHeightPx,
        @Nullable Integer phase1BodyYShiftPx) {
        if (slots == null || slots.isEmpty()) {
            return "";
        }
        boolean usePhase1Canvas = phase1BackgroundUrl != null && !phase1BackgroundUrl.isEmpty()
            && phase1CanvasWidthPx != null && phase1CanvasHeightPx != null && phase1CanvasWidthPx > 0
            && phase1CanvasHeightPx > 0;

        int canvasW;
        int canvasH;
        int minX = 0;
        int minY = 0;
        if (usePhase1Canvas) {
            canvasW = phase1CanvasWidthPx;
            canvasH = phase1CanvasHeightPx;
        } else {
            minX = Integer.MAX_VALUE;
            minY = Integer.MAX_VALUE;
            int maxR = Integer.MIN_VALUE;
            int maxB = Integer.MIN_VALUE;
            for (NeiRecipeLookup.Slot slot : slots) {
                if (slot == null) {
                    continue;
                }
                minX = Math.min(minX, slot.relx);
                minY = Math.min(minY, slot.rely);
                maxR = Math.max(maxR, slot.relx + NEI_SLOT_GUI_PIXELS);
                maxB = Math.max(maxB, slot.rely + NEI_SLOT_GUI_PIXELS);
            }
            if (minX == Integer.MAX_VALUE) {
                return "";
            }
            canvasW = Math.max(NEI_SLOT_GUI_PIXELS, maxR - minX);
            canvasH = Math.max(NEI_SLOT_GUI_PIXELS, maxB - minY);
        }

        StringBuilder html = new StringBuilder();
        html.append("<section class=\"recipe-grid recipe-grid--nei-positioned\" data-render-mode=\"nei-positioned\">");
        html.append("<div class=\"recipe-positioned-outer\" style=\"width:calc(")
            .append(canvasW)
            .append("px * var(--gui-scale));height:calc(")
            .append(canvasH)
            .append("px * var(--gui-scale));\">");
        html.append("<div class=\"recipe-positioned-canvas");
        if (usePhase1Canvas) {
            html.append(" recipe-positioned-canvas--phase1-bg\" style=\"background-image:url('")
                .append(escapeHtml(phase1BackgroundUrl))
                .append("');background-size:100% 100%;background-repeat:no-repeat;\">");
        } else {
            html.append("\">");
        }
        for (NeiRecipeLookup.Slot slot : slots) {
            if (slot == null) {
                continue;
            }
            int slotPxW;
            int slotPxH;
            double leftPct;
            double topPct;
            if (usePhase1Canvas) {
                /*
                 * PNG viewport is HandlerInfo WxH plus symmetric VIEWPORT_MARGIN_PX — same inset as Phase1 translate(m, yShift+m).
                 * GT/ModularUI nine-patch bezel can extend a few px past the nominal body; relying on HandlerInfo alone clips edges.
                 */
                int phase1YShift = phase1BodyYShiftPx != null ? phase1BodyYShiftPx : 0;
                int m = GuideSiteNeiPhase1BackgroundExporter.VIEWPORT_MARGIN_PX;
                int slotOx = Math.max(0, slot.relx - 1 + m);
                int slotOy = Math.max(0, slot.rely - 1 + phase1YShift + m);
                slotPxW = NEI_SLOT_GUI_PIXELS;
                slotPxH = NEI_SLOT_GUI_PIXELS;
                leftPct = 100.0 * slotOx / canvasW;
                topPct = 100.0 * slotOy / canvasH;
            } else {
                slotPxW = NEI_SLOT_GUI_PIXELS;
                slotPxH = NEI_SLOT_GUI_PIXELS;
                leftPct = 100.0 * (slot.relx - minX) / canvasW;
                topPct = 100.0 * (slot.rely - minY) / canvasH;
            }
            double widthPct = 100.0 * slotPxW / canvasW;
            double heightPct = 100.0 * slotPxH / canvasH;

            List<GuideSiteExportedItem> candidates = new ArrayList<>();
            if (slot.stacks != null) {
                for (ItemStack stack : slot.stacks) {
                    if (stack != null) {
                        candidates.add(itemInfo(stack, itemIconResolver));
                    }
                }
            }

            if (usePhase1Canvas) {
                html.append("<div class=\"recipe-positioned-slot recipe-positioned-slot--phase1-overlay\"");
            } else {
                html.append("<div class=\"ingredient-box recipe-positioned-slot\"");
            }
            if (candidates.size() > 1) {
                html.append(" data-ingredient-cycling");
            }
            html.append(" style=\"left:")
                .append(String.format(Locale.US, "%.5f", leftPct))
                .append("%;top:")
                .append(String.format(Locale.US, "%.5f", topPct))
                .append("%;width:")
                .append(String.format(Locale.US, "%.5f", widthPct))
                .append("%;height:")
                .append(String.format(Locale.US, "%.5f", heightPct))
                .append("%;\">");
            if (candidates.isEmpty()) {
                html.append("<span class=\"recipe-positioned-slot-empty\"></span>");
            } else {
                appendSlotContents(html, candidates);
            }
            html.append("</div>");
        }
        html.append("</div></div></section>");
        return html.toString();
    }

    private void appendSlotBoxes(StringBuilder html, List<List<GuideSiteExportedItem>> slots) {
        if (slots == null) {
            return;
        }
        for (List<GuideSiteExportedItem> candidates : slots) {
            List<GuideSiteExportedItem> safeCandidates = candidates != null ? candidates : Collections.emptyList();
            if (safeCandidates.isEmpty()) {
                html.append("<div class=\"ingredient-box empty-ingredient-box\"></div>");
                continue;
            }
            html.append("<div class=\"ingredient-box\"");
            if (safeCandidates.size() > 1) {
                html.append(" data-ingredient-cycling");
            }
            html.append(">");
            appendSlotContents(html, safeCandidates);
            html.append("</div>");
        }
    }

    /** Item icons for one slot (cycling markers when {@code stacks.size()>1}); outer box is callers' responsibility. */
    private void appendSlotContents(StringBuilder html, List<GuideSiteExportedItem> safeCandidates) {
        for (int i = 0; i < safeCandidates.size(); i++) {
            int beforeIcon = html.length();
            GuideSiteItemHtml.appendIcon(html, safeCandidates.get(i), null, 1f, true);
            if (safeCandidates.size() > 1 && i == 0) {
                int classAttr = html.indexOf("class=\"", beforeIcon);
                if (classAttr >= 0) {
                    int valueStart = classAttr + "class=\"".length();
                    html.insert(valueStart, "current ");
                }
            }
        }
    }

    public List<List<String>> ingredientsFromVanillaEntry(RecipeLookup.Entry entry) {
        List<List<String>> ingredients = new ArrayList<>();
        for (int i = 0; i < entry.input3x3.length; i++) {
            ItemStack stack = entry.input3x3[i];
            if (stack == null) {
                ingredients.add(new ArrayList<>());
            } else {
                List<String> candidates = new ArrayList<>();
                candidates.add(itemId(stack));
                ingredients.add(candidates);
            }
        }
        return ingredients;
    }

    public List<List<GuideSiteExportedItem>> ingredientItemsFromVanillaEntry(RecipeLookup.Entry entry,
        GuideSiteItemIconResolver itemIconResolver) {
        List<List<GuideSiteExportedItem>> ingredients = new ArrayList<>();
        if (entry == null) {
            return ingredients;
        }
        for (int i = 0; i < entry.input3x3.length; i++) {
            ItemStack stack = entry.input3x3[i];
            if (stack == null) {
                ingredients.add(new ArrayList<>());
            } else {
                List<GuideSiteExportedItem> candidates = new ArrayList<>(1);
                candidates.add(itemInfo(stack, itemIconResolver));
                ingredients.add(candidates);
            }
        }
        return ingredients;
    }

    public List<List<String>> ingredientsFromNeiEntry(NeiRecipeLookup.Entry entry) {
        return ingredientsFromNeiSlots(entry != null ? entry.ingredients : Collections.emptyList());
    }

    public List<List<GuideSiteExportedItem>> ingredientItemsFromNeiEntry(NeiRecipeLookup.Entry entry,
        GuideSiteItemIconResolver itemIconResolver) {
        return ingredientItemsFromNeiSlots(
            entry != null ? entry.ingredients : Collections.emptyList(),
            itemIconResolver);
    }

    public List<List<String>> ingredientsFromNeiSlots(List<NeiRecipeLookup.Slot> slots) {
        List<List<String>> ingredients = new ArrayList<>();
        if (slots == null) {
            return ingredients;
        }
        for (NeiRecipeLookup.Slot slot : slots) {
            List<String> candidates = new ArrayList<>();
            if (slot != null && slot.stacks != null) {
                for (ItemStack stack : slot.stacks) {
                    if (stack != null) {
                        candidates.add(itemId(stack));
                    }
                }
            }
            ingredients.add(candidates);
        }
        return ingredients;
    }

    public List<List<GuideSiteExportedItem>> ingredientItemsFromNeiSlots(List<NeiRecipeLookup.Slot> slots,
        GuideSiteItemIconResolver itemIconResolver) {
        List<List<GuideSiteExportedItem>> ingredients = new ArrayList<>();
        if (slots == null) {
            return ingredients;
        }
        for (NeiRecipeLookup.Slot slot : slots) {
            List<GuideSiteExportedItem> candidates = new ArrayList<>();
            if (slot != null && slot.stacks != null) {
                for (ItemStack stack : slot.stacks) {
                    if (stack != null) {
                        candidates.add(itemInfo(stack, itemIconResolver));
                    }
                }
            }
            ingredients.add(candidates);
        }
        return ingredients;
    }

    public List<List<String>> supportingSlotsFromNeiEntry(NeiRecipeLookup.Entry entry) {
        return ingredientsFromNeiSlots(entry != null ? entry.others : Collections.emptyList());
    }

    public List<List<GuideSiteExportedItem>> supportingSlotItemsFromNeiEntry(NeiRecipeLookup.Entry entry,
        GuideSiteItemIconResolver itemIconResolver) {
        return ingredientItemsFromNeiSlots(entry != null ? entry.others : Collections.emptyList(), itemIconResolver);
    }

    public List<List<String>> supportingSlotsFromNeiSlots(List<NeiRecipeLookup.Slot> slots) {
        return ingredientsFromNeiSlots(slots);
    }

    public List<List<GuideSiteExportedItem>> supportingSlotItemsFromNeiSlots(List<NeiRecipeLookup.Slot> slots,
        GuideSiteItemIconResolver itemIconResolver) {
        return ingredientItemsFromNeiSlots(slots, itemIconResolver);
    }

    public String resultItemId(@Nullable NeiRecipeLookup.Slot result, String fallbackItemId) {
        if (result != null && result.stacks != null) {
            for (ItemStack stack : result.stacks) {
                String itemId = itemId(stack);
                if (!itemId.isEmpty()) {
                    return itemId;
                }
            }
        }
        return fallbackItemId != null ? fallbackItemId : "";
    }

    public GuideSiteExportedItem resultItem(@Nullable NeiRecipeLookup.Slot result, @Nullable ItemStack fallbackStack,
        GuideSiteItemIconResolver itemIconResolver) {
        if (result != null && result.stacks != null) {
            for (ItemStack stack : result.stacks) {
                if (stack != null && stack.getItem() != null) {
                    return itemInfo(stack, itemIconResolver);
                }
            }
        }
        if (fallbackStack != null && fallbackStack.getItem() != null) {
            return itemInfo(fallbackStack, itemIconResolver);
        }
        return GuideSiteItemSupport.unresolved("");
    }

    public GuideSiteExportedItem itemInfo(@Nullable ItemStack stack, GuideSiteItemIconResolver itemIconResolver) {
        return GuideSiteItemSupport.export(stack, itemIconResolver);
    }

    public String itemId(ItemStack stack) {
        return GuideSiteItemSupport.itemId(stack);
    }

    private String escapeHtml(String text) {
        return text.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;");
    }

    private List<List<GuideSiteExportedItem>> unresolvedItems(List<List<String>> items) {
        List<List<GuideSiteExportedItem>> resolved = new ArrayList<>();
        if (items == null) {
            return resolved;
        }
        for (List<String> candidates : items) {
            List<GuideSiteExportedItem> resolvedCandidates = new ArrayList<>();
            if (candidates != null) {
                for (String candidate : candidates) {
                    resolvedCandidates.add(GuideSiteItemSupport.unresolved(candidate));
                }
            }
            resolved.add(resolvedCandidates);
        }
        return resolved;
    }
}
