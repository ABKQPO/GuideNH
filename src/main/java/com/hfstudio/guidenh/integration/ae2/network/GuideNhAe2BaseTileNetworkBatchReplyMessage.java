package com.hfstudio.guidenh.integration.ae2.network;

import com.hfstudio.guidenh.integration.ae2.Ae2BaseTileNetworkStreamPreview;
import com.hfstudio.guidenh.network.GuideNhCustomPayloadLimits;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import io.netty.buffer.ByteBuf;

/** Per-request-position {@code X} payloads (AE2 tile description stream), parallel to request order. */
public class GuideNhAe2BaseTileNetworkBatchReplyMessage implements IMessage {

    public static final int FORMAT_V1 = 1;

    public static final int MAX_REPLY_PAYLOAD_BYTES = GuideNhCustomPayloadLimits.MAX_PAYLOAD_BYTES;

    private static final int HEADER_BYTES = Long.BYTES + 1 + Integer.BYTES;
    private static final int FIXED_ENTRY_BYTES = Short.BYTES;

    private long corrId;
    private byte[][] xpPayloads;

    public GuideNhAe2BaseTileNetworkBatchReplyMessage() {
        this.corrId = 0L;
        this.xpPayloads = new byte[0][];
    }

    public GuideNhAe2BaseTileNetworkBatchReplyMessage(long corrId, byte[][] xpPayloads) {
        this.corrId = corrId;
        this.xpPayloads = budgetPayloads(xpPayloads);
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

    public int serializedSizeBytes() {
        int n = xpPayloads != null ? xpPayloads.length : 0;
        int total = HEADER_BYTES + n * FIXED_ENTRY_BYTES;
        for (int i = 0; i < n; i++) {
            byte[] chunk = xpPayloads[i] != null ? xpPayloads[i] : new byte[0];
            total += Math.min(chunk.length, Ae2BaseTileNetworkStreamPreview.MAX_X_PAYLOAD_BYTES);
        }
        return total;
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

    private static byte[][] budgetPayloads(byte[][] source) {
        int n = source != null ? Math.min(source.length, GuideNhAe2BaseTileNetworkBatchRequestMessage.MAX_POSITIONS)
            : 0;
        byte[][] out = new byte[n][];
        int remaining = MAX_REPLY_PAYLOAD_BYTES - HEADER_BYTES - n * FIXED_ENTRY_BYTES;
        int maxPayload = Ae2BaseTileNetworkStreamPreview.MAX_X_PAYLOAD_BYTES;
        for (int i = 0; i < n; i++) {
            byte[] chunk = source[i] != null ? source[i] : new byte[0];
            int len = Math.min(chunk.length, maxPayload);
            if (len <= 0 || len > remaining) {
                out[i] = new byte[0];
                continue;
            }
            if (len == chunk.length) {
                out[i] = chunk;
            } else {
                byte[] trimmed = new byte[len];
                System.arraycopy(chunk, 0, trimmed, 0, len);
                out[i] = trimmed;
            }
            remaining -= len;
        }
        return out;
    }
}
