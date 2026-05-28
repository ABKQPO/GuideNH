package com.hfstudio.guidenh.bridge.protocol;

import com.google.gson.JsonObject;

public class BridgeEnvelope {

    private String id;
    private String type;
    private String method;
    private int protocol;
    private JsonObject payload;

    public String getId() {
        return id;
    }

    public String getType() {
        return type;
    }

    public String getMethod() {
        return method;
    }

    public int getProtocol() {
        return protocol;
    }

    public JsonObject getPayload() {
        return payload;
    }

    public static BridgeEnvelope response(String id, String method, JsonObject payload) {
        BridgeEnvelope envelope = new BridgeEnvelope();
        envelope.id = id;
        envelope.type = "response";
        envelope.method = method;
        envelope.protocol = 1;
        envelope.payload = payload;
        return envelope;
    }

    public static BridgeEnvelope error(String id, String method, JsonObject payload) {
        BridgeEnvelope envelope = new BridgeEnvelope();
        envelope.id = id;
        envelope.type = "error";
        envelope.method = method;
        envelope.protocol = 1;
        envelope.payload = payload;
        return envelope;
    }
}
