package com.hfstudio.guidenh.guide.scene.cache;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.guide.internal.structure.GuideTextNbtCodec;
import com.hfstudio.guidenh.guide.scene.LytGuidebookScene;
import com.hfstudio.guidenh.guide.scene.StructureLibSceneBinding;
import com.hfstudio.guidenh.guide.scene.level.GuidebookLevel;
import com.hfstudio.guidenh.integration.structurelib.StructureLibHatchDescriptionLine;
import com.hfstudio.guidenh.integration.structurelib.StructureLibSceneMetadata;

public class GuideSceneStructureCacheEntry implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;
    private static final int MAX_DECODED_ITEM_STACK_CACHE_SIZE = 512;
    private static final Map<String, ItemStack> DECODED_ITEM_STACK_CACHE = Collections
        .synchronizedMap(new LinkedHashMap<>(256, 0.75f, true) {

            @Override
            protected boolean removeEldestEntry(Map.Entry<String, ItemStack> eldest) {
                return size() > MAX_DECODED_ITEM_STACK_CACHE_SIZE;
            }
        });

    private final GuideSceneStructureSnapshot levelSnapshot;
    private final List<StructureLibBindingEntry> structureLibBindings;

    public GuideSceneStructureCacheEntry(GuideSceneStructureSnapshot levelSnapshot,
        List<StructureLibBindingEntry> structureLibBindings) {
        this.levelSnapshot = levelSnapshot;
        this.structureLibBindings = structureLibBindings != null ? new ArrayList<>(structureLibBindings)
            : new ArrayList<>();
    }

    public static GuideSceneStructureCacheEntry capture(LytGuidebookScene scene) {
        GuidebookLevel level = scene.getLevel();
        List<StructureLibBindingEntry> bindings = new ArrayList<>();
        for (StructureLibSceneBinding binding : scene.getStructureLibBindings()) {
            StructureLibSceneMetadata metadata = binding.getMetadata();
            if (metadata != null) {
                bindings.add(StructureLibBindingEntry.capture(binding, metadata, level));
            }
        }
        return new GuideSceneStructureCacheEntry(GuideSceneStructureSnapshot.capture(level), bindings);
    }

    public GuidebookLevel restoreLevel() {
        return levelSnapshot.restoreLevel();
    }

    public void restoreInto(LytGuidebookScene scene) {
        scene.setLevel(restoreLevel());
        scene.setStructureLibSceneMetadata(null);
        for (StructureLibBindingEntry entry : structureLibBindings) {
            entry.restore(scene);
        }
    }

    public static class StructureLibBindingEntry implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        @Nullable
        private final String name;
        private final StructureLibMetadataEntry metadata;

        public StructureLibBindingEntry(@Nullable String name, StructureLibMetadataEntry metadata) {
            this.name = name;
            this.metadata = metadata;
        }

        public static StructureLibBindingEntry capture(StructureLibSceneBinding binding,
            StructureLibSceneMetadata metadata, GuidebookLevel level) {
            return new StructureLibBindingEntry(binding.getName(), StructureLibMetadataEntry.capture(metadata, level));
        }

        public void restore(LytGuidebookScene scene) {
            scene.setStructureLibSceneMetadata(name, metadata.restore());
        }
    }

    public static class StructureLibMetadataEntry implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        private final String controller;
        @Nullable
        private final String piece;
        @Nullable
        private final String facing;
        @Nullable
        private final String rotation;
        @Nullable
        private final String flip;
        @Nullable
        private final TierDataEntry tierData;
        private final List<ChannelDataEntry> channels;
        private final Map<Long, BlockTooltipDataEntry> tooltipsByPos;

        public StructureLibMetadataEntry(String controller, @Nullable String piece, @Nullable String facing,
            @Nullable String rotation, @Nullable String flip, @Nullable TierDataEntry tierData,
            List<ChannelDataEntry> channels, Map<Long, BlockTooltipDataEntry> tooltipsByPos) {
            this.controller = controller;
            this.piece = piece;
            this.facing = facing;
            this.rotation = rotation;
            this.flip = flip;
            this.tierData = tierData;
            this.channels = channels != null ? new ArrayList<>(channels) : new ArrayList<>();
            this.tooltipsByPos = tooltipsByPos != null ? new LinkedHashMap<>(tooltipsByPos) : new LinkedHashMap<>();
        }

        public static StructureLibMetadataEntry capture(StructureLibSceneMetadata metadata, GuidebookLevel level) {
            List<ChannelDataEntry> channels = new ArrayList<>();
            for (StructureLibSceneMetadata.ChannelData channelData : metadata.getChannelDataList()) {
                channels.add(ChannelDataEntry.capture(channelData));
            }
            LinkedHashMap<Long, BlockTooltipDataEntry> tooltips = new LinkedHashMap<>();
            for (int[] pos : level.getFilledBlocks()) {
                if (pos == null || pos.length < 3) {
                    continue;
                }
                StructureLibSceneMetadata.BlockTooltipData tooltipData = metadata
                    .getBlockTooltipData(pos[0], pos[1], pos[2]);
                if (tooltipData != null && tooltipData.hasAdditionalTooltipContent()) {
                    tooltips.put(
                        StructureLibSceneMetadata.packBlockPos(pos[0], pos[1], pos[2]),
                        BlockTooltipDataEntry.capture(tooltipData));
                }
            }
            for (StructureLibSceneMetadata.BlockTooltipEntry entry : metadata.getHatchTooltipEntries()) {
                if (entry == null) {
                    continue;
                }
                long packedPos = StructureLibSceneMetadata.packBlockPos(entry.getX(), entry.getY(), entry.getZ());
                if (tooltips.containsKey(packedPos)) {
                    continue;
                }
                StructureLibSceneMetadata.BlockTooltipData tooltipData = metadata
                    .getBlockTooltipData(entry.getX(), entry.getY(), entry.getZ());
                if (tooltipData != null && tooltipData.hasAdditionalTooltipContent()) {
                    tooltips.put(packedPos, BlockTooltipDataEntry.capture(tooltipData));
                }
            }
            return new StructureLibMetadataEntry(
                metadata.getController(),
                metadata.getPiece(),
                metadata.getFacing(),
                metadata.getRotation(),
                metadata.getFlip(),
                metadata.getTierData() != null ? TierDataEntry.capture(metadata.getTierData()) : null,
                channels,
                tooltips);
        }

        public StructureLibSceneMetadata restore() {
            StructureLibSceneMetadata metadata = new StructureLibSceneMetadata(
                controller,
                piece,
                facing,
                rotation,
                flip);
            if (tierData != null) {
                metadata = metadata
                    .withTierData(tierData.minValue, tierData.maxValue, tierData.defaultValue, tierData.currentValue);
            }
            for (ChannelDataEntry channel : channels) {
                metadata = metadata
                    .withChannelData(channel.channelId, channel.label, channel.maxValue, channel.currentValue);
            }
            if (!tooltipsByPos.isEmpty()) {
                LinkedHashMap<Long, StructureLibSceneMetadata.BlockTooltipData> tooltips = new LinkedHashMap<>();
                for (Map.Entry<Long, BlockTooltipDataEntry> entry : tooltipsByPos.entrySet()) {
                    tooltips.put(
                        entry.getKey(),
                        entry.getValue()
                            .restore());
                }
                metadata = metadata.withBlockTooltips(tooltips);
            }
            return metadata;
        }
    }

    public static class TierDataEntry implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        private final int minValue;
        private final int maxValue;
        private final int defaultValue;
        private final int currentValue;

        public TierDataEntry(int minValue, int maxValue, int defaultValue, int currentValue) {
            this.minValue = minValue;
            this.maxValue = maxValue;
            this.defaultValue = defaultValue;
            this.currentValue = currentValue;
        }

        public static TierDataEntry capture(StructureLibSceneMetadata.TierData tierData) {
            return new TierDataEntry(
                tierData.getMinValue(),
                tierData.getMaxValue(),
                tierData.getDefaultValue(),
                tierData.getCurrentValue());
        }
    }

    public static class ChannelDataEntry implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        private final String channelId;
        private final String label;
        private final int maxValue;
        private final int currentValue;

        public ChannelDataEntry(String channelId, String label, int maxValue, int currentValue) {
            this.channelId = channelId;
            this.label = label;
            this.maxValue = maxValue;
            this.currentValue = currentValue;
        }

        public static ChannelDataEntry capture(StructureLibSceneMetadata.ChannelData channelData) {
            return new ChannelDataEntry(
                channelData.getChannelId(),
                channelData.getLabel(),
                channelData.getMaxValue(),
                channelData.getCurrentValue());
        }
    }

    public static class BlockTooltipDataEntry implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        @Nullable
        private final String structureLibDescription;
        private final List<String> blockCandidates;
        private final List<HatchDescriptionLineEntry> hatchDescriptionLines;
        private final List<String> hatchCandidates;

        public BlockTooltipDataEntry(@Nullable String structureLibDescription, List<String> blockCandidates,
            List<HatchDescriptionLineEntry> hatchDescriptionLines, List<String> hatchCandidates) {
            this.structureLibDescription = structureLibDescription;
            this.blockCandidates = blockCandidates != null ? new ArrayList<>(blockCandidates) : List.of();
            this.hatchDescriptionLines = hatchDescriptionLines != null ? new ArrayList<>(hatchDescriptionLines)
                : List.of();
            this.hatchCandidates = hatchCandidates != null ? new ArrayList<>(hatchCandidates) : List.of();
        }

        public static BlockTooltipDataEntry capture(StructureLibSceneMetadata.BlockTooltipData data) {
            List<String> blockCandidates = new ArrayList<>();
            for (ItemStack stack : data.getBlockCandidates()) {
                String encoded = encodeItemStack(stack);
                if (encoded != null) {
                    blockCandidates.add(encoded);
                }
            }
            List<HatchDescriptionLineEntry> lines = new ArrayList<>();
            for (StructureLibHatchDescriptionLine line : data.getHatchDescriptionLines()) {
                lines.add(HatchDescriptionLineEntry.capture(line));
            }
            List<String> hatchCandidates = new ArrayList<>();
            for (ItemStack stack : data.getHatchCandidates()) {
                String encoded = encodeItemStack(stack);
                if (encoded != null) {
                    hatchCandidates.add(encoded);
                }
            }
            return new BlockTooltipDataEntry(
                data.getStructureLibDescription(),
                blockCandidates,
                lines,
                hatchCandidates);
        }

        public StructureLibSceneMetadata.BlockTooltipData restore() {
            List<ItemStack> blockItems = new ArrayList<>();
            for (String encoded : blockCandidates) {
                ItemStack stack = decodeItemStack(encoded);
                if (stack != null) {
                    blockItems.add(stack);
                }
            }
            List<StructureLibHatchDescriptionLine> lines = new ArrayList<>();
            for (HatchDescriptionLineEntry line : hatchDescriptionLines) {
                lines.add(line.restore());
            }
            List<ItemStack> hatchItems = new ArrayList<>();
            for (String encoded : hatchCandidates) {
                ItemStack stack = decodeItemStack(encoded);
                if (stack != null) {
                    hatchItems.add(stack);
                }
            }
            return new StructureLibSceneMetadata.BlockTooltipData(
                structureLibDescription,
                blockItems,
                lines,
                hatchItems);
        }
    }

    public static class HatchDescriptionLineEntry implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        private final String kindId;
        private final int hintDot;
        @Nullable
        private final String text;

        public HatchDescriptionLineEntry(String kindId, int hintDot, @Nullable String text) {
            this.kindId = kindId;
            this.hintDot = hintDot;
            this.text = text;
        }

        public static HatchDescriptionLineEntry capture(StructureLibHatchDescriptionLine line) {
            return new HatchDescriptionLineEntry(
                line.getKind()
                    .id(),
                line.getHintDot(),
                line.getText());
        }

        public StructureLibHatchDescriptionLine restore() {
            return StructureLibHatchDescriptionLine
                .of(StructureLibHatchDescriptionLine.registerKind(kindId), hintDot, text);
        }
    }

    @Nullable
    private static String encodeItemStack(@Nullable ItemStack stack) {
        if (stack == null) {
            return null;
        }
        NBTTagCompound tag = new NBTTagCompound();
        stack.writeToNBT(tag);
        return GuideTextNbtCodec.writeTextSafeCompound(tag);
    }

    @Nullable
    private static ItemStack decodeItemStack(@Nullable String encoded) {
        if (encoded == null || encoded.isEmpty()) {
            return null;
        }
        ItemStack cached = DECODED_ITEM_STACK_CACHE.get(encoded);
        if (cached != null) {
            return cached.copy();
        }
        try {
            ItemStack stack = ItemStack.loadItemStackFromNBT(GuideTextNbtCodec.readTextSafeCompound(encoded));
            if (stack != null) {
                DECODED_ITEM_STACK_CACHE.put(encoded, stack.copy());
            }
            return stack;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to decode cached StructureLib item stack", e);
        }
    }
}
