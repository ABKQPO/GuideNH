package com.hfstudio.guidenh.guide.internal.host.scripts;

import com.hfstudio.guidenh.guide.internal.host.LytEvent;
import com.hfstudio.guidenh.guide.internal.host.LytScript;
import com.hfstudio.guidenh.guide.internal.host.ScriptContext;
import com.hfstudio.guidenh.guide.internal.host.ScriptType;
import com.hfstudio.guidenh.guide.document.block.LytNode;

public class CsvTableScript implements LytScript {
    @Override
    public ScriptType type() { return ScriptType.JAVA; }

    @Override
    public String styleClass() { return "CsvTable"; }

    @Override
    public void onEvent(LytNode node, LytEvent event, ScriptContext ctx) {
        // Stub: CSV loading and table building will be wired later
    }
}
