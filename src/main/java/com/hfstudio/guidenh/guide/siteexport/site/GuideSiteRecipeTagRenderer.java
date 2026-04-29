package com.hfstudio.guidenh.guide.siteexport.site;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;

import net.minecraft.item.ItemStack;

import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.guide.compiler.IdUtils;
import com.hfstudio.guidenh.guide.compiler.tags.RecipeCompiler;
import com.hfstudio.guidenh.guide.internal.recipe.NeiRecipeLookup;
import com.hfstudio.guidenh.guide.internal.recipe.RecipeCache;
import com.hfstudio.guidenh.guide.internal.recipe.RecipeLookup;
import com.hfstudio.guidenh.libs.mdast.mdx.model.MdxJsxElementFields;

public class GuideSiteRecipeTagRenderer implements GuideSiteHtmlCompiler.RecipeTagRenderer {

    interface TargetStackResolver {

        @Nullable
        ItemStack resolve(String recipeId, String defaultNamespace);
    }

    interface VanillaRecipeFinder {

        List<RecipeLookup.Entry> findByOutput(ItemStack targetStack);
    }

    interface NeiRecipeFinder {

        List<NeiRecipeLookup.Entry> findCraftingRecipes(ItemStack targetStack);
    }

    interface RawHandlerFinder {

        List<Object> findCraftingHandlers(ItemStack targetStack);

        List<Object> findUsageHandlers(ItemStack targetStack);
    }

    interface HandlerRuntime extends RecipeCompiler.HandlerMetadataReader, RecipeCompiler.HandlerRecipeAccess {

        int recipeCount(Object handler);

        List<NeiRecipeLookup.Slot> readOtherSlots(Object handler, int recipeIndex);
    }

    private final GuideSiteRecipeExporter exporter;
    private final GuideSiteItemIconResolver itemIconResolver;
    private final TargetStackResolver targetStackResolver;
    private final VanillaRecipeFinder vanillaRecipeFinder;
    private final NeiRecipeFinder neiRecipeFinder;
    private final RawHandlerFinder rawHandlerFinder;
    private final HandlerRuntime handlerRuntime;

    public GuideSiteRecipeTagRenderer() {
        this(GuideSiteItemIconResolver.NONE);
    }

    public GuideSiteRecipeTagRenderer(GuideSiteItemIconResolver itemIconResolver) {
        this(new GuideSiteRecipeExporter(), itemIconResolver, IdUtils::resolveItemStack, targetStack -> {
            if (targetStack == null || targetStack.getItem() == null) {
                return Collections.emptyList();
            }
            return RecipeLookup.findByOutput(targetStack.getItem());
        }, targetStack -> {
            if (targetStack == null) {
                return Collections.emptyList();
            }
            return NeiRecipeLookup.findCraftingRecipes(targetStack);
        }, new RawHandlerFinder() {

            @Override
            public List<Object> findCraftingHandlers(ItemStack targetStack) {
                if (targetStack == null) {
                    return Collections.emptyList();
                }
                return RecipeCache.getCraftingHandlers(targetStack);
            }

            @Override
            public List<Object> findUsageHandlers(ItemStack targetStack) {
                if (targetStack == null) {
                    return Collections.emptyList();
                }
                return RecipeCache.getUsageHandlers(targetStack);
            }
        }, new HandlerRuntime() {

            @Override
            public @Nullable String handlerName(Object handler) {
                return NeiRecipeLookup.lookupHandlerName(handler);
            }

            @Override
            public @Nullable String overlayIdentifier(Object handler) {
                return NeiRecipeLookup.lookupOverlayIdentifier(handler);
            }

            @Override
            public int recipeCount(Object handler) {
                return NeiRecipeLookup.lookupNumRecipes(handler);
            }

            @Override
            public List<NeiRecipeLookup.Slot> readIngredientSlots(Object handler, int recipeIndex) {
                return NeiRecipeLookup.readIngredientSlots(handler, recipeIndex);
            }

            @Override
            public @Nullable NeiRecipeLookup.Slot readResultSlot(Object handler, int recipeIndex) {
                return NeiRecipeLookup.readResultSlot(handler, recipeIndex);
            }

            @Override
            public List<NeiRecipeLookup.Slot> readOtherSlots(Object handler, int recipeIndex) {
                return NeiRecipeLookup.readOtherSlots(handler, recipeIndex);
            }
        });
    }

    GuideSiteRecipeTagRenderer(GuideSiteRecipeExporter exporter, GuideSiteItemIconResolver itemIconResolver,
        TargetStackResolver targetStackResolver, VanillaRecipeFinder vanillaRecipeFinder,
        NeiRecipeFinder neiRecipeFinder) {
        this(
            exporter,
            itemIconResolver,
            targetStackResolver,
            vanillaRecipeFinder,
            neiRecipeFinder,
            new RawHandlerFinder() {

                @Override
                public List<Object> findCraftingHandlers(ItemStack targetStack) {
                    return Collections.emptyList();
                }

                @Override
                public List<Object> findUsageHandlers(ItemStack targetStack) {
                    return Collections.emptyList();
                }
            },
            new HandlerRuntime() {

                @Override
                public @Nullable String handlerName(Object handler) {
                    return null;
                }

                @Override
                public @Nullable String overlayIdentifier(Object handler) {
                    return null;
                }

                @Override
                public int recipeCount(Object handler) {
                    return 0;
                }

                @Override
                public List<NeiRecipeLookup.Slot> readIngredientSlots(Object handler, int recipeIndex) {
                    return Collections.emptyList();
                }

                @Override
                public @Nullable NeiRecipeLookup.Slot readResultSlot(Object handler, int recipeIndex) {
                    return null;
                }

                @Override
                public List<NeiRecipeLookup.Slot> readOtherSlots(Object handler, int recipeIndex) {
                    return Collections.emptyList();
                }
            });
    }

    GuideSiteRecipeTagRenderer(GuideSiteRecipeExporter exporter, TargetStackResolver targetStackResolver,
        VanillaRecipeFinder vanillaRecipeFinder, NeiRecipeFinder neiRecipeFinder) {
        this(exporter, GuideSiteItemIconResolver.NONE, targetStackResolver, vanillaRecipeFinder, neiRecipeFinder);
    }

    GuideSiteRecipeTagRenderer(GuideSiteRecipeExporter exporter, TargetStackResolver targetStackResolver,
        VanillaRecipeFinder vanillaRecipeFinder, NeiRecipeFinder neiRecipeFinder, RawHandlerFinder rawHandlerFinder,
        HandlerRuntime handlerRuntime) {
        this(
            exporter,
            GuideSiteItemIconResolver.NONE,
            targetStackResolver,
            vanillaRecipeFinder,
            neiRecipeFinder,
            rawHandlerFinder,
            handlerRuntime);
    }

    GuideSiteRecipeTagRenderer(GuideSiteRecipeExporter exporter, GuideSiteItemIconResolver itemIconResolver,
        TargetStackResolver targetStackResolver, VanillaRecipeFinder vanillaRecipeFinder,
        NeiRecipeFinder neiRecipeFinder, RawHandlerFinder rawHandlerFinder, HandlerRuntime handlerRuntime) {
        this.exporter = exporter;
        this.itemIconResolver = itemIconResolver != null ? itemIconResolver : GuideSiteItemIconResolver.NONE;
        this.targetStackResolver = targetStackResolver;
        this.vanillaRecipeFinder = vanillaRecipeFinder;
        this.neiRecipeFinder = neiRecipeFinder;
        this.rawHandlerFinder = rawHandlerFinder;
        this.handlerRuntime = handlerRuntime;
    }

    GuideSiteRecipeTagRenderer(GuideSiteRecipeExporter exporter, GuideSiteItemIconResolver itemIconResolver,
        TargetStackResolver targetStackResolver, VanillaRecipeFinder vanillaRecipeFinder,
        NeiRecipeFinder neiRecipeFinder, RawHandlerFinder rawHandlerFinder) {
        this(
            exporter,
            itemIconResolver,
            targetStackResolver,
            vanillaRecipeFinder,
            neiRecipeFinder,
            rawHandlerFinder,
            new HandlerRuntime() {

                @Override
                public @Nullable String handlerName(Object handler) {
                    return null;
                }

                @Override
                public @Nullable String overlayIdentifier(Object handler) {
                    return null;
                }

                @Override
                public int recipeCount(Object handler) {
                    return 0;
                }

                @Override
                public List<NeiRecipeLookup.Slot> readIngredientSlots(Object handler, int recipeIndex) {
                    return Collections.emptyList();
                }

                @Override
                public @Nullable NeiRecipeLookup.Slot readResultSlot(Object handler, int recipeIndex) {
                    return null;
                }

                @Override
                public List<NeiRecipeLookup.Slot> readOtherSlots(Object handler, int recipeIndex) {
                    return Collections.emptyList();
                }
            });
    }

    @Override
    public String render(MdxJsxElementFields element, String defaultNamespace) {
        String fallbackText = element.getAttributeString("fallbackText", "");
        String handlerOrderRaw = RecipeCompiler.trimToNull(element.getAttributeString("handlerOrder", null));
        Integer parsedHandlerOrder = parseInteger(handlerOrderRaw);
        if (handlerOrderRaw != null && parsedHandlerOrder == null) {
            return fallbackParagraph(fallbackText);
        }

        String limitRaw = RecipeCompiler.trimToNull(element.getAttributeString("limit", null));
        Integer parsedLimit = parseInteger(limitRaw);
        if (limitRaw != null && parsedLimit == null) {
            return fallbackParagraph(fallbackText);
        }

        boolean multi = "RecipesFor".equals(element.name());
        int limit = multi ? Integer.MAX_VALUE : 1;
        if (parsedLimit != null && parsedLimit > 0) {
            limit = parsedLimit;
        }

        return renderInternal(
            new RenderRequest(
                element.name() != null ? element.name() : "Recipe",
                element.getAttributeString("id", ""),
                fallbackText,
                defaultNamespace,
                RecipeCompiler.trimToNull(element.getAttributeString("handlerName", null)),
                RecipeCompiler.trimToNull(element.getAttributeString("handlerId", null)),
                parsedHandlerOrder != null ? parsedHandlerOrder : -1,
                RecipeCompiler.parseFilterExpr(
                    RecipeCompiler.trimToNull(element.getAttributeString("input", null)),
                    defaultNamespace),
                RecipeCompiler.parseFilterExpr(
                    RecipeCompiler.trimToNull(element.getAttributeString("output", null)),
                    defaultNamespace),
                limit,
                multi));
    }

    @Override
    public String render(String recipeId, String fallbackText, String defaultNamespace) {
        return renderInternal(
            new RenderRequest(
                "Recipe",
                recipeId,
                fallbackText,
                defaultNamespace,
                null,
                null,
                -1,
                RecipeCompiler.parseFilterExpr(null, defaultNamespace),
                RecipeCompiler.parseFilterExpr(null, defaultNamespace),
                1,
                false));
    }

    private String renderInternal(RenderRequest request) {
        String recipeId = RecipeCompiler.trimToNull(request.recipeId);
        if (recipeId == null) {
            return fallbackParagraph(request.fallbackText);
        }

        ItemStack targetStack;
        try {
            targetStack = targetStackResolver.resolve(recipeId, request.defaultNamespace);
        } catch (IllegalArgumentException e) {
            return fallbackParagraph(request.fallbackText);
        }
        if (targetStack == null || targetStack.getItem() == null) {
            return fallbackParagraph(request.fallbackText);
        }

        boolean hasRecipeFilter = !request.inputExpr.isEmpty() || !request.outputExpr.isEmpty();
        boolean hasHandlerFilter = request.handlerNameFilter != null || request.handlerIdFilter != null
            || request.handlerOrder >= 0;

        RawHandlerRenderResult rawHandlerResult = renderFromRawHandlers(request, targetStack, hasRecipeFilter);
        if (!rawHandlerResult.renderedRecipes.isEmpty()) {
            return exporter.renderRecipeCollection(rawHandlerResult.renderedRecipes, request.multi);
        }
        if (hasHandlerFilter && !rawHandlerResult.hadHandlersAfterFilter) {
            return fallbackParagraph(request.fallbackText);
        }

        List<String> renderedRecipes = renderFromNeiEntries(request, targetStack, hasRecipeFilter);
        if (!renderedRecipes.isEmpty()) {
            return exporter.renderRecipeCollection(renderedRecipes, request.multi);
        }

        renderedRecipes = renderFromVanillaEntries(request, targetStack, hasRecipeFilter);
        if (!renderedRecipes.isEmpty()) {
            return exporter.renderRecipeCollection(renderedRecipes, request.multi);
        }

        return fallbackParagraph(request.fallbackText);
    }

    private RawHandlerRenderResult renderFromRawHandlers(RenderRequest request, ItemStack targetStack,
        boolean hasRecipeFilter) {
        List<Object> rawHandlers = safeHandlers(rawHandlerFinder.findCraftingHandlers(targetStack));
        boolean hasHandlerFilter = request.handlerNameFilter != null || request.handlerIdFilter != null
            || request.handlerOrder >= 0;
        if (hasHandlerFilter) {
            rawHandlers = mergeHandlers(rawHandlers, safeHandlers(rawHandlerFinder.findUsageHandlers(targetStack)));
        }

        List<Object> handlers = RecipeCompiler.filterHandlers(
            rawHandlers,
            request.handlerNameFilter,
            request.handlerIdFilter,
            request.handlerOrder,
            handlerRuntime);
        if (handlers.isEmpty()) {
            return new RawHandlerRenderResult(Collections.emptyList(), false);
        }

        List<String> renderedRecipes = new ArrayList<>();
        for (int hi = 0; hi < handlers.size() && renderedRecipes.size() < request.limit; hi++) {
            Object handler = handlers.get(hi);
            int recipeCount = handlerRuntime.recipeCount(handler);
            for (int recipeIndex = 0; recipeIndex < recipeCount
                && renderedRecipes.size() < request.limit; recipeIndex++) {
                if (hasRecipeFilter && !RecipeCompiler
                    .recipeMatches(handler, recipeIndex, request.inputExpr, request.outputExpr, handlerRuntime)) {
                    continue;
                }

                String rendered = renderHandlerRecipe(handler, recipeIndex, targetStack);
                if (!rendered.isEmpty()) {
                    renderedRecipes.add(rendered);
                }
            }
        }
        return new RawHandlerRenderResult(renderedRecipes, true);
    }

    private List<String> renderFromNeiEntries(RenderRequest request, ItemStack targetStack, boolean hasRecipeFilter) {
        List<NeiRecipeLookup.Entry> neiEntries = neiRecipeFinder.findCraftingRecipes(targetStack);
        if (neiEntries.isEmpty()) {
            return Collections.emptyList();
        }

        List<String> renderedRecipes = new ArrayList<>();
        for (int i = 0; i < neiEntries.size() && renderedRecipes.size() < request.limit; i++) {
            NeiRecipeLookup.Entry entry = neiEntries.get(i);
            if (entry == null || entry.result == null || entry.ingredients == null || entry.ingredients.isEmpty()) {
                continue;
            }
            if (hasRecipeFilter && !RecipeCompiler.entryMatches(entry, request.inputExpr, request.outputExpr)) {
                continue;
            }

            String rendered = exporter.renderNeiOverlayGridItems(
                exporter.ingredientItemsFromNeiEntry(entry, itemIconResolver),
                exporter.resultItem(entry != null ? entry.result : null, targetStack, itemIconResolver),
                exporter.supportingSlotItemsFromNeiEntry(entry, itemIconResolver));
            if (!rendered.isEmpty()) {
                renderedRecipes.add(rendered);
            }
        }
        return renderedRecipes;
    }

    private List<String> renderFromVanillaEntries(RenderRequest request, ItemStack targetStack,
        boolean hasRecipeFilter) {
        List<RecipeLookup.Entry> vanillaEntries = vanillaRecipeFinder.findByOutput(targetStack);
        if (vanillaEntries.isEmpty()) {
            return Collections.emptyList();
        }

        List<String> renderedRecipes = new ArrayList<>();
        for (int i = 0; i < vanillaEntries.size() && renderedRecipes.size() < request.limit; i++) {
            RecipeLookup.Entry entry = vanillaEntries.get(i);
            if (entry == null || entry.result == null) {
                continue;
            }
            if (hasRecipeFilter && !RecipeCompiler.vanillaEntryMatches(entry, request.inputExpr, request.outputExpr)) {
                continue;
            }

            renderedRecipes.add(
                exporter.renderHtmlGridItems(
                    exporter.ingredientItemsFromVanillaEntry(entry, itemIconResolver),
                    exporter.itemInfo(entry.result, itemIconResolver)));
        }
        return renderedRecipes;
    }

    private String renderHandlerRecipe(Object handler, int recipeIndex, ItemStack targetStack) {
        List<List<GuideSiteExportedItem>> ingredients = exporter
            .ingredientItemsFromNeiSlots(handlerRuntime.readIngredientSlots(handler, recipeIndex), itemIconResolver);
        List<List<GuideSiteExportedItem>> supportingSlots = exporter
            .supportingSlotItemsFromNeiSlots(handlerRuntime.readOtherSlots(handler, recipeIndex), itemIconResolver);
        GuideSiteExportedItem resultItem = exporter
            .resultItem(handlerRuntime.readResultSlot(handler, recipeIndex), targetStack, itemIconResolver);
        if (ingredients.isEmpty() && supportingSlots.isEmpty() && resultItem.isEmpty()) {
            return "";
        }
        return exporter.renderNeiOverlayGridItems(ingredients, resultItem, supportingSlots);
    }

    private List<Object> mergeHandlers(List<Object> craftingHandlers, List<Object> usageHandlers) {
        if (craftingHandlers == null || craftingHandlers.isEmpty()) {
            return usageHandlers != null ? usageHandlers : Collections.emptyList();
        }
        if (usageHandlers == null || usageHandlers.isEmpty()) {
            return craftingHandlers;
        }

        List<Object> merged = new ArrayList<>(craftingHandlers.size() + usageHandlers.size());
        merged.addAll(craftingHandlers);
        IdentityHashMap<Object, Boolean> seen = new IdentityHashMap<>(merged.size());
        for (Object handler : craftingHandlers) {
            seen.put(handler, Boolean.TRUE);
        }
        for (Object handler : usageHandlers) {
            if (seen.put(handler, Boolean.TRUE) == null) {
                merged.add(handler);
            }
        }
        return merged;
    }

    private List<Object> safeHandlers(List<Object> handlers) {
        return handlers != null ? handlers : Collections.emptyList();
    }

    @Nullable
    private Integer parseInteger(@Nullable String raw) {
        if (raw == null || raw.trim()
            .isEmpty()) {
            return null;
        }
        try {
            return Integer.parseInt(raw.trim());
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    static String fallbackParagraph(String fallbackText) {
        if (fallbackText == null || fallbackText.isEmpty()) {
            return "";
        }
        return "<p>" + escapeHtml(fallbackText) + "</p>";
    }

    private static String escapeHtml(String text) {
        return text.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;");
    }

    private static final class RenderRequest {

        private final String tagName;
        private final String recipeId;
        private final String fallbackText;
        private final String defaultNamespace;
        @Nullable
        private final String handlerNameFilter;
        @Nullable
        private final String handlerIdFilter;
        private final int handlerOrder;
        private final RecipeCompiler.FilterExpr inputExpr;
        private final RecipeCompiler.FilterExpr outputExpr;
        private final int limit;
        private final boolean multi;

        private RenderRequest(String tagName, String recipeId, String fallbackText, String defaultNamespace,
            @Nullable String handlerNameFilter, @Nullable String handlerIdFilter, int handlerOrder,
            RecipeCompiler.FilterExpr inputExpr, RecipeCompiler.FilterExpr outputExpr, int limit, boolean multi) {
            this.tagName = tagName;
            this.recipeId = recipeId;
            this.fallbackText = fallbackText;
            this.defaultNamespace = defaultNamespace;
            this.handlerNameFilter = handlerNameFilter;
            this.handlerIdFilter = handlerIdFilter;
            this.handlerOrder = handlerOrder;
            this.inputExpr = inputExpr;
            this.outputExpr = outputExpr;
            this.limit = Math.max(1, limit);
            this.multi = multi;
        }
    }

    private static final class RawHandlerRenderResult {

        private final List<String> renderedRecipes;
        private final boolean hadHandlersAfterFilter;

        private RawHandlerRenderResult(List<String> renderedRecipes, boolean hadHandlersAfterFilter) {
            this.renderedRecipes = renderedRecipes;
            this.hadHandlersAfterFilter = hadHandlersAfterFilter;
        }
    }
}
