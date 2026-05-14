package com.hfstudio.guidenh.network;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import io.netty.buffer.ByteBuf;

public class GuideNhRegionExportRequestMessage implements IMessage {

    private int requestId;
    private int x;
    private int y;
    private int z;
    private int sizeX;
    private int sizeY;
    private int sizeZ;
    private boolean includeEntities;

    public GuideNhRegionExportRequestMessage() {}

    public GuideNhRegionExportRequestMessage(int requestId, int x, int y, int z, int sizeX, int sizeY, int sizeZ,
        boolean includeEntities) {
        this.requestId = requestId;
        this.x = x;
        this.y = y;
        this.z = z;
        this.sizeX = sizeX;
        this.sizeY = sizeY;
        this.sizeZ = sizeZ;
        this.includeEntities = includeEntities;
    }

    public int getRequestId() {
        return requestId;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getZ() {
        return z;
    }

    public int getSizeX() {
        return sizeX;
    }

    public int getSizeY() {
        return sizeY;
    }

    public int getSizeZ() {
        return sizeZ;
    }

    public boolean isIncludeEntities() {
        return includeEntities;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        requestId = buf.readInt();
        x = buf.readInt();
        y = buf.readInt();
        z = buf.readInt();
        sizeX = buf.readInt();
        sizeY = buf.readInt();
        sizeZ = buf.readInt();
        includeEntities = buf.readBoolean();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(requestId);
        buf.writeInt(x);
        buf.writeInt(y);
        buf.writeInt(z);
        buf.writeInt(sizeX);
        buf.writeInt(sizeY);
        buf.writeInt(sizeZ);
        buf.writeBoolean(includeEntities);
    }
}
