package com.hfstudio.guidenh.guide.scene.cache;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

public class GuideSceneStructureCache {

    private static final GuideSceneStructureCache INSTANCE = new GuideSceneStructureCache();
    public static final int DEFAULT_MAX_ENTRY_COUNT = 32;
    public static final long DEFAULT_MAX_TOTAL_BYTES = 48L * 1024L * 1024L;

    private final Map<GuideSceneStructureCacheKey, byte[]> cache = new LinkedHashMap<>(16, 0.75f, true);
    private long totalBytes;

    public static GuideSceneStructureCache global() {
        return INSTANCE;
    }

    @Nullable
    public synchronized GuideSceneStructureCacheEntry restore(GuideSceneStructureCacheKey key) {
        byte[] payload = cache.get(key);
        return payload != null ? deserialize(payload) : null;
    }

    public synchronized void put(GuideSceneStructureCacheKey key, GuideSceneStructureCacheEntry entry) {
        putPayload(key, serialize(entry));
    }

    public synchronized void clear() {
        cache.clear();
        totalBytes = 0L;
    }

    public synchronized GuideSceneStructureCacheStats snapshotStats() {
        return new GuideSceneStructureCacheStats(
            cache.size(),
            totalBytes,
            DEFAULT_MAX_ENTRY_COUNT,
            DEFAULT_MAX_TOTAL_BYTES);
    }

    private byte[] serialize(GuideSceneStructureCacheEntry entry) {
        try {
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            try (ObjectOutputStream output = new ObjectOutputStream(buffer)) {
                output.writeObject(entry);
            }
            return buffer.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to serialize GameScene structure cache entry", e);
        }
    }

    private GuideSceneStructureCacheEntry deserialize(byte[] payload) {
        try (ObjectInputStream input = new ObjectInputStream(new ByteArrayInputStream(payload))) {
            return (GuideSceneStructureCacheEntry) input.readObject();
        } catch (IOException | ClassNotFoundException e) {
            throw new IllegalStateException("Failed to deserialize GameScene structure cache entry", e);
        }
    }

    private void putPayload(GuideSceneStructureCacheKey key, byte[] payload) {
        byte[] previous = cache.put(key, payload);
        totalBytes += payload.length;
        if (previous != null) {
            totalBytes -= previous.length;
        }
        evictOverflowEntries();
    }

    private void evictOverflowEntries() {
        Iterator<Map.Entry<GuideSceneStructureCacheKey, byte[]>> iterator = cache.entrySet()
            .iterator();
        while ((cache.size() > DEFAULT_MAX_ENTRY_COUNT || totalBytes > DEFAULT_MAX_TOTAL_BYTES) && iterator.hasNext()) {
            Map.Entry<GuideSceneStructureCacheKey, byte[]> entry = iterator.next();
            totalBytes -= entry.getValue().length;
            iterator.remove();
        }
    }

    public static class GuideSceneStructureCacheStats {

        private final int entryCount;
        private final long totalBytes;
        private final int maxEntryCount;
        private final long maxTotalBytes;

        public GuideSceneStructureCacheStats(int entryCount, long totalBytes, int maxEntryCount, long maxTotalBytes) {
            this.entryCount = entryCount;
            this.totalBytes = totalBytes;
            this.maxEntryCount = maxEntryCount;
            this.maxTotalBytes = maxTotalBytes;
        }

        public int getEntryCount() {
            return entryCount;
        }

        public long getTotalBytes() {
            return totalBytes;
        }

        public int getMaxEntryCount() {
            return maxEntryCount;
        }

        public long getMaxTotalBytes() {
            return maxTotalBytes;
        }
    }
}
