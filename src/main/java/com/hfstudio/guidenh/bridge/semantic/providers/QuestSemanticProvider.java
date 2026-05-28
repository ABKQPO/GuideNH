package com.hfstudio.guidenh.bridge.semantic.providers;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.hfstudio.guidenh.bridge.semantic.SemanticCapability;

public class QuestSemanticProvider extends AbstractCollectionSemanticProvider {

    public QuestSemanticProvider() {
        super(SemanticCapability.QUESTS);
    }

    @Override
    protected List<Map<String, String>> loadEntries() {
        List<Map<String, String>> entries = new ArrayList<>();
        RuntimeSemanticSupport.addQuestEntries(entries);
        return entries;
    }
}
