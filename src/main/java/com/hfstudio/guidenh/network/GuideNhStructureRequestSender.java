package com.hfstudio.guidenh.network;

import cpw.mods.fml.common.network.simpleimpl.SimpleNetworkWrapper;

public class GuideNhStructureRequestSender {

    private GuideNhStructureRequestSender() {}

    public static void sendCache(SimpleNetworkWrapper channel, String structureText) {
        for (GuideNhStructureRequestMessage packet : GuideNhStructureRequestMessage.cachePackets(structureText)) {
            channel.sendToServer(packet);
        }
    }

    public static void sendImportAndPlace(SimpleNetworkWrapper channel, int x, int y, int z, String structureText) {
        for (GuideNhStructureRequestMessage packet : GuideNhStructureRequestMessage
            .importAndPlacePackets(x, y, z, structureText)) {
            channel.sendToServer(packet);
        }
    }

    public static void sendPlaceAll(SimpleNetworkWrapper channel, int x, int y, int z) {
        channel.sendToServer(GuideNhStructureRequestMessage.placeAll(x, y, z));
    }
}
