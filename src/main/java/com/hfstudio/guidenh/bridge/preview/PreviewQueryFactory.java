package com.hfstudio.guidenh.bridge.preview;

import java.util.Map;

import com.google.gson.JsonObject;
import com.hfstudio.guidenh.bridge.protocol.BridgeProtocolLimits;

public class PreviewQueryFactory {

    private final BridgeProtocolLimits limits;

    public PreviewQueryFactory(BridgeProtocolLimits limits) {
        this.limits = limits;
    }

    public String readCapability(JsonObject payload) {
        return PreviewRequestSupport.readOptionalString(payload, "capability", "");
    }

    public PreviewSearchQuery createSearchQuery(JsonObject payload) {
        String capability = PreviewRequestSupport.requireString(payload, "capability");
        String cursor = PreviewRequestSupport.readOptionalString(payload, "cursor", "");
        int limit = PreviewRequestSupport.readBoundedInt(
            payload,
            "limit",
            limits.getMaxPreviewSearchPageSize(),
            1,
            limits.getMaxPreviewSearchPageSize());
        String prefix = PreviewRequestSupport.readOptionalString(payload, "prefix", "");
        Map<String, String> filters = PreviewRequestSupport.readStringMap(payload, "filters");
        return new PreviewSearchQuery(capability, cursor, limit, prefix, filters);
    }

    public PreviewResolveQuery createResolveQuery(JsonObject payload) {
        String capability = PreviewRequestSupport.requireString(payload, "capability");
        String id = PreviewRequestSupport.requireString(payload, "id");
        int count = PreviewRequestSupport.readBoundedInt(payload, "count", 1, 1, 999999);
        String nbt = PreviewRequestSupport.readOptionalString(payload, "nbt", "");
        String renderVariant = PreviewRequestSupport.readOptionalString(payload, "renderVariant", "default");
        Map<String, String> filters = PreviewRequestSupport.readStringMap(payload, "filters");
        return new PreviewResolveQuery(capability, id, count, nbt, renderVariant, filters);
    }
}
