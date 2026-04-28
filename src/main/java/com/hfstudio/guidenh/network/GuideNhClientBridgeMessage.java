package com.hfstudio.guidenh.network;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import io.netty.buffer.ByteBuf;

public class GuideNhClientBridgeMessage implements IMessage {

    public static final byte ACTION_IMPORT_STRUCTURE = 0;

    private byte action;
    private int x;
    private int y;
    private int z;

    public GuideNhClientBridgeMessage() {
        this((byte) 0, 0, 0, 0);
    }

    private GuideNhClientBridgeMessage(byte action, int x, int y, int z) {
        this.action = action;
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public static GuideNhClientBridgeMessage importStructure(int x, int y, int z) {
        return new GuideNhClientBridgeMessage(ACTION_IMPORT_STRUCTURE, x, y, z);
    }

    public byte getAction() {
        return action;
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

    @Override
    public void fromBytes(ByteBuf buf) {
        action = buf.readByte();
        x = buf.readInt();
        y = buf.readInt();
        z = buf.readInt();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeByte(action);
        buf.writeInt(x);
        buf.writeInt(y);
        buf.writeInt(z);
    }
}
