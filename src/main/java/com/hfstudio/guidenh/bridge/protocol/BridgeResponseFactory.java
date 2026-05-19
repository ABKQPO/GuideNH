package com.hfstudio.guidenh.bridge.protocol;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

public class BridgeResponseFactory {

    private static final Gson GSON = new Gson();

    public BridgeEnvelope hello(String id, BridgeProtocolLimits limits) {
        JsonObject payload = new JsonObject();
        payload.addProperty("serverName", "GuideNH");
        payload.addProperty("protocol", 1);
        payload.add("limits", GSON.toJsonTree(limits));
        return BridgeEnvelope.response(id, "hello", payload);
    }

    public BridgeEnvelope semanticResult(String id, String method, Object result) {
        return BridgeEnvelope.response(
            id,
            method,
            GSON.toJsonTree(result)
                .getAsJsonObject());
    }

    public BridgeEnvelope capabilities(String id, Object capabilities) {
        JsonObject payload = new JsonObject();
        payload.add("capabilities", GSON.toJsonTree(capabilities));
        return BridgeEnvelope.response(id, "capabilities", payload);
    }

    public BridgeEnvelope error(String id, String method, String code, String message, boolean retryable) {
        JsonObject payload = GSON.toJsonTree(new BridgeError(code, message, retryable))
            .getAsJsonObject();
        return BridgeEnvelope.error(id, method, payload);
    }
}
