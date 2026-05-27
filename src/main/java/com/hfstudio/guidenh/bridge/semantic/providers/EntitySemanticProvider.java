package com.hfstudio.guidenh.bridge.semantic.providers;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.hfstudio.guidenh.bridge.semantic.SemanticCapability;

public class EntitySemanticProvider extends AbstractCollectionSemanticProvider {

    public EntitySemanticProvider() {
        super(SemanticCapability.ENTITIES);
    }

    @Override
    protected List<Map<String, String>> loadEntries() {
        List<Map<String, String>> entries = new ArrayList<>();
        RuntimeSemanticSupport.addEntityEntries(entries);
        return entries;
    }
}
