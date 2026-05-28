package com.hfstudio.guidenh.integration.ae2;

public class Ae2CableConnectionRules {

    private Ae2CableConnectionRules() {}

    public static boolean shouldConnect(boolean sourceHasSidePart, boolean sourceBlocked, boolean sourceCanConnect,
        boolean neighborCanConnect, boolean neighborFaceBlockedByPart, boolean neighborBlocked,
        boolean neighborAcceptsSide) {
        return !sourceHasSidePart && !sourceBlocked
            && sourceCanConnect
            && neighborCanConnect
            && !neighborFaceBlockedByPart
            && !neighborBlocked
            && neighborAcceptsSide;
    }

    public static boolean facePartBlocksAdjacentCable(boolean hasFacePart, boolean facePartCanConnect) {
        return hasFacePart && !facePartCanConnect;
    }
}
