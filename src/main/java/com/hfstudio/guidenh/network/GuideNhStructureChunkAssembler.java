package com.hfstudio.guidenh.network;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

import org.jetbrains.annotations.Nullable;

public final class GuideNhStructureChunkAssembler {

    private final byte[][] chunks;
    private int received;
    private int totalBytes;

    public GuideNhStructureChunkAssembler(int chunkCount) {
        if (chunkCount <= 0 || chunkCount > GuideNhStructureRequestMessage.MAX_CHUNKS_PER_STRUCTURE) {
            throw new IllegalArgumentException("Invalid structure chunk count: " + chunkCount);
        }
        this.chunks = new byte[chunkCount][];
    }

    @Nullable
    public synchronized String accept(GuideNhStructureRequestMessage message) {
        int index = message.getChunkIndex();
        if (message.getChunkCount() != chunks.length || index < 0 || index >= chunks.length) {
            return null;
        }
        byte[] bytes = message.getStructureBytes();
        if (bytes == null || bytes.length > GuideNhCustomPayloadLimits.MAX_STRUCTURE_BYTES_PER_PACKET) {
            return null;
        }
        if (chunks[index] == null) {
            chunks[index] = bytes;
            received++;
            totalBytes += bytes.length;
        }
        if (received != chunks.length) {
            return null;
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream(totalBytes);
        for (byte[] chunk : chunks) {
            if (chunk == null) {
                return null;
            }
            out.write(chunk, 0, chunk.length);
        }
        return new String(out.toByteArray(), StandardCharsets.UTF_8);
    }
}
