package com.hfstudio.guidenh.integration.railcraft;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.util.ArrayList;
import java.util.List;

import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

import com.hfstudio.guidenh.guide.scene.level.GuidebookLevel;
import com.hfstudio.guidenh.integration.Mods;

import cpw.mods.fml.common.Optional;
import mods.railcraft.common.blocks.RailcraftTileEntity;
import mods.railcraft.common.blocks.machine.TileMultiBlock;
import mods.railcraft.common.blocks.machine.beta.TileBoiler;
import mods.railcraft.common.blocks.machine.beta.TileBoilerFirebox;

public class RailcraftPreviewHelpers {

    public static final boolean RAILCRAFT_PREVIEW_WORLD_REMOTE = false;
    public static final boolean RAILCRAFT_PREVIEW_RESETS_CLIENT_TEST_STATE = true;
    public static final boolean RAILCRAFT_PREVIEW_SYNCS_PACKET_STATE = true;
    public static final boolean RAILCRAFT_PREVIEW_PRIORITIZES_BOILER_MASTERS = true;
    public static final boolean RAILCRAFT_PREVIEW_SKIPS_BOILER_NON_MASTERS = true;
    private static final String TILE_BOILER_FIREBOX_CLASS = "mods.railcraft.common.blocks.machine.beta.TileBoilerFirebox";
    private static final String TILE_BOILER_CLASS = "mods.railcraft.common.blocks.machine.beta.TileBoiler";

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
            tickRailcraftMultiblocks(prioritizeRailcraftMasters(multiblocks));
            syncRailcraftTilePackets(multiblocks);
        } finally {
            world.isRemote = previousRemote;
        }
    }

    @Optional.Method(modid = "Railcraft")
    private static List<TileMultiBlock> prioritizeRailcraftMasters(List<TileMultiBlock> multiblocks) {
        List<TileMultiBlock> prioritized = new ArrayList<>(multiblocks.size());
        for (TileMultiBlock multiblock : multiblocks) {
            if (multiblock instanceof TileBoilerFirebox) {
                prioritized.add(multiblock);
            }
        }
        for (TileMultiBlock multiblock : multiblocks) {
            if (!(multiblock instanceof TileBoiler)) {
                prioritized.add(multiblock);
            }
        }
        return prioritized;
    }

    @Optional.Method(modid = "Railcraft")
    private static void syncRailcraftTilePackets(List<TileMultiBlock> multiblocks) {
        for (TileMultiBlock multiblock : multiblocks) {
            if (!isUsable(multiblock)) {
                continue;
            }
            syncRailcraftTilePacket(multiblock);
        }
    }

    @Optional.Method(modid = "Railcraft")
    private static void syncRailcraftTilePacket(RailcraftTileEntity tile) {
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            DataOutputStream data = new DataOutputStream(bytes);
            tile.writePacketData(data);
            tile.readPacketData(new DataInputStream(new ByteArrayInputStream(bytes.toByteArray())));
        } catch (Throwable ignored) {}
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

    private static boolean isInstanceOf(Object value, String className) {
        if (value == null || className == null || className.isEmpty()) {
            return false;
        }
        for (Class<?> type = value.getClass(); type != null; type = type.getSuperclass()) {
            if (className.equals(type.getName())) {
                return true;
            }
        }
        return false;
    }
}
