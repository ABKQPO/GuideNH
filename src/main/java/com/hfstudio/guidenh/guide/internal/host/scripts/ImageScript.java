package com.hfstudio.guidenh.guide.internal.host.scripts;

import com.hfstudio.guidenh.guide.internal.host.LytEvent;
import com.hfstudio.guidenh.guide.internal.host.LytScript;
import com.hfstudio.guidenh.guide.internal.host.ScriptContext;
import com.hfstudio.guidenh.guide.internal.host.ScriptType;
import com.hfstudio.guidenh.guide.document.block.LytNode;

/**
 * Script that materializes image content for blocks with styleClass "Img".
 * For Phase 3 initial implementation, this is a stub that will be wired to
 * the asset loading system in a later task.
 */
public class ImageScript implements LytScript {

    @Override
    public ScriptType type() {
        return ScriptType.JAVA;
    }

    @Override
    public String styleClass() {
        return "Img";
    }

    @Override
    public void onEvent(LytNode node, LytEvent event, ScriptContext ctx) {
        // TODO: Load image asset and replace placeholder
        // For Phase 3 initial implementation, this is a stub.
        // The actual image loading will be added when the script infrastructure
        // is fully wired to the asset loading system.
    }
}
