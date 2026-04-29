package com.hfstudio.guidenh.guide.compiler.tags;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Consumer;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hfstudio.guidenh.guide.compiler.IdUtils;
import com.hfstudio.guidenh.guide.compiler.PageCompiler;
import com.hfstudio.guidenh.guide.document.block.LytBlockContainer;
import com.hfstudio.guidenh.guide.document.block.LytHBox;
import com.hfstudio.guidenh.guide.document.block.LytParagraph;
import com.hfstudio.guidenh.guide.document.block.recipes.LytStandardRecipeBox;
import com.hfstudio.guidenh.guide.internal.recipe.LytNeiRecipeBox;
import com.hfstudio.guidenh.guide.internal.recipe.NeiRecipeLookup;
import com.hfstudio.guidenh.guide.internal.recipe.RecipeCache;
import com.hfstudio.guidenh.guide.internal.recipe.RecipeLookup;
import com.hfstudio.guidenh.libs.mdast.mdx.model.MdxJsxElementFields;

public class RecipeCompiler extends BlockTagCompiler {

    public static final Logger LOG = LoggerFactory.getLogger(RecipeCompiler.class);
    public static final int MULTI_GAP = 4;

    @Override
    public Set<String> getTagNames() {
        return new HashSet<>(Arrays.asList("Recipe", "RecipeFor", "RecipesFor"));
    }

    @Override
    protected void compile(PageCompiler compiler, LytBlockContainer parent, MdxJsxElementFields el) {
        String fallbackText = MdxAttrs.getString(compiler, parent, el, "fallbackText", null);

        String idStr = MdxAttrs.getString(compiler, parent, el, "id", null);
        if (idStr == null || idStr.isEmpty()) {
            parent.appendError(compiler, "Recipe tag requires an 'id' attribute", el);
            return;
        }

        String defaultNs = compiler.getPageId()
            .getResourceDomain();
        IdUtils.ParsedItemRef ref;
        try {
            ref = IdUtils.parseItemRef(idStr, defaultNs);
        } catch (IllegalArgumentException e) {
            parent.appendError(compiler, "Malformed id '" + idStr + "': " + e.getMessage(), el);
            return;
        }
        if (ref == null) {
            parent.appendError(compiler, "Blank id", el);
            return;
        }
        Item item = (Item) Item.itemRegistry.getObject(
            ref.id()
                .toString());
        if (item == null) {
            if (fallbackText != null) {
                if (!fallbackText.isEmpty()) parent.append(LytParagraph.of(fallbackText));
            } else {
                parent.appendError(compiler, "Missing item: " + ref.id(), el);
            }
            return;
        }

        boolean multi = "RecipesFor".equals(el.name());

        // Build the concrete query stack with meta + nbt (wildcard meta collapses to 0).
        ItemStack targetStack = new ItemStack(item, 1, ref.concreteMeta());
        if (ref.nbt() != null) {
            targetStack.stackTagCompound = (net.minecraft.nbt.NBTTagCompound) ref.nbt()
                .copy();
        }

        String handlerNameFilter = trimToNull(MdxAttrs.getString(compiler, parent, el, "handlerName", null));
        String handlerIdFilter = trimToNull(MdxAttrs.getString(compiler, parent, el, "handlerId", null));
        String handlerOrderStr = MdxAttrs.getString(compiler, parent, el, "handlerOrder", null);
        int handlerOrder = -1;
        if (handlerOrderStr != null && !handlerOrderStr.isEmpty()) {
            try {
                handlerOrder = Integer.parseInt(handlerOrderStr.trim());
            } catch (NumberFormatException ignored) {
                parent.appendError(compiler, "handlerOrder must be a non-negative integer", el);
                return;
            }
        }

        FilterExpr inputExpr = parseFilterExpr(compiler, parent, el, "input", defaultNs);
        FilterExpr outputExpr = parseFilterExpr(compiler, parent, el, "output", defaultNs);

        String limitStr = MdxAttrs.getString(compiler, parent, el, "limit", null);
        int limit = multi ? Integer.MAX_VALUE : 1;
        if (limitStr != null && !limitStr.isEmpty()) {
            try {
                int parsed = Integer.parseInt(limitStr.trim());
                if (parsed > 0) limit = parsed;
            } catch (NumberFormatException ignored) {
                parent.appendError(compiler, "limit must be a positive integer", el);
                return;
            }
        }
        boolean hasRecipeFilter = !inputExpr.isEmpty() || !outputExpr.isEmpty();
        boolean hasHandlerFilter = handlerNameFilter != null || handlerIdFilter != null || handlerOrder >= 0;

        // Prefer NEI-native handler rendering if available.
        List<Object> rawHandlers = RecipeCache.getCraftingHandlers(targetStack);
        // When a handler filter is specified, also consult usage handlers. This covers NEI handlers
        // that treat the target as an input rather than an output (anvil / repair, fuel, brewing
        // ingredient) — they never show up under getCraftingHandlers.
        if (hasHandlerFilter) {
            List<Object> usage = RecipeCache.getUsageHandlers(targetStack);
            if (!usage.isEmpty()) {
                if (rawHandlers.isEmpty()) {
                    rawHandlers = usage;
                } else {
                    List<Object> merged = new ArrayList<>(rawHandlers.size() + usage.size());
                    merged.addAll(rawHandlers);
                    // Dedup by identity — the same IRecipeHandler instance may appear in both lists.
                    java.util.IdentityHashMap<Object, Boolean> seen = new java.util.IdentityHashMap<>(merged.size());
                    for (Object h : rawHandlers) seen.put(h, Boolean.TRUE);
                    for (Object h : usage) if (seen.put(h, Boolean.TRUE) == null) merged.add(h);
                    rawHandlers = merged;
                }
            }
        }
        List<Object> handlers = filterHandlers(rawHandlers, handlerNameFilter, handlerIdFilter, handlerOrder);
        if (!handlers.isEmpty()) {
            List<LytNeiRecipeBox> boxes = new ArrayList<>();
            for (int hi = 0; hi < handlers.size() && boxes.size() < limit; hi++) {
                Object handler = handlers.get(hi);
                int num = NeiRecipeLookup.lookupNumRecipes(handler);
                for (int ri = 0; ri < num && boxes.size() < limit; ri++) {
                    if (hasRecipeFilter && !recipeMatches(handler, ri, inputExpr, outputExpr)) continue;
                    boxes.add(new LytNeiRecipeBox(handler, ri));
                }
            }
            if (!boxes.isEmpty()) {
                appendRecipes(parent, boxes, multi);
                return;
            }
        } else if (hasHandlerFilter) {
            // Handler filter eliminated every candidate. Respect fallbackText (if any) and bail quietly —
            // this is a legitimate authoring case (e.g. "only render when NEI + the relevant handler is
            // installed") and should not spam error overlays.
            if (fallbackText != null && !fallbackText.isEmpty()) {
                parent.append(LytParagraph.of(fallbackText));
            } else if (LOG.isDebugEnabled()) {
                LOG.debug(
                    "No NEI handler matched filters for {} (handlerName={}, handlerId={}, handlerOrder={})",
                    ref.id(),
                    handlerNameFilter,
                    handlerIdFilter,
                    handlerOrder);
            }
            return;
        }

        // Legacy fallback: raw slot data coming from NEI (no handler draw) or from vanilla crafting registry.
        List<NeiRecipeLookup.Entry> neiEntries = NeiRecipeLookup.findCraftingRecipes(targetStack);
        if (!neiEntries.isEmpty()) {
            List<LytStandardRecipeBox> boxes = new ArrayList<>();
            for (int i = 0; i < neiEntries.size() && boxes.size() < limit; i++) {
                var e = neiEntries.get(i);
                if (e.result == null || e.ingredients.isEmpty()) continue;
                if (hasRecipeFilter && !entryMatches(e, inputExpr, outputExpr)) continue;
                List<ItemStack> flat = new ArrayList<>(9);
                for (int s = 0; s < 9; s++) flat.add(null);
                int idx = 0;
                for (var slot : e.ingredients) {
                    if (idx >= 9) break;
                    if (slot.stacks != null && !slot.stacks.isEmpty()) flat.set(idx, slot.stacks.get(0));
                    idx++;
                }
                ItemStack resultStack = e.result.stacks != null && !e.result.stacks.isEmpty() ? e.result.stacks.get(0)
                    : null;
                if (resultStack != null) boxes.add(LytStandardRecipeBox.shapeless(flat, resultStack));
            }
            if (!boxes.isEmpty()) {
                appendRecipes(parent, boxes, multi);
                return;
            }
        }

        List<RecipeLookup.Entry> entries = RecipeLookup.findByOutput(item);
        if (entries.isEmpty()) {
            if (fallbackText != null) {
                if (!fallbackText.isEmpty()) parent.append(LytParagraph.of(fallbackText));
            } else {
                parent.appendError(compiler, "Couldn't find recipe for " + ref.id(), el);
            }
            return;
        }

        List<LytStandardRecipeBox> boxes = new ArrayList<>();
        for (int i = 0; i < entries.size() && boxes.size() < limit; i++) {
            var e = entries.get(i);
            if (hasRecipeFilter && !vanillaEntryMatches(e, inputExpr, outputExpr)) continue;
            var box = e.shapeless ? LytStandardRecipeBox.shapeless(RecipeLookup.asList(e), e.result)
                : LytStandardRecipeBox.shaped3x3(RecipeLookup.asList(e), e.result);
            boxes.add(box);
        }
        appendRecipes(parent, boxes, multi);
    }

    /**
     * Wraps multiple recipe boxes in a horizontal flex row that wraps onto additional lines when
     * the available width runs out. Single recipes are appended directly so they keep their
     * original block flow (no extra wrapper overhead).
     */
    public static void appendRecipes(LytBlockContainer parent,
        List<? extends com.hfstudio.guidenh.guide.document.block.LytBlock> boxes, boolean multi) {
        if (boxes.isEmpty()) return;
        if (!multi || boxes.size() == 1) {
            for (var b : boxes) parent.append(b);
            return;
        }
        LytHBox row = new LytHBox();
        row.setGap(MULTI_GAP);
        for (var b : boxes) row.append(b);
        parent.append(row);
    }

    /**
     * Applies {@code handlerName} (case-insensitive substring), {@code handlerId} (case-insensitive
     * overlay identifier equality) and {@code handlerOrder} (0-based index into the post-filter
     * list) in that order. Null / empty filters are no-ops.
     */
    public static List<Object> filterHandlers(List<Object> raw, @Nullable String nameFilter, @Nullable String idFilter,
        int order) {
        return filterHandlers(raw, nameFilter, idFilter, order, NEI_HANDLER_METADATA_READER);
    }

    public static List<Object> filterHandlers(List<Object> raw, @Nullable String nameFilter, @Nullable String idFilter,
        int order, HandlerMetadataReader metadataReader) {
        if (raw.isEmpty()) return raw;
        List<Object> out = new ArrayList<>(raw.size());
        String nameLower = nameFilter != null ? nameFilter.toLowerCase(Locale.ROOT) : null;
        String idLower = idFilter != null ? idFilter.toLowerCase(Locale.ROOT) : null;
        for (Object h : raw) {
            if (nameLower != null) {
                String n = metadataReader.handlerName(h);
                if (n == null || !n.toLowerCase(Locale.ROOT)
                    .contains(nameLower)) continue;
            }
            if (idLower != null) {
                String oid = metadataReader.overlayIdentifier(h);
                boolean match = oid != null && oid.toLowerCase(Locale.ROOT)
                    .equals(idLower);
                if (!match) {
                    // Secondary: match the handler class simple-name (case-insensitive substring).
                    // Covers handlers whose overlay identifier differs from their canonical name —
                    // e.g. the user writes handlerId="fuel" and the class is "FuelRecipeHandler".
                    String cn = h.getClass()
                        .getSimpleName();
                    match = cn.toLowerCase(Locale.ROOT)
                        .contains(idLower);
                }
                if (!match) continue;
            }
            out.add(h);
        }
        if (order >= 0) {
            if (order < out.size()) {
                Object pick = out.get(order);
                out.clear();
                out.add(pick);
            } else {
                out.clear();
            }
        }
        return out;
    }

    public static @Nullable String trimToNull(@Nullable String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    /**
     * A single atom in a filter expression: an item reference plus an optional negation flag.
     * {@code negated == true} means "no stack matches this ref" (for ingredient-slot lists) or "the
     * target stack does not match this ref" (for single-stack matching).
     */
    private static final class FilterTerm {

        private final IdUtils.ParsedItemRef ref;
        private final boolean negated;

        private FilterTerm(IdUtils.ParsedItemRef ref, boolean negated) {
            this.ref = ref;
            this.negated = negated;
        }
    }

    /**
     * Disjunctive-normal-form filter expression:
     * 
     * <pre>
     *   expr  := orGroup ( ',' orGroup )*   // any group satisfied -> expression holds
     *   group := term    ( '&' term    )*   // every term in the group must be satisfied
     *   term  := [ '!' ] itemRef            // '!' flips the match sense
     * </pre>
     * 
     * Empty expression (from an absent/blank attribute) means "no filter" and is cheap to check
     * via {@link #isEmpty()}.
     */
    public static final class FilterExpr {

        private static final FilterExpr EMPTY = new FilterExpr(java.util.Collections.<List<FilterTerm>>emptyList());
        private final List<List<FilterTerm>> orGroups;

        private FilterExpr(List<List<FilterTerm>> orGroups) {
            this.orGroups = orGroups;
        }

        public boolean isEmpty() {
            return orGroups.isEmpty();
        }
    }

    public interface HandlerMetadataReader {

        @Nullable
        String handlerName(Object handler);

        @Nullable
        String overlayIdentifier(Object handler);
    }

    public interface HandlerRecipeAccess {

        List<NeiRecipeLookup.Slot> readIngredientSlots(Object handler, int recipeIndex);

        @Nullable
        NeiRecipeLookup.Slot readResultSlot(Object handler, int recipeIndex);
    }

    private static final HandlerMetadataReader NEI_HANDLER_METADATA_READER = new HandlerMetadataReader() {

        @Override
        public @Nullable String handlerName(Object handler) {
            return NeiRecipeLookup.lookupHandlerName(handler);
        }

        @Override
        public @Nullable String overlayIdentifier(Object handler) {
            return NeiRecipeLookup.lookupOverlayIdentifier(handler);
        }
    };

    private static final HandlerRecipeAccess NEI_HANDLER_RECIPE_ACCESS = new HandlerRecipeAccess() {

        @Override
        public List<NeiRecipeLookup.Slot> readIngredientSlots(Object handler, int recipeIndex) {
            return NeiRecipeLookup.readIngredientSlots(handler, recipeIndex);
        }

        @Override
        public @Nullable NeiRecipeLookup.Slot readResultSlot(Object handler, int recipeIndex) {
            return NeiRecipeLookup.readResultSlot(handler, recipeIndex);
        }
    };

    /**
     * Parse an {@code input} / {@code output} attribute supporting OR (',') + AND ('&') + NOT ('!').
     * Malformed tokens emit a compile error and are skipped; a group with no surviving terms is
     * dropped, and if every group is dropped the result is {@link FilterExpr#EMPTY} (i.e. "no
     * filter", which is safer than "always fail").
     */
    public static FilterExpr parseFilterExpr(PageCompiler compiler, LytBlockContainer parent, MdxJsxElementFields el,
        String attr, String defaultNs) {
        String raw = trimToNull(MdxAttrs.getString(compiler, parent, el, attr, null));
        return parseFilterExpr(raw, attr, defaultNs, new Consumer<String>() {

            @Override
            public void accept(String message) {
                parent.appendError(compiler, message, el);
            }
        });
    }

    public static FilterExpr parseFilterExpr(@Nullable String raw, String defaultNs) {
        return parseFilterExpr(raw, null, defaultNs, null);
    }

    private static FilterExpr parseFilterExpr(@Nullable String raw, @Nullable String attr, String defaultNs,
        @Nullable Consumer<String> errorSink) {
        if (raw == null) return FilterExpr.EMPTY;
        List<List<FilterTerm>> groups = new ArrayList<>();
        for (String orPart : raw.split(",")) {
            String orTrim = orPart.trim();
            if (orTrim.isEmpty()) continue;
            List<FilterTerm> andTerms = new ArrayList<>();
            for (String andPart : orTrim.split("&")) {
                String token = andPart.trim();
                if (token.isEmpty()) continue;
                boolean negated = false;
                if (token.startsWith("!")) {
                    negated = true;
                    token = token.substring(1)
                        .trim();
                    if (token.isEmpty()) {
                        if (errorSink != null) {
                            errorSink.accept("Empty " + filterAttrName(attr) + " negation token '!' has no id");
                        }
                        continue;
                    }
                }
                try {
                    IdUtils.ParsedItemRef p = IdUtils.parseItemRef(token, defaultNs);
                    if (p != null) andTerms.add(new FilterTerm(p, negated));
                } catch (IllegalArgumentException e) {
                    if (errorSink != null) {
                        errorSink
                            .accept("Malformed " + filterAttrName(attr) + " filter '" + token + "': " + e.getMessage());
                    }
                }
            }
            if (!andTerms.isEmpty()) groups.add(andTerms);
        }
        return groups.isEmpty() ? FilterExpr.EMPTY : new FilterExpr(groups);
    }

    private static String filterAttrName(@Nullable String attr) {
        return attr == null || attr.isEmpty() ? "filter" : attr;
    }

    /**
     * {@code true} when {@code stack} satisfies {@code ref}: item identity match, plus meta equality
     * when {@code ref} isn't wildcard-meta, plus NBT equality when {@code ref.nbt()} is non-null.
     * NBT comparison uses {@link net.minecraft.nbt.NBTBase#equals} which does structural compare.
     */
    public static boolean stackMatches(@Nullable ItemStack stack, IdUtils.ParsedItemRef ref) {
        if (stack == null) return false;
        Item refItem = (Item) Item.itemRegistry.getObject(
            ref.id()
                .toString());
        if (refItem == null || stack.getItem() != refItem) return false;
        if (!ref.isWildcardMeta() && stack.getItemDamage() != ref.meta()) return false;
        if (ref.nbt() != null) {
            if (stack.stackTagCompound == null) return false;
            if (!ref.nbt()
                .equals(stack.stackTagCompound)) return false;
        }
        return true;
    }

    /** {@code true} when any stack inside any slot matches {@code ref}. */
    public static boolean slotsContain(@Nullable List<NeiRecipeLookup.Slot> slots, IdUtils.ParsedItemRef ref) {
        if (slots == null) return false;
        for (NeiRecipeLookup.Slot s : slots) {
            if (s == null || s.stacks == null) continue;
            for (int j = 0, m = s.stacks.size(); j < m; j++) {
                if (stackMatches(s.stacks.get(j), ref)) return true;
            }
        }
        return false;
    }

    public static boolean resultSlotContains(@Nullable NeiRecipeLookup.Slot result, IdUtils.ParsedItemRef ref) {
        if (result == null || result.stacks == null) return false;
        for (int i = 0, n = result.stacks.size(); i < n; i++) {
            if (stackMatches(result.stacks.get(i), ref)) return true;
        }
        return false;
    }

    /** Evaluate a DNF expression against an ingredient-style slot list. */
    public static boolean evalSlots(List<NeiRecipeLookup.Slot> slots, FilterExpr expr) {
        if (expr.isEmpty()) return true;
        if (slots == null) return false;
        for (List<FilterTerm> group : expr.orGroups) {
            boolean allOk = true;
            for (FilterTerm t : group) {
                boolean present = slotsContain(slots, t.ref);
                if (present == t.negated) {
                    allOk = false;
                    break;
                }
            }
            if (allOk) return true;
        }
        return false;
    }

    /** Evaluate a DNF expression against a single-result slot (positional-stack cycling). */
    public static boolean evalResultSlot(@Nullable NeiRecipeLookup.Slot result, FilterExpr expr) {
        if (expr.isEmpty()) return true;
        if (result == null) return false;
        for (List<FilterTerm> group : expr.orGroups) {
            boolean allOk = true;
            for (FilterTerm t : group) {
                boolean present = resultSlotContains(result, t.ref);
                if (present == t.negated) {
                    allOk = false;
                    break;
                }
            }
            if (allOk) return true;
        }
        return false;
    }

    /** Evaluate a DNF expression against a single concrete stack (vanilla result). */
    public static boolean evalStack(@Nullable ItemStack stack, FilterExpr expr) {
        if (expr.isEmpty()) return true;
        for (List<FilterTerm> group : expr.orGroups) {
            boolean allOk = true;
            for (FilterTerm t : group) {
                boolean present = stackMatches(stack, t.ref);
                if (present == t.negated) {
                    allOk = false;
                    break;
                }
            }
            if (allOk) return true;
        }
        return false;
    }

    /** Evaluate a DNF expression against a flat ingredient array (vanilla 3x3). */
    public static boolean evalArray(ItemStack[] stacks, FilterExpr expr) {
        if (expr.isEmpty()) return true;
        for (List<FilterTerm> group : expr.orGroups) {
            boolean allOk = true;
            for (FilterTerm t : group) {
                boolean present = false;
                for (ItemStack s : stacks) {
                    if (stackMatches(s, t.ref)) {
                        present = true;
                        break;
                    }
                }
                if (present == t.negated) {
                    allOk = false;
                    break;
                }
            }
            if (allOk) return true;
        }
        return false;
    }

    public static boolean recipeMatches(Object handler, int recipeIndex, FilterExpr inputExpr, FilterExpr outputExpr) {
        return recipeMatches(handler, recipeIndex, inputExpr, outputExpr, NEI_HANDLER_RECIPE_ACCESS);
    }

    public static boolean recipeMatches(Object handler, int recipeIndex, FilterExpr inputExpr, FilterExpr outputExpr,
        HandlerRecipeAccess recipeAccess) {
        if (!outputExpr.isEmpty()) {
            if (!evalResultSlot(recipeAccess.readResultSlot(handler, recipeIndex), outputExpr)) return false;
        }
        if (!inputExpr.isEmpty()) {
            if (!evalSlots(recipeAccess.readIngredientSlots(handler, recipeIndex), inputExpr)) return false;
        }
        return true;
    }

    public static boolean entryMatches(NeiRecipeLookup.Entry e, FilterExpr inputExpr, FilterExpr outputExpr) {
        if (!outputExpr.isEmpty() && !evalResultSlot(e.result, outputExpr)) return false;
        if (!inputExpr.isEmpty() && !evalSlots(e.ingredients, inputExpr)) return false;
        return true;
    }

    public static boolean vanillaEntryMatches(RecipeLookup.Entry e, FilterExpr inputExpr, FilterExpr outputExpr) {
        if (!outputExpr.isEmpty() && !evalStack(e.result, outputExpr)) return false;
        if (!inputExpr.isEmpty() && !evalArray(e.input3x3, inputExpr)) return false;
        return true;
    }
}
