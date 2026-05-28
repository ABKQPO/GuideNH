package com.hfstudio.guidenh.guide.internal.recipe;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import com.hfstudio.guidenh.integration.api.GuideNhIntegrationRegistry;

/**
 * Caches raw recipe handler results per target (item + meta + NBT) for the lifetime of the
 * currently-loaded guide set. Cleared via {@link #clear()} on {@code F3+T} reload (see
 * {@code GuideReloadListener}). A plain HashMap is sufficient: entries are small (a list of handler
 * references) and keyed by a lightweight composite without referencing the original ItemStack.
 */
public class RecipeCache {

    public static final int MAX_CACHE_ENTRIES = 512;
    public static final Map<Key, List<Object>> HANDLERS = createCache();
    public static final Map<Key, List<Object>> USAGE_HANDLERS = createCache();

    private RecipeCache() {}

    public static synchronized List<Object> getCraftingHandlers(ItemStack target) {
        if (target == null) return List.of();
        Key key = Key.of(target);
        List<Object> cached = HANDLERS.get(key);
        if (cached != null) return cached;
        List<Object> fresh = GuideNhIntegrationRegistry.global()
            .queryRawCraftingHandlers(target);
        List<Object> stored = fresh.isEmpty() ? List.of() : new ArrayList<>(fresh);
        HANDLERS.put(key, stored);
        return stored;
    }

    /**
     * Cached raw usage handlers. Used to cover handlers that consume {@code target} as an input
     * (anvil / fuel / brewing ingredient) and thus do not appear in the crafting-handler list.
     */
    public static synchronized List<Object> getUsageHandlers(ItemStack target) {
        if (target == null) return List.of();
        Key key = Key.of(target);
        List<Object> cached = USAGE_HANDLERS.get(key);
        if (cached != null) return cached;
        List<Object> fresh = GuideNhIntegrationRegistry.global()
            .queryRawUsageHandlers(target);
        List<Object> stored = fresh.isEmpty() ? List.of() : new ArrayList<>(fresh);
        USAGE_HANDLERS.put(key, stored);
        return stored;
    }

    public static synchronized void clear() {
        HANDLERS.clear();
        USAGE_HANDLERS.clear();
    }

    private static Map<Key, List<Object>> createCache() {
        return new LinkedHashMap<Key, List<Object>>(16, 0.75f, true) {

            @Override
            protected boolean removeEldestEntry(Entry<Key, List<Object>> eldest) {
                return size() > MAX_CACHE_ENTRIES;
            }
        };
    }

    public static class Key {

        final Item item;
        final int meta;
        final String nbt;

        Key(Item item, int meta, String nbt) {
            this.item = item;
            this.meta = meta;
            this.nbt = nbt;
        }

        static Key of(ItemStack s) {
            String nbt = s.stackTagCompound == null ? "" : s.stackTagCompound.toString();
            return new Key(s.getItem(), s.getItemDamage(), nbt);
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof Key k)) return false;
            return meta == k.meta && item == k.item && nbt.equals(k.nbt);
        }

        @Override
        public int hashCode() {
            int h = System.identityHashCode(item);
            h = 31 * h + meta;
            h = 31 * h + nbt.hashCode();
            return h;
        }
    }
}
