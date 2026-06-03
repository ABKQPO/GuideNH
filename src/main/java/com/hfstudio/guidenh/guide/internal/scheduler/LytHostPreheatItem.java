package com.hfstudio.guidenh.guide.internal.scheduler;

import com.hfstudio.guidenh.guide.internal.host.LytHost;

public class LytHostPreheatItem implements WorkItem {

    private final LytHost host;

    public LytHostPreheatItem(LytHost host) {
        this.host = host;
    }

    @Override
    public Priority priority() {
        return Priority.MEDIUM;
    }

    @Override
    public boolean shouldRun() {
        return host.hasPreheatWork();
    }

    @Override
    public WorkResult tick(long deadlineNs) {
        host.preheatStep(deadlineNs);
        return WorkResult.YIELD; // never leave the queue — shouldRun guards when idle
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof LytHostPreheatItem;
    }

    @Override
    public int hashCode() {
        return LytHostPreheatItem.class.hashCode();
    }
}
