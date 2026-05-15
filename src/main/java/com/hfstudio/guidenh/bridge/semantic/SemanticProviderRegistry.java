package com.hfstudio.guidenh.bridge.semantic;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SemanticProviderRegistry {

    private final Map<String, SemanticProvider> providers = new HashMap<>();

    public void register(SemanticProvider provider) {
        providers.put(provider.getCapability(), provider);
    }

    public List<String> getCapabilities() {
        List<String> capabilities = new ArrayList<>(providers.keySet());
        Collections.sort(capabilities);
        return capabilities;
    }

    public SemanticQueryResult query(String capability, SemanticQuery query) {
        SemanticProvider provider = providers.get(capability);
        if (provider == null) {
            throw new IllegalArgumentException("Unknown capability");
        }
        return provider.query(query);
    }
}
