package com.hfstudio.guidenh.integration.ae2.network;

import com.hfstudio.guidenh.integration.Mods;
import com.hfstudio.guidenh.network.GuideNhNetwork;

import cpw.mods.fml.relauncher.Side;

public final class Ae2NetworkRegistration {

    public static final int CABLE_BATCH_REQUEST_ID = 3;
    public static final int CABLE_BATCH_REPLY_ID = 4;
    public static final int BASE_TILE_NETWORK_BATCH_REQUEST_ID = 5;
    public static final int BASE_TILE_NETWORK_BATCH_REPLY_ID = 6;

    private Ae2NetworkRegistration() {}

    public static void registerCommonMessages() {
        if (!Mods.AE2.isModLoaded()) {
            return;
        }
        GuideNhNetwork.channel()
            .registerMessage(
                GuideNhAe2CableBatchServerHandler.class,
                GuideNhAe2CableBatchRequestMessage.class,
                CABLE_BATCH_REQUEST_ID,
                Side.SERVER);
        GuideNhNetwork.channel()
            .registerMessage(
                GuideNhAe2BaseTileNetworkBatchServerHandler.class,
                GuideNhAe2BaseTileNetworkBatchRequestMessage.class,
                BASE_TILE_NETWORK_BATCH_REQUEST_ID,
                Side.SERVER);
    }

    public static void registerClientMessages() {
        if (!Mods.AE2.isModLoaded()) {
            return;
        }
        GuideNhNetwork.channel()
            .registerMessage(
                GuideNhAe2CableBatchClientHandler.class,
                GuideNhAe2CableBatchReplyMessage.class,
                CABLE_BATCH_REPLY_ID,
                Side.CLIENT);
        GuideNhNetwork.channel()
            .registerMessage(
                GuideNhAe2BaseTileNetworkBatchClientHandler.class,
                GuideNhAe2BaseTileNetworkBatchReplyMessage.class,
                BASE_TILE_NETWORK_BATCH_REPLY_ID,
                Side.CLIENT);
    }
}
