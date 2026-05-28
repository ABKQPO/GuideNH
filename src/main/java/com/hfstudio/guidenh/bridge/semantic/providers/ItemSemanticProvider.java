package com.hfstudio.guidenh.bridge.semantic.providers;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.hfstudio.guidenh.bridge.semantic.SemanticCapability;

public class ItemSemanticProvider extends AbstractCollectionSemanticProvider {

    public ItemSemanticProvider() {
        super(SemanticCapability.ITEMS);
    }

    @Override
    protected List<Map<String, String>> loadEntries() {
        List<Map<String, String>> entries = new ArrayList<>();
        RuntimeSemanticSupport.addItemEntries(entries);
        RuntimeSemanticSupport.addBlockOnlyEntries(entries);
        return entries;
    }
}
