package com.hfstudio.guidenh.guide.internal.scheduler;

import com.hfstudio.guidenh.guide.internal.GuideDevWatcherPump;
import com.hfstudio.guidenh.guide.internal.GuideRegistry;
import com.hfstudio.guidenh.guide.internal.MutableGuide;

public class DevWatchWorkItem implements WorkItem {

    private static final int INTERVAL_TICKS = 20;
    private int tickCounter;

    @Override
    public Priority priority() { return Priority.LOW; }

    @Override
    public boolean shouldRun() {
        tickCounter++;
        if (tickCounter < INTERVAL_TICKS) return false;
        tickCounter = 0;
        return GuideDevWatcherPump.hasAnyDevelopmentSources(GuideRegistry.getAll());
    }

    @Override
    public WorkResult tick(long deadlineNs) {
        for (MutableGuide guide : GuideRegistry.getAll()) {
            if (guide.hasDevelopmentSources()) {
                guide.tickDevelopmentSources();
            }
        }
        return WorkResult.DONE;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof DevWatchWorkItem;
    }

    @Override
    public int hashCode() { return DevWatchWorkItem.class.hashCode(); }
}
