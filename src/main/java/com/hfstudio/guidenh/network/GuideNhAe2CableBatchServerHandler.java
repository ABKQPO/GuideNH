package com.hfstudio.guidenh.network;

import java.io.IOException;

import net.minecraft.server.MinecraftServer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.util.ForgeDirection;

import com.hfstudio.guidenh.compat.Mods;
import com.hfstudio.guidenh.compat.ae2.Ae2CableBusPartStreamCodec;
import com.hfstudio.guidenh.compat.ae2.Ae2CableBusSideStreams;

import appeng.parts.networking.PartCable;
import appeng.tile.networking.TileCableBus;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

public class GuideNhAe2CableBatchServerHandler
    implements IMessageHandler<GuideNhAe2CableBatchRequestMessage, IMessage> {

    @Override
    public IMessage onMessage(GuideNhAe2CableBatchRequestMessage message, MessageContext ctx) {
        long corr = message.getCorrId();
        int dim = message.getDim();
        int[] xyz = message.getXyz();
        int n = message.positionCount();
        if (xyz.length < n * 3) {
            n = Math.max(0, xyz.length / 3);
        }

        if (!Mods.AE2.isModLoaded() || n <= 0 || n > GuideNhAe2CableBatchRequestMessage.MAX_POSITIONS) {
            return new GuideNhAe2CableBatchReplyMessage(corr, new byte[0], new byte[0], new int[0], new byte[0][]);
        }

        MinecraftServer srv = MinecraftServer.getServer();
        WorldServer ws = resolveWorldServer(srv, dim);

        byte[] hit = new byte[n];
        byte[] cs = new byte[n];
        int[] sideOut = new int[n];
        byte[][] partPacked = new byte[n][];

        if (ws == null) {
            return new GuideNhAe2CableBatchReplyMessage(corr, hit, cs, sideOut, partPacked);
        }

        for (int i = 0; i < n; i++) {
            int x = xyz[i * 3];
            int y = xyz[i * 3 + 1];
            int z = xyz[i * 3 + 2];
            TileEntity te = ws.getTileEntity(x, y, z);
            if (!(te instanceof TileCableBus bus)) {
                hit[i] = 0;
                cs[i] = 0;
                sideOut[i] = 0;
                partPacked[i] = Ae2CableBusPartStreamCodec.pack(Ae2CableBusSideStreams.EMPTY);
                continue;
            }

            if (bus.getPart(ForgeDirection.UNKNOWN) instanceof PartCable cable) {
                ByteBuf buf = Unpooled.buffer(5);
                try {
                    cable.writeToStream(buf);
                } catch (IOException ignored) {
                    // leave buf empty
                }
                if (buf.readableBytes() >= 5) {
                    hit[i] = 1;
                    cs[i] = buf.readByte();
                    sideOut[i] = buf.readInt();
                } else {
                    hit[i] = 0;
                    cs[i] = 0;
                    sideOut[i] = 0;
                }
            } else {
                hit[i] = 0;
                cs[i] = 0;
                sideOut[i] = 0;
            }

            Ae2CableBusSideStreams parts = Ae2CableBusPartStreamCodec.captureFromBus(bus);
            partPacked[i] = Ae2CableBusPartStreamCodec.pack(parts);
        }

        return new GuideNhAe2CableBatchReplyMessage(corr, hit, cs, sideOut, partPacked);
    }

    private static WorldServer resolveWorldServer(MinecraftServer srv, int dim) {
        if (srv == null) {
            return null;
        }
        WorldServer ws = srv.worldServerForDimension(dim);
        if (ws != null) {
            return ws;
        }
        if (srv.worldServers != null) {
            for (WorldServer w : srv.worldServers) {
                if (w != null && w.provider.dimensionId == dim) {
                    return w;
                }
            }
        }
        return null;
    }
}
