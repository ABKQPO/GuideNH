package com.hfstudio.guidenh.guide.internal.host;

import java.util.Map;
import com.hfstudio.guidenh.guide.document.block.LytDocument;
import com.hfstudio.guidenh.guide.document.block.LytNode;

public interface ScriptContext {
    Map<String, Object> data();
    void replace(LytNode newNode);
    String allocateId(String prefix);
    LytDocument document();
}
