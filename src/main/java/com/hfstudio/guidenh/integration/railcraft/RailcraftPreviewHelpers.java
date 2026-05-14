package com.hfstudio.guidenh.integration.railcraft;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

import com.hfstudio.guidenh.guide.scene.level.GuidebookLevel;
import com.hfstudio.guidenh.integration.Mods;

import cpw.mods.fml.common.Optional;
import mods.railcraft.common.blocks.machine.TileMultiBlock;

public class RailcraftPreviewHelpers {

    public static final boolean RAILCRAFT_PREVIEW_WORLD_REMOTE = false;
    public static final boolean RAILCRAFT_PREVIEW_RESETS_CLIENT_TEST_STATE = true;

    private RailcraftPreviewHelpers() {}

    public static void prepareMultiblocks(GuidebookLevel level) {
        if (!Mods.Railcraft.isModLoaded()) {
            return;
        }
        prepareMultiblocksImpl(level);
    }

    @Optional.Method(modid = "Railcraft")
    private static void prepareMultiblocksImpl(GuidebookLevel level) {
        World world = level.getOrCreateFakeWorld();
        List<TileMultiBlock> multiblocks = collectRailcraftMultiblocks(level);
        if (multiblocks.isEmpty()) {
            return;
        }

        boolean previousRemote = world.isRemote;
        world.isRemote = RAILCRAFT_PREVIEW_WORLD_REMOTE;
        try {
            resetRailcraftMultiblocks(multiblocks);
            notifyRailcraftMultiblocks(multiblocks);
            tickRailcraftMultiblocks(multiblocks);
        } finally {
            world.isRemote = previousRemote;
        }
    }

    @Optional.Method(modid = "Railcraft")
    private static void resetRailcraftMultiblocks(List<TileMultiBlock> multiblocks) {
        for (TileMultiBlock multiblock : multiblocks) {
            if (!isUsable(multiblock)) {
                continue;
            }
            multiblock.invalidate();
            multiblock.validate();
        }
    }

    @Optional.Method(modid = "Railcraft")
    private static List<TileMultiBlock> collectRailcraftMultiblocks(GuidebookLevel level) {
        List<TileMultiBlock> multiblocks = new ArrayList<>();
        for (TileEntity tile : level.getTileEntities()) {
            if (tile instanceof TileMultiBlock multiblock) {
                multiblocks.add(multiblock);
            }
        }
        return multiblocks;
    }

    @Optional.Method(modid = "Railcraft")
    private static void notifyRailcraftMultiblocks(List<TileMultiBlock> multiblocks) {
        for (TileMultiBlock multiblock : multiblocks) {
            if (!isUsable(multiblock)) {
                continue;
            }
            multiblock.onBlockAdded();
        }
    }

    @Optional.Method(modid = "Railcraft")
    private static void tickRailcraftMultiblocks(List<TileMultiBlock> multiblocks) {
        for (TileMultiBlock multiblock : multiblocks) {
            if (!isUsable(multiblock)) {
                continue;
            }
            multiblock.updateEntity();
        }
    }

    @Optional.Method(modid = "Railcraft")
    private static boolean isUsable(TileMultiBlock multiblock) {
        return multiblock != null && !multiblock.isInvalid() && multiblock.getWorldObj() != null;
    }
}
