package com.hfstudio.guidenh.guide.internal.scheduler;

import com.hfstudio.guidenh.guide.internal.host.LytHost;

public class LytHostPreheatItem implements WorkItem {
    private final LytHost host;

    public LytHostPreheatItem(LytHost host) {
        this.host = host;
    }

    @Override
    public Priority priority() { return Priority.MEDIUM; }

    @Override
    public boolean shouldRun() { return false; }

    @Override
    public WorkResult tick(long deadlineNs) {
        return WorkResult.DONE;
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
