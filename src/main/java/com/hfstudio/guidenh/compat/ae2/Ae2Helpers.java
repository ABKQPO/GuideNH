package com.hfstudio.guidenh.compat.ae2;

import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S35PacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.util.ForgeDirection;

import com.hfstudio.guidenh.guide.scene.level.GuidebookLevel;

import appeng.api.networking.IGridHost;
import appeng.api.util.AECableType;
import appeng.me.helpers.AENetworkProxy;
import appeng.me.helpers.IGridProxyable;
import appeng.parts.networking.PartCable;
import appeng.parts.networking.PartCableSmart;
import appeng.tile.AEBaseTile;
import appeng.tile.networking.TileCableBus;
import cpw.mods.fml.common.Optional;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

/**
 * Public facade for AE2-specific guide preview support. Callers MUST gate every entry
 * point behind {@code Mods.AE2.isModLoaded()} before invoking; the inner
 * {@link Optional.Method} annotations stub the bodies when AE2 is absent so the JVM
 * never resolves the AE2 types referenced here at runtime.
 *
 * <p>
 * Cable connections are computed from the guide level tile layout and applied directly
 * to the center cable part via {@code PartCable.readFromStream}. No AE2 grid nodes,
 * grids, or {@code WorldData} are touched, keeping the guide preview safe to run on
 * the client render thread without corrupting real-world AE2 network state.
 * </p>
 */
public final class Ae2Helpers {

    private Ae2Helpers() {}

    @Optional.Method(modid = "appliedenergistics2")
    public static void prepare(GuidebookLevel level) {
        for (TileEntity te : level.getTileEntities()) {
            if (te instanceof TileCableBus cableBusTile) {
                syncCableBusConnections(cableBusTile, level);
            } else if (te instanceof AEBaseTile aeTile) {
                syncDescriptionPacket(aeTile);
            }
        }
        level.getOrCreateFakeWorld()
            .syncLoadedTileEntities(level.getTileEntities());
    }

    /**
     * Determines which sides of this cable bus connect to adjacent {@link IGridHost}
     * tiles and applies the result to the center {@link PartCable} via
     * {@code PartCable.readFromStream}, without creating any AE2 grid node or grid
     * objects.
     *
     * <p>
     * Sides that have a part installed are excluded; those sides are owned by the
     * installed part and do not represent external cable connections from the center.
     * </p>
     */
    @Optional.Method(modid = "appliedenergistics2")
    static void syncCableBusConnections(TileCableBus cableBusTile, GuidebookLevel level) {
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
                var connectableSides = proxy.getConnectableSides();
                if (connectableSides.isEmpty()) {
                    // Unformed multiblock tiles (e.g. Crafting CPU components) initialise
                    // their proxy with no connectable sides until the cluster forms.
                    // Fall back to the cable-type check so they still appear connected in
                    // the guide preview.
                    adjCanConnect = adjHost.getCableConnectionType(dir.getOpposite()) != AECableType.NONE;
                } else {
                    adjCanConnect = connectableSides.contains(dir.getOpposite());
                }
            } else {
                adjCanConnect = adjHost.getCableConnectionType(dir.getOpposite()) != AECableType.NONE;
            }
            if (!adjCanConnect) {
                continue;
            }
            cs |= (1 << dir.ordinal());
        }

        if (!(cableBusTile.getPart(ForgeDirection.UNKNOWN) instanceof PartCable cable)) {
            return;
        }

        // PartCable.readFromStream expects 1 signed byte (connection bitmask) followed by
        // 4 bytes (channel counts per side, packed 4 bits each).
        //
        // Bit layout of the byte:
        // bits 0-5: one bit per ForgeDirection.VALID_DIRECTIONS (ordinals 0-5) = connected sides
        // bit 6 : ForgeDirection.UNKNOWN ordinal = "powered" flag
        //
        // If any connection exists we mark the cable as powered so that Smart Cable
        // renders its channel-usage strips rather than the unpowered (blank) state.
        //
        // For channel counts: in guide preview there is no live AE2 grid, so we use
        // a small non-zero default (1 for Smart Cable, 4 for Dense Cable) on each
        // connected side to make the channel-usage visualisation visible and meaningful.
        int csWithPower = cs;
        if (cs != 0) {
            csWithPower |= (1 << ForgeDirection.UNKNOWN.ordinal()); // set powered bit
        }

        int sideOut = 0;
        boolean isDense = !(cable instanceof PartCableSmart);
        int defaultChannels = isDense ? 4 : 1;
        for (ForgeDirection dir : ForgeDirection.VALID_DIRECTIONS) {
            if ((cs & (1 << dir.ordinal())) != 0) {
                sideOut |= (defaultChannels << (dir.ordinal() * 4));
            }
        }

        ByteBuf buf = Unpooled.buffer(5);
        buf.writeByte(csWithPower);
        buf.writeInt(sideOut);
        try {
            cable.readFromStream(buf);
        } catch (Throwable ignored) {}
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
