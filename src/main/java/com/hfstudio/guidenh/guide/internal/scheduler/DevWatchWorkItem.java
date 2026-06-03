package com.hfstudio.guidenh.guide.internal.scheduler;

public class DevWatchWorkItem implements WorkItem {

    @Override
    public Priority priority() {
        return Priority.LOW;
    }

    @Override
    public boolean shouldRun() {
        return false;
    }

    @Override
    public WorkResult tick(long deadlineNs) {
        return WorkResult.DONE;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof DevWatchWorkItem;
    }

    @Override
    public int hashCode() {
        return DevWatchWorkItem.class.hashCode();
    }
}
