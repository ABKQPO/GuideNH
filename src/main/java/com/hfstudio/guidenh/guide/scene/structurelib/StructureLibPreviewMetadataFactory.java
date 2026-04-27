package com.hfstudio.guidenh.guide.scene.structurelib;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

import com.gtnewhorizon.structurelib.structure.IStructureElement;

final class StructureLibPreviewMetadataFactory {

    private static final String GENERIC_STRUCTURELIB_DESCRIPTION = "StructureLib";

    private final StructureLibElementTooltipResolver tooltipResolver;

    StructureLibPreviewMetadataFactory(StructureLibElementTooltipResolver tooltipResolver) {
        this.tooltipResolver = tooltipResolver;
    }

    StructureLibSceneMetadata createMetadata(StructureLibImportRequest request, int minChannel, int maxChannel,
        int currentChannel, List<AbsolutePreviewBlock> absoluteBlocks, List<VisitedStructureElement> visitedElements,
        ItemStack trigger, @Nullable World world) {
        return createMetadata(
            request,
            minChannel,
            maxChannel,
            currentChannel,
            absoluteBlocks,
            visitedElements,
            trigger,
            world,
            null,
            null);
    }

    StructureLibSceneMetadata createMetadata(StructureLibImportRequest request, int minChannel, int maxChannel,
        int currentChannel, List<AbsolutePreviewBlock> absoluteBlocks, List<VisitedStructureElement> visitedElements,
        ItemStack trigger, @Nullable World world, @Nullable Object constructable, @Nullable EntityPlayer actor) {
        StructureLibSceneMetadata metadata = new StructureLibSceneMetadata(
            request.getController(),
            request.getPiece(),
            request.getFacing(),
            request.getRotation(),
            request.getFlip());
        if (maxChannel > minChannel) {
            metadata = metadata.withChannelData(minChannel, maxChannel, currentChannel, currentChannel);
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

    private static long pack(int x, int y, int z) {
        return (((long) x & 0x3FFFFFFL) << 38) | (((long) z & 0x3FFFFFFL) << 12) | ((long) y & 0xFFFL);
    }

    static final class AbsolutePreviewBlock {

        private final int x;
        private final int y;
        private final int z;

        AbsolutePreviewBlock(int x, int y, int z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }

        int getX() {
            return x;
        }

        int getY() {
            return y;
        }

        int getZ() {
            return z;
        }
    }

    static final class VisitedStructureElement {

        private final int x;
        private final int y;
        private final int z;
        private final IStructureElement<?> element;

        VisitedStructureElement(int x, int y, int z, IStructureElement<?> element) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.element = element;
        }

        int getX() {
            return x;
        }

        int getY() {
            return y;
        }

        int getZ() {
            return z;
        }

        IStructureElement<?> getElement() {
            return element;
        }
    }
}
