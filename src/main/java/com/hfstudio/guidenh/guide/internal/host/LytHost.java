package com.hfstudio.guidenh.guide.internal.host;

import java.util.ArrayDeque;
import java.util.Deque;

import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.guide.document.block.LytDocument;
import com.hfstudio.guidenh.guide.document.block.LytNode;
import com.hfstudio.guidenh.guide.document.interaction.InteractiveElement;

public class LytHost {

    @Nullable private LytDocument document;
    private final ViewportState viewport = new ViewportState();
    private final NavigationState nav = new NavigationState();
    private final Deque<LytEvent> eventQueue = new ArrayDeque<>();
    private final Deque<DeferredTask> taskQueue = new ArrayDeque<>();

    // ===== Document =====

    public void setDocument(@Nullable LytDocument doc) {
        this.document = doc;
        if (doc != null) {
            viewport.updateContent(doc.getAvailableWidth(), doc.getContentHeight());
        }
    }

    @Nullable public LytDocument getDocument() { return document; }
    public ViewportState getViewport() { return viewport; }
    public NavigationState getNavigation() { return nav; }

    // ===== Sync events =====

    public void pushEvent(LytEvent event) {
        eventQueue.addLast(event);
        processEventsNow();
    }

    private void processEventsNow() {
        while (!eventQueue.isEmpty()) {
            LytEvent event = eventQueue.pollFirst();
            if (document == null || event.target() == null) continue;
            LytNode target = event.target();
            if (target instanceof InteractiveElement interactive) {
                switch (event.type()) {
                    case CLICK:
                    case DOUBLE_CLICK:
                        if (event.data().containsKey("x") && event.data().containsKey("y")) {
                            interactive.mouseClicked(null,
                                ((Number) event.data().get("x")).intValue(),
                                ((Number) event.data().get("y")).intValue(),
                                event.data().containsKey("button")
                                    ? ((Number) event.data().get("button")).intValue() : 0,
                                event.type() == EventType.DOUBLE_CLICK);
                        }
                        break;
                    case MOUSE_SCROLL:
                        // InteractiveElement does not expose mouseScrolled yet
                        break;
                    default:
                        break;
                }
            }
        }
    }

    // ===== Async tasks =====

    public void submitTask(DeferredTask task) {
        taskQueue.addLast(task);
    }

    public boolean hasWork() {
        return !taskQueue.isEmpty();
    }

    public void step(long deadlineNs) {
        while (!taskQueue.isEmpty() && System.nanoTime() < deadlineNs) {
            DeferredTask task = taskQueue.peekFirst();
            DeferredTask.TaskResult result = task.step(deadlineNs);
            if (result == DeferredTask.TaskResult.DONE) {
                taskQueue.pollFirst();
            }
            if (result == DeferredTask.TaskResult.YIELD) {
                break;
            }
        }
    }

    public int pendingTaskCount() {
        return taskQueue.size();
    }

    public void clear() {
        document = null;
        eventQueue.clear();
        taskQueue.clear();
        nav.clear();
    }
}
