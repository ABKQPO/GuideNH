package com.hfstudio.guidenh.network;

import net.minecraft.client.Minecraft;

import com.hfstudio.guidenh.client.command.GuideNhClientBridgeController;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class GuideNhClientBridgeHandler implements IMessageHandler<GuideNhClientBridgeMessage, IMessage> {

    @Override
    public IMessage onMessage(GuideNhClientBridgeMessage message, MessageContext ctx) {
        GuideNhClientBridgeDispatcher.dispatch(
            message,
            task -> Minecraft.getMinecraft()
                .func_152344_a(task),
            GuideNhClientBridgeController.getInstance()::beginImportStructure);
        return null;
    }
}
