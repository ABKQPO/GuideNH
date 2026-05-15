package com.hfstudio.guidenh.bridge.semantic.providers;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.hfstudio.guidenh.bridge.semantic.SemanticCapability;
import com.hfstudio.guidenh.bridge.semantic.SemanticProviderRegistry;

public class RuntimeSemanticProviders {

    public static void registerBaseline(SemanticProviderRegistry registry) {
        registry.register(
            new StaticSemanticProvider(
                SemanticCapability.ITEMS,
                Collections.singletonList(entry("minecraft:stone", "Stone"))));
        registry.register(new StaticSemanticProvider(SemanticCapability.PAGES, Collections.emptyList()));
        registry.register(new StaticSemanticProvider(SemanticCapability.ORES, Collections.emptyList()));
        registry.register(new StaticSemanticProvider(SemanticCapability.SOUNDS, Collections.emptyList()));
        registry.register(new StaticSemanticProvider(SemanticCapability.KEYBINDS, Collections.emptyList()));
        registry.register(new StaticSemanticProvider(SemanticCapability.RECIPES, Collections.emptyList()));
        registry.register(new StaticSemanticProvider(SemanticCapability.QUESTS, Collections.emptyList()));
        registry.register(new StaticSemanticProvider(SemanticCapability.STRUCTURELIB, Collections.emptyList()));
    }

    private static Map<String, String> entry(String id, String label) {
        Map<String, String> entry = new HashMap<>();
        entry.put("id", id);
        entry.put("label", label);
        return entry;
    }
}
