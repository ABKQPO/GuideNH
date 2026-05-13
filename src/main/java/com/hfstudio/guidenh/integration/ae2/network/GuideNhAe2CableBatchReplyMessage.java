package com.hfstudio.guidenh.integration.ae2.network;

import com.hfstudio.guidenh.integration.ae2.Ae2CableBusPartStreamCodec;
import com.hfstudio.guidenh.network.GuideNhCustomPayloadLimits;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import io.netty.buffer.ByteBuf;

/** Parallel to request order: hit, cs (unsigned byte), sideOut, then packed side-part blobs. */
public class GuideNhAe2CableBatchReplyMessage implements IMessage {

    public static final int FORMAT_V1 = 1;

    public static final int MAX_REPLY_PAYLOAD_BYTES = GuideNhCustomPayloadLimits.MAX_PAYLOAD_BYTES;

    private static final int HEADER_BYTES = Long.BYTES + 1 + Integer.BYTES;
    private static final int FIXED_ENTRY_BYTES = 1 + 1 + Integer.BYTES + Short.BYTES;

    private long corrId;

    private byte[] hit;

    private byte[] cs;

    private int[] sideOut;

    /** Per tile index: {@link Ae2CableBusPartStreamCodec#pack} payload. */
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
        int n = safeEntryCount(hit, cs, sideOut, partPacked);
        this.hit = copyBytes(hit, n);
        this.cs = copyBytes(cs, n);
        this.sideOut = copyInts(sideOut, n);
        this.partPacked = budgetPartPayloads(partPacked, n);
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

    /** {@code true} when arrays match {@code n} positions from the request. */
    public boolean isConsistentPayload(int n) {
        if (n < 0 || n > GuideNhAe2CableBatchRequestMessage.MAX_POSITIONS) {
            return false;
        }
        if (hit == null || cs == null || sideOut == null) {
            return false;
        }
        if (hit.length != n || cs.length != n || sideOut.length != n) {
            return false;
        }
        if (partPacked == null || partPacked.length != n) {
            return false;
        }
        return true;
    }

    public int serializedSizeBytes() {
        int n = hit != null ? hit.length : 0;
        int total = HEADER_BYTES + n * FIXED_ENTRY_BYTES;
        for (int i = 0; i < n; i++) {
            byte[] chunk = i < partPacked.length && partPacked[i] != null ? partPacked[i] : new byte[0];
            total += Math.min(chunk.length, 0xFFFF);
        }
        return total;
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

    private static int safeEntryCount(byte[] hit, byte[] cs, int[] sideOut, byte[][] partPacked) {
        int n = hit != null ? hit.length : 0;
        n = Math.min(n, cs != null ? cs.length : 0);
        n = Math.min(n, sideOut != null ? sideOut.length : 0);
        n = Math.min(n, partPacked != null ? partPacked.length : 0);
        return Math.max(0, Math.min(n, GuideNhAe2CableBatchRequestMessage.MAX_POSITIONS));
    }

    private static byte[] copyBytes(byte[] source, int n) {
        byte[] out = new byte[n];
        if (source != null && n > 0) {
            System.arraycopy(source, 0, out, 0, Math.min(source.length, n));
        }
        return out;
    }

    private static int[] copyInts(int[] source, int n) {
        int[] out = new int[n];
        if (source != null && n > 0) {
            System.arraycopy(source, 0, out, 0, Math.min(source.length, n));
        }
        return out;
    }

    private static byte[][] budgetPartPayloads(byte[][] source, int n) {
        byte[][] out = new byte[n][];
        int remaining = MAX_REPLY_PAYLOAD_BYTES - HEADER_BYTES - n * FIXED_ENTRY_BYTES;
        for (int i = 0; i < n; i++) {
            byte[] chunk = source != null && i < source.length && source[i] != null ? source[i] : new byte[0];
            if (chunk.length <= 0 || chunk.length > 0xFFFF || chunk.length > remaining) {
                out[i] = new byte[0];
                continue;
            }
            out[i] = chunk;
            remaining -= chunk.length;
        }
        return out;
    }
}
