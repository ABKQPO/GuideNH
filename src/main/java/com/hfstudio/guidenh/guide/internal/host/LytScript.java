package com.hfstudio.guidenh.guide.internal.host;

import com.hfstudio.guidenh.guide.document.block.LytNode;

public interface LytScript {
    ScriptType type();
    String styleClass();
    void onEvent(LytNode node, LytEvent event, ScriptContext ctx);
}
