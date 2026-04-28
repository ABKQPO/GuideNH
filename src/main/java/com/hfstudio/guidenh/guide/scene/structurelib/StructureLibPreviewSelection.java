package com.hfstudio.guidenh.guide.scene.structurelib;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import javax.annotation.Nullable;

public class StructureLibPreviewSelection {

    public static final int DEFAULT_MASTER_TIER = 1;

    private final int masterTier;
    private final Map<String, Integer> channelOverrides;

    public StructureLibPreviewSelection() {
        this(DEFAULT_MASTER_TIER, Collections.emptyMap());
    }

    public StructureLibPreviewSelection(int masterTier, @Nullable Map<String, Integer> channelOverrides) {
        this.masterTier = Math.max(DEFAULT_MASTER_TIER, masterTier);
        this.channelOverrides = immutableChannelOverrides(channelOverrides);
    }

    public static StructureLibPreviewSelection defaultSelection() {
        return new StructureLibPreviewSelection();
    }

    public static StructureLibPreviewSelection ofMasterTier(int masterTier) {
        return new StructureLibPreviewSelection(masterTier, Collections.emptyMap());
    }

    public int getMasterTier() {
        return masterTier;
    }

    public Map<String, Integer> getChannelOverrides() {
        return channelOverrides;
    }

    public boolean hasChannelOverride(String channelId) {
        String normalized = normalizeChannelId(channelId);
        return normalized != null && channelOverrides.containsKey(normalized);
    }

    public int getChannelValue(String channelId) {
        String normalized = normalizeChannelId(channelId);
        if (normalized == null) {
            return 0;
        }
        Integer value = channelOverrides.get(normalized);
        return value != null ? value : 0;
    }

    public StructureLibPreviewSelection withMasterTier(int nextMasterTier) {
        return new StructureLibPreviewSelection(nextMasterTier, channelOverrides);
    }

    public StructureLibPreviewSelection withChannelOverride(String channelId, int value) {
        String normalized = normalizeChannelId(channelId);
        if (normalized == null) {
            return this;
        }
        LinkedHashMap<String, Integer> updated = new LinkedHashMap<>(channelOverrides);
        if (value > 0) {
            updated.put(normalized, value);
        } else {
            updated.remove(normalized);
        }
        return new StructureLibPreviewSelection(masterTier, updated);
    }

    public static Map<String, Integer> immutableChannelOverrides(@Nullable Map<String, Integer> source) {
        if (source == null || source.isEmpty()) {
            return Collections.emptyMap();
        }
        LinkedHashMap<String, Integer> normalized = new LinkedHashMap<>(source.size());
        for (Map.Entry<String, Integer> entry : source.entrySet()) {
            String channelId = normalizeChannelId(entry.getKey());
            Integer value = entry.getValue();
            if (channelId == null || value == null || value <= 0) {
                continue;
            }
            normalized.put(channelId, value);
        }
        return normalized.isEmpty() ? Collections.emptyMap() : Collections.unmodifiableMap(normalized);
    }

    @Nullable
    public static String normalizeChannelId(@Nullable String channelId) {
        if (channelId == null) {
            return null;
        }
        String trimmed = channelId.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        return trimmed.toLowerCase(Locale.ROOT);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof StructureLibPreviewSelection other)) {
            return false;
        }
        return masterTier == other.masterTier && channelOverrides.equals(other.channelOverrides);
    }

    @Override
    public int hashCode() {
        return Objects.hash(masterTier, channelOverrides);
    }
}
