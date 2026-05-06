package com.hfstudio.guidenh.compat.ae2;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S35PacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.util.ForgeDirection;

import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.guide.scene.level.GuidebookLevel;

import appeng.api.networking.IGridHost;
import appeng.api.parts.IPart;
import appeng.api.util.AECableType;
import appeng.me.helpers.AENetworkProxy;
import appeng.me.helpers.IGridProxyable;
import appeng.parts.networking.PartCable;
import appeng.tile.AEBaseTile;
import appeng.tile.networking.TileCableBus;
import cpw.mods.fml.common.Optional;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

/**
 * AE2 guide preview: applies server-authoritative AE2 preview bytes from {@link GuidebookLevel#previewAuthorityStore()}
 * ({@link Ae2ServerPreviewRegistration#SUPPLEMENT_ID} cable bus; {@link Ae2BaseTileNetworkStreamPreview#SUPPLEMENT_ID}
 * other {@link AEBaseTile}), merged with locally inferred cable facings where applicable.
 */
public final class Ae2Helpers {

    /** Low six bits of PartCable stream {@code cs}: {@link ForgeDirection#VALID_DIRECTIONS} only. */
    private static final int CS_DIRECTION_MASK = 0x3F;

    private Ae2Helpers() {}

    /**
     * Whether {@link net.minecraft.world.World#markBlockForUpdate} must not reapply
     * {@link TileEntity#getDescriptionPacket}
     * for this TE inside a {@link com.hfstudio.guidenh.guide.scene.level.GuidebookFakeWorld}:
     * {@link #prepare(GuidebookLevel)}
     * already merged server-authoritative preview bytes ({@link Ae2ServerPreviewRegistration#SUPPLEMENT_ID} /
     * {@link Ae2BaseTileNetworkStreamPreview#SUPPLEMENT_ID}). Vanilla description resync rebuilds payloads from an
     * inert
     * preview grid / proxy and overrides that state (channels, TileSecurity connectivity, …).
     */
    @Optional.Method(modid = "appliedenergistics2")
    public static boolean suppressMarkBlockForUpdateDescriptionResync(@Nullable TileEntity te, GuidebookLevel level) {
        if (te == null || level == null) {
            return false;
        }
        if (te instanceof TileCableBus) {
            return true;
        }
        if (te instanceof AEBaseTile) {
            long posKey = GuidebookLevel.packPos(te.xCoord, te.yCoord, te.zCoord);
            byte[] blob = level.previewAuthorityStore()
                .get(posKey, Ae2BaseTileNetworkStreamPreview.SUPPLEMENT_ID);
            return blob != null && blob.length > 0;
        }
        return false;
    }

    @Optional.Method(modid = "appliedenergistics2")
    public static void prepare(GuidebookLevel level) {
        for (TileEntity te : level.getTileEntities()) {
            if (te instanceof TileCableBus cableBusTile) {
                syncCableBusConnections(cableBusTile, level);
                syncCableBusSidePartStreams(cableBusTile, level);
            } else if (te instanceof AEBaseTile aeTile) {
                applyNonCableBaseTilePreview(aeTile, level);
            }
        }
        level.getOrCreateFakeWorld()
            .syncLoadedTileEntities(level.getTileEntities());
    }

    @Optional.Method(modid = "appliedenergistics2")
    static void syncCableBusConnections(TileCableBus cableBusTile, GuidebookLevel level) {
        if (!(cableBusTile.getPart(ForgeDirection.UNKNOWN) instanceof PartCable cable)) {
            return;
        }

        int csDirections = computeCableConnectionMask(cableBusTile, level);
        long posKey = GuidebookLevel.packPos(cableBusTile.xCoord, cableBusTile.yCoord, cableBusTile.zCoord);
        byte[] raw = level.previewAuthorityStore()
            .get(posKey, Ae2ServerPreviewRegistration.SUPPLEMENT_ID);
        Ae2CablePreviewSnapshot snap = raw != null ? Ae2CablePreviewWireCodec.decode(raw)
            : Ae2CablePreviewSnapshot.EMPTY;

        int poweredMask = 1 << ForgeDirection.UNKNOWN.ordinal();
        int csOut;
        int sideOut;
        if (snap.hasCableCore()) {
            csOut = (snap.gridCsUnsigned() & ~CS_DIRECTION_MASK) | (csDirections & CS_DIRECTION_MASK);
            sideOut = snap.sideOut();
            if (sideOut != 0 && (csOut & poweredMask) == 0) {
                csOut |= poweredMask;
            }
        } else {
            csOut = csDirections;
            sideOut = 0;
        }

        ByteBuf buf = Unpooled.buffer(5);
        buf.writeByte((byte) csOut);
        buf.writeInt(sideOut);
        try {
            cable.readFromStream(buf);
        } catch (Throwable ignored) {}
    }

    @Optional.Method(modid = "appliedenergistics2")
    static void syncCableBusSidePartStreams(TileCableBus cableBusTile, GuidebookLevel level) {
        long posKey = GuidebookLevel.packPos(cableBusTile.xCoord, cableBusTile.yCoord, cableBusTile.zCoord);
        byte[] raw = level.previewAuthorityStore()
            .get(posKey, Ae2ServerPreviewRegistration.SUPPLEMENT_ID);
        if (raw == null || raw.length == 0) {
            return;
        }
        Ae2CablePreviewSnapshot snap = Ae2CablePreviewWireCodec.decode(raw);
        if (snap.sideStreams()
            .isEmpty()) {
            return;
        }

        NBTTagCompound baseline = new NBTTagCompound();
        cableBusTile.writeToNBT(baseline);

        for (ForgeDirection dir : ForgeDirection.VALID_DIRECTIONS) {
            byte[] blob = snap.sideStreams()
                .bytesForSideOrdinal(dir.ordinal());
            if (blob == null || blob.length == 0) {
                continue;
            }
            if (cableBusTile.getPart(dir) == null) {
                continue;
            }

            cableBusTile.readFromNBT((NBTTagCompound) baseline.copy());
            cableBusTile.validate();
            IPart part = cableBusTile.getPart(dir);
            if (part == null) {
                continue;
            }
            ByteBuf buf = Unpooled.wrappedBuffer(blob);
            try {
                part.readFromStream(buf);
                if (buf.readableBytes() == 0) {
                    cableBusTile.writeToNBT(baseline);
                } else {
                    cableBusTile.readFromNBT((NBTTagCompound) baseline.copy());
                    cableBusTile.validate();
                }
            } catch (Throwable ignored) {
                cableBusTile.readFromNBT((NBTTagCompound) baseline.copy());
                cableBusTile.validate();
            }
        }
    }

    @Optional.Method(modid = "appliedenergistics2")
    private static int computeCableConnectionMask(TileCableBus cableBusTile, GuidebookLevel level) {
        int x = cableBusTile.xCoord;
        int y = cableBusTile.yCoord;
        int z = cableBusTile.zCoord;

        int cs = 0;
        for (ForgeDirection dir : ForgeDirection.VALID_DIRECTIONS) {
            if (cableBusTile.getPart(dir) != null) {
                continue;
            }
            var adj = level.getTileEntity(x + dir.offsetX, y + dir.offsetY, z + dir.offsetZ);
            if (!(adj instanceof IGridHost adjHost)) {
                continue;
            }
            AECableType myType = cableBusTile.getCableConnectionType(dir);
            if (myType == AECableType.NONE) {
                continue;
            }
            boolean adjCanConnect;
            if (adjHost instanceof IGridProxyable adjProxyable) {
                AENetworkProxy proxy = null;
                try {
                    proxy = adjProxyable.getProxy();
                } catch (Throwable ignored) {}
                if (proxy == null) {
                    continue;
                }
                adjCanConnect = proxy.getConnectableSides()
                    .contains(dir.getOpposite());
            } else {
                adjCanConnect = adjHost.getCableConnectionType(dir.getOpposite()) != AECableType.NONE;
            }
            if (!adjCanConnect) {
                continue;
            }
            cs |= (1 << dir.ordinal());
        }
        return cs;
    }

    @Optional.Method(modid = "appliedenergistics2")
    private static void applyNonCableBaseTilePreview(AEBaseTile aeTile, GuidebookLevel level) {
        if (aeTile instanceof TileCableBus) {
            return;
        }
        long posKey = GuidebookLevel.packPos(aeTile.xCoord, aeTile.yCoord, aeTile.zCoord);
        byte[] blob = level.previewAuthorityStore()
            .get(posKey, Ae2BaseTileNetworkStreamPreview.SUPPLEMENT_ID);
        boolean applied = blob != null && blob.length > 0
            && Ae2BaseTileNetworkStreamPreview.applyAuthorityToPreviewTile(aeTile, blob);
        if (applied) {
            return;
        }
        syncDescriptionPacket(aeTile);
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
