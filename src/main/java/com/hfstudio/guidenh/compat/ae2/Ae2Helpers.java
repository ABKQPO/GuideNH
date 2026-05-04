package com.hfstudio.guidenh.compat.ae2;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S35PacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.util.ForgeDirection;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.hfstudio.guidenh.guide.scene.level.GuidebookLevel;
import com.hfstudio.guidenh.guide.scene.support.GuideDebugLog;
import com.hfstudio.guidenh.mixins.late.compat.ae2.AccessorAENetworkProxy;

import appeng.api.networking.IGridNode;
import appeng.api.parts.IPart;
import appeng.me.Grid;
import appeng.me.GridNode;
import appeng.me.helpers.AENetworkProxy;
import appeng.me.helpers.IGridProxyable;
import appeng.parts.CableBusContainer;
import appeng.tile.AEBaseTile;
import appeng.tile.networking.TileCableBus;
import cpw.mods.fml.common.Optional;

/**
 * Public facade for AE2-specific guide preview support. Callers MUST gate every entry
 * point behind {@code Mods.AE2.isModLoaded()} before invoking; the inner
 * {@link Optional.Method} annotations stub the bodies when AE2 is absent so the JVM
 * never resolves the AE2 types referenced here at runtime.
 *
 * <p>
 * Replaces the legacy reflective field caches with Mixin Accessors
 * ({@link AccessorAENetworkProxy}); see {@code com.hfstudio.guidenh.mixins.Mixins#AE2_NETWORK_PROXY}.
 * </p>
 */
public final class Ae2Helpers {

    public static final Logger LOG = LogManager.getLogger("GuideNH/ScenePreview");
    public static final ForgeDirection[] PART_SIDES = ForgeDirection.values();

    public static volatile boolean nodeCreationFailureLogged;

    private Ae2Helpers() {}

    @Optional.Method(modid = "appliedenergistics2")
    public static void prepare(GuidebookLevel level) {
        preparePreviewState(
            level.getTileEntities(),
            () -> level.getOrCreateFakeWorld()
                .syncLoadedTileEntities(level.getTileEntities()));
    }

    @Optional.Method(modid = "appliedenergistics2")
    static void preparePreviewState(Iterable<? extends TileEntity> tileEntities, Runnable postSyncAction) {
        IdentityHashMap<IGridNode, Boolean> seenNodes = new IdentityHashMap<>();
        IdentityHashMap<CableBusContainer, Boolean> seenCableBuses = new IdentityHashMap<>();
        ArrayList<IGridNode> nodes = new ArrayList<>();
        ArrayList<CableBusContainer> cableBuses = new ArrayList<>();
        ArrayList<AEBaseTile> tilesToSync = new ArrayList<>();

        for (TileEntity tileEntity : tileEntities) {
            collectTileState(tileEntity, seenNodes, nodes, seenCableBuses, cableBuses, tilesToSync);
        }

        if (nodes.isEmpty() && cableBuses.isEmpty()) {
            return;
        }

        for (CableBusContainer cableBus : cableBuses) {
            try {
                cableBus.addToWorld();
            } catch (Throwable ignored) {}
        }

        for (CableBusContainer cableBus : cableBuses) {
            try {
                cableBus.updateConnections();
            } catch (Throwable ignored) {}
        }

        for (IGridNode node : nodes) {
            try {
                node.updateState();
            } catch (Throwable ignored) {}
        }

        updateOwningGrids(nodes);

        for (AEBaseTile tile : tilesToSync) {
            syncDescriptionPacket(tile);
        }

        if (postSyncAction != null) {
            postSyncAction.run();
        }
    }

    @Optional.Method(modid = "appliedenergistics2")
    public static void collectTileState(TileEntity tileEntity, IdentityHashMap<IGridNode, Boolean> seenNodes,
        List<IGridNode> nodes, IdentityHashMap<CableBusContainer, Boolean> seenCableBuses,
        List<CableBusContainer> cableBuses, List<AEBaseTile> tilesToSync) {
        if (!(tileEntity instanceof AEBaseTile aeBaseTile)) {
            // Non-AE2 tiles that expose an AE2 grid proxy (e.g. GT5 ME hatches) must also
            // have their node initialised so that adjacent cables can form connections.
            if (tileEntity instanceof IGridProxyable nonAeTile) {
                AENetworkProxy proxy = null;
                try {
                    proxy = nonAeTile.getProxy();
                } catch (Throwable ignored) {}
                if (proxy != null) {
                    collectNode(proxy, seenNodes, nodes);
                }
            }
            return;
        }

        tilesToSync.add(aeBaseTile);

        if (tileEntity instanceof TileCableBus cableBusTile) {
            CableBusContainer cableBus = cableBusTile.getCableBus();
            if (cableBus != null && seenCableBuses.put(cableBus, Boolean.TRUE) == null) {
                cableBuses.add(cableBus);
            }
            collectCableBusNodes(cableBusTile, seenNodes, nodes);
            return;
        }

        try {
            aeBaseTile.onReady();
        } catch (Throwable ignored) {}

        if (tileEntity instanceof IGridProxyable gridProxyable) {
            collectNode(gridProxyable.getProxy(), seenNodes, nodes);
        }
    }

    @Optional.Method(modid = "appliedenergistics2")
    public static void collectCableBusNodes(TileCableBus cableBusTile, IdentityHashMap<IGridNode, Boolean> seenNodes,
        List<IGridNode> nodes) {
        for (ForgeDirection side : PART_SIDES) {
            IPart part;
            try {
                part = cableBusTile.getPart(side);
            } catch (Throwable ignored) {
                continue;
            }
            if (part instanceof IGridProxyable gridProxyable) {
                collectNode(gridProxyable.getProxy(), seenNodes, nodes);
            }
        }
    }

    @Optional.Method(modid = "appliedenergistics2")
    public static void collectNode(AENetworkProxy proxy, IdentityHashMap<IGridNode, Boolean> seenNodes,
        List<IGridNode> nodes) {
        IGridNode node = ensureNode(proxy);
        if (node != null && seenNodes.put(node, Boolean.TRUE) == null) {
            nodes.add(node);
        }
    }

    @Optional.Method(modid = "appliedenergistics2")
    public static IGridNode ensureNode(AENetworkProxy proxy) {
        if (proxy == null) {
            return null;
        }

        try {
            proxy.onReady();
        } catch (Throwable ignored) {}

        try {
            IGridNode existingNode = proxy.getNode();
            if (existingNode != null) {
                return existingNode;
            }
        } catch (Throwable ignored) {}

        try {
            AccessorAENetworkProxy accessor = (AccessorAENetworkProxy) proxy;
            IGridNode createdNode = new GridNode(proxy);
            accessor.setNode(createdNode);

            NBTTagCompound pendingData = accessor.getData();
            if (pendingData != null) {
                proxy.readFromNBT(pendingData);
            } else {
                proxy.readFromNBT(null);
            }

            createdNode.updateState();
            return createdNode;
        } catch (Throwable t) {
            if (!nodeCreationFailureLogged) {
                nodeCreationFailureLogged = true;
                GuideDebugLog.warn(LOG, "Failed to synthesize an AE2 grid node for guide preview rendering", t);
            }
            return null;
        }
    }

    @Optional.Method(modid = "appliedenergistics2")
    public static void updateOwningGrids(List<IGridNode> nodes) {
        IdentityHashMap<Grid, Boolean> seenGrids = new IdentityHashMap<>();
        ArrayList<Grid> grids = new ArrayList<>();
        for (IGridNode node : nodes) {
            if (node == null) {
                continue;
            }
            try {
                Object grid = node.getGrid();
                if (grid instanceof Grid castGrid && seenGrids.put(castGrid, Boolean.TRUE) == null) {
                    grids.add(castGrid);
                }
            } catch (Throwable ignored) {}
        }

        for (int i = 0; i < 2; i++) {
            for (Grid grid : grids) {
                try {
                    grid.update();
                } catch (Throwable ignored) {}
            }
        }
    }

    @Optional.Method(modid = "appliedenergistics2")
    public static void syncDescriptionPacket(AEBaseTile tile) {
        try {
            Packet packet = tile.getDescriptionPacket();
            if (packet instanceof S35PacketUpdateTileEntity updatePacket) {
                tile.onDataPacket(null, updatePacket);
            }
        } catch (Throwable ignored) {}
    }
}
