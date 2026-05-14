package com.hfstudio.guidenh.network;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import io.netty.buffer.ByteBuf;

public class GuideNhRegionExportReplyMessage implements IMessage {

    public static final byte ACTION_COMPLETE = 0;
    public static final byte ACTION_CHUNK = 1;
    public static final byte ACTION_ERROR = 2;

    private byte action;
    private int requestId;
    private int chunkIndex;
    private int chunkCount;
    private byte[] payload;

    public GuideNhRegionExportReplyMessage() {
        this(ACTION_ERROR, 0, 0, 0, new byte[0]);
    }

    private GuideNhRegionExportReplyMessage(byte action, int requestId, int chunkIndex, int chunkCount,
        byte[] payload) {
        this.action = action;
        this.requestId = requestId;
        this.chunkIndex = chunkIndex;
        this.chunkCount = chunkCount;
        this.payload = payload != null ? payload : new byte[0];
    }

    public static List<GuideNhRegionExportReplyMessage> successPackets(int requestId, String structureText) {
        byte[] bytes = structureText != null ? structureText.getBytes(StandardCharsets.UTF_8) : new byte[0];
        int maxChunkBytes = GuideNhCustomPayloadLimits.MAX_STRUCTURE_BYTES_PER_PACKET;
        if (bytes.length <= maxChunkBytes) {
            return Collections
                .singletonList(new GuideNhRegionExportReplyMessage(ACTION_COMPLETE, requestId, 0, 0, bytes));
        }
        int chunkCount = (bytes.length + maxChunkBytes - 1) / maxChunkBytes;
        List<GuideNhRegionExportReplyMessage> packets = new ArrayList<>(chunkCount);
        for (int i = 0; i < chunkCount; i++) {
            int from = i * maxChunkBytes;
            int to = Math.min(bytes.length, from + maxChunkBytes);
            packets.add(
                new GuideNhRegionExportReplyMessage(
                    ACTION_CHUNK,
                    requestId,
                    i,
                    chunkCount,
                    Arrays.copyOfRange(bytes, from, to)));
        }
        return packets;
    }

    public static GuideNhRegionExportReplyMessage error(int requestId, String message) {
        return new GuideNhRegionExportReplyMessage(
            ACTION_ERROR,
            requestId,
            0,
            0,
            (message != null ? message : "").getBytes(StandardCharsets.UTF_8));
    }

    public byte getAction() {
        return action;
    }

    public int getRequestId() {
        return requestId;
    }

    public int getChunkIndex() {
        return chunkIndex;
    }

    public int getChunkCount() {
        return chunkCount;
    }

    public byte[] getPayloadBytes() {
        return payload;
    }

    public String getPayloadText() {
        return new String(payload, StandardCharsets.UTF_8);
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        action = buf.readByte();
        requestId = buf.readInt();
        chunkIndex = buf.readInt();
        chunkCount = buf.readInt();
        int len = buf.readInt();
        if (len < 0 || len > GuideNhCustomPayloadLimits.MAX_STRUCTURE_BYTES_PER_PACKET || len > buf.readableBytes()) {
            payload = new byte[0];
            if (len > 0) {
                buf.skipBytes(Math.min(len, buf.readableBytes()));
            }
            return;
        }
        payload = new byte[len];
        buf.readBytes(payload);
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeByte(action);
        buf.writeInt(requestId);
        buf.writeInt(chunkIndex);
        buf.writeInt(chunkCount);
        byte[] bytes = payload != null ? payload : new byte[0];
        int len = Math.min(bytes.length, GuideNhCustomPayloadLimits.MAX_STRUCTURE_BYTES_PER_PACKET);
        buf.writeInt(len);
        buf.writeBytes(bytes, 0, len);
    }
}
