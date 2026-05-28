package com.hfstudio.guidenh.bridge.semantic.providers;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.hfstudio.guidenh.bridge.semantic.SemanticCapability;

public class KeybindSemanticProvider extends AbstractCollectionSemanticProvider {

    public KeybindSemanticProvider() {
        super(SemanticCapability.KEYBINDS);
    }

    @Override
    protected List<Map<String, String>> loadEntries() {
        List<Map<String, String>> entries = new ArrayList<>();
        RuntimeSemanticSupport.addKeybindEntries(entries);
        return entries;
    }
}
