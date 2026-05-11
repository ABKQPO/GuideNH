package com.hfstudio.guidenh.integration.structurelib;

import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;

import org.jetbrains.annotations.Nullable;

import com.gtnewhorizon.structurelib.alignment.constructable.IConstructable;

public interface StructureLibControllerDiscoveryIntegration {

    default void appendCandidates(String blockId, Block block, @Nullable Item item, List<ItemStack> subItems,
        List<StructureLibControllerCandidate> candidates) {}

    @Nullable
    default Object resolveIdentity(StructureLibControllerCandidate candidate, TileEntity controllerTile,
        IConstructable constructable) {
        return null;
    }
}
