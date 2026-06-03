package com.hfstudio.guidenh.guide.internal.scheduler;

public interface WorkItem {

    Priority priority();

    boolean shouldRun();

    WorkResult tick(long deadlineNs);
}
