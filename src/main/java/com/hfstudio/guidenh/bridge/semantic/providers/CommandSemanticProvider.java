package com.hfstudio.guidenh.bridge.semantic.providers;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.hfstudio.guidenh.bridge.semantic.SemanticCapability;

public class CommandSemanticProvider extends AbstractCollectionSemanticProvider {

    public CommandSemanticProvider() {
        super(SemanticCapability.COMMANDS);
    }

    @Override
    protected List<Map<String, String>> loadEntries() {
        List<Map<String, String>> entries = new ArrayList<>();
        RuntimeSemanticSupport.addGlobalCommandEntries(entries);
        RuntimeSemanticSupport.addGuideCommandEntries(entries);
        RuntimeSemanticSupport.addGuideNhClientCommandEntries(entries);
        RuntimeSemanticSupport.addStructureExportCommandEntries(entries);
        return entries;
    }
}
