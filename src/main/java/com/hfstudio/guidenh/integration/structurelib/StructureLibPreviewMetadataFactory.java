package com.hfstudio.guidenh.integration.structurelib;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.Nullable;

import com.gtnewhorizon.structurelib.structure.IStructureElement;
import com.gtnewhorizon.structurelib.structure.IStructureElementChain;

public class StructureLibPreviewMetadataFactory {

    public static final String GENERIC_STRUCTURELIB_DESCRIPTION = "StructureLib";
    private static final Map<Class<?>, List<Field>> CHANNEL_FIELDS_CACHE = new ConcurrentHashMap<>();
    private static final StructureLibSceneMetadata.BlockTooltipData GENERIC_TOOLTIP_DATA = new StructureLibSceneMetadata.BlockTooltipData(
        GENERIC_STRUCTURELIB_DESCRIPTION,
        List.of(),
        List.of(),
        List.of());

    private final StructureLibElementTooltipResolver tooltipResolver;

    public StructureLibPreviewMetadataFactory(StructureLibElementTooltipResolver tooltipResolver) {
        this.tooltipResolver = tooltipResolver;
    }

    public StructureLibSceneMetadata createMetadata(StructureLibImportRequest request,
        StructureLibPreviewSelection selection, int maxTier, Map<String, Integer> channelMaxTierMap,
        List<AbsolutePreviewBlock> absoluteBlocks, Map<Long, IStructureElement<?>> visitedElementsByPos,
        ItemStack trigger, @Nullable World world, @Nullable String contextFingerprint) {
        return createMetadata(
            request,
            selection,
            maxTier,
            channelMaxTierMap,
            absoluteBlocks,
            visitedElementsByPos,
            trigger,
            world,
            contextFingerprint,
            null,
            null);
    }

    public StructureLibSceneMetadata createMetadata(StructureLibImportRequest request,
        StructureLibPreviewSelection selection, int maxTier, Map<String, Integer> channelMaxTierMap,
        List<AbsolutePreviewBlock> absoluteBlocks, Map<Long, IStructureElement<?>> visitedElementsByPos,
        ItemStack trigger, @Nullable World world, @Nullable String contextFingerprint, @Nullable Object constructable,
        @Nullable EntityPlayer actor) {
        StructureLibSceneMetadata metadata = new StructureLibSceneMetadata(
            request.getController(),
            request.getPiece(),
            request.getFacing(),
            request.getRotation(),
            request.getFlip());
        int resolvedMaxTier = Math.max(maxTier, resolveHintMaxTier(visitedElementsByPos, constructable));
        if (resolvedMaxTier > 0) {
            metadata = metadata.withTierData(1, resolvedMaxTier, selection.getMasterTier(), selection.getMasterTier());
        }
        if (channelMaxTierMap != null && !channelMaxTierMap.isEmpty()) {
            for (Map.Entry<String, Integer> entry : channelMaxTierMap.entrySet()) {
                String channelId = resolveChannelId(entry.getKey());
                if (channelId == null) {
                    continue;
                }
                metadata = metadata.withChannelData(
                    channelId,
                    channelId,
                    Math.max(0, entry.getValue()),
                    selection.getChannelValue(channelId));
            }
        }
        if (absoluteBlocks.isEmpty()) {
            return metadata;
        }

        int minX = Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;
        int minZ = Integer.MAX_VALUE;
        for (AbsolutePreviewBlock block : absoluteBlocks) {
            minX = Math.min(minX, block.getX());
            minY = Math.min(minY, block.getY());
            minZ = Math.min(minZ, block.getZ());
        }

        Map<Long, StructureLibSceneMetadata.BlockTooltipData> tooltipDataByPos = new LinkedHashMap<>(
            absoluteBlocks.size());
        Object tooltipConstructable = constructable != null ? constructable : new Object();
        for (AbsolutePreviewBlock block : absoluteBlocks) {
            IStructureElement<?> visitedElement = visitedElementsByPos != null
                ? visitedElementsByPos.get(pack(block.getX(), block.getY(), block.getZ()))
                : null;
            StructureLibElementTooltipResolver.TooltipDetails details = visitedElement != null
                ? tooltipResolver.resolve(
                    tooltipConstructable,
                    visitedElement,
                    world,
                    block.getX(),
                    block.getY(),
                    block.getZ(),
                    trigger,
                    actor,
                    contextFingerprint)
                : StructureLibElementTooltipResolver.TooltipDetails.empty();
            tooltipDataByPos.put(
                StructureLibSceneMetadata.packBlockPos(block.getX() - minX, block.getY() - minY, block.getZ() - minZ),
                createTooltipData(details));
        }
        return metadata.withBlockTooltips(tooltipDataByPos);
    }

    public static int resolveHintMaxTier(@Nullable Map<Long, IStructureElement<?>> visitedElementsByPos,
        @Nullable Object constructable) {
        if (visitedElementsByPos == null || visitedElementsByPos.isEmpty()) {
            return 0;
        }
        IdentityHashMap<IStructureElement<?>, Boolean> visited = new IdentityHashMap<>();
        int maxTier = 0;
        for (IStructureElement<?> element : visitedElementsByPos.values()) {
            maxTier = Math.max(maxTier, resolveHintMaxTier(element, constructable, visited));
        }
        return maxTier;
    }

    public static int resolveHintMaxTier(@Nullable IStructureElement<?> element, @Nullable Object constructable,
        IdentityHashMap<IStructureElement<?>, Boolean> visited) {
        if (element == null || visited.put(element, Boolean.TRUE) != null) {
            return 0;
        }
        int maxTier = resolveCapturedHintTierCount(element);
        if (element instanceof IStructureElementChain<?>chain) {
            for (IStructureElement<?> fallback : chain.fallbacks()) {
                maxTier = Math.max(maxTier, resolveHintMaxTier(fallback, constructable, visited));
            }
        }
        IStructureElement<?> lazyElement = StructureLibElementTooltipResolver.unwrapLazyElement(element, constructable);
        if (lazyElement != null) {
            maxTier = Math.max(maxTier, resolveHintMaxTier(lazyElement, constructable, visited));
        }
        for (IStructureElement<?> wrappedElement : StructureLibElementTooltipResolver.unwrapCapturedElements(element)) {
            maxTier = Math.max(maxTier, resolveHintMaxTier(wrappedElement, constructable, visited));
        }
        return maxTier;
    }

    public static int resolveCapturedHintTierCount(IStructureElement<?> element) {
        if (element == null) {
            return 0;
        }
        int maxTier = 0;
        for (Field field : element.getClass()
            .getDeclaredFields()) {
            if (!List.class.isAssignableFrom(field.getType())) {
                continue;
            }
            try {
                field.setAccessible(true);
                Object value = field.get(element);
                if (!(value instanceof List<?>list) || list.isEmpty() || !isTierHintList(list)) {
                    continue;
                }
                maxTier = Math.max(maxTier, list.size());
            } catch (IllegalAccessException ignored) {}
        }
        return maxTier;
    }

    public static boolean isTierHintList(List<?> candidates) {
        if (candidates == null || candidates.isEmpty()) {
            return false;
        }
        for (Object candidate : candidates) {
            if (!(candidate instanceof Pair<?, ?>pair)) {
                return false;
            }
            if (!(pair.getLeft() instanceof Block) || !(pair.getRight() instanceof Integer)) {
                return false;
            }
        }
        return true;
    }

    private static StructureLibSceneMetadata.BlockTooltipData createTooltipData(
        StructureLibElementTooltipResolver.TooltipDetails details) {
        if (details.getBlockCandidates()
            .isEmpty()
            && details.getHatchDescriptionLines()
                .isEmpty()
            && details.getHatchCandidates()
                .isEmpty()) {
            return GENERIC_TOOLTIP_DATA;
        }
        return new StructureLibSceneMetadata.BlockTooltipData(
            GENERIC_STRUCTURELIB_DESCRIPTION,
            details.getBlockCandidates(),
            details.getHatchDescriptionLines(),
            details.getHatchCandidates());
    }

    static String resolveFirstChannelLabel(Map<Long, IStructureElement<?>> visitedElementsByPos) {
        if (visitedElementsByPos == null || visitedElementsByPos.isEmpty()) {
            return "Channel";
        }
        for (IStructureElement<?> visitedElement : visitedElementsByPos.values()) {
            String label = resolveChannelId(visitedElement);
            if (label != null) {
                return label;
            }
        }
        return "Channel";
    }

    @Nullable
    static String resolveChannelId(@Nullable String channelId) {
        return StructureLibPreviewSelection.normalizeChannelId(channelId);
    }

    @Nullable
    static String resolveChannelId(@Nullable IStructureElement<?> element) {
        if (element == null) {
            return null;
        }
        for (Field field : channelFields(element.getClass())) {
            try {
                Object value = field.get(element);
                if (value instanceof String stringValue) {
                    String normalized = resolveChannelId(stringValue);
                    if (normalized != null) {
                        return normalized;
                    }
                }
            } catch (Throwable ignored) {}
        }
        return null;
    }

    private static List<Field> channelFields(Class<?> elementClass) {
        return CHANNEL_FIELDS_CACHE
            .computeIfAbsent(elementClass, StructureLibPreviewMetadataFactory::findChannelFields);
    }

    private static List<Field> findChannelFields(Class<?> elementClass) {
        List<Field> fields = new ArrayList<>();
        Class<?> current = elementClass;
        while (current != null && current != Object.class) {
            for (Field field : current.getDeclaredFields()) {
                if (field.getType() == String.class && field.getName()
                    .toLowerCase(Locale.ROOT)
                    .contains("channel")) {
                    field.setAccessible(true);
                    fields.add(field);
                }
            }
            current = current.getSuperclass();
        }
        return fields.isEmpty() ? List.of() : List.copyOf(fields);
    }

    public static long pack(int x, int y, int z) {
        return (((long) x & 0x3FFFFFFL) << 38) | (((long) z & 0x3FFFFFFL) << 12) | ((long) y & 0xFFFL);
    }

    public static class AbsolutePreviewBlock {

        private final int x;
        private final int y;
        private final int z;

        public AbsolutePreviewBlock(int x, int y, int z) {
            this.x = x;
            this.y = y;
            this.z = z;
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
    }

}
