package com.hfstudio.guidenh.bridge.semantic.providers;

import com.hfstudio.guidenh.bridge.semantic.SemanticCapability;
import com.hfstudio.guidenh.bridge.semantic.SemanticProvider;
import com.hfstudio.guidenh.bridge.semantic.SemanticProviderRegistry;

public class RuntimeSemanticProviders {

    private RuntimeSemanticProviders() {}

    public static void registerBaseline(SemanticProviderRegistry registry) {
        SemanticProvider itemProvider = new ItemSemanticProvider();
        registry.register(itemProvider);
        registry.register(new AliasSemanticProvider(SemanticCapability.RECIPES, itemProvider));
        registry.register(new PageSemanticProvider());
        registry.register(new OreSemanticProvider());
        registry.register(new CategorySemanticProvider());
        registry.register(new ModSemanticProvider());
        registry.register(new CommandSemanticProvider());
        registry.register(new SoundSemanticProvider());
        registry.register(new KeybindSemanticProvider());
        registry.register(new QuestSemanticProvider());
        registry.register(new StructureLibSemanticProvider());
        registry.register(new EntitySemanticProvider());
    }
}
