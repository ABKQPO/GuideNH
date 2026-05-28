package com.hfstudio.guidenh.bridge.semantic.providers;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.hfstudio.guidenh.bridge.semantic.SemanticCapability;

public class OreSemanticProvider extends AbstractCollectionSemanticProvider {

    public OreSemanticProvider() {
        super(SemanticCapability.ORES);
    }

    @Override
    protected List<Map<String, String>> loadEntries() {
        List<Map<String, String>> entries = new ArrayList<>();
        RuntimeSemanticSupport.addOreEntries(entries);
        return entries;
    }
}
