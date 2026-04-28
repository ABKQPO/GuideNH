package com.hfstudio.guidenh.guide.scene.support.ae2;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S35PacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.util.ForgeDirection;

import appeng.api.networking.IGridNode;
import appeng.api.parts.IPart;
import appeng.me.Grid;
import appeng.me.GridNode;
import appeng.me.helpers.AENetworkProxy;
import appeng.me.helpers.IGridProxyable;
import appeng.parts.CableBusContainer;
import appeng.tile.AEBaseTile;
import appeng.tile.networking.TileCableBus;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.hfstudio.guidenh.guide.scene.level.GuidebookLevel;

public final class GuideAe2PreviewSupport {

    private static final Logger LOG = LogManager.getLogger("GuideNH/ScenePreview");
    private static final ForgeDirection[] PART_SIDES = ForgeDirection.values();

    private static final Field PROXY_NODE_FIELD = resolveField(AENetworkProxy.class, "node");
    private static final Field PROXY_DATA_FIELD = resolveField(AENetworkProxy.class, "data");
    private static volatile boolean nodeCreationFailureLogged;

    private GuideAe2PreviewSupport() {}

    public static void prepare(GuidebookLevel level) {
        preparePreviewState(level.getTileEntities(), new Runnable() {

            @Override
            public void run() {
                level.getOrCreateFakeWorld()
                    .syncLoadedTileEntities(level.getTileEntities());
            }
        });
    }

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

    private static void collectTileState(TileEntity tileEntity, IdentityHashMap<IGridNode, Boolean> seenNodes,
        List<IGridNode> nodes, IdentityHashMap<CableBusContainer, Boolean> seenCableBuses,
        List<CableBusContainer> cableBuses, List<AEBaseTile> tilesToSync) {
        if (!(tileEntity instanceof AEBaseTile aeBaseTile)) {
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

    private static void collectCableBusNodes(TileCableBus cableBusTile, IdentityHashMap<IGridNode, Boolean> seenNodes,
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

    private static void collectNode(AENetworkProxy proxy, IdentityHashMap<IGridNode, Boolean> seenNodes,
        List<IGridNode> nodes) {
        IGridNode node = ensureNode(proxy);
        if (node != null && seenNodes.put(node, Boolean.TRUE) == null) {
            nodes.add(node);
        }
    }

    private static IGridNode ensureNode(AENetworkProxy proxy) {
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

        if (PROXY_NODE_FIELD == null) {
            return null;
        }

        try {
            IGridNode createdNode = new GridNode(proxy);
            if (createdNode == null) {
                return null;
            }

            PROXY_NODE_FIELD.set(proxy, createdNode);

            NBTTagCompound pendingData = readPendingProxyData(proxy);
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
                LOG.warn("Failed to synthesize an AE2 grid node for guide preview rendering", t);
            }
            return null;
        }
    }

    private static void updateOwningGrids(List<IGridNode> nodes) {
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

    private static void syncDescriptionPacket(AEBaseTile tile) {
        try {
            Packet packet = tile.getDescriptionPacket();
            if (packet instanceof S35PacketUpdateTileEntity updatePacket) {
                tile.onDataPacket(null, updatePacket);
            }
        } catch (Throwable ignored) {}
    }

    private static NBTTagCompound readPendingProxyData(AENetworkProxy proxy) {
        if (PROXY_DATA_FIELD == null) {
            return null;
        }
        try {
            Object value = PROXY_DATA_FIELD.get(proxy);
            return value instanceof NBTTagCompound tag ? tag : null;
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static Field resolveField(Class<?> owner, String name) {
        try {
            Field field = owner.getDeclaredField(name);
            field.setAccessible(true);
            return field;
        } catch (Throwable ignored) {
            return null;
        }
    }
}
