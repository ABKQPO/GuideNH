package com.hfstudio.guidenh.guide.internal.scheduler;

import com.hfstudio.guidenh.guide.internal.GuideRegistry;
import com.hfstudio.guidenh.guide.internal.GuideWarmupScheduler;
import com.hfstudio.guidenh.guide.internal.MutableGuide;

public class WarmupWorkItem implements WorkItem {

    private final GuideWarmupScheduler scheduler;
    private long currentTick;

    public WarmupWorkItem() {
        this.scheduler = new GuideWarmupScheduler();
    }

    @Override
    public Priority priority() { return Priority.LOW; }

    @Override
    public boolean shouldRun() {
        for (MutableGuide guide : GuideRegistry.getAll()) {
            if (guide.hasDevelopmentSources()) return true;
        }
        return true; // Always check — guides may have pages to warm up
    }

    @Override
    public WorkResult tick(long deadlineNs) {
        currentTick++;
        for (MutableGuide guide : GuideRegistry.getAll()) {
            guide.populateWarmupScheduler(scheduler, currentTick);
        }
        scheduler.processTick(currentTick);
        // Low priority — always yield so scheduler can run higher-priority items
        return WorkResult.DONE;
    }

    public void clearScheduler() {
        scheduler.clear();
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof WarmupWorkItem;
    }

    @Override
    public int hashCode() { return WarmupWorkItem.class.hashCode(); }
}
