package com.hfstudio.guidenh.guide.internal.host;

import com.hfstudio.guidenh.guide.internal.scheduler.Priority;
import com.hfstudio.guidenh.guide.internal.scheduler.WorkItem;
import com.hfstudio.guidenh.guide.internal.scheduler.WorkResult;

public class LytHostWorkItem implements WorkItem {

    private final LytHost host;

    public LytHostWorkItem(LytHost host) {
        this.host = host;
    }

    @Override
    public Priority priority() {
        return Priority.HIGH;
    }

    @Override
    public boolean shouldRun() {
        return host.hasWork();
    }

    @Override
    public WorkResult tick(long deadlineNs) {
        host.step(deadlineNs);
        return WorkResult.YIELD; // never leave the queue — shouldRun guards when idle
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof LytHostWorkItem;
    }

    @Override
    public int hashCode() {
        return LytHostWorkItem.class.hashCode();
    }
}
