package com.hfstudio.guidenh.guide.scene.structurelib;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jetbrains.annotations.Nullable;

import net.minecraft.item.ItemStack;

public class StructureLibSceneMetadata {

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
    private final TierData tierData;
    private final List<ChannelData> channelDataList;
    private final Map<String, ChannelData> channelDataById;
    private final Map<Long, BlockTooltipData> blockTooltipDataByPos;
    private final List<BlockTooltipEntry> hatchTooltipEntries;
    private final Set<Long> hatchTooltipPositions;
    private final boolean hasHatchTooltipData;

    public StructureLibSceneMetadata(String controller, @Nullable String piece, @Nullable String facing,
        @Nullable String rotation, @Nullable String flip) {
        this(controller, piece, facing, rotation, flip, null, Collections.emptyList(), Collections.emptyMap());
    }

    private StructureLibSceneMetadata(String controller, @Nullable String piece, @Nullable String facing,
        @Nullable String rotation, @Nullable String flip, @Nullable TierData tierData,
        List<ChannelData> channelDataList, Map<Long, BlockTooltipData> blockTooltipDataByPos) {
        this.controller = requireController(controller);
        this.piece = normalizeOptional(piece);
        this.facing = normalizeOptional(facing);
        this.rotation = normalizeOptional(rotation);
        this.flip = normalizeOptional(flip);
        this.tierData = tierData;
        this.channelDataList = immutableChannels(channelDataList);
        this.channelDataById = indexChannels(this.channelDataList);
        this.blockTooltipDataByPos = immutableCopy(blockTooltipDataByPos);
        this.hatchTooltipEntries = computeHatchTooltipEntries(this.blockTooltipDataByPos);
        this.hatchTooltipPositions = computeHatchTooltipPositions(this.hatchTooltipEntries);
        this.hasHatchTooltipData = !this.hatchTooltipEntries.isEmpty();
    }

    public String getController() {
        return controller;
    }

    public StructureLibSceneMetadata withBlockTooltip(int x, int y, int z, @Nullable BlockTooltipData tooltipData) {
        Map<Long, BlockTooltipData> updated = new LinkedHashMap<>(blockTooltipDataByPos);
        long key = packBlockPos(x, y, z);
        if (tooltipData == null || !tooltipData.hasAdditionalTooltipContent()) {
            updated.remove(key);
        } else {
            updated.put(key, tooltipData);
        }
        return new StructureLibSceneMetadata(
            controller,
            piece,
            facing,
            rotation,
            flip,
            tierData,
            channelDataList,
            updated);
    }

    public StructureLibSceneMetadata withTierData(int minValue, int maxValue, int defaultValue, int currentValue) {
        return new StructureLibSceneMetadata(
            controller,
            piece,
            facing,
            rotation,
            flip,
            new TierData(minValue, maxValue, defaultValue, currentValue),
            channelDataList,
            blockTooltipDataByPos);
    }

    public StructureLibSceneMetadata withChannelData(String channelId, String label, int maxValue, int currentValue) {
        LinkedHashMap<String, ChannelData> updated = new LinkedHashMap<>(channelDataById);
        ChannelData next = new ChannelData(channelId, label, maxValue, 0, currentValue);
        updated.put(next.getChannelId(), next);
        return new StructureLibSceneMetadata(
            controller,
            piece,
            facing,
            rotation,
            flip,
            tierData,
            new ArrayList<>(updated.values()),
            blockTooltipDataByPos);
    }

    @Nullable
    public BlockTooltipData getBlockTooltipData(int x, int y, int z) {
        return blockTooltipDataByPos.get(packBlockPos(x, y, z));
    }

    public boolean hasHatchTooltipData() {
        return hasHatchTooltipData;
    }

    public List<BlockTooltipEntry> getHatchTooltipEntries() {
        return hatchTooltipEntries;
    }

    public Set<Long> getHatchTooltipPositions() {
        return hatchTooltipPositions;
    }

    @Nullable
    public TierData getTierData() {
        return tierData;
    }

    public List<ChannelData> getChannelDataList() {
        return channelDataList;
    }

    @Nullable
    public ChannelData getChannelData(String channelId) {
        String normalized = StructureLibPreviewSelection.normalizeChannelId(channelId);
        return normalized != null ? channelDataById.get(normalized) : null;
    }

    public boolean hasSelectableChannels() {
        for (ChannelData channelData : channelDataList) {
            if (channelData.isSelectable()) {
                return true;
            }
        }
        return false;
    }

    @Nullable
    public String getPiece() {
        return piece;
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

    public static String requireController(@Nullable String controller) {
        if (controller == null) {
            throw new IllegalArgumentException("StructureLib metadata controller cannot be null");
        }
        String trimmed = controller.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("StructureLib metadata controller cannot be empty");
        }
        return trimmed;
    }

    @Nullable
    public static String normalizeOptional(@Nullable String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    public static List<ChannelData> immutableChannels(@Nullable List<ChannelData> source) {
        if (source == null || source.isEmpty()) {
            return Collections.emptyList();
        }
        LinkedHashMap<String, ChannelData> deduplicated = new LinkedHashMap<>(source.size());
        for (ChannelData channelData : source) {
            if (channelData != null) {
                deduplicated.put(channelData.getChannelId(), channelData);
            }
        }
        return deduplicated.isEmpty() ? Collections.emptyList()
            : Collections.unmodifiableList(new ArrayList<>(deduplicated.values()));
    }

    public static Map<String, ChannelData> indexChannels(List<ChannelData> channels) {
        if (channels.isEmpty()) {
            return Collections.emptyMap();
        }
        LinkedHashMap<String, ChannelData> indexed = new LinkedHashMap<>(channels.size());
        for (ChannelData channelData : channels) {
            indexed.put(channelData.getChannelId(), channelData);
        }
        return Collections.unmodifiableMap(indexed);
    }

    public static Map<Long, BlockTooltipData> immutableCopy(@Nullable Map<Long, BlockTooltipData> source) {
        if (source == null || source.isEmpty()) {
            return Collections.emptyMap();
        }
        return Collections.unmodifiableMap(new LinkedHashMap<>(source));
    }

    public static List<BlockTooltipEntry> computeHatchTooltipEntries(
        Map<Long, BlockTooltipData> blockTooltipDataByPos) {
        if (blockTooltipDataByPos.isEmpty()) {
            return Collections.emptyList();
        }
        List<BlockTooltipEntry> entries = new ArrayList<>();
        for (Map.Entry<Long, BlockTooltipData> entry : blockTooltipDataByPos.entrySet()) {
            BlockTooltipData value = entry.getValue();
            if (value != null && value.hasHatchDetails()) {
                entries.add(
                    new BlockTooltipEntry(
                        unpackBlockPosX(entry.getKey()),
                        unpackBlockPosY(entry.getKey()),
                        unpackBlockPosZ(entry.getKey()),
                        value));
            }
        }
        return entries.isEmpty() ? Collections.emptyList() : Collections.unmodifiableList(entries);
    }

    public static long packBlockPos(int x, int y, int z) {
        return (((long) x & 0x3FFFFFFL) << 38) | (((long) z & 0x3FFFFFFL) << 12) | ((long) y & 0xFFFL);
    }

    public static Set<Long> computeHatchTooltipPositions(List<BlockTooltipEntry> hatchTooltipEntries) {
        if (hatchTooltipEntries.isEmpty()) {
            return Collections.emptySet();
        }
        Set<Long> positions = new LinkedHashSet<>(hatchTooltipEntries.size());
        for (BlockTooltipEntry entry : hatchTooltipEntries) {
            positions.add(packBlockPos(entry.getX(), entry.getY(), entry.getZ()));
        }
        return Collections.unmodifiableSet(positions);
    }

    public static int unpackBlockPosX(long packedPos) {
        return (int) (packedPos >> 38);
    }

    public static int unpackBlockPosY(long packedPos) {
        return (int) (packedPos << 52 >> 52);
    }

    public static int unpackBlockPosZ(long packedPos) {
        return (int) (packedPos << 26 >> 38);
    }

    public static class BlockTooltipEntry {

        private final int x;
        private final int y;
        private final int z;
        private final BlockTooltipData tooltipData;

        private BlockTooltipEntry(int x, int y, int z, BlockTooltipData tooltipData) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.tooltipData = tooltipData;
        }

        public int getX() {
            return x;
        }

        public int getY() {
            return y;
        }

        public int getZ() {
            return z;
        }

        public BlockTooltipData getTooltipData() {
            return tooltipData;
        }
    }

    public static class BlockTooltipData {

        @Nullable
        private final String structureLibDescription;
        private final List<ItemStack> blockCandidates;
        private final List<StructureLibHatchDescriptionLine> hatchDescriptionLines;
        private final List<ItemStack> hatchCandidates;

        public BlockTooltipData(@Nullable String structureLibDescription, List<ItemStack> blockCandidates,
            List<StructureLibHatchDescriptionLine> hatchDescriptionLines, List<ItemStack> hatchCandidates) {
            this.structureLibDescription = normalizeOptional(structureLibDescription);
            this.blockCandidates = immutableStacks(blockCandidates);
            this.hatchDescriptionLines = immutableLines(hatchDescriptionLines);
            this.hatchCandidates = immutableStacks(hatchCandidates);
        }

        @Nullable
        public String getStructureLibDescription() {
            return structureLibDescription;
        }

        public List<ItemStack> getBlockCandidates() {
            return blockCandidates;
        }

        public List<StructureLibHatchDescriptionLine> getHatchDescriptionLines() {
            return hatchDescriptionLines;
        }

        public List<ItemStack> getHatchCandidates() {
            return hatchCandidates;
        }

        public boolean hasAdditionalTooltipContent() {
            return structureLibDescription != null || !blockCandidates.isEmpty()
                || !hatchDescriptionLines.isEmpty()
                || !hatchCandidates.isEmpty();
        }

        public boolean hasHatchDetails() {
            return !hatchDescriptionLines.isEmpty() || !hatchCandidates.isEmpty();
        }

        public static List<ItemStack> immutableStacks(@Nullable List<ItemStack> stacks) {
            if (stacks == null || stacks.isEmpty()) {
                return Collections.emptyList();
            }
            List<ItemStack> copied = new ArrayList<>(stacks.size());
            for (ItemStack stack : stacks) {
                if (stack != null && stack.stackSize > 0) {
                    copied.add(stack.copy());
                }
            }
            return copied.isEmpty() ? Collections.emptyList() : Collections.unmodifiableList(copied);
        }

        public static List<StructureLibHatchDescriptionLine> immutableLines(
            @Nullable List<StructureLibHatchDescriptionLine> lines) {
            if (lines == null || lines.isEmpty()) {
                return Collections.emptyList();
            }
            List<StructureLibHatchDescriptionLine> copied = new ArrayList<>(lines.size());
            for (StructureLibHatchDescriptionLine line : lines) {
                if (line != null) {
                    copied.add(line);
                }
            }
            return copied.isEmpty() ? Collections.emptyList() : Collections.unmodifiableList(copied);
        }
    }

    public static class TierData {

        private final int minValue;
        private final int maxValue;
        private final int defaultValue;
        private final int currentValue;

        private TierData(int minValue, int maxValue, int defaultValue, int currentValue) {
            int normalizedMin = Math.max(1, minValue);
            int normalizedMax = Math.max(normalizedMin, maxValue);
            this.minValue = normalizedMin;
            this.maxValue = normalizedMax;
            this.defaultValue = clamp(defaultValue, normalizedMin, normalizedMax);
            this.currentValue = clamp(currentValue, normalizedMin, normalizedMax);
        }

        public int getMinValue() {
            return minValue;
        }

        public int getMaxValue() {
            return maxValue;
        }

        public int getDefaultValue() {
            return defaultValue;
        }

        public int getCurrentValue() {
            return currentValue;
        }

        public boolean isSelectable() {
            return maxValue > minValue;
        }
    }

    public static class ChannelData {

        private final String channelId;
        private final String label;
        private final int maxValue;
        private final int defaultValue;
        private final int currentValue;

        private ChannelData(String channelId, String label, int maxValue, int defaultValue, int currentValue) {
            String normalizedChannelId = StructureLibPreviewSelection.normalizeChannelId(channelId);
            String normalizedLabel = normalizeOptional(label);
            int normalizedMax = Math.max(0, maxValue);
            this.channelId = normalizedChannelId != null ? normalizedChannelId : "channel";
            this.label = normalizedLabel != null ? normalizedLabel : this.channelId;
            this.maxValue = normalizedMax;
            this.defaultValue = clamp(defaultValue, 0, normalizedMax);
            this.currentValue = clamp(currentValue, 0, normalizedMax);
        }

        public String getChannelId() {
            return channelId;
        }

        public String getLabel() {
            return label;
        }

        public int getMinValue() {
            return 0;
        }

        public int getMaxValue() {
            return maxValue;
        }

        public int getDefaultValue() {
            return defaultValue;
        }

        public int getCurrentValue() {
            return currentValue;
        }

        public boolean isSelectable() {
            return maxValue > 0;
        }
    }

    public static int clamp(int value, int minValue, int maxValue) {
        if (value < minValue) {
            return minValue;
        }
        return Math.min(value, maxValue);
    }
}
