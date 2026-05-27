package com.hfstudio.guidenh.guide.mediawiki;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import com.github.bsideup.jabel.Desugar;

public class MediaWikiSpecialPageRefreshController {

    private final AtomicLong revision = new AtomicLong(1L);
    private final AtomicLong queuedRevision = new AtomicLong(Long.MIN_VALUE);
    private final ConcurrentLinkedQueue<RefreshTask> pendingTasks = new ConcurrentLinkedQueue<>();
    private final AtomicBoolean workerScheduled = new AtomicBoolean();
    private final AtomicBoolean closed = new AtomicBoolean();
    private final ExecutorService executor = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "guidenh-mediawiki-refresh");
        thread.setDaemon(true);
        return thread;
    });

    public long currentRevision() {
        return revision.get();
    }

    public long invalidate() {
        return revision.incrementAndGet();
    }

    public boolean isCurrent(long expectedRevision) {
        return revision.get() == expectedRevision;
    }

    public void requestRefresh(long expectedRevision, Runnable task) {
        if (task == null || closed.get()) {
            return;
        }
        while (true) {
            long latestQueuedRevision = queuedRevision.get();
            if (expectedRevision <= latestQueuedRevision) {
                return;
            }
            if (queuedRevision.compareAndSet(latestQueuedRevision, expectedRevision)) {
                break;
            }
        }
        pendingTasks.add(new RefreshTask(expectedRevision, task));
        if (!workerScheduled.compareAndSet(false, true)) {
            return;
        }
        executor.submit(() -> {
            try {
                drainPendingTasks();
            } finally {
                workerScheduled.set(false);
                if (!pendingTasks.isEmpty() && workerScheduled.compareAndSet(false, true)) {
                    executor.submit(this::drainPendingTasks);
                }
            }
        });
    }

    public void close() {
        if (!closed.compareAndSet(false, true)) {
            return;
        }
        pendingTasks.clear();
        workerScheduled.set(false);
        executor.shutdownNow();
    }

    private void drainPendingTasks() {
        while (true) {
            RefreshTask refreshTask = pendingTasks.poll();
            if (refreshTask == null) {
                return;
            }
            long latestQueuedRevision = queuedRevision.get();
            if (refreshTask.revision() < latestQueuedRevision || !isCurrent(refreshTask.revision())) {
                continue;
            }
            refreshTask.task()
                .run();
        }
    }

    @Desugar
    private record RefreshTask(long revision, Runnable task) {}
}
