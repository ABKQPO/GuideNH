package com.hfstudio.guidenh.network;

import net.minecraft.server.MinecraftServer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.WorldServer;

import com.hfstudio.guidenh.compat.Mods;
import com.hfstudio.guidenh.compat.ae2.Ae2BaseTileNetworkStreamPreview;

import appeng.tile.AEBaseTile;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;

public class GuideNhAe2BaseTileNetworkBatchServerHandler
    implements IMessageHandler<GuideNhAe2BaseTileNetworkBatchRequestMessage, IMessage> {

    @Override
    public IMessage onMessage(GuideNhAe2BaseTileNetworkBatchRequestMessage message, MessageContext ctx) {
        long corr = message.getCorrId();
        int dim = message.getDim();
        int[] xyz = message.getXyz();
        int n = message.positionCount();
        if (xyz.length < n * 3) {
            n = Math.max(0, xyz.length / 3);
        }

        if (!Mods.AE2.isModLoaded() || n <= 0 || n > GuideNhAe2BaseTileNetworkBatchRequestMessage.MAX_POSITIONS) {
            return new GuideNhAe2BaseTileNetworkBatchReplyMessage(corr, new byte[0][]);
        }

        MinecraftServer srv = MinecraftServer.getServer();
        WorldServer ws = resolveWorldServer(srv, dim);
        byte[][] out = new byte[n][];
        if (ws == null) {
            for (int i = 0; i < n; i++) {
                out[i] = new byte[0];
            }
            return new GuideNhAe2BaseTileNetworkBatchReplyMessage(corr, out);
        }

        int maxBytes = Ae2BaseTileNetworkStreamPreview.MAX_X_PAYLOAD_BYTES;
        for (int i = 0; i < n; i++) {
            int x = xyz[i * 3];
            int y = xyz[i * 3 + 1];
            int z = xyz[i * 3 + 2];
            TileEntity te = ws.getTileEntity(x, y, z);
            if (!Ae2BaseTileNetworkStreamPreview.eligible(te)) {
                out[i] = new byte[0];
                continue;
            }
            AEBaseTile aeTile = (AEBaseTile) te;
            byte[] raw = Ae2BaseTileNetworkStreamPreview.captureAuthoritativeXPayload(aeTile);
            if (raw == null || raw.length == 0 || raw.length > maxBytes) {
                out[i] = new byte[0];
            } else {
                out[i] = raw;
            }
        }

        return new GuideNhAe2BaseTileNetworkBatchReplyMessage(corr, out);
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
