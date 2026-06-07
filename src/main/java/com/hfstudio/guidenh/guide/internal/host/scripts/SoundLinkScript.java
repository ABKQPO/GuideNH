package com.hfstudio.guidenh.guide.internal.host.scripts;

import com.hfstudio.guidenh.guide.document.flow.LytFlowLink;
import com.hfstudio.guidenh.guide.internal.host.EventType;
import com.hfstudio.guidenh.guide.internal.host.LytEvent;
import com.hfstudio.guidenh.guide.internal.host.LytScript;
import com.hfstudio.guidenh.guide.internal.host.ScriptContext;
import com.hfstudio.guidenh.guide.internal.host.ScriptType;
import com.hfstudio.guidenh.guide.sound.GuideSoundSpec;

public class SoundLinkScript implements LytScript {

    @Override
    public ScriptType type() {
        return ScriptType.JAVA;
    }

    @Override
    public String styleClass() {
        return "SoundLink";
    }

    @Override
    public void onEvent(Object node, LytEvent event, ScriptContext ctx) {
        if (event.type() == EventType.MOUNT && node instanceof LytFlowLink link) {
            GuideSoundSpec spec = (GuideSoundSpec) link.getData("soundSpec");
            if (spec != null) {
                link.setClickSoundSpec(spec);
            }
        }
    }
}
