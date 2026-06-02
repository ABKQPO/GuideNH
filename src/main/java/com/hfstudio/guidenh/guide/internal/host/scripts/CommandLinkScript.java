package com.hfstudio.guidenh.guide.internal.host.scripts;

import net.minecraft.client.Minecraft;

import cpw.mods.fml.common.FMLLog;

import com.hfstudio.guidenh.guide.document.flow.LytFlowLink;
import com.hfstudio.guidenh.guide.internal.host.EventType;
import com.hfstudio.guidenh.guide.internal.host.LytEvent;
import com.hfstudio.guidenh.guide.internal.host.LytScript;
import com.hfstudio.guidenh.guide.internal.host.ScriptContext;
import com.hfstudio.guidenh.guide.internal.host.ScriptType;

public class CommandLinkScript implements LytScript {

    @Override
    public ScriptType type() {
        return ScriptType.JAVA;
    }

    @Override
    public String styleClass() {
        return "CommandLink";
    }

    @Override
    public void onEvent(Object node, LytEvent event, ScriptContext ctx) {
        if (event.type() == EventType.MOUNT && node instanceof LytFlowLink link) {
            String command = (String) link.getData("command");
            Boolean close = (Boolean) link.getData("close");
            if (command == null) return;
            link.setClickCallback(screen -> {
                if (Minecraft.getMinecraft().thePlayer == null) return;
                FMLLog.getLogger().info("[GuideNH] [CommandLink] Sending command: {}", command);
                Minecraft.getMinecraft().thePlayer.sendChatMessage(command);
                if (Boolean.TRUE.equals(close)) {
                    Minecraft.getMinecraft().displayGuiScreen(null);
                }
            });
        }
    }
}
