package com.hfstudio.guidenh.guide.internal.item;

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.oredict.OreDictionary;

import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.guide.compiler.IdUtils;
import com.hfstudio.guidenh.guide.scene.support.GuideDebugLog;

public class GuideDisplayItemStacks {

    private static final int META_CACHE_MAX = 2048;
    private static final int MAX_RENDER_PASSES = 16;
    private static final Map<String, Boolean> DISPLAYABLE_META_CACHE = Collections
        .synchronizedMap(new LinkedHashMap<>(256, 0.75f, true) {

            @Override
            protected boolean removeEldestEntry(Map.Entry<String, Boolean> eldest) {
                return size() > META_CACHE_MAX;
            }
        });
    private static final Set<String> WARNED_DISPLAY_FAILURES = Collections.synchronizedSet(new HashSet<>());

    private GuideDisplayItemStacks() {}

    @Nullable
    public static ItemStack resolveItemStack(@Nullable String idText, String defaultNamespace) {
        IdUtils.ParsedItemRef ref = IdUtils.parseItemRef(idText, defaultNamespace);
        if (ref == null) {
            return null;
        }

        Item item = (Item) Item.itemRegistry.getObject(ref.rawKey());
        if (item == null) {
            return null;
        }

        ItemStack stack = new ItemStack(item, 1, ref.concreteMeta());
        if (ref.nbt() != null) {
            stack.stackTagCompound = (NBTTagCompound) ref.nbt()
                .copy();
        }
        return isSafeForDisplay(stack, ref.hasExplicitMeta() && !ref.isWildcardMeta()) ? stack : null;
    }

    @Nullable
    public static ItemStack resolveOreStack(@Nullable String oreName) {
        if (oreName == null || oreName.trim()
            .isEmpty()) {
            return null;
        }
        List<ItemStack> ores = OreDictionary.getOres(oreName);
        if (ores == null || ores.isEmpty()) {
            return null;
        }
        for (ItemStack stack : ores) {
            if (stack == null) {
                continue;
            }
            ItemStack displayStack = stack.copy();
            if (displayStack.getItemDamage() == OreDictionary.WILDCARD_VALUE) {
                displayStack.setItemDamage(0);
            }
            if (isSafeForDisplay(displayStack, true)) {
                return displayStack;
            }
        }
        return null;
    }

    public static boolean isSafeForDisplay(@Nullable ItemStack stack, boolean validateMeta) {
        if (stack == null || stack.getItem() == null || stack.stackSize == 0) {
            return false;
        }
        if (validateMeta && !isMetaDisplayable(stack)) {
            return false;
        }
        try {
            stack.getDisplayName();
            return true;
        } catch (Throwable t) {
            warnOnce("display:" + describe(stack), "[GuideNH] Item stack is not safe for guide display: {}", stack, t);
            return false;
        }
    }

    public static void warnRenderFailure(String source, @Nullable ItemStack stack, Throwable t) {
        warnOnce(
            "render:" + source
                + ":"
                + describe(stack)
                + ":"
                + t.getClass()
                    .getName(),
            "[GuideNH] [{}] Failed to render item stack {}; skipping this item",
            source,
            describe(stack),
            t);
    }

    public static String describe(@Nullable ItemStack stack) {
        if (stack == null) {
            return "null";
        }
        Item item = stack.getItem();
        Object name = item != null ? Item.itemRegistry.getNameForObject(item) : null;
        return (name != null ? name.toString() : "unknown") + ":" + stack.getItemDamage();
    }

    private static boolean isMetaDisplayable(ItemStack stack) {
        Item item = stack.getItem();
        int meta = stack.getItemDamage();
        String key = metaCacheKey(item, meta);
        Boolean cached = DISPLAYABLE_META_CACHE.get(key);
        if (cached != null) {
            return cached;
        }

        boolean displayable = probeItemRenderAccess(stack, item, meta, key);
        DISPLAYABLE_META_CACHE.put(key, displayable);
        return displayable;
    }

    private static boolean probeItemRenderAccess(ItemStack stack, Item item, int meta, String key) {
        try {
            stack.getIconIndex();
            item.getColorFromItemStack(stack, 0);
            if (item.requiresMultipleRenderPasses()) {
                int passes = item.getRenderPasses(meta);
                if (passes <= 0 || passes > MAX_RENDER_PASSES) {
                    warnOnce(
                        "passes:" + key,
                        "[GuideNH] Item meta {} reports unsupported render pass count {}; skipping guide display",
                        key,
                        passes,
                        null);
                    return false;
                }
                for (int pass = 0; pass < passes; pass++) {
                    item.getIcon(stack, pass);
                    item.getColorFromItemStack(stack, pass);
                }
            }
            return true;
        } catch (Throwable t) {
            warnOnce(
                "meta:" + key,
                "[GuideNH] Item meta {} is not safe for guide rendering; skipping guide display",
                key,
                t);
            return false;
        }
    }

    private static String metaCacheKey(Item item, int meta) {
        Object name = Item.itemRegistry.getNameForObject(item);
        return (name != null ? name.toString()
            : item.getClass()
                .getName())
            + ":"
            + meta;
    }

    private static void warnOnce(String key, String message, Object arg, @Nullable Throwable t) {
        if (WARNED_DISPLAY_FAILURES.add(key)) {
            if (t == null) {
                GuideDebugLog.warnAlways(message, arg);
            } else {
                GuideDebugLog.warnAlways(message, arg, t);
            }
        }
    }

    private static void warnOnce(String key, String message, Object arg1, Object arg2, @Nullable Throwable t) {
        if (WARNED_DISPLAY_FAILURES.add(key)) {
            if (t == null) {
                GuideDebugLog.warnAlways(message, arg1, arg2);
            } else {
                GuideDebugLog.warnAlways(message, arg1, arg2, t);
            }
        }
    }
}
