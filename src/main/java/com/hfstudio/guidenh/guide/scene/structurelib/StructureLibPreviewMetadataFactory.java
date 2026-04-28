package com.hfstudio.guidenh.guide.scene.structurelib;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

import com.gtnewhorizon.structurelib.structure.IStructureElement;

public class StructureLibPreviewMetadataFactory {

    public static final String GENERIC_STRUCTURELIB_DESCRIPTION = "StructureLib";

    private final StructureLibElementTooltipResolver tooltipResolver;

    public StructureLibPreviewMetadataFactory(StructureLibElementTooltipResolver tooltipResolver) {
        this.tooltipResolver = tooltipResolver;
    }

    public StructureLibSceneMetadata createMetadata(StructureLibImportRequest request,
        StructureLibPreviewSelection selection, int maxTier, Map<String, Integer> channelMaxTierMap,
        List<AbsolutePreviewBlock> absoluteBlocks, List<VisitedStructureElement> visitedElements, ItemStack trigger,
        @Nullable World world) {
        return createMetadata(
            request,
            selection,
            maxTier,
            channelMaxTierMap,
            absoluteBlocks,
            visitedElements,
            trigger,
            world,
            null,
            null);
    }

    public StructureLibSceneMetadata createMetadata(StructureLibImportRequest request,
        StructureLibPreviewSelection selection, int maxTier, Map<String, Integer> channelMaxTierMap,
        List<AbsolutePreviewBlock> absoluteBlocks, List<VisitedStructureElement> visitedElements, ItemStack trigger,
        @Nullable World world, @Nullable Object constructable, @Nullable EntityPlayer actor) {
        StructureLibSceneMetadata metadata = new StructureLibSceneMetadata(
            request.getController(),
            request.getPiece(),
            request.getFacing(),
            request.getRotation(),
            request.getFlip());
        if (maxTier > 0) {
            metadata = metadata
                .withTierData(1, Math.max(1, maxTier), selection.getMasterTier(), selection.getMasterTier());
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

        Map<Long, IStructureElement<?>> visitedElementsByPos = new HashMap<>(visitedElements.size());
        for (VisitedStructureElement visitedElement : visitedElements) {
            visitedElementsByPos.put(
                pack(visitedElement.getX(), visitedElement.getY(), visitedElement.getZ()),
                visitedElement.getElement());
        }

        for (AbsolutePreviewBlock block : absoluteBlocks) {
            IStructureElement<?> visitedElement = visitedElementsByPos
                .get(pack(block.getX(), block.getY(), block.getZ()));
            StructureLibElementTooltipResolver.TooltipDetails details = visitedElement != null
                ? tooltipResolver.resolve(
                    constructable != null ? constructable : new Object(),
                    visitedElement,
                    world,
                    block.getX(),
                    block.getY(),
                    block.getZ(),
                    trigger,
                    actor)
                : StructureLibElementTooltipResolver.TooltipDetails.empty();
            metadata = metadata.withBlockTooltip(
                block.getX() - minX,
                block.getY() - minY,
                block.getZ() - minZ,
                new StructureLibSceneMetadata.BlockTooltipData(
                    GENERIC_STRUCTURELIB_DESCRIPTION,
                    details.getBlockCandidates(),
                    details.getHatchDescriptionLines(),
                    details.getHatchCandidates()));
        }
        return metadata;
    }

    static String resolveFirstChannelLabel(List<VisitedStructureElement> visitedElements) {
        for (VisitedStructureElement visitedElement : visitedElements) {
            String label = resolveChannelId(visitedElement.getElement());
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
        Class<?> current = element.getClass();
        while (current != null && current != Object.class) {
            for (Field field : current.getDeclaredFields()) {
                if (field.getType() != String.class || !field.getName()
                    .toLowerCase()
                    .contains("channel")) {
                    continue;
                }
                try {
                    field.setAccessible(true);
                    Object value = field.get(element);
                    if (value instanceof String stringValue) {
                        String normalized = resolveChannelId(stringValue);
                        if (normalized != null) {
                            return normalized;
                        }
                    }
                } catch (Throwable ignored) {}
            }
            current = current.getSuperclass();
        }
        return null;
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

    public static class VisitedStructureElement {

        private final int x;
        private final int y;
        private final int z;
        private final IStructureElement<?> element;

        public VisitedStructureElement(int x, int y, int z, IStructureElement<?> element) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.element = element;
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

        public IStructureElement<?> getElement() {
            return element;
        }
    }
}
