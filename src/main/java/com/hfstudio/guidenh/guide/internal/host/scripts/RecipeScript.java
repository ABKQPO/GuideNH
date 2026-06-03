package com.hfstudio.guidenh.guide.internal.host.scripts;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.guide.compiler.tags.RecipeCompiler;
import com.hfstudio.guidenh.guide.compiler.tags.RecipeCompiler.HandlerMetadataReader;
import com.hfstudio.guidenh.guide.compiler.tags.RecipeCompiler.HandlerRecipeAccess;
import com.hfstudio.guidenh.guide.compiler.tags.RecipeCompiler.RecipePlaceholder;
import com.hfstudio.guidenh.guide.document.block.LytBlock;
import com.hfstudio.guidenh.guide.document.block.LytHBox;
import com.hfstudio.guidenh.guide.document.block.LytParagraph;
import com.hfstudio.guidenh.guide.document.block.recipes.LytStandardRecipeBox;
import com.hfstudio.guidenh.guide.document.flow.LytFlowInlineBlock;
import com.hfstudio.guidenh.guide.internal.host.EventType;
import com.hfstudio.guidenh.guide.internal.host.LytEvent;
import com.hfstudio.guidenh.guide.internal.host.LytScript;
import com.hfstudio.guidenh.guide.internal.host.ScriptContext;
import com.hfstudio.guidenh.guide.internal.host.ScriptType;
import com.hfstudio.guidenh.guide.internal.recipe.LytNeiRecipeBox;
import com.hfstudio.guidenh.guide.internal.recipe.RecipeCache;
import com.hfstudio.guidenh.guide.internal.recipe.RecipeLookup;
import com.hfstudio.guidenh.integration.api.GuideNhIntegrationRegistry;
import com.hfstudio.guidenh.integration.api.RecipeEntry;
import com.hfstudio.guidenh.integration.api.RecipeSlot;
import com.hfstudio.guidenh.integration.nei.NeiRecipeLookup;

import cpw.mods.fml.common.FMLLog;

public class RecipeScript implements LytScript {

    @Override
    public ScriptType type() {
        return ScriptType.JAVA;
    }

    @Override
    public String styleClass() {
        return "Recipe";
    }

    @Override
    public boolean isAsync() {
        return true;
    }

    @Override
    @SuppressWarnings("deprecation")
    public void onEvent(Object node, LytEvent event, ScriptContext ctx) {
        if (event.type() != EventType.MOUNT) return;

        RecipePlaceholder ph = LytFlowInlineBlock.unwrapPlaceholder(node, RecipePlaceholder.class);
        if (ph == null) return;

        Item item = (Item) Item.itemRegistry.getObject(ph.ref.rawKey());
        if (item == null) {
            showFallback(ctx, ph, "Recipe item not found: " + ph.idStr);
            return;
        }
        ItemStack targetStack = new ItemStack(item, 1, ph.ref.concreteMeta());
        if (ph.ref.nbt() != null) {
            targetStack.stackTagCompound = (NBTTagCompound) ph.ref.nbt()
                .copy();
        }

        boolean hasHandlerFilter = ph.handlerName != null || ph.handlerId != null || ph.handlerOrder >= 0;
        boolean hasRecipeFilter = !ph.inputExpr.isEmpty() || !ph.outputExpr.isEmpty();
        int limit = ph.limit;
        boolean usageQuery = ph.usageQuery;

        // NEI handler path
        List<Object> rawHandlers = usageQuery ? RecipeCache.getUsageHandlers(targetStack)
            : RecipeCache.getCraftingHandlers(targetStack);
        if (!usageQuery && hasHandlerFilter) {
            List<Object> usage = RecipeCache.getUsageHandlers(targetStack);
            if (!usage.isEmpty()) {
                if (rawHandlers.isEmpty()) {
                    rawHandlers = usage;
                } else {
                    List<Object> merged = new ArrayList<>(rawHandlers.size() + usage.size());
                    merged.addAll(rawHandlers);
                    IdentityHashMap<Object, Boolean> seen = new IdentityHashMap<>(merged.size());
                    for (Object h : rawHandlers) seen.put(h, Boolean.TRUE);
                    for (Object h : usage) if (seen.put(h, Boolean.TRUE) == null) merged.add(h);
                    rawHandlers = merged;
                }
            }
        }
        HandlerMetadataReader metadataReader = new HandlerMetadataReader() {

            @Override
            public @Nullable String handlerName(Object h) {
                return NeiRecipeLookup.lookupHandlerName(h);
            }

            @Override
            public @Nullable String handlerId(Object h) {
                return NeiRecipeLookup.lookupHandlerId(h);
            }

            @Override
            public @Nullable String overlayIdentifier(Object h) {
                return NeiRecipeLookup.lookupOverlayIdentifier(h);
            }
        };
        HandlerRecipeAccess recipeAccess = new HandlerRecipeAccess() {

            @Override
            public List<NeiRecipeLookup.Slot> readIngredientSlots(Object h, int ri) {
                return NeiRecipeLookup.readIngredientSlots(h, ri);
            }

            @Override
            public @Nullable NeiRecipeLookup.Slot readResultSlot(Object h, int ri) {
                return NeiRecipeLookup.readResultSlot(h, ri);
            }
        };
        List<Object> handlers = RecipeCompiler
            .filterHandlers(rawHandlers, ph.handlerName, ph.handlerId, ph.handlerOrder, metadataReader);
        if (!handlers.isEmpty()) {
            List<LytNeiRecipeBox> boxes = new ArrayList<>();
            for (int hi = 0; hi < handlers.size() && boxes.size() < limit; hi++) {
                Object handler = handlers.get(hi);
                int num = GuideNhIntegrationRegistry.global()
                    .lookupRecipeHandlerRecipeCount(handler);
                int recipeStart = ph.recipeIndex >= 0 ? ph.recipeIndex : 0;
                int recipeEnd = ph.recipeIndex >= 0 ? Math.min(num, ph.recipeIndex + 1) : num;
                for (int ri = recipeStart; ri < recipeEnd && boxes.size() < limit; ri++) {
                    if (hasRecipeFilter
                        && !RecipeCompiler.recipeMatches(handler, ri, ph.inputExpr, ph.outputExpr, recipeAccess))
                        continue;
                    boxes.add(new LytNeiRecipeBox(handler, ri, !usageQuery));
                }
            }
            if (!boxes.isEmpty()) {
                ctx.replace(buildResult(boxes));
                return;
            }
            if (ph.recipeIndex >= 0) {
                showFallback(ctx, ph, "Recipe index " + ph.recipeIndex + " not found for " + ph.idStr);
                return;
            }
        } else if (hasHandlerFilter) {
            if (ph.fallbackText != null && !ph.fallbackText.isEmpty()) {
                String handlerPart = "";
                if (ph.handlerName != null || ph.handlerId != null) {
                    handlerPart = " with handler " + (ph.handlerName != null ? ph.handlerName : ph.handlerId);
                }
                showFallback(ctx, ph, "No recipe found for " + ph.idStr + handlerPart);
            } else if (FMLLog.getLogger()
                .isDebugEnabled()) {
                    FMLLog.getLogger()
                        .debug("Recipe handler filter eliminated all candidates for {}", ph.idStr);
                }
            return;
        }

        // Integration recipe entries
        List<RecipeEntry> recipeEntries = usageQuery ? Collections.<RecipeEntry>emptyList()
            : GuideNhIntegrationRegistry.global()
                .findCraftingRecipeEntries(targetStack);
        if (!recipeEntries.isEmpty()) {
            List<LytStandardRecipeBox> boxes = new ArrayList<>();
            int entryStart = ph.recipeIndex >= 0 ? ph.recipeIndex : 0;
            int entryEnd = ph.recipeIndex >= 0 ? Math.min(recipeEntries.size(), ph.recipeIndex + 1)
                : recipeEntries.size();
            for (int i = entryStart; i < entryEnd && boxes.size() < limit; i++) {
                var e = recipeEntries.get(i);
                if (e.result() == null || e.ingredients()
                    .isEmpty()) continue;
                if (hasRecipeFilter && !RecipeCompiler.entryMatches(e, ph.inputExpr, ph.outputExpr)) continue;
                List<ItemStack> flat = new ArrayList<>(9);
                for (int s = 0; s < 9; s++) flat.add(null);
                int idx = 0;
                for (RecipeSlot slot : e.ingredients()) {
                    if (idx >= 9) break;
                    if (slot.stacks() != null && !slot.stacks()
                        .isEmpty()) flat.set(
                            idx,
                            slot.stacks()
                                .get(0));
                    idx++;
                }
                ItemStack resultStack = e.result()
                    .stacks() != null
                    && !e.result()
                        .stacks()
                        .isEmpty() ? e.result()
                            .stacks()
                            .get(0) : null;
                if (resultStack != null) boxes.add(LytStandardRecipeBox.shapeless(flat, resultStack));
            }
            if (!boxes.isEmpty()) {
                ctx.replace(buildResult(boxes));
                return;
            }
        }

        // Vanilla recipe fallback
        List<RecipeLookup.Entry> entries = usageQuery ? Collections.<RecipeLookup.Entry>emptyList()
            : RecipeLookup.findByOutput(item);
        if (entries.isEmpty()) {
            showFallback(ctx, ph, "No recipe found for " + ph.idStr);
            return;
        }

        List<LytStandardRecipeBox> boxes = new ArrayList<>();
        int vanillaStart = ph.recipeIndex >= 0 ? ph.recipeIndex : 0;
        int vanillaEnd = ph.recipeIndex >= 0 ? Math.min(entries.size(), ph.recipeIndex + 1) : entries.size();
        for (int i = vanillaStart; i < vanillaEnd && boxes.size() < limit; i++) {
            var e = entries.get(i);
            if (hasRecipeFilter && !RecipeCompiler.vanillaEntryMatches(e, ph.inputExpr, ph.outputExpr)) continue;
            var box = e.shapeless ? LytStandardRecipeBox.shapeless(RecipeLookup.asList(e), e.result)
                : LytStandardRecipeBox.shaped3x3(RecipeLookup.asList(e), e.result);
            boxes.add(box);
        }
        if (!boxes.isEmpty()) {
            ctx.replace(buildResult(boxes));
            return;
        }
        showFallback(ctx, ph, "No recipe found for " + ph.idStr);
    }

    @SuppressWarnings("unchecked")
    private static LytBlock buildResult(List<?> boxes) {
        return buildResultTyped((List<LytBlock>) boxes);
    }

    private static LytBlock buildResultTyped(List<LytBlock> boxes) {
        if (boxes.size() == 1) return boxes.get(0);
        var row = new LytHBox();
        row.setGap(RecipeCompiler.MULTI_GAP);
        for (var b : boxes) row.append(b);
        return row;
    }

    private void showFallback(ScriptContext ctx, RecipePlaceholder ph, String autoMessage) {
        String text = (ph.fallbackText != null && !ph.fallbackText.isEmpty()) ? ph.fallbackText : autoMessage;
        ctx.replace(LytParagraph.error(text));
    }

}
