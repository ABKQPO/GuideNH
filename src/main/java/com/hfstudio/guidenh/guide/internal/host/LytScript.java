package com.hfstudio.guidenh.guide.internal.host;

public interface LytScript {

    ScriptType type();

    String styleClass();

    void onEvent(Object node, LytEvent event, ScriptContext ctx);

    default boolean isAsync() {
        return false;
    }
}
