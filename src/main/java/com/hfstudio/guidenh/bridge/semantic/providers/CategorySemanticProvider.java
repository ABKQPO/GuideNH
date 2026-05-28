package com.hfstudio.guidenh.bridge.semantic.providers;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.hfstudio.guidenh.bridge.semantic.SemanticCapability;

public class CategorySemanticProvider extends AbstractCollectionSemanticProvider {

    public CategorySemanticProvider() {
        super(SemanticCapability.CATEGORIES);
    }

    @Override
    protected List<Map<String, String>> loadEntries() {
        List<Map<String, String>> entries = new ArrayList<>();
        RuntimeSemanticSupport.addCategoryEntries(entries);
        return entries;
    }
}
