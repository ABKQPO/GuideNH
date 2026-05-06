package com.hfstudio.guidenh.network;

import java.nio.charset.StandardCharsets;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import io.netty.buffer.ByteBuf;

public class GuideNhClientBridgeMessage implements IMessage {

    public static final byte ACTION_IMPORT_STRUCTURE = 0;

    private byte action;
    private int x;
    private int y;
    private int z;
    private String filePath;

    public GuideNhClientBridgeMessage() {
        this((byte) 0, 0, 0, 0, "");
    }

    private GuideNhClientBridgeMessage(byte action, int x, int y, int z, String filePath) {
        this.action = action;
        this.x = x;
        this.y = y;
        this.z = z;
        this.filePath = filePath;
    }

    public static GuideNhClientBridgeMessage importStructure(int x, int y, int z, String filePath) {
        return new GuideNhClientBridgeMessage(ACTION_IMPORT_STRUCTURE, x, y, z, filePath);
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

    public String getFilePath() {
        return filePath;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        action = buf.readByte();
        x = buf.readInt();
        y = buf.readInt();
        z = buf.readInt();
        int len = buf.readShort() & 0xFFFF;
        byte[] bytes = new byte[len];
        buf.readBytes(bytes);
        filePath = new String(bytes, StandardCharsets.UTF_8);
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeByte(action);
        buf.writeInt(x);
        buf.writeInt(y);
        buf.writeInt(z);
        byte[] bytes = filePath.getBytes(StandardCharsets.UTF_8);
        buf.writeShort(bytes.length);
        buf.writeBytes(bytes);
    }
}
