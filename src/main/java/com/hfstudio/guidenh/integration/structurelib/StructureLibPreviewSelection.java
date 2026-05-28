package com.hfstudio.guidenh.integration.structurelib;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import org.jetbrains.annotations.Nullable;

public class StructureLibPreviewSelection {

    public static final int DEFAULT_MASTER_TIER = 1;
    public static final String SURVIVAL_CONSTRUCT_OPTION = "structurelib.survival_construct";
    public static final String SURVIVAL_FILL_EMPTY_HATCHES_OPTION = "structurelib.survival_fill_empty_hatches";
    public static final String FORCE_HATCH_PLACEMENT_OPTION = "structurelib.force_hatch_placement";

    private final int masterTier;
    private final Map<String, Integer> channelOverrides;
    private final Map<String, Boolean> integrationOptions;

    public StructureLibPreviewSelection() {
        this(DEFAULT_MASTER_TIER, Map.of(), Map.of());
    }

    public StructureLibPreviewSelection(int masterTier, @Nullable Map<String, Integer> channelOverrides) {
        this(masterTier, channelOverrides, Map.of());
    }

    public StructureLibPreviewSelection(int masterTier, @Nullable Map<String, Integer> channelOverrides,
        @Nullable Map<String, Boolean> integrationOptions) {
        this.masterTier = Math.max(DEFAULT_MASTER_TIER, masterTier);
        this.channelOverrides = immutableChannelOverrides(channelOverrides);
        this.integrationOptions = immutableIntegrationOptions(integrationOptions);
    }

    public static StructureLibPreviewSelection defaultSelection() {
        return new StructureLibPreviewSelection();
    }

    public static StructureLibPreviewSelection ofMasterTier(int masterTier) {
        return new StructureLibPreviewSelection(masterTier, Map.of());
    }

    public int getMasterTier() {
        return masterTier;
    }

    public Map<String, Integer> getChannelOverrides() {
        return channelOverrides;
    }

    public Map<String, Boolean> getIntegrationOptions() {
        return integrationOptions;
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
        return new StructureLibPreviewSelection(nextMasterTier, channelOverrides, integrationOptions);
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
        return new StructureLibPreviewSelection(masterTier, updated, integrationOptions);
    }

    public boolean isIntegrationOptionEnabled(String optionId) {
        String normalized = normalizeIntegrationOptionId(optionId);
        return normalized != null && Boolean.TRUE.equals(integrationOptions.get(normalized));
    }

    public StructureLibPreviewSelection withIntegrationOption(String optionId, boolean enabled) {
        String normalized = normalizeIntegrationOptionId(optionId);
        if (normalized == null) {
            return this;
        }
        LinkedHashMap<String, Boolean> updated = new LinkedHashMap<>(integrationOptions);
        if (enabled) {
            updated.put(normalized, Boolean.TRUE);
        } else {
            updated.remove(normalized);
        }
        return new StructureLibPreviewSelection(masterTier, channelOverrides, updated);
    }

    public static Map<String, Integer> immutableChannelOverrides(@Nullable Map<String, Integer> source) {
        if (source == null || source.isEmpty()) {
            return Map.of();
        }
        var normalized = new LinkedHashMap<String, Integer>(source.size());
        for (var entry : source.entrySet()) {
            String channelId = normalizeChannelId(entry.getKey());
            Integer value = entry.getValue();
            if (channelId == null || value == null || value <= 0) {
                continue;
            }
            normalized.put(channelId, value);
        }
        return normalized.isEmpty() ? Map.of() : Map.copyOf(normalized);
    }

    public static Map<String, Boolean> immutableIntegrationOptions(@Nullable Map<String, Boolean> source) {
        if (source == null || source.isEmpty()) {
            return Map.of();
        }
        var normalized = new LinkedHashMap<String, Boolean>(source.size());
        for (var entry : source.entrySet()) {
            String optionId = normalizeIntegrationOptionId(entry.getKey());
            if (optionId != null && Boolean.TRUE.equals(entry.getValue())) {
                normalized.put(optionId, Boolean.TRUE);
            }
        }
        return normalized.isEmpty() ? Map.of() : Map.copyOf(normalized);
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

    @Nullable
    public static String normalizeIntegrationOptionId(@Nullable String optionId) {
        if (optionId == null) {
            return null;
        }
        String trimmed = optionId.trim();
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
        return masterTier == other.masterTier && channelOverrides.equals(other.channelOverrides)
            && integrationOptions.equals(other.integrationOptions);
    }

    @Override
    public int hashCode() {
        return Objects.hash(masterTier, channelOverrides, integrationOptions);
    }
}
