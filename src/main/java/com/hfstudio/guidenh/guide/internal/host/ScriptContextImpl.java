package com.hfstudio.guidenh.guide.internal.host;

import java.util.HashMap;
import java.util.Map;
import com.hfstudio.guidenh.guide.document.block.LytDocument;
import com.hfstudio.guidenh.guide.document.block.LytNode;

class ScriptContextImpl implements ScriptContext {
    private final Map<String, Object> data = new HashMap<>();
    private final LytNode node;
    private final LytHost host;
    private final LytDocument document;

    ScriptContextImpl(LytNode node, LytHost host, LytDocument document) {
        this.node = node;
        this.host = host;
        this.document = document;
    }

    @Override
    public Map<String, Object> data() { return data; }

    @Override
    public void replace(LytNode newNode) {
        LytNode parent = node.getParent();
        if (parent != null) {
            parent.replaceChild(node, newNode);
        }
    }

    @Override
    public String allocateId(String prefix) {
        return prefix + ":" + System.identityHashCode(node);
    }

    @Override
    public LytDocument document() { return document; }
}
