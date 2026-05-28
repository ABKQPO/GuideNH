package com.hfstudio.guidenh.bridge.transport;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public class WebSocketHandshake {

    private static final String MAGIC = "258EAFA5-E914-47DA-95CA-C5AB0DC85B11";

    public boolean accept(InputStream input, OutputStream output) throws IOException {
        String request = readHttpHeader(input);
        String[] lines = request.split("\r\n");
        String requestLine = lines.length > 0 ? lines[0] : "";
        if (requestLine == null || !requestLine.startsWith("GET ")) {
            writeHttpError(output, 400);
            return false;
        }

        Map<String, String> headers = readHeaders(lines);
        String key = headers.get("sec-websocket-key");
        if (key == null || key.isEmpty() || !isUpgrade(headers)) {
            writeHttpError(output, 400);
            return false;
        }

        String response = "HTTP/1.1 101 Switching Protocols\r\n" + "Upgrade: websocket\r\n"
            + "Connection: Upgrade\r\n"
            + "Sec-WebSocket-Accept: "
            + acceptKey(key)
            + "\r\n"
            + "\r\n";
        output.write(response.getBytes(StandardCharsets.US_ASCII));
        output.flush();
        return true;
    }

    private String readHttpHeader(InputStream input) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        int previousThird = -1;
        int previousSecond = -1;
        int previous = -1;
        int current;
        while ((current = input.read()) >= 0) {
            output.write(current);
            if (previousThird == '\r' && previousSecond == '\n' && previous == '\r' && current == '\n') {
                break;
            }
            if (output.size() > 8192) {
                throw new IOException("WebSocket handshake exceeds maximum size");
            }
            previousThird = previousSecond;
            previousSecond = previous;
            previous = current;
        }
        return output.toString(StandardCharsets.UTF_8);
    }

    private Map<String, String> readHeaders(String[] lines) {
        Map<String, String> headers = new LinkedHashMap<>();
        for (int index = 1; index < lines.length; index++) {
            String line = lines[index];
            if (line.isEmpty()) {
                break;
            }
            int separator = line.indexOf(':');
            if (separator > 0) {
                headers.put(
                    line.substring(0, separator)
                        .trim()
                        .toLowerCase(Locale.ROOT),
                    line.substring(separator + 1)
                        .trim());
            }
        }
        return headers;
    }

    private boolean isUpgrade(Map<String, String> headers) {
        String upgrade = headers.get("upgrade");
        String connection = headers.get("connection");
        return "websocket".equalsIgnoreCase(upgrade) && connection != null
            && connection.toLowerCase(Locale.ROOT)
                .contains("upgrade");
    }

    private String acceptKey(String key) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            byte[] hash = digest.digest((key + MAGIC).getBytes(StandardCharsets.US_ASCII));
            return Base64.getEncoder()
                .encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IOException("SHA-1 digest is unavailable", e);
        }
    }

    private void writeHttpError(OutputStream output, int status) throws IOException {
        String response = "HTTP/1.1 " + status + " Bad Request\r\nConnection: close\r\n\r\n";
        output.write(response.getBytes(StandardCharsets.US_ASCII));
        output.flush();
    }
}
