package com.hfstudio.guidenh.guide.internal.host.scripts;

import net.minecraft.client.Minecraft;

import com.hfstudio.guidenh.guide.document.flow.LytFlowText;
import com.hfstudio.guidenh.guide.internal.host.EventType;
import com.hfstudio.guidenh.guide.internal.host.LytEvent;
import com.hfstudio.guidenh.guide.internal.host.LytScript;
import com.hfstudio.guidenh.guide.internal.host.ScriptContext;
import com.hfstudio.guidenh.guide.internal.host.ScriptType;

public class PlayerNameScript implements LytScript {

    @Override
    public ScriptType type() {
        return ScriptType.JAVA;
    }

    @Override
    public String styleClass() {
        return "PlayerName";
    }

    @Override
    public void onEvent(Object node, LytEvent event, ScriptContext ctx) {
        if (event.type() == EventType.MOUNT && node instanceof LytFlowText placeholder) {
            String username;
            try {
                username = Minecraft.getMinecraft()
                    .getSession()
                    .getUsername();
            } catch (Exception e) {
                username = "<?>";
            }
            placeholder.setText(username);
        }
    }
}
