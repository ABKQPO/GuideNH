package com.hfstudio.guidenh.network;

import com.hfstudio.guidenh.GuideNH;

import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import cpw.mods.fml.relauncher.Side;

public final class GuideNhNetwork {

    private static final SimpleNetworkWrapper CHANNEL = NetworkRegistry.INSTANCE.newSimpleChannel(GuideNH.MODID);

    private GuideNhNetwork() {}

    public static void initCommon() {
        CHANNEL.registerMessage(GuideNhServerHelloHandler.class, GuideNhServerHelloMessage.class, 0, Side.CLIENT);
        CHANNEL.registerMessage(
            GuideNhStructureRequestHandler.class,
            GuideNhStructureRequestMessage.class,
            1,
            Side.SERVER);
    }

    public static SimpleNetworkWrapper channel() {
        return CHANNEL;
    }
}
