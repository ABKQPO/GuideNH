package com.hfstudio.guidenh.network;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public final class GuideNhAe2CableBatchAwait {

    private static final ConcurrentHashMap<Long, Holder> PENDING = new ConcurrentHashMap<>();

    private GuideNhAe2CableBatchAwait() {}

    public static void register(long corrId) {
        PENDING.put(corrId, new Holder());
    }

    public static void complete(GuideNhAe2CableBatchReplyMessage msg) {
        if (msg == null) {
            return;
        }
        Holder h = PENDING.get(msg.getCorrId());
        if (h != null) {
            h.reply = msg;
            h.latch.countDown();
        }
    }

    public static GuideNhAe2CableBatchReplyMessage await(long corrId, long timeoutMs) throws InterruptedException {
        Holder h = PENDING.get(corrId);
        if (h == null) {
            return null;
        }
        try {
            h.latch.await(timeoutMs, TimeUnit.MILLISECONDS);
        } finally {
            PENDING.remove(corrId, h);
        }
        return h.reply;
    }

    private static final class Holder {

        final CountDownLatch latch = new CountDownLatch(1);
        volatile GuideNhAe2CableBatchReplyMessage reply;
    }
}
