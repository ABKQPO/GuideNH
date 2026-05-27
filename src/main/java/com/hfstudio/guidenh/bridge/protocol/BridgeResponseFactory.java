package com.hfstudio.guidenh.bridge.protocol;

import java.nio.charset.StandardCharsets;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.hfstudio.guidenh.bridge.preview.PreviewResolveResult;
import com.hfstudio.guidenh.bridge.preview.PreviewSearchResult;

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

    public BridgeEnvelope previewSearch(String id, PreviewSearchResult result) {
        return BridgeEnvelope.response(
            id,
            "preview.search",
            GSON.toJsonTree(result)
                .getAsJsonObject());
    }

    public BridgeEnvelope previewResolve(String id, PreviewResolveResult result) {
        return BridgeEnvelope.response(
            id,
            "preview.resolve",
            GSON.toJsonTree(result)
                .getAsJsonObject());
    }

    public BridgeEnvelope capabilities(String id, Object capabilities) {
        JsonObject payload = new JsonObject();
        payload.add("capabilities", GSON.toJsonTree(capabilities));
        return BridgeEnvelope.response(id, "capabilities", payload);
    }

    public BridgeEnvelope documentValidate(String id, String method) {
        JsonObject payload = new JsonObject();
        payload.addProperty("accepted", true);
        return BridgeEnvelope.response(id, method, payload);
    }

    public BridgeEnvelope error(String id, String method, String code, String message, boolean retryable) {
        JsonObject payload = GSON.toJsonTree(new BridgeError(code, message, retryable))
            .getAsJsonObject();
        return BridgeEnvelope.error(id, method, payload);
    }

    public void validatePreviewResultSize(PreviewResolveResult result, BridgeProtocolLimits limits) {
        JsonObject payload = GSON.toJsonTree(result)
            .getAsJsonObject();
        int payloadBytes = payload.toString()
            .getBytes(StandardCharsets.UTF_8).length;
        if (payloadBytes > limits.getMaxPreviewResolveBytes()) {
            throw new IllegalArgumentException(
                "Preview payload exceeds maximum size: " + payloadBytes + " > " + limits.getMaxPreviewResolveBytes());
        }
    }
}
