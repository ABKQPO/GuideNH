package com.hfstudio.guidenh.integration.structurelib;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

public class StructureLibSceneOptions {

    public static final String GREGTECH_ACTIVE_CONTROLLER_OPTION = "gregtech.active_controller";
    public static final String GREGTECH_PLACE_HATCHES_OPTION = "gregtech.place_hatches";

    @Nullable
    private final String facing;
    @Nullable
    private final String rotation;
    @Nullable
    private final String flip;
    @Nullable
    private final Integer tier;
    private final Map<String, Integer> channelOverrides;
    private final boolean gregTechActiveController;
    private final boolean gregTechPlaceHatches;

    public StructureLibSceneOptions(@Nullable String facing, @Nullable String rotation, @Nullable String flip,
        @Nullable Integer tier, @Nullable Map<String, Integer> channelOverrides, boolean gregTechActiveController,
        boolean gregTechPlaceHatches) {
        this.facing = normalizeOptional(facing);
        this.rotation = normalizeOptional(rotation);
        this.flip = normalizeOptional(flip);
        this.tier = tier != null && tier > 0 ? tier : null;
        this.channelOverrides = StructureLibPreviewSelection.immutableChannelOverrides(channelOverrides);
        this.gregTechActiveController = gregTechActiveController;
        this.gregTechPlaceHatches = gregTechPlaceHatches;
    }

    public static StructureLibSceneOptions empty() {
        return builder().build();
    }

    public static Builder builder() {
        return new Builder();
    }

    @Nullable
    public String getFacing() {
        return facing;
    }

    @Nullable
    public String getRotation() {
        return rotation;
    }

    @Nullable
    public String getFlip() {
        return flip;
    }

    @Nullable
    public Integer getTier() {
        return tier;
    }

    public Map<String, Integer> getChannelOverrides() {
        return channelOverrides;
    }

    public boolean isGregTechActiveController() {
        return gregTechActiveController;
    }

    public boolean isGregTechPlaceHatches() {
        return gregTechPlaceHatches;
    }

    public boolean hasOverrides() {
        return facing != null || rotation != null
            || flip != null
            || tier != null
            || !channelOverrides.isEmpty()
            || gregTechActiveController
            || gregTechPlaceHatches;
    }

    public StructureLibPreviewSelection createSelection(@Nullable Integer legacyChannel) {
        int masterTier = tier != null ? tier
            : legacyChannel != null && legacyChannel > 0 ? legacyChannel
                : StructureLibPreviewSelection.DEFAULT_MASTER_TIER;
        StructureLibPreviewSelection selection = new StructureLibPreviewSelection(masterTier, channelOverrides);
        selection = selection.withIntegrationOption(GREGTECH_ACTIVE_CONTROLLER_OPTION, gregTechActiveController);
        selection = selection.withIntegrationOption(GREGTECH_PLACE_HATCHES_OPTION, gregTechPlaceHatches);
        return selection;
    }

    public StructureLibSceneOptions merge(StructureLibSceneOptions overrides) {
        if (overrides == null || !overrides.hasOverrides()) {
            return this;
        }
        LinkedHashMap<String, Integer> channels = new LinkedHashMap<>(channelOverrides);
        channels.putAll(overrides.channelOverrides);
        return new StructureLibSceneOptions(
            overrides.facing != null ? overrides.facing : facing,
            overrides.rotation != null ? overrides.rotation : rotation,
            overrides.flip != null ? overrides.flip : flip,
            overrides.tier != null ? overrides.tier : tier,
            channels,
            overrides.gregTechActiveController || gregTechActiveController,
            overrides.gregTechPlaceHatches || gregTechPlaceHatches);
    }

    public static String resolveFacing(@Nullable String attributeFacing, StructureLibSceneOptions options) {
        String normalized = normalizeOptional(attributeFacing);
        return normalized != null ? normalized : options != null ? options.getFacing() : null;
    }

    public static String resolveRotation(@Nullable String attributeRotation, StructureLibSceneOptions options) {
        String normalized = normalizeOptional(attributeRotation);
        return normalized != null ? normalized : options != null ? options.getRotation() : null;
    }

    public static String resolveFlip(@Nullable String attributeFlip, StructureLibSceneOptions options) {
        String normalized = normalizeOptional(attributeFlip);
        return normalized != null ? normalized : options != null ? options.getFlip() : null;
    }

    @Nullable
    public static String normalizeOptional(@Nullable String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    public static class Builder {

        @Nullable
        private String facing;
        @Nullable
        private String rotation;
        @Nullable
        private String flip;
        @Nullable
        private Integer tier;
        private final Map<String, Integer> channels = new LinkedHashMap<>();
        private boolean gregTechActiveController;
        private boolean gregTechPlaceHatches;

        public Builder facing(@Nullable String facing) {
            this.facing = normalizeOptional(facing);
            return this;
        }

        public Builder rotation(@Nullable String rotation) {
            this.rotation = normalizeOptional(rotation);
            return this;
        }

        public Builder flip(@Nullable String flip) {
            this.flip = normalizeOptional(flip);
            return this;
        }

        public Builder tier(@Nullable Integer tier) {
            this.tier = tier != null && tier > 0 ? tier : null;
            return this;
        }

        public Builder channel(String name, int value) {
            String normalized = StructureLibPreviewSelection.normalizeChannelId(name);
            if (normalized != null && value > 0) {
                channels.put(normalized, value);
            }
            return this;
        }

        public Builder channels(@Nullable Map<String, Integer> channels) {
            if (channels != null) {
                for (Map.Entry<String, Integer> entry : channels.entrySet()) {
                    Integer value = entry.getValue();
                    if (value != null) {
                        channel(entry.getKey(), value);
                    }
                }
            }
            return this;
        }

        public Builder gregTechActiveController(boolean gregTechActiveController) {
            this.gregTechActiveController = gregTechActiveController;
            return this;
        }

        public Builder gregTechPlaceHatches(boolean gregTechPlaceHatches) {
            this.gregTechPlaceHatches = gregTechPlaceHatches;
            return this;
        }

        public StructureLibSceneOptions build() {
            return new StructureLibSceneOptions(
                facing,
                rotation,
                flip,
                tier,
                channels.isEmpty() ? Collections.emptyMap() : channels,
                gregTechActiveController,
                gregTechPlaceHatches);
        }
    }
}
