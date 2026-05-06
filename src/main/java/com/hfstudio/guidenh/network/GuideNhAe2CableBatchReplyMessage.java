package com.hfstudio.guidenh.network;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import io.netty.buffer.ByteBuf;

/** Parallel to request order: hit, cs (unsigned byte), sideOut, then packed side-part blobs. */
public class GuideNhAe2CableBatchReplyMessage implements IMessage {

    public static final int FORMAT_V1 = 1;

    private long corrId;

    private byte[] hit;

    private byte[] cs;

    private int[] sideOut;

    /** Per tile index: {@link com.hfstudio.guidenh.compat.ae2.Ae2CableBusPartStreamCodec#pack} payload. */
    private byte[][] partPacked;

    public GuideNhAe2CableBatchReplyMessage() {
        this.corrId = 0L;
        this.hit = new byte[0];
        this.cs = new byte[0];
        this.sideOut = new int[0];
        this.partPacked = new byte[0][];
    }

    public GuideNhAe2CableBatchReplyMessage(long corrId, byte[] hit, byte[] cs, int[] sideOut, byte[][] partPacked) {
        this.corrId = corrId;
        this.hit = hit != null ? hit : new byte[0];
        this.cs = cs != null ? cs : new byte[0];
        this.sideOut = sideOut != null ? sideOut : new int[0];
        this.partPacked = partPacked != null ? partPacked : new byte[0][];
    }

    public long getCorrId() {
        return corrId;
    }

    public byte[] getHit() {
        return hit;
    }

    public byte[] getCs() {
        return cs;
    }

    public int[] getSideOut() {
        return sideOut;
    }

    public byte[][] getPartPacked() {
        return partPacked;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        corrId = buf.readLong();
        int formatVer = buf.readUnsignedByte();
        int n = buf.readInt();
        if (n < 0 || n > GuideNhAe2CableBatchRequestMessage.MAX_POSITIONS) {
            hit = new byte[0];
            cs = new byte[0];
            sideOut = new int[0];
            partPacked = new byte[0][];
            return;
        }
        hit = new byte[n];
        cs = new byte[n];
        sideOut = new int[n];
        for (int i = 0; i < n; i++) {
            hit[i] = buf.readByte();
            cs[i] = buf.readByte();
            sideOut[i] = buf.readInt();
        }
        if (formatVer >= FORMAT_V1) {
            partPacked = new byte[n][];
            for (int i = 0; i < n; i++) {
                int chunkLen = buf.readUnsignedShort();
                if (chunkLen < 0 || chunkLen > 65535) {
                    partPacked[i] = new byte[0];
                    continue;
                }
                partPacked[i] = new byte[chunkLen];
                buf.readBytes(partPacked[i]);
            }
        } else {
            partPacked = new byte[n][];
            for (int i = 0; i < n; i++) {
                partPacked[i] = new byte[0];
            }
        }
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeLong(corrId);
        buf.writeByte(FORMAT_V1);
        int n = hit.length;
        buf.writeInt(n);
        for (int i = 0; i < n; i++) {
            buf.writeByte(hit[i]);
            buf.writeByte(cs[i]);
            buf.writeInt(sideOut[i]);
        }
        for (int i = 0; i < n; i++) {
            byte[] chunk = i < partPacked.length && partPacked[i] != null ? partPacked[i] : new byte[0];
            buf.writeShort(chunk.length);
            buf.writeBytes(chunk);
        }
    }
}
