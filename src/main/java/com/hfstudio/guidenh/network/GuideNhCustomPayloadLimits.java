package com.hfstudio.guidenh.network;

public class GuideNhCustomPayloadLimits {

    public static final int MAX_PAYLOAD_BYTES = 28 * 1024;
    public static final int MAX_STRUCTURE_BYTES_PER_PACKET = 27 * 1024;
    public static final int MAX_STRUCTURE_TRANSFER_BYTES = 8 * 1024 * 1024;
    public static final long STRUCTURE_TRANSFER_TTL_MILLIS = 30_000L;

    private GuideNhCustomPayloadLimits() {}
}
