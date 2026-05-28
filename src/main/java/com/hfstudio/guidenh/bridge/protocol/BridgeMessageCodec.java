package com.hfstudio.guidenh.bridge.protocol;

import java.nio.charset.StandardCharsets;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class BridgeMessageCodec {

    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping()
        .create();

    private final BridgeProtocolLimits limits;

    public BridgeMessageCodec(BridgeProtocolLimits limits) {
        this.limits = limits;
    }

    public BridgeEnvelope decode(String message) {
        if (message == null) {
            throw new IllegalArgumentException("Message must not be null");
        }
        if (message.getBytes(StandardCharsets.UTF_8).length > limits.getMaxMessageBytes()) {
            throw new IllegalArgumentException("Message exceeds maximum size");
        }
        BridgeEnvelope envelope = GSON.fromJson(message, BridgeEnvelope.class);
        if (envelope == null || envelope.getMethod() == null
            || envelope.getType() == null
            || envelope.getProtocol() != 1) {
            throw new IllegalArgumentException("Invalid bridge envelope");
        }
        return envelope;
    }

    public String encode(Object value) {
        String json = GSON.toJson(value);
        if (json.getBytes(StandardCharsets.UTF_8).length > limits.getMaxMessageBytes()) {
            throw new IllegalArgumentException("Encoded message exceeds maximum size");
        }
        return json;
    }
}
