package com.hfstudio.guidenh.compat.ae2;

import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S35PacketUpdateTileEntity;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.util.ForgeDirection;

import com.hfstudio.guidenh.guide.scene.level.ExportedAe2CableBusPartStreams;
import com.hfstudio.guidenh.guide.scene.level.ExportedAe2CableStream;
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
 * AE2 guide preview: merges structure-export cable bytes ({@link ExportedAe2CableStream}) with locally inferred facing
 * connections so preview geometry matches the guide layout while channel stripes match the exported grid snapshot.
 * Side parts apply serialized {@link appeng.api.parts.IPart#writeToStream} payloads from
 * {@link com.hfstudio.guidenh.guide.scene.level.GuideAe2CableBusPartStreamsSnbt}.
 */
public final class Ae2Helpers {

    /** Low six bits of PartCable stream {@code cs}: {@link ForgeDirection#VALID_DIRECTIONS} only. */
    private static final int CS_DIRECTION_MASK = 0x3F;

    private Ae2Helpers() {}

    @Optional.Method(modid = "appliedenergistics2")
    public static void prepare(GuidebookLevel level) {
        for (TileEntity te : level.getTileEntities()) {
            if (te instanceof TileCableBus cableBusTile) {
                syncCableBusConnections(cableBusTile, level);
                syncCableBusSidePartStreams(cableBusTile, level);
            } else if (te instanceof AEBaseTile aeTile) {
                syncDescriptionPacket(aeTile);
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
        ExportedAe2CableStream exported = level.getExportedAe2CableStream(
            cableBusTile.xCoord,
            cableBusTile.yCoord,
            cableBusTile.zCoord);

        int poweredMask = 1 << ForgeDirection.UNKNOWN.ordinal();
        int csOut;
        int sideOut;
        if (exported != null) {
            csOut = (exported.gridCsUnsigned & ~CS_DIRECTION_MASK) | (csDirections & CS_DIRECTION_MASK);
            sideOut = exported.sideOut;
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

    /** SNBT list payloads occasionally carry one trailing junk octet vs AE2's packet decoder — trim up to this many. */
    private static final int AE2_SIDE_STREAM_TAIL_TRIM_MAX = 3;

    @Optional.Method(modid = "appliedenergistics2")
    static void syncCableBusSidePartStreams(TileCableBus cableBusTile, GuidebookLevel level) {
        ExportedAe2CableBusPartStreams exported = level.getExportedAe2CableBusPartStreams(
            cableBusTile.xCoord,
            cableBusTile.yCoord,
            cableBusTile.zCoord);
        if (exported == null || exported.isEmpty()) {
            return;
        }

        NBTTagCompound baseline = new NBTTagCompound();
        cableBusTile.writeToNBT(baseline);

        for (ForgeDirection dir : ForgeDirection.VALID_DIRECTIONS) {
            byte[] blob = exported.bytesForSideOrdinal(dir.ordinal());
            if (blob == null || blob.length == 0) {
                continue;
            }
            if (cableBusTile.getPart(dir) == null) {
                continue;
            }

            boolean applied = false;
            for (int tailTrim = 0; tailTrim <= AE2_SIDE_STREAM_TAIL_TRIM_MAX; tailTrim++) {
                if (blob.length <= tailTrim) {
                    break;
                }
                cableBusTile.readFromNBT((NBTTagCompound) baseline.copy());
                cableBusTile.validate();
                IPart part = cableBusTile.getPart(dir);
                if (part == null) {
                    break;
                }
                ByteBuf buf = Unpooled.wrappedBuffer(blob, 0, blob.length - tailTrim);
                try {
                    part.readFromStream(buf);
                    if (buf.readableBytes() == 0) {
                        applied = true;
                        cableBusTile.writeToNBT(baseline);
                        break;
                    }
                } catch (Throwable ignored) {}
            }

            if (!applied) {
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
    public static void syncDescriptionPacket(AEBaseTile tile) {
        try {
            Packet packet = tile.getDescriptionPacket();
            if (packet instanceof S35PacketUpdateTileEntity updatePacket) {
                tile.onDataPacket(null, updatePacket);
            }
        } catch (Throwable ignored) {}
    }
}
