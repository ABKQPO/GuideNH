package com.hfstudio.guidenh.network;

import cpw.mods.fml.common.network.ByteBufUtils;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import io.netty.buffer.ByteBuf;

public final class GuideNhStructureRequestMessage implements IMessage {

    public static final byte ACTION_CACHE = 0;
    public static final byte ACTION_IMPORT_AND_PLACE = 1;
    public static final byte ACTION_PLACE_ALL = 2;

    private byte action;
    private int x;
    private int y;
    private int z;
    private String structureText;

    public GuideNhStructureRequestMessage() {
        this((byte) 0, 0, 0, 0, "");
    }

    private GuideNhStructureRequestMessage(byte action, int x, int y, int z, String structureText) {
        this.action = action;
        this.x = x;
        this.y = y;
        this.z = z;
        this.structureText = structureText;
    }

    public static GuideNhStructureRequestMessage cache(String structureText) {
        return new GuideNhStructureRequestMessage(ACTION_CACHE, 0, 0, 0, structureText);
    }

    public static GuideNhStructureRequestMessage importAndPlace(int x, int y, int z, String structureText) {
        return new GuideNhStructureRequestMessage(ACTION_IMPORT_AND_PLACE, x, y, z, structureText);
    }

    public static GuideNhStructureRequestMessage placeAll(int x, int y, int z) {
        return new GuideNhStructureRequestMessage(ACTION_PLACE_ALL, x, y, z, "");
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

    public String getStructureText() {
        return structureText;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        action = buf.readByte();
        x = buf.readInt();
        y = buf.readInt();
        z = buf.readInt();
        structureText = ByteBufUtils.readUTF8String(buf);
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeByte(action);
        buf.writeInt(x);
        buf.writeInt(y);
        buf.writeInt(z);
        ByteBufUtils.writeUTF8String(buf, structureText != null ? structureText : "");
    }
}
