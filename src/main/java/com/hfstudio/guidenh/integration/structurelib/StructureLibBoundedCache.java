package com.hfstudio.guidenh.integration.structurelib;

import java.util.LinkedHashMap;
import java.util.Map;

public class StructureLibBoundedCache<K, V> {

    private final Map<K, V> entries;

    public StructureLibBoundedCache(int maxEntries) {
        int capacity = Math.max(1, maxEntries);
        this.entries = new LinkedHashMap<K, V>(capacity, 0.75F, true) {

            @Override
            protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
                return size() > capacity;
            }
        };
    }

    public synchronized V get(K key) {
        return entries.get(key);
    }

    public synchronized void put(K key, V value) {
        if (key == null || value == null) {
            return;
        }
        entries.put(key, value);
    }

    public synchronized int size() {
        return entries.size();
    }

    public synchronized void clear() {
        entries.clear();
    }
}
