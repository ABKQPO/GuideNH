package com.hfstudio.guidenh.network;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import io.netty.buffer.ByteBuf;

/** Client asks server to snapshot AE2 cable stream bytes at world coords (server runs PartCable.writeToStream). */
public class GuideNhAe2CableBatchRequestMessage implements IMessage {

    public static final int MAX_POSITIONS = 512;

    private long corrId;
    private int dim;
    /** Flat xyzxyz… length divisible by 3. */
    private int[] xyz;

    public GuideNhAe2CableBatchRequestMessage() {
        this.corrId = 0L;
        this.dim = 0;
        this.xyz = new int[0];
    }

    public GuideNhAe2CableBatchRequestMessage(long corrId, int dim, int[] xyz) {
        this.corrId = corrId;
        this.dim = dim;
        this.xyz = xyz != null ? xyz : new int[0];
    }

    public long getCorrId() {
        return corrId;
    }

    public int getDim() {
        return dim;
    }

    public int[] getXyz() {
        return xyz;
    }

    public int positionCount() {
        return xyz.length / 3;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        corrId = buf.readLong();
        dim = buf.readInt();
        int n = buf.readInt();
        if (n < 0 || n > MAX_POSITIONS) {
            xyz = new int[0];
            return;
        }
        xyz = new int[n * 3];
        for (int i = 0; i < xyz.length; i++) {
            xyz[i] = buf.readInt();
        }
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeLong(corrId);
        buf.writeInt(dim);
        int n = positionCount();
        buf.writeInt(n);
        for (int v : xyz) {
            buf.writeInt(v);
        }
    }
}
