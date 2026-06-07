package com.hfstudio.guidenh.guide.internal.scheduler;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;

public class MasterScheduler {

    private static final long HIGH_BUDGET_NS = 4_000_000L;
    private static final long MEDIUM_BUDGET_NS = 2_000_000L;
    private static final long LOW_BUDGET_NS = 2_000_000L;

    private final Map<WorkItem, WorkItem> high = new LinkedHashMap<>();
    private final Map<WorkItem, WorkItem> medium = new LinkedHashMap<>();
    private final Map<WorkItem, WorkItem> low = new LinkedHashMap<>();

    private static MasterScheduler instance;

    public static MasterScheduler getInstance() {
        return instance;
    }

    public static void init() {
        instance = new MasterScheduler();
        FMLCommonHandler.instance()
            .bus()
            .register(instance);
    }

    private MasterScheduler() {}

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        long deadlineNs = System.nanoTime() + HIGH_BUDGET_NS;

        processPriority(high, deadlineNs);
        deadlineNs = Math.max(deadlineNs, System.nanoTime()) + MEDIUM_BUDGET_NS;
        processPriority(medium, deadlineNs);
        deadlineNs = Math.max(deadlineNs, System.nanoTime()) + LOW_BUDGET_NS;
        processPriority(low, deadlineNs);
    }

    public void submit(WorkItem item) {
        Map<WorkItem, WorkItem> queue = queueFor(item.priority());
        queue.put(item, item);
    }

    public void cancel(WorkItem item) {
        queueFor(item.priority()).remove(item);
    }

    public void clear() {
        high.clear();
        medium.clear();
        low.clear();
    }

    private Map<WorkItem, WorkItem> queueFor(Priority p) {
        switch (p) {
            case HIGH:
                return high;
            case MEDIUM:
                return medium;
            default:
                return low;
        }
    }

    private void processPriority(Map<WorkItem, WorkItem> queue, long deadlineNs) {
        Iterator<WorkItem> it = new ArrayList<>(queue.values()).iterator();
        while (it.hasNext() && System.nanoTime() < deadlineNs) {
            WorkItem item = it.next();
            if (!item.shouldRun()) continue;
            WorkResult result = item.tick(deadlineNs);
            if (result == WorkResult.DONE) {
                queue.remove(item);
            }
        }
    }
}
