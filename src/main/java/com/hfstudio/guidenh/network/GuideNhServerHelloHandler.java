package com.hfstudio.guidenh.network;

import com.hfstudio.guidenh.guide.internal.structure.GuideNhStructureRuntime;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;

public class GuideNhServerHelloHandler implements IMessageHandler<GuideNhServerHelloMessage, IMessage> {

    @Override
    public IMessage onMessage(GuideNhServerHelloMessage message, MessageContext ctx) {
        GuideNhStructureRuntime.setServerStructureCommandsAvailable(true);
        GuideNhStructureRuntime.setClientStructureSyncNeeded(true);
        return null;
    }
}
