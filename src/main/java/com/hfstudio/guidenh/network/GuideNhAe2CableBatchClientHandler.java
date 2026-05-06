package com.hfstudio.guidenh.network;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;

public class GuideNhAe2CableBatchClientHandler implements IMessageHandler<GuideNhAe2CableBatchReplyMessage, IMessage> {

    @Override
    public IMessage onMessage(GuideNhAe2CableBatchReplyMessage message, MessageContext ctx) {
        GuideNhAe2CableBatchAwait.complete(message);
        return null;
    }
}
