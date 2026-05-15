package com.hfstudio.guidenh.bridge.semantic;

public interface SemanticProvider {

    String getCapability();

    SemanticQueryResult query(SemanticQuery query);
}
