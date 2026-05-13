package com.hfstudio.guidenh.network;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import io.netty.buffer.ByteBuf;

public class GuideNhStructureRequestMessage implements IMessage {

    public static final byte ACTION_CACHE = 0;
    public static final byte ACTION_IMPORT_AND_PLACE = 1;
    public static final byte ACTION_PLACE_ALL = 2;
    public static final byte ACTION_CACHE_CHUNK = 3;
    public static final byte ACTION_IMPORT_AND_PLACE_CHUNK = 4;

    public static final int MAX_CHUNKS_PER_STRUCTURE = 4096;

    private static final AtomicInteger NEXT_TRANSFER_ID = new AtomicInteger(1);

    private byte action;
    private int x;
    private int y;
    private int z;
    private int transferId;
    private int chunkIndex;
    private int chunkCount;
    private byte[] structureBytes;

    public GuideNhStructureRequestMessage() {
        this((byte) 0, 0, 0, 0, 0, 0, 0, new byte[0]);
    }

    private GuideNhStructureRequestMessage(byte action, int x, int y, int z, String structureText) {
        this(
            action,
            x,
            y,
            z,
            0,
            0,
            0,
            structureText != null ? structureText.getBytes(StandardCharsets.UTF_8) : new byte[0]);
    }

    private GuideNhStructureRequestMessage(byte action, int x, int y, int z, int transferId, int chunkIndex,
        int chunkCount, byte[] structureBytes) {
        this.action = action;
        this.x = x;
        this.y = y;
        this.z = z;
        this.transferId = transferId;
        this.chunkIndex = chunkIndex;
        this.chunkCount = chunkCount;
        this.structureBytes = structureBytes != null ? structureBytes : new byte[0];
    }

    public static GuideNhStructureRequestMessage cache(String structureText) {
        byte[] bytes = encodeStructureTextForSinglePacket(structureText);
        return new GuideNhStructureRequestMessage(ACTION_CACHE, 0, 0, 0, 0, 0, 0, bytes);
    }

    public static GuideNhStructureRequestMessage importAndPlace(int x, int y, int z, String structureText) {
        byte[] bytes = encodeStructureTextForSinglePacket(structureText);
        return new GuideNhStructureRequestMessage(ACTION_IMPORT_AND_PLACE, x, y, z, 0, 0, 0, bytes);
    }

    public static GuideNhStructureRequestMessage placeAll(int x, int y, int z) {
        return new GuideNhStructureRequestMessage(ACTION_PLACE_ALL, x, y, z, "");
    }

    public static List<GuideNhStructureRequestMessage> cachePackets(String structureText) {
        return split(ACTION_CACHE, ACTION_CACHE_CHUNK, 0, 0, 0, structureText);
    }

    public static List<GuideNhStructureRequestMessage> importAndPlacePackets(int x, int y, int z,
        String structureText) {
        return split(ACTION_IMPORT_AND_PLACE, ACTION_IMPORT_AND_PLACE_CHUNK, x, y, z, structureText);
    }

    private static List<GuideNhStructureRequestMessage> split(byte singleAction, byte chunkAction, int x, int y, int z,
        String structureText) {
        byte[] bytes = structureText != null ? structureText.getBytes(StandardCharsets.UTF_8) : new byte[0];
        int maxChunkBytes = GuideNhCustomPayloadLimits.MAX_STRUCTURE_BYTES_PER_PACKET;
        if (bytes.length <= maxChunkBytes) {
            return Collections.singletonList(new GuideNhStructureRequestMessage(singleAction, x, y, z, 0, 0, 0, bytes));
        }

        int chunkCount = (bytes.length + maxChunkBytes - 1) / maxChunkBytes;
        if (chunkCount > MAX_CHUNKS_PER_STRUCTURE) {
            throw new IllegalArgumentException("Structure is too large to sync safely: " + bytes.length + " bytes");
        }

        int transferId = NEXT_TRANSFER_ID.getAndIncrement();
        List<GuideNhStructureRequestMessage> packets = new ArrayList<>(chunkCount);
        for (int i = 0; i < chunkCount; i++) {
            int from = i * maxChunkBytes;
            int to = Math.min(bytes.length, from + maxChunkBytes);
            packets.add(
                new GuideNhStructureRequestMessage(
                    chunkAction,
                    x,
                    y,
                    z,
                    transferId,
                    i,
                    chunkCount,
                    Arrays.copyOfRange(bytes, from, to)));
        }
        return packets;
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
        return new String(structureBytes, StandardCharsets.UTF_8);
    }

    public byte[] getStructureBytes() {
        return structureBytes;
    }

    public int getTransferId() {
        return transferId;
    }

    public int getChunkIndex() {
        return chunkIndex;
    }

    public int getChunkCount() {
        return chunkCount;
    }

    public boolean isChunkedStructureTransfer() {
        return action == ACTION_CACHE_CHUNK || action == ACTION_IMPORT_AND_PLACE_CHUNK;
    }

    public byte getCompletedChunkAction() {
        if (action == ACTION_CACHE_CHUNK) {
            return ACTION_CACHE;
        }
        if (action == ACTION_IMPORT_AND_PLACE_CHUNK) {
            return ACTION_IMPORT_AND_PLACE;
        }
        return action;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        action = buf.readByte();
        x = buf.readInt();
        y = buf.readInt();
        z = buf.readInt();
        transferId = buf.readInt();
        chunkIndex = buf.readInt();
        chunkCount = buf.readInt();
        int len = buf.readInt();
        if (len < 0 || len > GuideNhCustomPayloadLimits.MAX_STRUCTURE_BYTES_PER_PACKET || len > buf.readableBytes()) {
            structureBytes = new byte[0];
            if (len > 0) {
                buf.skipBytes(Math.min(len, buf.readableBytes()));
            }
            return;
        }
        structureBytes = new byte[len];
        buf.readBytes(structureBytes);
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeByte(action);
        buf.writeInt(x);
        buf.writeInt(y);
        buf.writeInt(z);
        buf.writeInt(transferId);
        buf.writeInt(chunkIndex);
        buf.writeInt(chunkCount);
        byte[] bytes = structureBytes != null ? structureBytes : new byte[0];
        int len = Math.min(bytes.length, GuideNhCustomPayloadLimits.MAX_STRUCTURE_BYTES_PER_PACKET);
        buf.writeInt(len);
        buf.writeBytes(bytes, 0, len);
    }

    private static byte[] encodeStructureTextForSinglePacket(String structureText) {
        byte[] bytes = structureText != null ? structureText.getBytes(StandardCharsets.UTF_8) : new byte[0];
        if (bytes.length > GuideNhCustomPayloadLimits.MAX_STRUCTURE_BYTES_PER_PACKET) {
            throw new IllegalArgumentException(
                "Structure payload is too large for one packet: " + bytes.length + " bytes; use split packets");
        }
        return bytes;
    }
}
