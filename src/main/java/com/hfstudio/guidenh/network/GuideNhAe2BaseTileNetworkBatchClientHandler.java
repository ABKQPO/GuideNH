package com.hfstudio.guidenh.network;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;

public class GuideNhAe2BaseTileNetworkBatchClientHandler
    implements IMessageHandler<GuideNhAe2BaseTileNetworkBatchReplyMessage, IMessage> {

    @Override
    public IMessage onMessage(GuideNhAe2BaseTileNetworkBatchReplyMessage message, MessageContext ctx) {
        GuideNhAe2BaseTileNetworkBatchAwait.complete(message);
        return null;
    }
}
