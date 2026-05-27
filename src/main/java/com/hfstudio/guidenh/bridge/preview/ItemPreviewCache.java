package com.hfstudio.guidenh.bridge.preview;

import java.util.LinkedHashMap;
import java.util.Map;

public class ItemPreviewCache {

    private final int maxEntries;
    private final Map<ItemPreviewCacheKey, ItemPreviewPayload> cache;

    public ItemPreviewCache(int maxEntries) {
        this.maxEntries = Math.max(1, maxEntries);
        this.cache = new LinkedHashMap<ItemPreviewCacheKey, ItemPreviewPayload>(16, 0.75f, true) {

            @Override
            protected boolean removeEldestEntry(Map.Entry<ItemPreviewCacheKey, ItemPreviewPayload> eldest) {
                return size() > ItemPreviewCache.this.maxEntries;
            }
        };
    }

    public synchronized ItemPreviewPayload get(ItemPreviewCacheKey key) {
        return cache.get(key);
    }

    public synchronized void put(ItemPreviewCacheKey key, ItemPreviewPayload value) {
        cache.put(key, value);
    }
}
