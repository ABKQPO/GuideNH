package com.hfstudio.guidenh.guide.internal.host;

public interface DeferredTask {

    enum Priority {
        HIGH,
        LOW
    }

    enum TaskResult {
        YIELD,
        DONE
    }

    Priority priority();

    TaskResult step(long deadlineNs);

    boolean isDone();
}
