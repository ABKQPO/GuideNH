package com.hfstudio.guidenh.network;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import io.netty.buffer.ByteBuf;

/** Parallel to request order: hit, cs (unsigned byte), sideOut. */
public class GuideNhAe2CableBatchReplyMessage implements IMessage {

    private long corrId;
    private byte[] hit;
    private byte[] cs;
    private int[] sideOut;

    public GuideNhAe2CableBatchReplyMessage() {
        this.corrId = 0L;
        this.hit = new byte[0];
        this.cs = new byte[0];
        this.sideOut = new int[0];
    }

    public GuideNhAe2CableBatchReplyMessage(long corrId, byte[] hit, byte[] cs, int[] sideOut) {
        this.corrId = corrId;
        this.hit = hit != null ? hit : new byte[0];
        this.cs = cs != null ? cs : new byte[0];
        this.sideOut = sideOut != null ? sideOut : new int[0];
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

    @Override
    public void fromBytes(ByteBuf buf) {
        corrId = buf.readLong();
        int n = buf.readInt();
        if (n < 0 || n > GuideNhAe2CableBatchRequestMessage.MAX_POSITIONS) {
            hit = new byte[0];
            cs = new byte[0];
            sideOut = new int[0];
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
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeLong(corrId);
        int n = hit.length;
        buf.writeInt(n);
        for (int i = 0; i < n; i++) {
            buf.writeByte(hit[i]);
            buf.writeByte(cs[i]);
            buf.writeInt(sideOut[i]);
        }
    }
}
