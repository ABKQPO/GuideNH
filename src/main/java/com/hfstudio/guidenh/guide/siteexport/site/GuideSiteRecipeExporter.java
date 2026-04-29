package com.hfstudio.guidenh.guide.siteexport.site;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.minecraft.item.ItemStack;

import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.guide.internal.recipe.NeiRecipeLookup;
import com.hfstudio.guidenh.guide.internal.recipe.RecipeLookup;

public class GuideSiteRecipeExporter {

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
        html.append("<div class=\"recipe-result\" data-result-item-id=\"")
            .append(escapeHtml(resultItem.itemId()))
            .append("\">");
        GuideSiteItemHtml.appendIcon(html, resultItem, null);
        html.append("</div>");
        html.append("</section>");
        return html.toString();
    }

    private void appendSlotBoxes(StringBuilder html, List<List<GuideSiteExportedItem>> slots) {
        if (slots == null) {
            return;
        }
        for (List<GuideSiteExportedItem> candidates : slots) {
            List<GuideSiteExportedItem> safeCandidates = candidates != null ? candidates : Collections.emptyList();
            html.append("<div class=\"ingredient-box");
            if (safeCandidates.size() > 1) {
                html.append(" cycling");
            }
            html.append("\">");
            for (GuideSiteExportedItem item : safeCandidates) {
                GuideSiteItemHtml.appendIcon(html, item, null);
            }
            html.append("</div>");
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
