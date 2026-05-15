package com.hfstudio.guidenh.bridge.transport;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public class WebSocketFrameCodec {

    private static final int FIN = 0x80;
    private static final int MASK = 0x80;
    private static final int SHORT_LENGTH = 126;
    private static final int LONG_LENGTH = 127;
    private static final int MAX_CONTROL_PAYLOAD_BYTES = 125;

    private final int maxPayloadBytes;

    public WebSocketFrameCodec(int maxPayloadBytes) {
        this.maxPayloadBytes = maxPayloadBytes;
    }

    public WebSocketFrame read(InputStream input) throws IOException {
        int first = input.read();
        if (first < 0) {
            throw new EOFException("WebSocket connection closed");
        }
        int second = readByte(input);
        int opcode = first & 0x0F;
        boolean masked = (second & MASK) != 0;
        long payloadLength = second & 0x7F;
        if (payloadLength == SHORT_LENGTH) {
            payloadLength = readUnsignedShort(input);
        } else if (payloadLength == LONG_LENGTH) {
            payloadLength = readLong(input);
        }
        if (payloadLength > maxPayloadBytes) {
            throw new IOException("WebSocket frame exceeds maximum size");
        }

        byte[] mask = masked ? readBytes(input, 4) : new byte[0];
        byte[] payload = readBytes(input, (int) payloadLength);
        if (masked) {
            for (int index = 0; index < payload.length; index++) {
                payload[index] = (byte) (payload[index] ^ mask[index % 4]);
            }
        }
        return new WebSocketFrame(opcode, payload);
    }

    public void writeText(OutputStream output, String message) throws IOException {
        write(output, 1, message.getBytes(StandardCharsets.UTF_8));
    }

    public void writePong(OutputStream output, byte[] payload) throws IOException {
        byte[] safePayload = payload.length <= MAX_CONTROL_PAYLOAD_BYTES ? payload : new byte[0];
        write(output, 10, safePayload);
    }

    public void writeClose(OutputStream output) throws IOException {
        write(output, 8, new byte[0]);
    }

    private void write(OutputStream output, int opcode, byte[] payload) throws IOException {
        if (payload.length > maxPayloadBytes) {
            throw new IOException("WebSocket response exceeds maximum size");
        }
        output.write(FIN | opcode);
        if (payload.length < SHORT_LENGTH) {
            output.write(payload.length);
        } else if (payload.length <= 65535) {
            output.write(SHORT_LENGTH);
            output.write(
                ByteBuffer.allocate(2)
                    .putShort((short) payload.length)
                    .array());
        } else {
            output.write(LONG_LENGTH);
            output.write(
                ByteBuffer.allocate(8)
                    .putLong(payload.length)
                    .array());
        }
        output.write(payload);
        output.flush();
    }

    private int readByte(InputStream input) throws IOException {
        int value = input.read();
        if (value < 0) {
            throw new EOFException("WebSocket connection closed");
        }
        return value;
    }

    private int readUnsignedShort(InputStream input) throws IOException {
        return readByte(input) << 8 | readByte(input);
    }

    private long readLong(InputStream input) throws IOException {
        long result = 0L;
        for (int index = 0; index < 8; index++) {
            result = result << 8 | readByte(input);
        }
        return result;
    }

    private byte[] readBytes(InputStream input, int length) throws IOException {
        byte[] bytes = new byte[length];
        int offset = 0;
        while (offset < length) {
            int read = input.read(bytes, offset, length - offset);
            if (read < 0) {
                throw new EOFException("WebSocket connection closed");
            }
            offset += read;
        }
        return bytes;
    }
}
