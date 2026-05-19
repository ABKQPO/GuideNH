package com.hfstudio.guidenh.bridge.semantic.providers;

import com.hfstudio.guidenh.bridge.semantic.SemanticProvider;
import com.hfstudio.guidenh.bridge.semantic.SemanticQuery;
import com.hfstudio.guidenh.bridge.semantic.SemanticQueryResult;

public class AliasSemanticProvider implements SemanticProvider {

    private final String capability;
    private final SemanticProvider delegate;

    public AliasSemanticProvider(String capability, SemanticProvider delegate) {
        this.capability = capability;
        this.delegate = delegate;
    }

    @Override
    public String getCapability() {
        return capability;
    }

    @Override
    public SemanticQueryResult query(SemanticQuery query) {
        SemanticQueryResult result = delegate.query(query);
        return new SemanticQueryResult(capability, result.getVersion(), result.getEntries(), result.getNextCursor());
    }
}
