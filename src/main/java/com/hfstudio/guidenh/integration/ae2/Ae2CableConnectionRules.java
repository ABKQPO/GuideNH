package com.hfstudio.guidenh.integration.ae2;

final class Ae2CableConnectionRules {

    private Ae2CableConnectionRules() {}

    static boolean shouldConnect(boolean sourceHasSidePart, boolean sourceBlocked, boolean sourceCanConnect,
        boolean neighborCanConnect, boolean neighborHasSidePart, boolean neighborBlocked, boolean neighborAcceptsSide) {
        return !sourceHasSidePart && !sourceBlocked
            && sourceCanConnect
            && neighborCanConnect
            && !neighborHasSidePart
            && !neighborBlocked
            && neighborAcceptsSide;
    }
}
