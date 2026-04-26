package com.hfstudio.guidenh.network;

import com.hfstudio.guidenh.client.command.GuideNhClientBridgeController;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public final class GuideNhClientBridgeHandler implements IMessageHandler<GuideNhClientBridgeMessage, IMessage> {

    @Override
    public IMessage onMessage(GuideNhClientBridgeMessage message, MessageContext ctx) {
        if (message.getAction() == GuideNhClientBridgeMessage.ACTION_IMPORT_STRUCTURE) {
            GuideNhClientBridgeController.getInstance()
                .beginImportStructure(message.getX(), message.getY(), message.getZ());
        }
        return null;
    }
}
