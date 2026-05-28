package com.hfstudio.guidenh.bridge.semantic.providers;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.hfstudio.guidenh.bridge.semantic.SemanticCapability;

public class PageSemanticProvider extends AbstractCollectionSemanticProvider {

    public PageSemanticProvider() {
        super(SemanticCapability.PAGES);
    }

    @Override
    protected List<Map<String, String>> loadEntries() {
        List<Map<String, String>> entries = new ArrayList<>();
        RuntimeSemanticSupport.addPageEntries(entries);
        return entries;
    }
}
