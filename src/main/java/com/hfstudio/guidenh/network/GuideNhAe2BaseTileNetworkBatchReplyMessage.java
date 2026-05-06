package com.hfstudio.guidenh.network;

import com.hfstudio.guidenh.compat.ae2.Ae2BaseTileNetworkStreamPreview;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import io.netty.buffer.ByteBuf;

/** Per-request-position {@code X} payloads (AE2 tile description stream), parallel to request order. */
public class GuideNhAe2BaseTileNetworkBatchReplyMessage implements IMessage {

    public static final int FORMAT_V1 = 1;

    private long corrId;
    private byte[][] xpPayloads;

    public GuideNhAe2BaseTileNetworkBatchReplyMessage() {
        this.corrId = 0L;
        this.xpPayloads = new byte[0][];
    }

    public GuideNhAe2BaseTileNetworkBatchReplyMessage(long corrId, byte[][] xpPayloads) {
        this.corrId = corrId;
        this.xpPayloads = xpPayloads != null ? xpPayloads : new byte[0][];
    }

    public long getCorrId() {
        return corrId;
    }

    public byte[][] getXpPayloads() {
        return xpPayloads;
    }

    public boolean isConsistentPayload(int n) {
        if (n < 0 || n > GuideNhAe2BaseTileNetworkBatchRequestMessage.MAX_POSITIONS) {
            return false;
        }
        return xpPayloads != null && xpPayloads.length == n;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        corrId = buf.readLong();
        buf.readUnsignedByte();
        int n = buf.readInt();
        if (n < 0 || n > GuideNhAe2BaseTileNetworkBatchRequestMessage.MAX_POSITIONS) {
            xpPayloads = new byte[0][];
            return;
        }
        xpPayloads = new byte[n][];
        int maxPayload = Ae2BaseTileNetworkStreamPreview.MAX_X_PAYLOAD_BYTES;
        for (int i = 0; i < n; i++) {
            int chunkLen = buf.readUnsignedShort();
            if (chunkLen <= 0) {
                xpPayloads[i] = new byte[0];
                continue;
            }
            if (chunkLen > maxPayload) {
                xpPayloads[i] = new byte[0];
                int skip = Math.min(chunkLen, buf.readableBytes());
                if (skip > 0) {
                    buf.skipBytes(skip);
                }
                continue;
            }
            xpPayloads[i] = new byte[chunkLen];
            buf.readBytes(xpPayloads[i]);
        }
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeLong(corrId);
        buf.writeByte(FORMAT_V1);
        int n = xpPayloads != null ? xpPayloads.length : 0;
        buf.writeInt(n);
        for (int i = 0; i < n; i++) {
            byte[] chunk = xpPayloads[i] != null ? xpPayloads[i] : new byte[0];
            int len = Math.min(chunk.length, Ae2BaseTileNetworkStreamPreview.MAX_X_PAYLOAD_BYTES);
            buf.writeShort(len);
            if (len > 0) {
                buf.writeBytes(chunk, 0, len);
            }
        }
    }
}
