package com.hfstudio.guidenh.guide.internal;

import java.util.ArrayDeque;
import java.util.Deque;

import com.hfstudio.guidenh.config.ModConfig;

import cpw.mods.fml.common.FMLLog;

public class GuideWarmupScheduler {

    public static final long NORMAL_RUNTIME_BUDGET_NANOS = 2_000_000L;
    public static final long DEVELOPMENT_VALIDATION_BUDGET_NANOS = 1_000_000L;

    private final Deque<GuideWarmupWorkItem> highPriorityQueue = new ArrayDeque<>();
    private final Deque<GuideWarmupWorkItem> normalQueue = new ArrayDeque<>();
    private final Deque<GuideWarmupWorkItem> validationQueue = new ArrayDeque<>();

    public void enqueue(GuideWarmupWorkItem item) {
        switch (item.kind()) {
            case HIGH_PRIORITY_PAGE -> highPriorityQueue.offerLast(item);
            case NORMAL_PAGE -> normalQueue.offerLast(item);
            case DEV_VALIDATION -> validationQueue.offerLast(item);
        }
    }

    public void clear() {
        int highPriorityCount = highPriorityQueue.size();
        int normalCount = normalQueue.size();
        int validationCount = validationQueue.size();
        highPriorityQueue.clear();
        normalQueue.clear();
        validationQueue.clear();
        if (ModConfig.debug.enableDebugMode && (highPriorityCount > 0 || normalCount > 0 || validationCount > 0)) {
            FMLLog.getLogger()
                .info(
                    "[GuideNH] [GuideWarmupScheduler] Cleared queued warmup work highPriority={}, normal={}, validation={}",
                    highPriorityCount,
                    normalCount,
                    validationCount);
        }
    }

    public void processTick(long nowTick) {
        processQueue(nowTick, NORMAL_RUNTIME_BUDGET_NANOS, false);
        processQueue(nowTick, DEVELOPMENT_VALIDATION_BUDGET_NANOS, true);
    }

    private void processQueue(long nowTick, long budgetNanos, boolean validationOnly) {
        long deadline = System.nanoTime() + budgetNanos;
        while (System.nanoTime() < deadline) {
            GuideWarmupWorkItem item = pollEligible(nowTick, validationOnly);
            if (item == null) {
                return;
            }

            MutableGuide guide = GuideRegistry.getById(item.guideId());
            if (guide == null) {
                continue;
            }

            boolean finished = guide.processWarmupWorkItem(item, nowTick);
            if (!finished) {
                enqueue(item);
            }
        }
    }

    private GuideWarmupWorkItem pollEligible(long nowTick, boolean validationOnly) {
        Deque<GuideWarmupWorkItem> firstQueue = validationOnly ? validationQueue : highPriorityQueue;
        Deque<GuideWarmupWorkItem> secondQueue = validationOnly ? null : normalQueue;
        GuideWarmupWorkItem item = pollEligible(firstQueue, nowTick);
        if (item != null || secondQueue == null) {
            return item;
        }
        return pollEligible(secondQueue, nowTick);
    }

    private GuideWarmupWorkItem pollEligible(Deque<GuideWarmupWorkItem> queue, long nowTick) {
        while (!queue.isEmpty()) {
            GuideWarmupWorkItem item = queue.pollFirst();
            if (item.nextEligibleTick() <= nowTick) {
                return item;
            }
            queue.offerLast(item);
            return null;
        }
        return null;
    }
}
